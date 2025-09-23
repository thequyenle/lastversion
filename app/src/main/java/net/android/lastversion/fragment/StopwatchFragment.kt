package net.android.lastversion.fragment

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
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())

    private val runnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                seconds++
                updateTimerText()
                handler.postDelayed(this, 1000)
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
            isRunning = true
            handler.post(runnable)
            switchToStopUI()
            layoutTimerText.visibility = View.VISIBLE

        }

        btnStop.setOnClickListener {
            isRunning = false
            switchToContinueRestartUI()
        }

        btnContinue.setOnClickListener {
            isRunning = true
            handler.post(runnable)
            switchToStopUI()
        }

        btnRestart.setOnClickListener {
            isRunning = false
            seconds = 0
            updateTimerText()
            switchToStartUI()

        }

        updateTimerText()
        return view
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
        handler.removeCallbacks(runnable)
    }
}

