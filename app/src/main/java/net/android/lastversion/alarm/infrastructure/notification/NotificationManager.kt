package net.android.lastversion.alarm.infrastructure.notification

interface NotificationManager {
    fun showAlarmNotification(
        alarmId: Int,
        title: String,
        message: String,
        isVibrationEnabled: Boolean,
        isSoundEnabled: Boolean,
        isSnoozeEnabled: Boolean
    )

    fun cancelNotification(alarmId: Int)

    fun showSnoozeNotification(
        alarmId: Int,
        title: String,
        snoozeTime: Long
    )
}