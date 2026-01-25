package com.gammasync.domain.noise

import kotlin.random.Random

/**
 * Brown noise (Brownian/red noise, 1/f² spectrum) generator.
 *
 * Brown noise is created by integrating white noise, producing a deep,
 * bass-heavy rumble similar to ocean waves or thunder. It has more
 * low-frequency content than pink noise and is often preferred for
 * sleep and relaxation.
 *
 * Implementation uses a simple random walk (integration of white noise)
 * with leak factor to prevent DC drift.
 */
class BrownNoiseGenerator(
    private val random: Random = Random.Default
) {
    // Current integrated value (the "position" in the random walk)
    private var currentValue = 0.0

    // Leak factor to prevent DC drift (slight pull toward zero)
    // Higher values = more high-frequency content, less bass
    private val leakFactor = 0.02

    // Step size for the random walk
    // Larger steps = more variance, but needs more normalization
    private val stepSize = 0.2

    // Soft clipping threshold
    private val clipThreshold = 0.95

    /**
     * Generate the next brown noise sample.
     * @return Sample value in range approximately -1.0 to 1.0
     */
    fun nextSample(): Double {
        // Generate white noise step
        val whiteNoise = random.nextDouble() * 2 - 1

        // Apply random walk with leak
        currentValue = currentValue * (1 - leakFactor) + whiteNoise * stepSize

        // Soft clip to prevent hard distortion
        return softClip(currentValue)
    }

    /**
     * Soft clipping function to keep output in range without harsh distortion.
     */
    private fun softClip(x: Double): Double {
        return when {
            x > clipThreshold -> clipThreshold + (1 - clipThreshold) * kotlin.math.tanh((x - clipThreshold) / (1 - clipThreshold))
            x < -clipThreshold -> -clipThreshold - (1 - clipThreshold) * kotlin.math.tanh((-x - clipThreshold) / (1 - clipThreshold))
            else -> x
        }
    }

    /**
     * Fill a buffer with brown noise samples.
     * @param buffer Array to fill with samples (-1.0 to 1.0 range)
     */
    fun fillBuffer(buffer: DoubleArray) {
        for (i in buffer.indices) {
            buffer[i] = nextSample()
        }
    }

    /**
     * Fill a buffer with brown noise samples scaled to Short range.
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
        currentValue = 0.0
    }
}
