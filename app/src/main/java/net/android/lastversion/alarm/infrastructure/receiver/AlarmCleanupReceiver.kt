package net.android.lastversion.alarm.infrastructure.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver to cleanup alarm sound and vibration
 * This is called after 30 seconds to stop immediate playback if AlarmRingingActivity doesn't start
 */
class AlarmCleanupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "🧹 AlarmCleanupReceiver triggered with action: $action")

        when (action) {
            "ACTION_CLEANUP_ALARM_SOUND" -> {
                // Stop the immediate sound and vibration from AlarmReceiver
                AlarmReceiver.stopImmediateSoundAndVibrationStatic()
                Log.d(TAG, "✅ Immediate alarm sound and vibration cleaned up")
            }
        }
    }

    companion object {
        private const val TAG = "AlarmCleanupReceiver"
    }
}