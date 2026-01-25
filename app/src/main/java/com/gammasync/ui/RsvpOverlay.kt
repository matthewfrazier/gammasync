package com.gammasync.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.View

/**
 * RSVP (Rapid Serial Visual Presentation) text overlay.
 *
 * Displays text word-by-word at configurable WPM, overlaid on top of any
 * therapy mode. Useful for reading while receiving entrainment.
 *
 * Features:
 * - Configurable words-per-minute (default 300 WPM)
 * - Large centered text with drop shadow for visibility
 * - Works on both phone and XREAL displays
 * - Optional phase-locked word transitions (synced to therapy frequency)
 */
class RsvpOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), Choreographer.FrameCallback {

    companion object {
        private const val TAG = "RsvpOverlay"
        private const val DEFAULT_WPM = 300
        private const val MIN_WPM = 100
        private const val MAX_WPM = 1000
    }

    // Text rendering
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    // RSVP state
    private var words: List<String> = emptyList()
    private var currentWordIndex = 0
    private var wordsPerMinute = DEFAULT_WPM
    private var msPerWord: Long = 60_000L / DEFAULT_WPM

    // Timing
    private val choreographer = Choreographer.getInstance()
    private var lastWordTimeNs = 0L
    private var isRunning = false

    // Phase-locked mode (optional)
    private var phaseLocked = false
    private var phaseProvider: (() -> Double)? = null
    private var lastPhaseHigh = false

    // Callbacks
    var onTextComplete: (() -> Unit)? = null

    /**
     * Set the text to display, split into words.
     */
    fun setText(text: String) {
        words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        currentWordIndex = 0
        Log.i(TAG, "Set text: ${words.size} words")
        invalidate()
    }

    /**
     * Set words per minute (100-1000).
     */
    fun setWpm(wpm: Int) {
        wordsPerMinute = wpm.coerceIn(MIN_WPM, MAX_WPM)
        msPerWord = 60_000L / wordsPerMinute
        Log.i(TAG, "Set WPM: $wordsPerMinute (${msPerWord}ms/word)")
    }

    /**
     * Enable phase-locked word transitions.
     * Words change at the peak of each therapy cycle.
     */
    fun enablePhaseLock(provider: () -> Double) {
        phaseProvider = provider
        phaseLocked = true
    }

    /**
     * Disable phase-locked mode (use fixed timing).
     */
    fun disablePhaseLock() {
        phaseLocked = false
        phaseProvider = null
    }

    /**
     * Start RSVP playback.
     */
    fun start() {
        if (isRunning) return
        if (words.isEmpty()) return

        isRunning = true
        currentWordIndex = 0
        lastWordTimeNs = System.nanoTime()
        lastPhaseHigh = false
        Log.i(TAG, "Starting RSVP at $wordsPerMinute WPM")
        choreographer.postFrameCallback(this)
        invalidate()
    }

    /**
     * Stop RSVP playback.
     */
    fun stop() {
        isRunning = false
        choreographer.removeFrameCallback(this)
        Log.i(TAG, "Stopped RSVP at word $currentWordIndex/${words.size}")
    }

    /**
     * Reset to beginning.
     */
    fun reset() {
        currentWordIndex = 0
        invalidate()
    }

    /**
     * Whether playback is currently running.
     */
    val playing: Boolean
        get() = isRunning

    /**
     * Current word being displayed.
     */
    val currentWord: String
        get() = words.getOrNull(currentWordIndex) ?: ""

    /**
     * Progress through text (0.0-1.0).
     */
    val progress: Float
        get() = if (words.isEmpty()) 0f else currentWordIndex.toFloat() / words.size

    override fun doFrame(frameTimeNanos: Long) {
        if (!isRunning) return

        val shouldAdvance = if (phaseLocked && phaseProvider != null) {
            // Phase-locked: advance on rising edge of phase
            val phase = phaseProvider?.invoke() ?: 0.0
            val isHigh = phase > 0.5
            val wasLow = !lastPhaseHigh
            lastPhaseHigh = isHigh
            isHigh && wasLow
        } else {
            // Time-based: advance after msPerWord
            val elapsedNs = frameTimeNanos - lastWordTimeNs
            val elapsedMs = elapsedNs / 1_000_000
            elapsedMs >= msPerWord
        }

        if (shouldAdvance) {
            currentWordIndex++
            lastWordTimeNs = frameTimeNanos

            if (currentWordIndex >= words.size) {
                // Text complete
                isRunning = false
                onTextComplete?.invoke()
                Log.i(TAG, "RSVP complete")
                return
            }

            invalidate()
        }

        choreographer.postFrameCallback(this)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (words.isEmpty()) return

        val word = words.getOrNull(currentWordIndex) ?: return

        // Calculate text size (15% of view height, min 48sp)
        val textSize = (height * 0.15f).coerceAtLeast(48f * resources.displayMetrics.scaledDensity)
        textPaint.textSize = textSize
        shadowPaint.textSize = textSize

        // Center text vertically
        val x = width / 2f
        val y = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2

        // Draw shadow (offset by 2dp)
        val shadowOffset = 2f * resources.displayMetrics.density
        canvas.drawText(word, x + shadowOffset, y + shadowOffset, shadowPaint)

        // Draw text
        canvas.drawText(word, x, y, textPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }
}
