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

package com.gammasync.domain

import kotlin.math.PI
import kotlin.math.sin

/**
 * Pure Kotlin signal generator for 40Hz gamma entrainment.
 * No Android dependencies - testable in isolation.
 *
 * @param sampleRate Audio sample rate in Hz (typically 48000)
 * @param frequency Target frequency in Hz (default 40.0 for gamma)
 */
class SignalOscillator(
    private val sampleRate: Int = 48000,
    private val frequency: Double = 40.0
) {
    @Volatile
    private var sampleIndex: Long = 0
    private val samplesPerCycle = sampleRate / frequency

    /**
     * Current phase of the oscillator (0.0 to 1.0)
     * Video renderer uses this to sync visual stimulus.
     */
    val phase: Double
        get() = (sampleIndex % samplesPerCycle) / samplesPerCycle

    /**
     * Generate next audio sample as a sine wave.
     * @param amplitude Volume from 0.0 to 1.0
     * @return Sample value from -1.0 to 1.0
     */
    fun nextSample(amplitude: Double = 1.0): Double {
        val sample = sin(2 * PI * frequency * sampleIndex / sampleRate) * amplitude
        sampleIndex++
        return sample
    }

    /**
     * Fill a buffer with samples.
     * @param buffer Array to fill with samples
     * @param amplitude Volume from 0.0 to 1.0
     */
    fun fillBuffer(buffer: ShortArray, amplitude: Double = 1.0) {
        for (i in buffer.indices) {
            val sample = nextSample(amplitude)
            // Convert -1.0..1.0 to Short range
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /**
     * Reset oscillator to start of cycle.
     */
    fun reset() {
        sampleIndex = 0
    }
}
