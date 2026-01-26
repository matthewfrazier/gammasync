package com.gammasync.domain.therapy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TherapyProfileTest {

    @Test
    fun `neurosync profile has correct configuration`() {
        val profile = TherapyProfiles.NEUROSYNC

        assertEquals(TherapyMode.NEUROSYNC, profile.mode)
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
        val profile = TherapyProfiles.MEMORY_WRITE

        assertEquals(TherapyMode.MEMORY_WRITE, profile.mode)
        assertEquals(AudioMode.COUPLED, profile.audioMode)

        val freq = profile.frequencyConfig as FrequencyConfig.Coupled
        assertEquals(40.0, freq.carrierHz, 0.001)  // Gamma
        assertEquals(6.0, freq.modulatorHz, 0.001)  // Theta
        assertEquals(0.3f, freq.burstDutyRatio, 0.001f)
    }

    @Test
    fun `sleep ramp profile uses ramping frequency`() {
        val profile = TherapyProfiles.SLEEP_RAMP

        assertEquals(TherapyMode.SLEEP_RAMP, profile.mode)
        assertEquals(AudioMode.BINAURAL, profile.audioMode)
        assertTrue(profile.isStereo)

        val freq = profile.frequencyConfig as FrequencyConfig.Ramp
        assertEquals(8.0, freq.startHz, 0.001)
        assertEquals(1.0, freq.endHz, 0.001)
        assertEquals(30 * 60 * 1000L, freq.durationMs)  // 30 minutes
    }

    @Test
    fun `migraine profile has no flicker`() {
        val profile = TherapyProfiles.MIGRAINE

        assertEquals(TherapyMode.MIGRAINE, profile.mode)
        assertEquals(AudioMode.SILENT, profile.audioMode)
        assertEquals(VisualMode.STATIC, profile.visualMode)
        assertFalse(profile.hasVisualFlicker)
        assertTrue(profile.visualConfig.maxBrightnessNits < Float.MAX_VALUE)
    }

    @Test
    fun `mood lift profile requires xreal`() {
        val profile = TherapyProfiles.MOOD_LIFT

        assertEquals(TherapyMode.MOOD_LIFT, profile.mode)
        assertTrue(profile.requiresXreal)
        assertEquals(VisualMode.SPLIT, profile.visualMode)
        assertTrue(profile.isStereo)

        val freq = profile.frequencyConfig as FrequencyConfig.DualChannel
        assertEquals(18.0, freq.leftHz, 0.001)
        assertEquals(10.0, freq.rightHz, 0.001)
    }

    @Test
    fun `forMode returns correct profiles`() {
        assertEquals(TherapyProfiles.NEUROSYNC, TherapyProfiles.forMode(TherapyMode.NEUROSYNC))
        assertEquals(TherapyProfiles.MEMORY_WRITE, TherapyProfiles.forMode(TherapyMode.MEMORY_WRITE))
        assertEquals(TherapyProfiles.SLEEP_RAMP, TherapyProfiles.forMode(TherapyMode.SLEEP_RAMP))
        assertEquals(TherapyProfiles.MIGRAINE, TherapyProfiles.forMode(TherapyMode.MIGRAINE))
        assertEquals(TherapyProfiles.MOOD_LIFT, TherapyProfiles.forMode(TherapyMode.MOOD_LIFT))
    }

    @Test
    fun `all profiles list has 5 entries`() {
        assertEquals(5, TherapyProfiles.all().size)
    }

    @Test
    fun `phoneCompatible returns profiles without xreal requirement`() {
        val compatible = TherapyProfiles.phoneCompatible()

        assertEquals(4, compatible.size)
        assertTrue(compatible.none { it.requiresXreal })
        assertFalse(compatible.contains(TherapyProfiles.MOOD_LIFT))
    }

    // --- FrequencyConfig Tests ---

    @Test
    fun `frequency config fixed validation`() {
        val config = FrequencyConfig.Fixed(40.0)
        assertEquals(40.0, config.hz, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `frequency config rejects negative frequency`() {
        FrequencyConfig.Fixed(-10.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `frequency config rejects frequency over 100Hz`() {
        FrequencyConfig.Fixed(150.0)
    }

    @Test
    fun `ramp config calculates frequency correctly`() {
        val config = FrequencyConfig.Ramp(
            startHz = 8.0,
            endHz = 1.0,
            durationMs = 30 * 60 * 1000L  // 30 minutes
        )

        // At start
        assertEquals(8.0, config.frequencyAt(0), 0.001)

        // At 50%
        assertEquals(4.5, config.frequencyAt(15 * 60 * 1000L), 0.001)

        // At end
        assertEquals(1.0, config.frequencyAt(30 * 60 * 1000L), 0.001)

        // Past end (should clamp)
        assertEquals(1.0, config.frequencyAt(60 * 60 * 1000L), 0.001)
    }

    @Test
    fun `coupled config gamma active during theta peak`() {
        val config = FrequencyConfig.Coupled(
            carrierHz = 40.0,
            modulatorHz = 6.0,
            burstDutyRatio = 0.3f
        )

        // Active in first 30% of cycle
        assertTrue(config.isGammaActive(0.0))
        assertTrue(config.isGammaActive(0.15))
        assertTrue(config.isGammaActive(0.29))

        // Inactive after 30%
        assertFalse(config.isGammaActive(0.31))
        assertFalse(config.isGammaActive(0.5))
        assertFalse(config.isGammaActive(0.99))
    }

    // --- TherapyMode Tests ---

    @Test
    fun `therapy modes have correct xreal requirements`() {
        assertFalse(TherapyMode.NEUROSYNC.requiresXreal)
        assertFalse(TherapyMode.MEMORY_WRITE.requiresXreal)
        assertFalse(TherapyMode.SLEEP_RAMP.requiresXreal)
        assertFalse(TherapyMode.MIGRAINE.requiresXreal)
        assertTrue(TherapyMode.MOOD_LIFT.requiresXreal)
    }

    @Test
    fun `therapy modes have display names`() {
        assertEquals("Memory", TherapyMode.NEUROSYNC.displayName)
        assertEquals("Learning", TherapyMode.MEMORY_WRITE.displayName)
        assertEquals("Sleep Ramp", TherapyMode.SLEEP_RAMP.displayName)
        assertEquals("Migraine Relief", TherapyMode.MIGRAINE.displayName)
        assertEquals("Mood Lift", TherapyMode.MOOD_LIFT.displayName)
    }
}
