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

            // THAY ĐỔI: isSnoozeEnabled → snoozeMinutes
            // THAY ĐỔI: Map 3 thuộc tính mới
            snoozeMinutes = entity.snoozeMinutes,
            vibrationPattern = entity.vibrationPattern,
            soundType = entity.soundType,
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

            // THAY ĐỔI: isSnoozeEnabled → snoozeMinutes
            snoozeMinutes = alarm.snoozeMinutes,
            vibrationPattern = alarm.vibrationPattern,
            soundType = alarm.soundType,
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