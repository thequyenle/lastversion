package net.android.lastversion.alarm.infrastructure.receiver

import android.Manifest
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import net.android.lastversion.R
import net.android.lastversion.alarm.data.local.database.AlarmDatabase
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.infrastructure.notification.AlarmNotificationManager
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmSchedulerImpl
import net.android.lastversion.alarm.presentation.activity.AlarmRingingActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        Log.d(TAG, "🔔 AlarmReceiver triggered")

        // 1) Lấy extras trước
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

        val soundResId = when (soundType) {
            "astro" -> R.raw.astro
            "bell"  -> R.raw.bell
            "piano" -> R.raw.piano
            else    -> 0
        }

        // Intent mở Activity chuông (dùng cho cả foreground và full-screen)
        val alarmActivityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
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

        // 2) DÙNG ACTIVITYMANAGER để kiểm tra app đang foreground
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val isAppForeground = am.runningAppProcesses?.any {
            it.pid == android.os.Process.myPid() &&
                    it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        } == true
        Log.d(TAG, "👀 isAppForeground=$isAppForeground")

        if (isAppForeground) {
            // App đang mở → bật thẳng activity (được phép trên Android 10+)
            context.startActivity(alarmActivityIntent)
            Log.d(TAG, "✅ App foreground → launch AlarmRingingActivity directly")
            return
        }

        // 3) App nền/khóa → full-screen notification
        Log.d(TAG, "📋 Prepare full-screen UI with alarm_id: $alarmId")

        val fullScreenPi = PendingIntent.getActivity(
            context, alarmId, alarmActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = context.getSystemService(NotificationManager::class.java)
        val channelId = CHANNEL_ID
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId, "Alarms", NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alarm ringing"
                    setBypassDnd(true) // hiệu lực nếu user cho phép DND access
                }
            )
        }
        Log.d(TAG, "🔎 Channel '$channelId' importance=${nm.getNotificationChannel(channelId)?.importance}")

        val noti = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_alarm_enable)
            .setContentTitle(title)
            .setContentText(if (note.isNotEmpty()) note else "Ringing…")
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(0)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPi, true)
            .build()

        val canNotify =
            Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        if (!canNotify) {
            Log.w(TAG, "⚠️ POST_NOTIFICATIONS not granted; skip full-screen notification")
        } else {
            try {
                if (alarmId != 0) {
                    NotificationManagerCompat.from(context).notify(FULLSCREEN_ID_OFFSET + alarmId, noti)
                    Log.d(TAG, "✅ Full-screen notification posted for alarm $alarmId")
                } else {
                    Log.d(TAG, "⚠️ Preview mode - skipping full-screen notification (alarmId = 0)")
                }
            } catch (se: SecurityException) {
                Log.e(TAG, "❌ Cannot post notification (permission/policy)", se)
            }
        }

        // 4) Giữ nguyên notification hành động (snooze/dismiss)
        if (alarmId != 0) {
            Log.d(TAG, "✅ Real alarm - showing action notification (AlarmNotificationManager)")
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
            Log.d(TAG, "⚠️ Preview mode - skipping action notification (alarmId = 0)")
        }
    }

    // ========== GIỮ NGUYÊN ==========
    suspend fun handleAlarmDismissed(context: Context, alarmId: Int) {
        try {
            if (alarmId == 0) {
                Log.d(TAG, "Preview mode - skipping post-dismiss logic")
                return
            }
            val repository = AlarmRepositoryImpl(AlarmDatabase.getDatabase(context).alarmDao())
            val scheduler = AlarmSchedulerImpl(context)

            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null && alarm.hasRecurringDays()) {
                scheduler.scheduleAlarm(alarm)
                Log.d(TAG, "Recurring alarm ${alarm.id} rescheduled")
            } else if (alarm != null) {
                repository.updateAlarm(alarm.copy(isEnabled = false))
                Log.d(TAG, "One-time alarm ${alarm.id} disabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling alarm dismiss", e)
        }
    }

    private fun isAppInForeground(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val procs = am.runningAppProcesses ?: return false
            val myPid = android.os.Process.myPid()
            val myPkg = context.packageName

            procs.any { info ->
                // Đúng tiến trình & đúng gói của app
                (info.pid == myPid || info.processName == myPkg) &&
                        // Chấp nhận các mức "đang hiện diện" trên màn hình
                        (
                                info.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                                        info.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE ||
                                        info.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE
                                )
            }
        } catch (_: Exception) { false }
    }
    companion object {
        private const val TAG = "AlarmReceiver"
        private const val CHANNEL_ID = "alarm_channel_v2"
        private const val FULLSCREEN_ID_OFFSET = 50000 // thêm vào companion object

    }
}
