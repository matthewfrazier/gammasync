package com.gammasync.infra

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.gammasync.domain.ColorTemperature
import com.gammasync.domain.therapy.TherapyProfile
import com.gammasync.domain.therapy.VisualConfig
import com.gammasync.domain.therapy.VisualMode
import kotlin.random.Random

/**
 * Universal visual renderer supporting multiple therapy visual modes.
 *
 * Extends GammaRenderer capabilities with:
 * - SINE: Smooth warm↔cool interpolation (original behavior)
 * - STROBE: Sharp on/off at 50% duty cycle
 * - STATIC: Single color, no animation
 * - SPLIT: Left/right eye independent (XREAL 3840x1080)
 * - Luminance noise to prevent habituation
 *
 * Uses Choreographer for frame-perfect timing at 120Hz.
 */
class UniversalVisualRenderer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Choreographer.FrameCallback {

    companion object {
        private const val TAG = "UniversalVisualRenderer"
        private const val TARGET_REFRESH_RATE = 120f
        private const val LUMINANCE_NOISE_AMPLITUDE = 0.1f  // ±10% brightness jitter
    }

    private val choreographer = Choreographer.getInstance()
    private val paint = Paint()
    private val random = Random.Default

    @Volatile
    private var isRendering = false

    // Phase providers
    private var phaseProvider: (() -> Double)? = null
    private var secondaryPhaseProvider: (() -> Double)? = null

    // Current visual configuration
    private var visualMode: VisualMode = VisualMode.SINE
    private var visualConfig: VisualConfig = VisualConfig.ISOLUMINANT
    private var luminanceNoiseEnabled = false

    // Frame timing diagnostics
    private var lastFrameTimeNanos = 0L
    private var frameCount = 0L
    private val frameTimes = LongArray(120)
    private var frameIndex = 0

    init {
        holder.addCallback(this)
    }

    /**
     * Configure the renderer with a therapy profile.
     */
    fun configure(profile: TherapyProfile) {
        visualMode = profile.visualMode
        visualConfig = profile.visualConfig
        luminanceNoiseEnabled = profile.visualConfig.luminanceNoise
        Log.i(TAG, "Configured for ${profile.mode.displayName}: $visualMode, noise=$luminanceNoiseEnabled")
    }

    /**
     * Set the primary phase provider function.
     * Called each frame to get current audio phase (0.0-1.0).
     */
    fun setPhaseProvider(provider: () -> Double) {
        phaseProvider = provider
    }

    /**
     * Set the secondary phase provider for split/dual modes.
     */
    fun setSecondaryPhaseProvider(provider: () -> Double) {
        secondaryPhaseProvider = provider
    }

    /**
     * Start rendering synchronized to audio phase.
     */
    fun start() {
        if (isRendering) return
        isRendering = true
        frameCount = 0
        lastFrameTimeNanos = 0
        Log.i(TAG, "Starting 120Hz rendering, mode=$visualMode")
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
        val primaryPhase = phaseProvider?.invoke() ?: 0.0
        val secondaryPhase = secondaryPhaseProvider?.invoke() ?: primaryPhase

        // Render based on current mode
        renderFrame(primaryPhase, secondaryPhase)
        frameCount++

        // Schedule next frame
        choreographer.postFrameCallback(this)
    }

    private fun renderFrame(primaryPhase: Double, secondaryPhase: Double) {
        val canvas: Canvas
        try {
            canvas = holder.lockCanvas() ?: return
        } catch (e: Exception) {
            return
        }

        try {
            when (visualMode) {
                VisualMode.SINE -> renderSine(canvas, primaryPhase)
                VisualMode.STROBE -> renderStrobe(canvas, primaryPhase)
                VisualMode.STATIC -> renderStatic(canvas)
                VisualMode.SPLIT -> renderSplit(canvas, primaryPhase, secondaryPhase)
            }
        } finally {
            try {
                holder.unlockCanvasAndPost(canvas)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to post canvas: ${e.message}")
            }
        }
    }

    /**
     * SINE mode: Smooth interpolation between warm and cool colors.
     * Creates triangle wave oscillation for natural-looking transitions.
     */
    private fun renderSine(canvas: Canvas, phase: Double) {
        val color = interpolateColors(phase)
        val finalColor = applyLuminanceNoise(color)
        canvas.drawColor(finalColor)
    }

    /**
     * STROBE mode: Sharp on/off transitions.
     * First half of cycle = primary color, second half = secondary color (black).
     */
    private fun renderStrobe(canvas: Canvas, phase: Double) {
        val color = if (phase < 0.5) {
            visualConfig.primaryColor
        } else {
            visualConfig.secondaryColor
        }
        val finalColor = applyLuminanceNoise(color)
        canvas.drawColor(finalColor)
    }

    /**
     * STATIC mode: Single fixed color, no animation.
     * Used for migraine relief (no flicker).
     */
    private fun renderStatic(canvas: Canvas) {
        canvas.drawColor(visualConfig.primaryColor)
    }

    /**
     * SPLIT mode: Independent colors for left/right halves.
     * Used for XREAL glasses (3840x1080 = 1920 per eye).
     */
    private fun renderSplit(canvas: Canvas, leftPhase: Double, rightPhase: Double) {
        val width = canvas.width
        val height = canvas.height
        val splitX = width / 2

        // Left eye
        val leftColor = applyLuminanceNoise(interpolateColors(leftPhase))
        canvas.save()
        canvas.clipRect(0, 0, splitX, height)
        canvas.drawColor(leftColor)
        canvas.restore()

        // Right eye
        val rightColor = applyLuminanceNoise(interpolateColors(rightPhase))
        canvas.save()
        canvas.clipRect(splitX, 0, width, height)
        canvas.drawColor(rightColor)
        canvas.restore()
    }

    /**
     * Interpolate between primary and secondary colors based on phase.
     * Creates smooth triangle wave (0→1→0) for natural transitions.
     */
    private fun interpolateColors(phase: Double): Int {
        // Convert to 0.0→1.0→0.0 triangle wave
        val t = if (phase < 0.5) phase * 2 else (1.0 - phase) * 2

        val primary = visualConfig.primaryColor
        val secondary = visualConfig.secondaryColor

        val r = lerp(Color.red(primary), Color.red(secondary), t)
        val g = lerp(Color.green(primary), Color.green(secondary), t)
        val b = lerp(Color.blue(primary), Color.blue(secondary), t)

        return Color.rgb(r, g, b)
    }

    private fun lerp(a: Int, b: Int, t: Double): Int {
        return (a + (b - a) * t).toInt().coerceIn(0, 255)
    }

    /**
     * Apply luminance noise to prevent visual habituation.
     * Adds random ±10% brightness jitter per frame.
     */
    private fun applyLuminanceNoise(color: Int): Int {
        if (!luminanceNoiseEnabled) return color

        // Random factor between 0.9 and 1.1
        val noiseFactor = 1.0f + (random.nextFloat() * 2 - 1) * LUMINANCE_NOISE_AMPLITUDE

        val r = (Color.red(color) * noiseFactor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * noiseFactor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * noiseFactor).toInt().coerceIn(0, 255)

        return Color.rgb(r, g, b)
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

        // Draw initial frame
        renderFrame(0.0, 0.0)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.i(TAG, "Surface changed: ${width}x${height}")
        if (!isRendering) {
            renderFrame(phaseProvider?.invoke() ?: 0.0, secondaryPhaseProvider?.invoke() ?: 0.0)
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
