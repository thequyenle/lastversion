package net.android.lastversion.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.willy.ratingbar.ScaleRatingBar
import net.android.lastversion.R
import net.android.lastversion.utils.hideNavigationBar
import net.android.lastversion.utils.setOnClickListenerWithDebounce
import net.android.lastversion.utils.showWithHiddenNavigation

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
        hideNavigationBar() // ẩn lại khi dialog được show lại

        // Only reset UI elements that should be reset, not the rating
        // The rating should remain as selected by the user
        resetUIElementsOnly()
    }

    private fun resetToInitialState() {
        // Reset rating
        selectedRating = 0
        ratingBar.rating = 0f

        // Reset icon và text về mặc định
        imvAvtRate.setImageResource(R.drawable.ic_ask)
        tv1.text = context.getString(R.string.do_you_like_the_app)
        tv2.text = context.getString(R.string.let_us_know_your_experience)

        // Disable button vote and apply blur effect
        btnVote.isEnabled = false
        btnVote.setBackgroundResource(R.drawable.btn_rate_background_disabled)
    }

    private fun resetUIElementsOnly() {
        // Only reset UI elements that should be reset when dialog is shown again
        // Do NOT reset the rating - keep user's selection

        // Update UI based on current rating (if any)
        if (selectedRating > 0) {
            updateUIForRating(selectedRating)
        } else {
            // If no rating selected, show default state
            imvAvtRate.setImageResource(R.drawable.ic_ask)
            tv1.text = context.getString(R.string.do_you_like_the_app)
            tv2.text = context.getString(R.string.let_us_know_your_experience)
            btnVote.isEnabled = false
            btnVote.setBackgroundResource(R.drawable.btn_rate_background_disabled)
        }
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
        tv1.text = context.getString(R.string.do_you_like_the_app)
        tv2.text = context.getString(R.string.let_us_know_your_experience)

        // Disable button vote ban đầu và áp dụng blur
        btnVote.isEnabled = false
        btnVote.setBackgroundResource(R.drawable.btn_rate_background_disabled)

        // Reset rating bar về 0 (tất cả sao empty)
        ratingBar.rating = 0f
        selectedRating = 0
    }

    private fun setupListeners() {
        // Set up touch listener to intercept clicks on already selected stars
        ratingBar.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN && selectedRating > 0) {
                // Calculate which star was clicked based on touch position
                val starWidth = ratingBar.width / 5f // Assuming 5 stars
                val clickedStar = ((event.x / starWidth) + 1).toInt().coerceIn(1, 5)

                android.util.Log.d("RatingDialog", "Touch detected on star: $clickedStar, current rating: $selectedRating")

                // Prevent clicking on the exact star that's already selected
                if (clickedStar == selectedRating) {
                    android.util.Log.d("RatingDialog", "Blocking click on already selected star: $selectedRating")
                    return@setOnTouchListener true // Consume the touch event
                }
            }
            false // Let the rating bar handle other touches
        }

        // Rating bar change listener
        ratingBar.setOnRatingChangeListener { _, rating, fromUser ->
            if (fromUser) {
                val newRating = rating.toInt()

                android.util.Log.d("RatingDialog", "Rating change: current=$selectedRating, new=$newRating")

                selectedRating = newRating

                // Always ensure button state matches rating
                if (selectedRating == 0) {
                    // Reset về trạng thái ban đầu khi rating = 0
                    resetToInitialState()
                } else {
                    updateUIForRating(selectedRating)
                }

                // Debug log to track rating changes
                android.util.Log.d("RatingDialog", "Rating changed: $selectedRating, Button enabled: ${btnVote.isEnabled}")
            }
        }

        btnVote.setOnClickListenerWithDebounce {
            // Double-check rating from rating bar to ensure accuracy
            val currentRating = ratingBar.rating.toInt()

            if (currentRating <= 0) {
                Toast.makeText(
                    context,
                    context.getString(R.string.please_select_a_rating_first),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListenerWithDebounce
            }

            // Proceed when rating > 0
            onRatingSubmitted?.invoke(currentRating)
            dismiss()
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
                tv1.text = context.getString(R.string.oh_no)
                tv2.text = context.getString(R.string.please_give_us_some_feedback)
            }
            2 -> {
                imvAvtRate.setImageResource(R.drawable.ic_2star)
                tv1.text = context.getString(R.string.oh_no)
                tv2.text = context.getString(R.string.please_give_us_some_feedback)
            }
            3 -> {
                imvAvtRate.setImageResource(R.drawable.ic_3star)
                tv1.text = context.getString(R.string.could_be_better)
                tv2.text = context.getString(R.string.how_can_we_improve)
            }
            4 -> {
                imvAvtRate.setImageResource(R.drawable.ic_4star)
                tv1.text = context.getString(R.string.we_love_you_too)
                tv2.text = context.getString(R.string.thanks_for_your_feedback)
            }
            5 -> {
                imvAvtRate.setImageResource(R.drawable.ic_5star)
                tv1.text = context.getString(R.string.we_love_you_too)
                tv2.text = context.getString(R.string.thanks_for_your_feedback)
            }
        }

        // Enable/disable button vote và áp dụng/xóa blur effect
        btnVote.isEnabled = rating > 0
        if (rating > 0) {
            btnVote.setBackgroundResource(R.drawable.btn_rate_background)
        } else {
            btnVote.setBackgroundResource(R.drawable.btn_rate_background_disabled)
        }

        // Debug log to confirm button state
        android.util.Log.d("RatingDialog", "UI updated for rating $rating, Button enabled: ${btnVote.isEnabled}")
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
            dialog.showWithHiddenNavigation()   // 👈 dùng extension bạn đã viết
            return dialog
        }
    }
}