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
            "ACTION_DISMISS" -> {
                notificationManager.cancelNotification(alarmId)
                Log.d(TAG, "Alarm $alarmId dismissed")
            }

            "ACTION_SNOOZE" -> {
                handleSnooze(context, alarmId, intent)
            }
        }
    }

    private fun handleSnooze(context: Context, alarmId: Int, intent: Intent) {
        val notificationManager = AlarmNotificationManager(context)
        notificationManager.cancelNotification(alarmId)

        val title = intent.getStringExtra("alarm_title") ?: "Alarm"
        val snoozeTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes

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
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling snooze", e)
            }
        }

        Log.d(TAG, "Alarm $alarmId snoozed until ${java.util.Date(snoozeTime)}")
    }

    companion object {
        private const val TAG = "AlarmActionReceiver"
    }
}