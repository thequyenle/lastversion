package net.android.lastversion.alarm.infrastructure.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import net.android.lastversion.R
import net.android.lastversion.MainActivity
import net.android.lastversion.alarm.infrastructure.receiver.AlarmActionReceiver

class AlarmNotificationManager(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    // THAY ĐỔI: Signature method mới
    fun showAlarmNotification(
        alarmId: Int,
        title: String,
        note: String,
        snoozeMinutes: Int,
        vibrationPattern: String,
        soundType: String,
        isSilentModeEnabled: Boolean,
        soundUri: String = ""
    ) {
        if (!hasNotificationPermission()) {
            Log.e(TAG, "No notification permission")
            return
        }

        try {
            val notification = buildAlarmNotification(
                alarmId, title, note, snoozeMinutes,
                vibrationPattern, soundType, isSilentModeEnabled, soundUri
            )

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notificationManager.notify(alarmId, notification)
            Log.d(TAG, "Alarm notification shown for ID: $alarmId")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show alarm notification", e)
        }
    }

    fun showSnoozeNotification(alarmId: Int, title: String, snoozeTime: Long) {
        if (!hasNotificationPermission()) return

        try {
            val notification = buildSnoozeNotification(alarmId, title, snoozeTime)
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notificationManager.notify(alarmId + SNOOZE_ID_OFFSET, notification)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show snooze notification", e)
        }
    }

    fun cancelNotification(alarmId: Int) {
        try {
            notificationManager.cancel(alarmId)
            notificationManager.cancel(alarmId + SNOOZE_ID_OFFSET)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel notification", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val systemNotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Alarm channel
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm notifications"
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // Snooze channel
            val snoozeChannel = NotificationChannel(
                SNOOZE_CHANNEL_ID,
                "Snooze",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Snooze notifications"
                enableVibration(false)
                setShowBadge(false)
            }

            systemNotificationManager.createNotificationChannel(alarmChannel)
            systemNotificationManager.createNotificationChannel(snoozeChannel)
        }
    }

    // THAY ĐỔI: Parameters mới
    private fun buildAlarmNotification(
        alarmId: Int,
        title: String,
        message: String,
        snoozeMinutes: Int,
        vibrationPattern: String,
        soundType: String,
        isSilentModeEnabled: Boolean,
        soundUri: String
    ): android.app.Notification {

        // Content intent - open app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_alarm", true)
            putExtra("alarm_id", alarmId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, alarmId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra("alarm_id", alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, alarmId * 10, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_enable)
            .setContentTitle(title)
            .setContentText(message.ifEmpty { "Time to wake up!" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(contentPendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_clock_disable, "Dismiss", dismissPendingIntent)
            .setDefaults(0)

        // Snooze action - chỉ thêm nếu snoozeMinutes > 0
        if (snoozeMinutes > 0) {
            val snoozeIntent = Intent(context, AlarmActionReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra("alarm_id", alarmId)
                putExtra("alarm_title", title)
                putExtra("snooze_minutes", snoozeMinutes)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context, alarmId * 10 + 1, snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_snooze, "Snooze $snoozeMinutes min", snoozePendingIntent)
        }

        // Sound - dựa vào soundType
        if (soundType != "off") {
            val alarmUri = when {
                soundType == "custom" && soundUri.isNotEmpty() -> Uri.parse(soundUri)
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            // Nếu silent mode enabled, vẫn phát âm thanh
            if (soundType != "off") {
                val alarmUri = when {
                    soundType == "custom" && soundUri.isNotEmpty() -> Uri.parse(soundUri)
                    else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }

                builder.setSound(alarmUri)
                // Note: isSilentModeEnabled chỉ xử lý trong AlarmRingingActivity
            }
        }

        // Vibration - dựa vào vibrationPattern
        when (vibrationPattern) {
            "off" -> {
                // Không rung
            }
            "short" -> builder.setVibrate(longArrayOf(0, 300, 200, 300))
            "long" -> builder.setVibrate(longArrayOf(0, 1000, 500, 1000))
            "double" -> builder.setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            "default" -> builder.setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
        }

        return builder.build()
    }

    private fun buildSnoozeNotification(
        alarmId: Int,
        title: String,
        snoozeTime: Long
    ): android.app.Notification {

        val snoozeText = "Snoozed until ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(snoozeTime))}"

        val cancelSnoozeIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = ACTION_CANCEL_SNOOZE
            putExtra("alarm_id", alarmId)
        }
        val cancelSnoozePendingIntent = PendingIntent.getBroadcast(
            context, alarmId * 100, cancelSnoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, SNOOZE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_snooze)
            .setContentTitle(title)
            .setContentText(snoozeText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_clock, "Cancel", cancelSnoozePendingIntent)
            .setShowWhen(true)
            .setWhen(snoozeTime)
            .build()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        private const val TAG = "AlarmNotificationManager"
        const val ALARM_CHANNEL_ID = "alarm_channel"
        const val SNOOZE_CHANNEL_ID = "snooze_channel"
        const val SNOOZE_ID_OFFSET = 10000

        const val ACTION_DISMISS = "ACTION_DISMISS"
        const val ACTION_SNOOZE = "ACTION_SNOOZE"
        const val ACTION_CANCEL_SNOOZE = "ACTION_CANCEL_SNOOZE"
    }
}