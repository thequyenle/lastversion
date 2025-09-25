package net.android.lastversion.alarm.usecase

import net.android.lastversion.alarm.model.Alarm
import net.android.lastversion.alarm.repository.AlarmRepository

class InsertAlarmUseCase(private val repository: AlarmRepository) {
    suspend operator fun invoke(alarm: Alarm): Long {
        return repository.insertAlarm(alarm)
    }
}