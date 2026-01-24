package com.gammasync.infra

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.VolumeShaper
import android.util.Log
import com.gammasync.domain.SignalOscillator
import kotlin.math.abs

/**
 * Audio engine for 40Hz gamma entrainment.
 * Wraps AudioTrack and exposes phase for video synchronization.
 * Uses VolumeShaper for click-free start/stop transitions.
 */
class GammaAudioEngine(
    private val sampleRate: Int = 48000,
    private val frequency: Double = 40.0
) {
    companion object {
        private const val TAG = "GammaAudioEngine"
        private const val FADE_DURATION_MS = 30L  // Imperceptible but click-free
    }

    private val oscillator = SignalOscillator(sampleRate, frequency)
    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null
    private var volumeShaper: VolumeShaper? = null

    @Volatile
    private var isPlaying = false

    @Volatile
    private var playbackStartTimeNs: Long = 0

    // VolumeShaper configurations for smooth transitions
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
     * Current phase (0.0 to 1.0) for video sync.
     * Uses wall-clock time for smooth continuous updates.
     * Video renderer polls this to determine visual state.
     */
    val phase: Double
        get() {
            if (!isPlaying) return 0.0
            val elapsedNs = System.nanoTime() - playbackStartTimeNs
            val elapsedSeconds = elapsedNs / 1_000_000_000.0
            return (elapsedSeconds * frequency) % 1.0
        }

    /**
     * Whether audio is currently playing.
     */
    val playing: Boolean
        get() = isPlaying

    /**
     * Start audio playback with fade-in.
     * @param amplitude Volume from 0.0 to 1.0
     */
    fun start(amplitude: Double = 0.5) {
        if (isPlaying) return

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // Use 4x min buffer to reduce underruns
        val bufferSize = minBufferSize * 4

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
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        isPlaying = true
        playbackStartTimeNs = System.nanoTime()
        oscillator.reset()
        discontinuityCount = 0
        maxBufferGapMs = 0.0

        // Create VolumeShaper for fade-in
        volumeShaper = audioTrack?.createVolumeShaper(fadeInConfig)

        audioTrack?.play()
        volumeShaper?.apply(VolumeShaper.Operation.PLAY)  // Start fade-in

        Log.i(TAG, "Starting playback with ${FADE_DURATION_MS}ms fade-in")

        playbackThread = Thread {
            val buffer = ShortArray(bufferSize / 2)
            var lastSample: Short = 0
            var writeCount = 0
            lastBufferWriteTimeNs = System.nanoTime()

            while (isPlaying) {
                val startNs = System.nanoTime()
                val gapMs = (startNs - lastBufferWriteTimeNs) / 1_000_000.0

                // Detect timing gaps (potential underrun)
                if (writeCount > 0 && gapMs > maxBufferGapMs) {
                    maxBufferGapMs = gapMs
                }

                oscillator.fillBuffer(buffer, amplitude)

                // Check for discontinuities in generated signal
                if (writeCount > 0) {
                    val jump = abs(buffer[0] - lastSample)
                    // Large jump between buffers indicates discontinuity
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

            Log.i(TAG, "Playback stopped. Buffers=$writeCount, Discontinuities=$discontinuityCount, MaxGap=${maxBufferGapMs}ms")
        }.apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    /**
     * Stop audio playback with fade-out.
     */
    fun stop() {
        if (!isPlaying) return

        Log.i(TAG, "Stopping playback with ${FADE_DURATION_MS}ms fade-out")

        // Apply fade-out
        try {
            volumeShaper?.close()
            volumeShaper = audioTrack?.createVolumeShaper(fadeOutConfig)
            volumeShaper?.apply(VolumeShaper.Operation.PLAY)

            // Wait for fade to complete
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
    }

    /**
     * Release all resources.
     */
    fun release() {
        stop()
    }
}
