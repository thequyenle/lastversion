// net/android/lastversion/alarm/domain/repository/AlarmRepository.kt
package net.android.lastversion.alarm.domain.repository

import kotlinx.coroutines.flow.Flow
import net.android.lastversion.alarm.domain.model.Alarm

/**
 * AlarmRepository Interface - Domain Layer
 *
 * Định nghĩa contract cho data operations
 * Data Layer sẽ implement interface này
 * Domain Layer chỉ biết đến interface, không biết implementation
 */
interface AlarmRepository {

    /**
     * Lấy tất cả alarms - real time updates
     */
    fun getAllAlarms(): Flow<List<Alarm>>

    /**
     * Thêm alarm mới
     * @return ID của alarm vừa được tạo
     */
    suspend fun insertAlarm(alarm: Alarm): Long

    /**
     * Cập nhật alarm existing
     */
    suspend fun updateAlarm(alarm: Alarm)

    /**
     * Xóa alarm
     */
    suspend fun deleteAlarm(alarm: Alarm)

    /**
     * Xóa alarm theo ID
     */
    suspend fun deleteAlarmById(id: Int)

    /**
     * Lấy alarm theo ID
     */
    suspend fun getAlarmById(id: Int): Alarm?

    /**
     * Lấy danh sách alarms đang enabled
     */
    fun getEnabledAlarms(): Flow<List<Alarm>>
}