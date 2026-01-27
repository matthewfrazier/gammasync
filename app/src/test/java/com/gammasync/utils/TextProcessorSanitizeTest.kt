package com.gammasync.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for TextProcessor.sanitize() method.
 * Verifies removal of markdown, emojis, URLs, and special formatting.
 */
class TextProcessorSanitizeTest {

    @Test
    fun `removes markdown headers`() {
        val text = """
            # Title
            ## Subtitle
            ### Section
            Regular text
        """.trimIndent()

        val result = TextProcessor.sanitize(text)

        assertFalse(result.contains("#"))
        assertTrue(result.contains("Title"))
        assertTrue(result.contains("Subtitle"))
        assertTrue(result.contains("Section"))
    }

    @Test
    fun `removes markdown bold and italic`() {
        val text = "This is **bold** and *italic* and _underline_ text"
        val result = TextProcessor.sanitize(text)

        assertFalse(result.contains("**"))
        assertFalse(result.contains("*"))
        assertFalse(result.contains("_"))
        assertTrue(result.contains("bold"))
        assertTrue(result.contains("italic"))
        assertTrue(result.contains("underline"))
    }

    @Test
    fun `removes markdown links`() {
        val text = "Check out [this link](https://example.com) for more info"
        val result = TextProcessor.sanitize(text)

        assertFalse(result.contains("["))
        assertFalse(result.contains("]"))
        assertFalse(result.contains("https://"))
        assertTrue(result.contains("this link"))
    }

    @Test
    fun `removes markdown lists`() {
        val text = """
            - Item 1
            * Item 2
            + Item 3
            1. Numbered item
        """.trimIndent()

        val result = TextProcessor.sanitize(text)

        assertFalse(result.startsWith("-"))
        assertFalse(result.contains("\n-"))
        assertFalse(result.contains("1."))
        assertTrue(result.contains("Item 1"))
        assertTrue(result.contains("Numbered item"))
    }

    @Test
    fun `removes emojis`() {
        // Using unicode escapes for emojis
        val text = "Hello \uD83D\uDE00 world \uD83C\uDF1F with emojis \u2B50"
        val result = TextProcessor.sanitize(text)

        assertFalse(result.contains("\uD83D\uDE00"))
        assertFalse(result.contains("\uD83C\uDF1F"))
        assertFalse(result.contains("\u2B50"))
        assertTrue(result.contains("Hello"))
        assertTrue(result.contains("world"))
    }

    @Test
    fun `removes URLs`() {
        val text = "Visit https://example.com or http://test.org for more"
        val result = TextProcessor.sanitize(text)

        assertFalse(result.contains("https://"))
        assertFalse(result.contains("http://"))
        assertTrue(result.contains("Visit"))
        assertTrue(result.contains("for more"))
    }

    @Test
    fun `removes email addresses`() {
        val text = "Contact us at test@example.com for support"
        val result = TextProcessor.sanitize(text)

        assertFalse(result.contains("@"))
        assertFalse(result.contains("test@example.com"))
        assertTrue(result.contains("Contact"))
        assertTrue(result.contains("support"))
    }

    @Test
    fun `removes HTML tags`() {
        val text = "<p>This is <strong>HTML</strong> content</p>"
        val result = TextProcessor.sanitize(text)

        assertFalse(result.contains("<p>"))
        assertFalse(result.contains("<strong>"))
        assertFalse(result.contains("</strong>"))
        assertTrue(result.contains("This is HTML content"))
    }

    @Test
    fun `removes code blocks`() {
        val text = """
            Some text
            ```kotlin
            val code = "example"
            ```
            More text with `inline code` here
        """.trimIndent()

        val result = TextProcessor.sanitize(text)

        assertFalse(result.contains("```"))
        assertFalse(result.contains("val code"))
        assertTrue(result.contains("Some text"))
        assertTrue(result.contains("inline code"))
    }

    @Test
    fun `normalizes smart quotes`() {
        // Using unicode escapes for smart quotes to avoid Kotlin syntax issues
        val text = "\u201CSmart quotes\u201D and \u2018apostrophes\u2019 should normalize"
        val result = TextProcessor.sanitize(text)

        assertTrue(result.contains("\"Smart quotes\""))
        assertTrue(result.contains("'apostrophes'"))
        assertFalse(result.contains("\u201C"))
        assertFalse(result.contains("\u201D"))
    }

    @Test
    fun `removes em and en dashes`() {
        // Em-dashes and en-dashes are typically used for punctuation, not compound words
        // They should be removed to simplify RSVP text
        val text = "This is important\u2014very important. Range 10\u201320."
        val result = TextProcessor.sanitize(text)

        // Em/en dashes should be removed
        assertFalse(result.contains("\u2014"))
        assertFalse(result.contains("\u2013"))
        // Text should remain
        assertTrue(result.contains("important"))
        assertTrue(result.contains("Range"))
    }

    @Test
    fun `preserves basic punctuation`() {
        val text = "Hello, world! How are you? I'm fine; thanks."
        val result = TextProcessor.sanitize(text)

        assertTrue(result.contains(","))
        assertTrue(result.contains("!"))
        assertTrue(result.contains("?"))
        assertTrue(result.contains(";"))
        assertTrue(result.contains("'"))
        assertTrue(result.contains("."))
    }

    @Test
    fun `removes parentheses and brackets`() {
        val text = "Words in (parentheses) and [brackets] get cleaned"
        val result = TextProcessor.sanitize(text)

        // Parentheses and brackets are removed as they're not needed for RSVP
        assertFalse(result.contains("("))
        assertFalse(result.contains(")"))
        assertFalse(result.contains("["))
        assertFalse(result.contains("]"))
        assertTrue(result.contains("parentheses"))
        assertTrue(result.contains("brackets"))
    }

    @Test
    fun `removes special symbols but keeps text`() {
        // Using unicode escapes for special symbols
        val text = "Text with \u00A9 symbols \u2122 and \u00AE marks \u00A7 should clean"
        val result = TextProcessor.sanitize(text)

        assertFalse(result.contains("\u00A9"))
        assertFalse(result.contains("\u2122"))
        assertFalse(result.contains("\u00AE"))
        assertFalse(result.contains("\u00A7"))
        assertTrue(result.contains("symbols"))
        assertTrue(result.contains("marks"))
    }

    @Test
    fun `collapses multiple spaces`() {
        val text = "Too    many     spaces"
        val result = TextProcessor.sanitize(text)

        assertEquals("Too many spaces", result)
    }

    @Test
    fun `trims leading and trailing whitespace`() {
        val text = "   Leading and trailing   "
        val result = TextProcessor.sanitize(text)

        assertEquals("Leading and trailing", result)
    }

    @Test
    fun `handles complex markdown example from issue`() {
        // Using unicode escapes for emojis to avoid syntax issues
        val text = """
            ## Why is this important?

            This is a **complex** example with:
            - Emojis ${"\uD83D\uDE00"} ${"\uD83C\uDF89"}
            - Markdown headers ##
            - Special symbols ${"\u2122"}
            - Links [example](http://test.com)

            The result should be clean text only.
        """.trimIndent()

        val result = TextProcessor.sanitize(text)

        // Should not contain markdown/formatting
        assertFalse(result.contains("#"))
        assertFalse(result.contains("**"))
        assertFalse(result.contains("\uD83D\uDE00"))
        assertFalse(result.contains("\u2122"))
        assertFalse(result.contains("["))
        assertFalse(result.contains("http://"))

        // Should contain the actual text
        assertTrue(result.contains("Why is this important"))
        assertTrue(result.contains("complex"))
        assertTrue(result.contains("example"))
        assertTrue(result.contains("clean text only"))
    }

    @Test
    fun `handles empty string`() {
        val result = TextProcessor.sanitize("")
        assertEquals("", result)
    }

    @Test
    fun `handles plain text unchanged`() {
        val text = "This is plain text with no special formatting."
        val result = TextProcessor.sanitize(text)

        assertEquals(text, result)
    }
}
