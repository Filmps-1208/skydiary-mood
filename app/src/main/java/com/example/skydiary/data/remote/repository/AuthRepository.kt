package com.example.skydiary.data.remote.repository

import com.example.skydiary.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** deep link ที่ลิงก์ยืนยันอีเมลจะเด้งกลับมา (ตรงกับ intent-filter ใน AndroidManifest) */
const val EMAIL_REDIRECT_URL = "skydiary://login-callback"

class AuthRepository {

    private val supabase = SupabaseClient.client

    suspend fun login(
        email: String,
        password: String
    ): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun logout(

    ): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        name: String,
        phone: String,
        email: String,
        gender: String,
        password: String
    ): Result<Unit> {
        return try {
            // กดลิงก์ยืนยันในอีเมลแล้วเด้งกลับเข้าแอป (ต้องเพิ่ม URL นี้ใน Supabase -> Auth -> URL Configuration -> Redirect URLs)
            supabase.auth.signUpWith(Email, redirectUrl = EMAIL_REDIRECT_URL) {
                this.email = email.trim()
                this.password = password

                data = buildJsonObject {
                    put("full_name", name.trim())
                    put("phone_number", phone.trim())
                    put("gender", gender)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /** Supabase sends a recovery link; app must not change passwords locally. */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        supabase.auth.resetPasswordForEmail(email.trim())
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

}