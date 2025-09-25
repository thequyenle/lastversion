package net.android.lastversion.alarm.usecase

import net.android.lastversion.alarm.model.Alarm
import net.android.lastversion.alarm.repository.AlarmRepository

class DeleteAlarmUseCase(private val repository: AlarmRepository) {
    suspend operator fun invoke(alarm: Alarm) {
        repository.deleteAlarm(alarm)
    }
}