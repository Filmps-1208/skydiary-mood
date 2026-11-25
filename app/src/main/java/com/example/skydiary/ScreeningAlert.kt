package com.example.skydiary

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Android notification for the 14-day negative mood rule.
 * Stays (ongoing) until the user submits DASS-21; MainActivity cancels it after a successful save.
 * Tapping it opens the app straight on the DASS-21 screen (EXTRA_OPEN_PAGE = "screening").
 */
object ScreeningAlert {
    const val EXTRA_OPEN_PAGE = "open_page"
    private const val CHANNEL_ID = "screening_reminder"
    private const val NOTIFICATION_ID = 2001

    fun show(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "แจ้งเตือนแบบประเมิน DASS-21", NotificationManager.IMPORTANCE_HIGH)
        )
        val open = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_PAGE, "screening")
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(context, 2, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val text = "คุณบันทึกอารมณ์ด้านลบติดต่อกัน 14 วัน ลองทำแบบประเมิน DASS-21 เพื่อสำรวจความรู้สึกของตัวเอง"
        manager.notify(NOTIFICATION_ID, NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("SkyDiary • แบบประเมิน DASS-21")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(pending)
            .build())
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
    }
}
