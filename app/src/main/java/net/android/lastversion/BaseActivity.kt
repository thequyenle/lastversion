package net.android.lastversion

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.android.lastversion.utils.LocaleHelper
import net.android.lastversion.utils.SystemUtils

/**
 * BaseActivity để apply ngôn ngữ cho tất cả các Activity
 * Tất cả Activity khác nên extends từ BaseActivity này
 */
open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Apply saved language
        SystemUtils.setLocale(newBase)
        super.attachBaseContext(newBase)
    }
}