package net.android.lastversion.alarm.presentation.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {

    fun getCurrentTime12H(): Triple<Int, Int, String> {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)
        val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

        return Triple(if (hour == 0) 12 else hour, minute, amPm)
    }

    fun formatTimeString(hour: Int, minute: Int, amPm: String): String {
        return String.format("%02d:%02d %s", hour, minute, amPm)
    }

    fun formatActiveDaysText(activeDays: BooleanArray): String {
        if (activeDays.none { it }) return "Never"
        if (activeDays.all { it }) return "Every day"

        val weekdays = activeDays.slice(1..5).all { it } // Mon-Fri
        val weekends = activeDays[0] && activeDays[6] // Sun, Sat

        return when {
            weekdays && !weekends -> "Weekdays"
            weekends && !weekdays -> "Weekends"
            else -> {
                val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                activeDays.mapIndexed { index, active ->
                    if (active) dayNames[index] else null
                }.filterNotNull().joinToString(", ")
            }
        }
    }

    fun parseTimeToMillis(hour: Int, minute: Int, amPm: String): Long {
        val calendar = Calendar.getInstance()

        val hour24 = when {
            amPm == "AM" && hour == 12 -> 0
            amPm == "AM" -> hour
            amPm == "PM" && hour == 12 -> 12
            else -> hour + 12
        }

        calendar.apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return calendar.timeInMillis
    }

    fun formatDateTime(millis: Long): String {
        val format = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return format.format(Date(millis))
    }
}