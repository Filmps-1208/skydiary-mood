package com.example.skydiary.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skydiary.components.*
import com.example.skydiary.ui.theme.*

private val activityOptions = listOf(
    "ออกกำลังกาย", "เล่นกีฬา", "ดูหนัง/ซีรี่ย์",
    "ช้อปปิ้ง", "เดินห้าง", "คาเฟ่",
    "วาดรูป", "อื่นๆ"
)

@Composable
fun BehaviorScreen(
    dateText: String = "",
    onSave: (bed: String, wake: String, sleepMinutes: Int, food: String?, waterMl: Int?, activities: List<String>) -> Unit =
        { _, _, _, _, _, _ -> },
    onSkip: () -> Unit = {}
) {
    val context = LocalContext.current
    var bed by remember { mutableStateOf("23:00") }
    var wake by remember { mutableStateOf("07:00") }
    var food by remember { mutableStateOf<String?>(null) }
    var water by remember { mutableStateOf("") }
    var activities by remember { mutableStateOf(setOf<String>()) }

    fun pickTime(current: String, onPicked: (String) -> Unit) {
        val (h, m) = current.split(":").map { it.toInt() }
        android.app.TimePickerDialog(
            context,
            { _, hh, mm -> onPicked("%02d:%02d".format(hh, mm)) },
            h, m, true
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 24.dp)
    ) {
        Text(
            "การบันทึกพฤติกรรม",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = SkyText
        )
        Text(
            dateText,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = SkyText
        )

        // ----- การนอนหลับ -----
        Spacer(Modifier.height(12.dp))
        Heading("การนอนหลับ:")
        Spacer(Modifier.height(6.dp))
        BehaviorCard {
            TimeRow("🌙", "เวลาเข้านอน", bed) { pickTime(bed) { bed = it } }
            Spacer(Modifier.height(10.dp))
            TimeRow("☀️", "เวลาตื่น", wake) { pickTime(wake) { wake = it } }
            Spacer(Modifier.height(10.dp))
            Text(
                "ระยะเวลารวม ${formatSleep(calcSleepMinutes(bed, wake))}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontFamily = Prompt, fontSize = 17.sp, color = SkyText
            )
        }

        // ----- การทานอาหาร -----
        Spacer(Modifier.height(16.dp))
        Heading("การทานอาหาร:")
        Spacer(Modifier.height(6.dp))
        BehaviorCard {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FoodOption("🍽️", "ทานได้น้อย", food == "less", Modifier.weight(1f)) { food = "less" }
                FoodOption("🍱", "ทานครบ\n3 มื้อ", food == "normal", Modifier.weight(1f)) { food = "normal" }
                FoodOption("🍔", "ทานมากกว่า\nปกติ", food == "more", Modifier.weight(1f)) { food = "more" }
            }
        }

        // ----- การดื่มน้ำ -----
        Spacer(Modifier.height(16.dp))
        Heading("การดื่มน้ำ:")
        Spacer(Modifier.height(6.dp))
        BehaviorCard {
            Text(
                "ปริมาณน้ำที่ดื่มของคุณในวันนี้",
                fontFamily = Prompt, fontSize = 14.sp, color = SkyText
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    SkyTextField(
                        value = water,
                        onValueChange = { water = it.filter { c -> c in '0'..'9' }.take(5) },
                        keyboardType = KeyboardType.Number
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text("มิลลิลิตร", fontFamily = Prompt, fontSize = 14.sp, color = SkyText)
            }
        }

        // ----- กิจกรรม -----
        Spacer(Modifier.height(16.dp))
        Heading("กิจกรรมเพิ่มเติมที่อยากเก็บข้อมูล:")
        Spacer(Modifier.height(6.dp))
        BehaviorCard {
            activityOptions.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { a ->
                        val sel = a in activities
                        Text(
                            a,
                            modifier = Modifier
                                .weight(1f)
                                .shadow(3.dp, RoundedCornerShape(20.dp))
                                .background(if (sel) Color(0xFFBFD9FF) else Color.White, RoundedCornerShape(20.dp))
                                .border(1.dp, SkyBorder, RoundedCornerShape(20.dp))
                                .clickable { activities = if (sel) activities - a else activities + a }
                                .padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                            fontFamily = Prompt, fontSize = 13.sp,
                            color = if (sel) SkyLink else SkyText
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        SkyButton("บันทึกกิจกรรม", {
            onSave(
                bed, wake, calcSleepMinutes(bed, wake),
                food, water.toIntOrNull(),
                activityOptions.filter { it in activities }
            )
        })

        Spacer(Modifier.height(14.dp))
        Text(
            "ข้าม",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .shadow(4.dp, RoundedCornerShape(20.dp))
                .background(SkyDanger, RoundedCornerShape(20.dp))
                .clickable { onSkip() }
                .padding(horizontal = 26.dp, vertical = 6.dp),
            fontFamily = Prompt, fontSize = 14.sp, color = Color.White
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SkyTitle)
}

@Composable
private fun BehaviorCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .background(Color(0xFFF0F6FF), RoundedCornerShape(24.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun TimeRow(icon: String, label: String, time: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 30.sp)
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), fontFamily = Prompt, fontSize = 17.sp, color = SkyText)
        Text(time, fontFamily = Prompt, fontSize = 20.sp, color = SkyText)
        Spacer(Modifier.width(8.dp))
        Text("🕒", fontSize = 24.sp)
    }
}

@Composable
private fun FoodOption(
    icon: String, label: String, selected: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(if (selected) Color(0xFFBFD9FF) else Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, SkyBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 32.sp)
        Text(label, fontFamily = Prompt, fontSize = 12.sp, color = SkyText, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 1100)
@Composable
fun BehaviorPreview() {
    SkyDiaryTheme { BehaviorScreen(dateText = "11/04/2026") }
}