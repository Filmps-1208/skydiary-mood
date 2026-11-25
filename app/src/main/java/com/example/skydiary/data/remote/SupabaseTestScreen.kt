package com.example.skydiary.data.remote

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SupabaseTestScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("ยังไม่ได้ทดสอบ") }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("SkyDiary - Supabase Connection Test")

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    status = "กำลังทดสอบ..."

                    try {
                        SupabaseConnectionTest.testConnection(
                            email = email.trim(),
                            password = password
                        )
                        status = "ทดสอบเสร็จแล้ว ดูผลใน Logcat"
                    } catch (e: Exception) {
                        status = "เกิดข้อผิดพลาด: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading &&
                    email.isNotBlank() &&
                    password.isNotBlank()
        ) {
            Text(if (isLoading) "กำลังเชื่อมต่อ..." else "Test Connection")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(status)
    }
}