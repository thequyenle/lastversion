package net.android.lastversion

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class LanguageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)

        val languages = listOf("English", "Vietnamese", "Spanish", "French", "German")
        val rv = findViewById<RecyclerView>(R.id.rvLanguages)
        var selectedLanguage: String? = null
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = LanguageAdapter(languages) { selected ->
            selectedLanguage = selected
        }

        findViewById<Button>(R.id.btnDone).setOnClickListener {
            getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
                .edit().putBoolean("lang_done", true).apply()
            startActivity(Intent(this, TutorialActivity::class.java))
            finish()
        }
    }
}