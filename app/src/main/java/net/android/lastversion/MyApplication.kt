package net.android.lastversion

import android.app.Application
import net.android.lastversion.utils.LocaleHelper

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Apply ngôn ngữ đã lưu khi app khởi động
        LocaleHelper.setLocale(this)
    }
}