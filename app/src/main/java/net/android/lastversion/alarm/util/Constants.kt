package net.android.lastversion.alarm.util

object Constants {
    const val DATABASE_NAME = "alarm_database"
    const val ALARM_TABLE_NAME = "alarms"

    // Notification
    const val ALARM_NOTIFICATION_ID = 1001
    const val ALARM_CHANNEL_ID = "alarm_channel"

    // Days of week
    val DAYS_OF_WEEK = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val DAYS_OF_WEEK_FULL = arrayOf(
        "Sunday", "Monday", "Tuesday", "Wednesday",
        "Thursday", "Friday", "Saturday"
    )
}