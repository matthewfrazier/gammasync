package com.gammasync.data

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences wrapper for persisting user settings.
 */
class SettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "gammasync_settings"
        private const val KEY_DURATION_MINUTES = "duration_minutes"
        private const val KEY_AUDIO_AMPLITUDE = "audio_amplitude"
        private const val KEY_MAX_BRIGHTNESS = "max_brightness"

        private const val DEFAULT_DURATION_MINUTES = 30
        private const val DEFAULT_AUDIO_AMPLITUDE = 0.3f
        private const val DEFAULT_MAX_BRIGHTNESS = false
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var durationMinutes: Int
        get() = prefs.getInt(KEY_DURATION_MINUTES, DEFAULT_DURATION_MINUTES)
        set(value) = prefs.edit().putInt(KEY_DURATION_MINUTES, value).apply()

    var audioAmplitude: Float
        get() = prefs.getFloat(KEY_AUDIO_AMPLITUDE, DEFAULT_AUDIO_AMPLITUDE)
        set(value) = prefs.edit().putFloat(KEY_AUDIO_AMPLITUDE, value).apply()

    var maxBrightness: Boolean
        get() = prefs.getBoolean(KEY_MAX_BRIGHTNESS, DEFAULT_MAX_BRIGHTNESS)
        set(value) = prefs.edit().putBoolean(KEY_MAX_BRIGHTNESS, value).apply()
}
