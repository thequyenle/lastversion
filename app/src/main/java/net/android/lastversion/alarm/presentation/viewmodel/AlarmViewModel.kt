package net.android.lastversion.alarm.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmSchedulerImpl
import java.util.Calendar
import java.util.Date

class AlarmViewModel(
    private val repository: AlarmRepositoryImpl,
    private val context: Context // ← Add context for AlarmScheduler
) : ViewModel() {

    companion object {
        private const val TAG = "AlarmViewModel"
    }

    private val alarmScheduler = AlarmSchedulerImpl(context)

    // LiveData cho danh sách alarm
    val allAlarms: LiveData<List<Alarm>> = repository.getAllAlarms()
        .map { entities ->
            entities.map { entity ->
                repository.convertToAlarmModel(entity)
            }
        }
        .asLiveData()

    // Thêm alarm mới
    fun insertAlarm(alarm: Alarm) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== INSERTING ALARM ===")
                Log.d(TAG, "Alarm: ${alarm.hour}:${alarm.minute} ${alarm.amPm}")
                Log.d(TAG, "Enabled: ${alarm.isEnabled}")

                // 1. Save to database first
                val entity = repository.convertToAlarmEntity(alarm)
                repository.insertAlarm(entity)
                Log.d(TAG, "Alarm saved to database")

                // 2. Schedule with AlarmManager if enabled
                if (alarm.isEnabled) {
                    scheduleAlarmWithManager(alarm)
                } else {
                    Log.d(TAG, "Alarm is disabled - not scheduling")
                }

                Log.d(TAG, "=== INSERT ALARM COMPLETE ===")
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting alarm", e)
            }
        }
    }

    // Cập nhật alarm
    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== UPDATING ALARM ===")
                Log.d(TAG, "Alarm ID: ${alarm.id}, Enabled: ${alarm.isEnabled}")

                // 1. Update database
                val entity = repository.convertToAlarmEntity(alarm)
                repository.updateAlarm(entity)
                Log.d(TAG, "Alarm updated in database")

                // 2. Cancel existing alarm first
                alarmScheduler.cancelAlarm(alarm.id)
                Log.d(TAG, "Existing alarm cancelled")

                // 3. Schedule new alarm if enabled
                if (alarm.isEnabled) {
                    scheduleAlarmWithManager(alarm)
                } else {
                    Log.d(TAG, "Alarm is disabled - not rescheduling")
                }

                Log.d(TAG, "=== UPDATE ALARM COMPLETE ===")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating alarm", e)
            }
        }
    }

    // Xóa alarm
    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== DELETING ALARM ===")
                Log.d(TAG, "Alarm ID: ${alarm.id}")

                // 1. Cancel alarm from AlarmManager
                alarmScheduler.cancelAlarm(alarm.id)
                Log.d(TAG, "Alarm cancelled from AlarmManager")

                // 2. Delete from database
                val entity = repository.convertToAlarmEntity(alarm)
                repository.deleteAlarm(entity)
                Log.d(TAG, "Alarm deleted from database")

                Log.d(TAG, "=== DELETE ALARM COMPLETE ===")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting alarm", e)
            }
        }
    }

    // Xóa alarm theo ID
    fun deleteAlarmById(id: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting alarm by ID: $id")

                // Cancel from AlarmManager first
                alarmScheduler.cancelAlarm(id)

                // Then delete from database
                repository.deleteAlarmById(id)

                Log.d(TAG, "Alarm $id deleted completely")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting alarm by ID", e)
            }
        }
    }

    // Toggle trạng thái alarm
    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== TOGGLING ALARM ===")
                Log.d(TAG, "Alarm ID: ${alarm.id}")
                Log.d(TAG, "Current state: ${alarm.isEnabled} -> New state: ${!alarm.isEnabled}")

                val updatedAlarm = alarm.copy(isEnabled = !alarm.isEnabled)

                // Update in database
                updateAlarm(updatedAlarm)

                Log.d(TAG, "=== TOGGLE ALARM COMPLETE ===")
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling alarm", e)
            }
        }
    }

    /**
     * Schedule alarm with AlarmManager
     */
    private fun scheduleAlarmWithManager(alarm: Alarm) {
        try {
            Log.d(TAG, "Scheduling alarm ${alarm.id} with AlarmManager...")

            val triggerTime = calculateAlarmTime(alarm)
            if (triggerTime != null && triggerTime > System.currentTimeMillis()) {
                Log.d(TAG, "Calculated trigger time: ${Date(triggerTime)}")
                alarmScheduler.scheduleAlarm(alarm.id, triggerTime, alarm)
                Log.d(TAG, "Alarm ${alarm.id} scheduled successfully")
            } else {
                Log.w(TAG, "Invalid trigger time calculated: $triggerTime")
                // If time is in the past, schedule for next occurrence
                val nextTriggerTime = calculateNextAlarmTime(alarm)
                if (nextTriggerTime != null) {
                    Log.d(TAG, "Scheduling for next occurrence: ${Date(nextTriggerTime)}")
                    alarmScheduler.scheduleAlarm(alarm.id, nextTriggerTime, alarm)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm with manager", e)
        }
    }

    /**
     * Calculate alarm trigger time
     */
    private fun calculateAlarmTime(alarm: Alarm): Long? {
        try {
            val calendar = Calendar.getInstance()

            // Convert 12-hour to 24-hour format
            val hour24 = when {
                alarm.amPm == "AM" && alarm.hour == 12 -> 0
                alarm.amPm == "AM" -> alarm.hour
                alarm.amPm == "PM" && alarm.hour == 12 -> 12
                else -> alarm.hour + 12
            }

            calendar.apply {
                set(Calendar.HOUR_OF_DAY, hour24)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            Log.d(TAG, "Calculated 24-hour time: $hour24:${alarm.minute}")
            Log.d(TAG, "Target time: ${Date(calendar.timeInMillis)}")
            Log.d(TAG, "Current time: ${Date(System.currentTimeMillis())}")

            return calendar.timeInMillis
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating alarm time", e)
            return null
        }
    }

    /**
     * Calculate next alarm time (if current time is in the past)
     */
    private fun calculateNextAlarmTime(alarm: Alarm): Long? {
        val calendar = Calendar.getInstance()

        // Convert to 24-hour format
        val hour24 = when {
            alarm.amPm == "AM" && alarm.hour == 12 -> 0
            alarm.amPm == "AM" -> alarm.hour
            alarm.amPm == "PM" && alarm.hour == 12 -> 12
            else -> alarm.hour + 12
        }

        calendar.apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If time has passed today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return calendar.timeInMillis
    }
}

// Updated ViewModelFactory with Context
class AlarmViewModelFactory(
    private val repository: AlarmRepositoryImpl,
    private val context: Context // ← Add context parameter
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlarmViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}