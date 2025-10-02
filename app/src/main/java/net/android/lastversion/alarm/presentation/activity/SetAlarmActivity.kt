package net.android.lastversion.alarm.presentation.activity

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import net.android.lastversion.R
import net.android.lastversion.alarm.data.local.database.AlarmDatabase
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmSchedulerImpl
import net.android.lastversion.alarm.infrastructure.notification.AlarmNotificationManager
import net.android.lastversion.alarm.presentation.activity.AlarmRingingActivity
import net.android.lastversion.dialog.AlarmNoteDialog


class SetAlarmActivity : AppCompatActivity() {

    // Views
    private lateinit var btnBack: ImageView
    private lateinit var btnSave: TextView
    private lateinit var hourPicker: com.shawnlin.numberpicker.NumberPicker
    private lateinit var minutePicker: com.shawnlin.numberpicker.NumberPicker
    private lateinit var amPmSpinner: com.shawnlin.numberpicker.NumberPicker
    private lateinit var tvPreview: LinearLayout
    private lateinit var tvAlarmNoteValue: TextView
    private lateinit var layoutAlarmNote: LinearLayout
    private var alarmNote = ""

    // Day checkboxes - ORDER MATCHES LAYOUT (Monday first)
    private lateinit var cbMonday: TextView
    private lateinit var cbTuesday: TextView
    private lateinit var cbWednesday: TextView
    private lateinit var cbThursday: TextView
    private lateinit var cbFriday: TextView
    private lateinit var cbSaturday: TextView
    private lateinit var cbSunday: TextView

    // Trong khai báo biến
    private lateinit var switchSilentMode: ImageView
    private var isSilentModeEnabled = true  // Mặc định bật (vì src="ic_switch_on")

    private lateinit var layoutSnooze: LinearLayout
    private lateinit var textSnoozeValue: TextView

    private lateinit var layoutVibration: LinearLayout
    private lateinit var textVibrationValue: TextView

    private lateinit var layoutSound: LinearLayout
    private lateinit var textSoundValue: TextView

    // Data
    private lateinit var repository: AlarmRepositoryImpl
    private lateinit var scheduler: AlarmSchedulerImpl
    private lateinit var notificationManager: AlarmNotificationManager

    private var currentAlarm: Alarm? = null
    private var isEditMode = false

    private lateinit var tvTitle: EditText  // ✅ THÊM


    // Alarm settings
    private var snoozeMinutes = 5
    private var vibrationPattern = "default"
    private var soundType = "default"
    private var currentSoundUri = ""

    private val soundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let { uri ->
                currentSoundUri = uri.toString()
                soundType = "custom"
                textSoundValue.text = "Tùy chỉnh"
            }
        }
    }

    private fun setupDayButtons() {
        val dayButtons = listOf(
            cbMonday,
            cbTuesday,
            cbWednesday,
            cbThursday,
            cbFriday,
            cbSaturday,
            cbSunday
        )

        dayButtons.forEach { textView ->
            textView.setOnClickListener {
                textView.isSelected = !textView.isSelected
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_alarm)

        initComponents()
        initViews()
        setupViews()
        loadAlarmIfEdit()
    }

    private fun initComponents() {
        repository = AlarmRepositoryImpl(AlarmDatabase.getDatabase(this).alarmDao())
        scheduler = AlarmSchedulerImpl(this)
        notificationManager = AlarmNotificationManager(this)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSave)
        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)
        amPmSpinner = findViewById(R.id.amPmSpinner)
        tvAlarmNoteValue = findViewById(R.id.tvAlarmNoteValue)
        layoutAlarmNote = findViewById(R.id.layoutAlarmNote)
        tvPreview = findViewById<LinearLayout>(R.id.tvPreview)

        tvTitle = findViewById(R.id.tvTitle)  // ✅ THÊM


        // Day checkboxes
        cbMonday = findViewById(R.id.cbMonday)
        cbTuesday = findViewById(R.id.cbTuesday)
        cbWednesday = findViewById(R.id.cbWednesday)
        cbThursday = findViewById(R.id.cbThursday)
        cbFriday = findViewById(R.id.cbFriday)
        cbSaturday = findViewById(R.id.cbSaturday)
        cbSunday = findViewById(R.id.cbSunday)

        layoutSnooze = findViewById(R.id.layoutSnooze)
        textSnoozeValue = findViewById(R.id.textSnoozeValue)

        layoutVibration = findViewById(R.id.layoutVibration)
        textVibrationValue = findViewById(R.id.textVibrationValue)

        layoutSound = findViewById(R.id.layoutSound)
        textSoundValue = findViewById(R.id.textSoundValue)

        switchSilentMode = findViewById(R.id.switchSilentMode)
    }

    private fun setupViews() {
        setupTimePickers()
        setupClickListeners()
        setupDayButtons()  // ← THÊM DÒNG NÀY
        setDefaultValues()
    }

    private fun setupTimePickers() {
        hourPicker.apply {
            minValue = 1
            maxValue = 12
        }

        minutePicker.apply {
            minValue = 0
            maxValue = 59
            setFormatter { i -> String.format("%02d", i) }
        }

        amPmSpinner.minValue = 0
        amPmSpinner.maxValue = 1
        amPmSpinner.displayedValues = arrayOf("AM", "PM")
        amPmSpinner.value = 0
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            saveAlarm()
        }

        tvPreview.setOnClickListener {
            playPreviewSound()
        }

        layoutSnooze.setOnClickListener {
            showSnoozeDialog()
        }

        layoutVibration.setOnClickListener {
            showVibrationDialog()
        }

        layoutSound.setOnClickListener {
            showSoundDialog()
        }

        switchSilentMode.setOnClickListener {
            isSilentModeEnabled = !isSilentModeEnabled
            updateSilentModeUI()
        }
        layoutAlarmNote.setOnClickListener {
            showAlarmNoteDialog()
        }
    }

    private fun setDefaultValues() {
        if (!isEditMode) {
            val currentTime = java.util.Calendar.getInstance()
            val hour = currentTime.get(java.util.Calendar.HOUR)
            val minute = currentTime.get(java.util.Calendar.MINUTE)
            val amPm = if (currentTime.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) 0 else 1  // ĐỔI THÀNH INT

            hourPicker.value = if (hour == 0) 12 else hour
            minutePicker.value = minute
            amPmSpinner.value = amPm  // ĐỔI DÒNG NÀY (không phải setSelection)
            tvAlarmNoteValue.text = "Wake Up!!!"
            // Default settings
            snoozeMinutes = 5
            vibrationPattern = "default"
            soundType = "default"
            isSilentModeEnabled = false  // Hoặc true tùy ý
            updateSilentModeUI()  // ← THÊM DÒNG NÀY


            updateDisplayTexts()
        }
    }

    private fun loadAlarmIfEdit() {
        intent.getParcelableExtra<Alarm>(EXTRA_ALARM)?.let { alarm ->
            isEditMode = true
            currentAlarm = alarm

            hourPicker.value = alarm.hour
            minutePicker.value = alarm.minute
            amPmSpinner.value = if (alarm.amPm == "AM") 0 else 1

            alarmNote = alarm.note
            updateAlarmNoteDisplay()
            // Load active days
            val layoutCheckboxes = listOf(cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday)
            layoutCheckboxes.forEachIndexed { layoutIndex, textView ->  // Đổi tên biến
                val domainIndex = toDomainIndex(layoutIndex)
                textView.isSelected = alarm.activeDays[domainIndex]  // Đổi .isChecked → .isSelected
                Log.d("DayButton", "Clicked: ${(textView as TextView).text}, isSelected: ${textView.isSelected}")            }

            // Load alarm settings
            snoozeMinutes = alarm.snoozeMinutes
            vibrationPattern = alarm.vibrationPattern
            soundType = alarm.soundType
            isSilentModeEnabled = alarm.isSilentModeEnabled
            updateSilentModeUI()  // ← THÊM DÒNG NÀY
            currentSoundUri = alarm.soundUri

            updateDisplayTexts()

            tvTitle.setText("Edit Alarm")  // ✅ THAY ĐỔI
        } ?: run {
            tvTitle.setText("Set Alarm")   // ✅ THAY ĐỔI
        }
    }

    private fun updateDisplayTexts() {
        // Snooze
        textSnoozeValue.text = when (snoozeMinutes) {
            0 -> "Tắt"
            else -> "$snoozeMinutes phút"
        }

        // Vibration
        textVibrationValue.text = when (vibrationPattern) {
            "off" -> "Tắt"
            "default" -> "Mặc định"
            "short" -> "Ngắn"
            "long" -> "Dài"
            "double" -> "Rung đôi"
            else -> "Mặc định"
        }

        // Sound
        textSoundValue.text = when (soundType) {
            "off" -> "Tắt"
            "default" -> "Mặc định"
            "gentle" -> "Nhẹ nhàng"
            "loud" -> "Lớn"
            "progressive" -> "Tăng dần"
            "custom" -> "Tùy chỉnh"
            else -> "Mặc định"
        }
    }

    private fun updateSilentModeUI() {
        if (isSilentModeEnabled) {
            switchSilentMode.setImageResource(R.drawable.ic_switch_on)
        } else {
            switchSilentMode.setImageResource(R.drawable.ic_switch_off)
        }
    }

    private fun saveAlarm() {
        lifecycleScope.launch {
            try {
                val hour = hourPicker.value
                val minute = minutePicker.value
                val amPm = if (amPmSpinner.value == 0) "AM" else "PM"
                val note = alarmNote.trim().ifEmpty { "Wake Up!!!" }

                // Get active days
                val activeDays = BooleanArray(7) { false }
                val layoutCheckboxes = listOf(cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday)
                layoutCheckboxes.forEachIndexed { layoutIndex, textView ->  // Đổi tên biến
                    if (textView.isSelected) {  // Đổi .isChecked → .isSelected
                        val domainIndex = toDomainIndex(layoutIndex)
                        activeDays[domainIndex] = true
                    }
            }

                val alarm = Alarm(
                    id = if (isEditMode) currentAlarm?.id ?: 0 else 0,
                    hour = hour,
                    minute = minute,
                    amPm = amPm,
                    activeDays = activeDays,
                    isEnabled = true,
                    snoozeMinutes = snoozeMinutes,
                    vibrationPattern = vibrationPattern,
                    soundType = soundType,
                    isSilentModeEnabled = isSilentModeEnabled,
                    note = note,
                    soundUri = currentSoundUri
                )

                if (isEditMode) {
                    repository.updateAlarm(alarm)
                    scheduler.cancelAlarm(alarm.id)
                } else {
                    repository.insertAlarm(alarm)
                }

                scheduler.scheduleAlarm(alarm)

                setResult(Activity.RESULT_OK)
                finish()

            } catch (e: Exception) {
                Toast.makeText(this@SetAlarmActivity, "Error saving alarm: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playPreviewSound() {
        val intent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra("alarm_id", 0) // Preview mode
            putExtra("alarm_hour", hourPicker.value)
            putExtra("alarm_minute", minutePicker.value)
            putExtra("alarm_am_pm", if (amPmSpinner.value == 0) "AM" else "PM")  // ĐỔI DÒNG NÀY
            putExtra("alarm_note", alarmNote.ifBlank { "Wake Up!!!" })  // ← Thêm .ifEmpty
            putExtra("snooze_minutes", snoozeMinutes)
            putExtra("vibration_pattern", vibrationPattern)
            putExtra("sound_type", soundType)
            putExtra("is_silent_mode_enabled", isSilentModeEnabled)
            putExtra("sound_uri", currentSoundUri)
        }
        startActivity(intent)
    }

    private fun showSnoozeDialog() {
        val options = arrayOf("Tắt", "5 phút", "10 phút", "15 phút", "20 phút", "30 phút")
        val values = arrayOf(0, 5, 10, 15, 20, 30)

        val currentIndex = values.indexOf(snoozeMinutes).takeIf { it >= 0 } ?: 1

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chọn thời gian báo lại")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                snoozeMinutes = values[which]
                updateDisplayTexts()
                dialog.dismiss()
            }
            .show()
    }

    private fun showVibrationDialog() {
        val options = arrayOf("Tắt", "Mặc định", "Ngắn", "Dài", "Rung đôi")
        val values = arrayOf("off", "default", "short", "long", "double")

        val currentIndex = values.indexOf(vibrationPattern).takeIf { it >= 0 } ?: 1

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chọn kiểu rung")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                vibrationPattern = values[which]
                updateDisplayTexts()
                dialog.dismiss()
            }
            .show()
    }

    private fun showSoundDialog() {
        val options = arrayOf("Tắt", "Mặc định", "Nhẹ nhàng", "Lớn", "Tăng dần", "Tùy chỉnh...")
        val values = arrayOf("off", "default", "gentle", "loud", "progressive", "custom")

        val currentIndex = values.indexOf(soundType).takeIf { it >= 0 } ?: 1

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Chọn âm thanh")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                if (values[which] == "custom") {
                    dialog.dismiss()
                    openSoundPicker()
                } else {
                    soundType = values[which]
                    currentSoundUri = ""
                    updateDisplayTexts()
                    dialog.dismiss()
                }
            }
            .show()
    }

    private fun openSoundPicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Chọn nhạc chuông báo thức")
            if (currentSoundUri.isNotEmpty()) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(currentSoundUri))
            }
        }
        soundPickerLauncher.launch(intent)
    }

    // Layout index (Mon=0...Sun=6) to Domain index (Sun=0...Sat=6)
    private fun toDomainIndex(layoutIndex: Int): Int = (layoutIndex + 1) % 7

    companion object {
        const val EXTRA_ALARM = "extra_alarm"
        const val RESULT_DELETED = 100
    }

    override fun onResume() {
        super.onResume()
        showSystemUI(white = false)
    }

    private fun Activity.showSystemUI(white: Boolean = false) {
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        if (white) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    private fun showAlarmNoteDialog() {
        val dialog = AlarmNoteDialog(
            context = this,
            currentNote = alarmNote,
            onNoteSet = { note ->
                alarmNote = note
                updateAlarmNoteDisplay()
            }
        )
        dialog.show()
    }

    private fun updateAlarmNoteDisplay() {
        tvAlarmNoteValue.text = if (alarmNote.isEmpty()) "Add note" else alarmNote
    }
}