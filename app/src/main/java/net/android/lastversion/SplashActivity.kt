package net.android.lastversion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.view.animation.AnimationUtils
import android.view.KeyEvent

class SplashActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val prefs = getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
        val progress = findViewById<ImageView>(R.id.ivProgressbar)
        val rotate = AnimationUtils.loadAnimation(this, R.anim.rotate)
        progress.startAnimation(rotate)

        Handler(Looper.getMainLooper()).postDelayed({
            when {
                // ✅ First time: Check if language is done
                !prefs.getBoolean("lang_done", false) -> {
                    // First launch - start full onboarding flow
                    startActivity(Intent(this, LanguageActivity::class.java))
                }
                else -> {
                    // ✅ Subsequent launches - always show tutorial
                    startActivity(Intent(this, TutorialActivity::class.java))
                }
            }
            finish()
        }, 4000)
    }

    override fun onBackPressed() {
        // Không làm gì cả - ngăn thoát ứng dụng
        super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true // Chặn sự kiện back
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        showSystemUI(white = false)
    }

    fun Activity.showSystemUI(white: Boolean = false) {
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        if (white) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }
}