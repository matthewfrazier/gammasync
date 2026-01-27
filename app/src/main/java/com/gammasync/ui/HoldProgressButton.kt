package com.gammasync.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatButton
import com.gammasync.infra.HapticFeedback
import com.google.android.material.color.MaterialColors

/**
 * Button that requires a continuous hold for [holdDurationMs] to trigger completion.
 * Shows a circular progress ring around the button during the hold.
 */
class HoldProgressButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.buttonStyle
) : AppCompatButton(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_HOLD_DURATION_MS = 1500L
        private const val PROGRESS_STROKE_WIDTH = 8f
        private const val PROGRESS_PADDING = 4f
    }

    var holdDurationMs: Long = DEFAULT_HOLD_DURATION_MS
    var onHoldComplete: (() -> Unit)? = null

    private var progress = 0f
    private var isHolding = false
    private var animator: ValueAnimator? = null
    private var haptics: HapticFeedback? = null

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = PROGRESS_STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = PROGRESS_STROKE_WIDTH
        color = 0x40FFFFFF // Semi-transparent white track (acceptable overlay)
    }

    private val progressRect = RectF()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        haptics = HapticFeedback(context)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = PROGRESS_STROKE_WIDTH / 2 + PROGRESS_PADDING
        progressRect.set(inset, inset, w - inset, h - inset)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startHold()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cancelHold()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun startHold() {
        if (isHolding) return
        isHolding = true
        haptics?.tick()

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = holdDurationMs
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                progress = animation.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (isHolding && progress >= 1f) {
                        haptics?.heavyClick()
                        onHoldComplete?.invoke()
                    }
                }
            })
            start()
        }
    }

    private fun cancelHold() {
        isHolding = false
        animator?.cancel()
        progress = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        // Update progress color to be theme-aware
        progressPaint.color = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, 0xFF26A69A.toInt())

        // Draw track (background ring)
        canvas.drawRoundRect(progressRect, progressRect.height() / 2, progressRect.height() / 2, trackPaint)

        // Draw progress arc
        if (progress > 0f) {
            val sweepAngle = 360f * progress
            canvas.drawArc(progressRect, -90f, sweepAngle, false, progressPaint)
        }

        super.onDraw(canvas)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
