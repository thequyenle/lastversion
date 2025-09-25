package net.android.lastversion.alarm.usecase

import kotlinx.coroutines.flow.Flow
import net.android.lastversion.alarm.model.Alarm
import net.android.lastversion.alarm.repository.AlarmRepository

class GetAllAlarmsUseCase(private val repository: AlarmRepository) {
    operator fun invoke(): Flow<List<Alarm>> {
        return repository.getAllAlarms()
    }
}