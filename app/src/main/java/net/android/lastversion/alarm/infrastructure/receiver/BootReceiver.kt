package net.android.lastversion.alarm.infrastructure.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.android.lastversion.alarm.data.database.AlarmDatabase
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmSchedulerImpl
import java.util.Calendar
import java.util.Date
import java.io.File
/**
 * BootReceiver - Reschedule all enabled alarms after device restart
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔥 BootReceiver.onReceive() ENTRY POINT")
        Log.d(TAG, "Context: $context")
        Log.d(TAG, "Intent: $intent")
        Log.d(TAG, "Action: ${intent.action}")
        //Log to both Logcat and file
        val message = "BootReceiver triggered at ${Date()}"
        Log.d(TAG, message)
        writeToFile(context, message)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            Log.d(TAG, "=== DEVICE BOOT COMPLETED - RESCHEDULING ALARMS ===")
            Log.d(TAG, "Action: ${intent.action}")

            // Reschedule all enabled alarms
            CoroutineScope(Dispatchers.IO).launch {
                rescheduleAllAlarms(context)
            }
        }
    }

    private fun writeToFile(context: Context, message: String) {
        try {
            val file = File(context.getExternalFilesDir(null), "boot_log.txt")
            file.appendText("${Date()}: $message\n")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to file", e)
        }
    }

    private fun logToPersistentStorage(context: Context, message: String) {
        val prefs = context.getSharedPreferences("boot_logs", Context.MODE_PRIVATE)
        val existingLogs = prefs.getString("logs", "")
        val newLog = "${Date()}: $message\n$existingLogs"

        prefs.edit()
            .putString("logs", newLog)
            .putLong("last_boot", System.currentTimeMillis())
            .apply()
    }


    private suspend fun rescheduleAllAlarms(context: Context) {
        try {
            Log.d(TAG, "Starting alarm rescheduling process...")

            val repository = AlarmRepositoryImpl(
                AlarmDatabase.getDatabase(context).alarmDao()
            )
            val alarmScheduler = AlarmSchedulerImpl(context)

            // Get all alarms from database
            repository.getAllAlarms().collect { alarmEntities ->
                Log.d(TAG, "Found ${alarmEntities.size} alarms in database")

                // Convert entities to domain models and filter enabled alarms
                val enabledAlarms = alarmEntities
                    .map { entity -> repository.convertToAlarmModel(entity) }
                    .filter { alarm -> alarm.isEnabled }

                Log.d(TAG, "Rescheduling ${enabledAlarms.size} enabled alarms")

                enabledAlarms.forEach { alarm ->
                    rescheduleAlarm(alarm, alarmScheduler)
                }

                Log.d(TAG, "=== ALARM RESCHEDULING COMPLETE ===")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling alarms after boot", e)
        }
    }

    /**
     * Schedule individual alarm after boot
     */
    private fun rescheduleAlarm(alarm: Alarm, alarmScheduler: AlarmSchedulerImpl) {
        try {
            Log.d(TAG, "Rescheduling alarm ${alarm.id}: ${alarm.hour}:${alarm.minute} ${alarm.amPm}")

            val nextTime = calculateNextAlarmTime(alarm)
            if (nextTime != null && nextTime > System.currentTimeMillis()) {
                alarmScheduler.scheduleAlarm(alarm.id, nextTime, alarm)
                Log.d(TAG, "Alarm ${alarm.id} rescheduled for ${java.util.Date(nextTime)}")
            } else {
                Log.w(TAG, "Invalid time calculated for alarm ${alarm.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling alarm ${alarm.id}", e)
        }
    }

    /**
     * Calculate next alarm time after boot
     */
    private fun calculateNextAlarmTime(alarm: Alarm): Long? {
        try {
            val now = Calendar.getInstance()
            val alarmCalendar = Calendar.getInstance()

            // Convert 12-hour to 24-hour format
            val hour24 = when {
                alarm.amPm == "AM" && alarm.hour == 12 -> 0
                alarm.amPm == "AM" -> alarm.hour
                alarm.amPm == "PM" && alarm.hour == 12 -> 12
                else -> alarm.hour + 12
            }

            alarmCalendar.apply {
                set(Calendar.HOUR_OF_DAY, hour24)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            Log.d(TAG, "Calculating time for ${alarm.hour}:${alarm.minute} ${alarm.amPm} -> ${hour24}:${alarm.minute}")

            // Check if alarm has recurring days
            val hasActiveDays = alarm.activeDays.any { it }

            if (!hasActiveDays) {
                // One-time alarm - schedule for today if not passed, otherwise tomorrow
                if (alarmCalendar.after(now)) {
                    Log.d(TAG, "One-time alarm scheduled for today")
                    return alarmCalendar.timeInMillis
                } else {
                    alarmCalendar.add(Calendar.DAY_OF_MONTH, 1)
                    Log.d(TAG, "One-time alarm scheduled for tomorrow")
                    return alarmCalendar.timeInMillis
                }
            } else {
                // Recurring alarm - find next valid day
                Log.d(TAG, "Finding next occurrence for recurring alarm")
                return findNextRecurringTime(alarmCalendar, now, alarm.activeDays)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating alarm time", e)
            return null
        }
    }

    /**
     * Find next valid time for recurring alarms
     */
    private fun findNextRecurringTime(
        alarmCalendar: Calendar,
        now: Calendar,
        activeDays: BooleanArray
    ): Long? {

        Log.d(TAG, "Active days: ${activeDays.contentToString()}")

        // Check up to 7 days from now
        for (dayOffset in 0..6) {
            val checkCalendar = alarmCalendar.clone() as Calendar
            checkCalendar.add(Calendar.DAY_OF_MONTH, dayOffset)

            // Get day of week (0 = Sunday, 1 = Monday, ..., 6 = Saturday)
            val dayOfWeek = checkCalendar.get(Calendar.DAY_OF_WEEK) - 1

            Log.d(TAG, "Checking day offset $dayOffset, dayOfWeek $dayOfWeek")

            // Check if this day is enabled
            if (activeDays[dayOfWeek]) {
                // If it's today, check if time hasn't passed
                if (dayOffset == 0) {
                    if (checkCalendar.after(now)) {
                        Log.d(TAG, "Recurring alarm scheduled for today")
                        return checkCalendar.timeInMillis
                    }
                } else {
                    // Future day is ok
                    Log.d(TAG, "Recurring alarm scheduled for day offset $dayOffset")
                    return checkCalendar.timeInMillis
                }
            }
        }

        Log.w(TAG, "No valid day found in next 7 days for recurring alarm")
        return null
    }
}