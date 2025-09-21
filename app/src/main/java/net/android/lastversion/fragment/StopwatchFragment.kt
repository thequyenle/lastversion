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
    private lateinit var btnReset: Button

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

        tvHour = view.findViewById(R.id.tvHour)
        tvMinute = view.findViewById(R.id.tvMinute)
        tvSecond = view.findViewById(R.id.tvSecond)
        btnStart = view.findViewById(R.id.btnStart)
        btnStop = view.findViewById(R.id.btnStop)
        btnReset = view.findViewById(R.id.btnReset)

        btnStart.setOnClickListener {
            if (!isRunning) {
                isRunning = true
                handler.post(runnable)
            }
        }

        btnStop.setOnClickListener {
            isRunning = false
        }

        btnReset.setOnClickListener {
            isRunning = false
            seconds = 0
            updateTimerText()
        }

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

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(runnable)
    }
}
