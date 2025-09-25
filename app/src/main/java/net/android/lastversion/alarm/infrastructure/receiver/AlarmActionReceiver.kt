package net.android.lastversion.alarm.infrastructure.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.android.lastversion.alarm.infrastructure.notification.NotificationManagerImpl

class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        if (alarmId == -1) return

        val notificationManager = NotificationManagerImpl(context)

        when (intent.action) {
            "DISMISS_ALARM" -> {
                notificationManager.cancelNotification(alarmId)
            }

            "SNOOZE_ALARM" -> {
                notificationManager.cancelNotification(alarmId)

                val title = intent.getStringExtra("alarm_title") ?: "Alarm"
                val snoozeTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes

                // Show snooze notification
                notificationManager.showSnoozeNotification(alarmId, title, snoozeTime)

                // Schedule snooze alarm
                CoroutineScope(Dispatchers.IO).launch {
                    // Implement snooze scheduling logic here
                    // This would involve rescheduling the alarm for 5 minutes later
                }
            }
        }
    }
}