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

package com.gammasync.domain.therapy

/**
 * Visual stimulus rendering modes.
 */
enum class VisualMode {
    /**
     * Sine wave interpolation: Smooth transition between warm and cool colors.
     * Current default behavior using ColorTemperature.interpolate().
     * Maintains isoluminance for epilepsy safety.
     */
    SINE,

    /**
     * Strobe: Sharp on/off transitions at 50% duty cycle.
     * phase < 0.5 = warm color, phase >= 0.5 = off (black).
     * More intense stimulus than SINE.
     */
    STROBE,

    /**
     * Static: Single fixed color with no animation.
     * Used for Migraine mode (525nm green, no flicker).
     */
    STATIC,

    /**
     * Split-field: Independent colors for left/right halves of display.
     * Used for Mood Lift mode on XREAL (3840x1080 = 1920 + 1920).
     * Each eye receives different frequency stimulation.
     */
    SPLIT
}
