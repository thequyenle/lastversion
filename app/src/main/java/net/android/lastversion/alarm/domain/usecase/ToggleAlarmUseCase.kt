package net.android.lastversion.alarm.domain.usecase

import net.android.lastversion.alarm.domain.repository.AlarmRepository

class ToggleAlarmUseCase(private val repository: AlarmRepository) {
    suspend operator fun invoke(alarmId: Int) = repository.toggleAlarm(alarmId)
}