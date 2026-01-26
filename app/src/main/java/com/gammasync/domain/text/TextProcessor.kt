package com.gammasync.domain.text

import org.commonmark.parser.Parser
import org.commonmark.renderer.text.TextContentRenderer

/**
 * Text processing utilities for RSVP display.
 *
 * Handles loading and cleaning text from various sources:
 * - Plain text (.txt) files
 * - Markdown (.md) files
 *
 * Cleans text by removing:
 * - URLs (http/https/www)
 * - Non-word characters (punctuation, symbols)
 * - Extra whitespace
 */
object TextProcessor {

    // Match http://, https://, and www. URLs
    private val urlRegex = Regex("""https?://\S+|www\.\S+""")

    // Match anything that's not a word character or whitespace
    private val nonWordRegex = Regex("""[^\w\s']""")

    // Match multiple spaces/newlines
    private val multiSpaceRegex = Regex("""\s+""")

    // Commonmark parser and renderer (thread-safe, reusable)
    private val mdParser: Parser = Parser.builder().build()
    private val textRenderer: TextContentRenderer = TextContentRenderer.builder().build()

    /**
     * Strip URLs from text, replacing with single space.
     */
    fun stripUrls(text: String): String = text.replace(urlRegex, " ")

    /**
     * Strip punctuation and symbols, keeping letters, numbers, spaces, and apostrophes.
     * Apostrophes are kept to preserve contractions (don't, it's, etc.)
     */
    fun stripNonWords(text: String): String = text.replace(nonWordRegex, " ")

    /**
     * Normalize whitespace: collapse multiple spaces/newlines to single space.
     */
    fun normalizeWhitespace(text: String): String = text.replace(multiSpaceRegex, " ").trim()

    /**
     * Convert markdown to plain text using Commonmark parser.
     * Extracts text content, stripping all markdown formatting.
     */
    fun markdownToPlainText(markdown: String): String {
        val document = mdParser.parse(markdown)
        return textRenderer.render(document)
    }

    /**
     * Split text into individual words for RSVP display.
     * Returns list of non-empty words.
     */
    fun splitIntoWords(text: String): List<String> {
        return text.split(multiSpaceRegex).filter { it.isNotBlank() }
    }

    /**
     * Full processing pipeline for RSVP text.
     *
     * 1. If markdown, convert to plain text
     * 2. Strip URLs
     * 3. Strip non-word characters (keep apostrophes for contractions)
     * 4. Normalize whitespace
     * 5. Split into words
     *
     * @param text Raw text content
     * @param isMarkdown Whether the text is markdown format
     * @return List of cleaned words ready for RSVP display
     */
    fun processForRsvp(text: String, isMarkdown: Boolean = false): List<String> {
        var processed = text

        // Convert markdown to plain text if needed
        if (isMarkdown) {
            processed = markdownToPlainText(processed)
        }

        // Clean the text
        processed = stripUrls(processed)
        processed = stripNonWords(processed)
        processed = normalizeWhitespace(processed)

        return splitIntoWords(processed)
    }

    /**
     * Estimate reading time in minutes based on word count and WPM.
     */
    fun estimateReadingTime(wordCount: Int, wpm: Int): Int {
        return ((wordCount.toFloat() / wpm) + 0.5f).toInt().coerceAtLeast(1)
    }
}
