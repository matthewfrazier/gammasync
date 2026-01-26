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

package com.gammasync.infra

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Haptic feedback utility for crisp, short vibration pulses.
 * Provides tactile confirmation for UI interactions.
 */
class HapticFeedback(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Light tick - for button taps, selections
     */
    fun tick() {
        vibrate(VibrationEffect.EFFECT_TICK)
    }

    /**
     * Medium click - for confirmations
     */
    fun click() {
        vibrate(VibrationEffect.EFFECT_CLICK)
    }

    /**
     * Heavy click - for important actions (start/stop session)
     */
    fun heavyClick() {
        vibrate(VibrationEffect.EFFECT_HEAVY_CLICK)
    }

    /**
     * Double click - for errors/warnings
     */
    fun error() {
        vibrate(VibrationEffect.EFFECT_DOUBLE_CLICK)
    }

    private fun vibrate(effectId: Int) {
        if (!vibrator.hasVibrator()) return

        val effect = VibrationEffect.createPredefined(effectId)
        vibrator.vibrate(effect)
    }
}
