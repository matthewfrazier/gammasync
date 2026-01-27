package com.gammasync.domain.experience

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperienceProfileTest {

    @Test
    fun `neurosync profile has correct configuration`() {
        val profile = ExperienceProfiles.NEUROSYNC

        assertEquals(ExperienceMode.NEUROSYNC, profile.mode)
        assertEquals(AudioMode.ISOCHRONIC, profile.audioMode)
        assertEquals(VisualMode.SINE, profile.visualMode)
        assertEquals(NoiseType.PINK, profile.noiseType)
        assertFalse(profile.isStereo)
        assertFalse(profile.requiresXreal)
        assertTrue(profile.hasVisualFlicker)

        val freq = profile.frequencyConfig as FrequencyConfig.Fixed
        assertEquals(40.0, freq.hz, 0.001)
    }

    @Test
    fun `memory write profile uses coupled frequency`() {
        val profile = ExperienceProfiles.MEMORY_WRITE

        assertEquals(ExperienceMode.MEMORY_WRITE, profile.mode)
        assertEquals(AudioMode.COUPLED, profile.audioMode)
        assertEquals(VisualMode.SINE, profile.visualMode)
        assertEquals(NoiseType.PINK, profile.noiseType)
        assertFalse(profile.isStereo)
        assertFalse(profile.requiresXreal)
        assertTrue(profile.hasVisualFlicker)
        assertTrue(profile.visualConfig.luminanceNoise)

        val freq = profile.frequencyConfig as FrequencyConfig.Coupled
        assertEquals(40.0, freq.carrierHz, 0.001)
        assertEquals(6.0, freq.modulatorHz, 0.001)
        assertEquals(0.3f, freq.burstDutyRatio)
    }

    @Test
    fun `sleep ramp profile uses binaural and frequency ramping`() {
        val profile = ExperienceProfiles.SLEEP_RAMP

        assertEquals(ExperienceMode.SLEEP_RAMP, profile.mode)
        assertEquals(AudioMode.BINAURAL, profile.audioMode)
        assertEquals(VisualMode.SINE, profile.visualMode)
        assertEquals(NoiseType.BROWN, profile.noiseType)
        assertTrue(profile.isStereo)
        assertFalse(profile.requiresXreal)
        assertTrue(profile.hasVisualFlicker)

        val freq = profile.frequencyConfig as FrequencyConfig.Ramp
        assertEquals(8.0, freq.startHz, 0.001)
        assertEquals(1.0, freq.endHz, 0.001)
    }

    @Test
    fun `migraine profile is static and safe`() {
        val profile = ExperienceProfiles.MIGRAINE

        assertEquals(ExperienceMode.MIGRAINE, profile.mode)
        assertEquals(AudioMode.SILENT, profile.audioMode)
        assertEquals(VisualMode.STATIC, profile.visualMode)
        assertEquals(NoiseType.BROWN, profile.noiseType)
        assertFalse(profile.isStereo)
        assertFalse(profile.requiresXreal)
        assertFalse(profile.hasVisualFlicker)  // Static = no flicker
    }

    @Test
    fun `mood lift profile requires xreal and uses split mode`() {
        val profile = ExperienceProfiles.MOOD_LIFT

        assertEquals(ExperienceMode.MOOD_LIFT, profile.mode)
        assertEquals(AudioMode.BINAURAL, profile.audioMode)
        assertEquals(VisualMode.SPLIT, profile.visualMode)
        assertEquals(NoiseType.PINK, profile.noiseType)
        assertTrue(profile.isStereo)
        assertTrue(profile.requiresXreal)
        assertTrue(profile.hasVisualFlicker)

        val freq = profile.frequencyConfig as FrequencyConfig.DualChannel
        assertEquals(18.0, freq.leftHz, 0.001)
        assertEquals(10.0, freq.rightHz, 0.001)
    }

    @Test
    fun `profile lookup by mode works`() {
        assertEquals(ExperienceProfiles.NEUROSYNC, ExperienceProfiles.forMode(ExperienceMode.NEUROSYNC))
        assertEquals(ExperienceProfiles.MEMORY_WRITE, ExperienceProfiles.forMode(ExperienceMode.MEMORY_WRITE))
        assertEquals(ExperienceProfiles.SLEEP_RAMP, ExperienceProfiles.forMode(ExperienceMode.SLEEP_RAMP))
        assertEquals(ExperienceProfiles.MIGRAINE, ExperienceProfiles.forMode(ExperienceMode.MIGRAINE))
        assertEquals(ExperienceProfiles.MOOD_LIFT, ExperienceProfiles.forMode(ExperienceMode.MOOD_LIFT))
    }

    @Test
    fun `all profiles returns correct count`() {
        assertEquals(5, ExperienceProfiles.all().size)
    }

    @Test
    fun `phone compatible excludes xreal-only profiles`() {
        val compatible = ExperienceProfiles.phoneCompatible()

        // Should include all except MOOD_LIFT
        assertTrue(compatible.contains(ExperienceProfiles.NEUROSYNC))
        assertTrue(compatible.contains(ExperienceProfiles.MEMORY_WRITE))
        assertTrue(compatible.contains(ExperienceProfiles.SLEEP_RAMP))
        assertTrue(compatible.contains(ExperienceProfiles.MIGRAINE))
        assertFalse(compatible.contains(ExperienceProfiles.MOOD_LIFT))
    }

    // --- Frequency Configuration Tests ---

    @Test
    fun `fixed frequency configuration is valid`() {
        val config = FrequencyConfig.Fixed(40.0)
        assertEquals(40.0, config.hz, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fixed frequency cannot be negative`() {
        FrequencyConfig.Fixed(-1.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fixed frequency cannot exceed 100Hz`() {
        FrequencyConfig.Fixed(120.0)
    }

    @Test
    fun `ramp frequency calculates correctly`() {
        val config = FrequencyConfig.Ramp(
            startHz = 8.0,
            endHz = 1.0,
            durationMs = 1000L
        )

        assertEquals(8.0, config.frequencyAt(0L), 0.001)
        assertEquals(4.5, config.frequencyAt(500L), 0.001)
        assertEquals(1.0, config.frequencyAt(1000L), 0.001)
        assertEquals(1.0, config.frequencyAt(2000L), 0.001)  // Clamps to end
    }

    @Test
    fun `coupled frequency gamma activity cycles correctly`() {
        val config = FrequencyConfig.Coupled(
            carrierHz = 40.0,
            modulatorHz = 6.0,
            burstDutyRatio = 0.3f
        )

        assertTrue(config.isGammaActive(0.0))    // Beginning of cycle
        assertTrue(config.isGammaActive(0.2))    // Within duty ratio
        assertFalse(config.isGammaActive(0.5))   // Outside duty ratio
        assertFalse(config.isGammaActive(0.9))   // Near end of cycle
    }

    @Test
    fun `dual channel configuration is valid`() {
        val config = FrequencyConfig.DualChannel(
            leftHz = 18.0,
            rightHz = 10.0
        )

        assertEquals(18.0, config.leftHz, 0.001)
        assertEquals(10.0, config.rightHz, 0.001)
    }

    // --- ExperienceMode Tests ---

    @Test
    fun `experience modes have correct xreal requirements`() {
        assertFalse(ExperienceMode.NEUROSYNC.requiresXreal)
        assertFalse(ExperienceMode.MEMORY_WRITE.requiresXreal)
        assertFalse(ExperienceMode.SLEEP_RAMP.requiresXreal)
        assertFalse(ExperienceMode.MIGRAINE.requiresXreal)
        assertTrue(ExperienceMode.MOOD_LIFT.requiresXreal)
    }

    @Test
    fun `experience modes have correct display names`() {
        assertEquals("Memory", ExperienceMode.NEUROSYNC.displayName)
        assertEquals("Learning", ExperienceMode.MEMORY_WRITE.displayName)
        assertEquals("Sleep Ramp", ExperienceMode.SLEEP_RAMP.displayName)
        assertEquals("Migraine Relief", ExperienceMode.MIGRAINE.displayName)
        assertEquals("Mood Lift", ExperienceMode.MOOD_LIFT.displayName)
    }
}