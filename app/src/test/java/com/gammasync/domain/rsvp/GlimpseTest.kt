package com.gammasync.domain.rsvp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlimpseTest {

    // --- ORP Calculation Tests ---

    @Test
    fun `ORP for single letter word is 0`() {
        assertEquals(0, Glimpse.calculateORP("I"))
        assertEquals(0, Glimpse.calculateORP("a"))
    }

    @Test
    fun `ORP for 2-letter word is 1`() {
        assertEquals(1, Glimpse.calculateORP("is"))
        assertEquals(1, Glimpse.calculateORP("to"))
        assertEquals(1, Glimpse.calculateORP("an"))
    }

    @Test
    fun `ORP for 3-letter word is 1`() {
        assertEquals(1, Glimpse.calculateORP("the"))
        assertEquals(1, Glimpse.calculateORP("and"))
        assertEquals(1, Glimpse.calculateORP("for"))
    }

    @Test
    fun `ORP for 4-letter word is 1`() {
        // 4 / 2 - 1 = 1
        assertEquals(1, Glimpse.calculateORP("that"))
        assertEquals(1, Glimpse.calculateORP("with"))
    }

    @Test
    fun `ORP for 5-letter word is 1`() {
        // 5 / 2 - 1 = 1
        assertEquals(1, Glimpse.calculateORP("world"))
        assertEquals(1, Glimpse.calculateORP("April"))
    }

    @Test
    fun `ORP for 6-letter word is 2`() {
        // 6 / 2 - 1 = 2
        assertEquals(2, Glimpse.calculateORP("memory"))
        assertEquals(2, Glimpse.calculateORP("desire"))
    }

    @Test
    fun `ORP for 9-letter word is 3`() {
        // 9 / 2 - 1 = 3
        assertEquals(3, Glimpse.calculateORP("cruellest"))   // 9 letters
        assertEquals(3, Glimpse.calculateORP("cruellest,"))  // 9 letters (strips comma)
        assertEquals(3, Glimpse.calculateORP("colonnade"))   // 9 letters
    }

    @Test
    fun `ORP for 13-letter word is 5`() {
        // 13 / 2 - 1 = 5
        assertEquals(5, Glimpse.calculateORP("cybersecurity"))  // 13 chars
    }

    @Test
    fun `ORP strips trailing punctuation`() {
        assertEquals(1, Glimpse.calculateORP("the."))    // 3 chars
        assertEquals(1, Glimpse.calculateORP("world,"))  // 5 chars
        assertEquals(2, Glimpse.calculateORP("memory!")) // 6 chars
        assertEquals(1, Glimpse.calculateORP("is?"))     // 2 chars
        assertEquals(1, Glimpse.calculateORP("\"end\"")) // Strips quotes
    }

    @Test
    fun `ORP handles empty string`() {
        assertEquals(0, Glimpse.calculateORP(""))
    }

    @Test
    fun `ORP handles punctuation only`() {
        assertEquals(0, Glimpse.calculateORP("..."))
        assertEquals(0, Glimpse.calculateORP("?!"))
    }

    // --- Sentence End Tests ---

    @Test
    fun `isSentenceEnd detects periods`() {
        assertTrue(Glimpse.isSentenceEnd("word."))
        assertTrue(Glimpse.isSentenceEnd("end. "))
    }

    @Test
    fun `isSentenceEnd detects exclamation marks`() {
        assertTrue(Glimpse.isSentenceEnd("wow!"))
        assertTrue(Glimpse.isSentenceEnd("Stop! "))
    }

    @Test
    fun `isSentenceEnd detects question marks`() {
        assertTrue(Glimpse.isSentenceEnd("why?"))
        assertTrue(Glimpse.isSentenceEnd("How? "))
    }

    @Test
    fun `isSentenceEnd returns false for commas`() {
        assertFalse(Glimpse.isSentenceEnd("word,"))
        assertFalse(Glimpse.isSentenceEnd("however,"))
    }

    @Test
    fun `isSentenceEnd returns false for plain words`() {
        assertFalse(Glimpse.isSentenceEnd("word"))
        assertFalse(Glimpse.isSentenceEnd("the"))
    }

    // --- Pause Punctuation Tests ---

    @Test
    fun `hasPausePunctuation detects commas`() {
        assertTrue(Glimpse.hasPausePunctuation("word,"))
        assertTrue(Glimpse.hasPausePunctuation("however, "))
    }

    @Test
    fun `hasPausePunctuation detects semicolons`() {
        assertTrue(Glimpse.hasPausePunctuation("clause;"))
    }

    @Test
    fun `hasPausePunctuation detects colons`() {
        assertTrue(Glimpse.hasPausePunctuation("note:"))
    }

    @Test
    fun `hasPausePunctuation returns false for periods`() {
        assertFalse(Glimpse.hasPausePunctuation("word."))
    }

    // --- Max Word Length Tests ---

    @Test
    fun `maxWordLength finds longest word`() {
        assertEquals(5, Glimpse.maxWordLength("the quick"))
        assertEquals(6, Glimpse.maxWordLength("a memory"))
        assertEquals(9, Glimpse.maxWordLength("the cruellest month"))
    }

    @Test
    fun `maxWordLength strips punctuation`() {
        assertEquals(5, Glimpse.maxWordLength("world,"))
        assertEquals(6, Glimpse.maxWordLength("memory."))
    }

    @Test
    fun `maxWordLength handles empty string`() {
        assertEquals(0, Glimpse.maxWordLength(""))
    }
}
