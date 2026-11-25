package com.example.skydiary.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skydiary.components.*
import com.example.skydiary.ui.theme.*

@Composable
fun HomeScreen(
    records: List<DiaryRecord> = emptyList(),
    showWelcome: Boolean = false,
    onCloseWelcome: () -> Unit = {},
    showDassBanner: Boolean = false,
    showDassPopup: Boolean = false,
    onDassStart: () -> Unit = {},
    onDassDismiss: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onAddClick: () -> Unit = {},
    onBellClick: () -> Unit = {},
    hasUnread: Boolean = false
) {
    val now = todayDate()
    var month by remember { mutableStateOf(YearMonth(now.yearBE, now.month)) }

    val list = records
        .filter { it.yearBE == month.yearBE && it.month == month.month }
        .sortedByDescending { it.day }

    Box(modifier = Modifier.fillMaxSize().background(SkyBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonthPicker(
                    value = month,
                    onChange = { month = it },
                    fontSize = 22,
                    modifier = Modifier.weight(1f)
                )
                BellButton(
                    hasUnread = hasUnread,
                    onClick = onBellClick,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (showDassBanner) {
                DassBanner(
                    onClick = onDassStart,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            if (list.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "ยังไม่มีบันทึกในเดือนนี้\nกดปุ่ม + เพื่อบันทึกอารมณ์ของคุณ",
                        textAlign = TextAlign.Center,
                        fontFamily = Prompt, fontSize = 16.sp, color = SkyHint
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(list) { EntryCard(it) }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }

            BottomBar(current = "home", onNavigate = onNavigate, onAddClick = onAddClick)
        }

        if (showWelcome) {
            WelcomePopup(onClose = onCloseWelcome)
        } else if (showDassPopup) {
            DassPopup(onStart = onDassStart, onDismiss = onDassDismiss)
        }
    }
}

@Composable
private fun DassBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(22.dp))
            .background(Color(0xFFF4C7D9), RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            "ข้อเสนอแนะการทำแบบประเมิน",
            fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SkyText
        )
        Text(
            "เราเห็นว่าคุณบันทึกอารมณ์เศร้าติดต่อกันหลายวัน แตะเพื่อทำแบบประเมินอารมณ์เบื้องต้น",
            fontFamily = Prompt, fontSize = 12.sp, color = SkyText
        )
    }
}

@Composable
private fun DassPopup(onStart: () -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66FFFFFF))
            .clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(28.dp))
                .background(SkyCard, RoundedCornerShape(28.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "✕",
                modifier = Modifier.align(Alignment.End).clickable { onDismiss() },
                fontSize = 20.sp, color = SkyDanger
            )
            Text("SkyDiary", fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = SkyText)
            Spacer(Modifier.height(8.dp))
            Text("ยินดีต้อนรับ!", fontFamily = Prompt, fontSize = 13.sp, color = SkyText)
            Spacer(Modifier.height(10.dp))
            Text(
                "เราเห็นว่าคุณบันทึกอารมณ์เศร้าติดต่อกันหลายวันแล้ว อยากลองทำแบบประเมินอารมณ์เบื้องต้นไหม?",
                fontFamily = Prompt, fontSize = 16.sp, color = SkyText, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            SkyButton("ทำแบบประเมิน", onStart)
            Spacer(Modifier.height(12.dp))
            Text(
                "DASS-21 เป็นแบบประเมินเบื้องต้นเกี่ยวกับภาวะซึมเศร้า วิตกกังวล และความเครียด ใช้เวลาประมาณ 3-5 นาที " +
                        "ไม่ใช่การวินิจฉัยโรค",
                fontFamily = Prompt, fontSize = 11.sp, color = SkyHint, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            // อ้างอิงที่มาของแบบประเมิน
            Text(
                "แบบประเมินสุขภาพจิตของ Naluri",
                fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                color = SkyText, textAlign = TextAlign.Center
            )
            Text(
                "Naluri ใช้แบบวัดภาวะซึมเศร้า ความวิตกกังวล และความเครียด (DASS-21) ในการประเมินสุขภาพจิต",
                fontFamily = Prompt, fontSize = 11.sp, color = SkyHint, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "ไว้ทำทีหลัง",
                modifier = Modifier.clickable { onDismiss() },
                fontFamily = Prompt, fontSize = 13.sp, color = SkyLink
            )
        }
    }
}

@Composable
private fun EntryCard(r: DiaryRecord) {
    val details = buildList {
        r.sleepMinutes?.let { add("นอนรวม ${formatSleep(it)}") }
        r.food?.let { add(foodLabel(it)) }
        r.waterMl?.let { add("ดื่มน้ำ $it มล.") }
        r.activities.forEach { add(it) }
        when (r.period) {
            "normal" -> add("ประจำเดือนมาปกติ")
            "abnormal" -> add("ประจำเดือนมาผิดปกติ")
        }
    }
    val dateText = "${r.day}/${r.month + 1}/${r.yearBE % 100}"
    val title = if (r.note.isNotBlank()) r.note else moodLabel(r.mood)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(28.dp))
            .background(moodCardColor(r.mood), RoundedCornerShape(28.dp))
            .padding(18.dp)
    ) {
        Text(
            dateText,
            modifier = Modifier.align(Alignment.End),
            fontFamily = Prompt, fontSize = 13.sp, color = SkyText
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoodIcon(r.mood, size = 64.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                ": $title",
                fontFamily = Prompt, fontSize = 20.sp, color = SkyText
            )
        }
        Spacer(Modifier.height(8.dp))

        if (r.photo != null) {
            Row {
                Image(
                    bitmap = r.photo.asImageBitmap(),
                    contentDescription = "รูปท้องฟ้า",
                    modifier = Modifier
                        .width(130.dp)
                        .height(170.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(14.dp))
                Column { details.forEach { DetailText(it) } }
            }
        } else {
            details.forEach { DetailText(it) }
        }
    }
}

@Composable
private fun DetailText(text: String) {
    Text("• $text", fontFamily = Prompt, fontSize = 16.sp, color = SkyText)
}

@Composable
private fun WelcomePopup(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66FFFFFF))
            .clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(28.dp))
                .background(SkyCard, RoundedCornerShape(28.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "✕",
                modifier = Modifier.align(Alignment.End).clickable { onClose() },
                fontSize = 20.sp, color = SkyDanger
            )
            Text("SkyDiary", fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = SkyText)
            Spacer(Modifier.height(8.dp))
            Text("ยินดีต้อนรับ!", fontFamily = Prompt, fontSize = 13.sp, color = SkyText)
            Spacer(Modifier.height(12.dp))
            Text(
                "วันนี้สภาพอากาศภายในใจของคุณเป็นอย่างไร?",
                fontFamily = Prompt, fontSize = 20.sp, color = SkyText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "** อย่าลืมมาบอกเราน่ะ **",
                fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SkyText
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
fun HomePreview() {
    SkyDiaryTheme { HomeScreen(showDassBanner = true) }
}