package net.android.last.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
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
        // Actions
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CONTINUE = "ACTION_CONTINUE"
        const val ACTION_RESTART = "ACTION_RESTART"
        const val ACTION_STOP_SOUND = "ACTION_STOP_SOUND"

        // Extras
        const val EXTRA_SECONDS = "EXTRA_SECONDS"
        const val EXTRA_SOUND_URI = "EXTRA_SOUND_URI"
        const val EXTRA_SOUND_RES_ID = "EXTRA_SOUND_RES_ID"

        // Messenger messages
        const val MSG_REGISTER_CLIENT = 100
        const val MSG_UNREGISTER_CLIENT = 101
        const val MSG_TICK = 102
        const val MSG_FINISHED = 103
    }

    // ===== State
    private var totalSeconds = 0
    private var currentSeconds = 0
    private var isPaused = false

    // ===== Sound
    private var soundUri: Uri? = null
    private var soundResId: Int? = null
    private var mediaPlayer: MediaPlayer? = null

    // ===== Timer loop
    private var handler: Handler? = null
    private var runnable: Runnable? = null

    // ===== IPC
    private var clientMessenger: Messenger? = null
    private val messenger = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_REGISTER_CLIENT -> {
                    clientMessenger = msg.replyTo
                    // Gửi tick đầu để Fragment cập nhật ngay
                    sendTick()
                }
                MSG_UNREGISTER_CLIENT -> if (clientMessenger == msg.replyTo) {
                    clientMessenger = null
                }
            }
        }
    })

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                totalSeconds = intent.getIntExtra(EXTRA_SECONDS, 0)
                currentSeconds = totalSeconds

                // Nhạc: nhận cả Uri hoặc resId
                val uriStr = intent.getStringExtra(EXTRA_SOUND_URI)
                val resId = intent.getIntExtra(EXTRA_SOUND_RES_ID, -1)
                soundUri = uriStr?.let { Uri.parse(it) }
                soundResId = if (resId != -1) resId else null

                startForegroundWithNotification()
                startCountdown()
            }
            ACTION_STOP -> isPaused = true
            ACTION_CONTINUE -> {
                isPaused = false
                handler?.post(runnable!!)
            }
            ACTION_RESTART, ACTION_STOP_SOUND -> {
                stopSound()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startCountdown() {
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                when {
                    !isPaused && currentSeconds > 0 -> {
                        currentSeconds--
                        sendTick()
                        handler?.postDelayed(this, 1000)
                    }
                    currentSeconds == 0 -> {
                        sendTick()              // tick cuối (0)
                        notifyFinished()
                        playSound(soundUri, soundResId)
                        // dừng loop — chờ người dùng bấm Stop/Restart
                    }
                    else -> {
                        // đang pause → kiểm tra lại sau
                        handler?.postDelayed(this, 250)
                    }
                }
            }
        }
        handler?.post(runnable!!)
    }

    private fun sendTick() {
        try {
            clientMessenger?.send(Message.obtain(null, MSG_TICK, currentSeconds, totalSeconds))
        } catch (e: RemoteException) {
            Log.w("TimerService", "sendTick failed; clearing client", e)
            clientMessenger = null
        }
    }

    private fun notifyFinished() {
        try {
            clientMessenger?.send(Message.obtain(null, MSG_FINISHED, 0, totalSeconds))
        } catch (_: Exception) {}
    }

    private fun playSound(uri: Uri? = null, resId: Int? = null) {
        try {
            if (mediaPlayer != null) return
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                when {
                    uri != null -> setDataSource(applicationContext, uri)
                    resId != null -> {
                        val afd = resources.openRawResourceFd(resId) ?: return
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                    }
                    else -> return
                }
                isLooping = true
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("TimerService", "Error playing sound", e)
        }
    }

    private fun stopSound() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    private fun startForegroundWithNotification() {
        val channelId = "timer_channel"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Timer Channel", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Timer Running")
            .setContentText("Countdown in progress")
            .setSmallIcon(R.drawable.ic_timer_enable)
            .build()
        startForeground(1, notif)
    }

    override fun onDestroy() {
        handler?.removeCallbacks(runnable ?: return)
        stopSound()
        super.onDestroy()
    }
}
