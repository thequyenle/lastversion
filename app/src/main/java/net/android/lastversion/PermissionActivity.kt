package net.android.lastversion

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionActivity : AppCompatActivity() {

    private val REQ_CODE_CAMERA = 100
    private val REQ_CODE_STORAGE = 101
    private val REQ_CODE_NOTIFICATION = 102

    private lateinit var btnContinue: Button
    private lateinit var btnCameraToggle: ImageButton
    private lateinit var btnStorageToggle: ImageButton
    private lateinit var btnNotificationToggle: ImageButton

    // Track states
    private var isCameraEnabled = false
    private var isStorageEnabled = false
    private var isNotificationEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        // Bind views
        btnContinue = findViewById(R.id.btnContinue)
        btnCameraToggle = findViewById(R.id.btnCameraToggle)
        btnStorageToggle = findViewById(R.id.btnStorageToggle)
        btnNotificationToggle = findViewById(R.id.btnNotificationToggle)

        // Notification button only visible on Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.ctr3).visibility = View.VISIBLE
        } else {
            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.ctr3).visibility = View.GONE
        }

        // Initialize button states
        updateToggleStates()
        updateButtonVisibility()

        // ImageButton listeners
        btnCameraToggle.setOnClickListener {
            toggleCameraPermission()
        }

        btnStorageToggle.setOnClickListener {
            toggleStoragePermission()
        }

        btnNotificationToggle.setOnClickListener {
            toggleNotificationPermission()
        }

        btnContinue.setOnClickListener {
            continueToHome()
        }
    }

    private fun toggleCameraPermission() {
        if (!isCameraEnabled) {
            // Request permission when toggling ON
            requestPermission(Manifest.permission.CAMERA, REQ_CODE_CAMERA)
        } else {
            // Show message that permission is already granted
            Toast.makeText(this, "Camera permission already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleStoragePermission() {
        if (!isStorageEnabled) {
            // Request permission when toggling ON
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQ_CODE_STORAGE)
        } else {
            // Show message that permission is already granted
            Toast.makeText(this, "Storage permission already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleNotificationPermission() {
        if (!isNotificationEnabled && Build.VERSION.SDK_INT >= 33) {
            // Request permission when toggling ON
            requestPermission(Manifest.permission.POST_NOTIFICATIONS, REQ_CODE_NOTIFICATION)
        } else if (Build.VERSION.SDK_INT >= 33) {
            // Show message that permission is already granted
            Toast.makeText(this, "Notification permission already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateToggleStates() {
        // Check current permission states
        isCameraEnabled = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        isStorageEnabled = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        isNotificationEnabled = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No permission needed for notification on Android < 13
        }

        // Update button images
        btnCameraToggle.setImageResource(
            if (isCameraEnabled) R.drawable.ic_switch_on else R.drawable.ic_switch_off
        )

        btnStorageToggle.setImageResource(
            if (isStorageEnabled) R.drawable.ic_switch_on else R.drawable.ic_switch_off
        )

        btnNotificationToggle.setImageResource(
            if (isNotificationEnabled) R.drawable.ic_switch_on else R.drawable.ic_switch_off
        )
    }

    private fun requestPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
        } else {
            updateToggleStates()
            updateButtonVisibility()
        }
    }

    override fun onRequestPermissionsResult(req: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(req, perms, results)

        if (results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }

        // Update states and visibility
        updateToggleStates()
        updateButtonVisibility()
    }

    private fun updateButtonVisibility() {
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        // Check notification permission only if container is visible (Android 13+)
        val notificationGranted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No permission needed for notification on Android < 13
        }

        // Show button only if all required permissions are granted
        val allPermissionsGranted = if (Build.VERSION.SDK_INT >= 33) {
            cameraGranted && storageGranted && notificationGranted
        } else {
            cameraGranted && storageGranted
        }

        btnContinue.visibility = if (allPermissionsGranted) View.VISIBLE else View.GONE
    }

    private fun continueToHome() {
        getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
            .edit().putBoolean("perm_done", true).apply()
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        showSystemUI(white = false)
        // Update states when returning from settings
        updateToggleStates()
        updateButtonVisibility()
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