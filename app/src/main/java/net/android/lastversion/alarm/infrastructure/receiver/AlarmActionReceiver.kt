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
        val alarmId = intent.getIntExtra("alarm_id", -1)
        if (alarmId == -1) return

        val notificationManager = AlarmNotificationManager(context)

        when (intent.action) {
            AlarmNotificationManager.ACTION_DISMISS -> {
                notificationManager.cancelNotification(alarmId)
                Log.d(TAG, "Alarm $alarmId dismissed")
            }

            AlarmNotificationManager.ACTION_SNOOZE -> {
                handleSnooze(context, alarmId, intent)
            }

            AlarmNotificationManager.ACTION_CANCEL_SNOOZE -> {
                handleCancelSnooze(context, alarmId)
            }
        }
    }

    private fun handleSnooze(context: Context, alarmId: Int, intent: Intent) {
        val notificationManager = AlarmNotificationManager(context)
        notificationManager.cancelNotification(alarmId)

        val title = intent.getStringExtra("alarm_title") ?: "Alarm"
        // THAY ĐỔI: Đọc snoozeMinutes từ intent thay vì hardcode
        val snoozeMinutes = intent.getIntExtra("snooze_minutes", 5)
        val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        // Show snooze notification
        notificationManager.showSnoozeNotification(alarmId, title, snoozeTime)

        // Schedule snooze alarm
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = AlarmRepositoryImpl(
                    AlarmDatabase.getDatabase(context).alarmDao()
                )
                val scheduler = AlarmSchedulerImpl(context)

                val alarm = repository.getAlarmById(alarmId)
                if (alarm != null) {
                    // Create temporary snooze alarm
                    val snoozeAlarm = alarm.copy(
                        id = alarmId + 50000, // Offset to avoid conflicts
                        activeDays = BooleanArray(7) { false } // One-time alarm
                    )
                    scheduler.scheduleAlarm(snoozeAlarm)
                    Log.d(TAG, "Alarm $alarmId snoozed for $snoozeMinutes minutes until ${java.util.Date(snoozeTime)}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling snooze", e)
            }
        }
    }

    private fun handleCancelSnooze(context: Context, alarmId: Int) {
        val notificationManager = AlarmNotificationManager(context)
        notificationManager.cancelNotification(alarmId)

        // Cancel the snooze alarm
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = AlarmSchedulerImpl(context)
                scheduler.cancelAlarm(alarmId + 50000)
                Log.d(TAG, "Snooze cancelled for alarm $alarmId")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling snooze", e)
            }
        }
    }

    companion object {
        private const val TAG = "AlarmActionReceiver"
    }
}