package net.android.lastversion

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.android.lastversion.utils.LocaleHelper

/**
 * BaseActivity để apply ngôn ngữ cho tất cả các Activity
 * Tất cả Activity khác nên extends từ BaseActivity này
 */
open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply lại ngôn ngữ mỗi khi Activity được tạo
        LocaleHelper.setLocale(this)
    }
}