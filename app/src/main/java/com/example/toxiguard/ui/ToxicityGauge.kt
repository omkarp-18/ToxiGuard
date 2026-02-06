package com.example.toxiguard.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import androidx.core.content.withStyledAttributes
import com.example.toxiguard.R

class ToxicityGauge @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var progress = 0f
    private var progressColor = Color.MAGENTA
    private var gaugeColor = Color.LTGRAY
    private var textColor = Color.WHITE
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        context.withStyledAttributes(attrs, R.styleable.ToxicityGauge) {
            progressColor = getColor(R.styleable.ToxicityGauge_progressColor, progressColor)
            gaugeColor = getColor(R.styleable.ToxicityGauge_gaugeColor, gaugeColor)
            textColor = getColor(R.styleable.ToxicityGauge_textColor, textColor)
        }
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 64f
        textPaint.color = textColor
    }

    fun setProgress(value: Float) {
        val animator = ValueAnimator.ofFloat(progress, value)
        animator.duration = 1000
        animator.addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = width / 2.5f
        val cx = width / 2f
        val cy = height / 2f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 24f
        paint.color = gaugeColor
        canvas.drawCircle(cx, cy, radius, paint)
        paint.color = progressColor
        val sweep = 360 * (progress / 100f)
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(rect, -90f, sweep, false, paint)
        canvas.drawText("${progress.toInt()}%", cx, cy + 20f, textPaint)
    }
}
