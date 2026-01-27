package com.gammasync.domain.rsvp

/**
 * A single display unit for RSVP (Rapid Serial Visual Presentation).
 * May contain 1-3 words grouped together for natural reading.
 *
 * Supports:
 * - ORP (Optimal Recognition Point) highlighting
 * - Variable timing based on punctuation and word length
 * - Position tracking for resume functionality
 */
data class Glimpse(
    /** The text to display (1-3 words combined) */
    val text: String,

    /** Index of character to highlight (ORP focus point), -1 if none */
    val focusCharIndex: Int,

    /** Display duration in milliseconds (base WPM adjusted for length/punctuation) */
    val durationMs: Long,

    /** Zero-based position in original word list (for resume tracking) */
    val wordStartIndex: Int,

    /** Number of words in this glimpse */
    val wordCount: Int,

    /** True if this glimpse ends a sentence (for TTS pause) */
    val sentenceEnd: Boolean
) {
    companion object {
        /** Sentence-ending punctuation */
        private val SENTENCE_ENDERS = setOf('.', '!', '?')

        /** Punctuation to strip when calculating ORP */
        private const val PUNCTUATION = "\n,.?!:;\"'"

        /**
         * Calculate ORP (Optimal Recognition Point) for a word.
         *
         * The ORP is slightly left of center, matching natural eye fixation.
         * Based on research from Spritz and similar RSVP implementations.
         *
         * @param word The word to calculate ORP for
         * @return Character index to highlight (0-based)
         */
        fun calculateORP(word: String): Int {
            // Strip trailing punctuation for length calculation
            var length = word.length
            while (length > 0 && PUNCTUATION.contains(word[length - 1])) {
                length--
            }

            return when (length) {
                0, 1 -> 0      // 1 letter: highlight first char
                2, 3 -> 1      // 2-3 letters: highlight second char
                else -> (length / 2) - 1  // 4+ letters: slightly left of center
            }
        }

        /**
         * Check if text ends with sentence-ending punctuation.
         */
        fun isSentenceEnd(text: String): Boolean {
            val lastChar = text.trimEnd().lastOrNull() ?: return false
            return SENTENCE_ENDERS.contains(lastChar)
        }

        /**
         * Check if text ends with a pause-worthy punctuation mark.
         * Includes commas, semicolons, colons.
         */
        fun hasPausePunctuation(text: String): Boolean {
            val lastChar = text.trimEnd().lastOrNull() ?: return false
            return lastChar == ',' || lastChar == ';' || lastChar == ':'
        }

        /**
         * Get the longest word length in a glimpse text.
         */
        fun maxWordLength(text: String): Int {
            return text.split(" ").maxOfOrNull { word ->
                word.trimEnd { PUNCTUATION.contains(it) }.length
            } ?: 0
        }
    }
}
