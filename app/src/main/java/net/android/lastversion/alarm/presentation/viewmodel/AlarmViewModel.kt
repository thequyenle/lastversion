package net.android.lastversion.alarm.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl

class AlarmViewModel(private val repository: AlarmRepositoryImpl) : ViewModel() {

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
            val entity = repository.convertToAlarmEntity(alarm)
            repository.insertAlarm(entity)
        }
    }

    // Cập nhật alarm
    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val entity = repository.convertToAlarmEntity(alarm)
            repository.updateAlarm(entity)
        }
    }

    // Xóa alarm
    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val entity = repository.convertToAlarmEntity(alarm)
            repository.deleteAlarm(entity)
        }
    }

    // Xóa alarm theo ID
    fun deleteAlarmById(id: Int) {
        viewModelScope.launch {
            repository.deleteAlarmById(id)
        }
    }

    // Toggle trạng thái alarm
    fun toggleAlarm(alarm: Alarm) {
        val updatedAlarm = alarm.copy(isEnabled = !alarm.isEnabled)
        updateAlarm(updatedAlarm)
    }
}

// ViewModelFactory
class AlarmViewModelFactory(private val repository: AlarmRepositoryImpl) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlarmViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}