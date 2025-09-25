package net.android.lastversion.alarm.presentation.usecase

import net.android.lastversion.alarm.infrastructure.scheduler.AlarmScheduler

class CancelAlarmUseCase(
    private val alarmScheduler: AlarmScheduler
) {
    operator fun invoke(alarmId: Int) {
        alarmScheduler.cancelAlarm(alarmId)
    }
}