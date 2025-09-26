package net.android.lastversion.alarm.domain.usecase

import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.domain.repository.AlarmRepository

class SaveAlarmUseCase(private val repository: AlarmRepository) {
    suspend operator fun invoke(alarm: Alarm): Long {
        return if (alarm.id == 0) {
            repository.insertAlarm(alarm)
        } else {
            repository.updateAlarm(alarm)
            alarm.id.toLong()
        }
    }
}