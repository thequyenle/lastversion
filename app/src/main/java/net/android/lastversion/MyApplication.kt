package net.android.lastversion

import android.app.Application
import net.android.lastversion.utils.LocaleHelper

class MyApplication : Application() {

    companion object {
        const val PREFS_NAME = "rating_prefs"
        const val KEY_LAUNCH_COUNT = "launch_count"
        const val KEY_HAS_RATED = "has_rated"
    }

    override fun onCreate() {
        super.onCreate()

        // Apply ngôn ngữ đã lưu khi app khởi động
        LocaleHelper.setLocale(this)

        // Increment launch count
        incrementLaunchCount()
    }

    private fun incrementLaunchCount() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentCount = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        prefs.edit().putInt(KEY_LAUNCH_COUNT, currentCount + 1).apply()
    }

    fun getLaunchCount(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getInt(KEY_LAUNCH_COUNT, 0)
    }

    fun hasRated(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAS_RATED, false)
    }

    fun setHasRated(hasRated: Boolean) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAS_RATED, hasRated).apply()
    }
}