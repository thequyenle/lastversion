// alarm/presentation/usecase/HandleAlarmTriggerUseCase.kt
package net.android.lastversion.alarm.presentation.usecase

import net.android.lastversion.alarm.domain.repository.AlarmRepository
import net.android.lastversion.alarm.infrastructure.notification.NotificationManager
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmScheduler

/**
 * HandleAlarmTriggerUseCase - Business logic khi alarm được trigger bởi Android system
 *
 * Responsibilities:
 * - Show notification với proper settings (sound, vibration, snooze)
 * - Reschedule recurring alarms cho lần kêu tiếp theo
 * - Handle one-time alarms (disable after trigger)
 * - Log alarm trigger for analytics (optional)
 */
class HandleAlarmTriggerUseCase(
    private val repository: AlarmRepository,
    private val notificationManager: NotificationManager,
    private val alarmScheduler: AlarmScheduler
) {

    /**
     * Handle alarm trigger với alarm data từ intent
     * Used khi AlarmReceiver trigger và chúng ta có basic alarm info
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

        // Get full alarm data từ database để handle recurring logic
        val alarm = repository.getAlarmById(alarmId)
        if (alarm != null) {
            handleAlarmAfterTrigger(alarm)
        }
    }

    /**
     * Handle alarm trigger với full Alarm object
     * Used khi chúng ta có complete alarm data
     */
    suspend operator fun invoke(alarm: net.android.lastversion.alarm.domain.model.Alarm) {
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
     *
     * @param alarm Complete alarm object từ database
     */
    private suspend fun handleAlarmAfterTrigger(alarm: net.android.lastversion.alarm.domain.model.Alarm) {
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
    private suspend fun scheduleNextRecurrence(alarm: net.android.lastversion.alarm.domain.model.Alarm) {
        try {
            // Use ScheduleAlarmUseCase để tính và schedule next occurrence
            val scheduleAlarmUseCase = ScheduleAlarmUseCase(repository, alarmScheduler)
            scheduleAlarmUseCase(alarm)
        } catch (e: Exception) {
            // Log error nhưng không crash app
            e.printStackTrace()
        }
    }

    /**
     * Disable one-time alarm sau khi đã trigger
     */
    private suspend fun disableOneTimeAlarm(alarm: net.android.lastversion.alarm.domain.model.Alarm) {
        try {
            val disabledAlarm = alarm.copy(isEnabled = false)
            repository.updateAlarm(disabledAlarm)
        } catch (e: Exception) {
            // Log error nhưng không crash app
            e.printStackTrace()
        }
    }

    /**
     * Handle snooze action từ notification
     *
     * @param alarmId ID của alarm được snooze
     * @param snoozeMinutes Số phút để snooze (default 5 minutes)
     */
    suspend fun handleSnoozeAction(alarmId: Int, snoozeMinutes: Int = 5) {
        try {
            // Cancel current notification
            notificationManager.cancelNotification(alarmId)

            // Get alarm data
            val alarm = repository.getAlarmById(alarmId) ?: return

            // Calculate snooze time
            val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000)

            // Show snooze notification
            notificationManager.showSnoozeNotification(
                alarmId = alarmId,
                title = alarm.label,
                snoozeTime = snoozeTime
            )

            // Schedule snooze alarm
            alarmScheduler.scheduleAlarm(
                alarmId = alarmId + 10000, // Different ID cho snooze để avoid conflict
                triggerTime = snoozeTime,
                alarm = alarm
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Handle dismiss action từ notification
     *
     * @param alarmId ID của alarm được dismiss
     */
    suspend fun handleDismissAction(alarmId: Int) {
        try {
            // Cancel notification
            notificationManager.cancelNotification(alarmId)

            // Get alarm để check nếu cần reschedule recurring
            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null) {
                handleAlarmAfterTrigger(alarm)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Optional: Log alarm trigger event cho analytics
     */
    private fun logAlarmTrigger(alarm: net.android.lastversion.alarm.domain.model.Alarm) {
        // Implementation tùy theo analytics platform bạn dùng
        // Ví dụ: Firebase Analytics, Crashlytics, etc.
        try {
            val logData = mapOf(
                "alarm_id" to alarm.id,
                "alarm_time" to alarm.getTimeString(),
                "has_recurring_days" to alarm.activeDays.any { it },
                "is_snooze_enabled" to alarm.isSnoozeEnabled,
                "trigger_timestamp" to System.currentTimeMillis()
            )

            // Log event
            // Analytics.logEvent("alarm_triggered", logData)

        } catch (e: Exception) {
            // Ignore analytics errors
            e.printStackTrace()
        }
    }

    /**
     * Get readable description của alarm trigger
     * Useful cho debugging hoặc logging
     */
    fun getAlarmTriggerDescription(alarm: net.android.lastversion.alarm.domain.model.Alarm): String {
        val recurringInfo = if (alarm.activeDays.any { it }) {
            "recurring (${alarm.activeDaysText})"
        } else {
            "one-time"
        }

        return "Alarm '${alarm.label}' triggered at ${alarm.getTimeString()} - $recurringInfo"
    }
}