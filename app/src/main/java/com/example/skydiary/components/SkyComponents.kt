package com.example.skydiary.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skydiary.ui.theme.*

@Composable
fun SkyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    error: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(4.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            isError = error != null,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SkyField,
                unfocusedContainerColor = SkyField,
                focusedBorderColor = SkyBorder,
                unfocusedBorderColor = SkyBorder,
                errorContainerColor = SkyField,
                errorBorderColor = SkyDanger
            )
        )
        if (error != null) {
            Text(
                error,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                fontFamily = Prompt, fontSize = 12.sp, color = SkyDanger
            )
        }
    }
}

@Composable
fun SkyButton(text: String, onClick: () -> Unit, color: Color = SkyPrimary) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(6.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}

@Composable
fun SkyLabel(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth(),
        fontFamily = Prompt, fontSize = 18.sp, color = SkyText
    )
}

@Composable
fun SkyRadio(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = SkyBorder)
        )
        Text(text, fontFamily = Prompt, fontSize = 16.sp, color = SkyText)
    }
}

// ---------- ตัวตรวจสอบข้อมูล (ใช้ร่วมกันทั้งหน้าสมัครและลืมรหัสผ่าน) ----------

// ตัวอักษรอยู่ในบล็อกภาษาไทยหรือไม่ (พยัญชนะ สระ วรรณยุกต์ ฯลฯ)
private fun isThaiChar(c: Char): Boolean =
    Character.UnicodeBlock.of(c) == Character.UnicodeBlock.THAI

// เลขไทย ๐-๙
private fun isThaiDigit(c: Char): Boolean = c in '\u0E50'..'\u0E59'

// ชื่อ: รับเฉพาะภาษาไทย อังกฤษ และเว้นวรรค (ตัดตัวเลขและสัญลักษณ์ทิ้ง)
fun filterName(s: String): String = s.filter { c ->
    c == ' ' ||
            c in 'a'..'z' ||
            c in 'A'..'Z' ||
            (isThaiChar(c) && !isThaiDigit(c))
}

// เบอร์: เฉพาะตัวเลข 0-9 ไม่เกิน 10 ตัว
fun filterPhone(s: String): String =
    s.filter { it in '0'..'9' }.take(10)

fun phoneError(s: String): String? = when {
    s.isEmpty() -> null
    !s.startsWith("0") -> "เบอร์โทรศัพท์ต้องขึ้นต้นด้วย 0"
    s.length < 10 -> "เบอร์โทรศัพท์ต้องมี 10 หลัก"
    else -> null
}

fun emailError(s: String): String? = when {
    s.isEmpty() -> null
    !s.endsWith("@gmail.com") || s.length <= "@gmail.com".length -> "อีเมลต้องลงท้ายด้วย @gmail.com"
    s.removeSuffix("@gmail.com").any { it.isWhitespace() || it == '@' } -> "รูปแบบอีเมลไม่ถูกต้อง"
    else -> null
}

fun passwordError(s: String): String? = when {
    s.isEmpty() -> null
    s.length < 6 -> "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร"
    s.count { it in '0'..'9' } < 2 -> "รหัสผ่านต้องมีตัวเลขอย่างน้อย 2 ตัว"
    else -> null
}

fun confirmError(password: String, confirm: String): String? = when {
    confirm.isEmpty() -> null
    confirm != password -> "รหัสผ่านไม่ตรงกัน"
    else -> null
}