package com.example.skydiary.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skydiary.components.*
import com.example.skydiary.ui.theme.*

private val dayHeaders = listOf("อา.", "จ.", "อ.", "พ.", "พฤ.", "ศ.", "ส.")

@Composable
fun CalendarScreen(
    records: List<DiaryRecord> = emptyList(),
    isFemale: Boolean = true,
    onNavigate: (String) -> Unit = {},
    onAddClick: () -> Unit = {},
    onBellClick: () -> Unit = {},
    hasUnread: Boolean = false
) {
    val now = todayDate()
    var month by remember { mutableStateOf(YearMonth(now.yearBE, now.month)) }

    val byDay: Map<Int, DiaryRecord> = records
        .filter { it.yearBE == month.yearBE && it.month == month.month }
        .associateBy { it.day }

    Column(modifier = Modifier.fillMaxSize().background(SkyBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "ปฏิทิน",
                fontFamily = Prompt, fontWeight = FontWeight.Bold,
                fontSize = 30.sp, color = SkyTitle
            )
            BellButton(hasUnread = hasUnread, onClick = onBellClick)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(32.dp))
                    .background(Color(0xFFF0F6FF), RoundedCornerShape(32.dp))
                    .padding(horizontal = 10.dp, vertical = 18.dp)
            ) {
                MonthPicker(value = month, onChange = { month = it })
                Spacer(Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    dayHeaders.forEach {
                        Text(
                            it,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontFamily = Prompt, fontWeight = FontWeight.Bold,
                            fontSize = 16.sp, color = SkyText
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                val cells: List<Int?> =
                    List(month.firstDayOffset) { null } + (1..month.daysInMonth).toList()
                val weeks = cells.chunked(7)

                weeks.forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (i in 0 until 7) {
                            val day = week.getOrNull(i)
                            DayCell(
                                day = day,
                                record = day?.let { byDay[it] },
                                showPeriod = isFemale,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                if (isFemale) {
                    Spacer(Modifier.height(10.dp))
                    Legend()
                }
            }
        }

        BottomBar(current = "calendar", onNavigate = onNavigate, onAddClick = onAddClick)
    }
}

@Composable
private fun DayCell(
    day: Int?,
    record: DiaryRecord?,
    showPeriod: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.height(62.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (day != null) {
            if (record != null) {
                MoodIcon(record.mood, size = 38.dp)
            } else {
                Box(modifier = Modifier.height(38.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "$day",
                        fontFamily = Prompt, fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, color = SkyText
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (showPeriod) {
                val dotColor = when (record?.period) {
                    "normal" -> Color(0xFFD90000)
                    "abnormal" -> Color(0xFFFFA94D)
                    else -> null
                }
                if (dotColor != null) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(dotColor, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(12.dp).background(Color(0xFFD90000), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text("มาปกติ", fontFamily = Prompt, fontSize = 12.sp, color = SkyHint)
        Spacer(Modifier.width(20.dp))
        Box(Modifier.size(12.dp).background(Color(0xFFFFA94D), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text("มาผิดปกติ", fontFamily = Prompt, fontSize = 12.sp, color = SkyHint)
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
fun CalendarPreview() {
    SkyDiaryTheme { CalendarScreen() }
}