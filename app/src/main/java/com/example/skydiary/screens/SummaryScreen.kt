package com.example.skydiary.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skydiary.components.*
import com.example.skydiary.ui.theme.*
import kotlin.math.roundToInt

private data class MoodSlice(val label: String, val count: Int, val color: Color)

private val moodOrder = listOf("happy", "sad", "scared", "angry", "confused")

private fun pieColor(key: String): Color = when (key) {
    "happy" -> Color(0xFFFFDD57)
    "sad" -> Color(0xFF4F7DF3)
    "scared" -> Color(0xFFF2B56B)
    "angry" -> Color(0xFF8E8E99)
    else -> Color(0xFFB394F5)
}

@Composable
fun SummaryScreen(
    records: List<DiaryRecord> = emptyList(),
    isFemale: Boolean = true,
    onNavigate: (String) -> Unit = {},
    onAddClick: () -> Unit = {},
    onBellClick: () -> Unit = {},
    hasUnread: Boolean = false,
    // ผลแบบประเมิน DASS-21 ทั้งหมด (ล่าสุดก่อน) จะแสดงเฉพาะของเดือนที่เลือก
    dassResults: List<DassRecord> = emptyList(),
    // ส่งผลของเดือนที่เลือก (ล่าสุดก่อน) ไปหน้าดูรายละเอียด
    onDassResultClick: (List<DassRecord>) -> Unit = {}
) {
    val now = todayDate()
    var month by remember { mutableStateOf(YearMonth(now.yearBE, now.month)) }

    val list = records.filter { it.yearBE == month.yearBE && it.month == month.month }
    val monthDass = dassResults.filter { it.yearBE == month.yearBE && it.month == month.month }

    // ----- อารมณ์ -----
    val slices = moodOrder
        .map { k -> k to list.count { it.mood == k } }
        .filter { it.second > 0 }
        .map { (k, n) -> MoodSlice(moodLabel(k), n, pieColor(k)) }
    val maxCount = slices.maxOfOrNull { it.count } ?: 0
    val moodSummary = slices.filter { it.count == maxCount }.joinToString(" , ") { it.label }

    // ----- การนอน -----
    val sleepList = list.mapNotNull { it.sleepMinutes }
    val sleepText = if (sleepList.isEmpty()) "ยังไม่มีข้อมูล" else {
        val avgH = sleepList.average() / 60.0
        val quality = if (avgH >= 7) "การนอนดี" else "นอนน้อยกว่าที่ควร"
        "$quality\nนอนเฉลี่ย ${"%.1f".format(avgH)} ชม./คืน"
    }

    // ----- อาหาร -----
    val foodList = list.mapNotNull { it.food }
    val foodText = if (foodList.isEmpty()) "ยังไม่มีข้อมูล" else {
        val top = foodList.groupingBy { it }.eachCount().maxByOrNull { it.value }!!
        "ส่วนใหญ่${foodLabel(top.key).removePrefix("ทานอาหาร")}\n(${top.value} จาก ${foodList.size} วัน)"
    }

    // ----- น้ำ -----
    val waterList = list.mapNotNull { it.waterMl }
    val waterText = if (waterList.isEmpty()) "ยังไม่มีข้อมูล" else {
        val avg = waterList.average().roundToInt()
        val quality = if (avg >= 1500) "ดื่มน้ำได้ดี" else "ควรดื่มน้ำเพิ่ม"
        "$quality\nเฉลี่ย $avg มล./วัน"
    }

    // ----- ประจำเดือน -----
    val normalDays = list.count { it.period == "normal" }
    val abnormalDays = list.count { it.period == "abnormal" }
    val periodText = if (normalDays + abnormalDays == 0) "ไม่มีบันทึกประจำเดือนในเดือนนี้" else
        "มีประจำเดือน ${normalDays + abnormalDays} วัน\n(ปกติ $normalDays วัน, ผิดปกติ $abnormalDays วัน)"

    Column(modifier = Modifier.fillMaxSize().background(SkyBackground)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                "สรุปผลรายเดือน",
                modifier = Modifier
                    .align(Alignment.Center)
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .background(Color(0xFFE6F1FF), RoundedCornerShape(20.dp))
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                fontFamily = Prompt, fontWeight = FontWeight.Bold,
                fontSize = 20.sp, color = SkyText
            )
            BellButton(
                hasUnread = hasUnread,
                onClick = onBellClick,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            MonthPicker(
                value = month,
                onChange = { month = it },
                fontSize = 18,
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(Color(0xFFE6F1FF), RoundedCornerShape(12.dp))
                    .padding(vertical = 6.dp)
            )

            if (list.isEmpty()) {
                Spacer(Modifier.height(80.dp))
                Text(
                    "ยังไม่มีข้อมูลในเดือนนี้",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontFamily = Prompt, fontSize = 18.sp, color = SkyHint
                )
            } else {
                Spacer(Modifier.height(14.dp))
                SectionTitle("อารมณ์:")
                Spacer(Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(24.dp))
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PieChart(slices, modifier = Modifier.size(140.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "คุณมีภาพรวมอารมณ์ในเดือนนี้ :",
                                fontFamily = Prompt, fontWeight = FontWeight.Bold,
                                fontSize = 14.sp, color = SkyText, textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                moodSummary,
                                modifier = Modifier
                                    .shadow(4.dp, RoundedCornerShape(16.dp))
                                    .background(Color(0xFFC98BFF), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                fontFamily = Prompt, fontSize = 13.sp, color = SkyText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val total = slices.sumOf { it.count }
                    slices.forEach { s ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(12.dp).background(s.color, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${s.label}  ${(s.count * 100f / total).roundToInt()}%  (${s.count} วัน)",
                                fontFamily = Prompt, fontSize = 13.sp, color = SkyText
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionTitle("การนอนหลับ:")
                        Spacer(Modifier.height(6.dp))
                        InfoCard("🛏️", sleepText)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        SectionTitle("การทานอาหาร:")
                        Spacer(Modifier.height(6.dp))
                        InfoCard("🍽️", foodText)
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionTitle("การดื่มน้ำ:")
                        Spacer(Modifier.height(6.dp))
                        InfoCard("🥛", waterText)
                    }
                    if (isFemale) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionTitle("ประจำเดือน:")
                            Spacer(Modifier.height(6.dp))
                            InfoCard(null, periodText)
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            // ผลการประเมินของเดือนที่เลือก (เดือนไหนไม่ได้ทำ ไม่แสดง)
            if (monthDass.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                DassHistorySection(monthDass) { onDassResultClick(monthDass) }
            }

            Spacer(Modifier.height(20.dp))
        }

        BottomBar(current = "summary", onNavigate = onNavigate, onAddClick = onAddClick)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontFamily = Prompt, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, color = SkyTitle
    )
}

@Composable
private fun InfoCard(icon: String?, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Text(icon, fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text,
            fontFamily = Prompt, fontWeight = FontWeight.Bold,
            fontSize = 13.sp, color = SkyText, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PieChart(slices: List<MoodSlice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.count }.coerceAtLeast(1)
    Canvas(modifier = modifier) {
        var startAngle = -90f
        slices.forEach { s ->
            val sweep = 360f * s.count / total
            drawArc(
                color = s.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                size = Size(size.width, size.height)
            )
            startAngle += sweep
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
fun SummaryPreview() {
    SkyDiaryTheme {
        SummaryScreen(
            dassResults = listOf(
                DassRecord("25 พฤศจิกายน 2569", 8, 6, 12, todayDate().yearBE, todayDate().month),
                DassRecord("10 พฤศจิกายน 2569", 12, 9, 15, todayDate().yearBE, todayDate().month)
            )
        )
    }
}