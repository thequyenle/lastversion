package net.android.lastversion.canvas

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View

class CircleCountdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var progress = 1f

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#76E0C1")
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D8F6F0")
        style = Paint.Style.STROKE
        strokeWidth = 20f
    }

    fun setProgress(value: Float) {
      //  progress = Math.random().toFloat()
        progress = value.coerceIn(0f, 1f)
        Log.d("CircleCountdown", "setProgress = $progress")
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = 20f
        val size = width.coerceAtMost(height).toFloat() - padding * 2
        val rect = RectF(
            (width - size) / 2,
            (height - size) / 2,
            (width + size) / 2,
            (height + size) / 2
        )
        canvas.drawArc(rect, -90f, 360f, false, backgroundPaint)
        canvas.drawArc(rect, -90f, 360f * progress, false, ringPaint)
    }
}
