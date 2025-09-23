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
import net.android.lastversion.view.CircleCountdownView

// Âm thanh + cấu hình âm thanh
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.AudioAttributes

// Rung (hỗ trợ Android 12+ và các phiên bản cũ hơn)
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.VibratorManager
import android.os.Build

// Lấy system service cho Vibrator ở các phiên bản
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import net.android.lastversion.service.TimerService


class TimerFragment : Fragment() {

    // ============= State 1: Chọn thời gian =============
    private lateinit var layoutPickers: LinearLayout
    private lateinit var npHour: NumberPicker
    private lateinit var npMinute: NumberPicker
    private lateinit var npSecond: NumberPicker
    private lateinit var btnStart: Button
    private lateinit var switchKeepScreen: Switch

    // Giữ instance nhạc chuông để có thể dừng khi cần
    private var ringtone: Ringtone? = null

    // Giữ instance Vibrator để có thể cancel khi cần
    private var vibrator: Vibrator? = null

    // ============= State 2: Đang chạy =============
    private lateinit var layoutRunning: LinearLayout
    private lateinit var ring: CircleCountdownView          // vòng tròn custom
    private lateinit var tvRH: TextView                     // Hours
    private lateinit var tvRM: TextView                     // Minutes
    private lateinit var tvRS: TextView                     // Seconds
    private lateinit var btnStop: Button
    private lateinit var btnRestart: Button

    // ============= State 3: Hết giờ =============
    private lateinit var layoutTimesUp: LinearLayout
    private lateinit var tvTimesUpSubtitle: TextView
    private lateinit var btnStopTimesUp: Button

    // Biến điều khiển
    private var initialMillis: Long = 0L                    // tổng thời gian ban đầu
    private var timer: CountDownTimer? = null               // CountDownTimer hiện tại

    // Bắt đầu Foreground service với tổng millis
    private fun startBgService(durationMs: Long) {
        val ctx = requireContext()
        val intent = Intent(ctx, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_DURATION_MS, durationMs)
        }
        ContextCompat.startForegroundService(ctx, intent)
    }

    // Gửi lệnh dừng service
    private fun stopBgService() {
        val ctx = requireContext()
        val intent = Intent(ctx, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        ctx.startService(intent)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_timer, container, false)

        // ---- Bind view theo đúng id trong XML ----
        layoutPickers = v.findViewById(R.id.layoutPickers)
        npHour = v.findViewById(R.id.npHour)
        npMinute = v.findViewById(R.id.npMinute)
        npSecond = v.findViewById(R.id.npSecond)
        btnStart = v.findViewById(R.id.btnStartTimer)
        switchKeepScreen = v.findViewById(R.id.switchKeepScreen)

        layoutRunning = v.findViewById(R.id.layoutRunning)
        ring = v.findViewById(R.id.progressRing)
        tvRH = v.findViewById(R.id.tvRH)
        tvRM = v.findViewById(R.id.tvRM)
        tvRS = v.findViewById(R.id.tvRS)
        btnStop = v.findViewById(R.id.btnStop)
        btnRestart = v.findViewById(R.id.btnRestart)

        layoutTimesUp = v.findViewById(R.id.layoutTimesUp)
        tvTimesUpSubtitle = v.findViewById(R.id.tvTimesUpSubtitle)
        btnStopTimesUp = v.findViewById(R.id.btnStopTimesUp)

        setupPickers()


        // ---- Clicks ----
        btnStart.setOnClickListener { startFromPickers() }      // Bắt đầu từ giá trị đã chọn
        btnStop.setOnClickListener {
            stopTimer(toPickers = true) // dừng UI
            stopBgService()  // ✅ dừng nền }
        }

        btnRestart.setOnClickListener {
            restartTimer() // reset UI
            stopBgService()
            startBgService(initialMillis) // ✅ đếm lại nền
        }
        btnStopTimesUp.setOnClickListener {
            showPickers()
            clearKeepScreen()
            stopBgService() // ✅ dừng nền
        }

        showPickers()
        return v
    }





    /** Thiết lập NumberPicker: min/max, format 2 chữ số, khóa bàn phím */
    /** Cấu hình 3 NumberPicker (giờ/phút/giây) */
    private fun setupPickers() {
        // 1) Giới hạn giá trị hợp lệ
        npHour.minValue = 0          // tối thiểu 0 giờ
        npHour.maxValue = 99         // tối đa 99 giờ (tùy app, bạn có thể đổi)
        npMinute.minValue = 0        // 0 phút
        npMinute.maxValue = 59       // 59 phút
        npSecond.minValue = 0        // 0 giây
        npSecond.maxValue = 59       // 59 giây

        // 2) Định dạng hiển thị 2 chữ số (ví dụ 0 -> "00", 5 -> "05")
        val twoDigits = NumberPicker.Formatter { value ->
            String.format("%02d", value)
        }
        npHour.setFormatter(twoDigits)
        npMinute.setFormatter(twoDigits)
        npSecond.setFormatter(twoDigits)

        // 3) Chặn focus vào EditText bên trong NumberPicker
        //    -> tránh mở bàn phím mềm, chỉ cho phép cuộn bằng bánh xe
        npHour.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        npMinute.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        npSecond.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS

        // (tuỳ chọn) nếu muốn cho NumberPicker quay vòng (59 -> 0, 0 -> 59)
        // npMinute.wrapSelectorWheel = true
        // npSecond.wrapSelectorWheel = true
        // npHour.wrapSelectorWheel = true // nếu bạn muốn cả giờ cũng quay vòng
    }


    /** Start khi nhấn nút Start ở màn pickers */
    private fun startFromPickers() {
        // 1) Quy đổi giờ/phút/giây sang tổng mili-giây.
        //   * 3600 = số giây trong 1 giờ; 60 = số giây trong 1 phút
        //   * nhân 1000L (hậu tố L) để ép kiểu sang Long, an toàn khi nhân lớn.
        val totalMillis =
            (npHour.value * 3600 + npMinute.value * 60 + npSecond.value) * 1000L

        // 2) Nếu người dùng chưa đặt thời gian (> 0) thì báo và thoát hàm.
        if (totalMillis <= 0) {
            Toast.makeText(requireContext(), "Please set time", Toast.LENGTH_SHORT).show()
            return
        }

        // 3) Lưu lại tổng thời gian ban đầu:
        //    - dùng cho nút Restart (đếm lại từ đầu)
        //    - dùng để tính % còn lại của vòng tròn: msLeft / initialMillis
        initialMillis = totalMillis

        // 4) Nếu bật công tắc "Keep my screen on", đặt cờ để màn hình không tắt
        //    trong lúc timer đang chạy (bỏ cờ ở stop/finish).
        if (switchKeepScreen.isChecked) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // 5) Tạo và khởi động CountDownTimer với tổng thời gian đã tính,
        //    đồng thời chuyển UI sang trạng thái Running.
        startTimerInternal(initialMillis)
        startBgService(initialMillis)     //  chạy nền
        startTimerInternal(initialMillis) // (tuỳ bạn) vẫn chạy UI trong fragment
    }

    /** Restart từ đúng initialMillis (không quay lại pickers) */
    private fun restartTimer() {
        // Đảm bảo vẫn giữ màn hình sáng nếu trước đó đã bật
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        startTimerInternal(initialMillis)
    }


    /**
     * Khởi động CountDownTimer với tổng thời gian [total] (ms)
     * - Chuyển UI sang trạng thái Running
     * - Hiển thị số đếm ban đầu và vòng tròn đầy 100%
     * - Cứ mỗi 1s: cập nhật HH:MM:SS và % vòng tròn còn lại
     * - Khi hết giờ: chuyển sang Time’s up, phát âm + rung, bỏ cờ giữ màn hình sáng
     */
    private fun startTimerInternal(total: Long) {
        // 1) Chuyển UI sang màn "đang chạy"
        showRunning()

        // 2) Hiển thị số ban đầu (từ tổng mili-giây) ngay khi vừa start
        setRunningTime(total)

        // 3) Vòng tròn đầy 100% lúc bắt đầu (progress = 1.0)
        ring.setProgress(1f)

        // 4) Nếu đang có timer cũ -> hủy để tránh chạy song song
        timer?.cancel()

        // 5) Tạo timer mới, tick mỗi 1000ms (1 giây)
        timer = object : CountDownTimer(total, 1000) {

            // Được gọi mỗi 1s với số mili-giây còn lại [msLeft]
            override fun onTick(msLeft: Long) {
                // 5.1) Cập nhật 3 TextView: giờ / phút / giây
                setRunningTime(msLeft)

                // 5.2) Cập nhật vòng tròn theo tỉ lệ còn lại (0..1)
                //      msLeft / initialMillis: phần trăm thời gian vẫn còn
                ring.setProgress(msLeft.toFloat() / initialMillis)
                //  (initialMillis đã được set > 0 ở startFromPickers)
            }

            // Gọi 1 lần khi đếm xong
            override fun onFinish() {
                // 6.1) Vòng tròn về 0% (hết)
                ring.setProgress(0f)

                // 6.2) Chuyển UI sang màn "Time’s up"
                showTimesUp()

                // 6.3) Hiển thị thời lượng đã hẹn (ví dụ: "5m 0s")
                tvTimesUpSubtitle.text = formatMillis(initialMillis)

                // 6.4) Phát âm báo + rung máy để nhắc người dùng
                playBeep()   // 🔊
                buzz()       // 📳

                // 6.5) Bỏ cờ giữ màn hình sáng (nếu trước đó đã bật)
                clearKeepScreen()
            }
        }.start() // 7) Bắt đầu chạy timer
    }

    /** Cập nhật 3 TextView: giờ/phút/giây trong màn chạy */
    private fun setRunningTime(msLeft: Long) {
        val h = (msLeft / 1000) / 3600
        val m = ((msLeft / 1000) % 3600) / 60
        val s = (msLeft / 1000) % 60
        tvRH.text = String.format("%02d", h)
        tvRM.text = String.format("%02d", m)
        tvRS.text = String.format("%02d", s)
    }

    /** Dừng Timer (toPickers=true thì quay lại màn chọn thời gian) */
    private fun stopTimer(toPickers: Boolean) {
        timer?.cancel()
        stopAlerts()
        if (toPickers) showPickers() else showRunning()
        clearKeepScreen()
    }

    // --------- Helpers: đổi giữa 3 state UI ----------
    private fun showPickers() {
        layoutPickers.visibility = View.VISIBLE
        layoutRunning.visibility = View.GONE
        layoutTimesUp.visibility = View.GONE
    }

    private fun showRunning() {
        layoutPickers.visibility = View.GONE
        layoutRunning.visibility = View.VISIBLE
        layoutTimesUp.visibility = View.GONE
    }

    private fun showTimesUp() {
        layoutPickers.visibility = View.GONE
        layoutRunning.visibility = View.GONE
        layoutTimesUp.visibility = View.VISIBLE
    }

    private fun clearKeepScreen() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /** Chuỗi mô tả thời lượng đã đặt: "1h 2m 5s" */
    private fun formatMillis(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return buildString {
            if (h > 0) append("${h}h ")
            if (m > 0) append("${m}m ")
            append("${s}s")
        }.trim()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        stopAlerts()
        clearKeepScreen()
    }
    /** 🔊 Phát âm thanh thông báo khi hết giờ */
    private fun playBeep() {
        // Lấy 3 loại âm mặc định của hệ thống: Alarm → Notification → Ringtone
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)          // âm báo thức
        val notiUri  = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)   // âm thông báo
        val ringUri  = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)       // nhạc chuông gọi đến

        // Chọn URI đầu tiên không null theo thứ tự ưu tiên ở trên
        val chosenUri = alarmUri ?: notiUri ?: ringUri

        // Tạo đối tượng Ringtone từ URI đã chọn
        ringtone = RingtoneManager.getRingtone(requireContext(), chosenUri)?.apply {
            // Với Android 5.0+ thì đặt AudioAttributes để hệ thống hiểu đây là âm báo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)                  // mục đích: âm báo/thức
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION) // loại nội dung: âm hệ thống
                    .build()
            }
            // Bắt đầu phát âm
            try { play() } catch (_: Throwable) { /* tránh crash nếu thiết bị lỗi âm thanh */ }
        }
    }

    /** 📳 Rung máy theo pattern ngắn để nhắc người dùng */
    private fun buzz() {
        // Lấy Vibrator theo phiên bản Android
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ dùng VibratorManager để lấy vibrator mặc định
            val vm = requireContext().getSystemService(VibratorManager::class.java)
            vm.defaultVibrator
        } else {
            // Các phiên bản cũ hơn dùng dịch vụ VIBRATOR trực tiếp
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Mảng thời gian: delay, rung, nghỉ, rung, nghỉ, rung (đơn vị: ms)
        val pattern = longArrayOf(0, 500, 300, 500, 300, 500)

        // Tạo hiệu ứng rung tuỳ phiên bản Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+: dùng VibrationEffect (chuẩn mới)
            val effect = VibrationEffect.createWaveform(pattern, -1) // -1: không lặp
            vibrator?.vibrate(effect)
        } else {
            // Cũ hơn: dùng API deprecated (vẫn chạy được)
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1) // -1: không lặp
        }
    }

    /**  Dừng mọi âm thanh/rung đang chạy (gọi khi Stop/thoát màn) */
    private fun stopAlerts() {
        // Dừng nhạc chuông nếu đang phát
        try { ringtone?.stop() } catch (_: Throwable) { /* phòng lỗi thiết bị */ }
        // Huỷ rung nếu đang rung
        try { vibrator?.cancel() } catch (_: Throwable) { /* phòng lỗi thiết bị */ }
    }



}
