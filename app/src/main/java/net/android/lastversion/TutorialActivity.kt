package net.android.lastversion

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2

class TutorialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        val pages = listOf("Welcome", "Explore Features", "Get Started")
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = TutorialAdapter(pages)

        findViewById<Button>(R.id.btnNext).setOnClickListener {
            if (viewPager.currentItem < pages.size - 1) {
                viewPager.currentItem += 1
            } else {
                getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
                    .edit().putBoolean("intro_done", true).apply()
                startActivity(Intent(this, PermissionActivity::class.java))
                finish()
            }
        }
    }
}