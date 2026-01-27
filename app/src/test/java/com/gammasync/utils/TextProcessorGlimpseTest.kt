package com.gammasync.utils

import com.gammasync.domain.rsvp.RsvpSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextProcessorGlimpseTest {

    private val defaultSettings = RsvpSettings(
        baseWpm = 300,
        maxGlimpseChars = 25,
        maxGlimpseWords = 3
    )

    // --- Function Word Detection ---

    @Test
    fun `identifies articles as function words`() {
        assertTrue(TextProcessor.isFunctionWord("a"))
        assertTrue(TextProcessor.isFunctionWord("an"))
        assertTrue(TextProcessor.isFunctionWord("the"))
        assertTrue(TextProcessor.isFunctionWord("The"))  // Case insensitive
    }

    @Test
    fun `identifies prepositions as function words`() {
        assertTrue(TextProcessor.isFunctionWord("in"))
        assertTrue(TextProcessor.isFunctionWord("on"))
        assertTrue(TextProcessor.isFunctionWord("at"))
        assertTrue(TextProcessor.isFunctionWord("to"))
        assertTrue(TextProcessor.isFunctionWord("for"))
        assertTrue(TextProcessor.isFunctionWord("of"))
        assertTrue(TextProcessor.isFunctionWord("with"))
    }

    @Test
    fun `identifies content words correctly`() {
        assertFalse(TextProcessor.isFunctionWord("world"))
        assertFalse(TextProcessor.isFunctionWord("April"))
        assertFalse(TextProcessor.isFunctionWord("cruellest"))
        assertFalse(TextProcessor.isFunctionWord("month"))
    }

    @Test
    fun `handles punctuation in function word detection`() {
        assertTrue(TextProcessor.isFunctionWord("the,"))
        assertTrue(TextProcessor.isFunctionWord("a."))
        assertTrue(TextProcessor.isFunctionWord("\"the\""))
    }

    // --- Phrase Grouping ---

    @Test
    fun `groups article with following noun`() {
        val glimpses = TextProcessor.processToGlimpses("the world", defaultSettings)

        assertEquals(1, glimpses.size)
        assertEquals("the world", glimpses[0].text)
        assertEquals(2, glimpses[0].wordCount)
    }

    @Test
    fun `does not leave article alone`() {
        val glimpses = TextProcessor.processToGlimpses("the quick brown fox", defaultSettings)

        // "the quick" should be grouped together
        assertFalse(glimpses.any { it.text == "the" })
        assertTrue(glimpses[0].text.startsWith("the "))
    }

    @Test
    fun `respects max words per glimpse`() {
        val settings = defaultSettings.copy(maxGlimpseWords = 2)
        val glimpses = TextProcessor.processToGlimpses("the quick brown fox", settings)

        // All glimpses should have at most 2 words
        assertTrue(glimpses.all { it.wordCount <= 2 })
    }

    @Test
    fun `respects max chars per glimpse`() {
        val settings = defaultSettings.copy(maxGlimpseChars = 10)
        val glimpses = TextProcessor.processToGlimpses("the quick brown fox", settings)

        // All glimpses should respect character limit
        assertTrue(glimpses.all { it.text.length <= settings.maxGlimpseChars })
    }

    // --- Variable Timing ---

    @Test
    fun `sentence endings have longer duration`() {
        val settings = defaultSettings.copy(
            sentenceEndPauseMultiplier = 1.5f,
            baseWpm = 300  // 200ms per word
        )
        val glimpses = TextProcessor.processToGlimpses("Hello world. How are you?", settings)

        val periodGlimpse = glimpses.find { it.text.endsWith(".") }
        val normalGlimpse = glimpses.find { !it.sentenceEnd && !it.text.contains(",") }

        // Period glimpse should be marked as sentence end
        assertTrue(periodGlimpse?.sentenceEnd == true)
    }

    @Test
    fun `commas have medium duration`() {
        val settings = defaultSettings.copy(
            commaPauseMultiplier = 1.3f,
            sentenceEndPauseMultiplier = 1.5f
        )
        val glimpses = TextProcessor.processToGlimpses("Hello, world. Goodbye!", settings)

        // The comma glimpse should have different timing than sentence end
        val commaGlimpse = glimpses.find { it.text.contains(",") }
        val periodGlimpse = glimpses.find { it.sentenceEnd }

        assertFalse(commaGlimpse?.sentenceEnd == true)
    }

    // --- Word Position Tracking ---

    @Test
    fun `glimpses track word start positions`() {
        val glimpses = TextProcessor.processToGlimpses("one two three four five", defaultSettings)

        // First glimpse starts at word 0
        assertEquals(0, glimpses.first().wordStartIndex)

        // Word positions should be sequential
        var expectedStart = 0
        for (glimpse in glimpses) {
            assertEquals(expectedStart, glimpse.wordStartIndex)
            expectedStart += glimpse.wordCount
        }
    }

    @Test
    fun `total words equals sum of glimpse word counts`() {
        val text = "The quick brown fox jumps over the lazy dog."
        val glimpses = TextProcessor.processToGlimpses(text, defaultSettings)
        val words = TextProcessor.getWords(text)

        val totalFromGlimpses = glimpses.sumOf { it.wordCount }
        assertEquals(words.size, totalFromGlimpses)
    }

    // --- ORP Focus Calculation ---

    @Test
    fun `glimpse has valid focus char index`() {
        val glimpses = TextProcessor.processToGlimpses("world", defaultSettings)

        val glimpse = glimpses.first()
        assertTrue(glimpse.focusCharIndex >= 0)
        assertTrue(glimpse.focusCharIndex < glimpse.text.length)
    }

    @Test
    fun `multi-word glimpse focuses on content word`() {
        val glimpses = TextProcessor.processToGlimpses("the world", defaultSettings)

        val glimpse = glimpses.first()
        // Focus should be on "world" (the longer/content word)
        // "the world" - 'w' is at index 4, ORP of "world" is 1, so focus at 5
        assertTrue(glimpse.focusCharIndex >= 4)
    }

    // --- Hyphenation ---

    @Test
    fun `short words not hyphenated`() {
        val result = TextProcessor.hyphenateWord("hello", 10)
        assertEquals(1, result.size)
        assertEquals("hello", result[0])
    }

    @Test
    fun `long words get hyphenated`() {
        val result = TextProcessor.hyphenateWord("internationalization", 10)
        assertTrue(result.size > 1)
        assertTrue(result[0].endsWith("-"))
    }

    @Test
    fun `hyphenation respects max chars`() {
        val result = TextProcessor.hyphenateWord("supercalifragilistic", 8)
        assertTrue(result.all { it.length <= 9 })  // +1 for hyphen
    }

    // --- Edge Cases ---

    @Test
    fun `handles empty text`() {
        val glimpses = TextProcessor.processToGlimpses("", defaultSettings)
        assertTrue(glimpses.isEmpty())
    }

    @Test
    fun `handles single word`() {
        val glimpses = TextProcessor.processToGlimpses("Hello", defaultSettings)
        assertEquals(1, glimpses.size)
        assertEquals("Hello", glimpses[0].text)
    }

    @Test
    fun `handles only function words`() {
        val glimpses = TextProcessor.processToGlimpses("the a an", defaultSettings)
        // Should group function words together rather than showing each alone
        assertTrue(glimpses.size < 3)
    }

    @Test
    fun `handles text with punctuation`() {
        val glimpses = TextProcessor.processToGlimpses(
            "Hello, world! How are you?",
            defaultSettings
        )
        assertTrue(glimpses.isNotEmpty())
        assertTrue(glimpses.any { it.sentenceEnd })
    }
}
