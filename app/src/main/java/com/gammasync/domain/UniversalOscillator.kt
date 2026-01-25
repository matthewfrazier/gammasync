package com.gammasync.domain

import com.gammasync.domain.noise.BrownNoiseGenerator
import com.gammasync.domain.noise.PinkNoiseGenerator
import com.gammasync.domain.entrainment.FrequencyConfig
import com.gammasync.domain.entrainment.NoiseType
import kotlin.math.PI
import kotlin.math.sin

/**
 * Universal oscillator supporting multiple frequency modes and noise types.
 *
 * Extends the original SignalOscillator capabilities with:
 * - Fixed frequency (original behavior)
 * - Ramping frequency (e.g., 8Hz → 1Hz for sleep)
 * - Coupled frequency (theta-gamma nesting)
 * - Dual channel (independent left/right frequencies)
 * - Mono and stereo output
 * - Pink and brown noise generation
 *
 * This is the master clock source - video renderer polls phase from this.
 *
 * @param sampleRate Audio sample rate in Hz (typically 48000)
 */
class UniversalOscillator(
    private val sampleRate: Int = 48000
) {
    // Noise generators (lazily initialized)
    private val pinkNoise by lazy { PinkNoiseGenerator() }
    private val brownNoise by lazy { BrownNoiseGenerator() }

    // Sample tracking
    @Volatile
    private var sampleIndex: Long = 0

    // Session start time for ramping calculations
    private var sessionStartTimeMs: Long = 0

    // Current frequency configuration
    private var frequencyConfig: FrequencyConfig = FrequencyConfig.Fixed(40.0)

    /**
     * Configure the oscillator with a new frequency configuration.
     * Call this before starting a session.
     */
    fun configure(config: FrequencyConfig) {
        frequencyConfig = config
        sessionStartTimeMs = System.currentTimeMillis()
        reset()
    }

    /**
     * Current primary frequency in Hz.
     * For ramping mode, this is the current interpolated frequency.
     * For coupled mode, this is the carrier (gamma) frequency.
     * For dual channel, this is the left frequency.
     */
    val currentFrequency: Double
        get() = when (val config = frequencyConfig) {
            is FrequencyConfig.Fixed -> config.hz
            is FrequencyConfig.Ramp -> {
                val elapsedMs = System.currentTimeMillis() - sessionStartTimeMs
                config.frequencyAt(elapsedMs)
            }
            is FrequencyConfig.Coupled -> config.carrierHz
            is FrequencyConfig.DualChannel -> config.leftHz
        }

    /**
     * Secondary frequency for coupled/dual channel modes.
     * For coupled mode: modulator (theta) frequency.
     * For dual channel: right frequency.
     * For other modes: returns currentFrequency.
     */
    val secondaryFrequency: Double
        get() = when (val config = frequencyConfig) {
            is FrequencyConfig.Fixed -> config.hz
            is FrequencyConfig.Ramp -> currentFrequency
            is FrequencyConfig.Coupled -> config.modulatorHz
            is FrequencyConfig.DualChannel -> config.rightHz
        }

    /**
     * Primary phase (0.0 to 1.0) for visual sync.
     * Video renderer polls this to determine visual state.
     */
    val primaryPhase: Double
        get() {
            val freq = currentFrequency
            val samplesPerCycle = sampleRate / freq
            return (sampleIndex % samplesPerCycle.toLong()) / samplesPerCycle
        }

    /**
     * Secondary phase (0.0 to 1.0) for coupled/split modes.
     * For coupled mode: theta modulator phase.
     * For dual channel: right channel phase.
     */
    val secondaryPhase: Double
        get() {
            val freq = secondaryFrequency
            val samplesPerCycle = sampleRate / freq
            return (sampleIndex % samplesPerCycle.toLong()) / samplesPerCycle
        }

    /**
     * Left channel phase for split visual mode.
     */
    val leftPhase: Double
        get() = primaryPhase

    /**
     * Right channel phase for split visual mode.
     */
    val rightPhase: Double
        get() = when (frequencyConfig) {
            is FrequencyConfig.DualChannel -> secondaryPhase
            else -> primaryPhase
        }

    /**
     * Generate next audio sample as a sine wave.
     * @param amplitude Volume from 0.0 to 1.0
     * @return Sample value from -1.0 to 1.0
     */
    fun nextSample(amplitude: Double = 1.0): Double {
        val sample = when (val config = frequencyConfig) {
            is FrequencyConfig.Fixed -> {
                sin(2 * PI * config.hz * sampleIndex / sampleRate) * amplitude
            }
            is FrequencyConfig.Ramp -> {
                val freq = currentFrequency
                sin(2 * PI * freq * sampleIndex / sampleRate) * amplitude
            }
            is FrequencyConfig.Coupled -> {
                // Theta-gamma coupling: gamma bursts only during theta peak
                val thetaPhase = secondaryPhase
                val isGammaActive = config.isGammaActive(thetaPhase)
                if (isGammaActive) {
                    sin(2 * PI * config.carrierHz * sampleIndex / sampleRate) * amplitude
                } else {
                    0.0
                }
            }
            is FrequencyConfig.DualChannel -> {
                // For mono output of dual channel, mix both frequencies
                val left = sin(2 * PI * config.leftHz * sampleIndex / sampleRate)
                val right = sin(2 * PI * config.rightHz * sampleIndex / sampleRate)
                (left + right) / 2 * amplitude
            }
        }
        sampleIndex++
        return sample
    }

    /**
     * Generate next stereo sample pair.
     * @param amplitude Volume from 0.0 to 1.0
     * @return Pair of (left, right) sample values from -1.0 to 1.0
     */
    fun nextStereoSample(amplitude: Double = 1.0): Pair<Double, Double> {
        val result = when (val config = frequencyConfig) {
            is FrequencyConfig.Fixed -> {
                val sample = sin(2 * PI * config.hz * sampleIndex / sampleRate) * amplitude
                Pair(sample, sample)
            }
            is FrequencyConfig.Ramp -> {
                val freq = currentFrequency
                val sample = sin(2 * PI * freq * sampleIndex / sampleRate) * amplitude
                Pair(sample, sample)
            }
            is FrequencyConfig.Coupled -> {
                val thetaPhase = secondaryPhase
                val isGammaActive = config.isGammaActive(thetaPhase)
                if (isGammaActive) {
                    val sample = sin(2 * PI * config.carrierHz * sampleIndex / sampleRate) * amplitude
                    Pair(sample, sample)
                } else {
                    Pair(0.0, 0.0)
                }
            }
            is FrequencyConfig.DualChannel -> {
                val left = sin(2 * PI * config.leftHz * sampleIndex / sampleRate) * amplitude
                val right = sin(2 * PI * config.rightHz * sampleIndex / sampleRate) * amplitude
                Pair(left, right)
            }
        }
        sampleIndex++
        return result
    }

    /**
     * Fill a mono buffer with samples.
     * @param buffer Array to fill with samples
     * @param amplitude Volume from 0.0 to 1.0
     * @param noiseType Type of noise to mix in
     * @param noiseAmplitude Volume of noise (0.0 to 1.0)
     */
    fun fillBufferMono(
        buffer: ShortArray,
        amplitude: Double = 1.0,
        noiseType: NoiseType = NoiseType.NONE,
        noiseAmplitude: Double = 0.3
    ) {
        for (i in buffer.indices) {
            var sample = nextSample(amplitude)

            // Add noise if requested
            sample += when (noiseType) {
                NoiseType.NONE -> 0.0
                NoiseType.PINK -> pinkNoise.nextSample() * noiseAmplitude
                NoiseType.BROWN -> brownNoise.nextSample() * noiseAmplitude
            }

            // Clip and convert to Short
            val clipped = sample.coerceIn(-1.0, 1.0)
            buffer[i] = (clipped * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /**
     * Fill a stereo buffer with samples (interleaved L/R).
     * @param buffer Array to fill with samples (size must be even)
     * @param amplitude Volume from 0.0 to 1.0
     * @param noiseType Type of noise to mix in
     * @param noiseAmplitude Volume of noise (0.0 to 1.0)
     */
    fun fillBufferStereo(
        buffer: ShortArray,
        amplitude: Double = 1.0,
        noiseType: NoiseType = NoiseType.NONE,
        noiseAmplitude: Double = 0.3
    ) {
        require(buffer.size % 2 == 0) { "Stereo buffer size must be even" }

        for (i in 0 until buffer.size / 2) {
            val (left, right) = nextStereoSample(amplitude)

            // Generate noise (same for both channels for coherence)
            val noise = when (noiseType) {
                NoiseType.NONE -> 0.0
                NoiseType.PINK -> pinkNoise.nextSample() * noiseAmplitude
                NoiseType.BROWN -> brownNoise.nextSample() * noiseAmplitude
            }

            val leftSample = (left + noise).coerceIn(-1.0, 1.0)
            val rightSample = (right + noise).coerceIn(-1.0, 1.0)

            buffer[i * 2] = (leftSample * Short.MAX_VALUE).toInt().toShort()
            buffer[i * 2 + 1] = (rightSample * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /**
     * Fill a stereo buffer with binaural beats.
     * Creates the perception of a beat at the target frequency by playing
     * slightly different frequencies in each ear.
     *
     * @param buffer Array to fill with samples (interleaved L/R, size must be even)
     * @param baseFrequency Base carrier frequency (typically 200-400Hz)
     * @param amplitude Volume from 0.0 to 1.0
     * @param noiseType Type of noise to mix in
     * @param noiseAmplitude Volume of noise (0.0 to 1.0)
     */
    fun fillBufferBinaural(
        buffer: ShortArray,
        baseFrequency: Double = 200.0,
        amplitude: Double = 1.0,
        noiseType: NoiseType = NoiseType.NONE,
        noiseAmplitude: Double = 0.3
    ) {
        require(buffer.size % 2 == 0) { "Stereo buffer size must be even" }

        val targetFreq = currentFrequency

        // Left ear: base frequency
        // Right ear: base + target frequency (creates binaural beat)
        val leftFreq = baseFrequency
        val rightFreq = baseFrequency + targetFreq

        for (i in 0 until buffer.size / 2) {
            val left = sin(2 * PI * leftFreq * sampleIndex / sampleRate) * amplitude
            val right = sin(2 * PI * rightFreq * sampleIndex / sampleRate) * amplitude

            // Generate noise
            val noise = when (noiseType) {
                NoiseType.NONE -> 0.0
                NoiseType.PINK -> pinkNoise.nextSample() * noiseAmplitude
                NoiseType.BROWN -> brownNoise.nextSample() * noiseAmplitude
            }

            val leftSample = (left + noise).coerceIn(-1.0, 1.0)
            val rightSample = (right + noise).coerceIn(-1.0, 1.0)

            buffer[i * 2] = (leftSample * Short.MAX_VALUE).toInt().toShort()
            buffer[i * 2 + 1] = (rightSample * Short.MAX_VALUE).toInt().toShort()

            sampleIndex++
        }
        // Note: we manually increment sampleIndex in the loop for binaural
        // because we're not using nextSample() which auto-increments
    }

    /**
     * Reset oscillator to start of session.
     */
    fun reset() {
        sampleIndex = 0
        sessionStartTimeMs = System.currentTimeMillis()
        pinkNoise.reset()
        brownNoise.reset()
    }
}
