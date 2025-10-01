package net.android.lastversion.fragment

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import net.android.lastversion.R
import net.android.lastversion.utils.showSystemUI
import net.android.lastversion.utils.InAppReviewHelper

class SettingsFragment : Fragment() {

    private lateinit var seekBarVolume: SeekBar
    private lateinit var audioManager: AudioManager
    private lateinit var layoutRateUs: ConstraintLayout

    private val PREFS_NAME = "settings_preferences"
    private val KEY_VOLUME = "alarm_volume"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Khởi tạo AudioManager
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Đặt ở đây - timing chính xác
        activity?.showSystemUI(white = false)

        // Khởi tạo các components
        setupVolumeSeekBar(view)
        setupRateUs(view)
    }

    /**
     * Thiết lập SeekBar cho điều chỉnh âm lượng
     */
    private fun setupVolumeSeekBar(view: View) {
        seekBarVolume = view.findViewById(R.id.seekBarVolume)

        // Lấy volume đã lưu hoặc dùng giá trị mặc định (50%)
        val savedVolume = getSavedVolume()
        seekBarVolume.progress = savedVolume

        // Thiết lập volume hiện tại cho hệ thống
        setSystemVolume(savedVolume)

        // Lắng nghe thay đổi của SeekBar
        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Cập nhật volume của hệ thống khi user kéo seekbar
                    setSystemVolume(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Không cần xử lý
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Lưu giá trị khi user thả tay khỏi seekbar
                seekBar?.let {
                    saveVolume(it.progress)
                }
            }
        })
    }

    /**
     * Thiết lập Rate Us click listener
     */
    private fun setupRateUs(view: View) {
        layoutRateUs = view.findViewById(R.id.layoutRateUs)

        layoutRateUs.setOnClickListener {
            // Hiển thị In-App Review
            activity?.let { act ->
                InAppReviewHelper.showInAppReview(act) { success ->
                    if (success) {
                        // Review flow thành công, lưu lại đã hiển thị
                        InAppReviewHelper.markReviewShown(act)
                    }
                }
            }
        }
    }

    /**
     * Thiết lập âm lượng cho hệ thống
     * @param volume: giá trị từ 0-100
     */
    private fun setSystemVolume(volume: Int) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        // Chuyển đổi từ 0-100 sang giá trị volume thực của hệ thống
        val scaledVolume = (volume * maxVolume) / 100
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, scaledVolume, 0)
    }

    /**
     * Lưu giá trị volume vào SharedPreferences
     * @param volume: giá trị từ 0-100
     */
    private fun saveVolume(volume: Int) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_VOLUME, volume).apply()
    }

    /**
     * Đọc giá trị volume đã lưu từ SharedPreferences
     * @return giá trị volume từ 0-100, mặc định là 50
     */
    private fun getSavedVolume(): Int {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_VOLUME, 50) // Mặc định là 50%
    }

    override fun onResume() {
        super.onResume()
        activity?.showSystemUI(white = false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SettingsFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString("param1", param1)
                    putString("param2", param2)
                }
            }
    }
}