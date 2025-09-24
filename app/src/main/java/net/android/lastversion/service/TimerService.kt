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

/**
 * ForegroundService that handles timer countdown, notifications with controls, and sound playback
 * Uses AlarmManager backup to guarantee timer completion even if service is killed
 */
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

    // Sound config
    private var soundUri: Uri? = null
    private var soundResId = R.raw.astro
    private var mediaPlayer: MediaPlayer? = null

    // Timer components
    private var countdownHandler: Handler? = null
    private var countdownRunnable: Runnable? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // AlarmManager backup
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
                enableVibration(false) // We handle vibration manually
                setBypassDnd(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializeComponents() {
        // WakeLock to prevent service from being killed
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TimerService::WakeLock")

        // AlarmManager for backup guarantee
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private fun handleAction(intent: Intent?) {
        val action = intent?.action ?: return

        Log.d("TimerService", "Handling action: $action")

        when (action) {
            ACTION_START -> startTimer(intent)
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimer()
        }
    }

    private fun startTimer(intent: Intent) {
        if (isRunning) {
            Log.w("TimerService", "Timer already running")
            return
        }

        // Get timer configuration
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

        // Initialize timer
        isRunning = true
        isPaused = false

        // Acquire wake lock
        acquireWakeLock()

        // Setup AlarmManager backup
        scheduleBackupAlarm()

        // Start foreground
        startForeground(NOTIFICATION_ID, buildNotification())

        // Start countdown
        startCountdown()

        Log.d("TimerService", "Timer started: $totalSeconds seconds")
    }

    private fun pauseTimer() {
        if (!isRunning || isPaused) return

        isPaused = true
        stopCountdown()
        cancelBackupAlarm() // Cancel alarm during pause

        updateNotification()
        Log.d("TimerService", "Timer paused at $remainingSeconds seconds")
    }

    private fun resumeTimer() {
        if (!isRunning || !isPaused) return

        isPaused = false
        scheduleBackupAlarm() // Reschedule for remaining time
        startCountdown()

        updateNotification()
        Log.d("TimerService", "Timer resumed from $remainingSeconds seconds")
    }

    private fun stopTimer() {
        isRunning = false
        isPaused = false

        stopCountdown()
        cancelBackupAlarm()
        stopSound()
        releaseWakeLock()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        Log.d("TimerService", "Timer stopped")
    }

    private fun acquireWakeLock() {
        try {
            wakeLock?.acquire((totalSeconds * 1000L) + 30000L) // 30s buffer
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
            val intent = Intent(this, TimerBackupReceiver::class.java).apply {
                putExtra(EXTRA_SOUND_URI, soundUri?.toString())
                putExtra(EXTRA_SOUND_RES_ID, soundResId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this, BACKUP_ALARM_ID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
            )

            val triggerTime = System.currentTimeMillis() + (remainingSeconds * 1000L)

            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )

            Log.d("TimerService", "Backup alarm scheduled for $remainingSeconds seconds")
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to schedule backup alarm", e)
        }
    }

    private fun cancelBackupAlarm() {
        try {
            val intent = Intent(this, TimerBackupReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this, BACKUP_ALARM_ID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
            )

            alarmManager?.cancel(pendingIntent)
            Log.d("TimerService", "Backup alarm cancelled")
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to cancel backup alarm", e)
        }
    }

    private fun startCountdown() {
        countdownHandler = Handler(Looper.getMainLooper())
        countdownRunnable = object : Runnable {
            override fun run() {
                when {
                    !isRunning -> return
                    isPaused -> countdownHandler?.postDelayed(this, 500) // Check again when paused
                    remainingSeconds > 0 -> {
                        remainingSeconds--
                        updateNotification()
                        countdownHandler?.postDelayed(this, 1000)
                    }
                    else -> onTimerCompleted()
                }
            }
        }
        countdownHandler?.post(countdownRunnable!!)
    }

    private fun stopCountdown() {
        countdownRunnable?.let { countdownHandler?.removeCallbacks(it) }
    }

    private fun onTimerCompleted() {
        Log.d("TimerService", "Timer completed!")

        isRunning = false
        cancelBackupAlarm() // We completed normally, no need for backup

        // Update to completion notification
        val completionNotification = buildCompletionNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, completionNotification)

        // Start foreground with completion notification to keep service alive for sound
        startForeground(NOTIFICATION_ID, completionNotification)

        // Play completion sound
        playCompletionSound()
    }

    private fun playCompletionSound() {
        try {
            stopSound() // Clean up any existing player

            mediaPlayer = MediaPlayer().apply {
                // Set for alarm playback
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

                // Set data source
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
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w("TimerService", "Error stopping sound", e)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
        )

        // Pause/Resume action
        val pauseResumeAction = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        val pauseResumeText = if (isPaused) "Resume" else "Pause"
        val pauseResumeIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TimerService::class.java).apply { action = pauseResumeAction },
            PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
        )

        // Stop action
        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, TimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_enable)
            .setContentTitle("Timer Running")
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
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
        )

        val stopSoundIntent = PendingIntent.getService(
            this, 3,
            Intent(this, TimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_enable)
            .setContentTitle("Time's Up!")
            .setContentText("Your ${formatTime(totalSeconds)} timer has finished")
            .setContentIntent(stopSoundIntent) // Tap notification to stop sound
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
        stopCountdown()
        cancelBackupAlarm()
        stopSound()
        releaseWakeLock()
        Log.d("TimerService", "Service destroyed")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Don't stop service - let it continue in background with AlarmManager backup
        Log.d("TimerService", "Task removed - service continues with alarm backup")
    }
}