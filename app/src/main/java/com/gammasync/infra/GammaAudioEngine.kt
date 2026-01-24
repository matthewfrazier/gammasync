package com.gammasync.infra

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.gammasync.domain.SignalOscillator
import kotlin.math.abs

/**
 * Audio engine for 40Hz gamma entrainment.
 * Wraps AudioTrack and exposes phase for video synchronization.
 */
class GammaAudioEngine(
    private val sampleRate: Int = 48000,
    private val frequency: Double = 40.0
) {
    companion object {
        private const val TAG = "GammaAudioEngine"
    }

    private val oscillator = SignalOscillator(sampleRate, frequency)
    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null

    @Volatile
    private var isPlaying = false

    // Diagnostics
    var discontinuityCount = 0
        private set
    var lastBufferWriteTimeNs = 0L
        private set
    var maxBufferGapMs = 0.0
        private set

    /**
     * Current phase (0.0 to 1.0) for video sync.
     * Video renderer polls this to determine visual state.
     */
    val phase: Double
        get() = oscillator.phase

    /**
     * Whether audio is currently playing.
     */
    val playing: Boolean
        get() = isPlaying

    /**
     * Start audio playback.
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
        oscillator.reset()
        discontinuityCount = 0
        maxBufferGapMs = 0.0
        audioTrack?.play()

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
     * Stop audio playback.
     */
    fun stop() {
        isPlaying = false
        playbackThread?.join(100)
        playbackThread = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    /**
     * Release all resources.
     */
    fun release() {
        stop()
    }
}
