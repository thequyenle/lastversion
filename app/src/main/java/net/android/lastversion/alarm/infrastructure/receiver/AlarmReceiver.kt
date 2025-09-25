package net.android.lastversion.alarm.infrastructure.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.android.lastversion.alarm.data.database.AlarmDatabase
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.infrastructure.notification.NotificationManagerImpl
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmSchedulerImpl
import net.android.lastversion.alarm.presentation.usecase.HandleAlarmTriggerUseCase

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        if (alarmId == -1) return

        // Extract alarm details từ intent
        val alarmHour = intent.getIntExtra("alarm_hour", 0)
        val alarmMinute = intent.getIntExtra("alarm_minute", 0)
        val alarmLabel = intent.getStringExtra("alarm_label") ?: "Alarm"
        val alarmNote = intent.getStringExtra("alarm_note") ?: ""
        val isVibrationEnabled = intent.getBooleanExtra("is_vibration_enabled", true)
        val isSoundEnabled = intent.getBooleanExtra("is_sound_enabled", true)
        val isSnoozeEnabled = intent.getBooleanExtra("is_snooze_enabled", true)

        // Handle alarm trigger
        CoroutineScope(Dispatchers.IO).launch {
            val repository = AlarmRepositoryImpl(
                AlarmDatabase.getDatabase(context).alarmDao()
            )
            val notificationManager = NotificationManagerImpl(context)
            val alarmScheduler = AlarmSchedulerImpl(context) // ← Thêm dòng này

            val handleAlarmTriggerUseCase = HandleAlarmTriggerUseCase(
                repository,
                notificationManager,
                alarmScheduler  // ← Thêm parameter này
            )

            handleAlarmTriggerUseCase(
                alarmId = alarmId,
                label = alarmLabel,
                note = alarmNote,
                isVibrationEnabled = isVibrationEnabled,
                isSoundEnabled = isSoundEnabled,
                isSnoozeEnabled = isSnoozeEnabled
            )
        }
    }
}