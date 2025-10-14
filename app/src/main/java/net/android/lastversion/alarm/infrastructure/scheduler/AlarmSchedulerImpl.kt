package net.android.lastversion.alarm.infrastructure.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.infrastructure.receiver.AlarmReceiver
import net.android.lastversion.alarm.presentation.activity.AlarmRingingActivity

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

        // Android 12+ cần quyền exact alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "❌ Cannot schedule exact alarms - permission denied")
                val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // an toàn nếu gọi không phải từ Activity
                context.startActivity(i)
                return
            }
            Log.d(TAG, "✅ Has exact alarm permission")
        }

        try {
            // ===== PendingIntent cho OPERATION (Broadcast) khi tới giờ =====
            val operationIntent = Intent(context, AlarmReceiver::class.java).apply {
                // dùng action để identity rõ ràng; extras không ảnh hưởng đến equality
                action = "net.android.lastversion.ACTION_FIRE_ALARM"
                putExtra(EXTRA_ALARM_ID, alarm.id)
                putExtra(EXTRA_ALARM_HOUR, alarm.hour)
                putExtra(EXTRA_ALARM_MINUTE, alarm.minute)
                putExtra(EXTRA_ALARM_AM_PM, alarm.amPm)
                putExtra(EXTRA_ALARM_LABEL, alarm.label)
                putExtra(EXTRA_ALARM_NOTE, alarm.note)
                putExtra(EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
                putExtra(EXTRA_VIBRATION_PATTERN, alarm.vibrationPattern)
                putExtra(EXTRA_SOUND_TYPE, alarm.soundType)
                putExtra(EXTRA_IS_SILENT_MODE_ENABLED, alarm.isSilentModeEnabled)
                putExtra(EXTRA_SOUND_URI, alarm.soundUri)
            }

            val opPendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.id, // giữ requestCode theo id để dễ quản lý
                operationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            Log.d(TAG, "✅ Operation PendingIntent created (requestCode=${alarm.id})")

            // ===== PendingIntent cho SHOW (Activity) khi user chạm icon đồng hồ =====
            val showIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_ALARM_ID, alarm.id)
                putExtra(EXTRA_ALARM_HOUR, alarm.hour)
                putExtra(EXTRA_ALARM_MINUTE, alarm.minute)
                putExtra(EXTRA_ALARM_AM_PM, alarm.amPm)
                putExtra(EXTRA_ALARM_LABEL, alarm.label)
                putExtra(EXTRA_ALARM_NOTE, alarm.note)
                putExtra(EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
                putExtra(EXTRA_VIBRATION_PATTERN, alarm.vibrationPattern)
                putExtra(EXTRA_SOUND_TYPE, alarm.soundType)
                putExtra(EXTRA_IS_SILENT_MODE_ENABLED, alarm.isSilentModeEnabled)
                putExtra(EXTRA_SOUND_URI, alarm.soundUri)
            }

            // dùng requestCode khác để không “đè” opPendingIntent
            val showPendingIntent = PendingIntent.getActivity(
                context,
                alarm.id shl 1, // khác với op PI (ví dụ: id*2)
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            Log.d(TAG, "✅ Show PendingIntent created (requestCode=${alarm.id shl 1})")

            // ===== Đặt AlarmClock (exact + ưu tiên) =====
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, opPendingIntent)

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
            // Phải tạo cùng "danh tính" với khi schedule: component + action + requestCode
            val opIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = "net.android.lastversion.ACTION_FIRE_ALARM"
            }
            val opPi = PendingIntent.getBroadcast(
                context,
                alarmId,
                opIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            val showIntent = Intent(context, AlarmRingingActivity::class.java)
            val showPi = PendingIntent.getActivity(
                context,
                alarmId shl 1,
                showIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            var cancelled = false
            if (opPi != null) {
                alarmManager.cancel(opPi)
                opPi.cancel()
                cancelled = true
            }
            if (showPi != null) {
                // Không bắt buộc cancel, nhưng sạch sẽ
                showPi.cancel()
            }

            if (cancelled) {
                Log.d(TAG, "✅ Alarm $alarmId cancelled successfully")
            } else {
                Log.w(TAG, "⚠️ PendingIntent(s) not found for alarm $alarmId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to cancel alarm $alarmId", e)
        }
    }

    override fun rescheduleAllAlarms(alarms: List<Alarm>) {
        Log.d(TAG, "Rescheduling ${alarms.size} alarms")
        alarms.filter { it.isEnabled }.forEach { alarm ->
            scheduleAlarm(alarm)
        }
    }

    private fun createAlarmIntent(alarm: Alarm): Intent {
        // (giữ nguyên nếu bạn còn dùng ở nơi khác)
        return Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_HOUR, alarm.hour)
            putExtra(EXTRA_ALARM_MINUTE, alarm.minute)
            putExtra(EXTRA_ALARM_AM_PM, alarm.amPm)
            putExtra(EXTRA_ALARM_LABEL, alarm.label)
            putExtra(EXTRA_ALARM_NOTE, alarm.note)
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
        const val EXTRA_IS_SILENT_MODE_ENABLED = "is_silent_mode_enabled"

        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        const val EXTRA_VIBRATION_PATTERN = "vibration_pattern"
        const val EXTRA_SOUND_TYPE = "sound_type"
        const val EXTRA_SOUND_URI = "sound_uri"
    }
}
