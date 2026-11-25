package com.example.skydiary.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.skydiary.components.*
import com.example.skydiary.ui.theme.*

// ---------------- ข้อมูลแบบประเมิน ----------------

// scale: D = ซึมเศร้า, A = วิตกกังวล, S = เครียด
private data class DassQ(val text: String, val scale: Char)

private val dassQuestions = listOf(
    DassQ("ข้าพเจ้ารู้สึกว่ายากที่จะผ่อนคลายอารมณ์", 'S'),
    DassQ("ข้าพเจ้าทราบว่าข้าพเจ้ามีอาการปากแห้ง", 'A'),
    DassQ("ข้าพเจ้ารู้สึกไม่ดีขึ้นเลย", 'D'),
    DassQ("ข้าพเจ้ามีอาการหายใจลำบาก (เช่น มีอาการหายใจเร็วขึ้นผิดปกติ มีอาการหายใจไม่ออกแม้ว่าจะไม่ได้ออกแรง)", 'A'),
    DassQ("ข้าพเจ้ารู้สึกทำกิจกรรมด้วยตนเองได้ค่อนข้างลำบาก", 'D'),
    DassQ("ข้าพเจ้าเริ่มมีปฏิกิริยาตอบสนองต่อสิ่งต่าง ๆ มากเกินไป", 'S'),
    DassQ("ข้าพเจ้ามีอาการสั่น (เช่น ที่มือทั้งสองข้าง)", 'A'),
    DassQ("ข้าพเจ้ารู้สึกว่าข้าพเจ้าวิตกกังวลมาก", 'S'),
    DassQ("ข้าพเจ้ารู้สึกกังวลกับเหตุการณ์ที่อาจทำให้ข้าพเจ้ารู้สึกตื่นกลัวและกระทำสิ่งใดโดยมิได้คิด", 'A'),
    DassQ("ข้าพเจ้ารู้สึกว่าข้าพเจ้าไม่มีเป้าหมาย", 'D'),
    DassQ("ข้าพเจ้ารู้สึกว่าข้าพเจ้ามีอาการกระวนกระวายใจ", 'S'),
    DassQ("ข้าพเจ้ารู้สึกไม่ผ่อนคลาย", 'S'),
    DassQ("ข้าพเจ้ารู้สึกจิตใจเหงาหงอยและเศร้าซึม", 'D'),
    DassQ("ข้าพเจ้าทนไม่ได้กับภาวะใดก็ตามที่ทำให้ข้าพเจ้าไม่สามารถทำอะไรต่อจากที่ข้าพเจ้ากำลังกระทำอยู่", 'S'),
    DassQ("ข้าพเจ้ารู้สึกว่าข้าพเจ้ามีอาการคล้ายกับอาการหวั่นวิตก", 'A'),
    DassQ("ข้าพเจ้าไม่รู้สึกกระตือรือร้นต่อสิ่งใด", 'D'),
    DassQ("ข้าพเจ้ารู้สึกเป็นคนไม่มีคุณค่า", 'D'),
    DassQ("ข้าพเจ้ารู้สึกว่าข้าพเจ้าค่อนข้างมีอารมณ์ฉุนเฉียวง่าย", 'S'),
    DassQ("ข้าพเจ้ารับรู้ถึงการทำงานของหัวใจของข้าพเจ้าในตอนที่ข้าพเจ้าไม่ได้ออกแรง (เช่น รู้สึกถึงการเต้นของหัวใจเพิ่มขึ้น การหยุดเต้นของหัวใจ)", 'A'),
    DassQ("ข้าพเจ้ารู้สึกกลัวโดยไม่มีเหตุผลใด ๆ", 'A'),
    DassQ("ข้าพเจ้ารู้สึกว่าชีวิตไม่มีความหมาย", 'D')
)

private val dassOptions = listOf(
    "ไม่ตรงกับข้าพเจ้าเลย",
    "ตรงกับข้าพเจ้าบ้าง หรือเกิดขึ้นบางครั้ง",
    "ตรงกับข้าพเจ้า หรือเกิดขึ้นบ่อย",
    "ตรงกับข้าพเจ้ามาก หรือเกิดขึ้นบ่อยมากที่สุด"
)

private class DassScale(val name: String, val code: Char, val bounds: List<Int>)

// เกณฑ์คะแนนดิบของ DASS-21 (ไม่คูณ 2) เต็มด้านละ 21 = เกณฑ์ในคู่มือ DASS (Lovibond & Lovibond, 1995) หาร 2
// ต้องตรงกับ trigger calculate_dass21_levels() ใน Supabase
// ค่าในลิสต์ = ขอบบนของ ปกติ / เล็กน้อย / ปานกลาง / รุนแรง (เกินจากนั้น = รุนแรงมาก)
private val dassScales = listOf(
    DassScale("ภาวะซึมเศร้า (Depression)", 'D', listOf(4, 6, 10, 13)),
    // วิตกกังวล เล็กน้อย = 4 เท่านั้น (คู่มือ 8-9 คะแนน) 5 ขึ้นไป = ปานกลาง (คู่มือ 10-14)
    DassScale("ภาวะวิตกกังวล (Anxiety)", 'A', listOf(3, 4, 7, 9)),
    DassScale("ความเครียด (Stress)", 'S', listOf(7, 9, 12, 16))
)

// คะแนนเต็มต่อด้าน (7 ข้อ × 3 คะแนน)
private const val DASS_SCALE_MAX = 21

private val levelNames = listOf("ปกติ", "เล็กน้อย", "ปานกลาง", "รุนแรง", "รุนแรงมาก")
private val levelColors = listOf(
    Color(0xFF3FAE6A), Color(0xFFD9A400), Color(0xFFF08A24), Color(0xFFE0533D), Color(0xFFB3261E)
)

private fun levelOf(score: Int, bounds: List<Int>): Int =
    bounds.indexOfFirst { score <= it }.let { if (it < 0) 4 else it }

private fun rangeText(bounds: List<Int>, level: Int): String = when (level) {
    0 -> "0 - ${bounds[0]}"
    4 -> "${bounds[3] + 1} ขึ้นไป"
    else -> if (bounds[level - 1] + 1 == bounds[level]) "${bounds[level]}"
            else "${bounds[level - 1] + 1} - ${bounds[level]}"
}

// ---------------- ข้อมูลติดต่อ ----------------

private data class Contact(val name: String, val desc: String, val phones: List<String> = emptyList())

private val nationalContacts = listOf(
    Contact("สายด่วนสุขภาพจิต 1323", "โทรฟรีตลอด 24 ชั่วโมง โดยกรมสุขภาพจิต", listOf("1323")),
    Contact("สายด่วนฉุกเฉิน 1669", "กรณีฉุกเฉินทางการแพทย์", listOf("1669"))
)

// โรงพยาบาล/คลินิก: ใส่ชื่อและเบอร์ตามดีไซน์ที่ตรวจสอบแล้ว เช่น
// Contact("ชื่อโรงพยาบาล", "รายละเอียด", listOf("02-xxx-xxxx"))
private val hospitalContacts = listOf<Contact>()

private fun dial(context: Context, number: String) {
    context.startActivity(
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number.filter { it.isDigit() }))
    )
}

// ---------------- หน้าทำแบบประเมิน ----------------

@Composable
fun DassScreen(
    onClose: () -> Unit = {},
    onSubmit: (List<Int>) -> Unit = {}
) {
    val answers = remember { mutableStateListOf<Int>().apply { repeat(dassQuestions.size) { add(-1) } } }
    var showError by remember { mutableStateOf(false) }
    val answered = answers.count { it >= 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackground)
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "✕",
                modifier = Modifier.clickable { onClose() }.padding(4.dp),
                fontSize = 26.sp, color = SkyDanger
            )
        }
        Text(
            "แบบประเมิน DASS-21",
            fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = SkyText
        )
        Text(
            "ตอบตามความรู้สึกของคุณในช่วงสัปดาห์ที่ผ่านมา",
            fontFamily = Prompt, fontSize = 14.sp, color = SkyHint
        )
        Text(
            "ตอบแล้ว $answered / ${dassQuestions.size} ข้อ",
            fontFamily = Prompt, fontSize = 14.sp, color = SkyLink
        )
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(dassQuestions) { i, q ->
                QuestionCard(
                    number = i + 1,
                    text = q.text,
                    selected = answers[i],
                    missing = showError && answers[i] < 0,
                    onSelect = { answers[i] = it }
                )
            }
            item {
                if (showError && answered < dassQuestions.size) {
                    Text(
                        "กรุณาตอบให้ครบทุกข้อ (ข้อที่ยังไม่ได้ตอบมีกรอบสีแดง)",
                        fontFamily = Prompt, fontSize = 13.sp, color = SkyDanger
                    )
                    Spacer(Modifier.height(8.dp))
                }
                SkyButton("ส่งแบบประเมิน", {
                    if (answered < dassQuestions.size) showError = true
                    else onSubmit(answers.toList())
                })
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun QuestionCard(
    number: Int,
    text: String,
    selected: Int,
    missing: Boolean,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .background(Color(0xFF2F63E8), RoundedCornerShape(20.dp))
            .border(
                if (missing) 3.dp else 0.dp,
                if (missing) SkyDanger else Color.Transparent,
                RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        Text(
            "$number. สัปดาห์ที่ผ่านมา $text",
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE6F1FF), RoundedCornerShape(12.dp))
                .padding(10.dp),
            fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SkyText
        )
        Spacer(Modifier.height(10.dp))
        dassOptions.forEachIndexed { score, label ->
            val sel = selected == score
            Text(
                label,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(if (sel) Color(0xFFBFD9FF) else Color.White, RoundedCornerShape(4.dp))
                    .border(if (sel) 2.dp else 0.dp, if (sel) SkyPrimary else Color.Transparent, RoundedCornerShape(4.dp))
                    .clickable { onSelect(score) }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                fontFamily = Prompt, fontSize = 13.sp, color = SkyText
            )
        }
    }
}

// ---------------- หน้าผลลัพธ์ ----------------

// ผลประเมินที่บันทึกไว้ (คะแนนดิบ 0-21 ต่อด้าน) ใช้แสดงประวัติในหน้าสรุป
data class DassRecord(
    val dateText: String,
    val depression: Int,
    val anxiety: Int,
    val stress: Int,
    // เดือนที่ทำ (ใช้กรองในหน้าสรุปรายเดือน) yearBE = พ.ศ., month = 0-11 เหมือน DiaryRecord
    val yearBE: Int = 0,
    val month: Int = 0
)

// คะแนนดิบ 0-21 ต่อด้าน (ตรงกับที่เก็บในฐานข้อมูล)
private fun DassRecord.scoreOf(code: Char): Int = when (code) {
    'D' -> depression
    'A' -> anxiety
    else -> stress
}

/** หลังทำแบบประเมินเสร็จ: คิดคะแนนจากคำตอบ 21 ข้อ */
@Composable
fun DassResultScreen(
    answers: List<Int>,
    onClose: () -> Unit = {}
) {
    fun sum(code: Char) = dassQuestions.indices.filter { dassQuestions[it].scale == code }
        .sumOf { answers.getOrElse(it) { 0 } }
    DassResultScreen(
        record = DassRecord("", sum('D'), sum('A'), sum('S')),
        onClose = onClose
    )
}

/** แสดงผลประเมินจากคะแนนที่บันทึกไว้ (ใช้ทั้งหลังทำเสร็จ และเปิดดูประวัติจากหน้าสรุป) */
@Composable
fun DassResultScreen(
    record: DassRecord,
    onClose: () -> Unit = {},
    closeText: String = "กลับหน้าหลัก",
    // ถ้าส่งมา จะแสดงแทนบรรทัดวันที่ (ใช้ใส่ลูกศรเลื่อนดูผลย้อนหลัง)
    header: (@Composable () -> Unit)? = null
) {
    var showContact by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val results = dassScales.map { s ->
        val score = record.scoreOf(s.code)
        Triple(s, score, levelOf(score, s.bounds))
    }
    val needHelp = results.any { it.third >= 2 }

    Box(modifier = Modifier.fillMaxSize().background(SkyBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "✕",
                    modifier = Modifier.clickable { onClose() }.padding(4.dp),
                    fontSize = 26.sp, color = SkyDanger
                )
            }
            Text(
                "ผลประเมินของคุณ:",
                fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = SkyText
            )
            if (header != null) {
                Spacer(Modifier.height(8.dp))
                header()
            } else if (record.dateText.isNotBlank()) {
                Text(
                    "ทำแบบประเมินเมื่อ ${record.dateText}",
                    fontFamily = Prompt, fontSize = 14.sp, color = SkyHint
                )
            }
            Spacer(Modifier.height(14.dp))

            Text(
                "เกณฑ์การประเมิน",
                fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SkyTitle
            )
            Text(
                "DASS-21 แปลผลแยกเป็น 3 ด้าน ด้านละ 7 ข้อ (เต็มด้านละ $DASS_SCALE_MAX คะแนน)",
                fontFamily = Prompt, fontSize = 12.sp, color = SkyHint
            )
            Spacer(Modifier.height(8.dp))

            results.forEach { (s, score, level) ->
                ScaleBlock(s, score, level)
                Spacer(Modifier.height(12.dp))
            }

            Text(
                "แบบประเมินนี้เป็นเครื่องมือคัดกรองเบื้องต้นเท่านั้น ไม่ใช่การวินิจฉัยโรค ผลที่ได้ไม่สามารถแทนการประเมินโดยแพทย์หรือนักจิตวิทยาได้",
                fontFamily = Prompt, fontSize = 12.sp, color = SkyHint
            )
            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (needHelp) Color(0xFFFFE3E0) else Color(0xFFE3F4E8), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text(
                    if (needHelp) "ผลของคุณอยู่ในระดับที่แนะนำให้ปรึกษาผู้เชี่ยวชาญ"
                    else "หากคุณรู้สึกไม่สบายใจต่อเนื่อง สามารถปรึกษาผู้เชี่ยวชาญได้เสมอ",
                    fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SkyText
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "สายด่วนสุขภาพจิต 1323 โทรฟรีตลอด 24 ชั่วโมง",
                    modifier = Modifier.clickable { dial(context, "1323") },
                    fontFamily = Prompt, fontSize = 14.sp, color = SkyLink,
                    textDecoration = TextDecoration.Underline
                )
            }

            Spacer(Modifier.height(14.dp))
            SkyButton("ข้อมูลติดต่อขอความช่วยเหลือ", { showContact = true })
            Spacer(Modifier.height(10.dp))
            SkyButton(closeText, onClose, color = SkyBorder)
            Spacer(Modifier.height(20.dp))
        }

        if (showContact) DassContactPopup(onDismiss = { showContact = false })
    }
}

@Composable
private fun ScaleBlock(s: DassScale, score: Int, level: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F6FF), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.name,
                modifier = Modifier.weight(1f),
                fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SkyText
            )
            Text(
                "$score/$DASS_SCALE_MAX คะแนน · ${levelNames[level]}",
                modifier = Modifier
                    .background(levelColors[level], RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFFB8C4D6))
        for (i in 0..4) {
            val mine = i == level
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (mine) Color(0xFFD6E6FF) else Color.Transparent, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 5.dp)
            ) {
                Text(
                    levelNames[i],
                    modifier = Modifier.weight(1f),
                    fontFamily = Prompt, fontSize = 13.sp,
                    fontWeight = if (mine) FontWeight.Bold else FontWeight.Normal, color = SkyText
                )
                Text(
                    rangeText(s.bounds, i),
                    fontFamily = Prompt, fontSize = 13.sp,
                    fontWeight = if (mine) FontWeight.Bold else FontWeight.Normal, color = SkyText
                )
            }
        }
    }
}

// ---------------- ดูผลย้อนหลัง (เลื่อน ‹ › แบบเลือกเดือน) ----------------

/** records เรียงจากล่าสุดไปเก่าสุด ‹ = ครั้งก่อนหน้า (เก่ากว่า), › = ครั้งถัดไป (ใหม่กว่า) */
@Composable
fun DassHistoryScreen(records: List<DassRecord>, onClose: () -> Unit = {}) {
    if (records.isEmpty()) return
    var index by remember { mutableStateOf(0) }
    val i = index.coerceIn(0, records.lastIndex)
    val canOlder = i < records.lastIndex
    val canNewer = i > 0

    DassResultScreen(
        record = records[i],
        onClose = onClose,
        closeText = "กลับหน้าสรุป",
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(Color(0xFFE6F1FF), RoundedCornerShape(12.dp))
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "‹",
                    modifier = Modifier
                        .clickable(enabled = canOlder) { index = i + 1 }
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 26.sp,
                    color = if (canOlder) SkyText else Color(0x33000000)
                )
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    // ครั้งที่ 1 = ครั้งแรกที่ทำ (records[lastIndex]) ครั้งล่าสุด = records[0]
                    Text(
                        "ครั้งที่ ${records.size - i}",
                        fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SkyText
                    )
                    Text(
                        "วันที่ ${records[i].dateText}" + if (i == 0) " (ล่าสุด)" else "",
                        fontFamily = Prompt, fontSize = 12.sp, color = SkyHint
                    )
                }
                Text(
                    "›",
                    modifier = Modifier
                        .clickable(enabled = canNewer) { index = i - 1 }
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 26.sp,
                    color = if (canNewer) SkyText else Color(0x33000000)
                )
            }
        }
    )
}

// ---------------- ผลการประเมินในหน้าสรุป ----------------

/** การ์ดเดียวในหน้าสรุป แสดงผลล่าสุด กดแล้วเข้าไปเลื่อนดูครั้งก่อนๆ ได้ (records เรียงจากล่าสุดไปเก่าสุด) */
@Composable
fun DassHistorySection(records: List<DassRecord>, onOpen: (DassRecord) -> Unit) {
    if (records.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("ผลการประเมิน:", fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SkyTitle)
        Spacer(Modifier.height(6.dp))
        DassHistoryCard(records.first(), count = records.size, onClick = { onOpen(records.first()) })
    }
}

@Composable
private fun DassHistoryCard(r: DassRecord, count: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "ครั้งที่ $count · วันที่ ${r.dateText}",
                modifier = Modifier.weight(1f),
                fontFamily = Prompt, fontWeight = FontWeight.Bold,
                fontSize = 15.sp, color = SkyText
            )
            Text("ดูรายละเอียด ›", fontFamily = Prompt, fontSize = 12.sp, color = SkyLink)
        }
        if (count > 1) {
            Text(
                "ทำแล้วทั้งหมด $count ครั้ง · กดเพื่อดูผลย้อนหลัง",
                fontFamily = Prompt, fontSize = 12.sp, color = SkyHint
            )
        }
        Spacer(Modifier.height(8.dp))
        dassScales.forEach { s ->
            val score = r.scoreOf(s.code)
            val level = levelOf(score, s.bounds)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    s.name.substringBefore(" ("),
                    modifier = Modifier.weight(1f),
                    fontFamily = Prompt, fontSize = 13.sp, color = SkyText
                )
                Text(
                    "$score/$DASS_SCALE_MAX · ${levelNames[level]}",
                    modifier = Modifier
                        .background(levelColors[level], RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White
                )
            }
        }
    }
}

// ---------------- Popup ข้อมูลติดต่อ ----------------

@Composable
fun DassContactPopup(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .background(SkyCard, RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "✕",
                    modifier = Modifier.clickable { onDismiss() }.padding(2.dp),
                    fontSize = 22.sp, color = SkyDanger
                )
            }
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "SkyDiary",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = SkyText
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "ขอบคุณที่ทำแบบประเมิน 💙 ผลที่ได้เป็นเพียงการประเมินเบื้องต้น หากคุณรู้สึกไม่สบายใจต่อเนื่อง แนะนำให้ปรึกษาผู้เชี่ยวชาญ",
                    fontFamily = Prompt, fontSize = 13.sp, color = SkyText
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "ช่องทางขอความช่วยเหลือ (24 ชั่วโมง)",
                    fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SkyTitle
                )
                nationalContacts.forEach { ContactRow(it, context) }

                Text(
                    "ปรึกษาออนไลน์ 1323 (เปิดบริการช่วง 14.30–22.30 น.)",
                    modifier = Modifier.padding(top = 10.dp),
                    fontFamily = Prompt, fontSize = 13.sp, color = SkyText
                )
                Text(
                    "https://1323alltime.camri.go.th/",
                    modifier = Modifier.clickable { uriHandler.openUri("https://1323alltime.camri.go.th/") },
                    fontFamily = Prompt, fontSize = 13.sp, color = SkyLink,
                    textDecoration = TextDecoration.Underline
                )

                if (hospitalContacts.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "โรงพยาบาล/คลินิกที่ให้บริการ",
                        fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SkyTitle
                    )
                    hospitalContacts.forEach { ContactRow(it, context) }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(c: Contact, context: Context) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text("• ${c.name}", fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SkyText)
        Text(c.desc, fontFamily = Prompt, fontSize = 12.sp, color = SkyHint)
        c.phones.forEach { p ->
            Text(
                "โทร $p",
                modifier = Modifier.clickable { dial(context, p) },
                fontFamily = Prompt, fontSize = 13.sp, color = SkyLink,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
fun DassPreview() {
    SkyDiaryTheme { DassScreen() }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 1100)
@Composable
fun DassResultPreview() {
    SkyDiaryTheme { DassResultScreen(List(21) { 1 }) }
}