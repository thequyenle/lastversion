package net.android.lastversion.fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.Fragment
import net.android.lastversion.R

class TimerFragment : Fragment() {

    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var secondPicker: NumberPicker
    private lateinit var btnStart: Button
    private lateinit var switchKeepScreen: Switch
    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timer, container, false)

        // Gán view
        hourPicker = view.findViewById(R.id.npHour)
        minutePicker = view.findViewById(R.id.npMinute)
        secondPicker = view.findViewById(R.id.npSecond)
        btnStart = view.findViewById(R.id.btnStartTimer)
        switchKeepScreen = view.findViewById(R.id.switchKeepScreen)

        // Set giá trị giới hạn
        hourPicker.minValue = 0
        hourPicker.maxValue = 99
        minutePicker.minValue = 0
        minutePicker.maxValue = 59
        secondPicker.minValue = 0
        secondPicker.maxValue = 59

        btnStart.setOnClickListener {
            startTimer()
        }

        return view
    }

    private fun startTimer() {
        val totalMillis = (
                hourPicker.value * 3600 +
                        minutePicker.value * 60 +
                        secondPicker.value
                ) * 1000L

        if (totalMillis <= 0) {
            Toast.makeText(requireContext(), "Please set time", Toast.LENGTH_SHORT).show()
            return
        }

        if (switchKeepScreen.isChecked) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        btnStart.isEnabled = false

        countDownTimer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = (millisUntilFinished / 1000) / 3600
                val minutes = ((millisUntilFinished / 1000) % 3600) / 60
                val seconds = (millisUntilFinished / 1000) % 60

                hourPicker.value = hours.toInt()
                minutePicker.value = minutes.toInt()
                secondPicker.value = seconds.toInt()
            }

            override fun onFinish() {
                Toast.makeText(requireContext(), "Time’s up!", Toast.LENGTH_SHORT).show()
                btnStart.isEnabled = true
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }
}
