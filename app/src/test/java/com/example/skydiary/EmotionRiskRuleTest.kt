package com.example.skydiary

import com.example.skydiary.components.DiaryRecord
import com.example.skydiary.data.remote.repository.EmotionRiskService.Companion.countNegativeDays
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** กติกา: อารมณ์ลบติดต่อกัน 14 วัน (รวมวันนี้), ขาดวันไหนหรืออารมณ์ไม่ลบ = เริ่มนับใหม่ */
class EmotionRiskRuleTest {
    private val today = LocalDate.of(2026, 11, 25)

    // daysAgo -> mood
    private fun recs(vararg entries: Pair<Int, String>) = entries.map { (ago, mood) ->
        val d = today.minusDays(ago.toLong())
        DiaryRecord("t@t", d.year + 543, d.monthValue - 1, d.dayOfMonth, mood)
    }

    private fun sadDays(days: Iterable<Int>) = days.map { it to "sad" }.toTypedArray()

    @Test fun fourteenInARow() = assertEquals(14, countNegativeDays(recs(*sadDays(0..13)), today))

    @Test fun thirteenIsNotEnough() = assertEquals(13, countNegativeDays(recs(*sadDays(0..12)), today))

    @Test fun oneMissedDayResets() =
        assertEquals(4, countNegativeDays(recs(*sadDays((0..20).filter { it != 4 })), today))

    @Test fun happyDayResets() =
        assertEquals(5, countNegativeDays(recs(*sadDays(0..4), 5 to "happy", *sadDays(6..20)), today))

    @Test fun todayMustBeRecorded() = assertEquals(0, countNegativeDays(recs(*sadDays(1..14)), today))

    @Test fun onlyCountsAfterLastScreening() =
        assertEquals(3, countNegativeDays(recs(*sadDays(0..20)), today, afterDate = today.minusDays(3)))
}
