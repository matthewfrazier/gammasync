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
 * RSVP (Rapid Serial Visual Presentation) text overlay for speed reading.
 *
 * Displays text word-by-word at configurable WPM, overlaid on therapy visuals.
 * Designed for use with Learning mode to enable reading while receiving
 * theta-gamma entrainment.
 *
 * ## Features
 *
 * - **Configurable WPM**: 100-1000 words per minute (default 300)
 * - **Phase-locked timing**: Optional sync to therapy frequency (e.g., 6Hz theta)
 * - **Anchor letter highlight**: Red ORP (Optimal Recognition Point) for faster reading
 * - **Progress annotations**: Word count and progress bar
 * - **Configurable text size**: Small (10%), Medium (15%), Large (20%) of screen
 *
 * ## Optimal Recognition Point (ORP)
 *
 * The ORP is the letter where your eye naturally focuses when reading a word.
 * Highlighting this letter in red reduces eye movement and improves reading speed.
 * Position is calculated as: word length / 3, biased left for short words.
 *
 * ## Usage
 *
 * ```kotlin
 * rsvpOverlay.setWords(wordList)
 * rsvpOverlay.setTextSizePercent(0.15f)  // 15% of screen height
 * rsvpOverlay.showAnchorHighlight = true
 * rsvpOverlay.showAnnotations = true
 * rsvpOverlay.enablePhaseLock { audioEngine.secondaryPhase }  // Sync to theta
 * rsvpOverlay.start()
 * ```
 *
 * @see com.gammasync.domain.text.TextProcessor For text loading and cleaning
 * @see com.gammasync.infra.DocumentLoader For file picker integration
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
        private const val DEFAULT_TEXT_SIZE_PERCENT = 0.15f
        private const val MIN_TEXT_SIZE_SP = 48f
    }

    // --- Text Paints ---

    /** Main text paint (white, centered) */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT  // Changed for ORP positioning
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    /** Drop shadow paint for visibility against any background */
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    /** Anchor letter paint (red highlight for ORP) */
    private val anchorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    /** Progress bar background paint */
    private val progressBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 255, 255, 255)  // 30% white
    }

    /** Progress bar foreground paint */
    private val progressFgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 255, 255, 255)  // 70% white
    }

    /** Annotation text paint (word count, etc.) */
    private val annotationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 255, 255, 255)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    // --- RSVP State ---

    private var words: List<String> = emptyList()
    private var currentWordIndex = 0
    private var wordsPerMinute = DEFAULT_WPM
    private var msPerWord: Long = 60_000L / DEFAULT_WPM

    // --- Display Settings ---

    /** Text size as percentage of view height (0.10 to 0.25) */
    var textSizePercent: Float = DEFAULT_TEXT_SIZE_PERCENT
        set(value) {
            field = value.coerceIn(0.05f, 0.30f)
            invalidate()
        }

    /** Whether to highlight the anchor letter (ORP) in red */
    var showAnchorHighlight: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    /** Whether to show progress bar and word count annotations */
    var showAnnotations: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    // --- Timing ---

    private val choreographer = Choreographer.getInstance()
    private var lastWordTimeNs = 0L
    private var isRunning = false

    // --- Phase-Lock Mode ---

    private var phaseLocked = false
    private var phaseProvider: (() -> Double)? = null
    private var lastPhaseHigh = false

    // --- Callbacks ---

    /** Called when all words have been displayed */
    var onTextComplete: (() -> Unit)? = null

    // --- Public API ---

    /**
     * Set the text to display, automatically split into words.
     * For pre-processed word lists, use [setWords] instead.
     */
    fun setText(text: String) {
        setWords(text.split(Regex("\\s+")).filter { it.isNotBlank() })
    }

    /**
     * Set pre-processed word list for display.
     * Use this with [TextProcessor.processForRsvp] for cleaned text.
     */
    fun setWords(wordList: List<String>) {
        words = wordList
        currentWordIndex = 0
        Log.i(TAG, "Set ${words.size} words for RSVP display")
        invalidate()
    }

    /**
     * Check if text has been loaded.
     */
    fun hasText(): Boolean = words.isNotEmpty()

    /**
     * Get total word count.
     */
    val wordCount: Int get() = words.size

    /**
     * Set words per minute (clamped to 100-1000).
     */
    fun setWpm(wpm: Int) {
        wordsPerMinute = wpm.coerceIn(MIN_WPM, MAX_WPM)
        msPerWord = 60_000L / wordsPerMinute
        Log.i(TAG, "Set WPM: $wordsPerMinute (${msPerWord}ms/word)")
    }

    /**
     * Enable phase-locked word transitions.
     *
     * When enabled, words advance on the rising edge of the phase signal
     * (when phase crosses 0.5 from below). This syncs word presentation
     * to the therapy frequency.
     *
     * For Learning mode, pass [UniversalAudioEngine.secondaryPhase] to sync
     * to the 6Hz theta rhythm for optimal memory encoding.
     *
     * @param provider Function returning current phase (0.0-1.0)
     */
    fun enablePhaseLock(provider: () -> Double) {
        phaseProvider = provider
        phaseLocked = true
        Log.i(TAG, "Phase-lock enabled")
    }

    /**
     * Disable phase-locked mode and use fixed WPM timing.
     */
    fun disablePhaseLock() {
        phaseLocked = false
        phaseProvider = null
        Log.i(TAG, "Phase-lock disabled, using ${wordsPerMinute} WPM")
    }

    /**
     * Start RSVP playback from current position.
     */
    fun start() {
        if (isRunning) return
        if (words.isEmpty()) {
            Log.w(TAG, "Cannot start RSVP: no text loaded")
            return
        }

        isRunning = true
        lastWordTimeNs = System.nanoTime()
        lastPhaseHigh = false
        Log.i(TAG, "Starting RSVP: ${words.size} words at $wordsPerMinute WPM")
        choreographer.postFrameCallback(this)
        invalidate()
    }

    /**
     * Stop RSVP playback, preserving current position.
     */
    fun stop() {
        isRunning = false
        choreographer.removeFrameCallback(this)
        Log.i(TAG, "Stopped RSVP at word ${currentWordIndex + 1}/${words.size}")
    }

    /**
     * Reset to the beginning of text.
     */
    fun reset() {
        currentWordIndex = 0
        lastPhaseHigh = false
        invalidate()
        Log.i(TAG, "Reset RSVP to beginning")
    }

    /** Whether playback is currently running */
    val playing: Boolean get() = isRunning

    /** Current word being displayed */
    val currentWord: String get() = words.getOrNull(currentWordIndex) ?: ""

    /** Progress through text (0.0-1.0) */
    val progress: Float
        get() = if (words.isEmpty()) 0f else currentWordIndex.toFloat() / words.size

    // --- Frame Callback ---

    override fun doFrame(frameTimeNanos: Long) {
        if (!isRunning) return

        val shouldAdvance = if (phaseLocked && phaseProvider != null) {
            // Phase-locked: advance on rising edge (phase crosses 0.5 going up)
            val phase = phaseProvider?.invoke() ?: 0.0
            val isHigh = phase > 0.5
            val wasLow = !lastPhaseHigh
            lastPhaseHigh = isHigh
            isHigh && wasLow
        } else {
            // Time-based: advance after msPerWord elapsed
            val elapsedNs = frameTimeNanos - lastWordTimeNs
            val elapsedMs = elapsedNs / 1_000_000
            elapsedMs >= msPerWord
        }

        if (shouldAdvance) {
            currentWordIndex++
            lastWordTimeNs = frameTimeNanos

            if (currentWordIndex >= words.size) {
                isRunning = false
                Log.i(TAG, "RSVP complete: ${words.size} words displayed")
                onTextComplete?.invoke()
                return
            }

            invalidate()
        }

        choreographer.postFrameCallback(this)
    }

    // --- Drawing ---

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (words.isEmpty()) return
        val word = words.getOrNull(currentWordIndex) ?: return

        // Calculate text size
        val textSize = (height * textSizePercent).coerceAtLeast(
            MIN_TEXT_SIZE_SP * resources.displayMetrics.scaledDensity
        )
        textPaint.textSize = textSize
        shadowPaint.textSize = textSize
        anchorPaint.textSize = textSize

        val shadowOffset = 2f * resources.displayMetrics.density

        if (showAnchorHighlight && word.length > 1) {
            // Draw word with highlighted anchor letter (ORP)
            drawWordWithAnchor(canvas, word, textSize, shadowOffset)
        } else {
            // Draw word centered without anchor highlight
            drawWordCentered(canvas, word, shadowOffset)
        }

        // Draw annotations if enabled
        if (showAnnotations) {
            drawAnnotations(canvas)
        }
    }

    /**
     * Draw word with the anchor letter (ORP) highlighted in red.
     *
     * The Optimal Recognition Point is where the eye naturally fixates.
     * It's calculated as approximately 1/3 into the word, biased left.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun drawWordWithAnchor(canvas: Canvas, word: String, textSize: Float, shadowOffset: Float) {
        // Calculate ORP index (roughly 1/3 into word, minimum index 0)
        val orpIndex = when {
            word.length <= 1 -> 0
            word.length <= 3 -> 0
            word.length <= 5 -> 1
            word.length <= 9 -> 2
            word.length <= 13 -> 3
            else -> word.length / 4
        }

        // Split word into three parts: before ORP, ORP letter, after ORP
        val before = word.substring(0, orpIndex)
        val anchor = word[orpIndex].toString()
        val after = word.substring(orpIndex + 1)

        // Measure widths
        val beforeWidth = textPaint.measureText(before)
        val anchorWidth = textPaint.measureText(anchor)

        // Position so ORP letter is at horizontal center
        val startX = width / 2f - beforeWidth - anchorWidth / 2f
        val y = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2

        // Draw shadow
        canvas.drawText(before, startX + shadowOffset, y + shadowOffset, shadowPaint)
        canvas.drawText(anchor, startX + beforeWidth + shadowOffset, y + shadowOffset, shadowPaint)
        canvas.drawText(after, startX + beforeWidth + anchorWidth + shadowOffset, y + shadowOffset, shadowPaint)

        // Draw text parts
        canvas.drawText(before, startX, y, textPaint)
        canvas.drawText(anchor, startX + beforeWidth, y, anchorPaint)  // Red anchor
        canvas.drawText(after, startX + beforeWidth + anchorWidth, y, textPaint)
    }

    /**
     * Draw word centered without anchor highlight.
     */
    private fun drawWordCentered(canvas: Canvas, word: String, shadowOffset: Float) {
        textPaint.textAlign = Paint.Align.CENTER
        shadowPaint.textAlign = Paint.Align.CENTER

        val x = width / 2f
        val y = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2

        canvas.drawText(word, x + shadowOffset, y + shadowOffset, shadowPaint)
        canvas.drawText(word, x, y, textPaint)

        // Restore alignment
        textPaint.textAlign = Paint.Align.LEFT
        shadowPaint.textAlign = Paint.Align.LEFT
    }

    /**
     * Draw progress bar and word count annotations.
     */
    private fun drawAnnotations(canvas: Canvas) {
        val density = resources.displayMetrics.density

        // Progress bar at bottom
        val barHeight = 4f * density
        val barMargin = 24f * density
        val barY = height - barMargin - barHeight

        // Background
        canvas.drawRect(barMargin, barY, width - barMargin, barY + barHeight, progressBgPaint)

        // Foreground (progress)
        val progressWidth = (width - 2 * barMargin) * progress
        canvas.drawRect(barMargin, barY, barMargin + progressWidth, barY + barHeight, progressFgPaint)

        // Word count text above progress bar
        annotationPaint.textSize = 12f * resources.displayMetrics.scaledDensity
        val countText = "${currentWordIndex + 1} / ${words.size}"
        val textY = barY - 8f * density
        canvas.drawText(countText, width / 2f, textY, annotationPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }
}
