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
