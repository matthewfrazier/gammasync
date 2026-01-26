/*
 * MIT License
 * Copyright (c) 2026 matthewfrazier
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gammasync.domain.therapy

/**
 * Complete therapy session configuration.
 * Combines audio mode, visual mode, frequency configuration, and noise settings.
 */
data class TherapyProfile(
    /**
     * The therapy mode this profile implements.
     */
    val mode: TherapyMode,

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

        // Validate migraine mode safety
        if (mode == TherapyMode.MIGRAINE) {
            require(visualMode == VisualMode.STATIC) { "Migraine mode must use STATIC visual mode (no flicker)" }
            require(audioMode == AudioMode.SILENT || audioMode == AudioMode.ISOCHRONIC) {
                "Migraine mode should use SILENT or gentle ISOCHRONIC audio"
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
