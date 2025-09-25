package net.android.lastversion.alarm.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import net.android.lastversion.R
import net.android.lastversion.alarm.presentation.adapter.AlarmAdapter
import net.android.lastversion.alarm.data.database.AlarmDatabase
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.domain.repository.AlarmRepository
import net.android.lastversion.alarm.presentation.viewmodel.AlarmViewModel
import net.android.lastversion.alarm.presentation.viewmodel.AlarmViewModelFactory

class AlarmFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var alarmAdapter: AlarmAdapter
    private lateinit var fabAddAlarm: FloatingActionButton
    private lateinit var emptyView: View

    private val alarmViewModel: AlarmViewModel by viewModels {
        AlarmViewModelFactory(
            AlarmRepository(
                AlarmDatabase.getDatabase(requireContext()).alarmDao()
            )
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
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewAlarms)
        fabAddAlarm = view.findViewById(R.id.fabAddAlarm)
        emptyView = view.findViewById(R.id.emptyView)
    }

    private fun setupRecyclerView() {
        alarmAdapter = AlarmAdapter(
            onItemClick = { alarm ->
                // Handle item click - mở dialog edit alarm
                showEditAlarmDialog(alarm)
            },
            onSwitchToggle = { alarm ->
                // Toggle alarm on/off
                alarmViewModel.toggleAlarm(alarm)
            }
        )

        recyclerView.apply {
            adapter = alarmAdapter
            layoutManager = LinearLayoutManager(requireContext())

            // Thêm swipe to delete
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
            showAddAlarmDialog()
        }
    }

    private fun showAddAlarmDialog() {
        // Tạo alarm mới với giá trị mặc định
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
            activeDays = BooleanArray(7) { false }, // Tất cả ngày đều tắt
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

    private fun showEditAlarmDialog(alarm: Alarm) {
        // TODO: Implement edit alarm dialog
        // Bạn có thể tạo một dialog hoặc navigate đến một fragment khác để edit
        Snackbar.make(requireView(), "Edit alarm: ${alarm.getTimeString()}", Snackbar.LENGTH_SHORT).show()
    }

    // Inner class cho swipe to delete
    inner class SwipeToDeleteCallback : ItemTouchHelper.SimpleCallback(
        0, // dragDirs = 0 (không cho phép drag)
        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // swipeDirs
    ) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false // Không support drag & drop
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val alarm = alarmAdapter.getAlarmAt(position)

            // Xóa alarm
            alarmViewModel.deleteAlarm(alarm)

            // Hiện snackbar với undo option
            Snackbar.make(requireView(), "Alarm deleted", Snackbar.LENGTH_LONG)
                .setAction("UNDO") {
                    // Khôi phục alarm
                    alarmViewModel.insertAlarm(alarm)
                }
                .show()
        }
    }
}