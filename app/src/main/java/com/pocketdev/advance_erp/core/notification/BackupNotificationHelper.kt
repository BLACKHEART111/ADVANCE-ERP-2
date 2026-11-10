package com.pocketdev.advance_erp.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pocketdev.advance_erp.MainActivity
import com.pocketdev.advance_erp.R

object BackupNotificationHelper {

    private const val CHANNEL_ID = "advance_erp_backup_channel"
    private const val NOTIFICATION_ID_BACKUP_SUCCESS = 7701
    private const val NOTIFICATION_ID_BACKUP_FAILED = 7702

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Advance ERP Backup Notifications"
            val descriptionText = "Notifications regarding auto-backup status and storage location"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun notifyBackupSuccess(
        context: Context,
        fileName: String,
        locationDesc: String,
        isEncrypted: Boolean,
        isAutomatic: Boolean
    ) {
        ensureNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = if (isAutomatic) {
            "অটো ব্যাকআপ সফল হয়েছে ✅ (Auto Backup Complete)"
        } else {
            "ম্যানুয়াল ব্যাকআপ সংরক্ষিত হয়েছে ✅ (Backup Saved)"
        }

        val securityText = if (isEncrypted) "🔒 এনক্রিপ্ট ও পাসওয়ার্ড সুরক্ষিত" else "সাধারণ আনএনক্রিপ্টেড জিপ (.zip)"

        val bigText = buildString {
            append("📁 ফাইল: $fileName\n")
            append("📍 সেভ লোকেশন: $locationDesc\n")
            append("🛡️ সিকিউরিটি: $securityText\n")
            append("সময়মতো আপনার সব প্রোডাক্ট, ভ্যারিয়েন্ট ও ছবি নিরাপদে সংরক্ষিত হয়েছে।")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("📁 $fileName • $locationDesc")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BACKUP_SUCCESS, builder.build())
        } catch (_: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission might not be granted by user yet
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun notifyBackupFailed(
        context: Context,
        error: String,
        isAutomatic: Boolean
    ) {
        ensureNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = if (isAutomatic) "অটো ব্যাকআপ ব্যর্থ হয়েছে ❌" else "ম্যানুয়াল ব্যাকআপ ত্রুটি ❌"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BACKUP_FAILED, builder.build())
        } catch (_: SecurityException) {
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
