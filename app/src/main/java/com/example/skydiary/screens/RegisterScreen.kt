package com.example.skydiary.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skydiary.components.*
import com.example.skydiary.ui.theme.*

@Composable
fun RegisterScreen(
    errorMessage: String? = null,
    onRegisterClick: (name: String, phone: String, email: String, gender: String, password: String) -> Unit =
        { _, _, _, _, _ -> },
    onLoginClick: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("female") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    // ก่อนกดสมัคร แสดง error เฉพาะช่องที่ผู้ใช้เริ่มพิมพ์แล้ว / หลังกดสมัคร แสดงช่องว่างด้วย
    val required = "กรุณากรอกช่องนี้"
    val nameErr = if (submitted && name.isBlank()) required else null
    val phoneErr = if (submitted && phone.isEmpty()) required else phoneError(phone)
    val emailErr = if (submitted && email.isEmpty()) required else emailError(email)
    val passErr = if (submitted && password.isEmpty()) required else passwordError(password)
    val confirmErr = if (submitted && confirm.isEmpty()) required else confirmError(password, confirm)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 40.dp)
    ) {
        Text(
            "สร้างบัญชีใหม่",
            fontFamily = Prompt,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            color = SkyText
        )
        Text(
            "กรอกข้อมูลเพื่อเริ่มต้นใช้งาน",
            fontFamily = Prompt,
            fontSize = 15.sp,
            color = SkyHint
        )

        Spacer(Modifier.height(20.dp))

        SkyLabel("ชื่อ - นามสกุล")
        Spacer(Modifier.height(6.dp))
        SkyTextField(name, { name = filterName(it) }, error = nameErr)

        Spacer(Modifier.height(14.dp))
        SkyLabel("เบอร์โทรศัพท์")
        Spacer(Modifier.height(6.dp))
        SkyTextField(phone, { phone = filterPhone(it) }, keyboardType = KeyboardType.Number, error = phoneErr)

        Spacer(Modifier.height(14.dp))
        SkyLabel("อีเมล")
        Spacer(Modifier.height(6.dp))
        SkyTextField(email, { email = it.trim() }, keyboardType = KeyboardType.Email, error = emailErr)

        Spacer(Modifier.height(14.dp))
        SkyLabel("เพศ")
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkyRadio("หญิง", gender == "female") { gender = "female" }
            Spacer(Modifier.width(16.dp))
            SkyRadio("ชาย", gender == "male") { gender = "male" }
        }

        Spacer(Modifier.height(8.dp))
        SkyLabel("รหัสผ่าน")
        Spacer(Modifier.height(6.dp))
        SkyTextField(password, { password = it }, KeyboardType.Password, isPassword = true, error = passErr)
        if (passErr == null) {
            Text(
                "อย่างน้อย 6 ตัว และมีตัวเลขอย่างน้อย 2 ตัว",
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                fontFamily = Prompt, fontSize = 12.sp, color = SkyHint
            )
        }

        Spacer(Modifier.height(14.dp))
        SkyLabel("ยืนยันรหัสผ่าน")
        Spacer(Modifier.height(6.dp))
        SkyTextField(confirm, { confirm = it }, KeyboardType.Password, isPassword = true, error = confirmErr)

        if (errorMessage != null) {
            Spacer(Modifier.height(10.dp))
            Text(errorMessage, fontFamily = Prompt, fontSize = 13.sp, color = SkyDanger)
        }

        Spacer(Modifier.height(24.dp))
        SkyButton("สมัครสมาชิก", {
            submitted = true
            val allOk = name.isNotBlank() && phone.isNotEmpty() && email.isNotEmpty() &&
                    password.isNotEmpty() && confirm.isNotEmpty() &&
                    phoneError(phone) == null && emailError(email) == null &&
                    passwordError(password) == null && confirmError(password, confirm) == null
            if (allOk) {
                onRegisterClick(name.trim(), phone, email, gender, password)
            }
        })

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(
                "มีบัญชีอยู่แล้ว? ",
                fontFamily = Prompt,
                fontSize = 13.sp,
                color = SkyHint
            )
            Text(
                "เข้าสู่ระบบเลย",
                modifier = Modifier.clickable { onLoginClick() },
                fontFamily = Prompt,
                fontSize = 13.sp,
                color = SkyLink
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
fun RegisterPreview() {
    SkyDiaryTheme { RegisterScreen() }
}