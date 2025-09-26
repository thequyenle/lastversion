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

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "AlarmReceiver triggered")

        val alarmId = intent.getIntExtra("alarm_id", -1)
        if (alarmId == -1) {
            Log.e(TAG, "Invalid alarm ID")
            return
        }

        val title = intent.getStringExtra("alarm_label") ?: "Alarm"
        val note = intent.getStringExtra("alarm_note") ?: ""
        val isVibrationEnabled = intent.getBooleanExtra("is_vibration_enabled", true)
        val isSoundEnabled = intent.getBooleanExtra("is_sound_enabled", true)
        val isSnoozeEnabled = intent.getBooleanExtra("is_snooze_enabled", true)
        val soundUri = intent.getStringExtra("sound_uri") ?: ""

        // Show notification
        val notificationManager = AlarmNotificationManager(context)
        notificationManager.showAlarmNotification(
            alarmId, title, note, isVibrationEnabled,
            isSoundEnabled, isSnoozeEnabled, soundUri
        )

        // Handle post-trigger logic
        CoroutineScope(Dispatchers.IO).launch {
            handleAlarmTriggered(context, alarmId)
        }
    }

    private suspend fun handleAlarmTriggered(context: Context, alarmId: Int) {
        try {
            val repository = AlarmRepositoryImpl(
                AlarmDatabase.getDatabase(context).alarmDao()
            )
            val scheduler = AlarmSchedulerImpl(context)

            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null && alarm.hasRecurringDays()) {
                // Reschedule recurring alarm
                scheduler.scheduleAlarm(alarm)
                Log.d(TAG, "Recurring alarm ${alarm.id} rescheduled")
            } else if (alarm != null) {
                // Disable one-time alarm
                repository.updateAlarm(alarm.copy(isEnabled = false))
                Log.d(TAG, "One-time alarm ${alarm.id} disabled")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling alarm trigger", e)
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}