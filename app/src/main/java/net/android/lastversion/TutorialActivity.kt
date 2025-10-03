package net.android.lastversion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator
import net.android.lastversion.data.IntroPage

class TutorialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        val introPages = listOf(
            IntroPage(
                R.drawable.ic_clock,
                getString(R.string.tutorial_slide1_title),
                getString(R.string.tutorial_slide1_desc)
            ),
            IntroPage(
                R.drawable.ic_time,
                getString(R.string.tutorial_slide2_title),
                getString(R.string.tutorial_slide2_desc)
            ),
            IntroPage(
                R.drawable.ic_stopwatch,
                getString(R.string.tutorial_slide3_title),
                getString(R.string.tutorial_slide3_desc)
            )
        )

        val dotsIndicator = findViewById<WormDotsIndicator>(R.id.dotsIndicator)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = TutorialAdapter(introPages)
        dotsIndicator.attachTo(viewPager)

        findViewById<Button>(R.id.btnNext).setOnClickListener {
            if (viewPager.currentItem < introPages.size - 1) {
                viewPager.currentItem += 1
            } else {
                getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
                    .edit().putBoolean("intro_done", true).apply()
                startActivity(Intent(this, PermissionActivity::class.java))
                finish()
            }
        }
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