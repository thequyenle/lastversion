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
        Log.d(TAG, "========================================")
        Log.d(TAG, "scheduleAlarm() CALLED")
        Log.d(TAG, "Alarm ID: ${alarm.id}")
        Log.d(TAG, "Time: ${alarm.hour}:${alarm.minute} ${alarm.amPm}")
        Log.d(TAG, "Enabled: ${alarm.isEnabled}")

        if (!alarm.isEnabled) {
            Log.w(TAG, "⚠️ Alarm ${alarm.id} is DISABLED, skipping schedule")
            return
        }

        val triggerTime = alarm.getNextTriggerTime()
        Log.d(TAG, "Trigger time: ${java.util.Date(triggerTime)}")
        Log.d(TAG, "Current time: ${java.util.Date()}")

        if (triggerTime <= System.currentTimeMillis()) {
            Log.e(TAG, "❌ ERROR: Trigger time is in the PAST!")
            Log.e(TAG, "Trigger: ${java.util.Date(triggerTime)}")
            Log.e(TAG, "Current: ${java.util.Date()}")
            return
        }

        Log.d(TAG, "✅ Trigger time is valid (in the future)")

        // Check exact alarm permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "❌ ERROR: Cannot schedule exact alarms - PERMISSION DENIED")
                return
            }
            Log.d(TAG, "✅ Has exact alarm permission")
        }

        try {
            val intent = createAlarmIntent(alarm)
            Log.d(TAG, "✅ Intent created")

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            Log.d(TAG, "✅ PendingIntent created with requestCode: ${alarm.id}")

            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

            Log.d(TAG, "🎉 SUCCESS! Alarm ${alarm.id} scheduled!")
            Log.d(TAG, "Will ring at: ${java.util.Date(triggerTime)}")
            Log.d(TAG, "========================================")

        } catch (e: Exception) {
            Log.e(TAG, "❌❌❌ EXCEPTION in scheduleAlarm:", e)
            Log.e(TAG, "Error: ${e.message}")
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

            // THAY ĐỔI: Từ Boolean sang giá trị cụ thể
            putExtra(EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
            putExtra(EXTRA_VIBRATION_PATTERN, alarm.vibrationPattern)
            putExtra(EXTRA_SOUND_TYPE, alarm.soundType)
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
        const val EXTRA_IS_SILENT_MODE_ENABLED = "is_silent_mode_enabled"  // ← THÊM DÒNG NÀY

        // THAY ĐỔI: Đổi tên constant
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        const val EXTRA_VIBRATION_PATTERN = "vibration_pattern"
        const val EXTRA_SOUND_TYPE = "sound_type"

        const val EXTRA_SOUND_URI = "sound_uri"
    }
}