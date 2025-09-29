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
import net.android.lastversion.alarm.presentation.activity.AlarmRingingActivity


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

        // THAY ĐỔI: Đọc giá trị mới thay vì Boolean
        val snoozeMinutes = intent.getIntExtra("snooze_minutes", 5)
        val vibrationPattern = intent.getStringExtra("vibration_pattern") ?: "default"
        val soundType = intent.getStringExtra("sound_type") ?: "default"
        val isSilentModeEnabled = intent.getBooleanExtra("is_silent_mode_enabled", false)
        val soundUri = intent.getStringExtra("sound_uri") ?: ""
        val hour = intent.getIntExtra("alarm_hour", 0)
        val minute = intent.getIntExtra("alarm_minute", 0)
        val amPm = intent.getStringExtra("alarm_am_pm") ?: "AM"

        // Open AlarmRingingActivity
        val alarmIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("alarm_id", alarmId)
            putExtra("alarm_hour", hour)
            putExtra("alarm_minute", minute)
            putExtra("alarm_am_pm", amPm)
            putExtra("alarm_label", title)
            putExtra("alarm_note", note)
            putExtra("snooze_minutes", snoozeMinutes)
            putExtra("vibration_pattern", vibrationPattern)
            putExtra("sound_type", soundType)
            putExtra("is_silent_mode_enabled", isSilentModeEnabled)
            putExtra("sound_uri", soundUri)
        }
        context.startActivity(alarmIntent)

        // Show notification as backup
        val notificationManager = AlarmNotificationManager(context)
        notificationManager.showAlarmNotification(
            alarmId = alarmId,
            title = title,
            note = note,
            snoozeMinutes = snoozeMinutes,
            vibrationPattern = vibrationPattern,
            soundType = soundType,
            soundUri = soundUri,
            isSilentModeEnabled = isSilentModeEnabled
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