package net.android.lastversion

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.android.lastversion.data.LanguageItem


class LanguageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)

        val languageList = mutableListOf(
            LanguageItem("English", R.drawable.flag_english),
            LanguageItem("Spanish", R.drawable.flag_spanish),
            LanguageItem("French", R.drawable.flag_french),
            LanguageItem("Hindi", R.drawable.flag_hindi),
            LanguageItem("Portugeese", R.drawable.flag_portugeese),
        )
        val rv = findViewById<RecyclerView>(R.id.rvLanguages)
        var selectedLanguage: String? = null
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = LanguageAdapter(languageList) { selected ->
            selectedLanguage = selected.name
        }

        findViewById<ImageButton>(R.id.btnDone).setOnClickListener {
            getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
                .edit().putBoolean("lang_done", true).apply()
            startActivity(Intent(this, TutorialActivity::class.java))
            finish()
        }
    }
}