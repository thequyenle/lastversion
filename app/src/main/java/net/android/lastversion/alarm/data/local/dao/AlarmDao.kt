package net.android.lastversion.alarm.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.android.lastversion.alarm.data.local.entity.AlarmEntity

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY hour, minute ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 ORDER BY hour, minute ASC")
    fun getEnabledAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Int): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Int)

    @Query("UPDATE alarms SET isEnabled = NOT isEnabled WHERE id = :id")
    suspend fun toggleAlarm(id: Int)

    @Query("SELECT COUNT(*) FROM alarms")
    suspend fun getAlarmCount(): Int

    @Query("DELETE FROM alarms")
    suspend fun deleteAllAlarms()
}