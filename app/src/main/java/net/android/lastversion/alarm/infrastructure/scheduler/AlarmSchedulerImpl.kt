package net.android.lastversion.alarm.infrastructure.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.infrastructure.receiver.AlarmReceiver
import android.util.Log                    // ← Thêm
import java.util.Date                     // ← Thêm
import android.os.Build                   // ← Thêm
import android.Manifest                   // ← Thêm
import android.content.pm.PackageManager  // ← Thêm
import androidx.core.app.NotificationCompat        // ← Thêm
import androidx.core.app.NotificationManagerCompat // ← Thêm
import androidx.core.app.ActivityCompat

class AlarmSchedulerImpl(private val context: Context) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleAlarm(alarmId: Int, triggerTime: Long, alarm: Alarm) {
        Log.d("AlarmScheduler", "=== SCHEDULING ALARM ===")
        Log.d("AlarmScheduler", "Alarm ID: $alarmId")
        Log.d("AlarmScheduler", "Current time: ${Date(System.currentTimeMillis())}")
        Log.d("AlarmScheduler", "Trigger time: ${Date(triggerTime)}")
        Log.d("AlarmScheduler", "Time until trigger: ${(triggerTime - System.currentTimeMillis()) / 1000} seconds")

        // Kiểm tra thời gian hợp lệ
        if (triggerTime <= System.currentTimeMillis()) {
            Log.e("AlarmScheduler", "ERROR: Trigger time is in the past!")
            return
        }

        // Kiểm tra permission trước khi schedule (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canScheduleExact = alarmManager.canScheduleExactAlarms()
            Log.d("AlarmScheduler", "Can schedule exact alarms: $canScheduleExact")

            if (!canScheduleExact) {
                Log.e("AlarmScheduler", "ERROR: Cannot schedule exact alarms - permission not granted!")

                // Show user notification about missing permission
                showExactAlarmPermissionNotification()
                return
            }
        }

        try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarmId)
                putExtra("alarm_hour", alarm.hour)
                putExtra("alarm_minute", alarm.minute)
                putExtra("alarm_label", alarm.label)
                putExtra("alarm_note", alarm.note)
                putExtra("is_snooze_enabled", alarm.isSnoozeEnabled)
                putExtra("is_vibration_enabled", alarm.isVibrationEnabled)
                putExtra("is_sound_enabled", alarm.isSoundEnabled)
            }

            Log.d("AlarmScheduler", "Created intent with extras: ${intent.extras?.keySet()}")

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            Log.d("AlarmScheduler", "Created PendingIntent: $pendingIntent")

            // Use setAlarmClock for exact timing and show in lock screen
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

            Log.d("AlarmScheduler", "SUCCESS: Alarm scheduled with setAlarmClock()")

            // Verify scheduling worked
            verifyAlarmScheduled(triggerTime)

        } catch (e: Exception) {
            Log.e("AlarmScheduler", "ERROR: Failed to schedule alarm", e)
        }

        Log.d("AlarmScheduler", "=== SCHEDULING COMPLETE ===")
    }

    override fun cancelAlarm(alarmId: Int) {
        Log.d("AlarmScheduler", "Cancelling alarm ID: $alarmId")

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
                Log.d("AlarmScheduler", "Alarm cancelled successfully")
            } ?: run {
                Log.w("AlarmScheduler", "PendingIntent not found - alarm may not have been scheduled")
            }

        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Error cancelling alarm", e)
        }
    }

    override fun rescheduleRecurringAlarm(alarmId: Int, alarm: Alarm) {
        Log.d("AlarmScheduler", "Rescheduling recurring alarm: $alarmId")
        // Implementation for recurring alarms
    }

    private fun verifyAlarmScheduled(expectedTime: Long) {
        try {
            // Get next alarm info (Android 5.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val nextAlarmInfo = alarmManager.nextAlarmClock
                if (nextAlarmInfo != null) {
                    Log.d("AlarmScheduler", "Next system alarm: ${Date(nextAlarmInfo.triggerTime)}")

                    // Check if our expected time is close to next alarm (within 2 minutes)
                    val timeDiff = Math.abs(nextAlarmInfo.triggerTime - expectedTime)
                    if (timeDiff < 120000) { // 2 minutes tolerance
                        Log.d("AlarmScheduler", "VERIFICATION: Alarm appears to be scheduled correctly!")
                    } else {
                        Log.w("AlarmScheduler", "VERIFICATION: Expected time doesn't match next system alarm")
                        Log.w("AlarmScheduler", "Time difference: ${timeDiff / 1000} seconds")
                    }
                } else {
                    Log.w("AlarmScheduler", "VERIFICATION: No next alarm found in system")
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Error verifying alarm", e)
        }
    }

    private fun showExactAlarmPermissionNotification() {
        // Simple notification to user about missing permission
        try {
            val notificationBuilder = NotificationCompat.Builder(context, "alarm_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Alarm Permission Needed")
                .setContentText("Please enable 'Alarms & reminders' permission in Settings")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            val notificationManager = NotificationManagerCompat.from(context)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(1, notificationBuilder.build())
            }
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Could not show permission notification", e)
        }
    }
}