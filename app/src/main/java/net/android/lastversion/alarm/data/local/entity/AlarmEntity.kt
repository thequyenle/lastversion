package net.android.lastversion.alarm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

//@Entity(tableName = "alarms")
//data class AlarmEntity(
//    @PrimaryKey(autoGenerate = true)
//    val id: Int = 0,
//    val hour: Int,
//    val minute: Int,
//    val amPm: String,
//    val label: String,
//    val activeDays: String, // Stored as "1,0,1,1,1,0,0" format
//    val isEnabled: Boolean,
//    val isSnoozeEnabled: Boolean,
//    val isVibrationEnabled: Boolean,
//    val isSoundEnabled: Boolean,
//    val isSilentModeEnabled: Boolean,
//    val note: String,
//    val soundUri: String,
//    val createdAt: Long,
//    val updatedAt: Long
//)


@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val amPm: String,
    val label: String,
    val activeDays: String, // Stored as "1,0,1,1,1,0,0" format
    val isEnabled: Boolean,

    // THAY ĐỔI: 3 cái này lưu giá trị cụ thể
    val snoozeMinutes: Int = 5, // 0 = tắt, 5, 10, 15, 20, 30 = số phút
    val vibrationPattern: String = "default", // "off", "default", "short", "long", "double"
    val soundType: String = "default", // "off", "default", "gentle", "loud", "progressive"

    val isSilentModeEnabled: Boolean,
    val note: String,
    val soundUri: String, // URI cho custom sound (nếu soundType = "custom")
    val createdAt: Long,
    val updatedAt: Long
)