
package net.android.lastversion.alarm.data.mapper

import net.android.lastversion.alarm.data.database.AlarmEntity
import net.android.lastversion.alarm.domain.model.Alarm

object AlarmMapper {

    fun entityToDomain(entity: AlarmEntity): Alarm {
        // Parse activeDays string thành BooleanArray
        val activeDaysArray = entity.activeDays.split(",")
            .map { it.toBoolean() }
            .toBooleanArray()

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
    fun domainToEntity(alarm: Alarm): AlarmEntity {
        // Convert BooleanArray thành string để lưu database
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
    fun entityListToDomainList(entities: List<AlarmEntity>): List<Alarm> {
        return entities.map { entity ->
            entityToDomain(entity)
        }
    }

    fun domainListToEntityList(alarms: List<Alarm>): List<AlarmEntity> {
        return alarms.map { alarm ->
            domainToEntity(alarm)
        }
    }
}