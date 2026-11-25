package com.example.skydiary.data.remote.repository

import com.example.skydiary.components.DiaryRecord
import com.example.skydiary.components.calcSleepMinutes
import com.example.skydiary.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.*
import java.time.LocalDate

/** Persistent diary operations. All tables are protected by Supabase RLS. */
/** UI label (BehaviorScreen) -> ACTIVITIES.activity_name where the spelling differs. */
private val uiToDbActivity = mapOf("ดูหนัง/ซีรี่ย์" to "ดูหนัง/ซีรีย์")
private fun toDbActivity(ui: String): String = uiToDbActivity[ui.trim()] ?: ui.trim()
private fun toUiActivity(db: String): String =
    uiToDbActivity.entries.firstOrNull { it.value == db.trim() }?.key ?: db.trim()

class DiaryRepository {
    private val db = SupabaseClient.client

    private fun uid(): String = db.auth.currentUserOrNull()?.id
        ?: error("กรุณาเข้าสู่ระบบก่อน")

    private fun JsonObject.str(key: String): String? = this[key]?.let {
        if (it is JsonNull) null else it.jsonPrimitive.content
    }
    private fun JsonObject.num(key: String): Long? = str(key)?.toLongOrNull()

    private suspend fun recordId(date: LocalDate): Long {
        val user = uid()
        val iso = date.toString()
        val existing = db.from("DAILY_RECORDS").select {
            filter { eq("user_id", user); eq("record_date", iso) }
        }.decodeList<JsonObject>().firstOrNull()
        if (existing != null) return existing.num("record_id") ?: error("Missing record ID")
        val inserted = db.from("DAILY_RECORDS").insert(buildJsonObject {
            put("user_id", user)
            put("record_date", iso)
            put("note", "")
        }) { select() }.decodeSingle<JsonObject>()
        return inserted.num("record_id") ?: error("Missing new record ID")
    }

    suspend fun saveMood(date: LocalDate, mood: String, note: String, period: String?) {
        val id = recordId(date)
        db.from("DAILY_RECORDS").update(buildJsonObject { put("note", note) }) {
            filter { eq("record_id", id) }
        }
        val old = db.from("EMOTION_RECORDS").select {
            filter { eq("record_id", id) }
        }.decodeList<JsonObject>()
        val data = buildJsonObject { put("record_id", id); put("emotion_type", mood) }
        if (old.isEmpty()) db.from("EMOTION_RECORDS").insert(data)
        else db.from("EMOTION_RECORDS").update(buildJsonObject { put("emotion_type", mood) }) {
            filter { eq("record_id", id) }
        }
        // UI uses "none" for not-yet-due; database uses "not_due".
        // null means no menstrual status was selected for this record.
        val dbPeriod = when (period) {
            "normal", "abnormal" -> period
            "none", "not_due" -> "not_due"
            null -> null
            else -> error("สถานะประจำเดือนไม่ถูกต้อง")
        }
        val oldPeriod = db.from("MENSTRUAL_RECORDS").select {
            filter { eq("record_id", id) }
        }.decodeList<JsonObject>()
        when {
            dbPeriod == null && oldPeriod.isNotEmpty() ->
                db.from("MENSTRUAL_RECORDS").delete { filter { eq("record_id", id) } }
            dbPeriod != null && oldPeriod.isEmpty() ->
                db.from("MENSTRUAL_RECORDS").insert(buildJsonObject {
                    put("record_id", id)
                    put("period_status", dbPeriod)
                })
            dbPeriod != null ->
                db.from("MENSTRUAL_RECORDS").update(buildJsonObject {
                    put("period_status", dbPeriod)
                }) { filter { eq("record_id", id) } }
        }
    }

    suspend fun saveBehavior(date: LocalDate, bed: String, wake: String, food: String?, water: Int?, activities: List<String>) {
        val id = recordId(date)
        val old = db.from("BEHAVIOR_RECORDS").select {
            filter { eq("record_id", id) }
        }.decodeList<JsonObject>()
        val data = buildJsonObject {
            put("record_id", id)
            put("sleep_time", bed)
            put("wake_time", wake)
            if (food == null) put("food_status", JsonNull) else put("food_status", food)
            if (water == null) put("water_intake_ml", JsonNull) else put("water_intake_ml", water)
        }
        if (old.isEmpty()) db.from("BEHAVIOR_RECORDS").insert(data)
        else db.from("BEHAVIOR_RECORDS").update(data) { filter { eq("record_id", id) } }

        db.from("DAILY_ACTIVITIES").delete { filter { eq("record_id", id) } }
        if (activities.isNotEmpty()) {
            val available = db.from("ACTIVITIES").select().decodeList<JsonObject>()
            val byName = available.associate { (it.str("activity_name") ?: "").trim() to it.num("activity_id") }
            // "อื่นๆ" is intentionally not stored (no row in ACTIVITIES); UI option will be removed by the frontend.
            val missing = activities.filter { it.trim() != "อื่นๆ" && byName[toDbActivity(it)] == null }
            val rows = activities.mapNotNull { byName[toDbActivity(it)] }.distinct().map { activityId ->
                buildJsonObject { put("record_id", id); put("activity_id", activityId) }
            }
            if (rows.isNotEmpty()) db.from("DAILY_ACTIVITIES").insert(rows)
            // UI labels must match ACTIVITIES.activity_name exactly; surface mismatches instead of dropping silently.
            if (missing.isNotEmpty()) error("ไม่พบกิจกรรมในตาราง ACTIVITIES: ${missing.joinToString()}")
        }
    }

    suspend fun loadRecords(email: String): List<DiaryRecord> {
        val user = uid()
        val daily = db.from("DAILY_RECORDS").select { filter { eq("user_id", user) } }.decodeList<JsonObject>()
        if (daily.isEmpty()) return emptyList()
        // Only fetch child rows that belong to this user's daily records (RLS is the second guard).
        val ids = daily.mapNotNull { it.num("record_id") }
        suspend fun children(table: String) = db.from(table).select {
            filter { isIn("record_id", ids) }
        }.decodeList<JsonObject>()
        val emotions = children("EMOTION_RECORDS").associateBy { it.num("record_id") }
        val behaviors = children("BEHAVIOR_RECORDS").associateBy { it.num("record_id") }
        val periods = children("MENSTRUAL_RECORDS").associateBy { it.num("record_id") }
        val dailyActivities = children("DAILY_ACTIVITIES")
        val activityNames = db.from("ACTIVITIES").select().decodeList<JsonObject>()
            .associate { it.num("activity_id") to toUiActivity(it.str("activity_name") ?: "") }
        return daily.mapNotNull { row ->
            val date = runCatching { LocalDate.parse(row.str("record_date")) }.getOrNull() ?: return@mapNotNull null
            val id = row.num("record_id")
            val emotion = emotions[id]
            val behavior = behaviors[id]
            val bed = behavior?.str("sleep_time")?.take(5)
            val wake = behavior?.str("wake_time")?.take(5)
            DiaryRecord(
                owner = email,
                yearBE = date.year + 543,
                month = date.monthValue - 1,
                day = date.dayOfMonth,
                mood = emotion?.str("emotion_type") ?: "",
                note = row.str("note") ?: "",
                period = when (periods[id]?.str("period_status")) {
                    "normal" -> "normal"
                    "abnormal" -> "abnormal"
                    "not_due" -> "none"
                    else -> null
                },
                bedTime = bed,
                wakeTime = wake,
                sleepMinutes = if (bed != null && wake != null) runCatching { calcSleepMinutes(bed, wake) }.getOrNull() else null,
                food = behavior?.str("food_status"),
                waterMl = behavior?.str("water_intake_ml")?.toIntOrNull(),
                activities = dailyActivities.filter { it.num("record_id") == id }
                    .mapNotNull { activityNames[it.num("activity_id")] }.filter { it.isNotEmpty() }
            )
        }
    }
}
