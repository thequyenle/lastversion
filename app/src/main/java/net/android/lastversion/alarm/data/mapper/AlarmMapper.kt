// net/android/lastversion/alarm/data/mapper/AlarmMapper.kt
package net.android.lastversion.alarm.data.mapper

import net.android.lastversion.alarm.data.database.AlarmEntity
import net.android.lastversion.alarm.domain.model.Alarm

/**
 * AlarmMapper - Chuyển đổi giữa Data Layer và Domain Layer
 *
 * Nhiệm vụ duy nhất: Entity ↔ Domain Model conversion
 * Giúp Domain Layer không phụ thuộc vào Room/Database implementation
 */
object AlarmMapper {

    /**
     * Chuyển đổi từ AlarmEntity (Database) sang Alarm (Domain Model)
     * Database → Business Logic
     */
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

    /**
     * Chuyển đổi từ Alarm (Domain Model) sang AlarmEntity (Database)
     * Business Logic → Database
     */
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

    /**
     * Chuyển đổi List<AlarmEntity> sang List<Alarm>
     * Hữu ích cho getAllAlarms() flows
     */
    fun entityListToDomainList(entities: List<AlarmEntity>): List<Alarm> {
        return entities.map { entity ->
            entityToDomain(entity)
        }
    }

    /**
     * Chuyển đổi List<Alarm> sang List<AlarmEntity>
     * Ít dùng nhưng có thể cần trong một số trường hợp
     */
    fun domainListToEntityList(alarms: List<Alarm>): List<AlarmEntity> {
        return alarms.map { alarm ->
            domainToEntity(alarm)
        }
    }
}