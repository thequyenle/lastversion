package net.android.lastversion.alarm.presentation.activity

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import net.android.lastversion.R
import net.android.lastversion.alarm.infrastructure.notification.AlarmNotificationManager
import net.android.lastversion.alarm.infrastructure.receiver.AlarmActionReceiver
import net.android.lastversion.utils.ThemeManager
import net.android.lastversion.utils.ThemeType
import java.text.SimpleDateFormat
import java.util.*

class AlarmRingingActivity : AppCompatActivity() {

    private lateinit var tvTime: TextView
    private lateinit var tvNote: TextView
    private lateinit var btnDismiss: Button
    private lateinit var btnSnooze: Button

    private lateinit var ivAlarmIcon: ImageView  // ← THÊM biến này

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var alarmId: Int = -1
    private var snoozeMinutes: Int = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContentView(R.layout.activity_alarm_ringing)
        setBackgroundTheme()
        initViews()
        loadAlarmData()
        startAlarm()
    }
    private fun setBackgroundTheme() {
        val themeManager = ThemeManager(this)
        val rootLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(
            android.R.id.content
        ).getChildAt(0) as androidx.constraintlayout.widget.ConstraintLayout

        val theme = themeManager.getCurrentTheme()
        theme?.let {
            when (it.type) {
                ThemeType.PRESET -> {
                    // Set drawable resource
                    rootLayout.setBackgroundResource(it.drawableRes)
                }
                ThemeType.CUSTOM -> {
                    // Load từ file
                    val file = themeManager.getCurrentThemeFile()
                    file?.let { themeFile ->
                        val bitmap = BitmapFactory.decodeFile(themeFile.absolutePath)
                        rootLayout.background = BitmapDrawable(resources, bitmap)
                    }
                }
            }
        }
    }
    private fun initViews() {
        ivAlarmIcon = findViewById(R.id.ivAlarmIcon)  // ← THÊM dòng này

        tvTime = findViewById(R.id.tvTime)
        tvNote = findViewById(R.id.tvNote)
        btnDismiss = findViewById(R.id.btnDismiss)
        btnSnooze = findViewById(R.id.btnSnooze)

        btnDismiss.setOnClickListener { dismissAlarm() }
        btnSnooze.setOnClickListener { snoozeAlarm() }
        startBellAnimation()

    }

    // Thay thế function loadAlarmData() trong AlarmRingingActivity.kt

    private fun loadAlarmData() {
        alarmId = intent.getIntExtra("alarm_id", -1)
        val hour = intent.getIntExtra("alarm_hour", 0)
        val minute = intent.getIntExtra("alarm_minute", 0)
        val amPm = intent.getStringExtra("alarm_am_pm") ?: "AM"
        val note = intent.getStringExtra("alarm_note") ?: ""
        snoozeMinutes = intent.getIntExtra("snooze_minutes", 5)

        android.util.Log.d("AlarmRinging", "Received note: '$note'")

        // Display time
        tvTime.text = String.format("%02d:%02d %s", hour, minute, amPm)

        // Display note - ẩn TextView nếu note rỗng
        if (note.isNotEmpty()) {
            android.util.Log.d("AlarmRinging", "Showing note")
            tvNote.text = note
            tvNote.visibility = android.view.View.VISIBLE
        } else {
            tvNote.visibility = android.view.View.GONE
        }

        // Hide snooze button if snooze disabled
        if (snoozeMinutes == 0) {
            btnSnooze.visibility = android.view.View.GONE
        } else {
            btnSnooze.text = "Snooze $snoozeMinutes min"
        }
    }

    private fun startAlarm() {
        val soundType = intent.getStringExtra("sound_type") ?: "default"
        val soundUri = intent.getStringExtra("sound_uri") ?: ""
        val vibrationPattern = intent.getStringExtra("vibration_pattern") ?: "default"
        val isSilentModeEnabled = intent.getBooleanExtra("is_silent_mode_enabled", false)

        // Play sound
        if (soundType != "off") {
            playSound(soundUri, isSilentModeEnabled)
        }

        // Start vibration
        if (vibrationPattern != "off") {
            startVibration(vibrationPattern)
        }
    }

    private fun playSound(soundUri: String, bypassSilentMode: Boolean) {
        try {
            val uri = if (soundUri.isNotEmpty()) {
                Uri.parse(soundUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                // QUAN TRỌNG: Dùng STREAM_ALARM để bypass silent mode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setAudioAttributes(audioAttributes)
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                }

                setDataSource(this@AlarmRingingActivity, uri)
                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration(pattern: String) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val vibrationPattern = when (pattern) {
            "short" -> longArrayOf(0, 300, 200, 300)
            "long" -> longArrayOf(0, 1000, 500, 1000)
            "double" -> longArrayOf(0, 500, 200, 500, 200, 500)
            else -> longArrayOf(0, 1000, 500, 1000, 500, 1000) // default
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(vibrationPattern, 0)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(vibrationPattern, 0)
        }
    }

    private fun dismissAlarm() {
        stopAlarm()
        stopBellAnimation()  // ← THÊM dòng này

        // Cancel notification
        val notificationManager = AlarmNotificationManager(this)
        notificationManager.cancelNotification(alarmId)

        finish()
    }

    private fun snoozeAlarm() {
        stopAlarm()
        stopBellAnimation()
        // Tạo Intent để trigger snooze action
        val snoozeIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = AlarmNotificationManager.ACTION_SNOOZE
            putExtra("alarm_id", alarmId)
            putExtra("alarm_title", intent.getStringExtra("alarm_label") ?: "Alarm")
            putExtra("snooze_minutes", snoozeMinutes)
        }

        // Gửi broadcast để xử lý snooze
        sendBroadcast(snoozeIntent)

        // Cancel notification hiện tại
        val notificationManager = AlarmNotificationManager(this)
        notificationManager.cancelNotification(alarmId)

        android.util.Log.d("AlarmRinging", "Snooze alarm $alarmId for $snoozeMinutes minutes")

        finish()
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null

            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        stopBellAnimation()  // ← THÊM dòng này

    }

    // ✅ THÊM HÀM MỚI: Start animation
    private fun startBellAnimation() {
        val shakeAnimation = android.view.animation.AnimationUtils.loadAnimation(
            this,
            R.anim.shake_bell
        )
        ivAlarmIcon.startAnimation(shakeAnimation)
    }

    // ✅ THÊM HÀM MỚI: Stop animation khi dismiss
    private fun stopBellAnimation() {
        ivAlarmIcon.clearAnimation()
    }

}