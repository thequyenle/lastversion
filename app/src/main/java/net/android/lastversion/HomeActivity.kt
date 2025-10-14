package net.android.lastversion

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import net.android.lastversion.utils.showSystemUI


class HomeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)


        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Setup custom navigation with LinearLayout
        setupCustomNavigation(navController)
        if (intent.getBooleanExtra("open_settings", false)) {
            navController.navigate(R.id.settingsFragment)
        }

        // ✅ Mark that app is running so SplashActivity won't show tutorial again
        val prefs = getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("app_was_running", true).apply()
    }

    private fun setupCustomNavigation(navController: androidx.navigation.NavController) {
        // Get references to navigation items
        val navAlarm = findViewById<LinearLayout>(R.id.nav_alarm)
       // val navClock = findViewById<LinearLayout>(R.id.nav_clock)
        val navStopwatch = findViewById<LinearLayout>(R.id.nav_stopwatch)
        val navTimer = findViewById<LinearLayout>(R.id.nav_timer)
        val navSettings = findViewById<LinearLayout>(R.id.nav_settings)

        // Set click listeners for navigation items
        navAlarm.setOnClickListener {
            navController.navigate(R.id.alarmFragment)
        }

//        navClock.setOnClickListener {
//            navController.navigate(R.id.clockFragment)
//        }

        navStopwatch.setOnClickListener {
            navController.navigate(R.id.stopwatchFragment)
        }

        navTimer.setOnClickListener {
            navController.navigate(R.id.timerFragment)
        }

        navSettings.setOnClickListener {
            navController.navigate(R.id.settingsFragment)
        }

        // Optional: Listen for destination changes to update visual state
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Reset all items to default state
            resetNavigationItemsState()

            // Highlight current item based on destination
            when (destination.id) {
                R.id.alarmFragment -> highlightNavigationItem(navAlarm)
                //R.id.clockFragment -> highlightNavigationItem(navClock)
                R.id.stopwatchFragment -> highlightNavigationItem(navStopwatch)
                R.id.timerFragment -> highlightNavigationItem(navTimer)
                R.id.settingsFragment -> highlightNavigationItem(navSettings)
            }
        }
    }

    private fun resetNavigationItemsState() {
        // Updated to use ConstraintLayout instead of FrameLayout
        val navContainers = listOf(
            findViewById<ConstraintLayout>(R.id.alarm_icon_container),
           // findViewById<ConstraintLayout>(R.id.clock_icon_container),
            findViewById<ConstraintLayout>(R.id.stopwatch_icon_container),
            findViewById<ConstraintLayout>(R.id.timer_icon_container),
            findViewById<ConstraintLayout>(R.id.settings_icon_container)
        )

        val navItems = listOf(
            findViewById<LinearLayout>(R.id.nav_alarm),
           // findViewById<LinearLayout>(R.id.nav_clock),
            findViewById<LinearLayout>(R.id.nav_stopwatch),
            findViewById<LinearLayout>(R.id.nav_timer),
            findViewById<LinearLayout>(R.id.nav_settings)
        )

        navContainers.forEach { container ->
            // Remove background from icon container
            container.background = null

            // Set white color for icon - find ImageView in ConstraintLayout
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is android.widget.ImageView) {
                    child.setColorFilter(android.graphics.Color.WHITE)
                    break
                }
            }
        }

        navItems.forEach { item ->
            // Set white color for text - find TextView in LinearLayout
            for (i in 0 until item.childCount) {
                val child = item.getChildAt(i)
                if (child is android.widget.TextView) {
                    child.setTextColor(android.graphics.Color.WHITE)
                    child.setTypeface(null, android.graphics.Typeface.NORMAL)
                    break
                }
            }
        }
    }

    private fun highlightNavigationItem(selectedItem: LinearLayout) {
        // Get the icon container (ConstraintLayout) - first child
        val iconContainer = selectedItem.getChildAt(0) as ConstraintLayout

        // Find TextView in LinearLayout
        var textView: android.widget.TextView? = null
        for (i in 0 until selectedItem.childCount) {
            val child = selectedItem.getChildAt(i)
            if (child is android.widget.TextView) {
                textView = child
                break
            }
        }

        // Find ImageView in ConstraintLayout
        var imageView: android.widget.ImageView? = null
        for (i in 0 until iconContainer.childCount) {
            val child = iconContainer.getChildAt(i)
            if (child is android.widget.ImageView) {
                imageView = child
                break
            }
        }

        // Set white rounded background only for icon container
        iconContainer.setBackgroundResource(R.drawable.selected_nav_background)

        // Set green color for icon
        imageView?.setColorFilter(android.graphics.Color.parseColor("#76E0C1"))

        // Keep text white but make it bold
        textView?.let { tv ->
            tv.setTextColor(android.graphics.Color.WHITE)
            tv.setTypeface(null, android.graphics.Typeface.BOLD)
        }
    }

    override fun onResume() {
        super.onResume()
        showSystemUI(white = false)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {

        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }


}