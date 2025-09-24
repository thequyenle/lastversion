package net.android.lastversion.activity

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import net.android.lastversion.R

class SetAlarmActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_alarm)

        initViews()
        setupTimePicker()
        setupClickListeners()
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
        hourPicker.value = 6 // Default to 6

        // Setup minute picker (0-59)
        minutePicker.minValue = 0
        minutePicker.maxValue = 59
        minutePicker.value = 0 // Default to 00

        // Setup AM/PM spinner
        val amPmOptions = arrayOf("AM", "PM")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, amPmOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        amPmSpinner.adapter = adapter
        amPmSpinner.setSelection(0) // Default to AM
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveAlarm()
        }

        tvPreview.setOnClickListener {
            // Handle preview click
            Toast.makeText(this, "Preview alarm sound", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAlarm() {
        val hour = hourPicker.value
        val minute = minutePicker.value
        val amPm = amPmSpinner.selectedItem.toString()

        // Get active days
        val activeDays = mutableListOf<String>()
        if (cbMonday.isChecked) activeDays.add("Mon")
        if (cbTuesday.isChecked) activeDays.add("Tue")
        if (cbWednesday.isChecked) activeDays.add("Wed")
        if (cbThursday.isChecked) activeDays.add("Thu")
        if (cbFriday.isChecked) activeDays.add("Fri")
        if (cbSaturday.isChecked) activeDays.add("Sat")
        if (cbSunday.isChecked) activeDays.add("Sun")

        // Get options
        val isSnoozeEnabled = switchSnooze.isChecked
        val isVibrationEnabled = switchVibration.isChecked
        val isSoundEnabled = switchSound.isChecked
        val isSilentModeEnabled = switchSilentMode.isChecked
        val alarmNote = etAlarmNote.text.toString()

        // Here you would save the alarm to database or shared preferences
        // For now, just show a toast and finish
        val timeString = String.format("%02d:%02d %s", hour, minute, amPm)
        Toast.makeText(this, "Alarm set for $timeString", Toast.LENGTH_SHORT).show()

        // Set result and finish
        setResult(RESULT_OK)
        finish()
    }
}