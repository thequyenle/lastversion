package net.android.lastversion.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CircleCountdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Giá trị tiến trình (từ 0.0 đến 1.0)
    private var progress = 1f

    // Paint cho vòng tiến trình
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#76E0C1") // Màu xanh chính
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
    }

    // Paint cho vòng nền
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D8F6F0") // Màu nền nhạt
        style = Paint.Style.STROKE
        strokeWidth = 20f
    }

    // Hàm cập nhật tiến trình (0.0 – 1.0)
    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate() // Vẽ lại
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = 20f
        val size = width.coerceAtMost(height).toFloat() - padding * 2

        // Vẽ trong một hình vuông chính giữa
        val rect = RectF(
            (width - size) / 2,
            (height - size) / 2,
            (width + size) / 2,
            (height + size) / 2
        )

        // Vẽ vòng nền (360°)
        canvas.drawArc(rect, -90f, 360f, false, backgroundPaint)

        // Vẽ vòng tiến trình (theo phần trăm progress)
        canvas.drawArc(rect, -90f, 360f * progress, false, ringPaint)
    }
}