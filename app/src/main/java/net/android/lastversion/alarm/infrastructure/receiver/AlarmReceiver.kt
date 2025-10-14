package net.android.lastversion.alarm.infrastructure.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.android.lastversion.R
import net.android.lastversion.alarm.data.local.database.AlarmDatabase
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.infrastructure.notification.AlarmNotificationManager
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmSchedulerImpl
import net.android.lastversion.alarm.presentation.activity.AlarmRingingActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔔 AlarmReceiver triggered")

        val alarmId = intent.getIntExtra("alarm_id", -1)
        Log.d(TAG, "📋 Received alarm_id from intent: $alarmId")

        if (alarmId == -1) {
            Log.e(TAG, "❌ Invalid alarm ID")
            return
        }

        val title = intent.getStringExtra("alarm_label") ?: "Alarm"
        val note = intent.getStringExtra("alarm_note") ?: ""
        val snoozeMinutes = intent.getIntExtra("snooze_minutes", 5)
        val vibrationPattern = intent.getStringExtra("vibration_pattern") ?: "default"
        val soundType = intent.getStringExtra("sound_type") ?: "default"
        val isSilentModeEnabled = intent.getBooleanExtra("is_silent_mode_enabled", false)
        val soundUri = intent.getStringExtra("sound_uri") ?: ""
        val hour = intent.getIntExtra("alarm_hour", 0)
        val minute = intent.getIntExtra("alarm_minute", 0)
        val amPm = intent.getStringExtra("alarm_am_pm") ?: "AM"

        Log.d(TAG, "📋 Starting AlarmRingingActivity with alarm_id: $alarmId")

        // ✅ Get sound resource ID based on sound type
        val soundResId = when (soundType) {
            "astro" -> R.raw.astro
            "bell" -> R.raw.bell
            "piano" -> R.raw.piano
            else -> 0
        }

        // ✅ Create Intent with proper flags to bypass SplashActivity/Tutorial
        val alarmIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            // ✅ CRITICAL: These flags ensure AlarmRingingActivity opens directly
            // even when app is killed, bypassing SplashActivity/Tutorial
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS

            // ✅ Set explicit component to ensure it goes directly to this activity
            component = android.content.ComponentName(
                context.packageName,
                "net.android.lastversion.alarm.presentation.activity.AlarmRingingActivity"
            )

            putExtra("alarm_id", alarmId)
            putExtra("alarm_hour", hour)
            putExtra("alarm_minute", minute)
            putExtra("alarm_am_pm", amPm)
            putExtra("alarm_label", title)
            putExtra("alarm_note", note)
            putExtra("snooze_minutes", snoozeMinutes)
            putExtra("vibration_pattern", vibrationPattern)
            putExtra("sound_type", soundType)
            putExtra("is_silent_mode_enabled", isSilentModeEnabled)
            putExtra("sound_uri", soundUri)
            putExtra("sound_res_id", soundResId)
        }

        try {
            context.startActivity(alarmIntent)
            Log.d(TAG, "✅ AlarmRingingActivity started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start AlarmRingingActivity", e)
        }

        // ✅ Show notification for real alarms (not preview mode)
        if (alarmId != 0) {
            Log.d(TAG, "✅ Real alarm - showing notification with functional snooze button")
            val notificationManager = AlarmNotificationManager(context)
            notificationManager.showAlarmNotification(
                alarmId = alarmId,
                title = title,
                note = note,
                snoozeMinutes = snoozeMinutes,
                vibrationPattern = vibrationPattern,
                soundType = soundType,
                soundUri = soundUri,
                isSilentModeEnabled = isSilentModeEnabled
            )
        } else {
            Log.d(TAG, "⚠️ Preview mode - skipping notification (alarmId = 0)")
        }

        // ✅ NOTE: Post-trigger logic (disable/reschedule) is handled in AlarmActionReceiver
        // when user dismisses the alarm, to avoid race conditions with snooze
    }

    // ✅ Function to handle alarm dismissal - called from AlarmActionReceiver
    suspend fun handleAlarmDismissed(context: Context, alarmId: Int) {
        try {
            if (alarmId == 0) {
                Log.d(TAG, "Preview mode - skipping post-dismiss logic")
                return
            }

            val repository = AlarmRepositoryImpl(
                AlarmDatabase.getDatabase(context).alarmDao()
            )
            val scheduler = AlarmSchedulerImpl(context)

            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null && alarm.hasRecurringDays()) {
                // Reschedule recurring alarm
                scheduler.scheduleAlarm(alarm)
                Log.d(TAG, "✅ Recurring alarm ${alarm.id} rescheduled")
            } else if (alarm != null) {
                // Disable one-time alarm
                repository.updateAlarm(alarm.copy(isEnabled = false))
                Log.d(TAG, "✅ One-time alarm ${alarm.id} disabled")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling alarm dismiss", e)
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}