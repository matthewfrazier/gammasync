package com.gammasync.domain.rsvp

/**
 * A document processed and ready for RSVP display.
 *
 * Contains pre-processed glimpses with timing metadata,
 * enabling efficient playback without runtime processing.
 */
data class ProcessedDocument(
    /** Original source identifier (content:// URI, https:// URL, or "clipboard") */
    val sourceId: String,

    /** Display name for UI (filename, page title, or "Pasted Text") */
    val displayName: String,

    /** Pre-processed glimpses ready for display */
    val glimpses: List<Glimpse>,

    /** Total word count (sum of all glimpse wordCounts) */
    val totalWords: Int,

    /** Estimated reading time in minutes at default WPM */
    val estimatedMinutes: Int,

    /** Preview text (first ~200 chars sanitized) */
    val preview: String,

    /** Timestamp when document was loaded */
    val loadedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** Maximum preview length in characters */
        const val MAX_PREVIEW_LENGTH = 200

        /**
         * Create a preview string from full text.
         */
        fun createPreview(text: String, maxLength: Int = MAX_PREVIEW_LENGTH): String {
            if (text.length <= maxLength) return text.trim()

            // Find a good break point (space, period, comma)
            val truncated = text.take(maxLength)
            val lastSpace = truncated.lastIndexOf(' ')
            val lastPeriod = truncated.lastIndexOf('.')

            val breakPoint = when {
                lastPeriod > maxLength * 0.7 -> lastPeriod + 1  // Break after period if late enough
                lastSpace > maxLength * 0.5 -> lastSpace        // Break at space if past halfway
                else -> maxLength
            }

            return truncated.take(breakPoint).trim() + "..."
        }

        /**
         * Estimate reading time in minutes.
         */
        fun estimateMinutes(wordCount: Int, wpm: Int): Int {
            return ((wordCount.toFloat() / wpm)).toInt().coerceAtLeast(1)
        }
    }

    /**
     * Get the glimpse at a specific word index.
     * Useful for resume functionality.
     *
     * @param wordIndex The word position to find
     * @return The glimpse containing that word, or null if out of bounds
     */
    fun glimpseAtWordIndex(wordIndex: Int): Glimpse? {
        return glimpses.find { glimpse ->
            wordIndex >= glimpse.wordStartIndex &&
                wordIndex < glimpse.wordStartIndex + glimpse.wordCount
        }
    }

    /**
     * Get the glimpse index for a specific word index.
     *
     * @param wordIndex The word position to find
     * @return The index of the glimpse containing that word, or 0 if not found
     */
    fun glimpseIndexForWordIndex(wordIndex: Int): Int {
        return glimpses.indexOfFirst { glimpse ->
            wordIndex >= glimpse.wordStartIndex &&
                wordIndex < glimpse.wordStartIndex + glimpse.wordCount
        }.coerceAtLeast(0)
    }

    /**
     * Calculate progress percentage for a given glimpse index.
     */
    fun progressPercent(glimpseIndex: Int): Float {
        if (glimpses.isEmpty()) return 0f
        return (glimpseIndex.toFloat() / glimpses.size * 100).coerceIn(0f, 100f)
    }

    /**
     * Calculate progress percentage for a given word index.
     */
    fun progressPercentByWord(wordIndex: Int): Float {
        if (totalWords == 0) return 0f
        return (wordIndex.toFloat() / totalWords * 100).coerceIn(0f, 100f)
    }
}
