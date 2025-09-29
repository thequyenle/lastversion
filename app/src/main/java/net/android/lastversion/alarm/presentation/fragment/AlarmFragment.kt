package net.android.lastversion.alarm.presentation.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import net.android.lastversion.HomeActivity
import net.android.lastversion.R
import net.android.lastversion.alarm.data.local.database.AlarmDatabase
import net.android.lastversion.alarm.data.preferences.AlarmPreferences
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.domain.usecase.*
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmSchedulerImpl
import net.android.lastversion.alarm.presentation.activity.SetAlarmActivity
import net.android.lastversion.alarm.presentation.adapter.AlarmAdapter
import net.android.lastversion.alarm.presentation.viewmodel.AlarmViewModel
import net.android.lastversion.alarm.presentation.viewmodel.AlarmViewModelFactory
import net.android.lastversion.alarm.presentation.utils.SwipeToDeleteCallback
import net.android.lastversion.alarm.presentation.utils.PermissionHelper
import net.android.lastversion.alarm.presentation.viewmodel.AlarmUiState
import net.android.lastversion.utils.showSystemUI


class AlarmFragment : Fragment() {

    // Use findViewById instead of binding to match existing layout
    private lateinit var recyclerViewAlarms: RecyclerView
    private lateinit var emptyView: View
    private lateinit var fabAddAlarm: FloatingActionButton

    private lateinit var alarmAdapter: AlarmAdapter
    private lateinit var permissionHelper: PermissionHelper

    private val alarmViewModel: AlarmViewModel by viewModels {
        AlarmViewModelFactory(
            repository = AlarmRepositoryImpl(AlarmDatabase.getDatabase(requireContext()).alarmDao()),
            alarmScheduler = AlarmSchedulerImpl(requireContext()),
            preferences = AlarmPreferences(requireContext())
        )
    }

    private val setAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                showSnackbar("Alarm saved")
            }
            SetAlarmActivity.RESULT_DELETED -> {
                showSnackbar("Alarm deleted")
            }
        }
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

        activity?.showSystemUI(white = false)
        initViews(view)
        permissionHelper = PermissionHelper(this)
        setupRecyclerView()
        setupFab()
        observeViewModel()
        checkPermissions()
    }

    private fun initViews(view: View) {
        recyclerViewAlarms = view.findViewById(R.id.recyclerViewAlarms)
        emptyView = view.findViewById(R.id.emptyView)
        fabAddAlarm = view.findViewById(R.id.fabAddAlarm)

        // Optional: Handle test buttons if they exist
        view.findViewById<View>(R.id.btnTestAlarm)?.setOnClickListener {
            // Test alarm functionality
            Toast.makeText(requireContext(), "Test alarm feature", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        alarmAdapter = AlarmAdapter(
            onItemClick = { alarm -> openSetAlarmActivity(alarm) },
            onSwitchToggle = { alarm -> alarmViewModel.toggleAlarm(alarm.id) }
        )

        recyclerViewAlarms.apply {
            adapter = alarmAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Swipe to delete
        val itemTouchHelper = ItemTouchHelper(
            SwipeToDeleteCallback { position ->
                val alarm = alarmAdapter.getAlarmAt(position)
                alarmViewModel.deleteAlarm(alarm)

                showSnackbar("Alarm deleted") {
                    alarmViewModel.saveAlarm(alarm)
                }
            }
        )
        itemTouchHelper.attachToRecyclerView(recyclerViewAlarms)
    }

    private fun setupFab() {
        fabAddAlarm.setOnClickListener {
            openSetAlarmActivity(null)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            alarmViewModel.uiState.collect { state ->
                handleUiState(state)
            }
        }
    }

    private fun handleUiState(state: AlarmUiState) {
        alarmAdapter.submitList(state.alarms)

        // Show/hide empty view
        emptyView.visibility = if (state.alarms.isEmpty()) View.VISIBLE else View.GONE
        recyclerViewAlarms.visibility = if (state.alarms.isEmpty()) View.GONE else View.VISIBLE

        state.error?.let { error ->
            showSnackbar("Error: $error")
            alarmViewModel.clearError()
        }
    }

    private fun openSetAlarmActivity(alarm: Alarm?) {
        val intent = Intent(requireContext(), SetAlarmActivity::class.java)
        alarm?.let {
            intent.putExtra(SetAlarmActivity.EXTRA_ALARM, it)
        }
        setAlarmLauncher.launch(intent)
    }

    private fun checkPermissions() {
        permissionHelper.checkAndRequestPermissions { allGranted ->
            if (!allGranted) {
                showSnackbar("Some permissions are required for alarms to work properly")
            }
        }
    }

    private fun showSnackbar(message: String, action: (() -> Unit)? = null) {
        val snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
        action?.let {
            snackbar.setAction("UNDO") { it() }
        }
        snackbar.show()
    }
    // Copy hàm extension đơn giản vào fragment
    private fun Activity.showSystemUI(white: Boolean = false) {
        if (white) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }
}