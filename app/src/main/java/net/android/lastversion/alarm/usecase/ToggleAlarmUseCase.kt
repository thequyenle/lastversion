package net.android.lastversion.alarm.usecase

import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.domain.repository.AlarmRepository

class ToggleAlarmUseCase(private val repository: AlarmRepository) {
    suspend operator fun invoke(alarm: Alarm) {
        val toggledAlarm = alarm.copy(isEnabled = !alarm.isEnabled)
        repository.updateAlarm(toggledAlarm)
    }