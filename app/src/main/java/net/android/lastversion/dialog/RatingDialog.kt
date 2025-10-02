package net.android.lastversion.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import net.android.lastversion.R

class RatingDialog(context: Context) : Dialog(context) {

    private var selectedRating = 0
    private val stars = mutableListOf<ImageView>()

    private lateinit var tvEmoji: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var btnRate: TextView
    private lateinit var btnExit: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_rating)

        // Làm trong suốt background
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        initViews()
        setupInitialScreen()
        setupClickListeners()
    }

    private fun initViews() {
        tvEmoji = findViewById(R.id.tvEmoji)
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        btnRate = findViewById(R.id.btnRate)
        btnExit = findViewById(R.id.btnExit)

        // Lấy các star views
        stars.add(findViewById(R.id.star1))
        stars.add(findViewById(R.id.star2))
        stars.add(findViewById(R.id.star3))
        stars.add(findViewById(R.id.star4))
        stars.add(findViewById(R.id.star5))
    }

    private fun setupInitialScreen() {
        tvEmoji.text = "😊"
        tvTitle.text = "Do you like the app?"
        tvSubtitle.text = "Let us know your experience"
        btnRate.visibility = android.view.View.VISIBLE
    }

    private fun setupClickListeners() {
        // Click listeners cho stars - Hiển thị feedback ngay nhưng giữ nút Rate
        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                selectedRating = index + 1
                updateStars(selectedRating)
                // Hiển thị feedback ngay khi click star
                showFeedbackScreen(selectedRating, keepRateButton = true)
            }
        }

        // Click listener cho Rate button - Submit và ẩn nút Rate
        btnRate.setOnClickListener {
            if (selectedRating > 0) {
                // Ẩn nút Rate khi user xác nhận
                btnRate.visibility = android.view.View.GONE
                // TODO: Gửi rating lên server hoặc lưu vào SharedPreferences
            }
        }

        // Click listener cho Exit button
        btnExit.setOnClickListener {
            dismiss()
        }
    }

    private fun updateStars(rating: Int) {
        stars.forEachIndexed { index, star ->
            if (index < rating) {
                star.setImageResource(R.drawable.ic_star_filled)
            } else {
                star.setImageResource(R.drawable.ic_star_empty)
            }
        }
    }

    private fun showFeedbackScreen(rating: Int, keepRateButton: Boolean = false) {
        when (rating) {
            1 -> {
                tvEmoji.text = "😭"
                tvTitle.text = "Oh, no!"
                tvSubtitle.text = "Please give us some feedback"
            }
            2 -> {
                tvEmoji.text = "😥"
                tvTitle.text = "Oh, no!"
                tvSubtitle.text = "Please give us some feedback"
            }
            3 -> {
                tvEmoji.text = "☹️"
                tvTitle.text = "Oh, no!"
                tvSubtitle.text = "Please give us some feedback"
            }
            4 -> {
                tvEmoji.text = "😌"
                tvTitle.text = "We love you too!"
                tvSubtitle.text = "Thanks for your feedback"
            }
            5 -> {
                tvEmoji.text = "😍"
                tvTitle.text = "We love you too!"
                tvSubtitle.text = "Thanks for your feedback"
            }
        }

        // Giữ nguyên nút Rate nếu keepRateButton = true
        if (!keepRateButton) {
            btnRate.visibility = android.view.View.GONE
        }
    }
}