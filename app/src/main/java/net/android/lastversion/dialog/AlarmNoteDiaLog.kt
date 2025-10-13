package net.android.lastversion.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import net.android.lastversion.R
import net.android.lastversion.utils.hideNavigationBar
import net.android.lastversion.utils.setOnClickListenerWithDebounce

class AlarmNoteDialog(
    context: Context,
    private val currentNote: String = "",
    private val onNoteSet: (String) -> Unit
) : Dialog(context) {

    private lateinit var etAlarmNote: EditText
    private lateinit var btnCancel: TextView
    private lateinit var btnOK: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_alarm_note)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        hideNavigationBar()

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etAlarmNote = findViewById(R.id.etAlarmNote)
        btnCancel = findViewById(R.id.btnCancel)
        btnOK = findViewById(R.id.btnOK)

        // Nếu currentNote rỗng, dùng giá trị mặc định từ strings
        val noteToDisplay = if (currentNote.isEmpty()) {
            context.getString(R.string.wake_up_default)
        } else {
            currentNote
        }

        etAlarmNote.setText(noteToDisplay)
        etAlarmNote.setSelection(noteToDisplay.length)
    }

    private fun setupClickListeners() {
        btnCancel.setOnClickListenerWithDebounce {
            dismiss()
        }

        btnOK.setOnClickListenerWithDebounce {
            val note = etAlarmNote.text.toString().trim()
            onNoteSet(note)
            dismiss()
        }
    }
}