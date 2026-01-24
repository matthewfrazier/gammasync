import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for SignalOscillator class.
 * Verifies correct sine wave generation and 40Hz frequency accuracy.
 */
class SignalOscillatorTest {

    private lateinit var oscillator: SignalOscillator
    private val sampleRate = 48000
    private val targetFreq = 40.0
    private val tolerance = 0.02 // 2% tolerance for frequency detection

    @Before
    fun setUp() {
        oscillator = SignalOscillator(
            sampleRate = sampleRate,
            targetFreq = targetFreq
        )
    }

    @Test
    fun testInitialPhaseIsZero() {
        assertEquals(0f, oscillator.currentPhase, "Initial phase should be 0")
    }

    @Test
    fun testBufferFillsWithSineWave() {
        val bufferSize = 1024
        val buffer = FloatArray(bufferSize)

        oscillator.fillBuffer(buffer)

        // Verify buffer is filled (not all zeros)
        val nonZeroValues = buffer.count { it != 0f }
        assertTrue(nonZeroValues > 0, "Buffer should contain non-zero sine values")
    }

    @Test
    fun testPhaseIncrementPerSample() {
        val buffer = FloatArray(1)
        val initialPhase = oscillator.currentPhase

        oscillator.fillBuffer(buffer)

        val expectedPhaseIncrement = (targetFreq / sampleRate).toFloat()
        val actualIncrement = (oscillator.currentPhase - initialPhase + 1f) % 1f

        assertEquals(expectedPhaseIncrement, actualIncrement, 0.0001f,
            "Phase should increment by targetFreq/sampleRate per sample")
    }

    @Test
    fun testPhaseWrapsCorrectly() {
        // Fill buffer multiple times to accumulate phase
        val buffer = FloatArray(sampleRate) // 1 second of samples

        // First fill
        oscillator.fillBuffer(buffer)
        val phaseAfterFirstFill = oscillator.currentPhase

        // Phase should wrap around to [0, 1) range
        assertTrue(phaseAfterFirstFill in 0f..1f,
            "Phase should remain in range [0, 1)")
    }

    @Test
    fun testSineWaveAmplitude() {
        val buffer = FloatArray(1000)
        oscillator.fillBuffer(buffer)

        val maxValue = buffer.maxOrNull() ?: 0f
        val minValue = buffer.minOrNull() ?: 0f

        // Sine wave should oscillate between -1 and 1
        assertTrue(maxValue <= 1.0f, "Max amplitude should be <= 1.0")
        assertTrue(minValue >= -1.0f, "Min amplitude should be >= -1.0")
        assertTrue(maxValue > 0.9f, "Max amplitude should be close to 1.0")
        assertTrue(minValue < -0.9f, "Min amplitude should be close to -1.0")
    }

    @Test
    fun testFrequencyAccuracy() {
        // Generate 1 second of samples at 48kHz
        val durationSeconds = 1.0
        val bufferSize = (sampleRate * durationSeconds).toInt()
        val buffer = FloatArray(bufferSize)

        oscillator.fillBuffer(buffer)

        // Count zero crossings to estimate frequency
        var zeroCrossings = 0
        for (i in 1 until buffer.size) {
            // Detect zero crossing (sign change)
            if ((buffer[i - 1] < 0 && buffer[i] > 0) ||
                (buffer[i - 1] > 0 && buffer[i] < 0)) {
                zeroCrossings++
            }
        }

        // Each complete cycle has 2 zero crossings
        val detectedFrequency = zeroCrossings / (2.0 * durationSeconds)
        val frequencyError = abs(detectedFrequency - targetFreq) / targetFreq

        assertTrue(frequencyError < tolerance,
            "Detected frequency ($detectedFrequency Hz) should be within $tolerance of target ($targetFreq Hz). " +
            "Error: ${frequencyError * 100}%")
    }

    @Test
    fun testMultipleBufferFillsAreContiguous() {
        val buffer1 = FloatArray(512)
        val buffer2 = FloatArray(512)

        oscillator.fillBuffer(buffer1)
        oscillator.fillBuffer(buffer2)

        // The phase progression should be continuous across buffer fills
        // We can verify this by checking that the wave doesn't have discontinuities
        val lastValueBuffer1 = buffer1.last()
        val firstValueBuffer2 = buffer2.first()

        // Calculate expected next value based on phase progression
        val phaseIncrement = (targetFreq / sampleRate).toFloat()
        val expectedPhaseAfterBuffer1 = (buffer1.size * phaseIncrement) % 1f
        val expectedFirstValueBuffer2 = sin(expectedPhaseAfterBuffer1 * 2 * PI).toFloat()

        // Allow small difference due to floating point precision
        val difference = abs(firstValueBuffer2 - expectedFirstValueBuffer2)
        assertTrue(difference < 0.01f,
            "Buffer fills should produce contiguous phase progression")
    }

    @Test
    fun testCustomSampleRate() {
        val customSampleRate = 44100
        val customOscillator = SignalOscillator(
            sampleRate = customSampleRate,
            targetFreq = targetFreq
        )

        val buffer = FloatArray(customSampleRate) // 1 second
        customOscillator.fillBuffer(buffer)

        // Verify frequency with custom sample rate
        var zeroCrossings = 0
        for (i in 1 until buffer.size) {
            if ((buffer[i - 1] < 0 && buffer[i] > 0) ||
                (buffer[i - 1] > 0 && buffer[i] < 0)) {
                zeroCrossings++
            }
        }

        val detectedFrequency = zeroCrossings / 2.0
        val frequencyError = abs(detectedFrequency - targetFreq) / targetFreq

        assertTrue(frequencyError < tolerance,
            "Custom sample rate should maintain frequency accuracy")
    }

    @Test
    fun testCustomTargetFrequency() {
        val customFreq = 10.0
        val customOscillator = SignalOscillator(
            sampleRate = sampleRate,
            targetFreq = customFreq
        )

        val bufferSize = sampleRate * 2 // 2 seconds for lower frequency
        val buffer = FloatArray(bufferSize)
        customOscillator.fillBuffer(buffer)

        // Verify custom frequency
        var zeroCrossings = 0
        for (i in 1 until buffer.size) {
            if ((buffer[i - 1] < 0 && buffer[i] > 0) ||
                (buffer[i - 1] > 0 && buffer[i] < 0)) {
                zeroCrossings++
            }
        }

        val detectedFrequency = zeroCrossings / (2.0 * 2.0) // 2 seconds
        val frequencyError = abs(detectedFrequency - customFreq) / customFreq

        assertTrue(frequencyError < tolerance,
            "Custom target frequency should be generated accurately")
    }

    @Test
    fun testLargeBufferFill() {
        val largeBuffer = FloatArray(48000) // 1 second at 48kHz

        // Should not throw and should complete successfully
        oscillator.fillBuffer(largeBuffer)

        // Verify the buffer was actually modified
        assertTrue(largeBuffer.any { it != 0f },
            "Large buffer should be filled with non-zero values")
    }

    @Test
    fun testPhaseIsPrivatelySet() {
        val buffer = FloatArray(100)
        val initialPhase = oscillator.currentPhase

        oscillator.fillBuffer(buffer)

        // currentPhase should have changed
        assertTrue(oscillator.currentPhase != initialPhase,
            "currentPhase should be updated by fillBuffer")

        // Should not be able to directly set currentPhase (compile-time check)
        // This is verified by the private set modifier in the class
    }
}
