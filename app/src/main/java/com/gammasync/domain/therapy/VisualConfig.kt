package com.gammasync.domain.therapy

import android.graphics.Color

/**
 * Visual stimulus configuration.
 */
data class VisualConfig(
    /**
     * Primary color for visual stimulus.
     * For SINE mode: warm color endpoint.
     * For STATIC mode: the displayed color.
     * For STROBE mode: on-state color.
     */
    val primaryColor: Int,

    /**
     * Secondary color for visual stimulus.
     * For SINE mode: cool color endpoint.
     * For STROBE mode: off-state color (typically black).
     */
    val secondaryColor: Int = Color.BLACK,

    /**
     * Maximum brightness in nits.
     * Float.MAX_VALUE means no limit (use system brightness).
     * Used for Migraine mode (<20 nits).
     */
    val maxBrightnessNits: Float = Float.MAX_VALUE,

    /**
     * Whether to apply luminance noise to prevent habituation.
     * Adds ±10% brightness jitter to the visual stimulus.
     */
    val luminanceNoise: Boolean = false
) {
    companion object {
        /**
         * Default isoluminant warm color (2700K incandescent).
         */
        val WARM_2700K = Color.rgb(255, 166, 80)

        /**
         * Default isoluminant cool color (6500K daylight).
         */
        val COOL_6500K = Color.rgb(220, 220, 255)

        /**
         * Migraine green (525nm).
         * Low-saturation green that has been shown to reduce migraine symptoms.
         */
        val MIGRAINE_GREEN = Color.rgb(74, 139, 91)

        /**
         * Sleep red for low-light visibility.
         */
        val SLEEP_RED = Color.rgb(139, 50, 50)

        /**
         * Default isoluminant configuration (warm ↔ cool).
         */
        val ISOLUMINANT = VisualConfig(
            primaryColor = WARM_2700K,
            secondaryColor = COOL_6500K
        )

        /**
         * Migraine configuration (static green, low brightness).
         */
        val MIGRAINE = VisualConfig(
            primaryColor = MIGRAINE_GREEN,
            secondaryColor = MIGRAINE_GREEN,
            maxBrightnessNits = 20f
        )

        /**
         * Sleep configuration (red tint, luminance noise).
         */
        val SLEEP = VisualConfig(
            primaryColor = SLEEP_RED,
            secondaryColor = Color.BLACK,
            luminanceNoise = true
        )
    }
}
