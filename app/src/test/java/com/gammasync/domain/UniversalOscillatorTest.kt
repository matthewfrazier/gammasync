package com.gammasync.domain

import com.gammasync.domain.entrainment.FrequencyConfig
import com.gammasync.domain.entrainment.NoiseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class UniversalOscillatorTest {

    @Test
    fun `fixed frequency - phase starts at zero`() {
        val oscillator = UniversalOscillator()
        oscillator.configure(FrequencyConfig.Fixed(40.0))
        assertEquals(0.0, oscillator.primaryPhase, 0.0001)
    }

    @Test
    fun `fixed frequency - phase cycles correctly at 40Hz`() {
        val sampleRate = 48000
        val frequency = 40.0
        val oscillator = UniversalOscillator(sampleRate)
        oscillator.configure(FrequencyConfig.Fixed(frequency))

        val samplesPerCycle = (sampleRate / frequency).toInt()

        // Generate one full cycle
        repeat(samplesPerCycle) { oscillator.nextSample() }

        // Phase should be back near 0 (or 1.0 which wraps to 0)
        val phase = oscillator.primaryPhase
        assertTrue("Phase should be near 0 after one cycle, was $phase", phase < 0.01 || phase > 0.99)
    }

    @Test
    fun `fixed frequency - samples within valid range`() {
        val oscillator = UniversalOscillator()
        oscillator.configure(FrequencyConfig.Fixed(40.0))

        repeat(1000) {
            val sample = oscillator.nextSample()
            assertTrue("Sample $sample out of range", sample >= -1.0 && sample <= 1.0)
        }
    }

    @Test
    fun `coupled mode - gamma only active during theta peak`() {
        val oscillator = UniversalOscillator()
        val config = FrequencyConfig.Coupled(
            carrierHz = 40.0,
            modulatorHz = 6.0,
            burstDutyRatio = 0.3f
        )
        oscillator.configure(config)

        // Collect samples over multiple theta cycles
        val sampleRate = 48000
        val thetaSamplesPerCycle = sampleRate / 6
        val totalSamples = thetaSamplesPerCycle * 3  // 3 theta cycles

        var silentInPeak = 0
        var activeOutsidePeak = 0

        repeat(totalSamples.toInt()) { i ->
            val sample = oscillator.nextSample()
            val thetaPhase = oscillator.secondaryPhase

            // Check if we're in the peak (first 30% of cycle)
            val inPeak = thetaPhase < 0.3

            if (inPeak && sample == 0.0) {
                silentInPeak++  // Should have some non-zero samples in peak
            }
            if (!inPeak && sample != 0.0) {
                activeOutsidePeak++  // Should be silent outside peak
            }
        }

        // Allow some tolerance for edge cases
        assertTrue("Too many silent samples in peak region: $silentInPeak", silentInPeak < 100)
        assertTrue("Too many active samples outside peak: $activeOutsidePeak", activeOutsidePeak < 100)
    }

    @Test
    fun `dual channel - left and right have different frequencies`() {
        val oscillator = UniversalOscillator()
        oscillator.configure(FrequencyConfig.DualChannel(leftHz = 18.0, rightHz = 10.0))

        // Generate enough samples to see phase difference
        repeat(4800) { // 0.1 seconds
            oscillator.nextStereoSample()
        }

        val leftPhase = oscillator.leftPhase
        val rightPhase = oscillator.rightPhase

        // Phases should be different due to different frequencies
        // After 0.1 seconds at 18Hz, left should have completed 1.8 cycles
        // After 0.1 seconds at 10Hz, right should have completed 1.0 cycles
        assertTrue("Left and right phases should differ", abs(leftPhase - rightPhase) > 0.1)
    }

    @Test
    fun `stereo buffer size must be even`() {
        val oscillator = UniversalOscillator()
        oscillator.configure(FrequencyConfig.Fixed(40.0))

        try {
            oscillator.fillBufferStereo(ShortArray(101)) // Odd size
            assertTrue("Should have thrown exception for odd buffer size", false)
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun `mono buffer with noise adds background`() {
        val oscillator = UniversalOscillator()
        oscillator.configure(FrequencyConfig.Fixed(40.0))

        val bufferWithNoise = ShortArray(1024)
        val bufferWithoutNoise = ShortArray(1024)

        oscillator.reset()
        oscillator.fillBufferMono(bufferWithNoise, amplitude = 0.5, noiseType = NoiseType.PINK)

        oscillator.reset()
        oscillator.fillBufferMono(bufferWithoutNoise, amplitude = 0.5, noiseType = NoiseType.NONE)

        // Buffers should be different due to noise
        var differences = 0
        for (i in bufferWithNoise.indices) {
            if (bufferWithNoise[i] != bufferWithoutNoise[i]) differences++
        }

        assertTrue("Buffers should differ when noise is added", differences > bufferWithNoise.size / 2)
    }

    @Test
    fun `reset clears phase and sample index`() {
        val oscillator = UniversalOscillator()
        oscillator.configure(FrequencyConfig.Fixed(40.0))

        repeat(500) { oscillator.nextSample() }
        assertTrue(oscillator.primaryPhase > 0)

        oscillator.reset()
        assertEquals(0.0, oscillator.primaryPhase, 0.0001)
    }

    @Test
    fun `binaural buffer generates stereo with frequency offset`() {
        val oscillator = UniversalOscillator()
        oscillator.configure(FrequencyConfig.Fixed(40.0))

        val buffer = ShortArray(2048)  // 1024 stereo samples
        oscillator.fillBufferBinaural(buffer, baseFrequency = 200.0, amplitude = 0.5)

        // Check that left and right channels are different
        var differences = 0
        for (i in 0 until buffer.size / 2) {
            if (buffer[i * 2] != buffer[i * 2 + 1]) differences++
        }

        assertTrue("Left and right channels should differ in binaural mode", differences > buffer.size / 4)
    }

    @Test
    fun `frequency accuracy within 0_05Hz tolerance`() {
        val sampleRate = 48000
        val targetFrequency = 40.0
        val oscillator = UniversalOscillator(sampleRate)
        oscillator.configure(FrequencyConfig.Fixed(targetFrequency))

        val cycles = 100
        val samplesPerCycle = sampleRate / targetFrequency
        val totalSamples = (samplesPerCycle * cycles).toInt()

        repeat(totalSamples) { oscillator.nextSample() }

        val phase = oscillator.primaryPhase
        val phaseError = if (phase > 0.5) 1.0 - phase else phase

        assertTrue("Phase error $phaseError too large after $cycles cycles", phaseError < 0.01)
    }
}
