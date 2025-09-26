package net.android.lastversion.alarm.infrastructure.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.android.lastversion.alarm.data.local.database.AlarmDatabase
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmSchedulerImpl

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            Log.d(TAG, "Device boot completed - scheduling alarm reschedule")

            // Use WorkManager to handle reschedule after boot
            WorkManager.getInstance(context).enqueueUniqueWork(
                "reschedule-after-boot",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<RescheduleWorker>().build()
            )
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

class RescheduleWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            CoroutineScope(Dispatchers.IO).launch {
                // Wait for system to stabilize
                delay(2000)
                rescheduleAllAlarms()
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("RescheduleWorker", "Error rescheduling alarms", e)
            Result.retry()
        }
    }

    private suspend fun rescheduleAllAlarms() {
        try {
            val repository = AlarmRepositoryImpl(
                AlarmDatabase.getDatabase(applicationContext).alarmDao()
            )
            val scheduler = AlarmSchedulerImpl(applicationContext)

            repository.getEnabledAlarms().collect { alarms ->
                alarms.forEach { alarm ->
                    scheduler.scheduleAlarm(alarm)
                }
                Log.d("RescheduleWorker", "Rescheduled ${alarms.size} alarms")
            }

        } catch (e: Exception) {
            Log.e("RescheduleWorker", "Error rescheduling alarms", e)
        }
    }
}