package net.android.lastversion

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Setup custom navigation with LinearLayout
        setupCustomNavigation(navController)
    }

    private fun setupCustomNavigation(navController: androidx.navigation.NavController) {
        // Get references to navigation items
        val navAlarm = findViewById<LinearLayout>(R.id.nav_alarm)
        val navClock = findViewById<LinearLayout>(R.id.nav_clock)
        val navStopwatch = findViewById<LinearLayout>(R.id.nav_stopwatch)
        val navTimer = findViewById<LinearLayout>(R.id.nav_timer)
        val navSettings = findViewById<LinearLayout>(R.id.nav_settings)

        // Set click listeners for navigation items
        navAlarm.setOnClickListener {
            navController.navigate(R.id.alarmFragment)
        }

        navClock.setOnClickListener {
            navController.navigate(R.id.clockFragment)
        }

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
                R.id.clockFragment -> highlightNavigationItem(navClock)
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
            findViewById<ConstraintLayout>(R.id.clock_icon_container),
            findViewById<ConstraintLayout>(R.id.stopwatch_icon_container),
            findViewById<ConstraintLayout>(R.id.timerIconContainer),
            findViewById<ConstraintLayout>(R.id.settings_icon_container)
        )

        val navItems = listOf(
            findViewById<LinearLayout>(R.id.nav_alarm),
            findViewById<LinearLayout>(R.id.nav_clock),
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
}