package com.example.skydiary.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import com.example.skydiary.components.*
import com.example.skydiary.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class Mood(val key: String, val label: String)

private val moods = listOf(
    Mood("happy", "มีความสุข"),
    Mood("sad", "เศร้า , เสียใจ"),
    Mood("scared", "กลัว"),
    Mood("angry", "โกรธ"),
    Mood("confused", "วุ่นวายใจ")
)

// สีป้ายชื่อและสีตัวอักษรตามอารมณ์
private fun badgeColor(key: String?): Color = when (key) {
    "happy" -> Color(0xFFFFD83D)
    "sad" -> Color(0xFF5B9BFF)
    "scared" -> Color(0xFFFFA94D)
    "angry" -> Color(0xFFA0A0AC)
    "confused" -> Color(0xFFB57BFF)
    else -> Color(0xFFB57BFF)
}

private fun badgeTextColor(key: String?): Color = when (key) {
    "happy" -> Color(0xFF5A4300)
    "angry" -> Color(0xFF1E1E24)
    else -> Color.White
}

@Composable
fun MoodScreen(
    isFemale: Boolean = true,
    dateText: String = "",
    streak: Int = 0,
    onClose: () -> Unit = {},
    onSave: (mood: String, note: String, period: String?) -> Unit = { _, _, _ -> }
) {
    var selected by remember { mutableStateOf<Mood?>(null) }
    var note by remember { mutableStateOf("") }
    var period by remember { mutableStateOf<String?>(null) }
    var moodError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "✕",
            modifier = Modifier.align(Alignment.End).clickable { onClose() },
            fontSize = 24.sp, color = SkyDanger
        )
        Text(
            "SkyDiary",
            modifier = Modifier.align(Alignment.Start),
            fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = SkyText
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "วันนี้คุณรู้สึกยังไงบ้าง?",
            fontFamily = Prompt, fontSize = 26.sp, color = SkyText, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(32.dp))
                .background(SkyCard, RoundedCornerShape(32.dp))
                .padding(20.dp)
        ) {
            Text("วันที่ $dateText", fontFamily = Prompt, fontSize = 15.sp, color = SkyText)
            Spacer(Modifier.height(8.dp))
            Text(
                if (streak > 0) "🔥 บันทึกต่อเนื่อง $streak วัน" else "🔥 เริ่มบันทึกวันแรกของคุณ",
                fontFamily = Prompt, fontSize = 15.sp, color = SkyText
            )
            Spacer(Modifier.height(8.dp))
            Text("โปรดเลือกสภาพอากาศภายในใจของคุณ", fontFamily = Prompt, fontSize = 14.sp, color = SkyText)
            Spacer(Modifier.height(12.dp))

            MoodWheel(
                selected = selected,
                onSelect = { selected = it; moodError = false },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))
            Text("โน้ตประจำวัน", fontFamily = Prompt, fontSize = 15.sp, color = SkyText)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("เริ่มพิมพ์สิ่งที่อยากเล่าตรงนี้สิ", fontFamily = Prompt, color = SkyHint) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFD8E8FF),
                    unfocusedContainerColor = Color(0xFFD8E8FF),
                    focusedBorderColor = SkyBorder,
                    unfocusedBorderColor = SkyBorder
                )
            )
        }

        if (isFemale) {
            Spacer(Modifier.height(16.dp))
            Text(
                "รอบประจำเดือน:",
                modifier = Modifier.align(Alignment.Start),
                fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = SkyTitle
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFC4E0FF), RoundedCornerShape(28.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PeriodOption("🙂", "มาปกติ", period == "normal", Modifier.weight(1f)) { period = "normal" }
                PeriodOption("😟", "มาผิดปกติ", period == "abnormal", Modifier.weight(1f)) { period = "abnormal" }
                PeriodOption("🚫", "ยังไม่ถึงรอบเดือน", period == "none", Modifier.weight(1f)) { period = "none" }
            }
        }

        if (moodError) {
            Spacer(Modifier.height(8.dp))
            Text(
                "กรุณาเลือกอารมณ์ของคุณก่อนบันทึก",
                fontFamily = Prompt, fontSize = 13.sp, color = SkyDanger
            )
        }

        Spacer(Modifier.height(20.dp))
        SkyButton(
            "บันทึก",
            onClick = {
                val m = selected
                if (m == null) {
                    moodError = true
                } else {
                    moodError = false
                    onSave(m.key, note, if (isFemale) period else null)
                }
            }
        )
        Spacer(Modifier.height(20.dp))
    }
}

/**
 * วงล้ออารมณ์: รูปทั้ง 5 เรียงเป็นวงกลมอยู่ในวงสีฟ้า
 * - แตะรูป -> วงหมุนให้รูปนั้นมาอยู่ตำแหน่งล่างสุด (ใกล้ป้าย) / ตรงกลางตามการจัดวาง และขยายใหญ่ขึ้น
 * - ลากนิ้วบนวงเพื่อหมุนเองได้
 */
@Composable
private fun MoodWheel(selected: Mood?, onSelect: (Mood) -> Unit, modifier: Modifier = Modifier) {
    val wheelSize = 290.dp
    val orbitRadius = 92.dp          // รัศมีวงโคจรของรูป (อยู่ในวงสีฟ้าเสมอ)
    val n = moods.size
    val step = 360f / n

    val scope = rememberCoroutineScope()
    // มุมหมุนของวง (องศา) รูปแรกอยู่บนสุดตอนมุม 0
    val rotation = remember { Animatable(0f) }

    // หมุนวงให้รูปที่เลือกมาอยู่ "บนสุด" แล้วแสดงเป็นรูปใหญ่ตรงกลางผ่านการสลับตำแหน่ง
    fun rotateTo(index: Int) {
        val current = rotation.value
        var target = -index * step
        while (target - current > 180f) target -= 360f
        while (target - current < -180f) target += 360f
        scope.launch { rotation.animateTo(target, tween(450)) }
    }

    val badge by animateColorAsState(badgeColor(selected?.key), tween(350), label = "badge")
    val badgeText by animateColorAsState(badgeTextColor(selected?.key), tween(350), label = "badgeText")

    Box(
        modifier = modifier
            .size(wheelSize)
            .background(Color(0xFFA4BDF5), CircleShape)
            .pointerInput(Unit) {
                // ลากนิ้วเพื่อหมุนวง
                var lastAngle = 0f
                detectDragGestures(
                    onDragStart = { p ->
                        lastAngle = Math.toDegrees(
                            atan2((p.y - size.height / 2f).toDouble(), (p.x - size.width / 2f).toDouble())
                        ).toFloat()
                    },
                    onDrag = { change, _ ->
                        val a = Math.toDegrees(
                            atan2(
                                (change.position.y - size.height / 2f).toDouble(),
                                (change.position.x - size.width / 2f).toDouble()
                            )
                        ).toFloat()
                        var d = a - lastAngle
                        if (d > 180f) d -= 360f
                        if (d < -180f) d += 360f
                        lastAngle = a
                        scope.launch { rotation.snapTo(rotation.value + d) }
                    },
                    onDragEnd = {
                        // ปล่อยนิ้ว: ดึงให้รูปที่ใกล้ตำแหน่งบนสุดที่สุดมาตรงช่อง แล้วเลือกให้
                        val idx = (((-rotation.value / step).roundToInt() % n) + n) % n
                        rotateTo(idx)
                        onSelect(moods[idx])
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // ----- รูปอารมณ์ทั้ง 5 เรียงบนวงโคจร -----
        moods.forEachIndexed { i, m ->
            val angleDeg = i * step + rotation.value - 90f   // -90 = เริ่มที่ด้านบน
            val rad = angleDeg * PI.toFloat() / 180f
            val isSel = selected == m

            val iconSize by animateFloatAsState(if (isSel) 74f else 52f, tween(300), label = "size")

            val ox = (cos(rad) * orbitRadius.value).dp
            val oy = (sin(rad) * orbitRadius.value).dp

            Column(
                modifier = Modifier
                    .offset(x = ox, y = oy)
                    .clickable {
                        rotateTo(i)
                        onSelect(m)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MoodIcon(m.key, size = iconSize.dp)
                if (!isSel) {
                    Text(
                        m.label,
                        fontFamily = Prompt, fontSize = 13.sp, color = SkyText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ----- ตรงกลาง: รูปที่เลือก (ใหญ่) + ป้ายชื่อเปลี่ยนสีตามอารมณ์ -----
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = 0.dp)
        ) {
            if (selected != null) {
                MoodIcon(selected.key, size = 96.dp)
            } else {
                Text("☁️", fontSize = 50.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                selected?.label ?: "เลือกเลย",
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(18.dp))
                    .background(badge, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = badgeText
            )
        }
    }
}

@Composable
private fun PeriodOption(
    icon: String,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(if (selected) Color(0xFF7CB6FF) else Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, SkyBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 30.sp)
        Text(label, fontFamily = Prompt, fontSize = 12.sp, color = SkyText, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 1000)
@Composable
fun MoodPreview() {
    SkyDiaryTheme { MoodScreen(dateText = "11 เมษายน 2569", streak = 3) }
}