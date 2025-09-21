package net.android.lastversion

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val prefs = getSharedPreferences("onboarding_prefs", MODE_PRIVATE)

        Handler(Looper.getMainLooper()).postDelayed({
            when {
                !prefs.getBoolean("lang_done", false) -> startActivity(Intent(this, LanguageActivity::class.java))
                !prefs.getBoolean("intro_done", false) -> startActivity(Intent(this, TutorialActivity::class.java))
                !prefs.getBoolean("perm_done", false) -> startActivity(Intent(this, PermissionActivity::class.java))
                else -> startActivity(Intent(this, HomeActivity::class.java))
            }
            finish()
        }, 2000)
    }
}