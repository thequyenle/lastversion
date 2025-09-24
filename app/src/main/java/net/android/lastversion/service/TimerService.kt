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

class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val RUNNING_NOTIFICATION_ID = 1001
        const val FINISHED_NOTIFICATION_ID = 1002

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CONTINUE = "ACTION_CONTINUE"
        const val ACTION_RESTART = "ACTION_RESTART"
        const val ACTION_STOP_SOUND = "ACTION_STOP_SOUND"

        const val EXTRA_SECONDS = "EXTRA_SECONDS"
        const val EXTRA_SOUND_URI = "EXTRA_SOUND_URI"
        const val EXTRA_SOUND_RES_ID = "EXTRA_SOUND_RES_ID"

        const val MSG_REGISTER_CLIENT = 100
        const val MSG_UNREGISTER_CLIENT = 101
        const val MSG_TICK = 102
        const val MSG_FINISHED = 103
    }

    private var totalSeconds = 0
    private var currentSeconds = 0
    private var isPaused = false
    private var isRunning = false

    private var soundUri: Uri? = null
    private var soundResId: Int = R.raw.astro
    private var mediaPlayer: MediaPlayer? = null

    private var handler: Handler? = null
    private var runnable: Runnable? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val messenger = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_REGISTER_CLIENT -> Log.d("TimerService", "Client registered")
                MSG_UNREGISTER_CLIENT -> Log.d("TimerService", "Client unregistered")
            }
        }
    })

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initWakeLock()
        Log.d("TimerService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleAction(intent?.action, intent)
        return START_STICKY
    }

    private fun initWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TimerService::WakeLock"
            )
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to initialize wake lock", e)
        }
    }

    private fun handleAction(action: String?, intent: Intent?) {
        when (action) {
            ACTION_START -> startTimer(intent)
            ACTION_STOP -> pauseTimer()
            ACTION_CONTINUE -> resumeTimer()
            ACTION_RESTART, ACTION_STOP_SOUND -> stopAndExit()
        }
    }

    private fun startTimer(intent: Intent?) {
        if (isRunning) return

        try {
            totalSeconds = intent?.getIntExtra(EXTRA_SECONDS, 0) ?: 0
            currentSeconds = totalSeconds

            val uriStr = intent?.getStringExtra(EXTRA_SOUND_URI)
            soundUri = if (!uriStr.isNullOrEmpty()) Uri.parse(uriStr) else null
            soundResId = intent?.getIntExtra(EXTRA_SOUND_RES_ID, R.raw.astro) ?: R.raw.astro

            if (totalSeconds > 0) {
                isRunning = true
                isPaused = false

                acquireWakeLock()
                startForeground(RUNNING_NOTIFICATION_ID, buildRunningNotification())
                startCountdown()

                Log.d("TimerService", "Timer started: $totalSeconds seconds")
            } else {
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Error starting timer", e)
            stopSelf()
        }
    }

    private fun pauseTimer() {
        if (!isRunning || isPaused) return

        try {
            isPaused = true
            handler?.removeCallbacks(runnable!!)
            updateNotification("Timer Paused", formatTime(currentSeconds))
            Log.d("TimerService", "Timer paused")
        } catch (e: Exception) {
            Log.e("TimerService", "Error pausing timer", e)
        }
    }

    private fun resumeTimer() {
        if (!isRunning || !isPaused) return

        try {
            isPaused = false
            startCountdown()
            Log.d("TimerService", "Timer resumed")
        } catch (e: Exception) {
            Log.e("TimerService", "Error resuming timer", e)
        }
    }

    private fun stopAndExit() {
        try {
            stopTimer()
            stopSelf()
        } catch (e: Exception) {
            Log.e("TimerService", "Error stopping and exiting", e)
        }
    }

    private fun stopTimer() {
        isRunning = false
        isPaused = false

        try {
            handler?.removeCallbacks(runnable!!)
        } catch (e: Exception) {
            Log.w("TimerService", "Error removing callbacks", e)
        }

        stopSound()
        stopForeground(STOP_FOREGROUND_REMOVE)
        releaseWakeLock()

        Log.d("TimerService", "Timer stopped")
    }

    private fun acquireWakeLock() {
        try {
            wakeLock?.acquire((totalSeconds * 1000L) + 60000L)
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to acquire wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Failed to release wake lock", e)
        }
    }

    private fun startCountdown() {
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                try {
                    when {
                        !isRunning -> return
                        isPaused -> handler?.postDelayed(this, 250)
                        currentSeconds > 0 -> {
                            currentSeconds--
                            updateNotification("Timer Running", "Time remaining: ${formatTime(currentSeconds)}")
                            handler?.postDelayed(this, 1000)
                        }
                        else -> onTimerFinished()
                    }
                } catch (e: Exception) {
                    Log.e("TimerService", "Error in countdown", e)
                }
            }
        }
        handler?.post(runnable!!)
    }

    private fun onTimerFinished() {
        try {
            isRunning = false
            showFinishedNotification()
            playSound()
            Log.d("TimerService", "Timer finished")
        } catch (e: Exception) {
            Log.e("TimerService", "Error handling timer finish", e)
        }
    }

    private fun playSound() {
        try {
            stopSound()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(createAudioAttributes())
                setDataSourceSafely()
                isLooping = true
                prepare()
                start()
                Log.d("TimerService", "Sound started")
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Error playing sound", e)
            playFallbackSound()
        }
    }

    private fun createAudioAttributes(): AudioAttributes {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        } else {
            AudioAttributes.Builder().build()
        }
    }

    private fun MediaPlayer.setDataSourceSafely() {
        when {
            soundUri != null -> {
                try {
                    setDataSource(applicationContext, soundUri!!)
                } catch (e: Exception) {
                    Log.w("TimerService", "Custom URI failed, using built-in", e)
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
            Log.e("TimerService", "Fallback sound failed", e)
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

    private fun buildRunningNotification(): Notification {
        return buildNotification(
            "Timer Running",
            "Time remaining: ${formatTime(currentSeconds)}",
            true
        )
    }

    private fun buildNotification(title: String, content: String, ongoing: Boolean): Notification {
        val contentIntent = createContentIntent()
        val stopIntent = createStopIntent(ongoing)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_enable)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentIntent)
            .setColor(Color.parseColor("#76E0C1"))
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .apply {
                if (ongoing) {
                    setOngoing(true)
                    setPriority(NotificationCompat.PRIORITY_HIGH)
                    setCategory(NotificationCompat.CATEGORY_ALARM)
                    addAction(R.drawable.ic_stopwatch_enable, "Stop", stopIntent)
                } else {
                    setAutoCancel(false)
                    setPriority(NotificationCompat.PRIORITY_MAX)
                    setCategory(NotificationCompat.CATEGORY_ALARM)
                    setFullScreenIntent(contentIntent, true)
                    setDefaults(NotificationCompat.DEFAULT_ALL)
                    addAction(R.drawable.ic_stopwatch_enable, "Stop Sound", stopIntent)
                }
            }
            .build()
    }

    private fun createContentIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
        )
    }

    private fun createStopIntent(ongoing: Boolean): PendingIntent {
        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = if (ongoing) ACTION_RESTART else ACTION_STOP_SOUND
        }
        return PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
        )
    }

    private fun updateNotification(title: String, content: String) {
        try {
            val notification = buildNotification(title, content, true)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(RUNNING_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("TimerService", "Error updating notification", e)
        }
    }

    private fun showFinishedNotification() {
        try {
            val notification = buildNotification(
                "Time's Up!",
                "Your ${formatTime(totalSeconds)} timer has finished",
                false
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(FINISHED_NOTIFICATION_ID, notification)

            startForeground(FINISHED_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("TimerService", "Error showing finished notification", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Timer Channel",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Timer service notifications"
                    enableLights(true)
                    lightColor = Color.parseColor("#76E0C1")
                    enableVibration(true)
                    setBypassDnd(true)
                    setShowBadge(true)
                }

                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            } catch (e: Exception) {
                Log.e("TimerService", "Error creating notification channel", e)
            }
        }
    }

    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%d:%02d", minutes, secs)
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
        stopTimer()
        Log.d("TimerService", "Service destroyed")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("TimerService", "Task removed, service continues")
    }
}