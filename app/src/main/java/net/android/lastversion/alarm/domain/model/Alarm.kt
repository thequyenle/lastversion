package net.android.lastversion.alarm.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Alarm(
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val amPm: String,
    val label: String = "Alarm",
    val activeDays: BooleanArray = BooleanArray(7) { false }, // Sun, Mon, Tue, Wed, Thu, Fri, Sat
    val isEnabled: Boolean = true,
    val isSnoozeEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val isSilentModeEnabled: Boolean = false,
    val note: String = "",
    val soundUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {

    fun getTimeString(): String {
        return String.format("%02d:%02d %s", hour, minute, amPm)
    }

    fun getActiveDaysText(): String {
        if (activeDays.none { it }) return "Never"
        if (activeDays.all { it }) return "Every day"

        val weekdays = activeDays.slice(1..5).all { it } // Mon-Fri
        val weekends = activeDays[0] && activeDays[6] // Sun, Sat

        return when {
            weekdays && !weekends -> "Weekdays"
            weekends && !weekdays -> "Weekends"
            else -> {
                val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                // Fix: Convert BooleanArray to List first
                activeDays.toList().mapIndexedNotNull { index, active ->
                    if (active) dayNames[index] else null
                }.joinToString(", ")
            }
        }
    }

    fun hasRecurringDays(): Boolean = activeDays.any { it }

    fun isActiveToday(): Boolean {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        return activeDays[today]
    }

    fun getNextTriggerTime(): Long {
        val calendar = Calendar.getInstance()
        val targetCalendar = Calendar.getInstance()

        // Convert to 24-hour format
        val hour24 = when {
            amPm == "AM" && hour == 12 -> 0
            amPm == "AM" -> hour
            amPm == "PM" && hour == 12 -> 12
            else -> hour + 12
        }

        targetCalendar.apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If no recurring days, schedule for today or tomorrow
        if (!hasRecurringDays()) {
            if (targetCalendar.timeInMillis <= calendar.timeInMillis) {
                targetCalendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            return targetCalendar.timeInMillis
        }

        // Find next active day
        for (i in 0..7) {
            val dayOfWeek = targetCalendar.get(Calendar.DAY_OF_WEEK) - 1

            if (activeDays[dayOfWeek] && targetCalendar.timeInMillis > calendar.timeInMillis) {
                return targetCalendar.timeInMillis
            }

            targetCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return targetCalendar.timeInMillis
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Alarm

        return id == other.id &&
                hour == other.hour &&
                minute == other.minute &&
                amPm == other.amPm &&
                label == other.label &&
                activeDays.contentEquals(other.activeDays) &&
                isEnabled == other.isEnabled &&
                isSnoozeEnabled == other.isSnoozeEnabled &&
                isVibrationEnabled == other.isVibrationEnabled &&
                isSoundEnabled == other.isSoundEnabled &&
                isSilentModeEnabled == other.isSilentModeEnabled &&
                note == other.note
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + hour
        result = 31 * result + minute
        result = 31 * result + amPm.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + activeDays.contentHashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + isSnoozeEnabled.hashCode()
        result = 31 * result + isVibrationEnabled.hashCode()
        result = 31 * result + isSoundEnabled.hashCode()
        result = 31 * result + isSilentModeEnabled.hashCode()
        result = 31 * result + note.hashCode()
        return result
    }
}
