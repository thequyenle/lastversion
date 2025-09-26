package net.android.lastversion.timer

import android.content.Intent
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
    private var selectedResId: Int = R.raw.astro
    private var totalSeconds = 0
    private var currentSeconds = 0
    private var isPaused = false

    // Handler để sync với Service
    private var syncHandler: Handler? = null
    private var syncRunnable: Runnable? = null

    private val availableSounds = listOf(
        "Astro" to R.raw.astro,
        "Bell" to R.raw.bell,
        "Piano" to R.raw.piano,
        "Custom (From device)" to -1
    )

    // Sound picker
    private val soundPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                requireContext().contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                selectedSoundUri = it
                selectedResId = -1
                binding.tvSoundValue.text = getFileNameFromUri(it)
            } catch (e: Exception) {
                Log.e("TimerFragment", "Error selecting sound", e)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()

        // Check if timer is already running
        if (TimerService.isServiceRunning) {
            currentSeconds = TimerService.currentRemainingSeconds
            totalSeconds = TimerService.currentTotalSeconds
            isPaused = TimerService.isCurrentlyPaused

            if (currentSeconds > 0) {
                // Timer still running
                switchToRunningState()
                startSyncTimer()
            } else {
                // Timer completed (currentSeconds = 0)
                switchToTimesUpState()
            }
        }
    }

    private fun setupUI() {
        setupPickers()
        setupSoundPicker()
        setupKeepScreen()
        setupButtons()
        binding.tvSoundValue.text = "Astro"
    }

    private fun setupPickers() = with(binding) {
        npHour.minValue = 0; npHour.maxValue = 23
        npMinute.minValue = 0; npMinute.maxValue = 59
        npSecond.minValue = 0; npSecond.maxValue = 59

        val formatter = NumberPicker.Formatter { "%02d".format(it) }
        npHour.setFormatter(formatter)
        npMinute.setFormatter(formatter)
        npSecond.setFormatter(formatter)

        npHour.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        npMinute.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        npSecond.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
    }

    private fun setupSoundPicker() {
        binding.layoutSound.setOnClickListener {
            val names = availableSounds.map { it.first }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Select Sound")
                .setItems(names) { _, which ->
                    val (name, resId) = availableSounds[which]
                    if (resId == -1) {
                        soundPickerLauncher.launch(arrayOf("audio/*"))
                    } else {
                        binding.tvSoundValue.text = name
                        selectedResId = resId
                        selectedSoundUri = null
                    }
                }
                .show()
        }
    }

    private fun setupKeepScreen() {
        binding.switchKeepScreen.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private fun setupButtons() {
        binding.btnStartTimer.setOnClickListener { startTimer() }
        binding.btnRestart.setOnClickListener { goBackToPicker() }
        binding.btnStopTimesUp.setOnClickListener { goBackToPicker() }

        // Pause/Resume button with debounce
        var lastClickTime = 0L
        val debounceDelay = 500L
        binding.btnStop.setOnClickListener {
            val current = System.currentTimeMillis()
            if (current - lastClickTime < debounceDelay) return@setOnClickListener
            lastClickTime = current
            togglePauseResume()
        }
    }

    private fun togglePauseResume() {
        if (isPaused) {
            resumeTimer()
        } else {
            pauseTimer()
        }
    }

    private fun pauseTimer() {
        isPaused = true

        // Send pause command to service
        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        requireContext().startService(intent)

        binding.btnStop.text = "Continue"
        Log.d("TimerFragment", "Timer paused")
    }

    private fun resumeTimer() {
        isPaused = false

        // Send resume command to service
        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME
        }
        requireContext().startService(intent)

        binding.btnStop.text = "Stop"
        Log.d("TimerFragment", "Timer resumed")
    }

    private fun startTimer() {
        val h = binding.npHour.value
        val m = binding.npMinute.value
        val s = binding.npSecond.value
        totalSeconds = h * 3600 + m * 60 + s

        if (totalSeconds == 0) {
            Toast.makeText(requireContext(), "Please set a time!", Toast.LENGTH_SHORT).show()
            return
        }

        // Reset pause state
        isPaused = false

        // Start service with timer configuration
        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_SECONDS, totalSeconds)
            selectedSoundUri?.let { putExtra(TimerService.EXTRA_SOUND_URI, it.toString()) }
            if (selectedResId != -1) putExtra(TimerService.EXTRA_SOUND_RES_ID, selectedResId)
        }
        ContextCompat.startForegroundService(requireContext(), intent)

        // Start syncing with service
        currentSeconds = totalSeconds
        switchToRunningState()
        startSyncTimer()

        Log.d("TimerFragment", "Timer started: $totalSeconds seconds")
    }

    private fun startSyncTimer() {
        stopSyncTimer()

        syncHandler = Handler(Looper.getMainLooper())
        syncRunnable = object : Runnable {
            override fun run() {
                // Read from Service's static variables
                if (TimerService.isServiceRunning) {
                    currentSeconds = TimerService.currentRemainingSeconds
                    isPaused = TimerService.isCurrentlyPaused

                    updateUI(currentSeconds, totalSeconds)

                    // Update button text based on pause state
                    binding.btnStop.text = if (isPaused) "Continue" else "Stop"

                    if (currentSeconds == 0 && !isPaused) {
                        // Timer completed
                        switchToTimesUpState()
                        stopSyncTimer()
                    } else {
                        // Continue syncing
                        syncHandler?.postDelayed(this, 100) // Check every 100ms for smooth UI
                    }
                } else {
                    // Service stopped
                    if (binding.layoutRunning.visibility == View.VISIBLE) {
                        switchToPickerState()
                    }
                    stopSyncTimer()
                }
            }
        }
        syncHandler?.post(syncRunnable!!)
    }

    private fun stopSyncTimer() {
        syncRunnable?.let {
            syncHandler?.removeCallbacks(it)
            syncRunnable = null
        }
        syncHandler = null
    }

    private fun updateUI(remaining: Int, total: Int) {
        val h = remaining / 3600
        val m = (remaining % 3600) / 60
        val s = remaining % 60

        binding.tvRH.text = "%02d".format(h)
        binding.tvRM.text = "%02d".format(m)
        binding.tvRS.text = "%02d".format(s)

        val progress = if (total > 0) remaining.toFloat() / total.toFloat() else 0f
        binding.progressRing.setProgress(progress)
    }

    private fun goBackToPicker() {
        // Stop service
        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        requireContext().startService(intent)

        // Stop syncing and reset state
        stopSyncTimer()
        isPaused = false

        // Reset UI
        switchToPickerState()
        clearKeepScreen()
    }

    private fun switchToRunningState() {
        binding.layoutPickers.visibility = View.GONE
        binding.layoutRunning.visibility = View.VISIBLE
        binding.layoutTimesUp.visibility = View.GONE

        // Reset button text
        binding.btnStop.text = if (isPaused) "Continue" else "Stop"
    }

    private fun switchToPickerState() {
        binding.layoutPickers.visibility = View.VISIBLE
        binding.layoutRunning.visibility = View.GONE
        binding.layoutTimesUp.visibility = View.GONE
        binding.progressRing.setProgress(1f)
        binding.tvTimesUpSubtitle.text = "00h 00m 00s"
    }

    private fun switchToTimesUpState() {
        binding.layoutPickers.visibility = View.GONE
        binding.layoutRunning.visibility = View.GONE
        binding.layoutTimesUp.visibility = View.VISIBLE

        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        binding.tvTimesUpSubtitle.text = "%02dh %02dm %02ds".format(h, m, s)

        clearKeepScreen()

        // Reset pause state
        isPaused = false
    }

    private fun clearKeepScreen() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding.switchKeepScreen.isChecked = false
    }

    private fun getFileNameFromUri(uri: Uri): String {
        return try {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else "Custom Sound"
                } else "Custom Sound"
            } ?: "Custom Sound"
        } catch (e: Exception) {
            "Custom Sound"
        }
    }

    override fun onResume() {
        super.onResume()
        // Check service state when fragment resumes
        if (TimerService.isServiceRunning && binding.layoutRunning.visibility != View.VISIBLE) {
            currentSeconds = TimerService.currentRemainingSeconds
            totalSeconds = TimerService.currentTotalSeconds
            isPaused = TimerService.isCurrentlyPaused

            if (currentSeconds > 0) {
                switchToRunningState()
                startSyncTimer()
            } else if (currentSeconds == 0) {
                switchToTimesUpState()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop syncing when app goes to background to save battery
        stopSyncTimer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopSyncTimer()
        _binding = null
    }
}