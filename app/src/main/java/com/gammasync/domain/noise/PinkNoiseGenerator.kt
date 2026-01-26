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

package com.gammasync.domain.noise

import kotlin.random.Random

/**
 * Pink noise (1/f noise) generator using the Voss-McCartney algorithm.
 *
 * Pink noise has equal energy per octave, creating a natural, comfortable
 * sound similar to rainfall or wind. It's preferred for audio therapy
 * as it's less harsh than white noise.
 *
 * The Voss-McCartney algorithm efficiently generates pink noise by
 * summing multiple white noise sources that update at different rates
 * (octave-spaced).
 */
class PinkNoiseGenerator(
    private val numOctaves: Int = 16,
    private val random: Random = Random.Default
) {
    // White noise values for each octave
    private val octaveValues = DoubleArray(numOctaves) { random.nextDouble() * 2 - 1 }

    // Counter for determining which octaves to update
    private var counter = 0

    // Running sum for efficiency
    private var runningSum = octaveValues.sum()

    // Normalization factor to keep output in -1.0 to 1.0 range
    // Factor of 8 balances variance preservation with amplitude bounds
    // (16 octaves with mean 0, sum rarely exceeds ±8 in practice)
    private val normalizationFactor = 1.0 / 8.0

    /**
     * Generate the next pink noise sample.
     * @return Sample value in range -1.0 to 1.0
     */
    fun nextSample(): Double {
        // Find which octave to update based on trailing zeros of counter
        // This is the key insight of Voss-McCartney: lower octaves update
        // less frequently, creating the 1/f spectral slope.
        val octaveToUpdate = counter.countTrailingZeroBits().coerceAtMost(numOctaves - 1)

        // Subtract old value from running sum
        runningSum -= octaveValues[octaveToUpdate]

        // Generate new random value for this octave
        val newValue = random.nextDouble() * 2 - 1
        octaveValues[octaveToUpdate] = newValue

        // Add new value to running sum
        runningSum += newValue

        counter++

        // Return normalized and clamped sample
        return (runningSum * normalizationFactor).coerceIn(-1.0, 1.0)
    }

    /**
     * Fill a buffer with pink noise samples.
     * @param buffer Array to fill with samples (-1.0 to 1.0 range)
     */
    fun fillBuffer(buffer: DoubleArray) {
        for (i in buffer.indices) {
            buffer[i] = nextSample()
        }
    }

    /**
     * Fill a buffer with pink noise samples scaled to Short range.
     * @param buffer Array to fill with PCM samples
     * @param amplitude Volume scaling (0.0 to 1.0)
     */
    fun fillBuffer(buffer: ShortArray, amplitude: Double = 1.0) {
        for (i in buffer.indices) {
            val sample = nextSample() * amplitude
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    /**
     * Reset the generator to initial state.
     */
    fun reset() {
        counter = 0
        for (i in octaveValues.indices) {
            octaveValues[i] = random.nextDouble() * 2 - 1
        }
        runningSum = octaveValues.sum()
    }
}
