package net.android.lastversion.alarm.util

import java.util.*

object TimeUtils {

    /**
     * Chuyển đổi từ 24h format sang 12h format với AM/PM
     */
    fun convertTo12HourFormat(hour24: Int, minute: Int): Triple<Int, Int, String> {
        val hour12 = when {
            hour24 == 0 -> 12  // 00:xx -> 12:xx AM
            hour24 <= 12 -> hour24  // 01:xx - 12:xx -> 01:xx - 12:xx AM/PM
            else -> hour24 - 12  // 13:xx - 23:xx -> 01:xx - 11:xx PM
        }
        val amPm = if (hour24 < 12) "AM" else "PM"
        return Triple(hour12, minute, amPm)
    }

    /**
     * Chuyển đổi từ 12h format sang 24h format
     */
    fun convertTo24HourFormat(hour12: Int, minute: Int, amPm: String): Pair<Int, Int> {
        val hour24 = when {
            amPm == "AM" && hour12 == 12 -> 0  // 12:xx AM -> 00:xx
            amPm == "AM" -> hour12  // 01:xx - 11:xx AM -> 01:xx - 11:xx
            amPm == "PM" && hour12 == 12 -> 12  // 12:xx PM -> 12:xx
            else -> hour12 + 12  // 01:xx - 11:xx PM -> 13:xx - 23:xx
        }
        return Pair(hour24, minute)
    }

    /**
     * Format time string
     */
    fun formatTimeString(hour: Int, minute: Int, amPm: String): String {
        return String.format("%02d:%02d %s", hour, minute, amPm)
    }

    /**
     * Get current time trong format 12h
     */
    fun getCurrentTime12H(): Triple<Int, Int, String> {
        val calendar = Calendar.getInstance()
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return convertTo12HourFormat(hour24, minute)
    }

    /**
     * Parse active days text
     */
    fun formatActiveDaysText(activeDays: BooleanArray): String {
        if (activeDays.all { !it }) return "Never"
        if (activeDays.all { it }) return "Everyday"

        val weekdays = activeDays.sliceArray(1..5) // Mon-Fri
        val weekends = activeDays.sliceArray(intArrayOf(0, 6)) // Sun, Sat

        when {
            weekdays.all { it } && weekends.all { !it } -> return "Weekdays"
            weekends.all { it } && weekdays.all { !it } -> return "Weekends"
            else -> {
                val activeDayNames = mutableListOf<String>()
                Constants.DAYS_OF_WEEK.forEachIndexed { index, day ->
                    if (activeDays[index]) activeDayNames.add(day)
                }
                return activeDayNames.joinToString(", ")
            }
        }
    }
}