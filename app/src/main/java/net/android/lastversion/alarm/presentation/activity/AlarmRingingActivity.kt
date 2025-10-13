package net.android.lastversion.alarm.presentation.activity

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
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
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import net.android.lastversion.BaseActivity
import net.android.lastversion.R
import net.android.lastversion.alarm.infrastructure.notification.AlarmNotificationManager
import net.android.lastversion.alarm.infrastructure.receiver.AlarmActionReceiver
import net.android.lastversion.utils.ThemeManager
import net.android.lastversion.utils.ThemeType

class AlarmRingingActivity : BaseActivity() {

    private lateinit var tvTime: TextView
    private lateinit var tvNote: TextView
    private lateinit var btnDismiss: Button
    private lateinit var btnSnooze: Button
    private lateinit var ivAlarmIcon: ImageView

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var alarmId: Int = -1
    private var snoozeMinutes: Int = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Remove dim/overlay from window
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
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

        // Hide system bars for full screen
        hideSystemBars()

        // Set theme background
        setBackgroundTheme()

        initViews()
        loadAlarmData()
        startAlarm()
    }

    private fun hideSystemBars() {
        // Set window to draw edge-to-edge (no rounded corners)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(
                    android.view.WindowInsets.Type.statusBars() or
                            android.view.WindowInsets.Type.navigationBars()
                )
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 and below
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
        }
    }

    private fun setBackgroundTheme() {
        val themeManager = ThemeManager(this)

        // ✅ CRITICAL: Use ImageView instead of root layout background
        val imgBackground = findViewById<ImageView>(R.id.imgBackground)

        val theme = themeManager.getCurrentTheme()
        theme?.let {
            when (it.type) {
                ThemeType.PRESET -> {
                    // ✅ Set image resource (not background)
                    imgBackground.setImageResource(it.drawableRes)
                }
                ThemeType.CUSTOM -> {
                    val file = themeManager.getCurrentThemeFile()
                    file?.let { themeFile ->
                        val bitmap = BitmapFactory.decodeFile(themeFile.absolutePath)
                        // ✅ Set bitmap (not background)
                        imgBackground.setImageBitmap(bitmap)
                    }
                }
                ThemeType.ADD_NEW -> {
                    // Do nothing
                }
            }
        }
    }

    private fun initViews() {
        ivAlarmIcon = findViewById(R.id.ivAlarmIcon)
        tvTime = findViewById(R.id.tvTime)
        tvNote = findViewById(R.id.tvNote)
        btnDismiss = findViewById(R.id.btnDismiss)
        btnSnooze = findViewById(R.id.btnSnooze)

        btnDismiss.setOnClickListener { dismissAlarm() }
        btnSnooze.setOnClickListener { snoozeAlarm() }
        startBellAnimation()
    }

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

        // Display note - hide TextView if note is empty
        if (note.isNotEmpty()) {
            android.util.Log.d("AlarmRinging", "Showing note")
            tvNote.text = note
            tvNote.visibility = View.VISIBLE
        } else {
            tvNote.visibility = View.GONE
        }

        // Hide snooze button if snooze disabled
        if (snoozeMinutes == 0) {
            btnSnooze.visibility = View.GONE
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
            // Check if we received a sound_res_id from SetAlarmActivity
            val soundResId = intent.getIntExtra("sound_res_id", 0)

            if (soundResId != 0) {
                // Play from raw resource
                mediaPlayer = MediaPlayer.create(this, soundResId).apply {
                    isLooping = true
                    setVolume(1.0f, 1.0f)
                    start()
                }
            } else {
                // Use URI if provided, otherwise fall back to default ringtone
                val uri = if (soundUri.isNotEmpty()) {
                    Uri.parse(soundUri)
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }

                mediaPlayer = MediaPlayer().apply {
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
            else -> longArrayOf(0, 1000, 500, 1000, 500, 1000)
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
        stopBellAnimation()

        val notificationManager = AlarmNotificationManager(this)
        notificationManager.cancelNotification(alarmId)

        finish()
    }

    private fun snoozeAlarm() {
        stopAlarm()
        stopBellAnimation()

        val snoozeIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = AlarmNotificationManager.ACTION_SNOOZE
            putExtra("alarm_id", alarmId)
            putExtra("alarm_title", intent.getStringExtra("alarm_label") ?: "Alarm")
            putExtra("snooze_minutes", snoozeMinutes)
        }

        sendBroadcast(snoozeIntent)

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
        stopBellAnimation()
    }

    private fun startBellAnimation() {
        val shakeAnimation = android.view.animation.AnimationUtils.loadAnimation(
            this,
            R.anim.shake_bell
        )
        ivAlarmIcon.startAnimation(shakeAnimation)
    }

    private fun stopBellAnimation() {
        ivAlarmIcon.clearAnimation()
    }
}