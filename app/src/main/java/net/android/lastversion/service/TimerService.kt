package net.android.lastversion.service

// app/src/main/java/…/service/TimerService.kt


import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import androidx.core.app.NotificationCompat
import net.android.lastversion.R
import net.android.lastversion.receiver.TimeUpReceiver
import java.util.concurrent.TimeUnit

/**
 * Foreground Service chạy timer:
 * - Hiển thị notification đang chạy (không bị hệ thống dọn dễ dàng).
 * - Cập nhật mỗi giây số còn lại trên notification.
 * - Đến giờ: phát thông báo Time’s up, kêu/rung.
 * - Kèm AlarmManager fallback: nếu service bị kill, Alarm vẫn bắn Time’s up.
 */
class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTI_ID = 1001

        const val ACTION_START = "TimerService.ACTION_START"
        const val ACTION_STOP = "TimerService.ACTION_STOP"

        const val EXTRA_DURATION_MS = "duration_ms"
    }

    private var endAtElapsed: Long = 0L            // mốc kết thúc theo elapsedRealtime
    private var countDown: CountDownTimer? = null  // cập nhật mỗi giây

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel() // Bắt buộc Android 8+
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val duration = intent.getLongExtra(EXTRA_DURATION_MS, 0L)
                if (duration > 0L) startTimer(duration)
            }
            ACTION_STOP -> {
                stopSelfSafely()
            }
        }
        // START_STICKY: nếu hệ thống kill vì thiếu RAM, service có thể được tạo lại.
        return START_STICKY
    }

    /** Bắt đầu timer + vào foreground + set Alarm fallback */
    private fun startTimer(durationMs: Long) {
        // Tính mốc kết thúc theo elapsedRealtime (không lệch khi người dùng đổi giờ hệ thống)
        endAtElapsed = SystemClock.elapsedRealtime() + durationMs

        // Vào foreground với notification ban đầu
        startForeground(NOTI_ID, buildNotification(durationMs))

        // Hẹn Alarm bắn đúng lúc hết giờ (fallback khi service bị kill)
        scheduleAlarm(endAtElapsed)

        // Huỷ timer cũ nếu có
        countDown?.cancel()
        // Tạo CountDownTimer để tự update notification mỗi giây
        countDown = object : CountDownTimer(durationMs, 1000L) {
            override fun onTick(msLeft: Long) {
                // Cập nhật notification hiển thị thời gian còn lại
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTI_ID, buildNotification(msLeft))
            }

            override fun onFinish() {
                // Đích đến: hiển thị thông báo "Time’s up!" + âm/rung (do Receiver phụ trách)
                triggerTimeUpNow()
                stopSelfSafely()
            }
        }.start()
    }

    /** Tạo/Update notification đếm ngược */
    private fun buildNotification(msLeft: Long): Notification {
        val remaining = format(msLeft)

        // PendingIntent mở app (hoặc activity chính) khi chạm
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or pendingMutable()
        )

        // PendingIntent cho nút STOP ngay trên notification
        val stopIntent = Intent(this, TimerService::class.java).apply { action = ACTION_STOP }
        val stopPI = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingMutable()
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_enable)           // TODO: icon của bạn
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Time remaining: $remaining")
            .setOngoing(true)                            // Không cho vuốt tắt
            .setOnlyAlertOnce(true)                      // Update êm
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_stopwatch_enable, "Stop", stopPI)
            .setColor(Color.parseColor("#76E0C1"))
            .build()
    }

    /** Channel cho Android 8+ */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "Timer",
                NotificationManager.IMPORTANCE_LOW  // thấp để không kêu mỗi lần update
            )
            ch.description = "Foreground timer channel"
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(ch)
        }
    }

    /** Hẹn Alarm đúng lúc hết giờ (fallback nếu service bị kill/Doze) */
    private fun scheduleAlarm(endAtElapsed: Long) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, TimeUpReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            this, 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingImmutable()
        )
        am.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            endAtElapsed,
            pi
        )
    }

    /** Gửi broadcast ngay lập tức để TimeUpReceiver hiển thị cảnh báo */
    private fun triggerTimeUpNow() {
        sendBroadcast(Intent(this, TimeUpReceiver::class.java))
    }

    /** Dừng service gọn gàng */
    private fun stopSelfSafely() {
        countDown?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        // Huỷ alarm fallback (khỏi bắn trễ)
        cancelAlarm()
    }

    private fun cancelAlarm() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, TimeUpReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            this, 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingImmutable()
        )
        am.cancel(pi)
    }

    /** Định dạng mm:ss / hh:mm:ss tuỳ độ dài */
    private fun format(ms: Long): String {
        var sec = TimeUnit.MILLISECONDS.toSeconds(ms)
        val h = sec / 3600
        sec %= 3600
        val m = sec / 60
        val s = sec % 60
        return if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
        else       String.format("%02d:%02d", m, s)
    }

    /** Flags PendingIntent tuỳ API */
    private fun pendingMutable(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

    private fun pendingImmutable(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
}
