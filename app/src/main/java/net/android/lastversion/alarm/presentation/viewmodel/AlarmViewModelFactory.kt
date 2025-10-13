package net.android.lastversion.alarm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.android.lastversion.alarm.data.preferences.AlarmPreferences
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.domain.usecase.*
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmScheduler
import android.content.Context

class AlarmViewModelFactory(
    private val repository: AlarmRepositoryImpl,
    private val alarmScheduler: AlarmScheduler,
    private val preferences: AlarmPreferences,
    private val context: Context  // ✅ ADD THIS

) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AlarmViewModel::class.java) -> {
                AlarmViewModel(
                    getAlarmsUseCase = GetAlarmsUseCase(repository),
                    saveAlarmUseCase = SaveAlarmUseCase(repository),
                    deleteAlarmUseCase = DeleteAlarmUseCase(repository),
                    toggleAlarmUseCase = ToggleAlarmUseCase(repository),
                    alarmScheduler = alarmScheduler,
                    context = context  // ✅ ADD THIS

                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}