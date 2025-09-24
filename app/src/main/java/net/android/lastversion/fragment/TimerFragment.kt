package net.android.lastversion.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import net.android.lastversion.databinding.FragmentTimerBinding
import net.android.last.service.TimerService
import net.android.lastversion.R

class TimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private var selectedSoundUri: Uri? = null
    private var selectedResId: Int = R.raw.astro // Default sound
    private var totalSeconds = 0
    private var currentSeconds = 0
    private var isBound = false
    private var isPaused = false

    // Local CountDownTimer for UI updates
    private var uiTimer: CountDownTimer? = null

    // Sound and vibration for time's up
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    // Sound options: built-in + custom
    private val availableSounds = listOf(
        "Astro" to R.raw.astro,
        "Bell" to R.raw.bell,
        "Piano" to R.raw.piano,
        "Custom (Choose from device)" to -1
    )

    // Service connection
    private var serviceMessenger: Messenger? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceMessenger = Messenger(service)
            isBound = true
            Log.d("TimerFragment", "Service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            serviceMessenger = null
            Log.d("TimerFragment", "Service disconnected")
        }
    }

    // File picker for custom sounds - compatible with Android 9+
    private val soundPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { selectedUri ->
                try {
                    // Take persistent permission for the URI
                    requireContext().contentResolver.takePersistableUriPermission(
                        selectedUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    selectedSoundUri = selectedUri
                    selectedResId = -1 // Clear built-in sound
                    binding.tvSoundValue.text = getFileNameFromUri(selectedUri)
                    Log.d("TimerFragment", "Custom sound selected: ${selectedUri}")
                } catch (e: Exception) {
                    Log.e("TimerFragment", "Error selecting custom sound", e)
                    Toast.makeText(requireContext(), "Error selecting sound file", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPickers()
        setupButtons()
        setupSoundPicker()
        setupKeepScreen()

        // Initialize vibrator - Compatible with all Android versions
        vibrator = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.w("TimerFragment", "Could not initialize vibrator", e)
            null
        }

        // Set default sound name
        binding.tvSoundValue.text = "Astro"
    }

    private fun setupPickers() = with(binding) {
        try {
            // Set value ranges
            npHour.minValue = 0; npHour.maxValue = 23
            npMinute.minValue = 0; npMinute.maxValue = 59
            npSecond.minValue = 0; npSecond.maxValue = 59

            // Format to 2 digits
            val formatter = NumberPicker.Formatter { value -> "%02d".format(value) }
            npHour.setFormatter(formatter)
            npMinute.setFormatter(formatter)
            npSecond.setFormatter(formatter)

            // Block keyboard input
            npHour.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
            npMinute.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
            npSecond.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        } catch (e: Exception) {
            Log.e("TimerFragment", "Error setting up pickers", e)
        }
    }

    private fun setupSoundPicker() {
        binding.layoutSound.setOnClickListener {
            try {
                val names = availableSounds.map { it.first }.toTypedArray()
                AlertDialog.Builder(requireContext())
                    .setTitle("Select Sound")
                    .setItems(names) { _, which ->
                        val (name, resId) = availableSounds[which]
                        if (resId == -1) {
                            // Open file picker for custom sound
                            try {
                                soundPickerLauncher.launch(arrayOf("audio/*"))
                            } catch (e: Exception) {
                                Log.e("TimerFragment", "Error opening file picker", e)
                                Toast.makeText(requireContext(), "Could not open file picker", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Use built-in sound
                            binding.tvSoundValue.text = name
                            selectedResId = resId
                            selectedSoundUri = null // Clear custom URI
                            Log.d("TimerFragment", "Selected sound: $name (resId: $resId)")
                        }
                    }
                    .show()
            } catch (e: Exception) {
                Log.e("TimerFragment", "Error showing sound picker dialog", e)
            }
        }
    }

    private fun setupKeepScreen() {
        binding.switchKeepScreen.setOnCheckedChangeListener { _, isChecked ->
            try {
                if (isChecked) {
                    requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            } catch (e: Exception) {
                Log.e("TimerFragment", "Error managing keep screen flag", e)
            }
        }
    }

    private fun setupButtons() {
        binding.btnStartTimer.setOnClickListener {
            try {
                val h = binding.npHour.value
                val m = binding.npMinute.value
                val s = binding.npSecond.value
                totalSeconds = h * 3600 + m * 60 + s

                if (totalSeconds == 0) {
                    Toast.makeText(requireContext(), "Please set a time!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                startTimer()
            } catch (e: Exception) {
                Log.e("TimerFragment", "Error starting timer", e)
                Toast.makeText(requireContext(), "Error starting timer", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStop.setOnClickListener {
            try {
                if (isPaused) {
                    resumeTimer()
                } else {
                    pauseTimer()
                }
            } catch (e: Exception) {
                Log.e("TimerFragment", "Error toggling timer pause", e)
            }
        }

        // btnRestart now goes back to picker state
        binding.btnRestart.setOnClickListener {
            try {
                goBackToPicker()
            } catch (e: Exception) {
                Log.e("TimerFragment", "Error restarting timer", e)
            }
        }

        binding.btnStopTimesUp.setOnClickListener {
            try {
                stopTimerCompletely()
                stopAlerts()
                switchToPickerState()
            } catch (e: Exception) {
                Log.e("TimerFragment", "Error stopping timer from times up state", e)
            }
        }
    }

    private fun startTimer() {
        currentSeconds = totalSeconds

        // Start background service
        startBackgroundService()

        // Start UI timer
        startUITimer()

        switchToRunningState()
        isPaused = false

        Log.d("TimerFragment", "Timer started: $totalSeconds seconds")
    }

    private fun startBackgroundService() {
        try {
            val intent = Intent(requireContext(), TimerService::class.java).apply {
                action = TimerService.ACTION_START
                putExtra(TimerService.EXTRA_SECONDS, totalSeconds)
                // Pass both URI and resource ID to service
                selectedSoundUri?.let { putExtra(TimerService.EXTRA_SOUND_URI, it.toString()) }
                if (selectedResId != -1) {
                    putExtra(TimerService.EXTRA_SOUND_RES_ID, selectedResId)
                }
            }

            ContextCompat.startForegroundService(requireContext(), intent)
            requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e("TimerFragment", "Error starting background service", e)
            // Continue with UI timer even if service fails
        }
    }

    private fun startUITimer() {
        try {
            uiTimer?.cancel()

            val totalMillis = currentSeconds * 1000L

            uiTimer = object : CountDownTimer(totalMillis, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    currentSeconds = (millisUntilFinished / 1000).toInt() + 1
                    updateUI(currentSeconds, totalSeconds)
                }

                override fun onFinish() {
                    currentSeconds = 0
                    updateUI(0, totalSeconds)
                    onTimerFinished()
                }
            }.start()

            // Update UI immediately
            updateUI(currentSeconds, totalSeconds)
        } catch (e: Exception) {
            Log.e("TimerFragment", "Error starting UI timer", e)
        }
    }

    private fun pauseTimer() {
        try {
            uiTimer?.cancel()
            isPaused = true
            binding.btnStop.text = "Continue"

            // Pause background service
            val intent = Intent(requireContext(), TimerService::class.java).apply {
                action = TimerService.ACTION_STOP
            }
            requireContext().startService(intent)

            Log.d("TimerFragment", "Timer paused at $currentSeconds seconds")
        } catch (e: Exception) {
            Log.e("TimerFragment", "Error pausing timer", e)
        }
    }

    private fun resumeTimer() {
        try {
            startUITimer()
            isPaused = false
            binding.btnStop.text = "Stop"

            // Resume background service
            val intent = Intent(requireContext(), TimerService::class.java).apply {
                action = TimerService.ACTION_CONTINUE
            }
            requireContext().startService(intent)

            Log.d("TimerFragment", "Timer resumed from $currentSeconds seconds")
        } catch (e: Exception) {
            Log.e("TimerFragment", "Error resuming timer", e)
        }
    }

    // Go back to picker instead of restarting timer
    private fun goBackToPicker() {
        try {
            stopTimerCompletely()
            switchToPickerState()
            Log.d("TimerFragment", "Returned to picker state")
        } catch (e: Exception) {
            Log.e("TimerFragment", "Error going back to picker", e)
        }
    }

    private fun stopTimerCompletely() {
        try {
            uiTimer?.cancel()

            // Stop background service
            val intent = Intent(requireContext(), TimerService::class.java).apply {
                action = TimerService.ACTION_RESTART
            }
            requireContext().startService(intent)

            // Unbind service
            if (isBound) {
                try {
                    requireContext().unbindService(serviceConnection)
                } catch (e: Exception) {
                    Log.w("TimerFragment", "Error unbinding service: ${e.message}")
                }
                isBound = false
            }
        } catch (e: Exception) {
            Log.e("TimerFragment", "Error stopping timer completely", e)
        }
    }

    private fun onTimerFinished() {
        try {
            switchToTimesUpState()
            playFinishAlerts()
            Log.d("TimerFragment", "Timer finished!")
        } catch (e: Exception) {
            Log.e("TimerFragment", "Error handling timer finish", e)
        }
    }

    private fun updateUI(remaining: Int, total: Int) {
        try {
            val h = remaining / 3600
            val m = (remaining % 3600) / 60
            val s = remaining % 60

            binding.tvRH.text = "%02d".format(h)
            binding.tvRM.text = "%02d".format(m)
            binding.tvRS.text = "%02d".format(s)

            // Progress decreases from 1.0 to 0.0
            val progress = if (total > 0) remaining.toFloat() / total.toFloat() else 0f
            binding.progressRing.setProgress(progress)

            Log.d("TimerUI", "updateUI: remaining=$remaining, total=$total, progress=$progress")
        } catch (e: Exception) {
            Log.e("TimerFragment", "Error updating UI", e)
        }
    }

    private fun playFinishAlerts() {
        playBeepSound()
        vibrate()
    }

    private fun playBeepSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val notiUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val chosenUri = alarmUri ?: notiUri ?: ringUri

            ringtone = RingtoneManager.getRingtone(requireContext(), chosenUri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                play()
            }
        } catch (e: Exception) {
            Log.w("TimerFragment", "Error playing beep sound: ${e.message}")
        }
    }

    private fun vibrate() {
        try {
            vibrator?.let { vib ->
                val pattern = longArrayOf(0, 500, 300, 500, 300, 500)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(pattern, -1)
                    vib.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.w("TimerFragment", "Error vibrating: ${e.message}")
        }
    }

    private fun stopAlerts() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            Log.w("TimerFragment", "Error stopping ringtone: ${e.message}")
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w("TimerFragment", "Error stopping vibration: ${e.message}")
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        return try {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = it.getString(idx)
                        if (!name.isNullOrBlank()) {
                            return name
                        }
                    }
                }
            }
            "Custom Sound"
        } catch (e: Exception) {
            Log.w("TimerFragment", "Error getting file name from URI", e)
            "Custom Sound"
        }
    }

    private fun switchToRunningState() {
        try {
            binding.layoutPickers.visibility = View.GONE
            binding.layoutRunning.visibility = View.VISIBLE
            binding.layoutTimesUp.visibility = View.GONE
        } catch (e: Exception) {
            Log.e("TimerFragment", "Error switching to running state", e)
        }
    }

    private fun switchToPickerState() {
        try {
            binding.layoutPickers.visibility = View.VISIBLE
            binding.layoutRunning.visibility = View.GONE
            binding.layoutTimesUp.visibility = View.GONE
            binding.progressRing.setProgress(1f)
            binding.btnStop.text = "Stop"
            isPaused = false
            binding.tvTimesUpSubtitle.text = "00h 00m 00s"

            // Clear keep screen flag
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            binding.switchKeepScreen.isChecked = false
        } catch (e: Exception) {
            Log.e("TimerFragment", "Error switching to picker state", e)
        }
    }

    private fun switchToTimesUpState() {
        try {
            binding.layoutPickers.visibility = View.GONE
            binding.layoutRunning.visibility = View.GONE
            binding.layoutTimesUp.visibility = View.VISIBLE

            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            val s = totalSeconds % 60
            binding.tvTimesUpSubtitle.text = "%02dh %02dm %02ds".format(h, m, s)

            // Clear keep screen flag
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            Log.e("TimerFragment", "Error switching to times up state", e)
        }
    }

    override fun onDestroyView() {
        try {
            super.onDestroyView()
            uiTimer?.cancel()
            stopTimerCompletely()
            stopAlerts()
            _binding = null
        } catch (e: Exception) {
            Log.e("TimerFragment", "Error in onDestroyView", e)
        }
    }
}