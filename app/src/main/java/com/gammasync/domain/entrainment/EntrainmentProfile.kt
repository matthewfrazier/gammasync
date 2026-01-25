package com.gammasync.domain.entrainment


/**
 * Complete entrainment session configuration.
 * Combines audio mode, visual mode, frequency configuration, and noise settings.
 */
data class EntrainmentProfile(
    /**
     * The entrainment mode this profile implements.
     */
    val mode: EntrainmentMode,

    /**
     * Audio stimulus modulation mode.
     */
    val audioMode: AudioMode,

    /**
     * Visual stimulus rendering mode.
     */
    val visualMode: VisualMode,

    /**
     * Frequency configuration (fixed, ramping, or coupled).
     */
    val frequencyConfig: FrequencyConfig,

    /**
     * Background noise type.
     */
    val noiseType: NoiseType,

    /**
     * Whether audio should be stereo (required for binaural, split modes).
     */
    val isStereo: Boolean,

    /**
     * Visual appearance configuration (colors, brightness, etc.).
     */
    val visualConfig: VisualConfig,

    /**
     * Default session duration in minutes.
     */
    val defaultDurationMinutes: Int = 30
) {
    init {
        // Validate stereo requirement
        if (audioMode == AudioMode.BINAURAL) {
            require(isStereo) { "Binaural mode requires stereo audio" }
        }

        // Validate split mode requirements
        if (visualMode == VisualMode.SPLIT) {
            require(mode.requiresXreal) { "Split visual mode requires XREAL glasses" }
            require(frequencyConfig is FrequencyConfig.DualChannel) {
                "Split visual mode requires DualChannel frequency config"
            }
        }

        // Validate static mode has matching colors
        if (visualMode == VisualMode.STATIC) {
            require(visualConfig.primaryColor == visualConfig.secondaryColor) {
                "Static visual mode should have matching primary and secondary colors"
            }
        }

        // Validate focus relief mode safety
        if (mode == EntrainmentMode.FOCUS_RELIEF) {
            require(visualMode == VisualMode.STATIC) { "Focus relief mode must use STATIC visual mode (no flicker)" }
            require(audioMode == AudioMode.SILENT || audioMode == AudioMode.ISOCHRONIC) {
                "Focus relief mode should use SILENT or gentle ISOCHRONIC audio"
            }
        }
    }

    /**
     * Whether this profile requires XREAL glasses.
     */
    val requiresXreal: Boolean
        get() = mode.requiresXreal

    /**
     * Whether this profile uses stereo audio.
     */
    val hasStereoAudio: Boolean
        get() = isStereo

    /**
     * Whether this profile has visual flicker (for epilepsy warnings).
     */
    val hasVisualFlicker: Boolean
        get() = visualMode != VisualMode.STATIC
}