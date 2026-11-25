package com.example.skydiary.data.remote.repository

import com.example.skydiary.components.DiaryRecord
import java.time.LocalDate

/**
 * เช็คเงื่อนไขแนะนำแบบประเมิน DASS-21: บันทึกอารมณ์ลบติดต่อกัน 14 วัน (รวมวันนี้)
 *
 * - นับย้อนจากวันนี้ไปตามปฏิทิน ต้องมีบันทึกทุกวัน
 * - วันที่ไม่ได้บันทึก หรือบันทึกอารมณ์ไม่ลบ (เช่น happy) = เริ่มนับใหม่
 *   (ถ้าอยากให้ลืมบันทึกได้บางวัน เปลี่ยน [MAX_MISSED_DAYS] เป็น 1 หรือ 2)
 * - นับเฉพาะวันหลังจากทำแบบประเมินครั้งล่าสุด (ทำแล้วต้องสะสมครบรอบใหม่ถึงจะแนะนำอีก)
 */
class EmotionRiskService(
    private val notifications: NotificationRepository = NotificationRepository(),
    private val screenings: ScreeningRepository = ScreeningRepository()
) {
    companion object {
        const val REQUIRED_NEGATIVE_DAYS = 14
        // 0 = ต้องบันทึกติดกันทุกวัน
        const val MAX_MISSED_DAYS = 0
        private val NEGATIVE = setOf("sad", "scared", "angry", "confused")

    /** จำนวนวันอารมณ์ลบที่นับต่อเนื่องได้ (ตามกติกาด้านบน) */
    fun countNegativeDays(
        records: List<DiaryRecord>,
        today: LocalDate = LocalDate.now(),
        afterDate: LocalDate? = null
    ): Int {
        val byDate = records.associateBy { LocalDate.of(it.yearBE - 543, it.month + 1, it.day) }
        val earliest = byDate.keys.minOrNull() ?: return 0
        var day = today
        var count = 0
        var missedInRow = 0
        while (!day.isBefore(earliest) && (afterDate == null || day.isAfter(afterDate))) {
            val mood = byDate[day]?.mood
            when {
                mood == null -> {
                    missedInRow++
                    if (missedInRow > MAX_MISSED_DAYS) break
                }
                mood in NEGATIVE -> {
                    count++
                    missedInRow = 0
                    if (count >= REQUIRED_NEGATIVE_DAYS) break
                }
                else -> break
            }
            day = day.minusDays(1)
        }
        return count
    }
    }

    fun hasFourteenNegativeDays(
        records: List<DiaryRecord>,
        today: LocalDate = LocalDate.now(),
        afterDate: LocalDate? = null
    ): Boolean = countNegativeDays(records, today, afterDate) >= REQUIRED_NEGATIVE_DAYS

    /** Call after successfully saving/loading the authenticated user's records. */
    suspend fun checkAndNotify(records: List<DiaryRecord>, today: LocalDate = LocalDate.now()): Boolean {
        val lastScreening = runCatching { screenings.history().firstOrNull()?.date?.let(LocalDate::parse) }
            .getOrNull()
        val atRisk = hasFourteenNegativeDays(records, today, lastScreening)
        if (atRisk) notifications.addScreeningReminderIfAbsent()
        return atRisk
    }
}
