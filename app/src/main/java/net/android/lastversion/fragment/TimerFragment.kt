package net.android.lastversion.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.*
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
    private var selectedResId: Int? = null
    private var totalSeconds = 0
    private var isBound = false
    private var isPaused = false

    private val availableSounds = listOf(
        "Astro (Default)" to R.raw.astro,
        "Bell" to R.raw.bell,
        "Piano" to R.raw.piano,
        "Other (Choose from device)" to -1
    )

    // Nhận tick từ Service
    private val clientMessenger = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                TimerService.MSG_TICK -> {
                    val remaining = msg.arg1
                    val total = msg.arg2
                    updateUI(remaining, total)
                    if (remaining == 0) switchToTimesUpState()
                }
                TimerService.MSG_FINISHED -> switchToTimesUpState()
            }
        }
    })

    private var serviceMessenger: Messenger? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceMessenger = Messenger(service)
            // Đăng ký nhận tick
            serviceMessenger?.send(
                Message.obtain(null, TimerService.MSG_REGISTER_CLIENT).also { it.replyTo = clientMessenger }
            )
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            serviceMessenger = null
        }
    }

    // Chọn nhạc từ thiết bị
    private val soundPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                requireContext().contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedSoundUri = it
                selectedResId = null
                binding.tvSoundValue.text = getFileNameFromUri(it)
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupPickers()
        setupButtons()

        binding.layoutSound.setOnClickListener {
            val names = availableSounds.map { it.first }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Select Sound")
                .setItems(names) { _, which ->
                    val (name, resId) = availableSounds[which]
                    binding.tvSoundValue.text = name
                    if (resId == -1) {
                        soundPickerLauncher.launch(arrayOf("audio/*"))
                    } else {
                        selectedResId = resId
                        selectedSoundUri = null
                    }
                }
                .show()
        }

        binding.switchKeepScreen.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private fun setupPickers() = with(binding) {
        npHour.minValue = 0;   npHour.maxValue = 23
        npMinute.minValue = 0; npMinute.maxValue = 59
        npSecond.minValue = 0; npSecond.maxValue = 59
    }

    private fun setupButtons() {
        binding.btnStartTimer.setOnClickListener {
            val h = binding.npHour.value
            val m = binding.npMinute.value
            val s = binding.npSecond.value
            totalSeconds = h * 3600 + m * 60 + s
            if (totalSeconds == 0) {
                Toast.makeText(requireContext(), "Please set a time!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(requireContext(), TimerService::class.java).apply {
                action = TimerService.ACTION_START
                putExtra(TimerService.EXTRA_SECONDS, totalSeconds)
                selectedSoundUri?.let { putExtra(TimerService.EXTRA_SOUND_URI, it.toString()) }
                selectedResId?.let { putExtra(TimerService.EXTRA_SOUND_RES_ID, it) }
            }

            ContextCompat.startForegroundService(requireContext(), intent)
            requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

            switchToRunningState()
            updateUI(totalSeconds, totalSeconds)
            isPaused = false
        }

        binding.btnStop.setOnClickListener {
            val action = if (isPaused) TimerService.ACTION_CONTINUE else TimerService.ACTION_STOP
            ContextCompat.startForegroundService(
                requireContext(),
                Intent(requireContext(), TimerService::class.java).setAction(action)
            )
            binding.btnStop.text = if (isPaused) "Stop" else "Continue"
            isPaused = !isPaused
        }

        binding.btnRestart.setOnClickListener {
            requireContext().startService(
                Intent(requireContext(), TimerService::class.java).setAction(TimerService.ACTION_RESTART)
            )
            if (isBound) requireContext().unbindService(serviceConnection)
            switchToPickerState()
        }

        binding.btnStopTimesUp.setOnClickListener {
            requireContext().startService(
                Intent(requireContext(), TimerService::class.java).setAction(TimerService.ACTION_RESTART)
            )
            if (isBound) requireContext().unbindService(serviceConnection)
            switchToPickerState()
        }
    }

    private fun updateUI(remaining: Int, total: Int) {
        val h = remaining / 3600
        val m = (remaining % 3600) / 60
        val s = remaining % 60
        binding.tvRH.text = "%02d".format(h)
        binding.tvRM.text = "%02d".format(m)
        binding.tvRS.text = "%02d".format(s)

        val progress = if (total > 0) {
            1f - (total - remaining).toFloat() / total.toFloat()
        } else {
            1f
        }
        Log.d("TimerUI", "Progress: $progress ($remaining / $total)")
        binding.progressRing.setProgress(progress)
    }

    private fun getFileNameFromUri(uri: Uri): String {
        val c = requireContext().contentResolver.query(uri, null, null, null, null)
        c?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return it.getString(idx)
            }
        }
        return "Unknown"
    }

    private fun switchToRunningState() {
        binding.layoutPickers.visibility = View.GONE
        binding.layoutRunning.visibility = View.VISIBLE
        binding.layoutTimesUp.visibility = View.GONE
    }

    private fun switchToPickerState() {
        binding.layoutPickers.visibility = View.VISIBLE
        binding.layoutRunning.visibility = View.GONE
        binding.layoutTimesUp.visibility = View.GONE
        binding.progressRing.setProgress(1f)
        binding.btnStop.text = "Stop"
        isPaused = false
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
    }

    override fun onDestroyView() {
        if (isBound) {
            try {
                serviceMessenger?.send(
                    Message.obtain(null, TimerService.MSG_UNREGISTER_CLIENT).also { it.replyTo = clientMessenger }
                )
            } catch (_: Exception) {}
            requireContext().unbindService(serviceConnection)
            isBound = false
        }
        _binding = null
        super.onDestroyView()
    }
}
