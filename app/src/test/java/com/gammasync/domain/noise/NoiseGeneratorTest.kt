package com.gammasync.domain.noise

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NoiseGeneratorTest {

    // --- Pink Noise Tests ---

    @Test
    fun `pink noise - samples within valid range`() {
        val generator = PinkNoiseGenerator()

        repeat(10000) {
            val sample = generator.nextSample()
            assertTrue("Pink noise sample $sample out of range", sample >= -1.0 && sample <= 1.0)
        }
    }

    @Test
    fun `pink noise - has variance (not constant)`() {
        val generator = PinkNoiseGenerator()
        val samples = DoubleArray(1000) { generator.nextSample() }

        val mean = samples.average()
        val variance = samples.map { (it - mean) * (it - mean) }.average()

        assertTrue("Pink noise should have significant variance: $variance", variance > 0.01)
    }

    @Test
    fun `pink noise - fillBuffer produces correct length`() {
        val generator = PinkNoiseGenerator()
        val buffer = ShortArray(1024)

        generator.fillBuffer(buffer, amplitude = 0.5)

        // Check that buffer was filled (not all zeros)
        val nonZeroCount = buffer.count { it != 0.toShort() }
        assertTrue("Expected most samples non-zero in pink noise", nonZeroCount > 800)
    }

    @Test
    fun `pink noise - reset produces different sequence`() {
        val generator = PinkNoiseGenerator()

        val firstSequence = DoubleArray(100) { generator.nextSample() }
        generator.reset()
        val secondSequence = DoubleArray(100) { generator.nextSample() }

        // After reset with new random values, sequences should differ
        var differences = 0
        for (i in firstSequence.indices) {
            if (abs(firstSequence[i] - secondSequence[i]) > 0.01) differences++
        }

        // With reset using new random values, most samples should differ
        assertTrue("Reset should produce different sequence", differences > 50)
    }

    @Test
    fun `pink noise - amplitude scaling works`() {
        val generator = PinkNoiseGenerator()
        val buffer = ShortArray(1024)

        generator.fillBuffer(buffer, amplitude = 0.1)

        // With low amplitude, samples should be small
        val maxAbsValue = buffer.maxOfOrNull { abs(it.toInt()) } ?: 0
        val maxExpected = (Short.MAX_VALUE * 0.15).toInt()  // Allow some headroom

        assertTrue("With 0.1 amplitude, max should be below $maxExpected, was $maxAbsValue",
            maxAbsValue < maxExpected)
    }

    // --- Brown Noise Tests ---

    @Test
    fun `brown noise - samples within valid range`() {
        val generator = BrownNoiseGenerator()

        repeat(10000) {
            val sample = generator.nextSample()
            assertTrue("Brown noise sample $sample out of range", sample >= -1.0 && sample <= 1.0)
        }
    }

    @Test
    fun `brown noise - has variance (not constant)`() {
        val generator = BrownNoiseGenerator()
        val samples = DoubleArray(1000) { generator.nextSample() }

        val mean = samples.average()
        val variance = samples.map { (it - mean) * (it - mean) }.average()

        assertTrue("Brown noise should have significant variance: $variance", variance > 0.001)
    }

    @Test
    fun `brown noise - is smoother than white noise`() {
        val generator = BrownNoiseGenerator()
        val samples = DoubleArray(1000) { generator.nextSample() }

        // Calculate average absolute difference between consecutive samples
        var totalDiff = 0.0
        for (i in 1 until samples.size) {
            totalDiff += abs(samples[i] - samples[i - 1])
        }
        val avgDiff = totalDiff / (samples.size - 1)

        // Brown noise should be smoother (smaller consecutive differences) than variance
        val mean = samples.average()
        val variance = samples.map { (it - mean) * (it - mean) }.average()

        assertTrue("Brown noise should be smooth: avgDiff=$avgDiff, variance=$variance",
            avgDiff < variance * 5)
    }

    @Test
    fun `brown noise - fillBuffer produces correct length`() {
        val generator = BrownNoiseGenerator()
        val buffer = ShortArray(1024)

        generator.fillBuffer(buffer, amplitude = 0.5)

        // Check that buffer was filled
        val nonZeroCount = buffer.count { it != 0.toShort() }
        assertTrue("Expected most samples non-zero in brown noise", nonZeroCount > 800)
    }

    @Test
    fun `brown noise - reset returns to zero`() {
        val generator = BrownNoiseGenerator()

        // Generate some samples to move away from zero
        repeat(100) { generator.nextSample() }

        generator.reset()

        // After reset, the internal value should be at zero
        // Next sample should be close to zero (just one step from zero)
        val sample = generator.nextSample()
        assertTrue("After reset, first sample should be small: $sample", abs(sample) < 0.5)
    }

    // --- Comparative Tests ---

    @Test
    fun `pink and brown noise have different characteristics`() {
        val pink = PinkNoiseGenerator()
        val brown = BrownNoiseGenerator()

        val pinkSamples = DoubleArray(5000) { pink.nextSample() }
        val brownSamples = DoubleArray(5000) { brown.nextSample() }

        // Calculate consecutive sample differences
        fun avgDiff(samples: DoubleArray): Double {
            var total = 0.0
            for (i in 1 until samples.size) {
                total += abs(samples[i] - samples[i - 1])
            }
            return total / (samples.size - 1)
        }

        val pinkAvgDiff = avgDiff(pinkSamples)
        val brownAvgDiff = avgDiff(brownSamples)

        // Brown noise should be smoother (smaller consecutive differences)
        // because it's integrated white noise
        assertTrue("Brown noise ($brownAvgDiff) should be smoother than pink noise ($pinkAvgDiff)",
            brownAvgDiff < pinkAvgDiff)
    }
}
