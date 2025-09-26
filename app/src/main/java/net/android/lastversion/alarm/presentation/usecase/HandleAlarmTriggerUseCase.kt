// alarm/presentation/usecase/HandleAlarmTriggerUseCase.kt
package net.android.lastversion.alarm.presentation.usecase

import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.infrastructure.notification.NotificationManager
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmScheduler

/**
 * HandleAlarmTriggerUseCase - Business logic khi alarm được trigger
 *
 * Works directly với AlarmRepositoryImpl methods hiện tại
 */
class HandleAlarmTriggerUseCase(
    private val repository: AlarmRepositoryImpl, // Concrete implementation
    private val notificationManager: NotificationManager,
    private val alarmScheduler: AlarmScheduler
) {

    /**
     * Handle alarm trigger với alarm data từ intent
     */
    suspend operator fun invoke(
        alarmId: Int,
        label: String,
        note: String,
        isVibrationEnabled: Boolean,
        isSoundEnabled: Boolean,
        isSnoozeEnabled: Boolean
    ) {
        // Show immediate notification
        notificationManager.showAlarmNotification(
            alarmId = alarmId,
            title = label,
            message = if (note.isNotEmpty()) note else "Time to wake up!",
            isVibrationEnabled = isVibrationEnabled,
            isSoundEnabled = isSoundEnabled,
            isSnoozeEnabled = isSnoozeEnabled
        )

        // Get full alarm data từ database - use existing methods
        val alarmEntity = repository.getAlarmById(alarmId)
        if (alarmEntity != null) {
            val alarm = repository.convertToAlarmModel(alarmEntity)
            handleAlarmAfterTrigger(alarm)
        }
    }

    /**
     * Handle alarm trigger với full Alarm object
     */
    suspend operator fun invoke(alarm: Alarm) {
        // Show notification
        notificationManager.showAlarmNotification(
            alarmId = alarm.id,
            title = alarm.label,
            message = if (alarm.note.isNotEmpty()) alarm.note else "Time to wake up!",
            isVibrationEnabled = alarm.isVibrationEnabled,
            isSoundEnabled = alarm.isSoundEnabled,
            isSnoozeEnabled = alarm.isSnoozeEnabled
        )

        // Handle post-trigger logic
        handleAlarmAfterTrigger(alarm)
    }

    /**
     * Handle logic sau khi alarm đã trigger và notification đã show
     */
    private suspend fun handleAlarmAfterTrigger(alarm: Alarm) {
        val hasRecurringDays = alarm.activeDays.any { it }

        if (hasRecurringDays) {
            // Recurring alarm - schedule next occurrence
            scheduleNextRecurrence(alarm)
        } else {
            // One-time alarm - disable after trigger
            disableOneTimeAlarm(alarm)
        }

        // Optional: Log alarm trigger event
        logAlarmTrigger(alarm)
    }

    /**
     * Schedule next occurrence cho recurring alarm
     */
    private suspend fun scheduleNextRecurrence(alarm: Alarm) {
        try {
            // Simple next day scheduling - can be improved later
            val nextTime = calculateSimpleNextTime(alarm)
            if (nextTime != null) {
                alarmScheduler.scheduleAlarm(alarm.id, nextTime, alarm)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Calculate next occurrence time (simplified)
     */
    private fun calculateSimpleNextTime(alarm: Alarm): Long? {
        val calendar = java.util.Calendar.getInstance()

        val hour24 = when {
            alarm.amPm == "AM" && alarm.hour == 12 -> 0
            alarm.amPm == "AM" -> alarm.hour
            alarm.amPm == "PM" && alarm.hour == 12 -> 12
            else -> alarm.hour + 12
        }

        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour24)
        calendar.set(java.util.Calendar.MINUTE, alarm.minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        // Schedule for next day (simplified logic)
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)

        return calendar.timeInMillis
    }

    /**
     * Disable one-time alarm sau khi đã trigger
     */
    private suspend fun disableOneTimeAlarm(alarm: Alarm) {
        try {
            val disabledAlarm = alarm.copy(isEnabled = false)
            val disabledEntity = repository.convertToAlarmEntity(disabledAlarm)
            repository.updateAlarm(disabledEntity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Handle snooze action từ notification
     */
    suspend fun handleSnoozeAction(alarmId: Int, snoozeMinutes: Int = 5) {
        try {
            // Cancel current notification
            notificationManager.cancelNotification(alarmId)

            // Get alarm data - use existing methods
            val alarmEntity = repository.getAlarmById(alarmId) ?: return
            val alarm = repository.convertToAlarmModel(alarmEntity)

            // Calculate snooze time
            val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000)

            // Show snooze notification
            notificationManager.showSnoozeNotification(
                alarmId = alarmId,
                title = alarm.label,
                snoozeTime = snoozeTime
            )

            // Schedule snooze alarm with different ID to avoid conflicts
            alarmScheduler.scheduleAlarm(
                alarmId = alarmId + 10000,
                triggerTime = snoozeTime,
                alarm = alarm
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Handle dismiss action từ notification
     */
    suspend fun handleDismissAction(alarmId: Int) {
        try {
            // Cancel notification
            notificationManager.cancelNotification(alarmId)

            // Get alarm để check nếu cần reschedule recurring
            val alarmEntity = repository.getAlarmById(alarmId)
            if (alarmEntity != null) {
                val alarm = repository.convertToAlarmModel(alarmEntity)
                handleAlarmAfterTrigger(alarm)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Optional: Log alarm trigger event cho analytics
     */
    private fun logAlarmTrigger(alarm: Alarm) {
        try {
            val logData = mapOf(
                "alarm_id" to alarm.id,
                "alarm_time" to alarm.getTimeString(),
                "has_recurring_days" to alarm.activeDays.any { it },
                "is_snooze_enabled" to alarm.isSnoozeEnabled,
                "trigger_timestamp" to System.currentTimeMillis()
            )

            // Log event (implement based on your analytics platform)
            // Analytics.logEvent("alarm_triggered", logData)

        } catch (e: Exception) {
            // Ignore analytics errors
            e.printStackTrace()
        }
    }

    /**
     * Get readable description của alarm trigger
     */
    fun getAlarmTriggerDescription(alarm: Alarm): String {
        val recurringInfo = if (alarm.activeDays.any { it }) {
            "recurring (${alarm.activeDaysText})"
        } else {
            "one-time"
        }

        return "Alarm '${alarm.label}' triggered at ${alarm.getTimeString()} - $recurringInfo"
    }
}