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
 * Audio stimulus modulation modes.
 */
enum class AudioMode {
    /**
     * Isochronic tones: Volume pulsing at target frequency.
     * Single channel (mono) with amplitude modulation.
     * Current default behavior in GammaSync.
     */
    ISOCHRONIC,

    /**
     * Binaural beats: Stereo frequency difference creates perception of beat.
     * Left ear: base frequency, Right ear: base + target Hz.
     * Requires headphones for proper effect.
     */
    BINAURAL,

    /**
     * Coupled oscillation: Gamma bursts nested inside theta cycles.
     * Used for Memory Write mode (theta-gamma coupling).
     * Gamma pulses only during theta peak (30% duty ratio).
     */
    COUPLED,

    /**
     * Silent: No audio stimulus, noise only.
     * Used for Migraine mode where audio may be triggering.
     */
    SILENT
}
