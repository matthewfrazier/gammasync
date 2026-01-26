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

package com.gammasync.domain

import android.graphics.Color

/**
 * Isoluminant color temperature values for visual entrainment.
 * Colors are calibrated to equal perceived luminance to avoid
 * brightness-based seizure triggers.
 */
object ColorTemperature {

    // Isoluminant pair (equal Rec.709 luminance ~0.75)
    val WARM_2700K = Color.rgb(255, 166, 80)   // Warm incandescent
    val COOL_6500K = Color.rgb(220, 220, 255)  // Cool daylight

    /**
     * Interpolate between warm and cool based on phase.
     * Creates smooth triangle wave oscillation.
     *
     * @param phase Audio phase from 0.0 to 1.0
     * @return Interpolated color
     */
    fun interpolate(phase: Double): Int {
        // Convert to 0.0→1.0→0.0 triangle wave
        val t = if (phase < 0.5) phase * 2 else (1.0 - phase) * 2

        val r = lerp(Color.red(WARM_2700K), Color.red(COOL_6500K), t)
        val g = lerp(Color.green(WARM_2700K), Color.green(COOL_6500K), t)
        val b = lerp(Color.blue(WARM_2700K), Color.blue(COOL_6500K), t)

        return Color.rgb(r, g, b)
    }

    private fun lerp(a: Int, b: Int, t: Double): Int {
        return (a + (b - a) * t).toInt().coerceIn(0, 255)
    }
}
