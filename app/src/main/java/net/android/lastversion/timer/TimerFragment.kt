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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import net.android.lastversion.databinding.FragmentTimerBinding
import net.android.last.service.TimerService
import net.android.lastversion.R
import net.android.lastversion.utils.showSystemUI

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

    companion object {
        private const val PREFS_NAME = "timer_prefs"
        private const val KEY_SOUND_URI = "sound_uri"
        private const val KEY_SOUND_RES_ID = "sound_res_id"
    }

    private val soundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            requireContext().contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            selectedSoundUri = it
            Toast.makeText(requireContext(), "Custom sound selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        npHour = binding.npHour
        npMinute = binding.npMinute
        npSecond = binding.npSecond
        imgTimerBackground = binding.imgTimerBackground

        setupNumberPickers()
        setupButtons()
        setupSoundOptions()
        loadSoundPreferences()
    }

    private fun setupNumberPickers() {
        npHour.minValue = 0
        npHour.maxValue = 23
        npHour.value = 0

        npMinute.minValue = 0
        npMinute.maxValue = 59
        npMinute.value = 0

        npSecond.minValue = 0
        npSecond.maxValue = 59
        npSecond.value = 5

        npHour.setSelectedTextColor(tealColor)
        npMinute.setSelectedTextColor(tealColor)
        npSecond.setSelectedTextColor(tealColor)
    }

    private fun setupSoundOptions() {
        val radioGroup = binding.radioGroupSound
        val btnCustomSound = binding.btnCustomSound

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedResId = when (checkedId) {
                R.id.radioSoundAstro -> R.raw.astro
                R.id.radioSoundRain -> R.raw.rainy
                R.id.radioSoundBird -> R.raw.bird
                R.id.radioSoundBird2 -> R.raw.bird2
                R.id.radioSoundSmooth -> R.raw.smooth
                else -> R.raw.astro
            }
            selectedSoundUri = null
        }

        btnCustomSound.setOnClickListener {
            soundPickerLauncher.launch(arrayOf("audio/*"))
        }
    }

    private fun saveSoundPreferences() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_SOUND_URI, selectedSoundUri?.toString())
            putInt(KEY_SOUND_RES_ID, selectedResId)
            apply()
        }
    }

    private fun loadSoundPreferences() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val uriString = prefs.getString(KEY_SOUND_URI, null)
        selectedSoundUri = if (uriString != null) Uri.parse(uriString) else null
        selectedResId = prefs.getInt(KEY_SOUND_RES_ID, R.raw.astro)

        when (selectedResId) {
            R.raw.astro -> binding.radioSoundAstro.isChecked = true
            R.raw.bell -> binding.radioSoundRain.isChecked = true
            R.raw.piano -> binding.radioSoundBird.isChecked = true
                    }
    }

    private fun keepScreenOn() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding.imgSwitch.setImageResource(R.drawable.ic_switch_on)
    }

    private fun clearKeepScreen() {
        if (binding.imgSwitch.drawable.constantState ==
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_switch_on)?.constantState) {
            binding.imgSwitch.setImageResource(R.drawable.ic_switch_off)
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun setupButtons() {
        binding.btnStartTimer.setOnClickListener { startTimer() }
        binding.btnRestart.setOnClickListener { goBackToPicker() }

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

        currentSeconds = totalSeconds

        val intent = Intent(requireContext(), TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_SECONDS, totalSeconds)
            putExtra(TimerService.EXTRA_SOUND_URI, selectedSoundUri?.toString())
            putExtra(TimerService.EXTRA_SOUND_RES_ID, selectedResId)
        }

        requireContext().startService(intent)

        isPaused = false
        switchToRunningState()
        startSyncTimer()
        keepScreenOn()
        hideBottomNavigation()
    }

    private fun hideBottomNavigation() {
        val bottomNav = activity?.findViewById<View>(R.id.custom_bottom_navigation)
        bottomNav?.visibility = View.GONE
    }

    private fun startSyncTimer() {
        stopSyncTimer()

        syncHandler = Handler(Looper.getMainLooper())
        syncRunnable = object : Runnable {
            override fun run() {
                if (TimerService.isServiceRunning) {
                    val remaining = TimerService.currentRemainingSeconds
                    val total = TimerService.currentTotalSeconds

                    updateUI(remaining, total)

                    if (remaining <= 0) {
                        stopSyncTimer()
                        return
                    }

                    syncHandler?.postDelayed(this, 250)
                } else {
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

        stopSyncTimer()
        isPaused = false
        switchToPickerState()
        clearKeepScreen()
    }

    override fun onStart() {
        super.onStart()
        Log.d("TimerFragment", "🟢 onStart() called")
    }

    private fun switchToRunningState() {
        binding.layoutPickers.visibility = View.GONE
        binding.layoutRunning.visibility = View.VISIBLE
        binding.tvTitle.visibility = View.VISIBLE
        binding.btnStop.text = if (isPaused) "Continue" else "Stop"

        imgTimerBackground.visibility = View.GONE
    }

    private fun switchToPickerState() {
        binding.layoutPickers.visibility = View.VISIBLE
        binding.layoutRunning.visibility = View.GONE
        binding.tvTitle.visibility = View.VISIBLE
        binding.progressRing.setProgress(1f)

        imgTimerBackground.visibility = View.GONE
        showBottomNavigation()
    }

    private fun showBottomNavigation() {
        val bottomNav = activity?.findViewById<View>(R.id.custom_bottom_navigation)
        bottomNav?.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        activity?.showSystemUI(white = false)

        Log.d("TimerFragment", "🟢 onResume() called")

        if (TimerService.isServiceRunning && binding.layoutRunning.visibility != View.VISIBLE) {
            currentSeconds = TimerService.currentRemainingSeconds
            totalSeconds = TimerService.currentTotalSeconds
            isPaused = TimerService.isCurrentlyPaused

            if (currentSeconds > 0) {
                switchToRunningState()
                startSyncTimer()
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