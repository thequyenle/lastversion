package net.android.lastversion.timer

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import net.android.lastversion.databinding.FragmentTimerBinding
import net.android.last.service.TimerService
import net.android.lastversion.R
import net.android.lastversion.utils.ThemeManager
import net.android.lastversion.utils.ThemeType
import net.android.lastversion.utils.showSystemUI
import net.android.lastversion.utils.showWithHiddenNavigation

class TimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private val tealColor = android.graphics.Color.parseColor("#84DCC6")
    private var selectedSoundUri: Uri? = null
    private var selectedResId: Int = R.raw.astro
    private var totalSeconds = 0
    private var currentSeconds = 0
    private var isPaused = false
    private lateinit var npHour: com.shawnlin.numberpicker.NumberPicker
    private lateinit var npMinute: com.shawnlin.numberpicker.NumberPicker
    private lateinit var npSecond: com.shawnlin.numberpicker.NumberPicker
    private lateinit var imgTimerBackground: ImageView

    private var syncHandler: Handler? = null
    private var syncRunnable: Runnable? = null

    private var isKeepScreenOn = false

    companion object {
        private const val PREFS_NAME = "timer_prefs"
        private const val KEY_SELECTED_SOUND_URI = "selected_sound_uri"
        private const val KEY_SELECTED_RES_ID = "selected_res_id"
        private const val KEY_TIMER_COMPLETED = "timer_completed"
        private const val KEY_COMPLETED_TOTAL_SECONDS = "completed_total_seconds"
        private const val KEY_COMPLETION_TIME = "completion_time"
    }

    private val availableSounds = listOf(
        "Astro" to R.raw.astro,
        "Bell" to R.raw.bell,
        "Piano" to R.raw.piano,
        "Custom (From device)" to -1
    )

    private val soundPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                requireContext().contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                selectedSoundUri = it
                selectedResId = -1
                updateSoundDisplay()
                saveSoundPreferences()
            } catch (e: Exception) {
                Log.e("TimerFragment", "Error selecting sound", e)
                Toast.makeText(requireContext(), "Error selecting sound", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgTimerBackground = binding.imgTimerBackground

        setupUI()
        npHour = view.findViewById(R.id.npHour)
        npMinute = view.findViewById(R.id.npMinute)
        npSecond = view.findViewById(R.id.npSecond)

        restoreSoundPreferences()
        checkCompletionState()

        if (!binding.layoutTimesUp.isShown && TimerService.isServiceRunning) {
            currentSeconds = TimerService.currentRemainingSeconds
            totalSeconds = TimerService.currentTotalSeconds
            isPaused = TimerService.isCurrentlyPaused

            if (currentSeconds > 0) {
                switchToRunningState()
                startSyncTimer()
            } else {
                switchToTimesUpState()
            }
        }
    }

    private fun setupUI() {
        setupPickers()
        setupSoundPicker()
        setupKeepScreen()
        setupButtons()
    }

    private fun setupPickers() = with(binding) {
        npHour.minValue = 0
        npHour.maxValue = 99
        npHour.value = 0

        npMinute.minValue = 0
        npMinute.maxValue = 59
        npMinute.value = 0

        npSecond.minValue = 0
        npSecond.maxValue = 59
        npSecond.value = 0

        npHour.displayedValues = (0..99).map { String.format("%02d", it) }.toTypedArray()
        npMinute.displayedValues = (0..59).map { String.format("%02d", it) }.toTypedArray()
        npSecond.displayedValues = (0..59).map { String.format("%02d", it) }.toTypedArray()

        npHour.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        npMinute.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        npSecond.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
    }

    private fun setupSoundPicker() {
        binding.layoutSound.setOnClickListener {
            showSoundPickerDialog()
        }
    }

    private fun showSoundPickerDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sound_picker, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.rgSounds)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var tempSelectedPosition = availableSounds.indexOfFirst {
            it.second == selectedResId || (it.second == -1 && selectedSoundUri != null)
        }.let { if (it == -1) 0 else it }

        val colorStateList = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(tealColor, android.graphics.Color.parseColor("#808080"))
        )

        availableSounds.forEachIndexed { index, (soundName, resId) ->
            val radioButton = RadioButton(requireContext()).apply {
                text = soundName
                id = View.generateViewId()
                textSize = 16f
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    buttonTintList = colorStateList
                }

                setPadding(16, 24, 16, 24)
                isChecked = (index == tempSelectedPosition)

                setOnClickListener {
                    tempSelectedPosition = index
                }
            }
            radioGroup.addView(radioButton)
        }

        dialogView.findViewById<TextView>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.btnOk).setOnClickListener {
            val (name, resId) = availableSounds[tempSelectedPosition]

            if (resId == -1) {
                dialog.dismiss()
                soundPickerLauncher.launch(arrayOf("audio/*"))
            } else {
                selectedResId = resId
                selectedSoundUri = null
                updateSoundDisplay()
                saveSoundPreferences()
                dialog.dismiss()
            }
        }

        dialog.showWithHiddenNavigation()
    }

    private fun setupKeepScreen() {
        binding.switchKeepScreen.setOnClickListener {
            isKeepScreenOn = !isKeepScreenOn
            updateKeepScreenUI()
        }
    }

    private fun updateKeepScreenUI() {
        if (isKeepScreenOn) {
            binding.switchKeepScreen.setImageResource(R.drawable.ic_switch_on)
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            binding.switchKeepScreen.setImageResource(R.drawable.ic_switch_off)
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun setupButtons() {
        binding.btnStartTimer.setOnClickListener { startTimer() }
        binding.btnRestart.setOnClickListener { goBackToPicker() }
        binding.btnStopTimesUp.setOnClickListener { goBackToPicker() }

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
        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        requireContext().startService(intent)
        binding.btnStop.text = "Continue"
    }

    private fun resumeTimer() {
        isPaused = false
        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME
        }
        requireContext().startService(intent)
        binding.btnStop.text = "Stop"
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

        isPaused = false

        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_SECONDS, totalSeconds)
            selectedSoundUri?.let { putExtra(TimerService.EXTRA_SOUND_URI, it.toString()) }
            if (selectedResId != -1) putExtra(TimerService.EXTRA_SOUND_RES_ID, selectedResId)
        }
        ContextCompat.startForegroundService(requireContext(), intent)

        currentSeconds = totalSeconds
        switchToRunningState()
        startSyncTimer()
    }

    private fun startSyncTimer() {
        stopSyncTimer()

        syncHandler = Handler(Looper.getMainLooper())
        syncRunnable = object : Runnable {
            override fun run() {
                if (TimerService.isServiceRunning) {
                    currentSeconds = TimerService.currentRemainingSeconds
                    isPaused = TimerService.isCurrentlyPaused

                    updateUI(currentSeconds, totalSeconds)
                    binding.btnStop.text = if (isPaused) "Continue" else "Stop"

                    if (currentSeconds == 0 && !isPaused) {
                        switchToTimesUpState()
                        stopSyncTimer()
                    } else {
                        syncHandler?.postDelayed(this, 100)
                    }
                } else {
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
        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        requireContext().startService(intent)

        TimerService.reset()
        clearCompletionState()

        stopSyncTimer()
        isPaused = false
        switchToPickerState()
        clearKeepScreen()
    }

    private fun switchToRunningState() {
        binding.layoutPickers.visibility = View.GONE
        binding.layoutRunning.visibility = View.VISIBLE
        binding.layoutTimesUp.visibility = View.GONE
        binding.tvTitle.visibility = View.VISIBLE
        binding.btnStop.text = if (isPaused) "Continue" else "Stop"

        imgTimerBackground.visibility = View.GONE
    }

    private fun switchToPickerState() {
        binding.layoutPickers.visibility = View.VISIBLE
        binding.layoutRunning.visibility = View.GONE
        binding.layoutTimesUp.visibility = View.GONE
        binding.tvTitle.visibility = View.VISIBLE
        binding.progressRing.setProgress(1f)
        binding.tvTimesUpSubtitle.text = "00h 00m 00s"

        imgTimerBackground.visibility = View.GONE
        showBottomNavigation()
    }

    private fun showBottomNavigation() {
        val bottomNav = activity?.findViewById<View>(R.id.custom_bottom_navigation)
        bottomNav?.visibility = View.VISIBLE
    }

    private fun switchToTimesUpState() {
        binding.layoutPickers.visibility = View.GONE
        binding.layoutRunning.visibility = View.GONE
        binding.layoutTimesUp.visibility = View.VISIBLE
        binding.tvTitle.visibility = View.GONE
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        binding.tvTimesUpSubtitle.text = "%02dh %02dm %02ds".format(h, m, s)

        clearKeepScreen()
        isPaused = false

        imgTimerBackground.visibility = View.VISIBLE
        setBackgroundTheme()
        hideBottomNavigation()
    }

    private fun hideBottomNavigation() {
        val bottomNav = activity?.findViewById<View>(R.id.custom_bottom_navigation)
        bottomNav?.visibility = View.GONE
    }

    private fun setBackgroundTheme() {
        val themeManager = ThemeManager(requireContext())
        val theme = themeManager.getCurrentTheme()

        theme?.let {
            when (it.type) {
                ThemeType.PRESET -> {
                    imgTimerBackground.setImageResource(it.drawableRes)
                }
                ThemeType.CUSTOM -> {
                    val file = themeManager.getCurrentThemeFile()
                    file?.let { themeFile ->
                        val bitmap = BitmapFactory.decodeFile(themeFile.absolutePath)
                        imgTimerBackground.setImageBitmap(bitmap)
                    }
                }
                ThemeType.ADD_NEW -> {
                }
            }
        }
    }

    private fun clearKeepScreen() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        isKeepScreenOn = false
        updateKeepScreenUI()
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

    private fun saveSoundPreferences() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_SELECTED_SOUND_URI, selectedSoundUri?.toString() ?: "")
            putInt(KEY_SELECTED_RES_ID, selectedResId)
            apply()
        }
    }

    private fun restoreSoundPreferences() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

        val uriString = prefs.getString(KEY_SELECTED_SOUND_URI, "")
        selectedSoundUri = if (uriString.isNullOrEmpty()) null else Uri.parse(uriString)

        selectedResId = prefs.getInt(KEY_SELECTED_RES_ID, R.raw.astro)

        updateSoundDisplay()
    }

    private fun updateSoundDisplay() {
        binding.tvSoundValue.text = when {
            selectedSoundUri != null -> getFileNameFromUri(selectedSoundUri!!)
            selectedResId == R.raw.astro -> "Astro"
            selectedResId == R.raw.bell -> "Bell"
            selectedResId == R.raw.piano -> "Piano"
            else -> "Astro"
        }
    }

    private fun checkCompletionState() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val isCompleted = prefs.getBoolean(KEY_TIMER_COMPLETED, false)

        if (isCompleted) {
            totalSeconds = prefs.getInt(KEY_COMPLETED_TOTAL_SECONDS, 0)
            currentSeconds = 0

            Log.d("TimerFragment", "Restoring Time's Up state")
            switchToTimesUpState()
        }
    }

    private fun clearCompletionState() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            remove(KEY_TIMER_COMPLETED)
            remove(KEY_COMPLETED_TOTAL_SECONDS)
            remove(KEY_COMPLETION_TIME)
            apply()
        }
        Log.d("TimerFragment", "Completion state cleared")
    }

    override fun onResume() {
        super.onResume()
        activity?.showSystemUI(white = false)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val isCompleted = prefs.getBoolean(KEY_TIMER_COMPLETED, false)

        if (isCompleted) {
            totalSeconds = prefs.getInt(KEY_COMPLETED_TOTAL_SECONDS, 0)
            currentSeconds = 0
            switchToTimesUpState()
            return
        }

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
        stopSyncTimer()
        saveSoundPreferences()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopSyncTimer()
        _binding = null
    }
}