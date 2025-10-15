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
 * Handles timer completion when app is killed
 */
class TimeUpReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "timer_alarm_channel"
        private const val NOTIFICATION_ID = 3001

        // Static MediaPlayer to control from notification actions
        private var mediaPlayer: MediaPlayer? = null

        // Method to stop sound from external calls
        fun stopSound() {
            try {
                mediaPlayer?.apply {
                    if (isPlaying) stop()
                    release()
                }
                mediaPlayer = null
                Log.d("TimeUpReceiver", "Sound stopped externally")
            } catch (e: Exception) {
                Log.w("TimeUpReceiver", "Error stopping sound externally", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("TimeUpReceiver", "Timer alarm received")

        when (intent?.action) {
            "STOP_TIMER_SOUND" -> {
                // Handle stop sound action from notification
                stopSound()

                // Cancel the notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(NOTIFICATION_ID)

                Log.d("TimeUpReceiver", "Timer sound stopped via notification")
                return
            }
            else -> {
                // Handle timer completion
                val soundUri = intent?.getStringExtra("EXTRA_SOUND_URI")
                val soundResId = intent?.getIntExtra("EXTRA_SOUND_RES_ID", R.raw.astro) ?: R.raw.astro

                createNotificationChannel(context)
                showTimerFinishedNotification(context)
                playTimerSound(context, soundUri, soundResId)
                vibrateDevice(context)
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Timer Alarm",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Timer finished notifications"
                    enableLights(true)
                    lightColor = Color.parseColor("#76E0C1")
                    enableVibration(true)
                    setBypassDnd(true)
                    setShowBadge(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }

                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(channel)

            } catch (e: Exception) {
                Log.e("TimeUpReceiver", "Error creating notification channel", e)
            }
        }
    }

    private fun showTimerFinishedNotification(context: Context) {
        try {
            // Intent to open the app
            val contentIntent = PendingIntent.getActivity(
                context, 0,
                context.packageManager.getLaunchIntentForPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
            )

            // Intent to stop sound
            val stopSoundIntent = Intent(context, TimeUpReceiver::class.java).apply {
                action = "STOP_TIMER_SOUND"
            }
            val stopSoundPendingIntent = PendingIntent.getBroadcast(
                context, 1, stopSoundIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or getPendingIntentFlags()
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer_enable)
                .setContentTitle(context.getString(R.string.time_up_notification))
                .setContentText(context.getString(R.string.your_timer_has_finished_tap_to_stop_sound))
                .setContentIntent(stopSoundPendingIntent) // Tap notification to stop sound
                .setColor(Color.parseColor("#76E0C1"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setOngoing(true)
                .setFullScreenIntent(contentIntent, true)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
                .addAction(R.drawable.ic_stopwatch_enable, context.getString(R.string.stop_sound), stopSoundPendingIntent)
                .addAction(R.drawable.ic_timer_enable, context.getString(R.string.open_app), contentIntent)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(NOTIFICATION_ID, notification)

            Log.d("TimeUpReceiver", "Timer finished notification shown")

        } catch (e: Exception) {
            Log.e("TimeUpReceiver", "Error showing notification", e)
        }
    }

    private fun playTimerSound(context: Context, soundUriStr: String?, soundResId: Int) {
        try {
            // Stop any existing sound
            stopExistingSound()

            val soundUri = if (!soundUriStr.isNullOrEmpty()) {
                try {
                    Uri.parse(soundUriStr)
                } catch (e: Exception) {
                    null
                }
            } else null

            mediaPlayer = MediaPlayer().apply {
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

                // Set data source
                when {
                    soundUri != null -> {
                        try {
                            setDataSource(context, soundUri)
                            Log.d("TimeUpReceiver", "Using custom sound URI")
                        } catch (e: Exception) {
                            Log.w("TimeUpReceiver", "Custom URI failed, using built-in", e)
                            setBuiltInSound(context, soundResId)
                        }
                    }
                    else -> setBuiltInSound(context, soundResId)
                }

                isLooping = true
                prepareAsync()

                setOnPreparedListener { player ->
                    try {
                        player.start()
                        Log.d("TimeUpReceiver", "Timer alarm sound started")
                    } catch (e: Exception) {
                        Log.e("TimeUpReceiver", "Failed to start sound after prepare", e)
                    }
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("TimeUpReceiver", "MediaPlayer error: what=$what, extra=$extra")
                    playFallbackSound(context)
                    false
                }
            }

        } catch (e: Exception) {
            Log.e("TimeUpReceiver", "Error playing timer sound", e)
            playFallbackSound(context)
        }
    }

    private fun MediaPlayer.setBuiltInSound(context: Context, soundResId: Int) {
        val afd = context.resources.openRawResourceFd(soundResId)
        afd?.let {
            setDataSource(it.fileDescriptor, it.startOffset, it.length)
            it.close()
            Log.d("TimeUpReceiver", "Using built-in sound: $soundResId")
        }
    }

    private fun playFallbackSound(context: Context) {
        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.astro)?.apply {
                isLooping = true
                start()
                Log.d("TimeUpReceiver", "Fallback sound started")
            }
        } catch (e: Exception) {
            Log.e("TimeUpReceiver", "Fallback sound also failed", e)
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
                val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(pattern, -1)
                    vib.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(pattern, -1)
                }

                Log.d("TimeUpReceiver", "Device vibration started")
            }
        } catch (e: Exception) {
            Log.e("TimeUpReceiver", "Error vibrating device", e)
        }
    }

    private fun stopExistingSound() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
            Log.d("TimeUpReceiver", "Existing sound stopped")
        } catch (e: Exception) {
            Log.w("TimeUpReceiver", "Error stopping existing sound", e)
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