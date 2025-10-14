package net.android.lastversion.alarm.infrastructure.receiver

import android.Manifest
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.android.lastversion.R
import net.android.lastversion.alarm.data.local.database.AlarmDatabase
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.infrastructure.notification.AlarmNotificationManager
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmSchedulerImpl

class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "onReceive CALLED")
        Log.d(TAG, "Action: ${intent.action}")
        Log.d(TAG, "========================================")

        val alarmId = intent.getIntExtra("alarm_id", -1)

        if (alarmId == -1) {
            Log.e(TAG, "❌ ERROR: Invalid alarm ID = -1")
            return
        }

        Log.d(TAG, "✅ Alarm ID: $alarmId")

        val notificationManager = AlarmNotificationManager(context)

        when (intent.action) {
            AlarmNotificationManager.ACTION_DISMISS -> {
                Log.d(TAG, "🔴 ACTION_DISMISS received for alarm $alarmId")

                // 🔊 Stop immediate sound from AlarmReceiver
                AlarmReceiver.stopImmediateSoundAndVibrationStatic()

                notificationManager.cancelNotification(alarmId)

                // ✅ FIX: Xử lý disable/reschedule alarm SAU KHI dismiss
                CoroutineScope(Dispatchers.IO).launch {
                    handleAlarmDismissed(context, alarmId)
                }

                Log.d(TAG, "✅ Alarm $alarmId dismissed successfully")
            }

            AlarmNotificationManager.ACTION_SNOOZE -> {
                Log.d(TAG, "😴 ACTION_SNOOZE received for alarm $alarmId")

                // 🔊 Stop immediate sound from AlarmReceiver
                AlarmReceiver.stopImmediateSoundAndVibrationStatic()

                handleSnooze(context, alarmId, intent)
            }

            AlarmNotificationManager.ACTION_STOP_SNOOZE_SOUND -> {
                Log.d(TAG, "🛑 ACTION_STOP_SNOOZE_SOUND received for alarm $alarmId")

                // 🔊 Stop immediate sound from AlarmReceiver (snooze alarm)
                AlarmReceiver.stopImmediateSoundAndVibrationStatic()

                // 🗑️ Cancel the snooze alarm completely
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val scheduler = AlarmSchedulerImpl(context)
                        scheduler.cancelAlarm(alarmId) // Cancel snooze alarm

                        // Also cancel notification
                        notificationManager.cancelNotification(alarmId)

                        Log.d(TAG, "✅ Snooze alarm completely cancelled for alarm $alarmId")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error cancelling snooze alarm", e)
                    }
                }
            }

            AlarmNotificationManager.ACTION_CANCEL_SNOOZE -> {
                Log.d(TAG, "❌ ACTION_CANCEL_SNOOZE received for alarm $alarmId")
                handleCancelSnooze(context, alarmId)
            }

            else -> {
                Log.w(TAG, "⚠️ UNKNOWN action: ${intent.action}")
            }
        }
    }

    //  THÊM: Xử lý logic sau khi dismiss
    private suspend fun handleAlarmDismissed(context: Context, alarmId: Int) {
        try {
            if (alarmId == 0) {
                Log.d(TAG, "Preview mode - skipping post-dismiss logic")
                return
            }

            val repository = AlarmRepositoryImpl(
                AlarmDatabase.getDatabase(context).alarmDao()
            )
            val scheduler = AlarmSchedulerImpl(context)

            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null && alarm.hasRecurringDays()) {
                // Reschedule recurring alarm
                scheduler.scheduleAlarm(alarm)
                Log.d(TAG, "✅ Recurring alarm ${alarm.id} rescheduled after dismiss")
            } else if (alarm != null) {
                // Disable one-time alarm
                repository.updateAlarm(alarm.copy(isEnabled = false))
                Log.d(TAG, "✅ One-time alarm ${alarm.id} disabled after dismiss")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling alarm dismiss", e)
        }
    }

    private fun handleSnooze(context: Context, alarmId: Int, intent: Intent) {
        Log.d(TAG, "----------------------------------------")
        Log.d(TAG, "handleSnooze() STARTED")
        Log.d(TAG, "Alarm ID: $alarmId")

        val notificationManager = AlarmNotificationManager(context)
        notificationManager.cancelNotification(alarmId)
        // Also cancel the status notification if it exists
        try {
            val nm = NotificationManagerCompat.from(context)
            nm.cancel(alarmId + 20000)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling status notification", e)
        }
        Log.d(TAG, "✅ Notification cancelled")

        val title = intent.getStringExtra("alarm_title") ?: "Alarm"
        val snoozeMinutes = intent.getIntExtra("snooze_minutes", 5)
        val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Snooze minutes: $snoozeMinutes")
        Log.d(TAG, "Snooze time: ${java.util.Date(snoozeTime)}")

        // Show simple snooze status notification (non-interactive)
        showSnoozeStatusNotification(context, alarmId, title, snoozeTime)
        Log.d(TAG, "✅ Snooze status notification shown")

        // Schedule snooze alarm
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "📅 Starting to schedule snooze alarm...")

                val repository = AlarmRepositoryImpl(
                    AlarmDatabase.getDatabase(context).alarmDao()
                )
                val scheduler = AlarmSchedulerImpl(context)

                val alarm = repository.getAlarmById(alarmId)

                if (alarm == null) {
                    Log.e(TAG, "❌ ERROR: Alarm $alarmId NOT FOUND in database!")
                    return@launch
                }

                Log.d(TAG, "✅ Found alarm in DB: ${alarm.label}")

                // Tính giờ phút mới cho snooze - SỬA LỖI CRITICAL
                val snoozeCalendar = java.util.Calendar.getInstance().apply {
                    timeInMillis = snoozeTime
                }

                val snoozeHour24 = snoozeCalendar.get(java.util.Calendar.HOUR_OF_DAY)
                val snoozeMinute = snoozeCalendar.get(java.util.Calendar.MINUTE)

                // Chuyển đổi sang 12-hour format cho display
                val displayHour = when {
                    snoozeHour24 == 0 -> 12
                    snoozeHour24 > 12 -> snoozeHour24 - 12
                    else -> snoozeHour24
                }
                val snoozeAmPm = if (snoozeHour24 < 12) "AM" else "PM"

                Log.d(TAG, "⏰ Snooze alarm will ring at: $displayHour:${String.format("%02d", snoozeMinute)} $snoozeAmPm (24h: $snoozeHour24)")

                // ✅ CRITICAL FIX: Tạo snooze alarm với trigger time trực tiếp
                val snoozeAlarm = alarm.copy(
                    id = alarmId + 50000,
                    hour = displayHour,
                    minute = snoozeMinute,
                    amPm = snoozeAmPm,
                    activeDays = BooleanArray(7) { false },
                    isEnabled = true
                )

                Log.d(TAG, "✅ Snooze alarm object created with ID: ${snoozeAlarm.id}")

                // ✅ CRITICAL FIX: Use direct snooze scheduling instead of getNextTriggerTime()
                scheduler.scheduleSnoozeAlarm(snoozeAlarm, snoozeTime)

                //  FIX: SAU KHI schedule snooze xong, xử lý alarm gốc
                // Nếu là recurring alarm → reschedule
                // Nếu là one-time alarm → disable
                if (alarm.hasRecurringDays()) {
                    scheduler.scheduleAlarm(alarm)
                    Log.d(TAG, "✅ Recurring alarm ${alarm.id} rescheduled after snooze")
                } else {
                    repository.updateAlarm(alarm.copy(isEnabled = false))
                    Log.d(TAG, "✅ One-time alarm ${alarm.id} disabled after snooze")
                }

                Log.d(TAG, "🎉 SUCCESS! Alarm $alarmId snoozed for $snoozeMinutes minutes")
                Log.d(TAG, "Will ring at: ${java.util.Date(snoozeTime)}")
                Log.d(TAG, "----------------------------------------")

            } catch (e: Exception) {
                Log.e(TAG, "❌❌❌ EXCEPTION in handleSnooze:", e)
                Log.e(TAG, "Error message: ${e.message}")
                Log.e(TAG, "Stack trace:", e)
            }
        }
    }

    private fun handleCancelSnooze(context: Context, alarmId: Int) {
        Log.d(TAG, "----------------------------------------")
        Log.d(TAG, "handleCancelSnooze() STARTED")
        Log.d(TAG, "Alarm ID: $alarmId")

        val notificationManager = AlarmNotificationManager(context)
        notificationManager.cancelNotification(alarmId)
        // Also cancel the status notification if it exists
        try {
            val nm = NotificationManagerCompat.from(context)
            nm.cancel(alarmId + 20000)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling status notification", e)
        }
        Log.d(TAG, "✅ Notification cancelled")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = AlarmSchedulerImpl(context)
                // Cancel both the snooze alarm and its notification
                scheduler.cancelAlarm(alarmId + 50000)
                notificationManager.cancelNotification(alarmId + 50000)
                Log.d(TAG, "✅ Snooze alarm ${alarmId + 50000} cancelled successfully")
                Log.d(TAG, "----------------------------------------")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cancelling snooze:", e)
            }
        }
    }

    /**
     * Show simple snooze status notification (non-interactive)
     * This shows when user first snoozes the alarm
     */
    private fun showSnoozeStatusNotification(context: Context, alarmId: Int, title: String, snoozeTime: Long) {
        try {
            val snoozeText = "Snoozed until ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(snoozeTime))}"

            val notification = NotificationCompat.Builder(context, "snooze_channel")
                .setSmallIcon(R.drawable.ic_snooze)
                .setContentTitle(title)
                .setContentText(snoozeText)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(false)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setWhen(snoozeTime)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(alarmId + 20000, notification) // Different ID to avoid conflict
                Log.d(TAG, "✅ Snooze status notification shown with ID: ${alarmId + 20000}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing snooze status notification", e)
        }
    }

    companion object {
        private const val TAG = "AlarmActionReceiver"
    }
}