package net.android.lastversion.receiver

// app/src/main/java/…/receiver/TimeUpReceiver.kt


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import net.android.lastversion.R

/**
 * Nhận tín hiệu "time's up" (từ Alarm hoặc service), hiển thị thông báo
 * và phát âm + rung ngay cả khi app đang ở nền.
 */
class TimeUpReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        // 1) Hiển thị notification Time’s up
        val nm = context.getSystemService(NotificationManager::class.java)
        val channelId = "timeup_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId, "Time’s up",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(ch)
        }
        val noti = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_timer_enable)  // TODO: icon
            .setContentTitle("Time’s up!")
            .setContentText("Your timer has finished.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(2001, noti)

        // 2) Phát âm báo
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val tone: Ringtone? = RingtoneManager.getRingtone(context, uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            tone?.play()
        } catch (_: Throwable) {}

        // 3) Rung máy
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 500, 300, 500, 300, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (_: Throwable) {}
    }
}
