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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SignalOscillatorTest {

    @Test
    fun `phase starts at zero`() {
        val oscillator = SignalOscillator()
        assertEquals(0.0, oscillator.phase, 0.0001)
    }

    @Test
    fun `phase completes cycle at correct frequency`() {
        val sampleRate = 48000
        val frequency = 40.0
        val oscillator = SignalOscillator(sampleRate, frequency)

        val samplesPerCycle = (sampleRate / frequency).toInt()

        // Generate one full cycle
        repeat(samplesPerCycle) { oscillator.nextSample() }

        // Phase should be back near 0 (or 1.0 which wraps to 0)
        val phase = oscillator.phase
        assertTrue("Phase should be near 0 after one cycle, was $phase", phase < 0.01 || phase > 0.99)
    }

    @Test
    fun `frequency accuracy within 0_05Hz tolerance`() {
        val sampleRate = 48000
        val targetFrequency = 40.0
        val oscillator = SignalOscillator(sampleRate, targetFrequency)

        // Verify by checking phase after exactly N cycles worth of samples
        val cycles = 100
        val samplesPerCycle = sampleRate / targetFrequency
        val totalSamples = (samplesPerCycle * cycles).toInt()

        repeat(totalSamples) { oscillator.nextSample() }

        // Phase should be very close to 0 (completed exact number of cycles)
        val phase = oscillator.phase
        val phaseError = if (phase > 0.5) 1.0 - phase else phase

        // Phase error translates to frequency error
        // If we're off by 0.05Hz over 100 cycles, phase error would be ~0.125
        assertTrue(
            "Phase error $phaseError too large after $cycles cycles",
            phaseError < 0.01
        )
    }

    @Test
    fun `samples are within valid range`() {
        val oscillator = SignalOscillator()

        repeat(1000) {
            val sample = oscillator.nextSample()
            assertTrue("Sample $sample out of range", sample >= -1.0 && sample <= 1.0)
        }
    }

    @Test
    fun `fillBuffer produces correct number of samples`() {
        val oscillator = SignalOscillator()
        val buffer = ShortArray(1024)

        oscillator.fillBuffer(buffer)

        // All samples should be populated (non-zero for sine wave mid-cycle)
        val nonZeroCount = buffer.count { it != 0.toShort() }
        assertTrue("Expected most samples non-zero", nonZeroCount > 900)
    }

    @Test
    fun `reset returns phase to zero`() {
        val oscillator = SignalOscillator()

        repeat(500) { oscillator.nextSample() }
        assertTrue(oscillator.phase > 0)

        oscillator.reset()
        assertEquals(0.0, oscillator.phase, 0.0001)
    }

    @Test
    fun `amplitude scales output correctly`() {
        val oscillator = SignalOscillator()

        // Find peak at quarter cycle (where sin = 1)
        val samplesPerCycle = 48000 / 40
        repeat(samplesPerCycle / 4) { oscillator.nextSample(0.5) }

        val sample = oscillator.nextSample(0.5)
        assertTrue("Sample with 0.5 amplitude should be around 0.5, was $sample", abs(sample) <= 0.51)
    }
}
