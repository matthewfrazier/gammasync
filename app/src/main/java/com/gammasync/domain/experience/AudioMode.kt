package com.gammasync.domain.experience

/**
 * Audio stimulus modulation modes.
 */
enum class AudioMode {
    /**
     * Isochronic tones: Volume pulsing at target frequency.
     * Single channel (mono) with amplitude modulation.
     * Current default behavior in GammaSync.
     */
    ISOCHRONIC,

    /**
     * Binaural beats: Stereo frequency difference creates perception of beat.
     * Left ear: base frequency, Right ear: base + target Hz.
     * Requires headphones for proper effect.
     */
    BINAURAL,

    /**
     * Coupled oscillation: Gamma bursts nested inside theta cycles.
     * Used for Memory Write mode (theta-gamma coupling).
     * Gamma pulses only during theta peak (30% duty ratio).
     */
    COUPLED,

    /**
     * Silent: No audio stimulus, noise only.
     * Used for Migraine mode where audio may be triggering.
     */
    SILENT
}