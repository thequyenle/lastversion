package net.android.lastversion.alarm.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AlarmPreferences(context: Context) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    var defaultSnoozeMinutes: Int
        get() = sharedPrefs.getInt(KEY_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES)
        set(value) = sharedPrefs.edit { putInt(KEY_SNOOZE_MINUTES, value) }

    var defaultVibration: Boolean
        get() = sharedPrefs.getBoolean(KEY_DEFAULT_VIBRATION, true)
        set(value) = sharedPrefs.edit { putBoolean(KEY_DEFAULT_VIBRATION, value) }

    var defaultSound: Boolean
        get() = sharedPrefs.getBoolean(KEY_DEFAULT_SOUND, true)
        set(value) = sharedPrefs.edit { putBoolean(KEY_DEFAULT_SOUND, value) }

    var defaultSilentMode: Boolean
        get() = sharedPrefs.getBoolean(KEY_DEFAULT_SILENT_MODE, false)
        set(value) = sharedPrefs.edit { putBoolean(KEY_DEFAULT_SILENT_MODE, value) }

    var defaultSoundUri: String
        get() = sharedPrefs.getString(KEY_DEFAULT_SOUND_URI, "") ?: ""
        set(value) = sharedPrefs.edit { putString(KEY_DEFAULT_SOUND_URI, value) }

    companion object {
        private const val PREFS_NAME = "alarm_preferences"
        private const val KEY_SNOOZE_MINUTES = "snooze_minutes"
        private const val KEY_DEFAULT_VIBRATION = "default_vibration"
        private const val KEY_DEFAULT_SOUND = "default_sound"
        private const val KEY_DEFAULT_SILENT_MODE = "default_silent_mode"
        private const val KEY_DEFAULT_SOUND_URI = "default_sound_uri"

        private const val DEFAULT_SNOOZE_MINUTES = 5
    }
}