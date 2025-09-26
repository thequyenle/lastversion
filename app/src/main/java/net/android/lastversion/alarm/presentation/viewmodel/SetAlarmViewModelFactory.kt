package net.android.lastversion.alarm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.android.lastversion.alarm.data.preferences.AlarmPreferences
import net.android.lastversion.alarm.domain.usecase.SaveAlarmUseCase
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmScheduler

class SetAlarmViewModelFactory(
    private val saveAlarmUseCase: SaveAlarmUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val preferences: AlarmPreferences
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SetAlarmViewModel::class.java) -> {
                SetAlarmViewModel(
                    saveAlarmUseCase = saveAlarmUseCase,
                    alarmScheduler = alarmScheduler,
                    preferences = preferences
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
