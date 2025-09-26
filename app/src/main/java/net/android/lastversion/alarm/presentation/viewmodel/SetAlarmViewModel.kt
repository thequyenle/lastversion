package net.android.lastversion.alarm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.android.lastversion.alarm.data.preferences.AlarmPreferences
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.domain.usecase.SaveAlarmUseCase
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmScheduler
import java.util.*

class SetAlarmViewModel(
    private val saveAlarmUseCase: SaveAlarmUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val preferences: AlarmPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetAlarmUiState())
    val uiState: StateFlow<SetAlarmUiState> = _uiState.asStateFlow()

    init {
        initializeDefaults()
    }

    private fun initializeDefaults() {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR)
        val minute = currentTime.get(Calendar.MINUTE)
        val amPm = if (currentTime.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

        _uiState.value = _uiState.value.copy(
            hour = if (hour == 0) 12 else hour,
            minute = minute,
            amPm = amPm,
            isSnoozeEnabled = preferences.defaultSnoozeMinutes > 0,
            isVibrationEnabled = preferences.defaultVibration,
            isSoundEnabled = preferences.defaultSound,
            isSilentModeEnabled = preferences.defaultSilentMode,
            soundUri = preferences.defaultSoundUri
        )
    }

    fun loadAlarmForEdit(alarm: Alarm) {
        _uiState.value = _uiState.value.copy(
            alarmId = alarm.id,
            hour = alarm.hour,
            minute = alarm.minute,
            amPm = alarm.amPm,
            label = alarm.label,
            activeDays = alarm.activeDays.clone(),
            isSnoozeEnabled = alarm.isSnoozeEnabled,
            isVibrationEnabled = alarm.isVibrationEnabled,
            isSoundEnabled = alarm.isSoundEnabled,
            isSilentModeEnabled = alarm.isSilentModeEnabled,
            note = alarm.note,
            soundUri = alarm.soundUri,
            isEditMode = true
        )
    }

    fun updateTime(hour: Int, minute: Int, amPm: String) {
        _uiState.value = _uiState.value.copy(
            hour = hour,
            minute = minute,
            amPm = amPm
        )
    }

    fun updateLabel(label: String) {
        _uiState.value = _uiState.value.copy(label = label)
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun toggleDay(dayIndex: Int) {
        val newActiveDays = _uiState.value.activeDays.clone()
        newActiveDays[dayIndex] = !newActiveDays[dayIndex]
        _uiState.value = _uiState.value.copy(activeDays = newActiveDays)
    }

    fun updateSnoozeEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isSnoozeEnabled = enabled)
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isVibrationEnabled = enabled)
    }

    fun updateSoundEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isSoundEnabled = enabled)
    }

    fun updateSilentModeEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isSilentModeEnabled = enabled)
    }

    fun updateSoundUri(uri: String) {
        _uiState.value = _uiState.value.copy(soundUri = uri)
    }

    fun saveAlarm(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val alarm = Alarm(
                    id = state.alarmId,
                    hour = state.hour,
                    minute = state.minute,
                    amPm = state.amPm,
                    label = state.label.ifEmpty { "Alarm" },
                    activeDays = state.activeDays,
                    isEnabled = true,
                    isSnoozeEnabled = state.isSnoozeEnabled,
                    isVibrationEnabled = state.isVibrationEnabled,
                    isSoundEnabled = state.isSoundEnabled,
                    isSilentModeEnabled = state.isSilentModeEnabled,
                    note = state.note,
                    soundUri = state.soundUri
                )

                val savedId = saveAlarmUseCase(alarm)
                val savedAlarm = if (alarm.id == 0) alarm.copy(id = savedId.toInt()) else alarm

                alarmScheduler.scheduleAlarm(savedAlarm)
                onSuccess()

            } catch (e: Exception) {
                onError(e.message ?: "Failed to save alarm")
            }
        }
    }

    fun getActiveDaysText(): String {
        val days = _uiState.value.activeDays
        if (days.none { it }) return "Never"
        if (days.all { it }) return "Every day"

        val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return days.indices
            .asSequence()                  // 0..6
            .filter { days[it] }           // lấy index đang bật
            .map { dayNames[it] }          // map sang tên ngày
            .joinToString(", ")
    }
}

data class SetAlarmUiState(
    val alarmId: Int = 0,
    val hour: Int = 6,
    val minute: Int = 0,
    val amPm: String = "AM",
    val label: String = "Alarm",
    val activeDays: BooleanArray = BooleanArray(7) { false },
    val isSnoozeEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val isSilentModeEnabled: Boolean = false,
    val note: String = "",
    val soundUri: String = "",
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SetAlarmUiState

        return alarmId == other.alarmId &&
                hour == other.hour &&
                minute == other.minute &&
                amPm == other.amPm &&
                label == other.label &&
                activeDays.contentEquals(other.activeDays) &&
                isSnoozeEnabled == other.isSnoozeEnabled &&
                isVibrationEnabled == other.isVibrationEnabled &&
                isSoundEnabled == other.isSoundEnabled &&
                isSilentModeEnabled == other.isSilentModeEnabled &&
                note == other.note &&
                soundUri == other.soundUri &&
                isEditMode == other.isEditMode &&
                isLoading == other.isLoading &&
                error == other.error
    }

    override fun hashCode(): Int {
        var result = alarmId
        result = 31 * result + hour
        result = 31 * result + minute
        result = 31 * result + amPm.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + activeDays.contentHashCode()
        result = 31 * result + isSnoozeEnabled.hashCode()
        result = 31 * result + isVibrationEnabled.hashCode()
        result = 31 * result + isSoundEnabled.hashCode()
        result = 31 * result + isSilentModeEnabled.hashCode()
        result = 31 * result + note.hashCode()
        result = 31 * result + soundUri.hashCode()
        result = 31 * result + isEditMode.hashCode()
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}