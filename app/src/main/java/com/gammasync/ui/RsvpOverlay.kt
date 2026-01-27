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
import com.gammasync.domain.rsvp.Glimpse
import com.gammasync.domain.rsvp.ProcessedDocument
import com.gammasync.domain.rsvp.RsvpSettings

/**
 * RSVP (Rapid Serial Visual Presentation) text overlay.
 *
 * Displays glimpses (1-3 words grouped) at variable timing, overlaid on top of any
 * therapy mode. Useful for reading while receiving entrainment.
 *
 * Features:
 * - Glimpse-based display with phrase grouping (function words grouped with content)
 * - ORP (Optimal Recognition Point) highlighting for faster recognition
 * - Variable timing based on punctuation and word length
 * - Position tracking for resume functionality
 * - Optional phase-locked transitions (synced to therapy frequency)
 */
class RsvpOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), Choreographer.FrameCallback {

    companion object {
        private const val TAG = "RsvpOverlay"
        private const val DEFAULT_WPM = 300
        private const val MIN_WPM = 60
        private const val MAX_WPM = 2000
        private const val ORP_COLOR = Color.RED
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

    private val orpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ORP_COLOR
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    // RSVP state - glimpse-based
    private var glimpses: List<Glimpse> = emptyList()
    private var currentGlimpseIndex = 0
    private var wordsPerMinute = DEFAULT_WPM
    private var baseMsPerWord: Long = 60_000L / DEFAULT_WPM

    // Settings
    private var settings: RsvpSettings = RsvpSettings.DEFAULT
    private var orpHighlightEnabled = true
    private var textSizePercent = 0.15f

    // Timing
    private val choreographer = Choreographer.getInstance()
    private var lastGlimpseTimeNs = 0L
    private var currentGlimpseDurationMs: Long = 200
    private var isRunning = false
    private var isPaused = false

    // Phase-locked mode (optional)
    private var phaseLocked = false
    private var phaseProvider: (() -> Double)? = null
    private var lastPhaseHigh = false

    // Callbacks
    var onTextComplete: (() -> Unit)? = null
    var onProgressUpdate: ((wordIndex: Int, glimpseIndex: Int, totalWords: Int, totalGlimpses: Int) -> Unit)? = null

    // Legacy support for word-based text
    private var legacyWords: List<String> = emptyList()
    private var useLegacyMode = false

    /**
     * Set a processed document for glimpse-based display.
     */
    fun setDocument(document: ProcessedDocument) {
        glimpses = document.glimpses
        currentGlimpseIndex = 0
        useLegacyMode = false
        Log.i(TAG, "Set document: ${glimpses.size} glimpses, ${document.totalWords} words")
        invalidate()
    }

    /**
     * Set glimpses directly.
     */
    fun setGlimpses(glimpseList: List<Glimpse>) {
        glimpses = glimpseList
        currentGlimpseIndex = 0
        useLegacyMode = false
        Log.i(TAG, "Set glimpses: ${glimpses.size} glimpses")
        invalidate()
    }

    /**
     * Set the text to display (legacy word-by-word mode).
     * For backwards compatibility.
     */
    fun setText(text: String) {
        legacyWords = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        currentGlimpseIndex = 0
        useLegacyMode = true
        Log.i(TAG, "Set text (legacy): ${legacyWords.size} words")
        invalidate()
    }

    /**
     * Apply RSVP display settings.
     */
    fun applySettings(rsvpSettings: RsvpSettings) {
        settings = rsvpSettings
        wordsPerMinute = rsvpSettings.baseWpm
        baseMsPerWord = rsvpSettings.baseMsPerWord
        orpHighlightEnabled = rsvpSettings.orpHighlightEnabled
        textSizePercent = rsvpSettings.textSizePercent
        Log.i(TAG, "Applied settings: ${wordsPerMinute} WPM, ORP=${orpHighlightEnabled}")
    }

    /**
     * Set words per minute.
     */
    fun setWpm(wpm: Int) {
        wordsPerMinute = wpm.coerceIn(MIN_WPM, MAX_WPM)
        baseMsPerWord = 60_000L / wordsPerMinute
        Log.i(TAG, "Set WPM: $wordsPerMinute (${baseMsPerWord}ms/word)")
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
        if (isRunning && !isPaused) return

        val hasContent = if (useLegacyMode) legacyWords.isNotEmpty() else glimpses.isNotEmpty()
        if (!hasContent) return

        isRunning = true
        isPaused = false

        // Only reset to beginning if starting fresh
        if (currentGlimpseIndex == 0) {
            lastGlimpseTimeNs = System.nanoTime()
            lastPhaseHigh = false
        } else {
            // Resuming - reset timing
            lastGlimpseTimeNs = System.nanoTime()
        }

        updateCurrentGlimpseDuration()
        Log.i(TAG, "Starting RSVP at $wordsPerMinute WPM, index=$currentGlimpseIndex")
        choreographer.postFrameCallback(this)
        invalidate()
    }

    /**
     * Pause RSVP playback (maintains position).
     */
    fun pause() {
        if (!isRunning) return
        isPaused = true
        choreographer.removeFrameCallback(this)
        Log.i(TAG, "Paused RSVP at index $currentGlimpseIndex")
    }

    /**
     * Resume RSVP playback from paused position.
     */
    fun resume() {
        if (!isRunning || !isPaused) return
        isPaused = false
        lastGlimpseTimeNs = System.nanoTime()
        choreographer.postFrameCallback(this)
        Log.i(TAG, "Resumed RSVP at index $currentGlimpseIndex")
    }

    /**
     * Stop RSVP playback.
     */
    fun stop() {
        isRunning = false
        isPaused = false
        choreographer.removeFrameCallback(this)
        val total = if (useLegacyMode) legacyWords.size else glimpses.size
        Log.i(TAG, "Stopped RSVP at index $currentGlimpseIndex/$total")
    }

    /**
     * Reset to beginning.
     */
    fun reset() {
        currentGlimpseIndex = 0
        invalidate()
    }

    /**
     * Seek to a specific word index.
     * Finds the glimpse containing that word.
     */
    fun seekToWord(wordIndex: Int) {
        if (useLegacyMode) {
            currentGlimpseIndex = wordIndex.coerceIn(0, legacyWords.size - 1)
        } else {
            currentGlimpseIndex = glimpses.indexOfFirst { glimpse ->
                wordIndex >= glimpse.wordStartIndex &&
                    wordIndex < glimpse.wordStartIndex + glimpse.wordCount
            }.coerceAtLeast(0)
        }
        invalidate()
    }

    /**
     * Seek to a specific glimpse index.
     */
    fun seekToGlimpse(glimpseIndex: Int) {
        val max = if (useLegacyMode) legacyWords.size - 1 else glimpses.size - 1
        currentGlimpseIndex = glimpseIndex.coerceIn(0, max)
        invalidate()
    }

    /**
     * Whether playback is currently running (includes paused state).
     */
    val playing: Boolean
        get() = isRunning && !isPaused

    /**
     * Whether playback is paused.
     */
    val paused: Boolean
        get() = isRunning && isPaused

    /**
     * Current glimpse text being displayed.
     */
    val currentText: String
        get() = if (useLegacyMode) {
            legacyWords.getOrNull(currentGlimpseIndex) ?: ""
        } else {
            glimpses.getOrNull(currentGlimpseIndex)?.text ?: ""
        }

    /**
     * Current word index (first word in current glimpse).
     */
    val currentWordIndex: Int
        get() = if (useLegacyMode) {
            currentGlimpseIndex
        } else {
            glimpses.getOrNull(currentGlimpseIndex)?.wordStartIndex ?: 0
        }

    /**
     * Total word count.
     */
    val totalWords: Int
        get() = if (useLegacyMode) {
            legacyWords.size
        } else {
            glimpses.sumOf { it.wordCount }
        }

    /**
     * Progress through text (0.0-1.0).
     */
    val progress: Float
        get() {
            val total = if (useLegacyMode) legacyWords.size else glimpses.size
            return if (total == 0) 0f else currentGlimpseIndex.toFloat() / total
        }

    private fun updateCurrentGlimpseDuration() {
        currentGlimpseDurationMs = if (useLegacyMode) {
            baseMsPerWord
        } else {
            glimpses.getOrNull(currentGlimpseIndex)?.durationMs ?: baseMsPerWord
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isRunning || isPaused) return

        val shouldAdvance = if (phaseLocked && phaseProvider != null) {
            // Phase-locked: advance on rising edge of phase
            val phase = phaseProvider?.invoke() ?: 0.0
            val isHigh = phase > 0.5
            val wasLow = !lastPhaseHigh
            lastPhaseHigh = isHigh
            isHigh && wasLow
        } else {
            // Time-based: advance after current glimpse duration
            val elapsedNs = frameTimeNanos - lastGlimpseTimeNs
            val elapsedMs = elapsedNs / 1_000_000
            elapsedMs >= currentGlimpseDurationMs
        }

        if (shouldAdvance) {
            currentGlimpseIndex++
            lastGlimpseTimeNs = frameTimeNanos

            val total = if (useLegacyMode) legacyWords.size else glimpses.size

            if (currentGlimpseIndex >= total) {
                // Text complete
                isRunning = false
                isPaused = false
                onTextComplete?.invoke()
                Log.i(TAG, "RSVP complete")
                return
            }

            updateCurrentGlimpseDuration()

            // Notify progress
            onProgressUpdate?.invoke(
                currentWordIndex,
                currentGlimpseIndex,
                totalWords,
                total
            )

            invalidate()
        }

        choreographer.postFrameCallback(this)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val hasContent = if (useLegacyMode) legacyWords.isNotEmpty() else glimpses.isNotEmpty()
        if (!hasContent) return

        // Get current text to measure
        val currentTextString = if (useLegacyMode) {
            legacyWords.getOrNull(currentGlimpseIndex) ?: return
        } else {
            glimpses.getOrNull(currentGlimpseIndex)?.text ?: return
        }

        // Calculate text size proportional to orientation
        // Use smaller dimension (portrait: width, landscape: height) as base
        val isPortrait = height > width
        val baseDimension = if (isPortrait) width else height
        val orientationFactor = if (isPortrait) 0.18f else 0.15f // Slightly larger for portrait

        // Start with dimension-based size
        var textSize = (baseDimension * textSizePercent * orientationFactor).coerceAtLeast(48f * resources.displayMetrics.scaledDensity)

        // Ensure text fits within 90% of screen width
        val maxWidth = width * 0.9f
        textPaint.textSize = textSize
        var measuredWidth = textPaint.measureText(currentTextString)

        // Reduce text size if it exceeds max width
        while (measuredWidth > maxWidth && textSize > 24f * resources.displayMetrics.scaledDensity) {
            textSize *= 0.95f
            textPaint.textSize = textSize
            measuredWidth = textPaint.measureText(currentTextString)
        }

        // Apply final text size to all paints
        textPaint.textSize = textSize
        shadowPaint.textSize = textSize
        orpPaint.textSize = textSize

        // Center position
        val x = width / 2f
        val y = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2
        val shadowOffset = 2f * resources.displayMetrics.density

        if (useLegacyMode) {
            // Legacy word-by-word display
            canvas.drawText(currentTextString, x + shadowOffset, y + shadowOffset, shadowPaint)
            canvas.drawText(currentTextString, x, y, textPaint)
        } else {
            // Glimpse-based display with ORP highlighting
            val glimpse = glimpses.getOrNull(currentGlimpseIndex) ?: return
            drawGlimpseWithORP(canvas, glimpse, x, y, shadowOffset)
        }
    }

    /**
     * Draw a glimpse with ORP (Optimal Recognition Point) highlighting.
     * The focus character is drawn in red for faster recognition.
     */
    private fun drawGlimpseWithORP(
        canvas: Canvas,
        glimpse: Glimpse,
        centerX: Float,
        y: Float,
        shadowOffset: Float
    ) {
        val text = glimpse.text
        val focusIndex = glimpse.focusCharIndex.coerceIn(0, text.length - 1)

        // Calculate text width to center properly
        val textWidth = textPaint.measureText(text)
        val startX = centerX - textWidth / 2

        // Draw shadow
        canvas.drawText(text, centerX + shadowOffset, y + shadowOffset, shadowPaint)

        if (!orpHighlightEnabled || text.isEmpty()) {
            // No ORP highlighting - draw text centered
            canvas.drawText(text, centerX, y, textPaint)
            return
        }

        // Draw text in three parts: before ORP, ORP char, after ORP
        val beforeORP = text.substring(0, focusIndex)
        val orpChar = text[focusIndex].toString()
        val afterORP = if (focusIndex + 1 < text.length) text.substring(focusIndex + 1) else ""

        var currentX = startX

        // Draw text before ORP
        if (beforeORP.isNotEmpty()) {
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(beforeORP, currentX, y, textPaint)
            currentX += textPaint.measureText(beforeORP)
        }

        // Draw ORP character in red
        orpPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(orpChar, currentX, y, orpPaint)
        currentX += orpPaint.measureText(orpChar)

        // Draw text after ORP
        if (afterORP.isNotEmpty()) {
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(afterORP, currentX, y, textPaint)
        }

        // Reset text alignment
        textPaint.textAlign = Paint.Align.CENTER
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }
}
