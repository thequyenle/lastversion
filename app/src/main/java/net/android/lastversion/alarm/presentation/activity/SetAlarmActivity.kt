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
import net.android.lastversion.BaseActivity
import net.android.lastversion.R
import net.android.lastversion.alarm.data.local.database.AlarmDatabase
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmSchedulerImpl
import net.android.lastversion.alarm.infrastructure.notification.AlarmNotificationManager
import net.android.lastversion.alarm.presentation.activity.AlarmRingingActivity
import net.android.lastversion.dialog.AlarmNoteDialog
import net.android.lastversion.utils.showWithHiddenNavigation
import android.view.LayoutInflater
import android.widget.RadioGroup
import android.widget.RadioButton
import androidx.core.content.ContextCompat

class SetAlarmActivity : BaseActivity() {

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



    // Add these companion object constants at the top of SetAlarmActivity class

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

        repository = AlarmRepositoryImpl(AlarmDatabase.getDatabase(this).alarmDao())
        scheduler = AlarmSchedulerImpl(this)
        notificationManager = AlarmNotificationManager(this)

        initComponents()
        initViews()
        setupViews()
        loadAlarmIfEdit()
        // Check if we're restoring from saved state
        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState)
        } else {
            // Only load defaults if we're not restoring state
            loadAlarmIfEdit()
        }
    }
    private fun restoreInstanceState(savedState: Bundle) {
        // Restore alarm settings
        snoozeMinutes = savedState.getInt(KEY_SNOOZE_MINUTES, 5)
        vibrationPattern = savedState.getString(KEY_VIBRATION_PATTERN, "default") ?: "default"
        soundType = savedState.getString(KEY_SOUND_TYPE, "default") ?: "default"
        isSilentModeEnabled = savedState.getBoolean(KEY_IS_SILENT_MODE, false)
        currentSoundUri = savedState.getString(KEY_SOUND_URI, "")
        alarmNote = savedState.getString(KEY_ALARM_NOTE, "")

        // Restore time picker values
        hourPicker.value = savedState.getInt(KEY_HOUR, 6)
        minutePicker.value = savedState.getInt(KEY_MINUTE, 0)
        amPmSpinner.value = savedState.getInt(KEY_AM_PM, 0)

        // Restore active days
        savedState.getBooleanArray(KEY_ACTIVE_DAYS)?.let { activeDays ->
            val layoutCheckboxes = listOf(cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday)
            layoutCheckboxes.forEachIndexed { index, textView ->
                if (index < activeDays.size) {
                    textView.isSelected = activeDays[index]
                }
            }
        }

        // If we're in edit mode, still load the alarm data
        if (intent.hasExtra(EXTRA_ALARM)) {
            isEditMode = true
            currentAlarm = intent.getParcelableExtra(EXTRA_ALARM)
            tvTitle.setText(R.string.edit_alarm_title)
        }

        // Update UI with restored values
        updateAlarmNoteDisplay()
        updateDisplayTexts()
        updateSilentModeUI()
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

            tvTitle.setText(R.string.edit_alarm_title)  // ✅ THAY ĐỔI
        } ?: run {
            tvTitle.setText(R.string.set_alarm_title)   // or R.string.edit_alarm_title
        }
    }

    private fun updateDisplayTexts() {
        // Snooze
        textSnoozeValue.text = when (snoozeMinutes) {
            0 -> getString(R.string.off)
            else -> getString(R.string.minutes_format, snoozeMinutes)
        }

        // Vibration
        textVibrationValue.text = when (vibrationPattern) {
            "off" -> getString(R.string.off)
            "default" -> getString(R.string.default_option)
            "short" -> getString(R.string.short_option)
            "long" -> getString(R.string.long_option)
            "double" -> getString(R.string.double_vibration)
            else -> getString(R.string.default_option)
        }

        // Sound
        textSoundValue.text = when (soundType) {
            "off" -> getString(R.string.off)
            "default" -> getString(R.string.default_option)
            "gentle" -> getString(R.string.gentle)
            "loud" -> getString(R.string.loud)
            "progressive" -> getString(R.string.progressive)
            "custom" -> getString(R.string.custom)
            else -> getString(R.string.default_option)
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sound_picker, null)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.rgSounds)

        tvDialogTitle.text = getString(R.string.choose_snooze_time)

        val options = arrayOf(
            getString(R.string.off),
            getString(R.string.five_minutes),
            getString(R.string.ten_minutes),
            getString(R.string.fifteen_minutes),
            getString(R.string.twenty_minutes),
            getString(R.string.thirty_minutes)
        )
        val values = arrayOf(0, 5, 10, 15, 20, 30)
        val currentIndex = values.indexOf(snoozeMinutes).takeIf { it >= 0 } ?: 1

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var tempSelectedPosition = currentIndex

        val tealColor = android.graphics.Color.parseColor("#84DCC6")
        val greyColor = android.graphics.Color.parseColor("#808080")

        val colorStateList = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(tealColor, greyColor)
        )

        options.forEachIndexed { index, option ->
            val radioButton = RadioButton(this).apply {
                text = option
                id = View.generateViewId()
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@SetAlarmActivity, android.R.color.black))

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    buttonTintList = colorStateList
                }

                setPadding(16, 24, 16, 24)
                isChecked = (index == currentIndex)

                setOnClickListener {
                    tempSelectedPosition = index
                }
            }

            radioGroup.addView(radioButton)
        }

        dialogView.findViewById<TextView>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.btnOk).setOnClickListener {
            snoozeMinutes = values[tempSelectedPosition]
            updateDisplayTexts()
            dialog.dismiss()
        }

        dialog.showWithHiddenNavigation()
    }


    private fun showVibrationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sound_picker, null)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.rgSounds)

        tvDialogTitle.text = getString(R.string.choose_vibration_type)

        val options = arrayOf(
            getString(R.string.off),
            getString(R.string.default_option),
            getString(R.string.short_option),
            getString(R.string.long_option),
            getString(R.string.double_vibration)
        )
        val values = arrayOf("off", "default", "short", "long", "double")
        val currentIndex = values.indexOf(vibrationPattern).takeIf { it >= 0 } ?: 1

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var tempSelectedPosition = currentIndex

        val tealColor = android.graphics.Color.parseColor("#84DCC6")
        val greyColor = android.graphics.Color.parseColor("#808080")

        val colorStateList = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(tealColor, greyColor)
        )

        options.forEachIndexed { index, option ->
            val radioButton = RadioButton(this).apply {
                text = option
                id = View.generateViewId()
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@SetAlarmActivity, android.R.color.black))

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    buttonTintList = colorStateList
                }

                setPadding(16, 24, 16, 24)
                isChecked = (index == currentIndex)

                setOnClickListener {
                    tempSelectedPosition = index
                }
            }

            radioGroup.addView(radioButton)
        }

        dialogView.findViewById<TextView>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.btnOk).setOnClickListener {
            vibrationPattern = values[tempSelectedPosition]
            updateDisplayTexts()
            dialog.dismiss()
        }

        dialog.showWithHiddenNavigation()
    }


    private fun showSoundDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sound_picker, null)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.rgSounds)

        tvDialogTitle.text = getString(R.string.choose_sound_type)

        val options = arrayOf(
            getString(R.string.off),
            getString(R.string.default_option),
            getString(R.string.gentle),
            getString(R.string.loud),
            getString(R.string.progressive),
            getString(R.string.custom_ellipsis)
        )
        val values = arrayOf("off", "default", "gentle", "loud", "progressive", "custom")
        val currentIndex = values.indexOf(soundType).takeIf { it >= 0 } ?: 1

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var tempSelectedPosition = currentIndex

        val tealColor = android.graphics.Color.parseColor("#84DCC6")
        val greyColor = android.graphics.Color.parseColor("#808080")

        val colorStateList = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(tealColor, greyColor)
        )

        options.forEachIndexed { index, option ->
            val radioButton = RadioButton(this).apply {
                text = option
                id = View.generateViewId()
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@SetAlarmActivity, android.R.color.black))

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    buttonTintList = colorStateList
                }

                setPadding(16, 24, 16, 24)
                isChecked = (index == currentIndex)

                setOnClickListener {
                    tempSelectedPosition = index
                }
            }

            radioGroup.addView(radioButton)
        }

        dialogView.findViewById<TextView>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.btnOk).setOnClickListener {
            soundType = values[tempSelectedPosition]

            if (soundType == "custom") {
                dialog.dismiss()
                openSoundPicker()
            } else {
                updateDisplayTexts()
                dialog.dismiss()
            }
        }

        dialog.showWithHiddenNavigation()
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
        const val RESULT_DELETED = Activity.RESULT_FIRST_USER

        // State keys for saving instance
        private const val KEY_SNOOZE_MINUTES = "snooze_minutes"
        private const val KEY_VIBRATION_PATTERN = "vibration_pattern"
        private const val KEY_SOUND_TYPE = "sound_type"
        private const val KEY_IS_SILENT_MODE = "is_silent_mode"
        private const val KEY_SOUND_URI = "sound_uri"
        private const val KEY_ALARM_NOTE = "alarm_note"
        private const val KEY_HOUR = "hour"
        private const val KEY_MINUTE = "minute"
        private const val KEY_AM_PM = "am_pm"
        private const val KEY_ACTIVE_DAYS = "active_days"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save all alarm settings
        outState.putInt(KEY_SNOOZE_MINUTES, snoozeMinutes)
        outState.putString(KEY_VIBRATION_PATTERN, vibrationPattern)
        outState.putString(KEY_SOUND_TYPE, soundType)
        outState.putBoolean(KEY_IS_SILENT_MODE, isSilentModeEnabled)
        outState.putString(KEY_SOUND_URI, currentSoundUri)
        outState.putString(KEY_ALARM_NOTE, alarmNote)

        // Save time picker values
        outState.putInt(KEY_HOUR, hourPicker.value)
        outState.putInt(KEY_MINUTE, minutePicker.value)
        outState.putInt(KEY_AM_PM, amPmSpinner.value)

        // Save active days
        val activeDays = BooleanArray(7)
        val layoutCheckboxes = listOf(cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday)
        layoutCheckboxes.forEachIndexed { layoutIndex, textView ->
            activeDays[layoutIndex] = textView.isSelected
        }
        outState.putBooleanArray(KEY_ACTIVE_DAYS, activeDays)    }

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
        dialog.showWithHiddenNavigation()
    }

    private fun updateAlarmNoteDisplay() {
        tvAlarmNoteValue.text = if (alarmNote.isEmpty()) "Add note" else alarmNote
    }
}