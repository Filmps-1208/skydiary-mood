package com.example.skydiary.data.remote.repository

import com.example.skydiary.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.time.LocalDate

@Serializable
private data class ScreeningInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("screening_date") val screeningDate: String,
    @SerialName("depression_score") val depressionScore: Int,
    @SerialName("anxiety_score") val anxietyScore: Int,
    @SerialName("stress_score") val stressScore: Int
)

class ScreeningRepository {
    private val client = SupabaseClient.client
    /** ผลทั้งหมดของผู้ใช้ เรียงจากล่าสุดไปเก่าสุด (วันเดียวกันใช้ลำดับที่บันทึกตัดสิน) */
    suspend fun history(): List<ScreeningResultRow> {
        // ไม่คืนลิสต์ว่างเงียบๆ ถ้า session หลุด (ไม่งั้นผลประเมินจะดูเหมือนหายไป)
        val userId = client.auth.currentUserOrNull()?.id ?: error("กรุณาเข้าสู่ระบบก่อน")
        val desc = io.github.jan.supabase.postgrest.query.Order.DESCENDING
        // ทำหลายครั้งในวันเดียวกัน screening_date จะเท่ากัน ต้องมีคอลัมน์ลำดับการบันทึกช่วยเรียง
        for (tieBreaker in listOf("screening_id", "created_at")) {
            val rows = runCatching {
                client.from("SCREENING_RESULTS").select {
                    filter { eq("user_id", userId) }
                    order("screening_date", desc)
                    order(tieBreaker, desc)
                }.decodeList<ScreeningResultRow>()
            }
            if (rows.isSuccess) {
                android.util.Log.d("SkyDiary", "screening history ordered by $tieBreaker")
                return rows.getOrThrow()
            }
            android.util.Log.w("SkyDiary", "screening history: no column $tieBreaker", rows.exceptionOrNull())
        }
        return client.from("SCREENING_RESULTS").select {
            filter { eq("user_id", userId) }
            order("screening_date", desc)
        }.decodeList()
    }

    suspend fun save(answers: List<Int>) {
        require(answers.size == 21 && answers.all { it in 0..3 }) { "กรุณาตอบคำถามให้ครบ 21 ข้อ" }
        val userId = client.auth.currentUserOrNull()?.id ?: error("กรุณาเข้าสู่ระบบ")
        // DASS-21 standard subscale item indexes, zero-based. Store RAW 0..21 per domain.
        val d = listOf(2, 4, 9, 12, 15, 16, 20).sumOf { answers[it] }
        val a = listOf(1, 3, 6, 8, 14, 18, 19).sumOf { answers[it] }
        val s = listOf(0, 5, 7, 10, 11, 13, 17).sumOf { answers[it] }
        client.from("SCREENING_RESULTS").insert(ScreeningInsert(userId, LocalDate.now().toString(), d, a, s))
    }
}

@Serializable
data class ScreeningResultRow(
    @SerialName("screening_date") val date: String,
    @SerialName("depression_score") val depressionScore: Int,
    @SerialName("anxiety_score") val anxietyScore: Int,
    @SerialName("stress_score") val stressScore: Int,
    @SerialName("depression_level") val depressionLevel: String? = null,
    @SerialName("anxiety_level") val anxietyLevel: String? = null,
    @SerialName("stress_level") val stressLevel: String? = null,
    val recommendation: String? = null
)
