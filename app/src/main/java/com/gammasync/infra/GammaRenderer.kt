package com.cognihertz.infra

import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.cognihertz.domain.ColorTemperature

/**
 * 120Hz visual renderer synchronized to audio phase.
 * Uses Choreographer for frame-perfect timing.
 */
class GammaRenderer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Choreographer.FrameCallback {

    companion object {
        private const val TAG = "GammaRenderer"
        private const val TARGET_REFRESH_RATE = 120f
    }

    private val choreographer = Choreographer.getInstance()

    @Volatile
    private var isRendering = false
    private var phaseProvider: (() -> Double)? = null

    // Frame timing diagnostics
    private var lastFrameTimeNanos = 0L
    private var frameCount = 0L
    private val frameTimes = LongArray(120)
    private var frameIndex = 0

    init {
        holder.addCallback(this)
    }

    /**
     * Set the phase provider function.
     * Called each frame to get current audio phase (0.0-1.0).
     */
    fun setPhaseProvider(provider: () -> Double) {
        phaseProvider = provider
    }

    /**
     * Start rendering synchronized to audio phase.
     */
    fun start() {
        if (isRendering) return
        isRendering = true
        frameCount = 0
        lastFrameTimeNanos = 0
        Log.i(TAG, "Starting 120Hz rendering")
        choreographer.postFrameCallback(this)
    }

    /**
     * Stop rendering.
     */
    fun stop() {
        isRendering = false
        choreographer.removeFrameCallback(this)
        Log.i(TAG, "Stopped rendering after $frameCount frames")
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isRendering) return

        // Calculate frame timing for diagnostics
        if (lastFrameTimeNanos > 0) {
            val frameTimeMs = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000.0
            frameTimes[frameIndex % frameTimes.size] = (frameTimeMs * 1000).toLong()
            frameIndex++
        }
        lastFrameTimeNanos = frameTimeNanos

        // Get phase from audio engine
        val phase = phaseProvider?.invoke() ?: 0.0

        // Render
        renderFrame(phase)
        frameCount++

        // Schedule next frame
        choreographer.postFrameCallback(this)
    }

    private fun renderFrame(phase: Double) {
        val canvas: Canvas
        try {
            canvas = holder.lockCanvas() ?: return
        } catch (e: Exception) {
            return
        }

        try {
            val color = ColorTemperature.interpolate(phase)
            canvas.drawColor(color)
        } finally {
            try {
                holder.unlockCanvasAndPost(canvas)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to post canvas: ${e.message}")
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i(TAG, "Surface created")

        // Request 120Hz refresh rate (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                holder.surface.setFrameRate(
                    TARGET_REFRESH_RATE,
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                    Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS
                )
                Log.i(TAG, "Requested ${TARGET_REFRESH_RATE}Hz refresh rate")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set frame rate: ${e.message}")
            }
        }

        // Draw initial warm color
        renderFrame(0.0)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.i(TAG, "Surface changed: ${width}x${height}")
        // Redraw at new size if not actively rendering
        if (!isRendering) {
            renderFrame(phaseProvider?.invoke() ?: 0.0)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i(TAG, "Surface destroyed")
        stop()
    }

    /**
     * Get P99 frame time in milliseconds.
     */
    fun getP99FrameTimeMs(): Double {
        if (frameIndex < 10) return 0.0
        val count = minOf(frameIndex, frameTimes.size)
        val sorted = frameTimes.take(count).sorted()
        val p99Index = (sorted.size * 0.99).toInt().coerceIn(0, sorted.size - 1)
        return sorted[p99Index] / 1000.0
    }
}
