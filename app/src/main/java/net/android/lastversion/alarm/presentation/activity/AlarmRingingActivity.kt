package net.android.lastversion.alarm.presentation.activity

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // ✅ Broadcast receiver for stopping alarm
    private var stopAlarmReceiver: BroadcastReceiver? = null

    companion object {
        // Store reference to current instance
        private var currentInstance: AlarmRingingActivity? = null

        // Function to stop alarm from outside
        fun stopAlarmById(alarmId: Int) {
            android.util.Log.d("AlarmRinging", "🔴 stopAlarmById called for alarm $alarmId")
            android.util.Log.d("AlarmRinging", "   Current instance: ${currentInstance != null}")
            android.util.Log.d("AlarmRinging", "   Current instance alarmId: ${currentInstance?.alarmId}")

            if (currentInstance == null) {
                android.util.Log.w("AlarmRinging", "⚠️ No current instance found!")
                return
            }

            currentInstance?.let { activity ->
                if (activity.alarmId == alarmId) {
                    android.util.Log.d("AlarmRinging", "✅ IDs match! Stopping alarm $alarmId")

                    // Stop alarm sound and vibration
                    activity.stopAlarm()
                    activity.stopBellAnimation()

                    // Cancel notification
                    val notificationManager = AlarmNotificationManager(activity)
                    notificationManager.cancelNotification(alarmId)

                    // Close activity
                    activity.finish()
                } else {
                    android.util.Log.w("AlarmRinging", "⚠️ IDs don't match! Requested: $alarmId, Current: ${activity.alarmId}")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        hideSystemBars()
        setBackgroundTheme()
        initViews()
        loadAlarmData()

        // ✅ Set this as current instance AFTER alarmId is loaded
        currentInstance = this
        android.util.Log.d("AlarmRinging", "📱 AlarmRingingActivity created for alarm $alarmId, instance set")

        startAlarm()

        // ✅ Register broadcast receiver
        registerStopAlarmReceiver()
    }

    // ✅ Register broadcast receiver to stop alarm when switch is turned off
    private fun registerStopAlarmReceiver() {
        stopAlarmReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val receivedAlarmId = intent.getIntExtra("alarm_id", -1)
                android.util.Log.d("AlarmRinging", "🔴 Received stop broadcast for alarm $receivedAlarmId, current alarm: $alarmId")

                if (receivedAlarmId == alarmId) {
                    android.util.Log.d("AlarmRinging", "✅ IDs match! Stopping alarm and closing activity")
                    stopAlarm()

                    // Cancel notification
                    val notificationManager = AlarmNotificationManager(this@AlarmRingingActivity)
                    notificationManager.cancelNotification(alarmId)

                    finish()
                } else {
                    android.util.Log.d("AlarmRinging", "⚠️ IDs don't match, ignoring broadcast")
                }
            }
        }

        val filter = IntentFilter("ACTION_STOP_ALARM")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopAlarmReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopAlarmReceiver, filter)
        }

        android.util.Log.d("AlarmRinging", "📡 Broadcast receiver registered for alarm $alarmId")
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
        val imgBackground = findViewById<ImageView>(R.id.imgBackground)

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
        android.util.Log.d("AlarmRinging", "📋 loadAlarmData: Received alarm_id from intent: $alarmId")

        val hour = intent.getIntExtra("alarm_hour", 0)
        val minute = intent.getIntExtra("alarm_minute", 0)
        val amPm = intent.getStringExtra("alarm_am_pm") ?: "AM"
        val note = intent.getStringExtra("alarm_note") ?: ""
        snoozeMinutes = intent.getIntExtra("snooze_minutes", 5)

        android.util.Log.d("AlarmRinging", "Received note: '$note'")

        tvTime.text = String.format("%02d:%02d %s", hour, minute, amPm)

        if (note.isNotEmpty()) {
            android.util.Log.d("AlarmRinging", "Showing note")
            tvNote.text = note
            tvNote.visibility = View.VISIBLE
        } else {
            tvNote.visibility = View.GONE
        }

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

        if (soundType != "off") {
            playSound(soundUri, isSilentModeEnabled)
        }

        if (vibrationPattern != "off") {
            startVibration(vibrationPattern)
        }
    }

    private fun playSound(soundUri: String, bypassSilentMode: Boolean) {
        try {
            val soundResId = intent.getIntExtra("sound_res_id", 0)
            val soundType = intent.getStringExtra("sound_type") ?: "default"

            val resIdToPlay = when {
                soundResId != 0 -> soundResId
                soundType == "astro" -> R.raw.astro
                soundType == "bell" -> R.raw.bell
                soundType == "piano" -> R.raw.piano
                soundType == "custom" && soundUri.isNotEmpty() -> 0
                else -> 0
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

                if (resIdToPlay != 0) {
                    val uri = Uri.parse("android.resource://${packageName}/$resIdToPlay")
                    setDataSource(this@AlarmRingingActivity, uri)
                } else if (soundUri.isNotEmpty()) {
                    setDataSource(this@AlarmRingingActivity, Uri.parse(soundUri))
                } else {
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setDataSource(this@AlarmRingingActivity, defaultUri)
                }

                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }

            android.util.Log.d("AlarmRinging", "✅ Sound started with type: $soundType")
        } catch (e: Exception) {
            android.util.Log.e("AlarmRinging", "❌ Error playing sound", e)
            e.printStackTrace()
        }
    }

    private fun startVibration(pattern: String) {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator == null) {
                android.util.Log.e("AlarmRinging", "❌ Vibrator service is null")
                return
            }

            if (!vibrator!!.hasVibrator()) {
                android.util.Log.e("AlarmRinging", "❌ Device does not have vibrator")
                return
            }

            val vibrationPattern = when (pattern) {
                "short" -> longArrayOf(0, 300, 200, 300, 200, 300)
                "long" -> longArrayOf(0, 1000, 500, 1000, 500, 1000)
                "double" -> longArrayOf(0, 500, 200, 500, 200, 500, 200, 500)
                "default" -> longArrayOf(0, 1000, 500, 1000, 500, 1000)
                else -> longArrayOf(0, 1000, 500, 1000, 500, 1000)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(vibrationPattern, 0)
                vibrator?.vibrate(effect)
                android.util.Log.d("AlarmRinging", "✅ Vibration started (API 26+) with pattern: $pattern")
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(vibrationPattern, 0)
                android.util.Log.d("AlarmRinging", "✅ Vibration started (API < 26) with pattern: $pattern")
            }

        } catch (e: Exception) {
            android.util.Log.e("AlarmRinging", "❌ Error starting vibration", e)
            e.printStackTrace()
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

    override fun onResume() {
        super.onResume()

        // ✅ Check if alarm is still enabled when activity resumes
        // This handles the case where user toggles switch while activity is in background
        if (alarmId != 0) { // Skip for preview mode
            checkAlarmStatus()
        }
    }

    private fun checkAlarmStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val repository = net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl(
                    net.android.lastversion.alarm.data.local.database.AlarmDatabase.getDatabase(this@AlarmRingingActivity).alarmDao()
                )
                val alarm = repository.getAlarmById(alarmId)

                withContext(Dispatchers.Main) {
                    if (alarm == null || !alarm.isEnabled) {
                        android.util.Log.d("AlarmRinging", "⚠️ Alarm $alarmId is disabled or deleted, closing activity")
                        stopAlarm()
                        stopBellAnimation()
                        finish()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AlarmRinging", "Error checking alarm status", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear current instance
        if (currentInstance == this) {
            currentInstance = null
        }

        stopAlarm()
        stopBellAnimation()

        // ✅ Unregister receiver
        try {
            stopAlarmReceiver?.let {
                unregisterReceiver(it)
                android.util.Log.d("AlarmRinging", "📡 Broadcast receiver unregistered")
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmRinging", "❌ Error unregistering receiver", e)
        }

        android.util.Log.d("AlarmRinging", "📱 AlarmRingingActivity destroyed for alarm $alarmId")
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