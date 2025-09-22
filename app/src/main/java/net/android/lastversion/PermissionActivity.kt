package net.android.lastversion

import android.Manifest
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
    private lateinit var switchCamera: Switch
    private lateinit var switchStorage: Switch
    private lateinit var switchNotification: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        // Bind views
        btnContinue = findViewById(R.id.btnContinue)
        switchCamera = findViewById(R.id.switchCamera)
        switchStorage = findViewById(R.id.switchStorage)
        switchNotification = findViewById(R.id.switchNotification)

        // Notification switch only visible on Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            switchNotification.visibility = View.VISIBLE
        } else {
            switchNotification.visibility = View.GONE
        }

        // Initial visibility check
        updateButtonVisibility()

        // Switch listeners
        switchCamera.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestPermission(Manifest.permission.CAMERA, REQ_CODE_CAMERA)
            }
        }

        switchStorage.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQ_CODE_STORAGE)
            }
        }

        switchNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && Build.VERSION.SDK_INT >= 33) {
                requestPermission(Manifest.permission.POST_NOTIFICATIONS, REQ_CODE_NOTIFICATION)
            }
        }

        btnContinue.setOnClickListener {
            continueToHome()
        }
    }

    private fun requestPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
        } else {
            updateButtonVisibility()
        }
    }

    override fun onRequestPermissionsResult(req: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(req, perms, results)
        if (results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            when (req) {
                REQ_CODE_CAMERA -> switchCamera.isChecked = false
                REQ_CODE_STORAGE -> switchStorage.isChecked = false
                REQ_CODE_NOTIFICATION -> switchNotification.isChecked = false
            }
        }

        updateButtonVisibility()
    }

    private fun updateButtonVisibility() {
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val notificationGranted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No permission needed for notification on Android < 13
        }

        // Show button only if all required permissions are granted
        btnContinue.visibility = if (cameraGranted && storageGranted && notificationGranted) View.VISIBLE else View.GONE
    }

    private fun continueToHome() {
        getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
            .edit().putBoolean("perm_done", true).apply()
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}