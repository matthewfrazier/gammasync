package com.cognihertz.domain.therapy

/**
 * Available therapy modes in the Universal Modulation Platform.
 *
 * @param displayName User-facing name for the mode
 * @param description Brief description of the therapy purpose
 * @param requiresXreal Whether this mode requires XREAL glasses (split-field)
 */
enum class TherapyMode(
    val displayName: String,
    val description: String,
    val requiresXreal: Boolean = false
) {
    /**
     * Memory: 40Hz gamma entrainment for memory and cellular cleanup.
     * Research basis: MIT GENUS study (Tsai et al., 2019)
     */
    NEUROSYNC(
        displayName = "Memory",
        description = "40Hz gamma + pink noise for memory and cellular cleanup"
    ),

    /**
     * Learning: Theta-gamma coupling for memory consolidation.
     * Research basis: Hippocampal memory encoding studies
     * Will support RSVP file display in future.
     */
    MEMORY_WRITE(
        displayName = "Learning",
        description = "Theta-gamma coupling for learning"
    ),

    /**
     * Sleep Ramp: Progressive frequency reduction for sleep induction.
     * Ramps from 8Hz (alpha/theta border) to 1Hz (delta) over 30 minutes.
     */
    SLEEP_RAMP(
        displayName = "Sleep Ramp",
        description = "8Hz→1Hz ramp for sleep"
    ),

    /**
     * Migraine Relief: Static green light therapy.
     * Research basis: Harvard Medical School green light studies
     * No flicker, 525nm green, <20 nits brightness.
     */
    MIGRAINE(
        displayName = "Migraine Relief",
        description = "Static green for pain relief"
    ),

    /**
     * Mood Lift: Split-field asymmetric stimulation.
     * Left: 18Hz (frontal beta), Right: 10Hz (alpha).
     * Requires XREAL glasses for independent eye stimulation.
     */
    MOOD_LIFT(
        displayName = "Mood Lift",
        description = "Split-field for mood enhancement",
        requiresXreal = true
    )
}
