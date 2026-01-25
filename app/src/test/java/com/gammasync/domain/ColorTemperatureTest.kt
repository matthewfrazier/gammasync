package com.cognihertz.domain

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ColorTemperatureTest {

    @Test
    fun `phase 0 returns warm color`() {
        val color = ColorTemperature.interpolate(0.0)
        assertEquals(ColorTemperature.WARM_2700K, color)
    }

    @Test
    fun `phase 0_5 returns cool color`() {
        val color = ColorTemperature.interpolate(0.5)
        assertEquals(ColorTemperature.COOL_6500K, color)
    }

    @Test
    fun `phase 1 returns warm color`() {
        val color = ColorTemperature.interpolate(1.0)
        assertEquals(ColorTemperature.WARM_2700K, color)
    }

    @Test
    fun `phase 0_25 returns midpoint color`() {
        val color = ColorTemperature.interpolate(0.25)

        // Should be halfway between warm and cool
        val expectedR = (Color.red(ColorTemperature.WARM_2700K) + Color.red(ColorTemperature.COOL_6500K)) / 2
        val expectedG = (Color.green(ColorTemperature.WARM_2700K) + Color.green(ColorTemperature.COOL_6500K)) / 2
        val expectedB = (Color.blue(ColorTemperature.WARM_2700K) + Color.blue(ColorTemperature.COOL_6500K)) / 2

        assertEquals(expectedR.toDouble(), Color.red(color).toDouble(), 1.0)
        assertEquals(expectedG.toDouble(), Color.green(color).toDouble(), 1.0)
        assertEquals(expectedB.toDouble(), Color.blue(color).toDouble(), 1.0)
    }

    @Test
    fun `interpolation is symmetric`() {
        // Phase 0.25 and 0.75 should produce same color (triangle wave)
        val color1 = ColorTemperature.interpolate(0.25)
        val color2 = ColorTemperature.interpolate(0.75)

        assertEquals(Color.red(color1), Color.red(color2))
        assertEquals(Color.green(color1), Color.green(color2))
        assertEquals(Color.blue(color1), Color.blue(color2))
    }
}
