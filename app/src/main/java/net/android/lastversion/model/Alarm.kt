package net.android.lastversion.model

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val amPm: String,
    val label: String,
    val activeDays: BooleanArray,
    val activeDaysText: String,
    val isEnabled: Boolean,
    val isSnoozeEnabled: Boolean,
    val isVibrationEnabled: Boolean,
    val isSoundEnabled: Boolean,
    val isSilentModeEnabled: Boolean,
    val note: String
) {
    fun getTimeString(): String {
        return String.format("%02d:%02d %s", hour, minute, amPm)
    }
}
