package com.example.skydiary.data.remote.repository

import com.example.skydiary.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("user_id")
    val userId: String,

    @SerialName("full_name")
    val fullName: String? = null,

    @SerialName("phone_number")
    val phoneNumber: String? = null,

    val gender: String,

    @SerialName("created_at")
    val createdAt: String? = null
)

class UserRepository {

    private val supabase = SupabaseClient.client

    /** Only editable public profile fields are sent; never store passwords in USERS. */
    suspend fun updateProfile(name: String, phone: String): Result<UserProfile> = try {
        require(name.isNotBlank()) { "กรุณากรอกชื่อ" }
        val userId = supabase.auth.currentUserOrNull()?.id ?: error("กรุณาเข้าสู่ระบบ")
        supabase.from("USERS").update({
            set("full_name", name.trim())
            set("phone_number", phone.trim())
        }) { filter { eq("user_id", userId) } }
        getCurrentUserProfile()
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getCurrentUserProfile(): Result<UserProfile> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(
                    IllegalStateException("ยังไม่ได้เข้าสู่ระบบ")
                )

            val profile = supabase
                .from("USERS")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<UserProfile>()
                ?: return Result.failure(
                    NoSuchElementException("ไม่พบข้อมูลโปรไฟล์ผู้ใช้")
                )

            Result.success(profile)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
