package net.android.lastversion.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object SystemUtils {
    var defaultFontScale: Float = 1f
    private var myLocale: Locale? = null
    fun saveLocale(context: Context, lang: String?) {
        setPreLanguage(context, lang)
    }

    fun setLocale(context: Context) {
        val language = getPreLanguage(context)
        if (language == "") {
            val config = Configuration()
            val locale = Locale.getDefault()
            Locale.setDefault(locale)
            config.locale = locale
            context.resources
                .updateConfiguration(config, context.resources.displayMetrics)
        } else {
            changeLang(language, context)
        }
    }

    fun changeLang(lang: String?, context: Context) {
        if (lang.equals("", ignoreCase = true)) {
            return
        }
        myLocale = Locale(lang)
        saveLocale(context, lang)
        Locale.setDefault(myLocale)
        val config = Configuration()
        config.locale = myLocale
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun getPreLanguage(mContext: Context): String? {
        val preferences = mContext.getSharedPreferences("data2", Context.MODE_PRIVATE)
        return preferences.getString("KEY_LANGUAGE_2", "en")
    }

    fun setPreLanguage(context: Context, language: String?) {
        if (language == null || language == "") {
        } else {
            val preferences = context.getSharedPreferences("data2", Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString("KEY_LANGUAGE_2", language)
            editor.apply()
        }
    }
}