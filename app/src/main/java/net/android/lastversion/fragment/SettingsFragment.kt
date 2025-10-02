package net.android.lastversion.fragment

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import net.android.lastversion.LanguageActivity
import net.android.lastversion.R
import net.android.lastversion.dialog.RatingDialog
import net.android.lastversion.utils.showSystemUI

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SettingsFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var audioManager: AudioManager
    private lateinit var volumeSeekBar: SeekBar

    companion object {
        private const val PREFS_NAME = "AlarmSettings"
        private const val KEY_VOLUME = "volume"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString("param1", param1)
                    putString("param2", param2)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        // Khởi tạo AudioManager
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupVolumeControl(view)
        setupLanguageClick(view)
        setupRateUsClick(view)

        activity?.showSystemUI(white = false)
    }

    private fun setupRateUsClick(view: View) {
        val layoutRateUs = view.findViewById<ConstraintLayout>(R.id.layoutRateUs)
        layoutRateUs.setOnClickListener {
            showRatingDialog()
        }
    }

    private fun showRatingDialog() {
        val dialog = RatingDialog(requireContext())
        dialog.show()
    }

    private fun setupLanguageClick(view: View) {
        val layoutLanguage = view.findViewById<ConstraintLayout>(R.id.layoutLanguage)
        layoutLanguage.setOnClickListener {
            val intent = Intent(requireContext(), LanguageActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupVolumeControl(view: View) {
        volumeSeekBar = view.findViewById(R.id.seekBarVolume)

        // Lấy giá trị volume đã lưu và set cho seekbar
        val savedVolume = getSavedVolume()
        volumeSeekBar.progress = savedVolume
        setSystemVolume(savedVolume)

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setSystemVolume(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Không cần xử lý
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    saveVolume(it.progress)
                }
            }
        })
    }

    private fun setSystemVolume(volume: Int) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val scaledVolume = (volume * maxVolume) / 100
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, scaledVolume, 0)
    }

    private fun saveVolume(volume: Int) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_VOLUME, volume).apply()
    }

    private fun getSavedVolume(): Int {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_VOLUME, 50)
    }

    override fun onResume() {
        super.onResume()
        activity?.showSystemUI(white = false)
    }
}