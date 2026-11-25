package com.example.skydiary.data.remote.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.skydiary.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlin.math.max

/** Stores private images under the authenticated user's folder: daily-photos/{uid}/{date}.jpg */
class PhotoRepository {
    private val client = SupabaseClient.client
    private val bucket get() = client.storage.from(BUCKET)

    private fun userId(): String = client.auth.currentUserOrNull()?.id
        ?: error("กรุณาเข้าสู่ระบบก่อน")

    private suspend fun recordIdOrNull(uid: String, date: LocalDate): Long? =
        client.from("DAILY_RECORDS").select {
            filter { eq("user_id", uid); eq("record_date", date.toString()) }
        }.decodeList<JsonObject>().firstOrNull()?.get("record_id")?.jsonPrimitive?.longOrNull

    suspend fun savePhoto(date: LocalDate, bitmap: Bitmap) {
        val uid = userId()
        val recordId = recordIdOrNull(uid, date) ?: error("กรุณาบันทึกอารมณ์ก่อนเพิ่มรูป")
        val path = "$uid/${date}.jpg"
        val bytes = encodeJpeg(bitmap)
        require(bytes.size <= MAX_BYTES) { "รูปภาพมีขนาดเกิน 5 MB" }
        bucket.upload(path, bytes) {
            upsert = true
            contentType = ContentType.Image.JPEG
        }
        val old = client.from("PHOTO_RECORDS").select {
            filter { eq("record_id", recordId) }
        }.decodeList<JsonObject>()
        // PHOTO_RECORDS.uploaded_at is NOT NULL with no default in the database.
        val uploadedAt = java.time.OffsetDateTime.now().toString()
        if (old.isEmpty()) {
            client.from("PHOTO_RECORDS").insert(buildJsonObject {
                put("record_id", recordId); put("storage_path", path); put("uploaded_at", uploadedAt)
            })
        } else {
            client.from("PHOTO_RECORDS").update(buildJsonObject {
                put("storage_path", path); put("uploaded_at", uploadedAt)
            }) { filter { eq("record_id", recordId) } }
        }
    }

    /** Removes the file from Storage and its PHOTO_RECORDS row. No-op if the day has no photo. */
    suspend fun deletePhoto(date: LocalDate) {
        val uid = userId()
        val recordId = recordIdOrNull(uid, date) ?: return
        val rows = client.from("PHOTO_RECORDS").select {
            filter { eq("record_id", recordId) }
        }.decodeList<JsonObject>()
        val paths = rows.mapNotNull { it["storage_path"]?.jsonPrimitive?.contentOrNull }
            .filter { it.startsWith("$uid/") }
        if (paths.isNotEmpty()) bucket.delete(paths)
        client.from("PHOTO_RECORDS").delete { filter { eq("record_id", recordId) } }
    }

    suspend fun loadPhotos(): Map<LocalDate, Bitmap> {
        val uid = userId()
        val daily = client.from("DAILY_RECORDS").select {
            filter { eq("user_id", uid) }
        }.decodeList<JsonObject>()
        val dates = daily.mapNotNull { row ->
            val id = row["record_id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val date = runCatching { LocalDate.parse(row["record_date"]!!.jsonPrimitive.content) }.getOrNull()
                ?: return@mapNotNull null
            id to date
        }.toMap()
        if (dates.isEmpty()) return emptyMap()
        val rows = client.from("PHOTO_RECORDS").select {
            filter { isIn("record_id", dates.keys.toList()) }
        }.decodeList<JsonObject>()
        val result = mutableMapOf<LocalDate, Bitmap>()
        for (row in rows) {
            val date = dates[row["record_id"]?.jsonPrimitive?.longOrNull] ?: continue
            val path = row["storage_path"]?.jsonPrimitive?.contentOrNull ?: continue
            if (!path.startsWith("$uid/")) continue
            runCatching {
                val bytes = bucket.downloadAuthenticated(path)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()?.let { result[date] = it }
        }
        return result
    }

    /** Camera/gallery images can be 12MP+; shrink to keep uploads well under the 5 MB bucket limit. */
    private fun encodeJpeg(source: Bitmap): ByteArray {
        val longest = max(source.width, source.height)
        val bitmap = if (longest > MAX_DIMENSION) {
            val scale = MAX_DIMENSION.toFloat() / longest
            Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true)
        } else source
        val stream = ByteArrayOutputStream()
        require(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)) { "แปลงรูปภาพไม่สำเร็จ" }
        return stream.toByteArray()
    }

    private companion object {
        const val BUCKET = "daily-photos"
        const val MAX_BYTES = 5 * 1024 * 1024
        const val MAX_DIMENSION = 1600
    }
}
