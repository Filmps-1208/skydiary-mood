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
fun ForgotPasswordScreen(
    // คืนค่า true ถ้าอีเมลและเบอร์ตรงกับบัญชีเดียวกัน
    onVerify: (email: String, phone: String) -> Boolean = { _, _ -> false },
    onResetPassword: (newPassword: String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var step by remember { mutableIntStateOf(1) }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var verifyError by remember { mutableStateOf<String?>(null) }

    val required = "กรุณากรอกช่องนี้"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 40.dp)
    ) {
        Text(
            "‹ กลับ",
            modifier = Modifier.clickable { onBack() },
            fontFamily = Prompt, fontSize = 16.sp, color = SkyLink
        )
        Spacer(Modifier.height(20.dp))

        if (step == 1) {
            Text(
                "ยืนยันตัวตน",
                fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = SkyText
            )
            Text(
                "กรอกอีเมลและเบอร์โทรศัพท์ที่ใช้สมัครสมาชิก",
                fontFamily = Prompt, fontSize = 15.sp, color = SkyHint
            )
            Spacer(Modifier.height(24.dp))

            SkyLabel("อีเมล")
            Spacer(Modifier.height(6.dp))
            SkyTextField(
                email, { email = it.trim(); verifyError = null },
                keyboardType = KeyboardType.Email,
                error = if (submitted && email.isEmpty()) required else emailError(email)
            )

            Spacer(Modifier.height(14.dp))
            SkyLabel("เบอร์โทรศัพท์")
            Spacer(Modifier.height(6.dp))
            SkyTextField(
                phone, { phone = filterPhone(it); verifyError = null },
                keyboardType = KeyboardType.Number,
                error = if (submitted && phone.isEmpty()) required else phoneError(phone)
            )

            if (verifyError != null) {
                Spacer(Modifier.height(10.dp))
                Text(verifyError!!, fontFamily = Prompt, fontSize = 13.sp, color = SkyDanger)
            }

            Spacer(Modifier.height(28.dp))
            SkyButton("ยืนยันตัวตน", {
                submitted = true
                val ok = email.isNotEmpty() && phone.isNotEmpty() &&
                        emailError(email) == null && phoneError(phone) == null
                if (ok) {
                    if (onVerify(email, phone)) {
                        submitted = false
                        step = 2
                    } else {
                        verifyError = "ไม่พบบัญชีที่ตรงกับอีเมลและเบอร์โทรศัพท์นี้"
                    }
                }
            })
        } else {
            Text(
                "ตั้งรหัสผ่านใหม่",
                fontFamily = Prompt, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = SkyText
            )
            Text(
                "ยืนยันตัวตนสำเร็จ กรุณาตั้งรหัสผ่านใหม่",
                fontFamily = Prompt, fontSize = 15.sp, color = SkyHint
            )
            Spacer(Modifier.height(24.dp))

            SkyLabel("รหัสผ่านใหม่")
            Spacer(Modifier.height(6.dp))
            SkyTextField(
                newPass, { newPass = it },
                KeyboardType.Password, isPassword = true,
                error = if (submitted && newPass.isEmpty()) required else passwordError(newPass)
            )
            if (passwordError(newPass) == null && !(submitted && newPass.isEmpty())) {
                Text(
                    "อย่างน้อย 6 ตัว และมีตัวเลขอย่างน้อย 2 ตัว",
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    fontFamily = Prompt, fontSize = 12.sp, color = SkyHint
                )
            }

            Spacer(Modifier.height(14.dp))
            SkyLabel("ยืนยันรหัสผ่านใหม่")
            Spacer(Modifier.height(6.dp))
            SkyTextField(
                confirm, { confirm = it },
                KeyboardType.Password, isPassword = true,
                error = if (submitted && confirm.isEmpty()) required else confirmError(newPass, confirm)
            )

            Spacer(Modifier.height(28.dp))
            SkyButton("บันทึกรหัสผ่านใหม่", {
                submitted = true
                val ok = newPass.isNotEmpty() && confirm.isNotEmpty() &&
                        passwordError(newPass) == null && confirmError(newPass, confirm) == null
                if (ok) onResetPassword(newPass)
            })
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
fun ForgotPasswordPreview() {
    SkyDiaryTheme { ForgotPasswordScreen() }
}