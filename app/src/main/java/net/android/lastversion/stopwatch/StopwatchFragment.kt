package net.android.lastversion.stopwatch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

import net.android.lastversion.R
import net.android.lastversion.utils.showSystemUI

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

    // BroadcastReceiver để nhận tick từ Service
    private val tickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == StopwatchConst.ACTION_TICK) {
                seconds = intent.getIntExtra(StopwatchConst.EXTRA_ELAPSED, 0)
                updateTimerText()
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
            switchToStopUI()
            layoutTimerText.visibility = View.VISIBLE
        }

        btnStop.setOnClickListener {
            pauseStopwatchService()
            switchToContinueRestartUI()
        }

        btnContinue.setOnClickListener {
            resumeStopwatchService()
            switchToStopUI()
        }

        btnRestart.setOnClickListener {
            stopStopwatchService()
            seconds = 0
            updateTimerText()
            switchToStartUI()
        }

        updateTimerText()
        return view
    }

    // Thêm method này
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.showSystemUI(white = false)
    }

    override fun onResume() {
        super.onResume()
        activity?.showSystemUI(white = false)

        // Set SystemUI

        // Đăng ký nhận broadcast từ Service
        val filter = IntentFilter(StopwatchConst.ACTION_TICK)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(tickReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(tickReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        }

        promoteServiceToForeground()
    }

    override fun onPause() {
        super.onPause()
        // Hủy đăng ký broadcast
        requireContext().unregisterReceiver(tickReceiver)

        // Demote service khỏi foreground khi app đang chạy
        demoteServiceFromForeground()
    }

    private fun startStopwatchService() {
        val intent = Intent(requireContext(), StopwatchService::class.java).apply {
            action = StopwatchConst.ACTION_START
        }
        requireContext().startService(intent)
        isServiceRunning = true
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
        val intent = Intent(requireContext(), StopwatchService::class.java).apply {
            action = StopwatchConst.ACTION_STOP
        }
        requireContext().startService(intent)
        isServiceRunning = false
    }

    private fun promoteServiceToForeground() {
        if (isServiceRunning) {
            val intent = Intent(requireContext(), StopwatchService::class.java).apply {
                action = StopwatchConst.ACTION_PROMOTE_FOREGROUND
            }
            requireContext().startService(intent)
        }
    }

    private fun demoteServiceFromForeground() {
        if (isServiceRunning) {
            val intent = Intent(requireContext(), StopwatchService::class.java).apply {
                action = StopwatchConst.ACTION_DEMOTE_FOREGROUND
            }
            requireContext().startService(intent)
        }
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
        layoutTimerText.visibility = View.GONE
    }

    private fun switchToStopUI() {
        btnStart.visibility = View.GONE
        btnStop.visibility = View.VISIBLE
        layoutContinueRestart.visibility = View.GONE
    }

    private fun switchToContinueRestartUI() {
        btnStart.visibility = View.GONE
        btnStop.visibility = View.GONE
        layoutContinueRestart.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cleanup: bỏ đăng ký receiver nếu còn
        try {
            requireContext().unregisterReceiver(tickReceiver)
        } catch (e: Exception) {
            // Receiver có thể đã được unregister rồi
        }
    }
}