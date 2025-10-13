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
        Log.d(TAG, "AlarmReceiver triggered")

        val alarmId = intent.getIntExtra("alarm_id", -1)
        if (alarmId == -1) {
            Log.e(TAG, "Invalid alarm ID")
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

        // ✅ FIX: Add sound resource ID based on sound type
        val soundResId = when (soundType) {
            "astro" -> R.raw.astro
            "bell" -> R.raw.bell
            "piano" -> R.raw.piano
            else -> 0
        }
        // Open AlarmRingingActivity
        val alarmIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
            putExtra("sound_res_id", soundResId)  // ✅ ADD THIS LINE

        }
        try {
            context.startActivity(alarmIntent)
            Log.d(TAG, "✅ AlarmRingingActivity started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start AlarmRingingActivity", e)
        }
        context.startActivity(alarmIntent)

        // ✅ FIX: Chỉ show notification nếu KHÔNG phải preview mode
        // Preview mode có alarmId = 0, alarm thật có alarmId > 0
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

        // ✅ FIX: KHÔNG xử lý post-trigger logic ngay lập tức
        // Để tránh race condition khi user bấm Snooze
        // Logic này sẽ được xử lý khi user bấm Dismiss hoặc sau khi Snooze xong
        // COMMENT OUT CODE NÀY:
        /*
        CoroutineScope(Dispatchers.IO).launch {
            handleAlarmTriggered(context, alarmId)
        }
        */
    }

    // ✅ THÊM: Function mới để xử lý sau khi Dismiss
    // Gọi function này từ AlarmActionReceiver khi user bấm Dismiss
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
                Log.d(TAG, "Recurring alarm ${alarm.id} rescheduled")
            } else if (alarm != null) {
                // Disable one-time alarm
                repository.updateAlarm(alarm.copy(isEnabled = false))
                Log.d(TAG, "One-time alarm ${alarm.id} disabled")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling alarm dismiss", e)
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }
}