/*
 * MIT License
 * Copyright (c) 2026 matthewfrazier
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gammasync.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Circular pie timer that fills clockwise as time progresses.
 * Shows remaining time as text in the center.
 */
class CircularTimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_SIZE_DP = 120
        private const val STROKE_WIDTH_DP = 4f
    }

    private var totalSeconds = 0
    private var remainingSeconds = 0
    private var progress = 0f // 0.0 = just started, 1.0 = complete

    // Colors
    private val backgroundColor = 0x40000000  // 25% black
    private val trackColor = 0x40FFFFFF       // 25% white
    private var progressColor = 0xFF26A69A.toInt()  // Teal (default)
    private val textColor = 0xFFFFFFFF.toInt()

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = backgroundColor
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH_DP * resources.displayMetrics.density
        color = trackColor
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = progressColor
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val pieRect = RectF()

    fun setTotalDuration(totalSecs: Int) {
        totalSeconds = totalSecs
        remainingSeconds = totalSecs
        progress = 0f
        invalidate()
    }

    fun setRemainingTime(remainingSecs: Int) {
        remainingSeconds = remainingSecs
        progress = if (totalSeconds > 0) {
            1f - (remainingSecs.toFloat() / totalSeconds)
        } else {
            0f
        }
        invalidate()
    }

    fun setAccentColor(color: Int) {
        progressColor = color
        progressPaint.color = color
        invalidate()
    }

    fun getTimeText(): String {
        val mins = remainingSeconds / 60
        val secs = remainingSeconds % 60
        return String.format("%d:%02d", mins, secs)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSize = (DEFAULT_SIZE_DP * resources.displayMetrics.density).toInt()

        val width = resolveSize(defaultSize, widthMeasureSpec)
        val height = resolveSize(defaultSize, heightMeasureSpec)
        val size = minOf(width, height)

        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = trackPaint.strokeWidth / 2
        pieRect.set(padding, padding, w - padding, h - padding)

        // Scale text size to ~25% of view size for legibility
        val size = minOf(w, h)
        textPaint.textSize = size * 0.25f
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) - trackPaint.strokeWidth / 2

        // Draw circular background (semi-transparent)
        canvas.drawCircle(cx, cy, radius, backgroundPaint)

        // Draw progress pie (fills clockwise from top)
        if (progress > 0f) {
            val sweepAngle = 360f * progress
            canvas.drawArc(pieRect, -90f, sweepAngle, true, progressPaint)
        }

        // Draw track ring
        canvas.drawCircle(cx, cy, radius, trackPaint)

        // Draw time text centered
        val timeText = getTimeText()
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(timeText, cx, textY, textPaint)
    }
}
