package net.android.lastversion.alarm.infrastructure.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import net.android.lastversion.R

/**
 * Foreground service to ensure alarm reliability on Android 9-13
 * This service keeps the app process alive when alarms are scheduled
 */
class AlarmForegroundService : Service() {

    companion object {
        private const val TAG = "AlarmForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "alarm_service_channel"
        private const val ACTION_START = "ACTION_START_ALARM_SERVICE"
        private const val ACTION_STOP = "ACTION_STOP_ALARM_SERVICE"

        /**
         * Start the foreground service to ensure alarm reliability
         */
        fun startService(context: Context) {
            Log.d(TAG, "Starting AlarmForegroundService")
            val intent = Intent(context, AlarmForegroundService::class.java).apply {
                action = ACTION_START
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop the foreground service
         */
        fun stopService(context: Context) {
            Log.d(TAG, "Stopping AlarmForegroundService")
            val intent = Intent(context, AlarmForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AlarmForegroundService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                startForegroundWithNotification()
                return START_STICKY // Service will be restarted if killed
            }
            ACTION_STOP -> {
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                // Default behavior - start service
                startForegroundWithNotification()
                return START_STICKY
            }
        }
    }

    private fun startForegroundWithNotification() {
        val notification = createNotification()
        try {
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Foreground service started with notification")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Alarm Service")
            .setContentText("Keeping alarms active in background")
            .setSmallIcon(R.drawable.ic_alarm_enable)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps alarm service running for reliability"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AlarmForegroundService destroyed")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "Task removed - service will be restarted due to START_STICKY")
        super.onTaskRemoved(rootIntent)
    }
}