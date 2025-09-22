package net.android.lastversion

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.animation.Animation
import android.view.animation.AnimationUtils

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val prefs = getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
        val progress = findViewById<ImageView>(R.id.ivProgressbar)
        val rotate = AnimationUtils.loadAnimation(this, R.anim.rotate)
        progress.startAnimation(rotate)

        Handler(Looper.getMainLooper()).postDelayed({
            when {
                !prefs.getBoolean("lang_done", false) -> startActivity(Intent(this, LanguageActivity::class.java))
                !prefs.getBoolean("intro_done", false) -> startActivity(Intent(this, TutorialActivity::class.java))
                !prefs.getBoolean("perm_done", false) -> startActivity(Intent(this, PermissionActivity::class.java))
                else -> startActivity(Intent(this, HomeActivity::class.java))
            }
            finish()
        }, 4000)
    }
}