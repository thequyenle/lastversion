package net.android.lastversion.alarm.infrastructure.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
                notificationManager.cancelNotification(alarmId)

                // ✅ FIX: Xử lý disable/reschedule alarm SAU KHI dismiss
                CoroutineScope(Dispatchers.IO).launch {
                    handleAlarmDismissed(context, alarmId)
                }

                Log.d(TAG, "✅ Alarm $alarmId dismissed successfully")
            }

            AlarmNotificationManager.ACTION_SNOOZE -> {
                Log.d(TAG, "😴 ACTION_SNOOZE received for alarm $alarmId")
                handleSnooze(context, alarmId, intent)
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
        Log.d(TAG, "✅ Notification cancelled")

        val title = intent.getStringExtra("alarm_title") ?: "Alarm"
        val snoozeMinutes = intent.getIntExtra("snooze_minutes", 5)
        val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Snooze minutes: $snoozeMinutes")
        Log.d(TAG, "Snooze time: ${java.util.Date(snoozeTime)}")

        // Show snooze notification
        notificationManager.showSnoozeNotification(alarmId, title, snoozeTime)
        Log.d(TAG, "✅ Snooze notification shown")

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

                // Tính giờ phút mới cho snooze
                val snoozeCalendar = java.util.Calendar.getInstance().apply {
                    timeInMillis = snoozeTime
                }

                val snoozeHour12 = snoozeCalendar.get(java.util.Calendar.HOUR)
                val snoozeMinute = snoozeCalendar.get(java.util.Calendar.MINUTE)
                val snoozeAmPm = if (snoozeCalendar.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "AM" else "PM"

                val displayHour = if (snoozeHour12 == 0) 12 else snoozeHour12

                Log.d(TAG, "⏰ Snooze alarm will ring at: $displayHour:${String.format("%02d", snoozeMinute)} $snoozeAmPm")

                val snoozeAlarm = alarm.copy(
                    id = alarmId + 50000,
                    hour = displayHour,
                    minute = snoozeMinute,
                    amPm = snoozeAmPm,
                    activeDays = BooleanArray(7) { false },
                    isEnabled = true  // ✅ FIX: Đảm bảo snooze alarm được ENABLE
                )

                Log.d(TAG, "✅ Snooze alarm object created with ID: ${snoozeAlarm.id}")

                scheduler.scheduleAlarm(snoozeAlarm)

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
        Log.d(TAG, "✅ Notification cancelled")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = AlarmSchedulerImpl(context)
                scheduler.cancelAlarm(alarmId + 50000)
                Log.d(TAG, "✅ Snooze alarm ${alarmId + 50000} cancelled successfully")
                Log.d(TAG, "----------------------------------------")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cancelling snooze:", e)
            }
        }
    }

    companion object {
        private const val TAG = "AlarmActionReceiver"
    }
}