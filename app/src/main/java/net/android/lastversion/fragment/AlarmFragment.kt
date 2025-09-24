package net.android.lastversion.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import net.android.lastversion.R
import net.android.lastversion.activity.SetAlarmActivity
import net.android.lastversion.activity.EditAlarmActivity
import net.android.lastversion.adapter.AlarmAdapter
import net.android.lastversion.model.Alarm

class AlarmFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddAlarm: FloatingActionButton
    private lateinit var emptyView: View
    private lateinit var alarmAdapter: AlarmAdapter
    private var alarmList = mutableListOf<Alarm>()

    // Activity Result Launcher for Set Alarm
    private val setAlarmLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Refresh alarm list after setting new alarm
            loadAlarms()
        }
    }

    // Activity Result Launcher for Edit Alarm
    private val editAlarmLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Refresh alarm list after editing alarm
            loadAlarms()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_alarm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        loadAlarms()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewAlarms)
        fabAddAlarm = view.findViewById(R.id.fabAddAlarm)
        emptyView = view.findViewById(R.id.emptyView)
    }

    private fun setupRecyclerView() {
        alarmAdapter = AlarmAdapter(alarmList) { alarm ->
            // Handle alarm item click - open edit alarm
            openEditAlarm(alarm)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = alarmAdapter
    }

    private fun setupClickListeners() {
        fabAddAlarm.setOnClickListener {
            openSetAlarm()
        }
    }

    private fun openSetAlarm() {
        val intent = Intent(requireContext(), SetAlarmActivity::class.java)
        setAlarmLauncher.launch(intent)
    }

    private fun openEditAlarm(alarm: Alarm) {
        val intent = Intent(requireContext(), EditAlarmActivity::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_HOUR", alarm.hour)
            putExtra("ALARM_MINUTE", alarm.minute)
            putExtra("ALARM_AMPM", alarm.amPm)
            putExtra("ALARM_DAYS", alarm.activeDays)
            putExtra("ALARM_SNOOZE", alarm.isSnoozeEnabled)
            putExtra("ALARM_VIBRATION", alarm.isVibrationEnabled)
            putExtra("ALARM_SOUND", alarm.isSoundEnabled)
            putExtra("ALARM_SILENT_MODE", alarm.isSilentModeEnabled)
            putExtra("ALARM_NOTE", alarm.note)
        }
        editAlarmLauncher.launch(intent)
    }

    private fun loadAlarms() {
        // Here you would load alarms from database or shared preferences
        // For demonstration, I'll create some sample alarms
        alarmList.clear()

        // Sample alarms - replace this with actual data loading
        val sampleAlarms = listOf(
            Alarm(1, 8, 0, "AM", "Alarm", booleanArrayOf(true, true, true, true, true, false, false),
                "Mon, Tue, Wed, Thu, Fri", true, true, false, true, true, ""),
            Alarm(2, 10, 0, "PM", "Brush teeth", booleanArrayOf(false, false, false, false, false, false, false),
                "Every day", false, true, false, true, true, ""),
            Alarm(3, 6, 0, "AM", "Wake up!!", booleanArrayOf(false, false, false, false, false, false, false),
                "Every day", false, true, false, true, true, "")
        )

        alarmList.addAll(sampleAlarms)
        alarmAdapter.notifyDataSetChanged()

        // Show/hide empty view
        if (alarmList.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = AlarmFragment()
    }
}