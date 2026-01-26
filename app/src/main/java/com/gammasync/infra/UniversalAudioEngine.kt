package com.gammasync.infra

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.VolumeShaper
import android.util.Log
import com.gammasync.domain.UniversalOscillator
import com.gammasync.domain.therapy.AudioMode
import com.gammasync.domain.therapy.NoiseType
import com.gammasync.domain.therapy.TherapyProfile
import kotlin.math.abs

/**
 * Universal audio engine supporting multiple therapy profiles.
 *
 * Extends the capabilities of GammaAudioEngine with:
 * - Stereo AudioTrack support (for binaural beats)
 * - Profile-based configuration
 * - Multiple phase providers for split mode
 * - Noise mixing
 *
 * This is the MASTER timing source - visual renderer polls phase from this.
 *
 * @param sampleRate Audio sample rate in Hz (typically 48000)
 */
class UniversalAudioEngine(
    private val sampleRate: Int = 48000
) {
    companion object {
        private const val TAG = "UniversalAudioEngine"
        private const val FADE_DURATION_MS = 30L
        private const val BINAURAL_BASE_FREQUENCY = 200.0  // Hz carrier for binaural beats
        private const val DEFAULT_NOISE_AMPLITUDE = 0.25
    }

    private val oscillator = UniversalOscillator(sampleRate)
    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null
    private var volumeShaper: VolumeShaper? = null

    // Current profile
    private var currentProfile: TherapyProfile? = null

    @Volatile
    private var isPlaying = false

    @Volatile
    private var playbackStartTimeNs: Long = 0

    @Volatile
    private var noiseEnabled = true

    // VolumeShaper configurations
    private val fadeInConfig = VolumeShaper.Configuration.Builder()
        .setDuration(FADE_DURATION_MS)
        .setCurve(floatArrayOf(0f, 1f), floatArrayOf(0f, 1f))
        .setInterpolatorType(VolumeShaper.Configuration.INTERPOLATOR_TYPE_LINEAR)
        .build()

    private val fadeOutConfig = VolumeShaper.Configuration.Builder()
        .setDuration(FADE_DURATION_MS)
        .setCurve(floatArrayOf(0f, 1f), floatArrayOf(1f, 0f))
        .setInterpolatorType(VolumeShaper.Configuration.INTERPOLATOR_TYPE_LINEAR)
        .build()

    // Diagnostics
    var discontinuityCount = 0
        private set
    var lastBufferWriteTimeNs = 0L
        private set
    var maxBufferGapMs = 0.0
        private set

    /**
     * Primary phase (0.0 to 1.0) for visual sync.
     * Uses wall-clock time for smooth continuous updates.
     */
    val phase: Double
        get() = primaryPhase

    /**
     * Primary phase for visual synchronization.
     * For coupled mode: gamma (carrier) phase.
     * For dual channel: left channel phase.
     */
    val primaryPhase: Double
        get() {
            if (!isPlaying) return 0.0
            val elapsedNs = System.nanoTime() - playbackStartTimeNs
            val elapsedSeconds = elapsedNs / 1_000_000_000.0
            val freq = oscillator.currentFrequency
            return (elapsedSeconds * freq) % 1.0
        }

    /**
     * Secondary phase for coupled/split modes.
     * For coupled mode: theta (modulator) phase.
     * For dual channel: right channel phase.
     */
    val secondaryPhase: Double
        get() {
            if (!isPlaying) return 0.0
            val elapsedNs = System.nanoTime() - playbackStartTimeNs
            val elapsedSeconds = elapsedNs / 1_000_000_000.0
            val freq = oscillator.secondaryFrequency
            return (elapsedSeconds * freq) % 1.0
        }

    /**
     * Left channel phase for split visual mode (same as primary).
     */
    val leftPhase: Double
        get() = primaryPhase

    /**
     * Right channel phase for split visual mode.
     */
    val rightPhase: Double
        get() = secondaryPhase

    /**
     * Current frequency being played.
     * For ramping modes, this changes over time.
     */
    val currentFrequency: Double
        get() = oscillator.currentFrequency

    /**
     * Whether audio is currently playing.
     */
    val playing: Boolean
        get() = isPlaying

    /**
     * Set whether background noise is enabled during playback.
     * Can be called during active session for runtime toggling.
     * @param enabled Whether to enable background noise
     */
    fun setNoiseEnabled(enabled: Boolean) {
        noiseEnabled = enabled
    }

    /**
     * Start audio playback with a therapy profile.
     * @param profile The therapy profile configuration
     * @param amplitude Volume from 0.0 to 1.0
     * @param noiseEnabled Whether to play background noise (default true)
     */
    fun start(profile: TherapyProfile, amplitude: Double = 0.5, noiseEnabled: Boolean = true) {
        if (isPlaying) return

        currentProfile = profile

        // Configure oscillator with profile's frequency config
        oscillator.configure(profile.frequencyConfig)

        // Determine channel configuration
        val channelConfig = if (profile.isStereo) {
            AudioFormat.CHANNEL_OUT_STEREO
        } else {
            AudioFormat.CHANNEL_OUT_MONO
        }

        val bytesPerSample = if (profile.isStereo) 4 else 2  // 2 bytes per sample * channels

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = minBufferSize * 4  // 4x for safety

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        isPlaying = true
        playbackStartTimeNs = System.nanoTime()
        this.noiseEnabled = noiseEnabled  // Initialize volatile flag
        oscillator.reset()
        discontinuityCount = 0
        maxBufferGapMs = 0.0

        // Create VolumeShaper for fade-in
        volumeShaper = audioTrack?.createVolumeShaper(fadeInConfig)

        audioTrack?.play()
        volumeShaper?.apply(VolumeShaper.Operation.PLAY)

        // Determine effective noise type based on setting
        val effectiveNoiseType = if (noiseEnabled) profile.noiseType else NoiseType.NONE

        Log.i(TAG, "Starting ${profile.mode.displayName} mode, stereo=${profile.isStereo}, " +
                "audioMode=${profile.audioMode}, noise=$effectiveNoiseType (setting=$noiseEnabled)")

        playbackThread = Thread {
            val samplesPerBuffer = bufferSize / bytesPerSample
            val buffer = ShortArray(if (profile.isStereo) samplesPerBuffer else samplesPerBuffer)
            var lastSample: Short = 0
            var writeCount = 0
            lastBufferWriteTimeNs = System.nanoTime()

            while (isPlaying) {
                val startNs = System.nanoTime()
                val gapMs = (startNs - lastBufferWriteTimeNs) / 1_000_000.0

                if (writeCount > 0 && gapMs > maxBufferGapMs) {
                    maxBufferGapMs = gapMs
                }

                // Fill buffer based on audio mode
                fillBuffer(buffer, profile, amplitude)

                // Check for discontinuities
                if (writeCount > 0) {
                    val jump = abs(buffer[0] - lastSample)
                    if (jump > 5000) {
                        discontinuityCount++
                        Log.w(TAG, "Discontinuity detected: jump=$jump at buffer $writeCount")
                    }
                }
                lastSample = buffer[buffer.size - 1]

                audioTrack?.write(buffer, 0, buffer.size)
                lastBufferWriteTimeNs = System.nanoTime()
                writeCount++
            }

            Log.i(TAG, "Playback stopped. Buffers=$writeCount, Discontinuities=$discontinuityCount, " +
                    "MaxGap=${maxBufferGapMs}ms")
        }.apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    /**
     * Start with legacy single-frequency mode (for backward compatibility).
     */
    fun start(amplitude: Double = 0.5) {
        // Use default NeuroSync profile
        start(com.gammasync.domain.therapy.TherapyProfiles.NEUROSYNC, amplitude)
    }

    private fun fillBuffer(buffer: ShortArray, profile: TherapyProfile, amplitude: Double) {
        val noiseAmplitude = DEFAULT_NOISE_AMPLITUDE
        val effectiveNoiseType = if (noiseEnabled) profile.noiseType else NoiseType.NONE

        when (profile.audioMode) {
            AudioMode.ISOCHRONIC -> {
                if (profile.isStereo) {
                    oscillator.fillBufferStereo(buffer, amplitude, effectiveNoiseType, noiseAmplitude)
                } else {
                    oscillator.fillBufferMono(buffer, amplitude, effectiveNoiseType, noiseAmplitude)
                }
            }

            AudioMode.BINAURAL -> {
                oscillator.fillBufferBinaural(
                    buffer,
                    BINAURAL_BASE_FREQUENCY,
                    amplitude,
                    effectiveNoiseType,
                    noiseAmplitude
                )
            }

            AudioMode.COUPLED -> {
                // Coupled mode uses the oscillator's built-in theta-gamma nesting
                if (profile.isStereo) {
                    oscillator.fillBufferStereo(buffer, amplitude, effectiveNoiseType, noiseAmplitude)
                } else {
                    oscillator.fillBufferMono(buffer, amplitude, effectiveNoiseType, noiseAmplitude)
                }
            }

            AudioMode.SILENT -> {
                // Only noise, no tone
                fillNoiseOnly(buffer, effectiveNoiseType, noiseAmplitude)
            }
        }
    }

    private fun fillNoiseOnly(buffer: ShortArray, noiseType: NoiseType, amplitude: Double) {
        // Use the oscillator to generate noise-only buffer
        // We call it with zero signal amplitude
        oscillator.fillBufferMono(buffer, 0.0, noiseType, amplitude)
    }

    /**
     * Stop audio playback with fade-out.
     */
    fun stop() {
        if (!isPlaying) return

        Log.i(TAG, "Stopping playback with ${FADE_DURATION_MS}ms fade-out")

        try {
            volumeShaper?.close()
            volumeShaper = audioTrack?.createVolumeShaper(fadeOutConfig)
            volumeShaper?.apply(VolumeShaper.Operation.PLAY)
            Thread.sleep(FADE_DURATION_MS + 10)
        } catch (e: Exception) {
            Log.w(TAG, "VolumeShaper fade-out failed: ${e.message}")
        }

        isPlaying = false
        playbackThread?.join(100)
        playbackThread = null

        audioTrack?.pause()
        audioTrack?.flush()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        volumeShaper = null
        currentProfile = null
    }

    /**
     * Release all resources.
     */
    fun release() {
        stop()
    }
}
