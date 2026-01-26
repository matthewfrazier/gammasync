package com.gammasync.domain.text

import org.junit.Assert.*
import org.junit.Test

class TextProcessorTest {

    @Test
    fun `stripUrls removes http URLs`() {
        val input = "Check out https://example.com for more info"
        val result = TextProcessor.stripUrls(input)
        assertEquals("Check out   for more info", result)
    }

    @Test
    fun `stripUrls removes www URLs`() {
        val input = "Visit www.example.com/page today"
        val result = TextProcessor.stripUrls(input)
        assertEquals("Visit   today", result)
    }

    @Test
    fun `stripNonWords removes punctuation but keeps apostrophes`() {
        val input = "Hello, world! It's a test... #hashtag"
        val result = TextProcessor.stripNonWords(input)
        // , ! ... # all become spaces: 3 dots + # + original space = 5 spaces before hashtag
        assertEquals("Hello  world  It's a test     hashtag", result)
    }

    @Test
    fun `normalizeWhitespace collapses multiple spaces`() {
        val input = "Hello    world\n\ntest"
        val result = TextProcessor.normalizeWhitespace(input)
        assertEquals("Hello world test", result)
    }

    @Test
    fun `markdownToPlainText strips formatting`() {
        val markdown = """
            # Heading

            This is **bold** and *italic* text.

            - List item 1
            - List item 2

            [Link text](https://example.com)
        """.trimIndent()

        val result = TextProcessor.markdownToPlainText(markdown)

        assertTrue(result.contains("Heading"))
        assertTrue(result.contains("bold"))
        assertTrue(result.contains("italic"))
        assertTrue(result.contains("List item"))
        assertTrue(result.contains("Link text"))
        assertFalse(result.contains("**"))
        assertFalse(result.contains("*"))
        assertFalse(result.contains("#"))
    }

    @Test
    fun `splitIntoWords handles various whitespace`() {
        val input = "Hello   world\ntest\t\ttabs"
        val result = TextProcessor.splitIntoWords(input)
        assertEquals(listOf("Hello", "world", "test", "tabs"), result)
    }

    @Test
    fun `processForRsvp full pipeline with plain text`() {
        val input = "Hello, world! Visit https://example.com for more."
        val result = TextProcessor.processForRsvp(input, isMarkdown = false)
        assertEquals(listOf("Hello", "world", "Visit", "for", "more"), result)
    }

    @Test
    fun `processForRsvp full pipeline with markdown`() {
        val markdown = "# Title\n\nThis is **bold** text with a [link](https://example.com)."
        val result = TextProcessor.processForRsvp(markdown, isMarkdown = true)

        assertTrue(result.contains("Title"))
        assertTrue(result.contains("bold"))
        assertTrue(result.contains("link"))
        assertFalse(result.any { it.contains("http") })
    }

    @Test
    fun `estimateReadingTime calculates correctly`() {
        // 300 words at 300 WPM = 1 minute
        assertEquals(1, TextProcessor.estimateReadingTime(300, 300))

        // 600 words at 300 WPM = 2 minutes
        assertEquals(2, TextProcessor.estimateReadingTime(600, 300))

        // 150 words at 300 WPM = 1 minute (rounds up)
        assertEquals(1, TextProcessor.estimateReadingTime(150, 300))

        // 450 words at 300 WPM = 2 minutes (rounds)
        assertEquals(2, TextProcessor.estimateReadingTime(450, 300))
    }

    @Test
    fun `processForRsvp handles empty input`() {
        val result = TextProcessor.processForRsvp("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `processForRsvp handles URL-only input`() {
        val result = TextProcessor.processForRsvp("https://example.com")
        assertTrue(result.isEmpty())
    }
}
