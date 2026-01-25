package com.gammasync.domain.entrainment

/**
 * Visual stimulus configuration.
 *
 * Note: Uses raw ARGB integers to keep domain layer free of Android dependencies.
 * Format: 0xAARRGGBB (fully opaque = 0xFF prefix)
 */
data class VisualConfig(
    /**
     * Primary color for visual stimulus (ARGB format).
     * For SINE mode: warm color endpoint.
     * For STATIC mode: the displayed color.
     * For STROBE mode: on-state color.
     */
    val primaryColor: Int,

    /**
     * Secondary color for visual stimulus (ARGB format).
     * For SINE mode: cool color endpoint.
     * For STROBE mode: off-state color (typically black).
     */
    val secondaryColor: Int = COLOR_BLACK,

    /**
     * Maximum brightness in nits.
     * Float.MAX_VALUE means no limit (use system brightness).
     * Used for focus modes (<20 nits).
     */
    val maxBrightnessNits: Float = Float.MAX_VALUE,

    /**
     * Whether to apply luminance noise to prevent habituation.
     * Adds ±10% brightness jitter to the visual stimulus.
     */
    val luminanceNoise: Boolean = false
) {
    companion object {
        /** Black color constant */
        const val COLOR_BLACK = 0xFF000000.toInt()

        /**
         * Default isoluminant warm color (2700K incandescent).
         * RGB(255, 166, 80) = 0xFFA650
         */
        const val WARM_2700K = 0xFFFFA650.toInt()

        /**
         * Default isoluminant cool color (6500K daylight).
         * RGB(220, 220, 255) = 0xDCDCFF
         */
        const val COOL_6500K = 0xFFDCDCFF.toInt()

        /**
         * Focus green (525nm).
         * Low-saturation green that has been shown to enhance focus.
         * RGB(74, 139, 91) = 0x4A8B5B
         */
        const val FOCUS_GREEN = 0xFF4A8B5B.toInt()

        /**
         * Sleep red for low-light visibility.
         * RGB(139, 50, 50) = 0x8B3232
         */
        const val SLEEP_RED = 0xFF8B3232.toInt()

        /**
         * Default isoluminant configuration (warm ↔ cool).
         */
        val ISOLUMINANT = VisualConfig(
            primaryColor = WARM_2700K,
            secondaryColor = COOL_6500K
        )

        /**
         * Focus configuration (static green, low brightness).
         */
        val MIGRAINE = VisualConfig(
            primaryColor = FOCUS_GREEN,
            secondaryColor = FOCUS_GREEN,
            maxBrightnessNits = 20f
        )

        /**
         * Sleep configuration (red tint, luminance noise).
         */
        val SLEEP = VisualConfig(
            primaryColor = SLEEP_RED,
            secondaryColor = COLOR_BLACK,
            luminanceNoise = true
        )
    }
}