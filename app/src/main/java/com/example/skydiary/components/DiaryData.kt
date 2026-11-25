package com.example.skydiary.components

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import java.util.Calendar

// บันทึกของ 1 วัน (1 คน 1 วัน)
data class DiaryRecord(
    val owner: String,            // อีเมลเจ้าของบัญชี
    val yearBE: Int,
    val month: Int,               // 0-11
    val day: Int,
    val mood: String,             // happy/sad/scared/angry/confused
    val note: String = "",
    val period: String? = null,   // normal/abnormal/none/null
    val photo: Bitmap? = null,
    val bedTime: String? = null,
    val wakeTime: String? = null,
    val sleepMinutes: Int? = null,
    val food: String? = null,     // less/normal/more
    val waterMl: Int? = null,
    val activities: List<String> = emptyList()
)

data class TodayDate(val yearBE: Int, val month: Int, val day: Int) {
    val fullText: String get() = "$day ${thaiMonths[month]} $yearBE"
    val slashText: String
        get() = "${day.toString().padStart(2, '0')}/${(month + 1).toString().padStart(2, '0')}/${yearBE - 543}"
}

fun todayDate(): TodayDate {
    val c = Calendar.getInstance()
    return TodayDate(c.get(Calendar.YEAR) + 543, c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
}

fun moodLabel(key: String): String = when (key) {
    "happy" -> "มีความสุข"
    "sad" -> "เศร้า , เสียใจ"
    "scared" -> "กลัว"
    "angry" -> "โกรธ"
    else -> "วุ่นวายใจ"
}

fun moodCardColor(key: String): Color = when (key) {
    "happy" -> Color(0xFFFFFBC8)
    "sad" -> Color(0xFFCFE3FF)
    "scared" -> Color(0xFFC98BFF)
    "angry" -> Color(0xFFD9D9E3)
    else -> Color(0xFFE8E3F5)
}

fun foodLabel(key: String): String = when (key) {
    "less" -> "ทานอาหารได้น้อย"
    "normal" -> "ทานอาหารครบ 3 มื้อ"
    else -> "ทานอาหารมากกว่าปกติ"
}

fun formatSleep(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "$h ชม." else "$h ชม. $m นาที"
}

// เวลา "HH:mm" -> นาทีที่นอนทั้งหมด (ข้ามเที่ยงคืนได้)
fun calcSleepMinutes(bed: String, wake: String): Int {
    val (bh, bm) = bed.split(":").map { it.toInt() }
    val (wh, wm) = wake.split(":").map { it.toInt() }
    var d = (wh * 60 + wm) - (bh * 60 + bm)
    if (d <= 0) d += 24 * 60
    return d
}

// จำนวนวันที่บันทึกติดต่อกัน (ถ้าวันนี้ยังไม่บันทึก นับถึงเมื่อวาน)
fun calcStreak(records: List<DiaryRecord>): Int {
    val keys = records.map { Triple(it.yearBE, it.month, it.day) }.toSet()
    val c = Calendar.getInstance()
    fun has() = keys.contains(
        Triple(c.get(Calendar.YEAR) + 543, c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    )
    if (!has()) c.add(Calendar.DAY_OF_MONTH, -1)
    var n = 0
    while (has()) {
        n++
        c.add(Calendar.DAY_OF_MONTH, -1)
    }
    return n
}

// ---------- DASS-21 ----------

// จำนวนวันเศร้าติดต่อกันที่จะเริ่มแนะนำแบบประเมิน (ตอนทดสอบเปลี่ยนเป็น 1 หรือ 2 ได้)
const val DASS_TRIGGER_DAYS = 14

// รหัสวันที่ เช่น 20260411 ใช้เทียบก่อน/หลัง
fun dateCode(yearBE: Int, month: Int, day: Int): Int =
    (yearBE - 543) * 10000 + (month + 1) * 100 + day

// นับจำนวนวัน "เศร้า" ติดต่อกัน (ถ้าวันนี้ยังไม่บันทึก นับถึงเมื่อวาน)
// afterCode = วันที่ทำแบบประเมินล่าสุด จะนับเฉพาะวันหลังจากนั้น
fun calcSadStreak(records: List<DiaryRecord>, afterCode: Int = 0): Int {
    val sad = records.filter { it.mood == "sad" }
        .map { dateCode(it.yearBE, it.month, it.day) }.toSet()
    val c = Calendar.getInstance()
    fun code() = c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 +
            c.get(Calendar.DAY_OF_MONTH)
    if (code() !in sad) c.add(Calendar.DAY_OF_MONTH, -1)
    var n = 0
    while (code() in sad && code() > afterCode) {
        n++
        c.add(Calendar.DAY_OF_MONTH, -1)
    }
    return n
}