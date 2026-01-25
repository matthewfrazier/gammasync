package com.gammasync.domain.therapy

/**
 * Frequency configuration for audio/visual stimulus.
 * Supports fixed, ramping, and coupled frequency patterns.
 */
sealed class FrequencyConfig {

    /**
     * Fixed frequency throughout the session.
     * @param hz Target frequency in Hertz
     */
    data class Fixed(val hz: Double) : FrequencyConfig() {
        init {
            require(hz > 0) { "Frequency must be positive" }
            require(hz <= 100) { "Frequency must not exceed 100Hz for safety" }
        }
    }

    /**
     * Ramping frequency that transitions linearly over the session.
     * @param startHz Starting frequency in Hertz
     * @param endHz Ending frequency in Hertz
     * @param durationMs Duration of the ramp in milliseconds
     */
    data class Ramp(
        val startHz: Double,
        val endHz: Double,
        val durationMs: Long
    ) : FrequencyConfig() {
        init {
            require(startHz > 0) { "Start frequency must be positive" }
            require(endHz > 0) { "End frequency must be positive" }
            require(startHz <= 100 && endHz <= 100) { "Frequencies must not exceed 100Hz for safety" }
            require(durationMs > 0) { "Duration must be positive" }
        }

        /**
         * Calculate the current frequency at a given elapsed time.
         * @param elapsedMs Elapsed time in milliseconds
         * @return Current frequency, clamped to endHz after duration
         */
        fun frequencyAt(elapsedMs: Long): Double {
            if (elapsedMs >= durationMs) return endHz
            val progress = elapsedMs.toDouble() / durationMs
            return startHz + (endHz - startHz) * progress
        }
    }

    /**
     * Coupled frequency for theta-gamma nesting.
     * Gamma bursts occur only during the peak portion of each theta cycle.
     *
     * @param carrierHz The carrier (gamma) frequency in Hertz
     * @param modulatorHz The modulator (theta) frequency in Hertz
     * @param burstDutyRatio Portion of theta cycle during which gamma is active (0.0-1.0)
     */
    data class Coupled(
        val carrierHz: Double,
        val modulatorHz: Double,
        val burstDutyRatio: Float = 0.3f
    ) : FrequencyConfig() {
        init {
            require(carrierHz > 0) { "Carrier frequency must be positive" }
            require(modulatorHz > 0) { "Modulator frequency must be positive" }
            require(carrierHz <= 100) { "Carrier frequency must not exceed 100Hz for safety" }
            require(modulatorHz <= 100) { "Modulator frequency must not exceed 100Hz for safety" }
            require(burstDutyRatio in 0f..1f) { "Burst duty ratio must be between 0 and 1" }
        }

        /**
         * Check if gamma should be active at the current theta phase.
         * Gamma is active during the peak (rising) portion of the theta cycle.
         * @param thetaPhase Current theta phase (0.0-1.0)
         * @return True if gamma burst should be active
         */
        fun isGammaActive(thetaPhase: Double): Boolean {
            // Active during the first portion of the cycle (peak phase)
            return thetaPhase < burstDutyRatio
        }
    }

    /**
     * Dual independent frequencies for split-field mode.
     * Left and right eyes receive different frequency stimulation.
     *
     * @param leftHz Frequency for left eye/ear
     * @param rightHz Frequency for right eye/ear
     */
    data class DualChannel(
        val leftHz: Double,
        val rightHz: Double
    ) : FrequencyConfig() {
        init {
            require(leftHz > 0 && rightHz > 0) { "Frequencies must be positive" }
            require(leftHz <= 100 && rightHz <= 100) { "Frequencies must not exceed 100Hz for safety" }
        }
    }
}
