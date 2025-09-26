package net.android.lastversion.alarm.infrastructure.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.android.lastversion.alarm.data.database.AlarmDatabase
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmSchedulerImpl

/**
 * BootReceiver - Reschedule all enabled alarms after device restart
 *
 * Android clears all alarms when device restarts, so we need to
 * reschedule all enabled alarms from database
 *
 * Works directly với AlarmRepositoryImpl methods hiện tại
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            // Reschedule all enabled alarms
            CoroutineScope(Dispatchers.IO).launch {
                rescheduleAllAlarms(context)
            }
        }
    }

    private suspend fun rescheduleAllAlarms(context: Context) {
        try {
            val repository = AlarmRepositoryImpl(
                AlarmDatabase.getDatabase(context).alarmDao()
            )
            val alarmScheduler = AlarmSchedulerImpl(context)

            // Get all alarms from database - use existing method
            repository.getAllAlarms().collect { alarmEntities ->
                // Convert entities to domain models và filter enabled alarms
                alarmEntities
                    .map { entity -> repository.convertToAlarmModel(entity) }
                    .filter { alarm -> alarm.isEnabled }
                    .forEach { alarm ->
                        // Schedule each enabled alarm
                        scheduleAlarm(alarm, alarmScheduler)
                    }
            }
        } catch (e: Exception) {
            // Log error but don't crash
            e.printStackTrace()
        }
    }

    /**
     * Schedule individual alarm - simplified logic
     */
    private fun scheduleAlarm(alarm: Alarm, alarmScheduler: AlarmSchedulerImpl) {
        try {
            val nextTime = calculateNextAlarmTime(alarm)
            if (nextTime != null) {
                alarmScheduler.scheduleAlarm(alarm.id, nextTime, alarm)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Calculate next alarm time - simplified version
     */
    private fun calculateNextAlarmTime(alarm: Alarm): Long? {
        val now = java.util.Calendar.getInstance()
        val alarmCalendar = java.util.Calendar.getInstance()

        // Convert 12-hour to 24-hour format
        val hour24 = when {
            alarm.amPm == "AM" && alarm.hour == 12 -> 0
            alarm.amPm == "AM" -> alarm.hour
            alarm.amPm == "PM" && alarm.hour == 12 -> 12
            else -> alarm.hour + 12
        }

        alarmCalendar.apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour24)
            set(java.util.Calendar.MINUTE, alarm.minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        // Check if alarm has recurring days
        val hasActiveDays = alarm.activeDays.any { it }

        if (!hasActiveDays) {
            // One-time alarm - schedule for today if not passed, otherwise tomorrow
            if (alarmCalendar.after(now)) {
                return alarmCalendar.timeInMillis
            } else {
                alarmCalendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
                return alarmCalendar.timeInMillis
            }
        } else {
            // Recurring alarm - find next valid day
            return findNextRecurringTime(alarmCalendar, now, alarm.activeDays)
        }
    }

    /**
     * Find next valid time cho recurring alarms
     */
    private fun findNextRecurringTime(
        alarmCalendar: java.util.Calendar,
        now: java.util.Calendar,
        activeDays: BooleanArray
    ): Long? {

        // Check up to 7 days from now
        for (dayOffset in 0..6) {
            val checkCalendar = alarmCalendar.clone() as java.util.Calendar
            checkCalendar.add(java.util.Calendar.DAY_OF_MONTH, dayOffset)

            // Get day of week (0 = Sunday, 1 = Monday, ..., 6 = Saturday)
            val dayOfWeek = checkCalendar.get(java.util.Calendar.DAY_OF_WEEK) - 1

            // Check if this day is enabled
            if (activeDays[dayOfWeek]) {
                // If it's today, check if time hasn't passed
                if (dayOffset == 0) {
                    if (checkCalendar.after(now)) {
                        return checkCalendar.timeInMillis
                    }
                } else {
                    // Future day is ok
                    return checkCalendar.timeInMillis
                }
            }
        }

        // No valid day found in next 7 days
        return null
    }
}