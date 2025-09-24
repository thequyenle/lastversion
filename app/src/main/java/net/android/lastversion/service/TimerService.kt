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
import net.android.lastversion.receiver.TimerBackupReceiver

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
    }

    // Timer state
    private var totalSeconds = 0
    private var remainingSeconds = 0
    private var isRunning = false
    private var isPaused = false
    private var isCompleted = false
    private var hasTimerEnded = false
    // Timing precision
    private var lastUpdateTime = 0L

    // Sound config
    private var soundUri: Uri? = null
    private var soundResId = R.raw.astro
    private var mediaPlayer: MediaPlayer? = null

    // Timer components
    private var countdownHandler: Handler? = null
    private var countdownRunnable: Runnable? = null
    private var wakeLock: PowerManager.WakeLock? = null


    // AlarmManager backup - CHỈ MỘT ALARM DUY NHẤT
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

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timer countdown and controls"
                enableLights(true)
                lightColor = Color.parseColor("#76E0C1")
                enableVibration(false)
                setBypassDnd(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializeComponents() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TimerService::WakeLock")
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private fun handleAction(intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("TimerService", "Action: $action, State: running=$isRunning, paused=$isPaused, completed=$isCompleted, remaining=$remainingSeconds")

        when (action) {
            ACTION_START -> startTimer(intent)
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimer()
        }
    }

    private fun startTimer(intent: Intent) {
        hasTimerEnded = false
        if (isRunning && !isCompleted) {
            Log.w("TimerService", "Timer already running")
            return
        }

        // RESET HOÀN TOÀN
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

        // Initialize
        isRunning = true
        isPaused = false
        isCompleted = false
        lastUpdateTime = System.currentTimeMillis()

        // Setup
        acquireWakeLock()
        scheduleBackupAlarm()
        startForeground(NOTIFICATION_ID, buildNotification())
        startCountdown()

        Log.d("TimerService", "Timer started: $totalSeconds seconds")
    }

    private fun pauseTimer() {
        hasTimerEnded = true

        if (!isRunning || isPaused || isCompleted) {
            Log.w("TimerService", "Cannot pause - invalid state")
            return
        }
        isPaused = true
        // Update remaining time accurately
        updateRemainingTime()
        // Stop countdown và cancel alarm
        stopCountdown()
        cancelAllAlarms() // QUAN TRỌNG
        updateNotification()
        stopSound()
        Log.d("TimerService", "Timer paused at $remainingSeconds seconds")
    }

    private fun resumeTimer() {
        hasTimerEnded = false
        isPaused = false
        lastUpdateTime = System.currentTimeMillis()

        // VALIDATE remaining time
        if (remainingSeconds <= 0) {
            Log.w("TimerService", "No time left to resume")
            onTimerCompleted()
            return
        }

        // Reschedule alarm và start countdown
        scheduleBackupAlarm()
        startCountdown()

        updateNotification()
        Log.d("TimerService", "Timer resumed from $remainingSeconds seconds")
    }

    private fun stopTimer() {
        Log.d("TimerService", "Stopping timer...")
        cleanupEverything()
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

    private fun updateRemainingTime() {
        if (isPaused || !isRunning) return

        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = ((currentTime - lastUpdateTime) / 1000).toInt()
        remainingSeconds = maxOf(0, remainingSeconds - elapsedSeconds)
        lastUpdateTime = currentTime

        Log.d("TimerService", "Time updated: remaining=$remainingSeconds")
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
        // ALWAYS cancel first
        cancelAllAlarms()

        // Validate remaining time
        if (remainingSeconds <= 0) {
            Log.w("TimerService", "Not scheduling alarm - no time remaining")
            return
        }

        try {
            val intent = Intent(this, TimerBackupReceiver::class.java).apply {
                putExtra(EXTRA_SOUND_URI, soundUri?.toString())
                putExtra(EXTRA_SOUND_RES_ID, soundResId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this, BACKUP_ALARM_ID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
            )

            val triggerTime = System.currentTimeMillis() + (remainingSeconds * 1000L)

            // VALIDATION - đảm bảo trigger time trong tương lai
            if (triggerTime <= System.currentTimeMillis() + 1000) {
                Log.w("TimerService", "Alarm trigger time too close, completing now")
                onTimerCompleted()
                return
            }

            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )

            Log.d("TimerService", "Backup alarm scheduled for $remainingSeconds seconds (at $triggerTime)")
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
        stopCountdown() // Ensure no duplicate

        countdownHandler = Handler(Looper.getMainLooper())
        countdownRunnable = object : Runnable {
            override fun run() {
                when {
                    !isRunning || isCompleted -> {
                        Log.d("TimerService", "Countdown stopped")
                        return
                    }
                    isPaused -> {
                        // Khi pause, chỉ check lại
                        countdownHandler?.postDelayed(this, 1000)
                    }
                    remainingSeconds > 0 -> {
                        remainingSeconds--
                        lastUpdateTime = System.currentTimeMillis()
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
        if (hasTimerEnded) {
            Log.d("TimerService", "Timer was manually paused/stopped — skip onTimerCompleted()")
            return
        }
        if (isCompleted) {
            Log.w("TimerService", "Already completed, ignoring")
            return
        }

        Log.d("TimerService", "Timer completed!")
        hasTimerEnded = true
        isCompleted = true
        isRunning = false
        cancelAllAlarms() // No more backup needed

        // Show completion notification
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
                    Log.w("TimerService", "Custom URI failed, using built-in")
                    setBuiltInDataSource()
                }
            }
            else -> setBuiltInDataSource()
        }
    }

    private fun MediaPlayer.setBuiltInDataSource() {
        val afd = resources.openRawResourceFd(soundResId)
        afd?.let {
            setDataSource(it.fileDescriptor, it.startOffset, it.length)
            it.close()
        }
    }

    private fun playFallbackSound() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.astro)?.apply {
                isLooping = true
                start()
                Log.d("TimerService", "Fallback sound started")
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Fallback sound also failed", e)
        }
    }

    private fun stopSound() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                    Log.d("TimerService", "Sound stopped")
                }
                player.release()
                Log.d("TimerService", "MediaPlayer released")
                mediaPlayer = null
            } ?: run {
                Log.d("TimerService", "No mediaPlayer to stop")
            }
        } catch (e: Exception) {
            Log.w("TimerService", "Error stopping sound", e)
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
            .setContentTitle(if (isPaused) "Timer Paused" else "Timer Running")
            .setContentText("Time remaining: ${formatTime(remainingSeconds)}")
            .setContentIntent(contentIntent)
            .setColor(Color.parseColor("#76E0C1"))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(R.drawable.ic_stopwatch_enable, pauseResumeText, pauseResumeIntent)
            .addAction(R.drawable.ic_stopwatch_enable, "Stop", stopIntent)
            .build()
    }

    private fun buildCompletionNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, packageManager.getLaunchIntentForPackage(packageName),
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
            .setContentIntent(stopSoundIntent)
            .setColor(Color.parseColor("#76E0C1"))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .addAction(R.drawable.ic_stopwatch_enable, "Stop Sound", stopSoundIntent)
            .addAction(R.drawable.ic_timer_enable, "Open App", contentIntent)
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

    override fun onDestroy() {
        super.onDestroy()
        cleanupEverything()
        Log.d("TimerService", "Service destroyed")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("TimerService", "Task removed - service continues with alarm backup")
    }
}