package net.android.lastversion.stopwatch

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import net.android.lastversion.R
import kotlin.math.max

class StopwatchService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    // thời gian đã tích luỹ (ms) + mốc bắt đầu lần chạy hiện tại (ms)
    private var baseAccumulatedMs = 0L
    private var startedAtMs = 0L
    private var isForeground = false

    private val ticker = object : Runnable {
        override fun run() {
            if (running) {
                sendTickBroadcast()
                // cập nhật nội dung thông báo mỗi ~1s
                if (isForeground) {
                    startForeground(StopwatchConst.NOTI_ID, buildNotification())
                }
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            StopwatchConst.ACTION_START -> start()
            StopwatchConst.ACTION_RESUME -> resume()
            StopwatchConst.ACTION_PAUSE -> pause()
            StopwatchConst.ACTION_STOP  -> stopAll()
            StopwatchConst.ACTION_GET_STATE -> sendCurrentState()

            StopwatchConst.ACTION_PROMOTE_FOREGROUND -> {
                if (!isForeground) {
                    isForeground = true
                    startForeground(StopwatchConst.NOTI_ID, buildNotification())
                }
            }

            StopwatchConst.ACTION_DEMOTE_FOREGROUND -> {
                if (isForeground && !running) {
                    // Chỉ demote nếu không đang chạy
                    isForeground = false
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }
        }
        return START_STICKY
    }

    private fun start() {
        if (!running && elapsedMs() == 0L) {
            baseAccumulatedMs = 0L
            startedAtMs = SystemClock.elapsedRealtime()
            running = true

            // Bắt buộc chạy foreground khi start
            isForeground = true
            startForeground(StopwatchConst.NOTI_ID, buildNotification())

            handler.removeCallbacks(ticker)
            handler.post(ticker)
        } else {
            // nếu đã chạy trước đó mà bấm START, coi như RESUME
            resume()
        }
    }

    private fun resume() {
        if (!running) {
            startedAtMs = SystemClock.elapsedRealtime()
            running = true

            // Chạy foreground khi resume
            isForeground = true
            startForeground(StopwatchConst.NOTI_ID, buildNotification())

            handler.removeCallbacks(ticker)
            handler.post(ticker)
        }
    }

    private fun pause() {
        if (running) {
            baseAccumulatedMs = elapsedMs()
            running = false
            handler.removeCallbacks(ticker)

            // Vẫn giữ foreground khi pause (hiển thị nút Resume/Stop)
            if (isForeground) {
                startForeground(StopwatchConst.NOTI_ID, buildNotification())
            }

            // Gửi broadcast state pause
            sendStateBroadcast("PAUSED")
        }
    }

    private fun stopAll() {
        running = false
        handler.removeCallbacks(ticker)
        baseAccumulatedMs = 0L
        startedAtMs = 0L

        // Gửi broadcast state stop
        sendStateBroadcast("STOPPED")

        if (isForeground) {
            isForeground = false
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    private fun elapsedMs(): Long {
        return if (running) {
            baseAccumulatedMs + (SystemClock.elapsedRealtime() - startedAtMs)
        } else baseAccumulatedMs
    }

    private fun buildNotification(): Notification {
        val elapsed = max(0L, elapsedMs() / 1000L)
        val hh = elapsed / 3600
        val mm = (elapsed % 3600) / 60
        val ss = elapsed % 60
        val content = String.format("%02d:%02d:%02d", hh, mm, ss)

        // Intent để pause/resume
        val intentPauseOrResume = Intent(this, StopwatchService::class.java).apply {
            action = if (running) StopwatchConst.ACTION_PAUSE else StopwatchConst.ACTION_RESUME
        }
        val piPauseOrResume = PendingIntent.getService(
            this, 1, intentPauseOrResume,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent để stop
        val intentStop = Intent(this, StopwatchService::class.java).apply {
            action = StopwatchConst.ACTION_STOP
        }
        val piStop = PendingIntent.getService(
            this, 2, intentStop,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent để mở app
        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val piOpen = PendingIntent.getActivity(
            this, 3, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, StopwatchConst.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_enable) // Đảm bảo có icon này
            .setContentTitle(getString(R.string.stopwatch_notification))
            .setContentText(content)
            .setContentIntent(piOpen)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Thêm action buttons
        if (running) {
            builder.addAction(
                R.drawable.ic_pause, // Đảm bảo có icon này  
                getString(R.string.pause),
                piPauseOrResume
            )
        } else {
            builder.addAction(
                R.drawable.ic_play_arrow, // Đảm bảo có icon này
                getString(R.string.resume),
                piPauseOrResume
            )
        }

        builder.addAction(
            R.drawable.ic_stop, // Đảm bảo có icon này
            getString(R.string.stop_),
            piStop
        )

        return builder.build()
    }

    private fun sendTickBroadcast() {
        val sec = max(0L, elapsedMs() / 1000L).toInt()
        val i = Intent(StopwatchConst.ACTION_TICK).apply {
            putExtra(StopwatchConst.EXTRA_ELAPSED, sec)
            putExtra("isRunning", running)
        }
        sendBroadcast(i)
    }

    private fun sendStateBroadcast(state: String) {
        val sec = max(0L, elapsedMs() / 1000L).toInt()
        val i = Intent(StopwatchConst.ACTION_TICK).apply {
            putExtra(StopwatchConst.EXTRA_ELAPSED, sec)
            putExtra("isRunning", running)
            putExtra("state", state)
        }
        sendBroadcast(i)
    }

    private fun sendCurrentState() {
        val sec = max(0L, elapsedMs() / 1000L).toInt()
        val i = Intent(StopwatchConst.ACTION_TICK).apply {
            putExtra(StopwatchConst.EXTRA_ELAPSED, sec)
            putExtra("isRunning", running)
            putExtra("state", if (running) "RUNNING" else if (sec > 0) "PAUSED" else "STOPPED")
        }
        sendBroadcast(i)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(
                StopwatchConst.CHANNEL_ID,
                StopwatchConst.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW // không kêu, chỉ hiển thị
            ).apply {
                description = "Stopwatch notification"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    override fun onBind(intent: Intent?) = null
}