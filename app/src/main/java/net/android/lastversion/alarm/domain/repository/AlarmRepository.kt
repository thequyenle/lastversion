package net.android.lastversion.alarm.domain.repository

import kotlinx.coroutines.flow.Flow
import net.android.lastversion.alarm.domain.model.Alarm

interface AlarmRepository {
    fun getAllAlarms(): Flow<List<Alarm>>
    fun getEnabledAlarms(): Flow<List<Alarm>>
    suspend fun getAlarmById(id: Int): Alarm?
    suspend fun insertAlarm(alarm: Alarm): Long
    suspend fun updateAlarm(alarm: Alarm)
    suspend fun deleteAlarm(alarm: Alarm)
    suspend fun deleteAlarmById(id: Int)
    suspend fun toggleAlarm(alarmId: Int)
}