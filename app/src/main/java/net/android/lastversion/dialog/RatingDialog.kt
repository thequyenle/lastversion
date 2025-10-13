package net.android.lastversion.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.willy.ratingbar.ScaleRatingBar
import net.android.lastversion.R
import net.android.lastversion.utils.setOnClickListenerWithDebounce

class RatingDialog(
    context: Context,
    private val onRatingSubmitted: ((Int) -> Unit)? = null,
    private val onDismiss: (() -> Unit)? = null
) : Dialog(context) {

    private var selectedRating = 0

    private lateinit var imvAvtRate: AppCompatImageView
    private lateinit var tv1: AppCompatTextView
    private lateinit var tv2: AppCompatTextView
    private lateinit var btnVote: AppCompatButton
    private lateinit var btnCancel: AppCompatTextView
    private lateinit var ratingBar: ScaleRatingBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_rating)

        // Set dialog properties
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        setCancelable(false)
        setCanceledOnTouchOutside(false)

        initViews()
        setupInitialState()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        // Reset về trạng thái ban đầu mỗi khi dialog hiển thị
        resetToInitialState()
    }

    private fun resetToInitialState() {
        // Reset rating
        selectedRating = 0
        ratingBar.rating = 0f

        // Reset icon và text về mặc định
        imvAvtRate.setImageResource(R.drawable.ic_ask)
        tv1.text = "Do you like the app?"
        tv2.text = "Let us know your experience"

        // Disable button vote
        btnVote.isEnabled = false
    }

    private fun initViews() {
        imvAvtRate = findViewById(R.id.imvAvtRate)
        tv1 = findViewById(R.id.tv1)
        tv2 = findViewById(R.id.tv2)
        btnVote = findViewById(R.id.btnVote)
        btnCancel = findViewById(R.id.btnCancel)
        ratingBar = findViewById(R.id.ratingBar)
    }

    private fun setupInitialState() {
        // Set icon mặc định là ic_ask (emoji hỏi)
        imvAvtRate.setImageResource(R.drawable.ic_ask)

        // Set text mặc định
        tv1.text = "Do you like the app?"
        tv2.text = "Let us know your experience"

        // Disable button vote ban đầu
        btnVote.isEnabled = false

        // Reset rating bar về 0 (tất cả sao empty)
        ratingBar.rating = 0f
        selectedRating = 0
    }

    private fun setupListeners() {
        // Rating bar change listener
        ratingBar.setOnRatingChangeListener { _, rating, fromUser ->
            if (fromUser) {
                selectedRating = rating.toInt()
                if (selectedRating == 0) {
                    // Reset về trạng thái ban đầu khi rating = 0
                    resetToInitialState()
                } else {
                    updateUIForRating(selectedRating)
                }
            }
        }

        // Vote button click
        btnVote.setOnClickListenerWithDebounce {
            if (selectedRating > 0) {
                onRatingSubmitted?.invoke(selectedRating)
                dismiss()
            }
        }

        // Cancel button click
        btnCancel.setOnClickListenerWithDebounce {
            dismiss()
        }

        // Set dismiss listener
        setOnDismissListener {
            onDismiss?.invoke()
        }
    }

    private fun updateUIForRating(rating: Int) {
        when (rating) {
            1 -> {
                imvAvtRate.setImageResource(R.drawable.ic_1star)
                tv1.text = "Oh, no!"
                tv2.text = "Please give us some feedback"
            }
            2 -> {
                imvAvtRate.setImageResource(R.drawable.ic_2star)
                tv1.text = "Oh, no!"
                tv2.text = "Please give us some feedback"
            }
            3 -> {
                imvAvtRate.setImageResource(R.drawable.ic_3star)
                tv1.text = "Could be better!"
                tv2.text = "How can we improve?"
            }
            4 -> {
                imvAvtRate.setImageResource(R.drawable.ic_4star)
                tv1.text = "We love you too!"
                tv2.text = "Thanks for your feedback"
            }
            5 -> {
                imvAvtRate.setImageResource(R.drawable.ic_5star)
                tv1.text = "We love you too!"
                tv2.text = "Thanks for your feedback"
            }
        }

        // Enable button vote (không đổi background, không làm mờ)
        btnVote.isEnabled = true
    }

    companion object {
        /**
         * Hiển thị dialog rating
         * @param context Context
         * @param onRatingSubmitted Callback khi user submit rating (1-5)
         * @param onDismiss Callback khi dialog bị đóng
         */
        fun show(
            context: Context,
            onRatingSubmitted: ((Int) -> Unit)? = null,
            onDismiss: (() -> Unit)? = null
        ): RatingDialog {
            val dialog = RatingDialog(context, onRatingSubmitted, onDismiss)
            dialog.show()
            return dialog
        }
    }
}