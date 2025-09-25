package net.android.lastversion.alarm.usecase

import kotlinx.coroutines.flow.Flow
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.domain.repository.AlarmRepository

class GetAllAlarmsUseCase(private val repository: AlarmRepository) {
    operator fun invoke(): Flow<List<Alarm>> {
        return repository.getAllAlarms()
    }
}