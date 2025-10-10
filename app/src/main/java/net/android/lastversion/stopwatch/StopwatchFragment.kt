package net.android.lastversion.stopwatch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

import net.android.lastversion.R

class StopwatchFragment : Fragment() {

    private lateinit var tvHour: TextView
    private lateinit var tvMinute: TextView
    private lateinit var tvSecond: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnContinue: Button
    private lateinit var btnRestart: Button
    private lateinit var layoutContinueRestart: View
    private lateinit var layoutTimerText: View

    private var seconds = 0
    private var isServiceRunning = false
    private var isPaused = false  // ✅ Add this to track paused state

    // ✅ Enhanced BroadcastReceiver to capture running/paused state
    private val tickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == StopwatchConst.ACTION_TICK) {
                seconds = intent.getIntExtra(StopwatchConst.EXTRA_ELAPSED, 0)
                isServiceRunning = intent.getBooleanExtra("isRunning", false)

                // ✅ Determine if paused based on state
                val state = intent.getStringExtra("state")
                isPaused = (state == "PAUSED")

                updateTimerText()

                // ✅ Update UI based on current state
                updateUIBasedOnState()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stopwatch, container, false)

        // TextViews
        tvHour = view.findViewById(R.id.tvHour)
        tvMinute = view.findViewById(R.id.tvMinute)
        tvSecond = view.findViewById(R.id.tvSecond)

        // Buttons
        btnStart = view.findViewById(R.id.btnStart)
        btnStop = view.findViewById(R.id.btnStop)
        btnContinue = view.findViewById(R.id.btnContinue)
        btnRestart = view.findViewById(R.id.btnRestart)
        layoutContinueRestart = view.findViewById(R.id.layoutContinueRestart)
        layoutTimerText = view.findViewById(R.id.layoutTimerText)

        btnStart.setOnClickListener {
            startStopwatchService()
            isPaused = false
            switchToStopUI()
        }

        btnStop.setOnClickListener {
            pauseStopwatchService()
            isPaused = true
            switchToContinueRestartUI()
        }

        btnContinue.setOnClickListener {
            resumeStopwatchService()
            isPaused = false
            switchToStopUI()
        }

        btnRestart.setOnClickListener {
            stopStopwatchService()
            seconds = 0
            isServiceRunning = false
            isPaused = false

            // ✅ Hide time temporarily, then show again
            layoutTimerText.visibility = View.GONE
            updateTimerText()

            // ✅ Show time again after 300ms
            Handler(Looper.getMainLooper()).postDelayed({
                layoutTimerText.visibility = View.VISIBLE
            }, 300)

            switchToStartUI()
        }

        // ✅ Restore state when returning to fragment
        restoreStopwatchState()

        return view
    }

    override fun onResume() {
        super.onResume()
        // ✅ Register receiver to receive updates from service
        val filter = IntentFilter(StopwatchConst.ACTION_TICK)
        requireContext().registerReceiver(tickReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

        // ✅ Request current state from service
        val intent = Intent(requireContext(), StopwatchService::class.java).apply {
            action = StopwatchConst.ACTION_GET_STATE
        }
        requireContext().startService(intent)
    }

    override fun onPause() {
        super.onPause()
        // ✅ Save current state including paused state
        saveStopwatchState()
    }

    private fun restoreStopwatchState() {
        val prefs = requireContext().getSharedPreferences("stopwatch_prefs", Context.MODE_PRIVATE)
        seconds = prefs.getInt("seconds", 0)
        isServiceRunning = prefs.getBoolean("is_running", false)
        isPaused = prefs.getBoolean("is_paused", false)  // ✅ Now properly read

        updateTimerText()

        // ✅ Restore UI state based on saved data
        when {
            isServiceRunning && !isPaused -> switchToStopUI()
            isPaused -> switchToContinueRestartUI()  // ✅ Simplified: if paused, show continue/restart
            else -> switchToStartUI()
        }
    }

    private fun saveStopwatchState() {
        val prefs = requireContext().getSharedPreferences("stopwatch_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("seconds", seconds)
            putBoolean("is_running", isServiceRunning)
            putBoolean("is_paused", isPaused)  // ✅ NOW SAVING PAUSED STATE
            apply()
        }
    }

    // ✅ New method to update UI based on current state from broadcast
    private fun updateUIBasedOnState() {
        when {
            isServiceRunning && !isPaused -> {
                // Currently running
                if (btnStop.visibility != View.VISIBLE) {
                    switchToStopUI()
                }
            }
            isPaused -> {
                // Currently paused
                if (layoutContinueRestart.visibility != View.VISIBLE) {
                    switchToContinueRestartUI()
                }
            }
            else -> {
                // Stopped
                if (btnStart.visibility != View.VISIBLE) {
                    switchToStartUI()
                }
            }
        }
    }

    private fun startStopwatchService() {
        isServiceRunning = true
        val intent = Intent(requireContext(), StopwatchService::class.java).apply {
            action = StopwatchConst.ACTION_START
        }
        requireContext().startService(intent)
    }

    private fun pauseStopwatchService() {
        val intent = Intent(requireContext(), StopwatchService::class.java).apply {
            action = StopwatchConst.ACTION_PAUSE
        }
        requireContext().startService(intent)
    }

    private fun resumeStopwatchService() {
        val intent = Intent(requireContext(), StopwatchService::class.java).apply {
            action = StopwatchConst.ACTION_RESUME
        }
        requireContext().startService(intent)
    }

    private fun stopStopwatchService() {
        isServiceRunning = false
        val intent = Intent(requireContext(), StopwatchService::class.java).apply {
            action = StopwatchConst.ACTION_STOP
        }
        requireContext().startService(intent)
    }

    private fun updateTimerText() {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60

        tvHour.text = String.format("%02d", hrs)
        tvMinute.text = String.format("%02d", mins)
        tvSecond.text = String.format("%02d", secs)
    }

    private fun switchToStartUI() {
        btnStart.visibility = View.VISIBLE
        btnStop.visibility = View.GONE
        layoutContinueRestart.visibility = View.GONE
        // ✅ Keep time visible always
        layoutTimerText.visibility = View.VISIBLE
    }

    private fun switchToStopUI() {
        btnStart.visibility = View.GONE
        btnStop.visibility = View.VISIBLE
        layoutContinueRestart.visibility = View.GONE
        layoutTimerText.visibility = View.VISIBLE
    }

    private fun switchToContinueRestartUI() {
        btnStart.visibility = View.GONE
        btnStop.visibility = View.GONE
        layoutContinueRestart.visibility = View.VISIBLE
        layoutTimerText.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cleanup: unregister receiver if still registered
        try {
            requireContext().unregisterReceiver(tickReceiver)
        } catch (e: Exception) {
            // Receiver may have already been unregistered
        }
    }
}