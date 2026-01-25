package com.gammasync.domain.entrainment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntrainmentProfileTest {

    @Test
    fun `neurosync profile has correct configuration`() {
        val profile = EntrainmentProfiles.NEUROSYNC

        assertEquals(EntrainmentMode.NEUROSYNC, profile.mode)
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
        val profile = EntrainmentProfiles.MEMORY_WRITE

        assertEquals(EntrainmentMode.MEMORY_WRITE, profile.mode)
        assertEquals(AudioMode.COUPLED, profile.audioMode)

        val freq = profile.frequencyConfig as FrequencyConfig.Coupled
        assertEquals(40.0, freq.carrierHz, 0.001)  // Gamma
        assertEquals(6.0, freq.modulatorHz, 0.001)  // Theta
        assertEquals(0.3f, freq.burstDutyRatio, 0.001f)
    }

    @Test
    fun `sleep ramp profile uses ramping frequency`() {
        val profile = EntrainmentProfiles.SLEEP_RAMP

        assertEquals(EntrainmentMode.SLEEP_RAMP, profile.mode)
        assertEquals(AudioMode.BINAURAL, profile.audioMode)

        val freq = profile.frequencyConfig as FrequencyConfig.Ramp
        assertEquals(8.0, freq.startHz, 0.001)
        assertEquals(1.0, freq.endHz, 0.001)
        assertEquals(30 * 60 * 1000L, freq.durationMs)  // 30 minutes
    }

    @Test
    fun `focus relief profile uses static visual mode`() {
        val profile = EntrainmentProfiles.FOCUS_RELIEF

        assertEquals(EntrainmentMode.FOCUS_RELIEF, profile.mode)
        assertEquals(AudioMode.SILENT, profile.audioMode)
        assertEquals(VisualMode.STATIC, profile.visualMode)
        assertFalse(profile.hasVisualFlicker)  // Static mode = no flicker
    }

    @Test
    fun `mood enhancement profile requires xreal`() {
        val profile = EntrainmentProfiles.MOOD_ENHANCEMENT

        assertEquals(EntrainmentMode.MOOD_ENHANCEMENT, profile.mode)
        assertEquals(AudioMode.BINAURAL, profile.audioMode)
        assertEquals(VisualMode.SPLIT, profile.visualMode)
        assertTrue(profile.requiresXreal)
        assertTrue(profile.hasStereoAudio)

        val freq = profile.frequencyConfig as FrequencyConfig.DualChannel
        assertEquals(18.0, freq.leftHz, 0.001)
        assertEquals(10.0, freq.rightHz, 0.001)
    }

    @Test
    fun `mode lookup works correctly`() {
        assertEquals(EntrainmentProfiles.NEUROSYNC, EntrainmentProfiles.forMode(EntrainmentMode.NEUROSYNC))
        assertEquals(EntrainmentProfiles.MEMORY_WRITE, EntrainmentProfiles.forMode(EntrainmentMode.MEMORY_WRITE))
        assertEquals(EntrainmentProfiles.SLEEP_RAMP, EntrainmentProfiles.forMode(EntrainmentMode.SLEEP_RAMP))
        assertEquals(EntrainmentProfiles.FOCUS_RELIEF, EntrainmentProfiles.forMode(EntrainmentMode.FOCUS_RELIEF))
        assertEquals(EntrainmentProfiles.MOOD_ENHANCEMENT, EntrainmentProfiles.forMode(EntrainmentMode.MOOD_ENHANCEMENT))
    }

    @Test
    fun `all profiles returns correct count`() {
        assertEquals(5, EntrainmentProfiles.all().size)
    }

    @Test
    fun `phone compatible excludes xreal-only profiles`() {
        val compatible = EntrainmentProfiles.phoneCompatible()

        assertTrue(compatible.contains(EntrainmentProfiles.NEUROSYNC))
        assertTrue(compatible.contains(EntrainmentProfiles.MEMORY_WRITE))
        assertTrue(compatible.contains(EntrainmentProfiles.SLEEP_RAMP))
        assertTrue(compatible.contains(EntrainmentProfiles.FOCUS_RELIEF))
        assertFalse(compatible.contains(EntrainmentProfiles.MOOD_ENHANCEMENT))
    }

    @Test
    fun `profile validation prevents invalid split mode on basic devices`() {
        // This would fail validation
        try {
            EntrainmentProfile(
                mode = EntrainmentMode.NEUROSYNC,  // Doesn't require XREAL
                audioMode = AudioMode.ISOCHRONIC,
                visualMode = VisualMode.SPLIT,  // But using split mode
                frequencyConfig = FrequencyConfig.Fixed(40.0),
                noiseType = NoiseType.PINK,
                isStereo = false,
                visualConfig = VisualConfig.ISOLUMINANT
            )
            assertFalse("Should have thrown validation error", true)
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun `profile validation enforces binaural stereo requirement`() {
        // This would fail validation
        try {
            EntrainmentProfile(
                mode = EntrainmentMode.NEUROSYNC,
                audioMode = AudioMode.BINAURAL,  // Requires stereo
                visualMode = VisualMode.SINE,
                frequencyConfig = FrequencyConfig.Fixed(40.0),
                noiseType = NoiseType.PINK,
                isStereo = false,  // But not stereo
                visualConfig = VisualConfig.ISOLUMINANT
            )
            assertFalse("Should have thrown validation error", true)
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    // --- EntrainmentMode Tests ---

    @Test
    fun `neurosync mode has correct display properties`() {
        assertEquals("Memory", EntrainmentMode.NEUROSYNC.displayName)
        assertEquals("40Hz gamma + pink noise for memory and cellular cleanup", EntrainmentMode.NEUROSYNC.description)
        assertFalse(EntrainmentMode.NEUROSYNC.requiresXreal)
    }

    @Test
    fun `mood enhancement mode requires xreal`() {
        assertEquals("Mood Enhancement", EntrainmentMode.MOOD_ENHANCEMENT.displayName)
        assertTrue(EntrainmentMode.MOOD_ENHANCEMENT.requiresXreal)
    }

    @Test
    fun `all modes have non-empty display names`() {
        for (mode in EntrainmentMode.values()) {
            assertTrue("Mode ${mode.name} has empty display name", mode.displayName.isNotEmpty())
            assertTrue("Mode ${mode.name} has empty description", mode.description.isNotEmpty())
        }
    }
}