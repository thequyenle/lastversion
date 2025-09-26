package net.android.lastversion.alarm.data.mapper

import net.android.lastversion.alarm.data.local.entity.AlarmEntity
import net.android.lastversion.alarm.domain.model.Alarm

object AlarmMapper {

    fun entityToDomain(entity: AlarmEntity): Alarm {
        return Alarm(
            id = entity.id,
            hour = entity.hour,
            minute = entity.minute,
            amPm = entity.amPm,
            label = entity.label,
            activeDays = entity.activeDays.split(",").map { it.toBoolean() }.toBooleanArray(),
            isEnabled = entity.isEnabled,
            isSnoozeEnabled = entity.isSnoozeEnabled,
            isVibrationEnabled = entity.isVibrationEnabled,
            isSoundEnabled = entity.isSoundEnabled,
            isSilentModeEnabled = entity.isSilentModeEnabled,
            note = entity.note,
            soundUri = entity.soundUri,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun domainToEntity(alarm: Alarm): AlarmEntity {
        return AlarmEntity(
            id = alarm.id,
            hour = alarm.hour,
            minute = alarm.minute,
            amPm = alarm.amPm,
            label = alarm.label,
            activeDays = alarm.activeDays.joinToString(","),
            isEnabled = alarm.isEnabled,
            isSnoozeEnabled = alarm.isSnoozeEnabled,
            isVibrationEnabled = alarm.isVibrationEnabled,
            isSoundEnabled = alarm.isSoundEnabled,
            isSilentModeEnabled = alarm.isSilentModeEnabled,
            note = alarm.note,
            soundUri = alarm.soundUri,
            createdAt = alarm.createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun entityListToDomain(entities: List<AlarmEntity>): List<Alarm> {
        return entities.map { entityToDomain(it) }
    }

    fun domainListToEntity(alarms: List<Alarm>): List<AlarmEntity> {
        return alarms.map { domainToEntity(it) }
    }
}