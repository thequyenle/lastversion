package net.android.last.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import net.android.lastversion.R
import net.android.lastversion.timer.receiver.TimerBackupReceiver
import net.android.lastversion.timer.presentation.activity.TimerRingingActivity

class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1001

        // Actions
        const val ACTION_START = "START"
        const val ACTION_PAUSE = "PAUSE"
        const val ACTION_RESUME = "RESUME"
        const val ACTION_STOP = "STOP"

        // Extras
        const val EXTRA_SECONDS = "SECONDS"
        const val EXTRA_SOUND_URI = "SOUND_URI"
        const val EXTRA_SOUND_RES_ID = "SOUND_RES_ID"

        private const val BACKUP_ALARM_ID = 1000

        // Static state for Fragment to read
        @JvmStatic
        var currentRemainingSeconds: Int = 0
            private set

        @JvmStatic
        var currentTotalSeconds: Int = 0
            private set

        @JvmStatic
        var isCurrentlyPaused: Boolean = false
            private set

        @JvmStatic
        var isServiceRunning: Boolean = false
            private set

        // Reset function
        fun reset() {
            isServiceRunning = false
            currentRemainingSeconds = 0
            currentTotalSeconds = 0
            isCurrentlyPaused = false
        }
    }

    // Timer state
    private var totalSeconds = 0
    private var remainingSeconds = 0
    private var isRunning = false
    private var isPaused = false
    private var isCompleted = false

    // Sound config
    private var soundUri: Uri? = null
    private var soundResId = R.raw.astro
    private var mediaPlayer: MediaPlayer? = null

    // Timer components
    private var countdownHandler: Handler? = null
    private var countdownRunnable: Runnable? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmManager: AlarmManager? = null

    override fun onCreate() {
        super.onCreate()
        setupNotificationChannel()
        initializeComponents()
        Log.d("TimerService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleAction(intent)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initializeComponents() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TimerService::WakeLock"
        )
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Timer notifications"
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun handleAction(intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("TimerService", "Action: $action")

        when (action) {
            ACTION_START -> startTimer(intent)
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimer()
        }
    }

    private fun startTimer(intent: Intent) {
        cleanupEverything()

        totalSeconds = intent.getIntExtra(EXTRA_SECONDS, 0)
        remainingSeconds = totalSeconds

        val uriString = intent.getStringExtra(EXTRA_SOUND_URI)
        soundUri = if (!uriString.isNullOrEmpty()) Uri.parse(uriString) else null
        soundResId = intent.getIntExtra(EXTRA_SOUND_RES_ID, R.raw.astro)

        if (totalSeconds <= 0) {
            Log.e("TimerService", "Invalid timer duration: $totalSeconds")
            stopSelf()
            return
        }

        isRunning = true
        isPaused = false
        isCompleted = false

        // Update static vars
        currentRemainingSeconds = remainingSeconds
        currentTotalSeconds = totalSeconds
        isCurrentlyPaused = false
        isServiceRunning = true

        acquireWakeLock()
        scheduleBackupAlarm()
        startForeground(NOTIFICATION_ID, buildNotification())
        startCountdown()

        Log.d("TimerService", "Timer started: $totalSeconds seconds")
    }

    private fun pauseTimer() {
        if (!isRunning || isPaused || isCompleted) {
            Log.w("TimerService", "Cannot pause - invalid state")
            return
        }

        isPaused = true
        isCurrentlyPaused = true

        stopCountdown()
        cancelAllAlarms()
        updateNotification()
        stopSound()

        Log.d("TimerService", "Timer paused at $remainingSeconds seconds")
    }

    private fun resumeTimer() {
        if (!isRunning || !isPaused || isCompleted) {
            Log.w("TimerService", "Cannot resume - invalid state")
            return
        }

        isPaused = false
        isCurrentlyPaused = false

        if (remainingSeconds <= 0) {
            Log.w("TimerService", "No time left to resume")
            onTimerCompleted()
            return
        }

        scheduleBackupAlarm()
        startCountdown()
        updateNotification()

        Log.d("TimerService", "Timer resumed from $remainingSeconds seconds")
    }

    private fun stopTimer() {
        Log.d("TimerService", "Stopping timer...")

        // Clear completion state when manually stopped
        clearCompletionState()

        cleanupEverything()

        // Reset static vars
        reset()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d("TimerService", "Timer stopped")
    }

    private fun cleanupEverything() {
        isRunning = false
        isPaused = false
        isCompleted = false

        stopCountdown()
        cancelAllAlarms()
        stopSound()
        releaseWakeLock()
    }

    private fun acquireWakeLock() {
        try {
            wakeLock?.acquire((totalSeconds * 1000L) + 30000L)
            Log.d("TimerService", "WakeLock acquired")
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to acquire WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d("TimerService", "WakeLock released")
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to release WakeLock", e)
        }
    }

    private fun scheduleBackupAlarm() {
        try {
            val triggerTime = SystemClock.elapsedRealtime() + (remainingSeconds * 1000L)
            val intent = Intent(this, TimerBackupReceiver::class.java).apply {
                putExtra("TOTAL_SECONDS", totalSeconds)
                soundUri?.let { putExtra("SOUND_URI", it.toString()) }
                putExtra("SOUND_RES_ID", soundResId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this, BACKUP_ALARM_ID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager?.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d("TimerService", "Backup alarm scheduled for $remainingSeconds seconds")
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to schedule backup alarm", e)
        }
    }

    private fun cancelAllAlarms() {
        try {
            val intent = Intent(this, TimerBackupReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this, BACKUP_ALARM_ID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
            )
            alarmManager?.cancel(pendingIntent)
            Log.d("TimerService", "All backup alarms cancelled")
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to cancel backup alarms", e)
        }
    }

    private fun startCountdown() {
        stopCountdown()

        countdownHandler = Handler(Looper.getMainLooper())
        countdownRunnable = object : Runnable {
            override fun run() {
                when {
                    !isRunning || isCompleted -> {
                        Log.d("TimerService", "Countdown stopped")
                        return
                    }
                    isPaused -> {
                        countdownHandler?.postDelayed(this, 100)
                    }
                    remainingSeconds > 0 -> {
                        remainingSeconds--
                        currentRemainingSeconds = remainingSeconds
                        updateNotification()
                        countdownHandler?.postDelayed(this, 1000)
                    }
                    else -> {
                        onTimerCompleted()
                    }
                }
            }
        }
        countdownHandler?.post(countdownRunnable!!)
    }

    private fun stopCountdown() {
        countdownRunnable?.let {
            countdownHandler?.removeCallbacks(it)
            countdownRunnable = null
        }
    }

    private fun onTimerCompleted() {
        if (isCompleted) {
            Log.w("TimerService", "Already completed, ignoring")
            return
        }

        Log.d("TimerService", "Timer completed!")
        isCompleted = true
        isRunning = false
        isServiceRunning = false
        currentRemainingSeconds = 0

        // Save completion state
        saveCompletionState()

        cancelAllAlarms()

        // ✅ LAUNCH TimerRingingActivity
        val timerIntent = Intent(this, TimerRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("total_seconds", totalSeconds)
            putExtra("sound_uri", soundUri?.toString() ?: "")
            putExtra("sound_res_id", soundResId)
        }
        startActivity(timerIntent)

        // Show notification as backup
        val completionNotification = buildCompletionNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, completionNotification)

        startForeground(NOTIFICATION_ID, completionNotification)
        playCompletionSound()
    }

    private fun playCompletionSound() {
        try {
            stopSound()

            mediaPlayer = MediaPlayer().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                }

                setDataSource()
                isLooping = true
                prepare()
                start()

                Log.d("TimerService", "Completion sound started")
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to play completion sound", e)
            playFallbackSound()
        }
    }

    private fun MediaPlayer.setDataSource() {
        when {
            soundUri != null -> {
                try {
                    setDataSource(applicationContext, soundUri!!)
                } catch (e: Exception) {
                    Log.e("TimerService", "Failed to set custom sound URI", e)
                    setDataSource(applicationContext,
                        Uri.parse("android.resource://${packageName}/${R.raw.astro}"))
                }
            }
            soundResId != -1 -> {
                setDataSource(applicationContext,
                    Uri.parse("android.resource://${packageName}/$soundResId"))
            }
            else -> {
                setDataSource(applicationContext,
                    Uri.parse("android.resource://${packageName}/${R.raw.astro}"))
            }
        }
    }

    private fun playFallbackSound() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.astro)?.apply {
                isLooping = true
                start()
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to play fallback sound", e)
        }
    }

    private fun stopSound() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
            Log.d("TimerService", "Sound stopped")
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to stop sound", e)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
        )

        val pauseResumeAction = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        val pauseResumeText = if (isPaused) "Resume" else "Pause"
        val pauseResumeIntent = PendingIntent.getService(
            this, 1, Intent(this, TimerService::class.java).apply { action = pauseResumeAction },
            PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
        )

        val stopIntent = PendingIntent.getService(
            this, 2, Intent(this, TimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_enable)
            .setContentTitle("Timer Running")
            .setContentText("${formatTime(remainingSeconds)} remaining")
            .setContentIntent(contentIntent)
            .setColor(Color.parseColor("#76E0C1"))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_stopwatch_enable, pauseResumeText, pauseResumeIntent)
            .addAction(R.drawable.ic_stopwatch_enable, "Stop", stopIntent)
            .build()
    }

    private fun buildCompletionNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, packageManager.getLaunchIntentForPackage(packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
        )

        val stopSoundIntent = PendingIntent.getService(
            this, 3, Intent(this, TimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_enable)
            .setContentTitle("Time's Up!")
            .setContentText("Your ${formatTime(totalSeconds)} timer has finished")
            .setContentIntent(contentIntent)
            .setColor(Color.parseColor("#76E0C1"))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_stopwatch_enable, "Stop Sound", stopSoundIntent)
            .build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
            else -> String.format("%d:%02d", minutes, secs)
        }
    }

    private fun getPendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    // ==================== COMPLETION STATE PERSISTENCE ====================

    private fun saveCompletionState() {
        val prefs = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("timer_completed", true)
        editor.putInt("completed_total_seconds", totalSeconds)
        editor.putLong("completion_time", System.currentTimeMillis())
        val success = editor.commit()  // ✅ Use commit() for immediate save!
        Log.d("TimerService", "Completion state saved: $success, totalSeconds=$totalSeconds")
    }

    private fun clearCompletionState() {
        val prefs = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove("timer_completed")
        editor.remove("completed_total_seconds")
        editor.remove("completion_time")
        val success = editor.commit()  // ✅ Use commit() for immediate clear!
        Log.d("TimerService", "Completion state cleared: $success")
    }

    // ======================================================================

    override fun onDestroy() {
        super.onDestroy()
        cleanupEverything()
        reset()
        Log.d("TimerService", "Service destroyed")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("TimerService", "Task removed - service continuing")
    }
}