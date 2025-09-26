package net.android.lastversion.alarm.presentation.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.os.Bundle
import android.util.Log
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import net.android.lastversion.R
import net.android.lastversion.alarm.activity.SetAlarmActivity
import net.android.lastversion.alarm.presentation.adapter.AlarmAdapter
import net.android.lastversion.alarm.data.database.AlarmDatabase
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.infrastructure.receiver.BootReceiver
import net.android.lastversion.alarm.presentation.viewmodel.AlarmViewModel
import net.android.lastversion.alarm.presentation.viewmodel.AlarmViewModelFactory
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmSchedulerImpl
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class AlarmFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var alarmAdapter: AlarmAdapter
    private lateinit var fabAddAlarm: FloatingActionButton
    private lateinit var emptyView: View

    private val setAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                Snackbar.make(requireView(), "Alarm saved", Snackbar.LENGTH_SHORT).show()
            }
            SetAlarmActivity.RESULT_DELETED -> {
                Snackbar.make(requireView(), "Alarm deleted", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private val alarmViewModel: AlarmViewModel by viewModels {
        AlarmViewModelFactory(
            AlarmRepositoryImpl(
                AlarmDatabase.getDatabase(requireContext()).alarmDao()
            ),
            requireContext().applicationContext // ← Add context parameter
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_alarm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupObservers()
        setupFab()

        // Check permissions when fragment is created
        checkAllPermissions()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewAlarms)
        fabAddAlarm = view.findViewById(R.id.fabAddAlarm)
        emptyView = view.findViewById(R.id.emptyView)

        // Add test button functionality (if you have a test button in layout)
        view.findViewById<View>(R.id.btnTestAlarm)?.setOnClickListener {
            createTestAlarm()
        }
        // New boot receiver test button
        view.findViewById<Button>(R.id.btnTestBootReceiver)?.setOnClickListener {
            testBootReceiver()
        }
    }

    private fun checkAllPermissions() {
        Log.d("PermissionCheck", "=== CHECKING PERMISSIONS ===")

        // 1. Check Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            Log.d("PermissionCheck", "Notification Permission: $hasNotificationPermission")

            if (!hasNotificationPermission) {
                Log.w("PermissionCheck", "REQUESTING notification permission...")
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // 2. Check Exact Alarm Permission (Android 12+) - QUAN TRỌNG NHẤT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val canScheduleExact = alarmManager.canScheduleExactAlarms()

            Log.d("PermissionCheck", "Can Schedule Exact Alarms: $canScheduleExact")

            if (!canScheduleExact) {
                Log.e("PermissionCheck", "CANNOT SCHEDULE EXACT ALARMS - THIS IS THE PROBLEM!")
                showExactAlarmPermissionDialog()
            }
        }

        // 3. Check Notifications Enabled
        val notificationManager = NotificationManagerCompat.from(requireContext())
        val notificationsEnabled = notificationManager.areNotificationsEnabled()
        Log.d("PermissionCheck", "Notifications Enabled: $notificationsEnabled")

        // 4. Check Battery Optimization
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
            Log.d("PermissionCheck", "Ignoring Battery Optimization: $isIgnoringBatteryOptimizations")

            if (!isIgnoringBatteryOptimizations) {
                Log.w("PermissionCheck", "App is being battery optimized - may affect alarms!")
                showBatteryOptimizationDialog()
            }
        }

        Log.d("PermissionCheck", "=== PERMISSION CHECK COMPLETE ===")
    }

    private fun showExactAlarmPermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage("App needs permission to set exact alarms. Please enable 'Alarms & reminders' in the next screen.")
            .setPositiveButton("Open Settings") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBatteryOptimizationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Battery Optimization")
            .setMessage("For reliable alarms, please disable battery optimization for this app.")
            .setPositiveButton("Open Settings") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${requireContext().packageName}")
                    }
                    startActivity(intent)
                }
            }
            .setNegativeButton("Skip", null)
            .show()
    }

    private fun createTestAlarm() {
        Log.d("TestAlarm", "Creating test alarm 30 seconds from now...")

        try {
            // Check permissions first
            if (!checkRequiredPermissions()) {
                Toast.makeText(requireContext(), "Missing required permissions", Toast.LENGTH_LONG).show()
                return
            }

            val testAlarm = Alarm(
                id = 99999,
                hour = 12,
                minute = 0,
                amPm = "PM",
                label = "Test Alarm - 30s",
                note = "Test",
                isEnabled = true,
                isSnoozeEnabled = true,
                isVibrationEnabled = true,
                isSoundEnabled = true,
                activeDays = BooleanArray(7) { false }
            )

            // Test alarm 30 seconds from now
            val testTime = System.currentTimeMillis() + (30 * 1000)
            val timeText = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(testTime))

            Log.d("TestAlarm", "Test time: $timeText")

            val alarmScheduler = AlarmSchedulerImpl(requireContext())
            alarmScheduler.scheduleAlarm(99999, testTime, testAlarm)

            Toast.makeText(requireContext(), "Test alarm set for $timeText (30 seconds from now)", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e("TestAlarm", "Error creating test alarm", e)
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkRequiredPermissions(): Boolean {
        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPerm = ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPerm) {
                Log.e("TestAlarm", "Missing POST_NOTIFICATIONS permission")
                return false
            }
        }

        // Check exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val canScheduleExact = alarmManager.canScheduleExactAlarms()

            if (!canScheduleExact) {
                Log.e("TestAlarm", "Missing SCHEDULE_EXACT_ALARM permission")
                return false
            }
        }

        return true
    }

    private fun setupRecyclerView() {
        alarmAdapter = AlarmAdapter(
            onItemClick = { alarm ->
                openSetAlarmActivity(alarm)
            },
            onSwitchToggle = { alarm ->
                alarmViewModel.toggleAlarm(alarm)
            }
        )

        recyclerView.apply {
            adapter = alarmAdapter
            layoutManager = LinearLayoutManager(requireContext())

            // Add swipe to delete
            val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback())
            itemTouchHelper.attachToRecyclerView(this)
        }
    }

    private fun setupObservers() {
        alarmViewModel.allAlarms.observe(viewLifecycleOwner, Observer { alarms ->
            alarmAdapter.submitList(alarms)

            // Hiện/ẩn empty view
            if (alarms.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
            }
        })
    }

    private fun setupFab() {
        fabAddAlarm.setOnClickListener {
            openSetAlarmActivity(null) // null = add new alarm mode
        }
    }

    private fun openSetAlarmActivity(alarm: Alarm?) {
        val intent = Intent(requireContext(), SetAlarmActivity::class.java)

        alarm?.let {
            intent.putExtra(SetAlarmActivity.EXTRA_ALARM_ID, it.id)
            intent.putExtra(SetAlarmActivity.EXTRA_HOUR, it.hour)
            intent.putExtra(SetAlarmActivity.EXTRA_MINUTE, it.minute)
            intent.putExtra(SetAlarmActivity.EXTRA_AM_PM, it.amPm)
            intent.putExtra(SetAlarmActivity.EXTRA_LABEL, it.label)
            intent.putExtra(SetAlarmActivity.EXTRA_ACTIVE_DAYS, it.activeDays)
            intent.putExtra(SetAlarmActivity.EXTRA_IS_SNOOZE_ENABLED, it.isSnoozeEnabled)
            intent.putExtra(SetAlarmActivity.EXTRA_IS_VIBRATION_ENABLED, it.isVibrationEnabled)
            intent.putExtra(SetAlarmActivity.EXTRA_IS_SOUND_ENABLED, it.isSoundEnabled)
            intent.putExtra(SetAlarmActivity.EXTRA_IS_SILENT_MODE_ENABLED, it.isSilentModeEnabled)
            intent.putExtra(SetAlarmActivity.EXTRA_NOTE, it.note)
            intent.putExtra(SetAlarmActivity.EXTRA_IS_EDIT_MODE, true)
        }

        setAlarmLauncher.launch(intent)
    }

    private fun showEditAlarmDialog(alarm: Alarm) {
        openSetAlarmActivity(alarm)
    }

    private fun showAddAlarmDialog() {
        val currentTime = java.util.Calendar.getInstance()
        val hour = currentTime.get(java.util.Calendar.HOUR)
        val minute = currentTime.get(java.util.Calendar.MINUTE)
        val amPm = if (currentTime.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "AM" else "PM"

        val newAlarm = Alarm(
            id = 0, // Auto-generate
            hour = if (hour == 0) 12 else hour,
            minute = minute,
            amPm = amPm,
            label = "Alarm",
            activeDays = BooleanArray(7) { false },
            activeDaysText = "Never",
            isEnabled = true,
            isSnoozeEnabled = true,
            isVibrationEnabled = true,
            isSoundEnabled = true,
            isSilentModeEnabled = false,
            note = ""
        )

        alarmViewModel.insertAlarm(newAlarm)
        Snackbar.make(requireView(), "Alarm added", Snackbar.LENGTH_SHORT).show()
    }

    inner class SwipeToDeleteCallback : ItemTouchHelper.SimpleCallback(
        0, // dragDirs = 0
        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // swipeDirs
    ) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val alarm = alarmAdapter.getAlarmAt(position)

            alarmViewModel.deleteAlarm(alarm)

            Snackbar.make(requireView(), "Alarm deleted", Snackbar.LENGTH_LONG)
                .setAction("UNDO") {
                    alarmViewModel.insertAlarm(alarm)
                }
                .show()
        }
    }

    // Handle permission results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PermissionCheck", "Notification permission granted")
                    Toast.makeText(requireContext(), "Notification permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w("PermissionCheck", "Notification permission denied")
                    Toast.makeText(requireContext(), "Notification permission is required for alarms", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when coming back from settings
        checkAllPermissions()
    }

    // Thêm ở cuối class, trước dấu } cuối
    private fun testBootReceiver() {
        Log.d("AlarmFragment", "Testing BootReceiver logic directly...")

        try {
            Log.d("AlarmFragment", "Step 1: Creating BootReceiver...")
            val bootReceiver = BootReceiver()
            Log.d("AlarmFragment", "Step 2: BootReceiver created successfully")

            Log.d("AlarmFragment", "Step 3: Creating intent...")
            val testIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
            Log.d("AlarmFragment", "Step 4: Intent created successfully")

            Log.d("AlarmFragment", "Step 5: Calling onReceive...")
            bootReceiver.onReceive(requireContext(), testIntent)
            Log.d("AlarmFragment", "Step 6: onReceive completed")

        } catch (e: Exception) {
            Log.e("AlarmFragment", "Exception at step: ${e.message}", e)
        }
    }

    // Test button để call function này

}