package net.android.lastversion.alarm.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.android.lastversion.alarm.data.local.dao.AlarmDao
import net.android.lastversion.alarm.data.local.entity.AlarmEntity

@Database(
    entities = [AlarmEntity::class],
    version = 3, // THAY ĐỔI: version 2 → 3
    exportSchema = false
)
abstract class AlarmDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AlarmDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE alarms ADD COLUMN soundUri TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE alarms ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE alarms ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        // THÊM: Migration 2 → 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Bước 1: Thêm các cột mới
                database.execSQL(
                    "ALTER TABLE alarms ADD COLUMN snoozeMinutes INTEGER NOT NULL DEFAULT 5"
                )
                database.execSQL(
                    "ALTER TABLE alarms ADD COLUMN vibrationPattern TEXT NOT NULL DEFAULT 'default'"
                )
                database.execSQL(
                    "ALTER TABLE alarms ADD COLUMN soundType TEXT NOT NULL DEFAULT 'default'"
                )

                // Bước 2: Convert dữ liệu cũ
                database.execSQL(
                    """
                    UPDATE alarms 
                    SET snoozeMinutes = CASE 
                            WHEN isSnoozeEnabled = 1 THEN 5 
                            ELSE 0 
                        END,
                        vibrationPattern = CASE 
                            WHEN isVibrationEnabled = 1 THEN 'default' 
                            ELSE 'off' 
                        END,
                        soundType = CASE 
                            WHEN isSoundEnabled = 1 THEN 'default' 
                            ELSE 'off' 
                        END
                    """.trimIndent()
                )

                // Bước 3: Tạo bảng mới (bỏ các cột Boolean cũ)
                database.execSQL(
                    """
                    CREATE TABLE alarms_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        amPm TEXT NOT NULL,
                        label TEXT NOT NULL,
                        activeDays TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        snoozeMinutes INTEGER NOT NULL,
                        vibrationPattern TEXT NOT NULL,
                        soundType TEXT NOT NULL,
                        isSilentModeEnabled INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        soundUri TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // Bước 4: Copy dữ liệu (BỎ 3 cột Boolean cũ)
                database.execSQL(
                    """
                    INSERT INTO alarms_new 
                    SELECT 
                        id, hour, minute, amPm, label, activeDays, isEnabled,
                        snoozeMinutes, vibrationPattern, soundType,
                        isSilentModeEnabled, note, soundUri, createdAt, updatedAt
                    FROM alarms
                    """.trimIndent()
                )

                // Bước 5: Xóa bảng cũ
                database.execSQL("DROP TABLE alarms")

                // Bước 6: Đổi tên bảng mới
                database.execSQL("ALTER TABLE alarms_new RENAME TO alarms")
            }
        }

        fun getDatabase(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarm_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // THÊM migration mới
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}