package net.android.lastversion.alarm.database

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromBooleanArray(value: BooleanArray): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toBooleanArray(value: String): BooleanArray {
        return value.split(",").map { it.toBoolean() }.toBooleanArray()
    }
}
