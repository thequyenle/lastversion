package net.android.lastversion.alarm.infrastructure.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.infrastructure.receiver.AlarmReceiver

class AlarmSchedulerImpl(private val context: Context) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleAlarm(alarm: Alarm) {
        if (!alarm.isEnabled) {
            Log.d(TAG, "Alarm ${alarm.id} is disabled, skipping schedule")
            return
        }

        val triggerTime = alarm.getNextTriggerTime()

        if (triggerTime <= System.currentTimeMillis()) {
            Log.w(TAG, "Trigger time is in the past for alarm ${alarm.id}")
            return
        }

        // Check exact alarm permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Cannot schedule exact alarms - permission denied")
                return
            }
        }

        try {
            val intent = createAlarmIntent(alarm)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Use setAlarmClock for exact timing and lock screen display
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

            Log.d(TAG, "Alarm ${alarm.id} scheduled for ${java.util.Date(triggerTime)}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm ${alarm.id}", e)
        }
    }

    override fun cancelAlarm(alarmId: Int) {
        try {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
                Log.d(TAG, "Alarm $alarmId cancelled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel alarm $alarmId", e)
        }
    }

    override fun rescheduleAllAlarms(alarms: List<Alarm>) {
        Log.d(TAG, "Rescheduling ${alarms.size} alarms")
        alarms.filter { it.isEnabled }.forEach { alarm ->
            scheduleAlarm(alarm)
        }
    }

    private fun createAlarmIntent(alarm: Alarm): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_HOUR, alarm.hour)
            putExtra(EXTRA_ALARM_MINUTE, alarm.minute)
            putExtra(EXTRA_ALARM_AM_PM, alarm.amPm)
            putExtra(EXTRA_ALARM_LABEL, alarm.label)
            putExtra(EXTRA_ALARM_NOTE, alarm.note)
            putExtra(EXTRA_IS_SNOOZE_ENABLED, alarm.isSnoozeEnabled)
            putExtra(EXTRA_IS_VIBRATION_ENABLED, alarm.isVibrationEnabled)
            putExtra(EXTRA_IS_SOUND_ENABLED, alarm.isSoundEnabled)
            putExtra(EXTRA_IS_SILENT_MODE_ENABLED, alarm.isSilentModeEnabled)
            putExtra(EXTRA_SOUND_URI, alarm.soundUri)
        }
    }

    companion object {
        private const val TAG = "AlarmScheduler"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_HOUR = "alarm_hour"
        const val EXTRA_ALARM_MINUTE = "alarm_minute"
        const val EXTRA_ALARM_AM_PM = "alarm_am_pm"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_ALARM_NOTE = "alarm_note"
        const val EXTRA_IS_SNOOZE_ENABLED = "is_snooze_enabled"
        const val EXTRA_IS_VIBRATION_ENABLED = "is_vibration_enabled"
        const val EXTRA_IS_SOUND_ENABLED = "is_sound_enabled"
        const val EXTRA_IS_SILENT_MODE_ENABLED = "is_silent_mode_enabled"
        const val EXTRA_SOUND_URI = "sound_uri"
    }
}