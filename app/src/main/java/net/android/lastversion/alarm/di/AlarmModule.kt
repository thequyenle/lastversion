package net.android.lastversion.alarm.di

import android.content.Context
import net.android.lastversion.alarm.data.local.database.AlarmDatabase
import net.android.lastversion.alarm.data.preferences.AlarmPreferences
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.domain.repository.AlarmRepository
import net.android.lastversion.alarm.domain.usecase.*
import net.android.lastversion.alarm.infrastructure.notification.AlarmNotificationManager
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmScheduler
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmSchedulerImpl

object AlarmModule {

    fun provideAlarmDatabase(context: Context): AlarmDatabase {
        return AlarmDatabase.getDatabase(context)
    }

    fun provideAlarmRepository(database: AlarmDatabase): AlarmRepository {
        return AlarmRepositoryImpl(database.alarmDao())
    }

    fun provideAlarmScheduler(context: Context): AlarmScheduler {
        return AlarmSchedulerImpl(context)
    }

    fun provideAlarmNotificationManager(context: Context): AlarmNotificationManager {
        return AlarmNotificationManager(context)
    }

    fun provideAlarmPreferences(context: Context): AlarmPreferences {
        return AlarmPreferences(context)
    }

    // Use Cases
    fun provideGetAlarmsUseCase(repository: AlarmRepository): GetAlarmsUseCase {
        return GetAlarmsUseCase(repository)
    }

    fun provideSaveAlarmUseCase(repository: AlarmRepository): SaveAlarmUseCase {
        return SaveAlarmUseCase(repository)
    }

    fun provideDeleteAlarmUseCase(repository: AlarmRepository): DeleteAlarmUseCase {
        return DeleteAlarmUseCase(repository)
    }

    fun provideToggleAlarmUseCase(repository: AlarmRepository): ToggleAlarmUseCase {
        return ToggleAlarmUseCase(repository)
    }
}