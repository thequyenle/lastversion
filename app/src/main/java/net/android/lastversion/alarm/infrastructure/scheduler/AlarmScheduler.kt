package net.android.lastversion.alarm.infrastructure.scheduler

import net.android.lastversion.alarm.domain.model.Alarm

interface AlarmScheduler {
    fun scheduleAlarm(alarm: Alarm)
    fun cancelAlarm(alarmId: Int)
    fun rescheduleAllAlarms(alarms: List<Alarm>)
    fun scheduleSnoozeAlarm(alarm: Alarm, snoozeTime: Long)
}