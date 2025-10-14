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
import net.android.lastversion.alarm.presentation.activity.AlarmRingingActivity

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

    // In AlarmViewModel.kt, replace the saveAlarm() function:

    fun saveAlarm(alarm: Alarm) {
        viewModelScope.launch {
            try {
                // ✅ FIX: Get the saved alarm ID
                val savedId = if (alarm.id == 0) {
                    saveAlarmUseCase(alarm)  // Returns the new ID for new alarms
                } else {
                    saveAlarmUseCase(alarm)  // Returns the existing ID for updates
                    alarm.id.toLong()
                }

                // ✅ FIX: Create alarm with correct ID before scheduling
                val alarmToSchedule = if (alarm.id == 0) {
                    alarm.copy(id = savedId.toInt())
                } else {
                    alarm
                }

                if (alarmToSchedule.isEnabled) {
                    android.util.Log.d(TAG, "🔵 Scheduling alarm with ID: ${alarmToSchedule.id}")
                    alarmScheduler.scheduleAlarm(alarmToSchedule)
                }

                Log.d(TAG, "✅ Alarm saved successfully with ID: ${alarmToSchedule.id}")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
                Log.e(TAG, "❌ Error saving alarm", e)
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
                        // Cancel the scheduled alarm
                        alarmScheduler.cancelAlarm(alarmId)
                        Log.d(TAG, "🔴 Alarm $alarmId is being turned OFF")

                        // ✅ Stop currently ringing alarm directly via companion object
                        AlarmRingingActivity.stopAlarmById(alarmId)
                        Log.d(TAG, "🔇 Called stopAlarmById for alarm $alarmId")

                        // Also send broadcast as backup
                        val stopIntent = Intent("ACTION_STOP_ALARM")
                        stopIntent.putExtra("alarm_id", alarmId)
                        context.sendBroadcast(stopIntent)
                        Log.d(TAG, "📡 Broadcast sent to stop alarm $alarmId")
                    } else {
                        Log.d(TAG, "🟢 Alarm $alarmId is being turned ON")
                        alarmScheduler.scheduleAlarm(alarm.copy(isEnabled = true))
                    }
                }

                toggleAlarmUseCase(alarmId)
                Log.d(TAG, "✅ Alarm toggled successfully")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
                Log.e(TAG, "❌ Error toggling alarm", e)
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