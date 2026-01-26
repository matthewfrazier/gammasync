/*
 * MIT License
 * Copyright (c) 2026 matthewfrazier
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gammasync.utils

import android.util.Log

/**
 * Text cleaning pipeline for RSVP reading.
 * Strips formatting, URLs, and other distracting elements while preserving
 * essential punctuation and capitalization.
 */
object TextProcessor {
    
    private const val TAG = "TextProcessor"

    /**
     * Clean text for RSVP presentation.
     * Removes URLs, formatting, excessive whitespace while preserving
     * basic punctuation for pause timing.
     */
    fun sanitize(text: String): String {
        Log.d(TAG, "Processing ${text.length} characters")
        
        var cleaned = text
        
        // Strip HTML tags
        cleaned = cleaned.replace(Regex("<[^>]+>"), " ")
        
        // Remove URLs (http/https)
        cleaned = cleaned.replace(Regex("https?://\\S+"), " ")
        
        // Remove email addresses
        cleaned = cleaned.replace(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), " ")
        
        // Remove markdown formatting
        cleaned = cleaned.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1") // **bold**
        cleaned = cleaned.replace(Regex("\\*([^*]+)\\*"), "$1") // *italic*
        cleaned = cleaned.replace(Regex("_([^_]+)_"), "$1") // _italic_
        cleaned = cleaned.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1") // [text](url)
        
        // Remove code blocks
        cleaned = cleaned.replace(Regex("```[^`]*```"), " ") // ```code```
        cleaned = cleaned.replace(Regex("`([^`]+)`"), "$1") // `inline code`
        
        // Normalize quotes (smart quotes to straight)
        cleaned = cleaned.replace(Regex("[\u201C\u201D\u2018\u2019`]"), "'")
        cleaned = cleaned.replace(Regex("[\u2013\u2014]"), "-")
        
        // Normalize whitespace
        cleaned = cleaned.replace(Regex("\\s+"), " ")
        cleaned = cleaned.trim()
        
        Log.d(TAG, "Cleaned to ${cleaned.length} characters")
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
}