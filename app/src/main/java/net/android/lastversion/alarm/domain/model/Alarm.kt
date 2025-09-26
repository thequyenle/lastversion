package net.android.lastversion.alarm.domain.model

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val amPm: String,
    val label: String,
    val activeDays: BooleanArray,
    val activeDaysText: String = "Never", // ← Default value
    val isEnabled: Boolean,
    val isSnoozeEnabled: Boolean,
    val isVibrationEnabled: Boolean,
    val isSoundEnabled: Boolean,
    val isSilentModeEnabled: Boolean = false, // ← Default value
    val note: String
) {
    fun getTimeString(): String {
        return String.format("%02d:%02d %s", hour, minute, amPm)
    }
}
