package com.example.skydiary.components

import java.util.Calendar

val thaiMonths = listOf(
    "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
    "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
)

// month: 0-11, yearBE = ปี พ.ศ.
data class YearMonth(val yearBE: Int, val month: Int) {
    val title: String get() = "${thaiMonths[month]} $yearBE"

    fun plus(delta: Int): YearMonth {
        val total = yearBE * 12 + month + delta
        return YearMonth(total / 12, total % 12)
    }

    private fun calendar(): Calendar =
        Calendar.getInstance().apply { set(yearBE - 543, month, 1) }

    val daysInMonth: Int get() = calendar().getActualMaximum(Calendar.DAY_OF_MONTH)

    // อาทิตย์ = 0 ... เสาร์ = 6
    val firstDayOffset: Int get() = calendar().get(Calendar.DAY_OF_WEEK) - 1

    // key สำหรับเก็บข้อมูล เช่น "2569-04"
    val key: String get() = "$yearBE-${(month + 1).toString().padStart(2, '0')}"
}