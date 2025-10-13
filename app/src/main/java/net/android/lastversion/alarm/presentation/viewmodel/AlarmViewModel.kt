package net.android.lastversion.alarm.presentation.viewmodel

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.domain.usecase.DeleteAlarmUseCase
import net.android.lastversion.alarm.domain.usecase.GetAlarmsUseCase
import net.android.lastversion.alarm.domain.usecase.SaveAlarmUseCase
import net.android.lastversion.alarm.domain.usecase.ToggleAlarmUseCase
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmScheduler
import android.content.Context

class AlarmViewModel(
    private val getAlarmsUseCase: GetAlarmsUseCase,
    private val saveAlarmUseCase: SaveAlarmUseCase,
    private val deleteAlarmUseCase: DeleteAlarmUseCase,
    private val toggleAlarmUseCase: ToggleAlarmUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val context: Context  // ✅ ADD THIS LINE

) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    init {
        loadAlarms()
    }

    private fun loadAlarms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            getAlarmsUseCase()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
                .collect { alarms ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        alarms = alarms,
                        error = null
                    )
                }
        }
    }

    fun saveAlarm(alarm: Alarm) {
        viewModelScope.launch {
            try {
                saveAlarmUseCase(alarm)

                if (alarm.isEnabled) {
                    alarmScheduler.scheduleAlarm(alarm)
                }

                Log.d(TAG, "Alarm saved successfully")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
                Log.e(TAG, "Error saving alarm", e)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            try {
                alarmScheduler.cancelAlarm(alarm.id)
                deleteAlarmUseCase(alarm)
                Log.d(TAG, "Alarm deleted successfully")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
                Log.e(TAG, "Error deleting alarm", e)
            }
        }
    }

    fun toggleAlarm(alarmId: Int) {
        viewModelScope.launch {
            try {
                val alarm = _uiState.value.alarms.find { it.id == alarmId }
                if (alarm != null) {
                    if (alarm.isEnabled) {
                        alarmScheduler.cancelAlarm(alarmId)
                        // ✅ NEW: Send broadcast to stop currently ringing alarm
                        val stopIntent = Intent("ACTION_STOP_ALARM")
                        stopIntent.putExtra("alarm_id", alarmId)
                        context.sendBroadcast(stopIntent)
                    } else {
                        alarmScheduler.scheduleAlarm(alarm.copy(isEnabled = true))
                    }
                }

                toggleAlarmUseCase(alarmId)
                Log.d(TAG, "Alarm toggled successfully")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
                Log.e(TAG, "Error toggling alarm", e)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    companion object {
        private const val TAG = "AlarmViewModel"
    }
}

data class AlarmUiState(
    val isLoading: Boolean = false,
    val alarms: List<Alarm> = emptyList(),
    val error: String? = null
)