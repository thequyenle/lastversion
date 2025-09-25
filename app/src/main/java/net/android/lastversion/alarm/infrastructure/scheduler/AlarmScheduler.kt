package net.android.lastversion.alarm.infrastructure.scheduler

import net.android.lastversion.alarm.domain.model.Alarm

interface AlarmScheduler {
    fun scheduleAlarm(alarmId: Int, triggerTime: Long, alarm: Alarm)
    fun cancelAlarm(alarmId: Int)
    fun rescheduleRecurringAlarm(alarmId: Int, alarm: Alarm)
}