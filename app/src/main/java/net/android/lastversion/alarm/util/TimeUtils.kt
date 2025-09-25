package net.android.lastversion.alarm.util

import java.util.Calendar
import java.util.Locale

object TimeUtils {

    // Có thể override bằng cách truyền vào hàm formatActiveDaysText(...)
    private val DEFAULT_DAY_LABELS = arrayOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat") // 0..6

    /** 24h -> 12h (trả về Triple(hour, minute, "AM"/"PM"))  */
    fun convertTo12HourFormat(hour24: Int, minute: Int): Triple<Int, Int, String> {
        require(hour24 in 0..23) { "hour24 must be 0..23" }
        require(minute in 0..59) { "minute must be 0..59" }

        val hour12 = when {
            hour24 == 0 -> 12
            hour24 <= 12 -> hour24
            else -> hour24 - 12
        }
        val amPm = if (hour24 < 12) "AM" else "PM"
        return Triple(hour12, minute, amPm)
    }

    /** 12h -> 24h (trả về Pair(hour, minute))  */
    fun convertTo24HourFormat(hour12: Int, minute: Int, amPm: String): Pair<Int, Int> {
        require(hour12 in 1..12) { "hour12 must be 1..12" }
        require(minute in 0..59) { "minute must be 0..59" }

        val mer = amPm.trim().uppercase(Locale.US)
        val hour24 = when {
            mer == "AM" && hour12 == 12 -> 0
            mer == "AM" -> hour12 % 12
            mer == "PM" && hour12 == 12 -> 12
            else -> (hour12 % 12) + 12
        }
        return hour24 to minute
    }

    /** "hh:mm AM/PM" – không phụ thuộc Android resources */
    fun formatTimeString(hour: Int, minute: Int, amPm: String, locale: Locale = Locale.getDefault()): String {
        return String.format(locale, "%02d:%02d %s", hour, minute, amPm.trim().uppercase(Locale.US))
    }

    /** Lấy giờ hiện tại (12h) */
    fun getCurrentTime12H(): Triple<Int, Int, String> {
        val cal = Calendar.getInstance()
        val h24 = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return convertTo12HourFormat(h24, m)
    }

    /**
     * Hiển thị ngày kích hoạt:
     * - "Never" / "Every day" / "Weekdays" / "Weekends" / hoặc danh sách "Mon, Wed, Fri"
     * - Mặc định coi 0=Sun ... 6=Sat để đồng bộ với DEFAULT_DAY_LABELS.
     * - Có thể truyền labels riêng để tránh phụ thuộc hằng bên ngoài.
     */
    fun formatActiveDaysText(
        activeDays: BooleanArray,
        dayLabels: Array<String> = DEFAULT_DAY_LABELS
    ): String {
        require(activeDays.size == 7) { "activeDays must have size 7 (Sun..Sat)" }
        require(dayLabels.size == 7) { "dayLabels must have size 7 (Sun..Sat)" }

        // Không có ngày nào
        if (activeDays.none { it }) return "Never"
        // Tất cả các ngày
        if (activeDays.all { it })  return "Every day"

        // Mon..Fri = 1..5 ; Sun = 0 ; Sat = 6
        val weekdaysAll  = (1..5).all { activeDays[it] }
        val weekdaysNone = (1..5).all { !activeDays[it] }
        val weekendsAll  = activeDays[0] && activeDays[6]
        val weekendsNone = !activeDays[0] && !activeDays[6]

        return when {
            weekdaysAll && weekendsNone -> "Weekdays"
            weekendsAll && weekdaysNone -> "Weekends"
            else -> dayLabels
                .mapIndexedNotNull { idx, name -> if (activeDays[idx]) name else null }
                .joinToString(", ")
        }
    }
}
