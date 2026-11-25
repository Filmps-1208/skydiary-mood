package com.example.skydiary.data.remote

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

object SupabaseConnectionTest {

    suspend fun testConnection(
        email: String,
        password: String
    ) {
        try {
            // 1. Login ด้วยบัญชีทดสอบ
            SupabaseClient.client.auth.signInWith(
                io.github.jan.supabase.auth.providers.builtin.Email
            ) {
                this.email = email
                this.password = password
            }

            Log.d("SkyDiaryTest", "Login successful")

            // 2. อ่านข้อมูลกิจกรรม
            val result = SupabaseClient.client
                .from("ACTIVITIES")
                .select()

            Log.d(
                "SkyDiaryTest",
                "Activities: ${result.data}"
            )

        } catch (e: Exception) {
            Log.e(
                "SkyDiaryTest",
                "Connection test failed",
                e
            )
        }
    }
}
