package net.android.lastversion.alarm.activity

import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import net.android.lastversion.R
import net.android.lastversion.alarm.data.database.AlarmDatabase
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.presentation.viewmodel.AlarmViewModel
import net.android.lastversion.alarm.presentation.viewmodel.AlarmViewModelFactory
import net.android.lastversion.alarm.util.TimeUtils
import java.util.*

class SetAlarmActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_HOUR = "extra_hour"
        const val EXTRA_MINUTE = "extra_minute"
        const val EXTRA_AM_PM = "extra_am_pm"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_ACTIVE_DAYS = "extra_active_days"
        const val EXTRA_IS_SNOOZE_ENABLED = "extra_is_snooze_enabled"
        const val EXTRA_IS_VIBRATION_ENABLED = "extra_is_vibration_enabled"
        const val EXTRA_IS_SOUND_ENABLED = "extra_is_sound_enabled"
        const val EXTRA_IS_SILENT_MODE_ENABLED = "extra_is_silent_mode_enabled"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_IS_EDIT_MODE = "extra_is_edit_mode"
        const val RESULT_DELETED = 100
    }

    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var amPmSpinner: Spinner
    private lateinit var btnSave: TextView
    private lateinit var btnBack: ImageView

    // Active days checkboxes - Layout order: M, T, W, T, F, S, S
    private lateinit var cbMonday: CheckBox
    private lateinit var cbTuesday: CheckBox
    private lateinit var cbWednesday: CheckBox
    private lateinit var cbThursday: CheckBox
    private lateinit var cbFriday: CheckBox
    private lateinit var cbSaturday: CheckBox
    private lateinit var cbSunday: CheckBox
    // Domain model order: [Sun, Mon, Tue, Wed, Thu, Fri, Sat]
    private lateinit var checkboxes: List<CheckBox>

    // Options switches
    private lateinit var switchSnooze: Switch
    private lateinit var switchVibration: Switch
    private lateinit var switchSound: Switch
    private lateinit var switchSilentMode: Switch

    // Other options
    private lateinit var etAlarmNote: EditText
    private lateinit var tvPreview: TextView

    // ViewModel
    private val alarmViewModel: AlarmViewModel by viewModels {
        AlarmViewModelFactory(
            AlarmRepositoryImpl(
                AlarmDatabase.getDatabase(this).alarmDao()
            )
        )
    }

    // Variables for edit mode
    private var isEditMode = false
    private var alarmId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_alarm)

        initViews()
        setupTimePicker()
        setupClickListeners()
        loadDataFromIntent()
    }

    private fun initViews() {
        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)
        amPmSpinner = findViewById(R.id.amPmSpinner)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)

        // Bind checkboxes theo đúng ID trong layout
        cbMonday = findViewById(R.id.cbMonday)
        cbTuesday = findViewById(R.id.cbTuesday)
        cbWednesday = findViewById(R.id.cbWednesday)
        cbThursday = findViewById(R.id.cbThursday)
        cbFriday = findViewById(R.id.cbFriday)
        cbSaturday = findViewById(R.id.cbSaturday)
        cbSunday = findViewById(R.id.cbSunday)

        // IMPORTANT: Order theo domain model [Sun, Mon, Tue, Wed, Thu, Fri, Sat]
        // để match với activeDays BooleanArray
        checkboxes = listOf(
            cbSunday,    // index 0 = Sunday
            cbMonday,    // index 1 = Monday
            cbTuesday,   // index 2 = Tuesday
            cbWednesday, // index 3 = Wednesday
            cbThursday,  // index 4 = Thursday
            cbFriday,    // index 5 = Friday
            cbSaturday   // index 6 = Saturday
        )

        switchSnooze = findViewById(R.id.switchSnooze)
        switchVibration = findViewById(R.id.switchVibration)
        switchSound = findViewById(R.id.switchSound)
        switchSilentMode = findViewById(R.id.switchSilentMode)

        etAlarmNote = findViewById(R.id.etAlarmNote)
        tvPreview = findViewById(R.id.tvPreview)
    }

    private fun setupTimePicker() {
        // Setup hour picker (1-12 for 12-hour format)
        hourPicker.minValue = 1
        hourPicker.maxValue = 12

        // Setup minute picker (0-59)
        minutePicker.minValue = 0
        minutePicker.maxValue = 59

        // Setup AM/PM spinner
        val amPmOptions = arrayOf("AM", "PM")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, amPmOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        amPmSpinner.adapter = adapter

        // Set default values if not in edit mode
        if (!isEditMode) {
            val currentTime = TimeUtils.getCurrentTime12H()
            hourPicker.value = currentTime.first
            minutePicker.value = currentTime.second
            amPmSpinner.setSelection(if (currentTime.third == "AM") 0 else 1)

            // Set default switches to match layout defaults
            switchSnooze.isChecked = true
            switchVibration.isChecked = false  // Layout default
            switchSound.isChecked = true
            switchSilentMode.isChecked = true  // Layout default
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveAlarm()
        }

        tvPreview.setOnClickListener {
            Toast.makeText(this, "Preview alarm sound", Toast.LENGTH_SHORT).show()
        }

        // Add listener to checkboxes for future enhancements
        checkboxes.forEach { checkbox ->
            checkbox.setOnCheckedChangeListener { _, _ ->
                // Could add real-time preview of active days text here
            }
        }
    }

    private fun loadDataFromIntent() {
        isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false)

        if (isEditMode) {
            // Load existing alarm data for editing
            alarmId = intent.getIntExtra(EXTRA_ALARM_ID, 0)

            hourPicker.value = intent.getIntExtra(EXTRA_HOUR, 6)
            minutePicker.value = intent.getIntExtra(EXTRA_MINUTE, 0)

            val amPm = intent.getStringExtra(EXTRA_AM_PM) ?: "AM"
            amPmSpinner.setSelection(if (amPm == "AM") 0 else 1)

            // Load active days - match với domain model order
            val activeDays = intent.getBooleanArrayExtra(EXTRA_ACTIVE_DAYS) ?: BooleanArray(7) { false }
            checkboxes.forEachIndexed { index, checkbox ->
                checkbox.isChecked = activeDays[index]
            }

            // Load switches
            switchSnooze.isChecked = intent.getBooleanExtra(EXTRA_IS_SNOOZE_ENABLED, true)
            switchVibration.isChecked = intent.getBooleanExtra(EXTRA_IS_VIBRATION_ENABLED, false)
            switchSound.isChecked = intent.getBooleanExtra(EXTRA_IS_SOUND_ENABLED, true)
            switchSilentMode.isChecked = intent.getBooleanExtra(EXTRA_IS_SILENT_MODE_ENABLED, true)

            val note = intent.getStringExtra(EXTRA_NOTE) ?: ""
            etAlarmNote.setText(note)

            // Update title
            supportActionBar?.title = "Edit Alarm"
        } else {
            supportActionBar?.title = "Set Alarm"
        }
    }

    private fun saveAlarm() {
        val hour = hourPicker.value
        val minute = minutePicker.value
        val amPm = amPmSpinner.selectedItem.toString()

        // Get active days - theo domain model order [Sun, Mon, Tue, Wed, Thu, Fri, Sat]
        val activeDays = BooleanArray(7) { index ->
            checkboxes[index].isChecked
        }

        // Generate active days text
        val activeDaysText = TimeUtils.formatActiveDaysText(activeDays)

        // Get label - default since layout không có label EditText
        val label = "Alarm"

        // Get options
        val isSnoozeEnabled = switchSnooze.isChecked
        val isVibrationEnabled = switchVibration.isChecked
        val isSoundEnabled = switchSound.isChecked
        val isSilentModeEnabled = switchSilentMode.isChecked
        val alarmNote = etAlarmNote.text.toString().trim()

        // Create alarm object
        val alarm = Alarm(
            id = if (isEditMode) alarmId else 0, // 0 for auto-generate
            hour = hour,
            minute = minute,
            amPm = amPm,
            label = label,
            activeDays = activeDays,
            activeDaysText = activeDaysText,
            isEnabled = true, // New/edited alarms enabled by default
            isSnoozeEnabled = isSnoozeEnabled,
            isVibrationEnabled = isVibrationEnabled,
            isSoundEnabled = isSoundEnabled,
            isSilentModeEnabled = isSilentModeEnabled,
            note = alarmNote
        )

        // Save to database via ViewModel
        if (isEditMode) {
            alarmViewModel.updateAlarm(alarm)
            Toast.makeText(this, "Alarm updated", Toast.LENGTH_SHORT).show()
        } else {
            alarmViewModel.insertAlarm(alarm)
            Toast.makeText(this, "Alarm added", Toast.LENGTH_SHORT).show()
        }

        // Return success result
        setResult(RESULT_OK)
        finish()
    }
}