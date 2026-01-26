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
import android.graphics.Color
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View

/**
 * Subtle dark flicker overlay for phone display.
 * Renders semi-transparent black that pulses with the 40Hz audio phase.
 * Provides visual feedback that therapy is active without full-screen color.
 */
class SubtleFlickerOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), Choreographer.FrameCallback {

    companion object {
        // Alpha range for subtle flicker (0-255)
        private const val MIN_ALPHA = 0      // Fully transparent at peak
        private const val MAX_ALPHA = 40     // ~15% opacity at trough
    }

    private val choreographer = Choreographer.getInstance()
    private var phaseProvider: (() -> Double)? = null

    @Volatile
    private var isAnimating = false

    fun setPhaseProvider(provider: () -> Double) {
        phaseProvider = provider
    }

    fun start() {
        if (isAnimating) return
        isAnimating = true
        choreographer.postFrameCallback(this)
    }

    fun stop() {
        isAnimating = false
        choreographer.removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isAnimating) return
        invalidate()
        choreographer.postFrameCallback(this)
    }

    override fun onDraw(canvas: Canvas) {
        val phase = phaseProvider?.invoke() ?: 0.0

        // Triangle wave: 0→1→0 over the phase cycle
        val t = if (phase < 0.5) phase * 2 else (1.0 - phase) * 2

        // Interpolate alpha: more transparent at peak (t=1), darker at trough (t=0)
        val alpha = (MAX_ALPHA - (MAX_ALPHA - MIN_ALPHA) * t).toInt()

        canvas.drawColor(Color.argb(alpha, 0, 0, 0))
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }
}
