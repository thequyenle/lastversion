package net.android.lastversion.alarm.presentation.activity

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import net.android.lastversion.R

class EditAlarmActivity : AppCompatActivity() {

    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var amPmSpinner: Spinner
    private lateinit var btnSave: TextView
    private lateinit var btnBack: ImageView

    // Active days checkboxes
    private lateinit var cbMonday: CheckBox
    private lateinit var cbTuesday: CheckBox
    private lateinit var cbWednesday: CheckBox
    private lateinit var cbThursday: CheckBox
    private lateinit var cbFriday: CheckBox
    private lateinit var cbSaturday: CheckBox
    private lateinit var cbSunday: CheckBox

    // Options switches
    private lateinit var switchSnooze: Switch
    private lateinit var switchVibration: Switch
    private lateinit var switchSound: Switch
    private lateinit var switchSilentMode: Switch

    // Other options
    private lateinit var etAlarmNote: EditText
    private lateinit var tvPreview: TextView

    // Alarm data from intent
    private var alarmId: Int = -1
    private var alarmHour: Int = 6
    private var alarmMinute: Int = 0
    private var alarmAmPm: String = "AM"
    private var alarmDays: BooleanArray = BooleanArray(7) { false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_alarm)

        getAlarmDataFromIntent()
        initViews()
        setupTimePicker()
        loadAlarmData()
        setupClickListeners()
    }

    private fun getAlarmDataFromIntent() {
        alarmId = intent.getIntExtra("ALARM_ID", -1)
        alarmHour = intent.getIntExtra("ALARM_HOUR", 6)
        alarmMinute = intent.getIntExtra("ALARM_MINUTE", 0)
        alarmAmPm = intent.getStringExtra("ALARM_AMPM") ?: "AM"
        alarmDays = intent.getBooleanArrayExtra("ALARM_DAYS") ?: BooleanArray(7) { false }
    }

    private fun initViews() {
        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)
        amPmSpinner = findViewById(R.id.amPmSpinner)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)

        cbMonday = findViewById(R.id.cbMonday)
        cbTuesday = findViewById(R.id.cbTuesday)
        cbWednesday = findViewById(R.id.cbWednesday)
        cbThursday = findViewById(R.id.cbThursday)
        cbFriday = findViewById(R.id.cbFriday)
        cbSaturday = findViewById(R.id.cbSaturday)
        cbSunday = findViewById(R.id.cbSunday)

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
    }

    private fun loadAlarmData() {
        // Set time picker values
        hourPicker.value = alarmHour
        minutePicker.value = alarmMinute
        amPmSpinner.setSelection(if (alarmAmPm == "AM") 0 else 1)

        // Set active days
        val dayCheckboxes = arrayOf(cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday)
        for (i in alarmDays.indices) {
            dayCheckboxes[i].isChecked = alarmDays[i]
        }

        // Load other alarm settings from preferences or database
        // For now, set some default values
        switchSnooze.isChecked = intent.getBooleanExtra("ALARM_SNOOZE", true)
        switchVibration.isChecked = intent.getBooleanExtra("ALARM_VIBRATION", false)
        switchSound.isChecked = intent.getBooleanExtra("ALARM_SOUND", true)
        switchSilentMode.isChecked = intent.getBooleanExtra("ALARM_SILENT_MODE", true)
        etAlarmNote.setText(intent.getStringExtra("ALARM_NOTE") ?: "")
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            updateAlarm()
        }

        tvPreview.setOnClickListener {
            // Handle preview click
            Toast.makeText(this, "Preview alarm sound", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAlarm() {
        val hour = hourPicker.value
        val minute = minutePicker.value
        val amPm = amPmSpinner.selectedItem.toString()

        // Get active days
        val activeDays = mutableListOf<String>()
        val dayCheckboxes = arrayOf(cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday)
        val dayNames = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        for (i in dayCheckboxes.indices) {
            if (dayCheckboxes[i].isChecked) {
                activeDays.add(dayNames[i])
            }
        }

        // Get options
        val isSnoozeEnabled = switchSnooze.isChecked
        val isVibrationEnabled = switchVibration.isChecked
        val isSoundEnabled = switchSound.isChecked
        val isSilentModeEnabled = switchSilentMode.isChecked
        val alarmNote = etAlarmNote.text.toString()

        // Here you would update the alarm in database or shared preferences
        // For now, just show a toast and finish
        val timeString = String.format("%02d:%02d %s", hour, minute, amPm)
        Toast.makeText(this, "Alarm updated for $timeString", Toast.LENGTH_SHORT).show()

        // Set result and finish
        setResult(RESULT_OK)
        finish()
    }
}