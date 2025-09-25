package net.android.lastversion.alarm.infrastructure.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.android.lastversion.alarm.data.database.AlarmDatabase
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmSchedulerImpl
import net.android.lastversion.alarm.presentation.usecase.ScheduleAlarmUseCase

/**
 * BootReceiver - Reschedule all enabled alarms after device restart
 *
 * Android clears all alarms when device restarts, so we need to
 * reschedule all enabled alarms from database
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            // Reschedule all enabled alarms
            CoroutineScope(Dispatchers.IO).launch {
                rescheduleAllAlarms(context)
            }
        }
    }

    private suspend fun rescheduleAllAlarms(context: Context) {
        try {
            val repository = AlarmRepositoryImpl(
                AlarmDatabase.getDatabase(context).alarmDao()
            )
            val alarmScheduler = AlarmSchedulerImpl(context)
            val scheduleAlarmUseCase = ScheduleAlarmUseCase(repository, alarmScheduler)

            // Get all alarms from database
            repository.getAllAlarms().collect { alarms ->
                // Reschedule only enabled alarms
                alarms.filter { it.isEnabled }.forEach { alarm ->
                    scheduleAlarmUseCase(alarm)
                }
            }
        } catch (e: Exception) {
            // Log error but don't crash
            e.printStackTrace()
        }
    }
}