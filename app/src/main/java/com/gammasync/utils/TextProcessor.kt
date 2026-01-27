package com.gammasync.utils

import com.gammasync.domain.rsvp.Glimpse
import com.gammasync.domain.rsvp.RsvpSettings

/**
 * Text cleaning pipeline for RSVP reading.
 * Strips formatting, URLs, and other distracting elements while preserving
 * essential punctuation and capitalization.
 *
 * Also handles glimpse processing with phrase grouping, variable timing,
 * and ORP (Optimal Recognition Point) calculation.
 */
object TextProcessor {

    /**
     * Function words that should never appear alone in a glimpse.
     * These are grouped with adjacent content words for natural reading.
     */
    private val FUNCTION_WORDS = setOf(
        "a", "an", "the",           // Articles
        "and", "or", "but", "nor",  // Conjunctions
        "if", "so", "as", "than",   // Subordinating conjunctions
        "in", "on", "at", "to", "for", "of", "with", "by", "from", // Prepositions
        "is", "are", "was", "were", "be", "been", "being",  // Be verbs
        "it", "its", "he", "she", "we", "they", "i",  // Pronouns
        "has", "have", "had", "do", "does", "did",   // Auxiliary verbs
        "not", "no", "yes"           // Adverbs/particles
    )

    /**
     * Clean text for RSVP presentation.
     * Removes URLs, formatting, excessive whitespace while preserving
     * basic punctuation for pause timing.
     */
    fun sanitize(text: String): String {
        var cleaned = text

        // Strip HTML tags
        cleaned = cleaned.replace(Regex("<[^>]+>"), " ")

        // Remove markdown headers (###, ##, #) - must be before bold/italic
        cleaned = cleaned.replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")

        // Remove markdown formatting - do this BEFORE URL removal
        cleaned = cleaned.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1") // **bold**
        cleaned = cleaned.replace(Regex("\\*([^*]+)\\*"), "$1") // *italic*
        cleaned = cleaned.replace(Regex("_([^_]+)_"), "$1") // _italic_
        cleaned = cleaned.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1") // [text](url) - extract text, URL removed later

        // Remove URLs (http/https) - after markdown links so we catch both inline and link URLs
        cleaned = cleaned.replace(Regex("https?://\\S+"), " ")

        // Remove email addresses
        cleaned = cleaned.replace(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), " ")

        // Remove markdown lists (-, *, +, numbered) BEFORE normalizing dashes
        // This way we only remove true markdown lists, not compound words with em-dashes
        cleaned = cleaned.replace(Regex("^[\\s]*[-\u2013\u2014*+]\\s+", RegexOption.MULTILINE), "")
        cleaned = cleaned.replace(Regex("^[\\s]*\\d+\\.\\s+", RegexOption.MULTILINE), "")

        // Normalize quotes and dashes AFTER markdown list removal
        // This preserves hyphens in compound words like "well-known"
        cleaned = cleaned.replace(Regex("[\u201C\u201D]"), "\"")  // Smart double quotes to straight
        cleaned = cleaned.replace(Regex("[\u2018\u2019]"), "'")   // Smart single quotes to straight
        cleaned = cleaned.replace(Regex("[\u2013\u2014]"), "-")   // Em/en dash to hyphen

        // Remove code blocks
        cleaned = cleaned.replace(Regex("```[^`]*```"), " ") // ```code```
        cleaned = cleaned.replace(Regex("`([^`]+)`"), "$1") // `inline code`

        // Remove emojis and other pictographs
        // Unicode ranges for emoji: https://unicode.org/emoji/charts/full-emoji-list.html
        cleaned = cleaned.replace(Regex("[\u2600-\u27BF]"), "") // Miscellaneous Symbols
        cleaned = cleaned.replace(Regex("[\uD83C-\uDBFF\uDC00-\uDFFF]+"), "") // Emoji surrogate pairs
        cleaned = cleaned.replace(Regex("[\u2300-\u23FF]"), "") // Miscellaneous Technical
        cleaned = cleaned.replace(Regex("[\u2B50-\u2B55]"), "") // Stars and other symbols
        cleaned = cleaned.replace(Regex("[\u200D\uFE0F]"), "") // Zero-width joiner and variation selector

        // Remove special symbols (preserve only basic punctuation and hyphen)
        // Preserved: letters, digits, whitespace, . , ! ? ; : ' " -
        // Note: hyphen at end of character class to avoid needing escape
        cleaned = cleaned.replace(Regex("[^\\p{L}\\p{N}\\s.,!?;:'\"-]"), " ")

        // Normalize whitespace
        cleaned = cleaned.replace(Regex("\\s+"), " ")
        cleaned = cleaned.trim()

        return cleaned
    }
    
    /**
     * Split text into individual words, filtering out empty strings.
     */
    fun getWords(text: String): List<String> {
        return text.split(Regex("\\s+")).filter { it.isNotBlank() }
    }
    
    /**
     * Estimate reading duration based on word count and WPM.
     */
    fun estimateReadingTime(wordCount: Int, wpm: Int): Int {
        return ((wordCount.toFloat() / wpm) * 60).toInt().coerceAtLeast(1)
    }

    // ============================================================
    // GLIMPSE PROCESSING
    // ============================================================

    /**
     * Process text into glimpses with phrase grouping and variable timing.
     *
     * Features:
     * - Groups function words with adjacent content words
     * - Adjusts timing based on punctuation and word length
     * - Calculates ORP (Optimal Recognition Point) for each glimpse
     * - Supports hyphenation for long words
     *
     * @param text Sanitized input text
     * @param settings Display settings for timing and grouping
     * @return List of glimpses ready for display
     */
    fun processToGlimpses(text: String, settings: RsvpSettings): List<Glimpse> {
        val words = getWords(text)
        if (words.isEmpty()) return emptyList()

        val glimpses = mutableListOf<Glimpse>()
        var wordIndex = 0

        while (wordIndex < words.size) {
            val result = createGlimpse(words, wordIndex, settings)
            glimpses.add(result.glimpse)
            wordIndex += result.wordsConsumed
        }

        return glimpses
    }

    /**
     * Result of creating a single glimpse.
     */
    private data class GlimpseResult(
        val glimpse: Glimpse,
        val wordsConsumed: Int
    )

    /**
     * Create a single glimpse starting at the given word index.
     * Groups function words with content words.
     */
    private fun createGlimpse(
        words: List<String>,
        startIndex: Int,
        settings: RsvpSettings
    ): GlimpseResult {
        val glimpseWords = mutableListOf<String>()
        var currentIndex = startIndex
        var totalChars = 0

        // Collect words for this glimpse
        while (currentIndex < words.size && glimpseWords.size < settings.maxGlimpseWords) {
            val word = words[currentIndex]
            val wordWithSpace = if (glimpseWords.isEmpty()) word else " $word"

            // Check character limit
            if (totalChars + wordWithSpace.length > settings.maxGlimpseChars && glimpseWords.isNotEmpty()) {
                break
            }

            glimpseWords.add(word)
            totalChars += wordWithSpace.length
            currentIndex++

            // If this word is a function word and we haven't hit limits, try to add the next word
            val isFunction = isFunctionWord(word)
            val nextWord = words.getOrNull(currentIndex)

            if (isFunction && nextWord != null && glimpseWords.size < settings.maxGlimpseWords) {
                val nextWithSpace = " $nextWord"
                if (totalChars + nextWithSpace.length <= settings.maxGlimpseChars) {
                    // Add the next word to avoid orphaned function word
                    continue
                }
            }

            // If we have a content word and next is not a trailing function word, we're done
            if (!isFunction) {
                break
            }
        }

        // Build the glimpse text
        val text = glimpseWords.joinToString(" ")

        // Calculate ORP on the primary content word (longest word in glimpse)
        val primaryWord = glimpseWords.maxByOrNull { wordWithoutPunctuation(it).length } ?: glimpseWords.first()
        val primaryIndex = findWordStartIndex(text, primaryWord)
        val orpInWord = Glimpse.calculateORP(primaryWord)
        val focusCharIndex = if (primaryIndex >= 0) primaryIndex + orpInWord else orpInWord

        // Calculate duration with adjustments
        val durationMs = calculateGlimpseDuration(text, glimpseWords.size, settings)

        // Check for sentence end
        val sentenceEnd = Glimpse.isSentenceEnd(text)

        return GlimpseResult(
            glimpse = Glimpse(
                text = text,
                focusCharIndex = focusCharIndex,
                durationMs = durationMs,
                wordStartIndex = startIndex,
                wordCount = glimpseWords.size,
                sentenceEnd = sentenceEnd
            ),
            wordsConsumed = glimpseWords.size
        )
    }

    /**
     * Calculate display duration for a glimpse with timing adjustments.
     */
    private fun calculateGlimpseDuration(
        text: String,
        wordCount: Int,
        settings: RsvpSettings
    ): Long {
        var durationMs = settings.baseMsPerWord * wordCount

        // Adjust for sentence-ending punctuation
        if (Glimpse.isSentenceEnd(text)) {
            durationMs = (durationMs * settings.sentenceEndPauseMultiplier).toLong()
        }
        // Adjust for comma/semicolon
        else if (Glimpse.hasPausePunctuation(text)) {
            durationMs = (durationMs * settings.commaPauseMultiplier).toLong()
        }

        // Adjust for long words
        val maxLen = Glimpse.maxWordLength(text)
        if (maxLen >= settings.longWordThreshold) {
            durationMs = (durationMs * settings.longWordPauseMultiplier).toLong()
        }

        return durationMs
    }

    /**
     * Check if a word is a function word (should not appear alone).
     */
    fun isFunctionWord(word: String): Boolean {
        val normalized = wordWithoutPunctuation(word).lowercase()
        return FUNCTION_WORDS.contains(normalized)
    }

    /**
     * Remove punctuation from a word for comparison.
     */
    private fun wordWithoutPunctuation(word: String): String {
        return word.trim { !it.isLetterOrDigit() }
    }

    /**
     * Find the start index of a word within the glimpse text.
     */
    private fun findWordStartIndex(text: String, word: String): Int {
        val words = text.split(" ")
        var index = 0
        for (w in words) {
            if (w == word) return index
            index += w.length + 1  // +1 for space
        }
        return 0
    }

    // ============================================================
    // HYPHENATION (Simple syllable-based)
    // ============================================================

    /**
     * Hyphenate a word if it exceeds maxChars.
     * Uses simple vowel-consonant boundary detection.
     *
     * @param word The word to potentially hyphenate
     * @param maxChars Maximum characters per segment
     * @return List of segments (e.g., ["bio-", "luminescence"])
     */
    fun hyphenateWord(word: String, maxChars: Int): List<String> {
        if (word.length <= maxChars) return listOf(word)

        val vowels = setOf('a', 'e', 'i', 'o', 'u', 'y')
        val segments = mutableListOf<String>()
        var currentSegment = StringBuilder()

        for (i in word.indices) {
            val char = word[i]
            currentSegment.append(char)

            // Check for hyphenation point: vowel followed by consonant, near maxChars
            if (currentSegment.length >= maxChars - 2 && currentSegment.length < word.length - 2) {
                val isVowel = vowels.contains(char.lowercaseChar())
                val nextIsConsonant = i + 1 < word.length && !vowels.contains(word[i + 1].lowercaseChar())

                if (isVowel && nextIsConsonant) {
                    segments.add(currentSegment.toString() + "-")
                    currentSegment = StringBuilder()
                }
            }
        }

        if (currentSegment.isNotEmpty()) {
            segments.add(currentSegment.toString())
        }

        // If no good break point found, just split at maxChars
        if (segments.size == 1 && word.length > maxChars) {
            return listOf(
                word.take(maxChars - 1) + "-",
                word.drop(maxChars - 1)
            )
        }

        return segments
    }
}