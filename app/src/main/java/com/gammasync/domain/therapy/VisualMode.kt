package com.cognihertz.domain.therapy

/**
 * Visual stimulus rendering modes.
 */
enum class VisualMode {
    /**
     * Sine wave interpolation: Smooth transition between warm and cool colors.
     * Current default behavior using ColorTemperature.interpolate().
     * Maintains isoluminance for epilepsy safety.
     */
    SINE,

    /**
     * Strobe: Sharp on/off transitions at 50% duty cycle.
     * phase < 0.5 = warm color, phase >= 0.5 = off (black).
     * More intense stimulus than SINE.
     */
    STROBE,

    /**
     * Static: Single fixed color with no animation.
     * Used for Migraine mode (525nm green, no flicker).
     */
    STATIC,

    /**
     * Split-field: Independent colors for left/right halves of display.
     * Used for Mood Lift mode on XREAL (3840x1080 = 1920 + 1920).
     * Each eye receives different frequency stimulation.
     */
    SPLIT
}
