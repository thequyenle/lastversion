package net.android.lastversion.alarm.infrastructure.receiver

import android.Manifest
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val CHANNEL_ID = "alarm_channel_v2"
        private const val FULLSCREEN_ID_OFFSET = 50000

        // Static media player and vibrator for immediate sound/vibration
        private var mediaPlayer: MediaPlayer? = null
        private var vibrator: Vibrator? = null

        /**
         * Stop immediate sound and vibration from any instance
         * This is a static method that can be called from anywhere
         */
        fun stopImmediateSoundAndVibrationStatic() {
            try {
                mediaPlayer?.apply {
                    if (isPlaying) stop()
                    release()
                }
                mediaPlayer = null

                vibrator?.cancel()
                vibrator = null

                Log.d(TAG, "Immediate sound and vibration stopped (static method)")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping immediate sound and vibration (static method)", e)
            }
        }
    }

    // Instance variables for backward compatibility
    private var instanceMediaPlayer: MediaPlayer? = null
    private var instanceVibrator: Vibrator? = null

    override fun onReceive(context: Context, intent: Intent) {

        Log.d(TAG, context.getString(R.string.alarm_receiver_triggered))

        // 1) Lấy extras trước
        val alarmId = intent.getIntExtra("alarm_id", -1)
        Log.d(TAG, context.getString(R.string.received_alarm_id_from_intent, alarmId))
        if (alarmId == -1) {
            Log.e(TAG, context.getString(R.string.invalid_alarm_id))
            return
        }

        // Check if this is a snooze alarm (ID >= 50000)
        val isSnoozeAlarm = alarmId >= 50000
        Log.d(TAG, context.getString(R.string.is_snooze_alarm, isSnoozeAlarm))

        val title = intent.getStringExtra("alarm_label") ?: context.getString(R.string.alarm)
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

        // 🔊 PLAY IMMEDIATE SOUND AND VIBRATION (even when app is in background)
        Log.d(TAG, context.getString(R.string.starting_immediate_sound_and_vibration))
        playImmediateSoundAndVibration(context, soundType, soundUri, isSilentModeEnabled, vibrationPattern, soundResId)

        // 🚫 SNOOZE ALARMS: Don't launch activity, only show notification
        if (isSnoozeAlarm) {
            Log.d(TAG, context.getString(R.string.snooze_alarm_showing_interactive_notification))

            // Cancel the status notification first
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(alarmId + 20000) // Cancel status notification

            // Also cancel any existing snooze notification to prevent duplicates
            val originalAlarmId = alarmId - 50000
            notificationManager.cancel(originalAlarmId + 10000) // Cancel existing snooze notification

            // Show interactive snooze notification
            showSnoozeNotification(context, alarmId, title, note, snoozeMinutes, vibrationPattern, soundType, soundUri, isSilentModeEnabled)
            return
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
            it.pid == android.os.Process.myPid() && (
                    it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                    it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE ||
                    it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE
                    )
        } == true
        Log.d(TAG, context.getString(R.string.is_app_foreground, isAppForeground))

        if (isAppForeground) {
            // App đang mở → bật thẳng activity (được phép trên Android 10+)
            context.startActivity(alarmActivityIntent)
            Log.d(TAG, context.getString(R.string.app_foreground_launch_activity))

            // KHÔNG hiển thị notification khi app foreground vì activity đã hiển thị
            if (alarmId != 0) {
                Log.d(TAG, context.getString(R.string.app_foreground_no_notification))
            } else {
                Log.d(TAG, context.getString(R.string.preview_mode_skip_notification))
            }
            return
        }

        // 3) App nền/khóa → full-screen notification
        Log.d(TAG, context.getString(R.string.prepare_fullscreen_ui, alarmId))

        val fullScreenPi = PendingIntent.getActivity(
            context, alarmId, alarmActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = context.getSystemService(NotificationManager::class.java)
        val channelId = CHANNEL_ID
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId, context.getString(R.string.alarms), NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.alarm_ringing)
                    setBypassDnd(true) // hiệu lực nếu user cho phép DND access
                }
            )
        }
        Log.d(TAG, context.getString(R.string.channel_importance, channelId, nm.getNotificationChannel(channelId)?.importance))

        val noti = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_alarm_enable)
            .setContentTitle(title)
            .setContentText(if (note.isNotEmpty()) note else context.getString(R.string.ringing))
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
            Log.w(TAG, context.getString(R.string.post_notifications_not_granted))
        } else {
            try {
                if (alarmId != 0) {
                    NotificationManagerCompat.from(context).notify(FULLSCREEN_ID_OFFSET + alarmId, noti)
                    Log.d(TAG, context.getString(R.string.fullscreen_notification_posted, alarmId))
                } else {
                    Log.d(TAG, context.getString(R.string.preview_mode_skip_notification))
                }
            } catch (se: SecurityException) {
                Log.e(TAG, context.getString(R.string.cannot_post_notification), se)
            }
        }

        // 4) App background - KHÔNG hiển thị action notification
        // Full-screen notification đã đủ để xử lý alarm và launch activity
        if (alarmId != 0) {
            if (isSnoozeAlarm) {
                Log.d(TAG, context.getString(R.string.snooze_alarm_background_fullscreen_notification))
            } else {
                Log.d(TAG, context.getString(R.string.app_background_fullscreen_notification_only))
            }
        } else {
            Log.d(TAG, context.getString(R.string.preview_mode_skip_notification))
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

    /**
     * Play immediate sound and vibration when alarm triggers
     * This ensures audio feedback even when app is in background
     */
    private fun playImmediateSoundAndVibration(
        context: Context,
        soundType: String,
        soundUri: String,
        isSilentModeEnabled: Boolean,
        vibrationPattern: String,
        soundResId: Int
    ) {
        try {
            // Play sound if not disabled
            if (soundType != "off") {
                playSound(context, soundUri, isSilentModeEnabled, soundResId, soundType)
            }

            // Play vibration if not disabled
            if (vibrationPattern != "off") {
                startVibration(context, vibrationPattern)
            }

            // Schedule cleanup after 30 seconds to prevent infinite playback
            // AlarmRingingActivity will take over and stop this when it starts
            val cleanupIntent = Intent(context, AlarmCleanupReceiver::class.java).apply {
                action = "ACTION_CLEANUP_ALARM_SOUND"
            }
            val cleanupPendingIntent = PendingIntent.getBroadcast(
                context, 0, cleanupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Use AlarmManager to schedule cleanup
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                android.os.SystemClock.elapsedRealtime() + 30000, // 30 seconds
                cleanupPendingIntent
            )

            Log.d(TAG, context.getString(R.string.immediate_sound_and_vibration_started_cleanup_scheduled))

        } catch (e: Exception) {
            Log.e(TAG, context.getString(R.string.error_playing_immediate_sound_and_vibration), e)
        }
    }

    private fun playSound(
        context: Context,
        soundUri: String,
        bypassSilentMode: Boolean,
        soundResId: Int,
        soundType: String
    ) {
        try {
            // Stop any existing sound before playing new one
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }

            mediaPlayer = MediaPlayer().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setAudioAttributes(audioAttributes)
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                }

                if (soundResId != 0) {
                    val uri = Uri.parse("android.resource://${context.packageName}/$soundResId")
                    setDataSource(context, uri)
                } else if (soundUri.isNotEmpty()) {
                    setDataSource(context, Uri.parse(soundUri))
                } else {
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setDataSource(context, defaultUri)
                }

                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }

            Log.d(TAG, context.getString(R.string.immediate_sound_started_with_type, soundType))
        } catch (e: Exception) {
            Log.e(TAG, context.getString(R.string.error_playing_immediate_sound), e)
        }
    }

    private fun startVibration(context: Context, pattern: String) {
        try {
            // Stop any existing vibration before starting new one
            vibrator?.cancel()

            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator == null) {
                Log.e(TAG, context.getString(R.string.vibrator_service_is_null))
                return
            }

            if (!vibrator!!.hasVibrator()) {
                Log.e(TAG, context.getString(R.string.device_does_not_have_vibrator))
                return
            }

            val vibrationPattern = when (pattern) {
                "short" -> longArrayOf(0, 300, 200, 300, 200, 300)
                "long" -> longArrayOf(0, 1000, 500, 1000, 500, 1000)
                "double" -> longArrayOf(0, 500, 200, 500, 200, 500, 200, 500)
                "default" -> longArrayOf(0, 1000, 500, 1000, 500, 1000)
                else -> longArrayOf(0, 1000, 500, 1000, 500, 1000)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(vibrationPattern, 0)
                vibrator?.vibrate(effect)
                Log.d(TAG, context.getString(R.string.immediate_vibration_started_api_26, pattern))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(vibrationPattern, 0)
                Log.d(TAG, context.getString(R.string.immediate_vibration_started_api_below_26, pattern))
            }

        } catch (e: Exception) {
            Log.e(TAG, context.getString(R.string.error_starting_immediate_vibration), e)
        }
    }

    /**
     * Stop immediate sound and vibration
     * Called by AlarmCleanupReceiver or when AlarmRingingActivity starts
     * @deprecated Use stopImmediateSoundAndVibrationStatic() instead
     */
    @Deprecated("Use stopImmediateSoundAndVibrationStatic() instead")
    fun stopImmediateSoundAndVibration() {
        stopImmediateSoundAndVibrationStatic()
    }

    /**
     * Show snooze notification for snooze alarms
     */
    private fun showSnoozeNotification(
        context: Context,
        alarmId: Int,
        title: String,
        note: String,
        snoozeMinutes: Int,
        vibrationPattern: String,
        soundType: String,
        soundUri: String,
        isSilentModeEnabled: Boolean
    ) {
        try {
            val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
            val notificationManager = AlarmNotificationManager(context)
            // Use the original alarm ID for notification to maintain consistency
            val originalAlarmId = alarmId - 50000
            notificationManager.showSnoozeNotification(originalAlarmId, title, snoozeTime)
            Log.d(TAG, context.getString(R.string.snooze_notification_shown, alarmId))
        } catch (e: Exception) {
            Log.e(TAG, context.getString(R.string.error_showing_snooze_notification), e)
        }
    }
}
