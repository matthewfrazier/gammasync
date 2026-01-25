package com.cognihertz.infra

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
