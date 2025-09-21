package net.android.lastversion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionActivity : AppCompatActivity() {
    private val REQ_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            if (checkPermissions()) {
                continueToHome()
            } else {
                requestPermissions()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.POST_NOTIFICATIONS
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
        ActivityCompat.requestPermissions(this, arrayOf(permission), REQ_CODE)
    }

    override fun onRequestPermissionsResult(req: Int, p: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(req, p, res)
        if (req == REQ_CODE) {
            if (res.isNotEmpty() && res[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                continueToHome()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun continueToHome() {
        getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
            .edit().putBoolean("perm_done", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}