package net.android.lastversion.alarm.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.domain.repository.AlarmRepository

class GetAlarmsUseCase(private val repository: AlarmRepository) {
    operator fun invoke(): Flow<List<Alarm>> = repository.getAllAlarms()
}