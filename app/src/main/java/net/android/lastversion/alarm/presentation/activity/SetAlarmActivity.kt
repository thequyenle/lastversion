package net.android.lastversion.alarm.presentation.activity

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.android.lastversion.R
import net.android.lastversion.alarm.data.local.database.AlarmDatabase
import net.android.lastversion.alarm.data.repository.AlarmRepositoryImpl
import net.android.lastversion.alarm.domain.model.Alarm
import net.android.lastversion.alarm.infrastructure.scheduler.AlarmSchedulerImpl
import net.android.lastversion.alarm.infrastructure.notification.AlarmNotificationManager

class SetAlarmActivity : AppCompatActivity() {

    // Views - using findViewById to match existing layout
    private lateinit var btnBack: ImageView
    private lateinit var btnSave: TextView
    private lateinit var etLabel: EditText
    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var amPmSpinner: Spinner
    private lateinit var etAlarmNote: EditText
    private lateinit var tvPreview: TextView

    // Day checkboxes - ORDER MATCHES LAYOUT (Monday first)
    private lateinit var cbMonday: CheckBox
    private lateinit var cbTuesday: CheckBox
    private lateinit var cbWednesday: CheckBox
    private lateinit var cbThursday: CheckBox
    private lateinit var cbFriday: CheckBox
    private lateinit var cbSaturday: CheckBox
    private lateinit var cbSunday: CheckBox

    // Switches
    private lateinit var switchSnooze: Switch
    private lateinit var switchVibration: Switch
    private lateinit var switchSound: Switch
    private lateinit var switchSilentMode: Switch

    // Data
    private lateinit var repository: AlarmRepositoryImpl
    private lateinit var scheduler: AlarmSchedulerImpl
    private lateinit var notificationManager: AlarmNotificationManager

    private var currentAlarm: Alarm? = null
    private var isEditMode = false
    private var currentSoundUri = ""

    private val soundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let { uri ->
                currentSoundUri = uri.toString()
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
        etLabel = findViewById(R.id.etLabel)
        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)
        amPmSpinner = findViewById(R.id.amPmSpinner)
        etAlarmNote = findViewById(R.id.etAlarmNote)
        tvPreview = findViewById(R.id.tvPreview)

        // Day checkboxes - ORDER MATCHES LAYOUT
        cbMonday = findViewById(R.id.cbMonday)
        cbTuesday = findViewById(R.id.cbTuesday)
        cbWednesday = findViewById(R.id.cbWednesday)
        cbThursday = findViewById(R.id.cbThursday)
        cbFriday = findViewById(R.id.cbFriday)
        cbSaturday = findViewById(R.id.cbSaturday)
        cbSunday = findViewById(R.id.cbSunday)

        // Switches
        switchSnooze = findViewById(R.id.switchSnooze)
        switchVibration = findViewById(R.id.switchVibration)
        switchSound = findViewById(R.id.switchSound)
        switchSilentMode = findViewById(R.id.switchSilentMode)
    }

    private fun setupViews() {
        setupTimePickers()
        setupClickListeners()
        setDefaultValues()
    }

    private fun setupTimePickers() {
        // Hour picker (1-12)
        hourPicker.apply {
            minValue = 1
            maxValue = 12
        }

        // Minute picker (0-59)
        minutePicker.apply {
            minValue = 0
            maxValue = 59
        }

        // AM/PM Spinner
        val amPmAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf("AM", "PM"))
        amPmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        amPmSpinner.adapter = amPmAdapter
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            saveAlarm()
        }

        tvPreview.setOnClickListener {
            playPreviewSound()
        }
    }

    private fun setDefaultValues() {
        if (!isEditMode) {
            val currentTime = java.util.Calendar.getInstance()
            val hour = currentTime.get(java.util.Calendar.HOUR)
            val minute = currentTime.get(java.util.Calendar.MINUTE)
            val amPm = if (currentTime.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "AM" else "PM"

            hourPicker.value = if (hour == 0) 12 else hour
            minutePicker.value = minute
            amPmSpinner.setSelection(if (amPm == "AM") 0 else 1)

            // Default switches
            switchSnooze.isChecked = true
            switchVibration.isChecked = true
            switchSound.isChecked = true
            switchSilentMode.isChecked = false
        }
    }

    private fun loadAlarmIfEdit() {
        intent.getParcelableExtra<Alarm>(EXTRA_ALARM)?.let { alarm ->
            isEditMode = true
            currentAlarm = alarm

            hourPicker.value = alarm.hour
            minutePicker.value = alarm.minute
            amPmSpinner.setSelection(if (alarm.amPm == "AM") 0 else 1)

            etLabel.setText(alarm.label)
            etAlarmNote.setText(alarm.note)

            // Load active days - FIXED MAPPING (Layout order: Mon-Sun, Domain: Sun-Sat)
            val layoutCheckboxes = listOf(cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday)
            layoutCheckboxes.forEachIndexed { layoutIndex, checkbox ->
                val domainIndex = toDomainIndex(layoutIndex)
                checkbox.isChecked = alarm.activeDays[domainIndex]
            }

            switchSnooze.isChecked = alarm.isSnoozeEnabled
            switchVibration.isChecked = alarm.isVibrationEnabled
            switchSound.isChecked = alarm.isSoundEnabled
            switchSilentMode.isChecked = alarm.isSilentModeEnabled

            currentSoundUri = alarm.soundUri

            title = "Edit Alarm"
        } ?: run {
            title = "Set Alarm"
        }
    }

    private fun saveAlarm() {
        lifecycleScope.launch {
            try {
                val hour = hourPicker.value
                val minute = minutePicker.value
                val amPm = amPmSpinner.selectedItem.toString()
                val label = etLabel.text.toString().trim().ifEmpty { "Alarm" }
                val note = etAlarmNote.text.toString().trim()

                // Get active days - FIXED MAPPING
                val activeDays = BooleanArray(7) { false }
                val layoutCheckboxes = listOf(cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday)
                layoutCheckboxes.forEachIndexed { layoutIndex, checkbox ->
                    if (checkbox.isChecked) {
                        val domainIndex = toDomainIndex(layoutIndex)
                        activeDays[domainIndex] = true
                    }
                }

                val alarm = Alarm(
                    id = if (isEditMode) currentAlarm?.id ?: 0 else 0,
                    hour = hour,
                    minute = minute,
                    amPm = amPm,
                    label = label,
                    activeDays = activeDays,
                    isEnabled = true,
                    isSnoozeEnabled = switchSnooze.isChecked,
                    isVibrationEnabled = switchVibration.isChecked,
                    isSoundEnabled = switchSound.isChecked,
                    isSilentModeEnabled = switchSilentMode.isChecked,
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
        try {
            val soundUri = if (currentSoundUri.isNotEmpty()) {
                Uri.parse(currentSoundUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
            val ringtone = RingtoneManager.getRingtone(this, soundUri)
            ringtone?.play()

            // Stop after 3 seconds
            android.os.Handler(mainLooper).postDelayed({
                try {
                    ringtone?.stop()
                } catch (e: Exception) {
                    // Ignore
                }
            }, 3000)

        } catch (e: Exception) {
            Toast.makeText(this, "Could not play preview", Toast.LENGTH_SHORT).show()
        }
    }

    // Fixed day mapping: Layout index (Mon=0...Sun=6) to Domain index (Sun=0...Sat=6)
    private fun toDomainIndex(layoutIndex: Int): Int = (layoutIndex + 1) % 7

    companion object {
        const val EXTRA_ALARM = "extra_alarm"
        const val RESULT_DELETED = 100
    }
}