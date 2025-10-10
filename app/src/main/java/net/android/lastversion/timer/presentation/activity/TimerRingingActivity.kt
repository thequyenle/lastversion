package net.android.lastversion.timer.presentation.activity

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import net.android.lastversion.BaseActivity
import net.android.lastversion.R
import net.android.lastversion.utils.ThemeManager
import net.android.lastversion.utils.ThemeType
import net.android.last.service.TimerService

class TimerRingingActivity : BaseActivity() {

    private lateinit var tvTimesUpTitle: TextView
    private lateinit var tvTimesUpSubtitle: TextView
    private lateinit var btnStopTimesUp: Button
    private lateinit var ivTimerIcon: ImageView
    private lateinit var imgBackground: ImageView

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var soundUri: Uri? = null
    private var soundResId: Int = R.raw.astro

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

        setContentView(R.layout.activity_timer_ringing)

        // Hide system bars for full screen
        hideSystemBars()

        // Initialize views FIRST
        initViews()

        // Then set theme background
        setBackgroundTheme()

        loadTimerData()
        startTimerSound()
    }

    private fun hideSystemBars() {
        // Set window to draw edge-to-edge
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
        val theme = themeManager.getCurrentTheme()

        theme?.let {
            when (it.type) {
                ThemeType.PRESET -> {
                    imgBackground.setImageResource(it.drawableRes)
                }
                ThemeType.CUSTOM -> {
                    val file = themeManager.getCurrentThemeFile()
                    file?.let { themeFile ->
                        val bitmap = BitmapFactory.decodeFile(themeFile.absolutePath)
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
        imgBackground = findViewById(R.id.imgBackground)
        ivTimerIcon = findViewById(R.id.ivTimerIcon)
        tvTimesUpTitle = findViewById(R.id.tvTimesUpTitle)
        tvTimesUpSubtitle = findViewById(R.id.tvTimesUpSubtitle)
        btnStopTimesUp = findViewById(R.id.btnStopTimesUp)

        btnStopTimesUp.setOnClickListener { stopTimer() }
        startBellAnimation()
    }

    private fun loadTimerData() {
        val totalSeconds = intent.getIntExtra("total_seconds", 0)
        val soundUriString = intent.getStringExtra("sound_uri")
        soundResId = intent.getIntExtra("sound_res_id", R.raw.astro)

        if (!soundUriString.isNullOrEmpty()) {
            soundUri = Uri.parse(soundUriString)
        }

        // Format and display timer duration
        tvTimesUpSubtitle.text = formatTime(totalSeconds)
    }

    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> String.format("%d hours %d minutes %d seconds", hours, minutes, secs)
            minutes > 0 -> String.format("%d minutes %d seconds", minutes, secs)
            else -> String.format("%d seconds", secs)
        }
    }

    private fun startTimerSound() {
        playSound()
        startVibration()
    }

    private fun playSound() {
        try {
            stopSound()

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

                // Set data source based on what's available
                when {
                    soundUri != null -> {
                        try {
                            setDataSource(this@TimerRingingActivity, soundUri!!)
                        } catch (e: Exception) {
                            setDataSource(
                                this@TimerRingingActivity,
                                Uri.parse("android.resource://${packageName}/$soundResId")
                            )
                        }
                    }
                    soundResId != -1 -> {
                        setDataSource(
                            this@TimerRingingActivity,
                            Uri.parse("android.resource://${packageName}/$soundResId")
                        )
                    }
                    else -> {
                        setDataSource(
                            this@TimerRingingActivity,
                            Uri.parse("android.resource://${packageName}/${R.raw.astro}")
                        )
                    }
                }

                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(vibrationPattern, 0)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(vibrationPattern, 0)
        }
    }

    private fun stopTimer() {
        stopSound()
        stopVibration()
        stopBellAnimation()

        // Clear completion state from SharedPreferences
        clearCompletionState()

        // Stop the timer service
        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        startService(stopIntent)

        finish()
    }

    private fun clearCompletionState() {
        val prefs = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            remove("timer_completed")
            remove("completed_total_seconds")
            remove("completion_time")
            apply()
        }
        Log.d("TimerRingingActivity", "Completion state cleared")
    }

    private fun stopSound() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    private fun startBellAnimation() {
        val shakeAnimation = android.view.animation.AnimationUtils.loadAnimation(
            this,
            R.anim.shake_bell
        )
        ivTimerIcon.startAnimation(shakeAnimation)
    }

    private fun stopBellAnimation() {
        ivTimerIcon.clearAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSound()
        stopVibration()
        stopBellAnimation()
    }
}