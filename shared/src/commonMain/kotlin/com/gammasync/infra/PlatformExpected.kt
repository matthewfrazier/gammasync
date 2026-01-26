package com.gammasync.infra

import com.gammasync.domain.SignalOscillator
import com.gammasync.domain.therapy.AudioMode
import com.gammasync.domain.therapy.VisualConfig

/**
 * Platform-specific audio engine implementation.
 * Android uses AudioTrack, iOS uses AudioUnit.
 */
expect class GammaAudioEngine() {
    fun initialize(): Boolean
    fun start(oscillator: SignalOscillator, amplitude: Double): Boolean
    fun stop()
    val phase: Double
    fun updateMode(mode: AudioMode)
    fun release()
}

/**
 * Platform-specific visual renderer implementation.
 * Android uses SurfaceView + Choreographer, iOS uses Metal + CADisplayLink.
 */
expect class GammaRenderer {
    fun initialize(): Boolean
    fun start(phaseProvider: () -> Double, visualConfig: VisualConfig): Boolean
    fun stop()
    fun updateConfig(config: VisualConfig)
    fun updateBounds(width: Int, height: Int)
    fun release()
}