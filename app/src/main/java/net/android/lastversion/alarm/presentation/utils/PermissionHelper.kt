package net.android.lastversion.alarm.presentation.utils

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment

class PermissionHelper(private val fragment: Fragment) {

    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var exactAlarmLauncher: ActivityResultLauncher<Intent>? = null
    private var onPermissionResult: ((Boolean) -> Unit)? = null

    init {
        setupLaunchers()
    }

    private fun setupLaunchers() {
        permissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            onPermissionResult?.invoke(allGranted)
        }

        exactAlarmLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            checkAndRequestPermissions(onPermissionResult ?: {})
        }
    }

    fun checkAndRequestPermissions(callback: (Boolean) -> Unit) {
        onPermissionResult = callback

        val context = fragment.requireContext()
        val missingPermissions = mutableListOf<String>()

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Request basic permissions first
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher?.launch(missingPermissions.toTypedArray())
            return
        }

        // Check exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showExactAlarmDialog()
                return
            }
        }

        callback(true)
    }

    private fun showExactAlarmDialog() {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Exact Alarm Permission Required")
            .setMessage("For alarms to work reliably, please enable 'Alarms & reminders' permission.")
            .setPositiveButton("Open Settings") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    exactAlarmLauncher?.launch(intent)
                }
            }
            .setNegativeButton("Skip") { _, _ ->
                onPermissionResult?.invoke(false)
            }
            .show()
    }
}