package net.android.lastversion.alarm.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val amPm: String,
    val label: String,
    val activeDays: String, // Lưu như string "1,0,1,1,1,0,0" (Mon-Sun)
    val activeDaysText: String,
    val isEnabled: Boolean,
    val isSnoozeEnabled: Boolean,
    val isVibrationEnabled: Boolean,
    val isSoundEnabled: Boolean,
    val isSilentModeEnabled: Boolean,
    val note: String
)