package com.gammasync.domain.entrainment

/**
 * Background noise types for audio comfort and masking.
 */
enum class NoiseType {
    /**
     * No background noise.
     */
    NONE,

    /**
     * Pink noise (1/f spectrum): Equal energy per octave.
     * Natural, comfortable sound similar to rainfall or wind.
     * Used with gamma entrainment (NeuroSync, Memory Write).
     */
    PINK,

    /**
     * Brown noise (1/f² spectrum): Deeper, bass-heavy rumble.
     * More relaxing, ocean-like sound.
     * Used with sleep and focus modes.
     */
    BROWN
}