package net.android.lastversion.timer.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import net.android.lastversion.R

/**
 * BroadcastReceiver that handles timer completion when the app/service is killed
 * Triggered by AlarmManager as a backup guarantee mechanism
 */
class TimerBackupReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "timer_backup_channel"
        private const val NOTIFICATION_ID = 2001

        // Static MediaPlayer so notification actions can control it
        private var backupMediaPlayer: MediaPlayer? = null

        fun stopBackupSound() {
            try {
                backupMediaPlayer?.apply {
                    if (isPlaying) stop()
                    release()
                }
                backupMediaPlayer = null
                Log.d("TimerBackupReceiver", "Backup sound stopped")
            } catch (e: Exception) {
                Log.w("TimerBackupReceiver", "Error stopping backup sound", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            "STOP_BACKUP_SOUND" -> {
                // Handle stop sound action from notification
                stopBackupSound()

                // Cancel notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID)

                Log.d("TimerBackupReceiver", "Backup sound stopped via notification")
            }
            else -> {
                // Handle timer completion - app was killed, this is our backup
                Log.d("TimerBackupReceiver", "BACKUP ALARM TRIGGERED - App was killed, playing backup sound")

                val soundUri = intent?.getStringExtra("SOUND_URI")
                val soundResId = intent?.getIntExtra("SOUND_RES_ID", R.raw.astro) ?: R.raw.astro

                createNotificationChannel(context)
                showBackupNotification(context)
                playBackupSound(context, soundUri, soundResId)
                vibrateDevice(context)
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Backup",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Timer completion notifications when app is killed"
                enableLights(true)
                lightColor = Color.parseColor("#76E0C1")
                enableVibration(true)
                setBypassDnd(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun showBackupNotification(context: Context) {
        try {
            // Intent to open app
            val openAppIntent = PendingIntent.getActivity(
                context, 0,
                context.packageManager.getLaunchIntentForPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
            )

            // Intent to stop sound
            val stopSoundIntent = Intent(context, TimerBackupReceiver::class.java).apply {
                action = "STOP_BACKUP_SOUND"
            }
            val stopSoundPendingIntent = PendingIntent.getBroadcast(
                context, 1, stopSoundIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer_enable)
                .setContentTitle("Time's Up!")
                .setContentText("Your timer finished. Tap to stop sound.")
                .setContentIntent(stopSoundPendingIntent) // Tap notification = stop sound
                .setColor(Color.parseColor("#76E0C1"))
                .setOngoing(true) // Keep persistent until user stops it
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
                .addAction(R.drawable.ic_stopwatch_enable, "Stop Sound", stopSoundPendingIntent)
                .addAction(R.drawable.ic_timer_enable, "Open App", openAppIntent)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager?.notify(NOTIFICATION_ID, notification)

            Log.d("TimerBackupReceiver", "Backup notification shown")

        } catch (e: Exception) {
            Log.e("TimerBackupReceiver", "Error showing backup notification", e)
        }
    }

    private fun playBackupSound(context: Context, soundUriString: String?, soundResId: Int) {
        try {
            stopBackupSound() // Clean up any existing player

            val soundUri = if (!soundUriString.isNullOrEmpty()) {
                try {
                    Uri.parse(soundUriString)
                } catch (e: Exception) {
                    null
                }
            } else null

            backupMediaPlayer = MediaPlayer().apply {
                // Set audio attributes for alarm
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

                // Set data source - prefer custom, fallback to built-in
                setDataSourceSafely(context, soundUri, soundResId)

                isLooping = true
                prepareAsync()

                setOnPreparedListener { player ->
                    try {
                        player.start()
                        Log.d("TimerBackupReceiver", "Backup sound started successfully")
                    } catch (e: Exception) {
                        Log.e("TimerBackupReceiver", "Failed to start backup sound", e)
                    }
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("TimerBackupReceiver", "Backup MediaPlayer error: $what, $extra")
                    playFallbackSound(context)
                    false
                }
            }

        } catch (e: Exception) {
            Log.e("TimerBackupReceiver", "Error playing backup sound", e)
            playFallbackSound(context)
        }
    }

    private fun MediaPlayer.setDataSourceSafely(context: Context, soundUri: Uri?, soundResId: Int) {
        when {
            soundUri != null -> {
                try {
                    setDataSource(context, soundUri)
                    Log.d("TimerBackupReceiver", "Using custom sound URI in backup")
                } catch (e: Exception) {
                    Log.w("TimerBackupReceiver", "Custom URI failed in backup, using built-in")
                    setBuiltInDataSource(context, soundResId)
                }
            }
            else -> setBuiltInDataSource(context, soundResId)
        }
    }

    private fun MediaPlayer.setBuiltInDataSource(context: Context, soundResId: Int) {
        val afd = context.resources.openRawResourceFd(soundResId)
        afd?.let {
            setDataSource(it.fileDescriptor, it.startOffset, it.length)
            it.close()
            Log.d("TimerBackupReceiver", "Using built-in sound in backup: $soundResId")
        }
    }

    private fun playFallbackSound(context: Context) {
        try {
            backupMediaPlayer = MediaPlayer.create(context, R.raw.astro)?.apply {
                isLooping = true
                start()
                Log.d("TimerBackupReceiver", "Backup fallback sound started")
            }
        } catch (e: Exception) {
            Log.e("TimerBackupReceiver", "Backup fallback sound failed", e)
        }
    }

    private fun vibrateDevice(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let { vib ->
                val pattern = longArrayOf(0, 800, 400, 800, 400, 800)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(pattern, -1)
                    vib.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(pattern, -1)
                }

                Log.d("TimerBackupReceiver", "Backup vibration started")
            }
        } catch (e: Exception) {
            Log.e("TimerBackupReceiver", "Error with backup vibration", e)
        }
    }

    private fun getPendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }
}