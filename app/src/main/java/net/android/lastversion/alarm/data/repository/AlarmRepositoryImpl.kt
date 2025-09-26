package net.android.lastversion.alarm.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.android.lastversion.alarm.data.local.dao.AlarmDao
import net.android.lastversion.alarm.data.mapper.AlarmMapper
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.domain.repository.AlarmRepository

class AlarmRepositoryImpl(
    private val alarmDao: AlarmDao
) : AlarmRepository {

    override fun getAllAlarms(): Flow<List<Alarm>> {
        return alarmDao.getAllAlarms().map { entities ->
            AlarmMapper.entityListToDomain(entities)
        }
    }

    override fun getEnabledAlarms(): Flow<List<Alarm>> {
        return alarmDao.getEnabledAlarms().map { entities ->
            AlarmMapper.entityListToDomain(entities)
        }
    }

    override suspend fun getAlarmById(id: Int): Alarm? {
        return alarmDao.getAlarmById(id)?.let { entity ->
            AlarmMapper.entityToDomain(entity)
        }
    }

    override suspend fun insertAlarm(alarm: Alarm): Long {
        val entity = AlarmMapper.domainToEntity(alarm)
        return alarmDao.insertAlarm(entity)
    }

    override suspend fun updateAlarm(alarm: Alarm) {
        val entity = AlarmMapper.domainToEntity(alarm)
        alarmDao.updateAlarm(entity)
    }

    override suspend fun deleteAlarm(alarm: Alarm) {
        val entity = AlarmMapper.domainToEntity(alarm)
        alarmDao.deleteAlarm(entity)
    }

    override suspend fun deleteAlarmById(id: Int) {
        alarmDao.deleteAlarmById(id)
    }

    override suspend fun toggleAlarm(alarmId: Int) {
        alarmDao.toggleAlarm(alarmId)
    }
}