package net.android.lastversion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.animation.Animation
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
                !prefs.getBoolean("lang_done", false) -> startActivity(Intent(this, LanguageActivity::class.java))
                !prefs.getBoolean("intro_done", false) -> startActivity(Intent(this, TutorialActivity::class.java))
                !prefs.getBoolean("perm_done", false) -> startActivity(Intent(this, PermissionActivity::class.java))
                else -> startActivity(Intent(this, HomeActivity::class.java))
            }
            finish()
        }, 4000)
    }

    override fun onBackPressed() {
        // Không làm gì cả - ngăn thoát ứng dụng
     super.onBackPressed() // Bỏ comment dòng này
    }

    // Hoặc dùng cách này cho Android 13+
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true // Chặn sự kiện back
        }
        return super.onKeyDown(keyCode, event)
    }
    override fun onResume() {
        super.onResume()
        showSystemUI(white =false)
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