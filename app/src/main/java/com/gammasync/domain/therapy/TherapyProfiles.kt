package com.gammasync.domain.therapy

/**
 * Predefined therapy profiles based on research specifications.
 * Each profile is configured for its specific therapeutic purpose.
 */
object TherapyProfiles {

    /**
     * NeuroSync: 40Hz gamma entrainment.
     * - 40Hz isoluminant visual flicker (warm ↔ cool)
     * - 40Hz isochronic audio + pink noise
     * - Standard phone/XREAL display
     *
     * Research: MIT GENUS study (Iaccarino et al., 2016; Martorell et al., 2019)
     */
    val NEUROSYNC = TherapyProfile(
        mode = TherapyMode.NEUROSYNC,
        audioMode = AudioMode.ISOCHRONIC,
        visualMode = VisualMode.SINE,
        frequencyConfig = FrequencyConfig.Fixed(hz = 40.0),
        noiseType = NoiseType.PINK,
        isStereo = false,
        visualConfig = VisualConfig.ISOLUMINANT,
        defaultDurationMinutes = 30
    )

    /**
     * Memory Write: Theta-gamma coupling for memory consolidation.
     * - 6Hz visual pulse modulating gamma bursts
     * - Coupled audio: 40Hz gamma nested inside 6Hz theta
     * - Gamma active during 30% of theta cycle (peak phase)
     *
     * Research: Hippocampal theta-gamma phase-amplitude coupling studies
     */
    val MEMORY_WRITE = TherapyProfile(
        mode = TherapyMode.MEMORY_WRITE,
        audioMode = AudioMode.COUPLED,
        visualMode = VisualMode.SINE,
        frequencyConfig = FrequencyConfig.Coupled(
            carrierHz = 40.0,     // Gamma
            modulatorHz = 6.0,    // Theta
            burstDutyRatio = 0.3f // 30% duty cycle
        ),
        noiseType = NoiseType.PINK,
        isStereo = false,
        visualConfig = VisualConfig.ISOLUMINANT.copy(luminanceNoise = true),
        defaultDurationMinutes = 20
    )

    /**
     * Sleep Ramp: Progressive frequency reduction for sleep induction.
     * - Visual: 8Hz → 1Hz red sine wave (30 min ramp)
     * - Audio: Binaural beat ramp + brown noise
     *
     * Research: EEG-guided sleep onset studies
     */
    val SLEEP_RAMP = TherapyProfile(
        mode = TherapyMode.SLEEP_RAMP,
        audioMode = AudioMode.BINAURAL,
        visualMode = VisualMode.SINE,
        frequencyConfig = FrequencyConfig.Ramp(
            startHz = 8.0,                    // Alpha/theta border
            endHz = 1.0,                      // Deep delta
            durationMs = 30 * 60 * 1000L      // 30 minutes
        ),
        noiseType = NoiseType.BROWN,
        isStereo = true,
        visualConfig = VisualConfig.SLEEP,
        defaultDurationMinutes = 30
    )

    /**
     * Migraine Relief: Static green light therapy.
     * - No visual flicker (static 525nm green)
     * - Deep delta audio (1Hz) + brown noise for relaxation
     * - Low brightness (<20 nits)
     *
     * Research: Harvard Medical School green light migraine studies
     */
    val MIGRAINE = TherapyProfile(
        mode = TherapyMode.MIGRAINE,
        audioMode = AudioMode.SILENT,
        visualMode = VisualMode.STATIC,
        frequencyConfig = FrequencyConfig.Fixed(hz = 1.0), // Delta for audio if enabled
        noiseType = NoiseType.BROWN,
        isStereo = false,
        visualConfig = VisualConfig.MIGRAINE,
        defaultDurationMinutes = 60
    )

    /**
     * Mood Lift: Split-field asymmetric stimulation.
     * - Left eye/ear: 18Hz (frontal beta)
     * - Right eye/ear: 10Hz (alpha)
     * - Requires XREAL glasses for independent eye stimulation
     * - Dual binaural audio
     *
     * Research: Frontal alpha asymmetry and depression studies
     */
    val MOOD_LIFT = TherapyProfile(
        mode = TherapyMode.MOOD_LIFT,
        audioMode = AudioMode.BINAURAL,
        visualMode = VisualMode.SPLIT,
        frequencyConfig = FrequencyConfig.DualChannel(
            leftHz = 18.0,   // Frontal beta (left hemisphere activation)
            rightHz = 10.0   // Alpha (right hemisphere calming)
        ),
        noiseType = NoiseType.PINK,
        isStereo = true,
        visualConfig = VisualConfig.ISOLUMINANT,
        defaultDurationMinutes = 20
    )

    /**
     * Get profile for a given therapy mode.
     */
    fun forMode(mode: TherapyMode): TherapyProfile = when (mode) {
        TherapyMode.NEUROSYNC -> NEUROSYNC
        TherapyMode.MEMORY_WRITE -> MEMORY_WRITE
        TherapyMode.SLEEP_RAMP -> SLEEP_RAMP
        TherapyMode.MIGRAINE -> MIGRAINE
        TherapyMode.MOOD_LIFT -> MOOD_LIFT
    }

    /**
     * Get all available profiles.
     */
    fun all(): List<TherapyProfile> = listOf(
        NEUROSYNC,
        MEMORY_WRITE,
        SLEEP_RAMP,
        MIGRAINE,
        MOOD_LIFT
    )

    /**
     * Get profiles that don't require XREAL glasses.
     */
    fun phoneCompatible(): List<TherapyProfile> = all().filter { !it.requiresXreal }
}
