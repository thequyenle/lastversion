package net.android.lastversion

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionActivity : BaseActivity() {

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
            // Permission denied - show dialog to go to settings
            showPermissionDeniedDialog(perms[0])
        }

        // Update states and visibility
        updateToggleStates()
        updateButtonVisibility()
    }

    private fun showPermissionDeniedDialog(permission: String) {
        val permissionName = when (permission) {
            Manifest.permission.CAMERA -> "Camera"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage"
            Manifest.permission.POST_NOTIFICATIONS -> "Notification"
            else -> "This"
        }

        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("$permissionName permission is required for the app to work properly. Please enable it in app settings.")
            .setPositiveButton("Go to Settings") { dialog, _ ->
                dialog.dismiss()
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun updateButtonVisibility() {
        // Always show the continue button regardless of permission status
        btnContinue.visibility = View.VISIBLE
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