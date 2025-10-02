package net.android.lastversion.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.Window
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
    private lateinit var layoutStars: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_rating)

        // Làm trong suốt background
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        initViews()
        setupInitialScreen()
        setupClickListeners()
        setupTouchListener()
    }

    private fun initViews() {
        tvEmoji = findViewById(R.id.tvEmoji)
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        btnRate = findViewById(R.id.btnRate)
        btnExit = findViewById(R.id.btnExit)
        layoutStars = findViewById(R.id.layoutStars)

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
        // Set initial state với ic_ask
        updateStars(0)
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

    private fun setupTouchListener() {
        layoutStars.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE -> {
                    // Lấy vị trí touch tương đối với layoutStars
                    val x = event.x

                    // Tính toán star nào được chạm dựa trên vị trí X
                    val starWidth = if (stars.isNotEmpty()) stars[0].width else 0
                    val starMargin = 4 // margin giữa các sao (dp converted to px nếu cần)

                    var newRating = 0
                    var currentX = 0f

                    stars.forEachIndexed { index, star ->
                        val starEndX = currentX + starWidth
                        if (x >= currentX && x < starEndX) {
                            newRating = index + 1
                            return@forEachIndexed
                        }
                        currentX = starEndX + starMargin
                    }

                    // Nếu touch nằm ngoài phạm vi stars thì giữ nguyên rating
                    if (newRating > 0 && selectedRating != newRating) {
                        selectedRating = newRating
                        updateStars(selectedRating)
                        showFeedbackScreen(selectedRating, keepRateButton = true)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Khi nhả tay, giữ nguyên rating đã chọn
                    true
                }
                else -> false
            }
        }
    }

    private fun updateStars(rating: Int) {
        stars.forEachIndexed { index, star ->
            if (rating == 0) {
                // Trạng thái ban đầu - chưa chạm vào
                star.setImageResource(R.drawable.ic_ask)
            } else if (index < rating) {
                // Sao đã được chọn - hiển thị theo số sao tương ứng
                val iconRes = when (rating) {
                    1 -> R.drawable.ic_1star
                    2 -> R.drawable.ic_2star
                    3 -> R.drawable.ic_3star
                    4 -> R.drawable.ic_4star
                    5 -> R.drawable.ic_5star
                    else -> R.drawable.ic_ask
                }
                star.setImageResource(iconRes)
            } else {
                // Sao chưa được chọn - hiển thị ic_ask (sao trống)
                star.setImageResource(R.drawable.ic_ask)
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