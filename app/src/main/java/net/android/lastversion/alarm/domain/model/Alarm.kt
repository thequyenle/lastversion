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
    val activeDays: BooleanArray = BooleanArray(7) { false },
    val isEnabled: Boolean = true,

    // THAY ĐỔI: 3 thuộc tính mới
    val snoozeMinutes: Int = 5,
    val vibrationPattern: String = "default",
    val soundType: String = "default",

    val isSilentModeEnabled: Boolean = false,
    val note: String = "Wake Up !!!",
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

        val weekdays = activeDays.slice(1..5).all { it }
        val weekends = activeDays[0] && activeDays[6]

        return when {
            weekdays && !weekends -> "Weekdays"
            weekends && !weekdays -> "Weekends"
            else -> {
                val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
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

    // Thay thế function getNextTriggerTime() trong Alarm.kt

    fun getNextTriggerTime(): Long {
        val calendar = Calendar.getInstance()
        val targetCalendar = Calendar.getInstance()

        // Chuyển đổi 12-hour sang 24-hour format
        val hour24 = when {
            amPm == "AM" && hour == 12 -> 0
            amPm == "AM" -> hour
            amPm == "PM" && hour == 12 -> 12
            else -> hour + 12
        }

        // Set giờ phút cho target calendar
        targetCalendar.apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Nếu không có ngày lặp lại (one-time alarm)
        if (!hasRecurringDays()) {
            // Nếu thời gian đã qua hôm nay, chuyển sang ngày mai
            if (targetCalendar.timeInMillis <= calendar.timeInMillis) {
                targetCalendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            return targetCalendar.timeInMillis
        }

        // Với recurring alarm: tìm ngày kế tiếp có alarm
        for (i in 0..7) {
            val dayOfWeek = targetCalendar.get(Calendar.DAY_OF_WEEK) - 1

            // Kiểm tra nếu ngày này được active VÀ thời gian chưa qua
            if (activeDays[dayOfWeek] && targetCalendar.timeInMillis > calendar.timeInMillis) {
                return targetCalendar.timeInMillis
            }

            // Chuyển sang ngày tiếp theo
            targetCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Fallback: trả về thời gian hiện tại (không nên xảy ra)
        return targetCalendar.timeInMillis
    }

    // THÊM: Helper functions để hiển thị
    fun getSnoozeDisplayText(): String = when (snoozeMinutes) {
        0 -> "Tắt"
        else -> "$snoozeMinutes phút"
    }

    fun getVibrationDisplayText(): String = when (vibrationPattern) {
        "off" -> "Tắt"
        "default" -> "Mặc định"
        "short" -> "Ngắn"
        "long" -> "Dài"
        "double" -> "Rung đôi"
        else -> "Mặc định"
    }

    fun getSoundDisplayText(): String = when (soundType) {
        "off" -> "Tắt"
        "default" -> "Mặc định"
        "gentle" -> "Nhẹ nhàng"
        "loud" -> "Lớn"
        "progressive" -> "Tăng dần"
        "custom" -> "Tùy chỉnh"
        else -> "Mặc định"
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
                snoozeMinutes == other.snoozeMinutes &&
                vibrationPattern == other.vibrationPattern &&
                soundType == other.soundType &&
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
        result = 31 * result + snoozeMinutes.hashCode()
        result = 31 * result + vibrationPattern.hashCode()
        result = 31 * result + soundType.hashCode()
        result = 31 * result + isSilentModeEnabled.hashCode()
        result = 31 * result + note.hashCode()
        return result
    }
}