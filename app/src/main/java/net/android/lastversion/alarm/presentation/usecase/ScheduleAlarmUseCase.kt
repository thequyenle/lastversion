package net.android.lastversion.alarm.presentation.usecase

import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.domain.repository.AlarmRepository
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmScheduler
import net.android.lastversion.alarm.util.TimeUtils
import java.util.*

/**
 * ScheduleAlarmUseCase - Business logic để schedule alarm với Android system
 *
 * Responsibilities:
 * - Calculate next alarm time based on current time và activeDays
 * - Schedule với AlarmScheduler nếu alarm enabled
 * - Cancel existing alarm nếu alarm disabled
 * - Handle one-time và recurring alarms
 */
class ScheduleAlarmUseCase(
    private val repository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {

    suspend operator fun invoke(alarm: Alarm) {
        if (!alarm.isEnabled) {
            // Cancel alarm nếu disabled
            alarmScheduler.cancelAlarm(alarm.id)
            return
        }

        // Calculate next alarm time
        val nextAlarmTime = calculateNextAlarmTime(alarm)

        if (nextAlarmTime != null) {
            // Schedule alarm với Android system
            alarmScheduler.scheduleAlarm(alarm.id, nextAlarmTime, alarm)
        } else {
            // No valid next time found, cancel existing alarm
            alarmScheduler.cancelAlarm(alarm.id)
        }
    }

    /**
     * Calculate next alarm time based on alarm settings và current time
     *
     * @param alarm Alarm object với time và activeDays settings
     * @return Next alarm time in milliseconds, null nếu không tìm được valid time
     */
    private fun calculateNextAlarmTime(alarm: Alarm): Long? {
        val now = Calendar.getInstance()
        val alarmCalendar = Calendar.getInstance()

        // Convert 12-hour format to 24-hour format
        val (hour24, minute24) = TimeUtils.convertTo24HourFormat(
            alarm.hour,
            alarm.minute,
            alarm.amPm
        )

        // Set alarm time for today
        alarmCalendar.apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute24)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Check nếu có active days được set
        val hasActiveDays = alarm.activeDays.any { it }

        if (!hasActiveDays) {
            // One-time alarm - schedule for today nếu chưa qua, otherwise tomorrow
            if (alarmCalendar.after(now)) {
                return alarmCalendar.timeInMillis
            } else {
                // Schedule for tomorrow
                alarmCalendar.add(Calendar.DAY_OF_MONTH, 1)
                return alarmCalendar.timeInMillis
            }
        } else {
            // Recurring alarm - find next valid day
            return findNextRecurringAlarmTime(alarmCalendar, now, alarm.activeDays)
        }
    }

    /**
     * Find next valid alarm time cho recurring alarms
     *
     * @param alarmCalendar Calendar object với alarm time
     * @param now Current time
     * @param activeDays Boolean array [Sun, Mon, Tue, Wed, Thu, Fri, Sat]
     * @return Next valid alarm time, null nếu không tìm được trong 7 ngày tới
     */
    private fun findNextRecurringAlarmTime(
        alarmCalendar: Calendar,
        now: Calendar,
        activeDays: BooleanArray
    ): Long? {

        // Check up to 7 days từ now
        for (dayOffset in 0..6) {
            val checkCalendar = alarmCalendar.clone() as Calendar
            checkCalendar.add(Calendar.DAY_OF_MONTH, dayOffset)

            // Get day of week (0 = Sunday, 1 = Monday, ..., 6 = Saturday)
            val dayOfWeek = checkCalendar.get(Calendar.DAY_OF_WEEK) - 1

            // Check nếu day này enabled trong activeDays
            if (activeDays[dayOfWeek]) {
                // Nếu là hôm nay, check nếu time chưa qua
                if (dayOffset == 0) {
                    if (checkCalendar.after(now)) {
                        return checkCalendar.timeInMillis
                    }
                } else {
                    // Ngày khác thì OK
                    return checkCalendar.timeInMillis
                }
            }
        }

        // Không tìm được valid day trong 7 ngày tới
        return null
    }

    /**
     * Get human-readable description của next alarm time
     * Useful cho debugging hoặc UI display
     */
    fun getNextAlarmDescription(alarm: Alarm): String? {
        val nextTime = calculateNextAlarmTime(alarm) ?: return null

        val nextCalendar = Calendar.getInstance().apply {
            timeInMillis = nextTime
        }

        val now = Calendar.getInstance()
        val daysDiff = ((nextTime - now.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

        return when {
            daysDiff == 0 -> "Today at ${alarm.getTimeString()}"
            daysDiff == 1 -> "Tomorrow at ${alarm.getTimeString()}"
            daysDiff <= 7 -> {
                val dayName = getDayName(nextCalendar.get(Calendar.DAY_OF_WEEK))
                "$dayName at ${alarm.getTimeString()}"
            }
            else -> {
                val dateFormat = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                "${dateFormat.format(nextCalendar.time)} at ${alarm.getTimeString()}"
            }
        }
    }

    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Unknown"
        }
    }
}