package com.gammasync.domain.rsvp

/**
 * User-configurable RSVP display settings.
 *
 * Controls text appearance, timing adjustments, and phrase grouping behavior.
 */
data class RsvpSettings(
    /** Base words per minute (theta-locked values preferred) */
    val baseWpm: Int = 360,

    /** Maximum characters per glimpse (for text wrapping) */
    val maxGlimpseChars: Int = 25,

    /** Maximum words per glimpse */
    val maxGlimpseWords: Int = 3,

    /** Enable multi-line display for long glimpses */
    val multiLineEnabled: Boolean = false,

    /** Max lines when multi-line enabled */
    val maxLines: Int = 2,

    /** Text size as percentage of view height (0.05 - 0.25) */
    val textSizePercent: Float = 0.15f,

    /** Enable hyphenation for long words */
    val hyphenationEnabled: Boolean = true,

    /** Enable ORP (Optimal Recognition Point) highlighting */
    val orpHighlightEnabled: Boolean = true,

    /** Punctuation pause multiplier for sentence endings (1.0 = no change) */
    val sentenceEndPauseMultiplier: Float = 1.5f,

    /** Punctuation pause multiplier for commas/semicolons (1.0 = no change) */
    val commaPauseMultiplier: Float = 1.3f,

    /** Long word pause multiplier for words 10+ chars (1.0 = no change) */
    val longWordPauseMultiplier: Float = 1.2f,

    /** Threshold for "long word" pause adjustment */
    val longWordThreshold: Int = 10
) {
    companion object {
        /** Minimum text size percentage */
        const val MIN_TEXT_SIZE_PERCENT = 0.05f

        /** Maximum text size percentage */
        const val MAX_TEXT_SIZE_PERCENT = 0.25f

        /** Default settings instance */
        val DEFAULT = RsvpSettings()
    }

    /**
     * Calculate the base milliseconds per word for current WPM.
     */
    val baseMsPerWord: Long
        get() = 60_000L / baseWpm

    /**
     * Validate and constrain text size to valid range.
     */
    fun withValidTextSize(): RsvpSettings {
        val constrained = textSizePercent.coerceIn(MIN_TEXT_SIZE_PERCENT, MAX_TEXT_SIZE_PERCENT)
        return if (constrained != textSizePercent) copy(textSizePercent = constrained) else this
    }
}
