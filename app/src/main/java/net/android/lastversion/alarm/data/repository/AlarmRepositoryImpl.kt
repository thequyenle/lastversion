package net.android.lastversion.alarm.data.repository



import kotlinx.coroutines.flow.Flow
import net.android.lastversion.alarm.data.database.AlarmDao
import net.android.lastversion.alarm.data.database.AlarmEntity
import net.android.lastversion.alarm.domain.model.Alarm

class AlarmRepositoryImpl(private val alarmDao: AlarmDao) {

    fun getAllAlarms(): Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    suspend fun insertAlarm(alarm: AlarmEntity): Long = alarmDao.insertAlarm(alarm)

    suspend fun updateAlarm(alarm: AlarmEntity) = alarmDao.updateAlarm(alarm)

    suspend fun deleteAlarm(alarm: AlarmEntity) = alarmDao.deleteAlarm(alarm)

    suspend fun deleteAlarmById(id: Int) = alarmDao.deleteAlarmById(id)

    suspend fun getAlarmById(id: Int): AlarmEntity? = alarmDao.getAlarmById(id)

    fun getEnabledAlarms(): Flow<List<AlarmEntity>> = alarmDao.getEnabledAlarms()

    // Chuyển đổi từ AlarmEntity sang Alarm model
    fun convertToAlarmModel(entity: AlarmEntity): Alarm {
        val activeDaysArray = entity.activeDays.split(",").map { it.toBoolean() }.toBooleanArray()

        return Alarm(
            id = entity.id,
            hour = entity.hour,
            minute = entity.minute,
            amPm = entity.amPm,
            label = entity.label,
            activeDays = activeDaysArray,
            activeDaysText = entity.activeDaysText,
            isEnabled = entity.isEnabled,
            isSnoozeEnabled = entity.isSnoozeEnabled,
            isVibrationEnabled = entity.isVibrationEnabled,
            isSoundEnabled = entity.isSoundEnabled,
            isSilentModeEnabled = entity.isSilentModeEnabled,
            note = entity.note
        )
    }

    // Chuyển đổi từ Alarm model sang AlarmEntity
    fun convertToAlarmEntity(alarm: Alarm): AlarmEntity {
        val activeDaysString = alarm.activeDays.joinToString(",")

        return AlarmEntity(
            id = alarm.id,
            hour = alarm.hour,
            minute = alarm.minute,
            amPm = alarm.amPm,
            label = alarm.label,
            activeDays = activeDaysString,
            activeDaysText = alarm.activeDaysText,
            isEnabled = alarm.isEnabled,
            isSnoozeEnabled = alarm.isSnoozeEnabled,
            isVibrationEnabled = alarm.isVibrationEnabled,
            isSoundEnabled = alarm.isSoundEnabled,
            isSilentModeEnabled = alarm.isSilentModeEnabled,
            note = alarm.note
        )
    }
}