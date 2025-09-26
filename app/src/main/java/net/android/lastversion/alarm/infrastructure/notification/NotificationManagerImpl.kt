package net.android.lastversion.alarm.infrastructure.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import net.android.lastversion.R
import net.android.lastversion.MainActivity
import net.android.lastversion.alarm.infrastructure.receiver.AlarmActionReceiver

class NotificationManagerImpl(private val context: Context) : net.android.lastversion.alarm.infrastructure.notification.NotificationManager {

    companion object {
        private const val CHANNEL_ID = "alarm_channel"
        private const val CHANNEL_NAME = "Alarms"
        private const val SNOOZE_CHANNEL_ID = "snooze_channel"
        private const val SNOOZE_CHANNEL_NAME = "Snooze Alarms"
        private const val TAG = "NotificationManagerImpl"
    }

    private val notificationManager = NotificationManagerCompat.from(context)
    private val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val alarmChannel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alarm notifications"
                    enableVibration(true)
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
                    setBypassDnd(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }

                val snoozeChannel = NotificationChannel(
                    SNOOZE_CHANNEL_ID,
                    SNOOZE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Snooze notifications"
                    enableVibration(false)
                    setShowBadge(false)
                }

                systemNotificationManager.createNotificationChannel(alarmChannel)
                systemNotificationManager.createNotificationChannel(snoozeChannel)

                Log.d(TAG, "Notification channels created successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channels", e)
            }
        }
    }

    override fun showAlarmNotification(
        alarmId: Int,
        title: String,
        message: String,
        isVibrationEnabled: Boolean,
        isSoundEnabled: Boolean,
        isSnoozeEnabled: Boolean
    ) {
        // Add entry log
        Log.d(TAG, "=== SHOWING ALARM NOTIFICATION ===")
        Log.d(TAG, "Alarm ID: $alarmId")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Message: $message")
        Log.d(TAG, "Vibration: $isVibrationEnabled, Sound: $isSoundEnabled, Snooze: $isSnoozeEnabled")
        // Early permission check
        if (!canShowNotifications()) {
            Log.e(TAG, "Cannot show notifications - permission/settings issue")
            return
        }
        Log.d(TAG, "Permission checks passed - building notification...")

        try {
            val notification = buildAlarmNotification(
                alarmId, title, message,
                isVibrationEnabled, isSoundEnabled, isSnoozeEnabled
            )
            Log.d(TAG, "Notification built successfully - showing notification...")

            // Safe notification call with explicit permission check
            showNotificationSafely(alarmId, notification)
            Log.d(TAG, "=== NOTIFICATION PROCESS COMPLETE ===")

        } catch (e: Exception) {
            Log.e(TAG, "Error showing alarm notification", e)
            handleNotificationError()
        }
    }

    override fun cancelNotification(alarmId: Int) {
        try {
            notificationManager.cancel(alarmId)
            notificationManager.cancel(alarmId + 1000) // Cancel snooze notification too
            Log.d(TAG, "Notification cancelled for ID: $alarmId")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling notification", e)
        }
    }

    override fun showSnoozeNotification(
        alarmId: Int,
        title: String,
        snoozeTime: Long
    ) {
        if (!canShowNotifications()) {
            return
        }

        try {
            val notification = buildSnoozeNotification(alarmId, title, snoozeTime)
            showNotificationSafely(alarmId + 1000, notification)

        } catch (e: Exception) {
            Log.e(TAG, "Error showing snooze notification", e)
        }
    }

    /**
     * Check if we can show notifications (combines all permission checks)
     */
    private fun canShowNotifications(): Boolean {
        Log.d(TAG, "Checking notification permissions...")
        val hasPermission = hasNotificationPermission()
        val isEnabled = notificationManager.areNotificationsEnabled()
        Log.d(TAG, "Has notification permission: $hasPermission")
        Log.d(TAG, "Notifications enabled: $isEnabled")

        if (!hasPermission) {
            Log.w(TAG, "Notification permission not granted")
            showPermissionError()
            return false
        }

        if (!isEnabled) {
            Log.w(TAG, "Notifications are disabled for this app")
            showNotificationDisabledError()
            return false
        }

        return true
    }

    /**
     * Safely show notification with proper permission handling
     */
    private fun showNotificationSafely(notificationId: Int, notification: android.app.Notification) {

        Log.d(TAG, "Attempting to show notification ID: $notificationId")
        try {
            // Double-check permission right before notify call to satisfy IDE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, notification)
                    Log.d(TAG, "SUCCESS: Notification shown for ID: $notificationId")
                } else {
                    Log.e(TAG, "FAILED: Cannot show notification - permission check failed at notify time")
                }
            } else {
                // No runtime permission needed for older versions
                Log.d(TAG, "Android < 13 - showing notification without runtime permission")

                notificationManager.notify(notificationId, notification)
                Log.d(TAG, "SUCCESS: Notification shown for ID: $notificationId")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when showing notification", e)
            handleNotificationError()
        }
    }

    /**
     * Build alarm notification
     */
    private fun buildAlarmNotification(
        alarmId: Int,
        title: String,
        message: String,
        isVibrationEnabled: Boolean,
        isSoundEnabled: Boolean,
        isSnoozeEnabled: Boolean
    ): android.app.Notification {

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_alarm_notification", true)
            putExtra("alarm_id", alarmId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, alarmId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = "DISMISS_ALARM"
            putExtra("alarm_id", alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, alarmId * 10, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_enable)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_clock, "Dismiss", dismissPendingIntent)
            .setFullScreenIntent(contentPendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(0)

        if (isSnoozeEnabled) {
            val snoozeIntent = Intent(context, AlarmActionReceiver::class.java).apply {
                action = "SNOOZE_ALARM"
                putExtra("alarm_id", alarmId)
                putExtra("alarm_title", title)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context, alarmId * 10 + 1, snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent)
        }

        if (isSoundEnabled) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(alarmUri)
        }

        if (isVibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
        }

        return builder.build()
    }

    /**
     * Build snooze notification
     */
    private fun buildSnoozeNotification(
        alarmId: Int,
        title: String,
        snoozeTime: Long
    ): android.app.Notification {

        val snoozeText = "Snoozed until ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(snoozeTime))}"

        val cancelSnoozeIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = "CANCEL_SNOOZE"
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
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun showPermissionError() {
        try {
            Toast.makeText(
                context,
                "Notification permission required for alarms. Please enable in Settings.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.e(TAG, "Could not show permission error toast", e)
        }
    }

    private fun showNotificationDisabledError() {
        try {
            Toast.makeText(
                context,
                "Notifications are disabled. Please enable notifications for alarms to work.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.e(TAG, "Could not show notification disabled error toast", e)
        }
    }

    private fun handleNotificationError() {
        showPermissionError()
    }
}