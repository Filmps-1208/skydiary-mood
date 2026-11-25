package com.example.skydiary.data.remote.repository

import com.example.skydiary.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class NotificationRow(
    @SerialName("notification_id") val id: Long,
    val message: String,
    @SerialName("notification_type") val type: String,
    val status: String,
    @SerialName("notification_time") val time: String
)

@Serializable
private data class NewNotification(
    @SerialName("user_id") val userId: String,
    @SerialName("notification_time") val time: String,
    val message: String,
    @SerialName("notification_type") val type: String,
    // No default value on purpose: kotlinx.serialization skips defaults, which sent status = NULL.
    val status: String
)

class NotificationRepository {
    private val client = SupabaseClient.client
    suspend fun list(): List<NotificationRow> {
        val userId = client.auth.currentUserOrNull()?.id ?: return emptyList()
        return client.from("NOTIFICATIONS").select {
            filter { eq("user_id", userId) }
            order("notification_time", Order.DESCENDING)
        }.decodeList()
    }
    suspend fun addScreeningReminderIfAbsent() {
        val userId = client.auth.currentUserOrNull()?.id ?: return
        // No duplicates: skip if one is still pending (unread = DASS-21 not done yet) or one was already made today.
        val today = java.time.LocalDate.now()
        val existing = list().any {
            it.type == "screening_reminder" && (it.status == "unread" || localDateOf(it.time) == today)
        }
        if (!existing) client.from("NOTIFICATIONS").insert(
            NewNotification(userId, OffsetDateTime.now().toString(),
                "คุณบันทึกอารมณ์ด้านลบติดต่อกัน 14 วัน ลองทำแบบประเมิน DASS-21 เพื่อสำรวจความรู้สึกของตัวเอง", "screening_reminder", "unread")
        )
    }
    /** Call after DASS-21 is saved: pending screening reminders become read (ends the "until done" state). */
    suspend fun completeScreeningReminders() {
        val userId = client.auth.currentUserOrNull()?.id ?: return
        client.from("NOTIFICATIONS").update({ set("status", "read") }) {
            filter { eq("user_id", userId); eq("notification_type", "screening_reminder"); eq("status", "unread") }
        }
    }

    suspend fun markRead(ids: List<Long>) {
        if (ids.isEmpty()) return
        val userId = client.auth.currentUserOrNull()?.id ?: return
        client.from("NOTIFICATIONS").update({ set("status", "read") }) {
            filter { isIn("notification_id", ids); eq("user_id", userId) }
        }
    }
}

/** timestamptz comes back in UTC; convert to the phone's local date (Thailand is UTC+7). */
fun localDateOf(timestamp: String): java.time.LocalDate? = runCatching {
    OffsetDateTime.parse(timestamp).atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDate()
}.getOrElse { runCatching { java.time.LocalDate.parse(timestamp.take(10)) }.getOrNull() }
