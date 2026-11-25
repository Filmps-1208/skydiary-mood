
package com.example.skydiary.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skydiary.R
import com.example.skydiary.components.*
import com.example.skydiary.ui.theme.*

@Composable
fun LoginScreen(
    errorMessage: String? = null,
    noticeMessage: String? = null,
    onLoginClick: (String, String) -> Unit = { _, _ -> },
    onRegisterClick: () -> Unit = {},
    onForgotClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))

        Text(
            "SkyDiary",
            fontFamily = Prompt,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            color = SkyText
        )

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "SkyDiary logo",
            modifier = Modifier
                .size(240.dp)
                .padding(vertical = 8.dp)
        )

        Spacer(Modifier.height(24.dp))

        SkyLabel("อีเมล / เบอร์โทรศัพท์")
        Spacer(Modifier.height(8.dp))
        SkyTextField(
            value = email,
            onValueChange = { email = it },
            keyboardType = KeyboardType.Email
        )

        Spacer(Modifier.height(16.dp))

        SkyLabel("รหัสผ่าน")
        Spacer(Modifier.height(8.dp))
        SkyTextField(
            value = password,
            onValueChange = { password = it },
            keyboardType = KeyboardType.Password,
            isPassword = true
        )

        if (noticeMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                noticeMessage,
                modifier = Modifier.fillMaxWidth(),
                fontFamily = Prompt, fontSize = 13.sp, color = Color(0xFF2E9E5B)
            )
        }

        val shownError = localError ?: errorMessage
        if (shownError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                shownError,
                modifier = Modifier.fillMaxWidth(),
                fontFamily = Prompt, fontSize = 13.sp, color = SkyDanger
            )
        }

        Text(
            "ลืมรหัสผ่าน?",
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp)
                .clickable { onForgotClick() },
            fontFamily = Prompt,
            fontSize = 13.sp,
            color = SkyLink
        )

        Spacer(Modifier.height(20.dp))

        SkyButton(
            text = "เข้าสู่ระบบ",
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    localError = "กรุณากรอกอีเมล/เบอร์โทรศัพท์ และรหัสผ่าน"
                } else {
                    localError = null
                    onLoginClick(email.trim(), password)
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        Row {
            Text(
                "ยังไม่มีบัญชีใช่หรือไม่? ",
                fontFamily = Prompt,
                fontSize = 13.sp,
                color = SkyHint
            )
            Text(
                "สมัครสมาชิก",
                modifier = Modifier.clickable { onRegisterClick() },
                fontFamily = Prompt,
                fontSize = 13.sp,
                color = SkyLink
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
fun LoginPreview() {
    SkyDiaryTheme {
        LoginScreen()
    }
}
