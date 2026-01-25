package com.gammasync.data

import android.content.Context
import android.content.SharedPreferences
import com.gammasync.domain.entrainment.EntrainmentMode

/**
 * SharedPreferences wrapper for persisting user settings.
 */
class SettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "gammasync_settings"
        private const val KEY_DURATION_MINUTES = "duration_minutes"
        private const val KEY_AUDIO_AMPLITUDE = "audio_amplitude"
        private const val KEY_MAX_BRIGHTNESS = "max_brightness"
        private const val KEY_ENTRAINMENT_MODE = "entrainment_mode"
        private const val KEY_RSVP_ENABLED = "rsvp_enabled"
        private const val KEY_RSVP_WPM = "rsvp_wpm"
        private const val KEY_COLOR_SCHEME = "color_scheme"
        private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
        private const val KEY_DARK_MODE = "dark_mode"

        private const val DEFAULT_DURATION_MINUTES = 30
        private const val DEFAULT_AUDIO_AMPLITUDE = 0.3f
        private const val DEFAULT_MAX_BRIGHTNESS = false
        private const val DEFAULT_RSVP_WPM = 300
        private const val DEFAULT_COLOR_SCHEME = "TEAL"
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

    var entrainmentMode: EntrainmentMode
        get() {
            val modeName = prefs.getString(KEY_ENTRAINMENT_MODE, EntrainmentMode.NEUROSYNC.name)
            return try {
                EntrainmentMode.valueOf(modeName ?: EntrainmentMode.NEUROSYNC.name)
            } catch (e: IllegalArgumentException) {
                EntrainmentMode.NEUROSYNC
            }
        }
        set(value) = prefs.edit().putString(KEY_ENTRAINMENT_MODE, value.name).apply()

    var rsvpEnabled: Boolean
        get() = prefs.getBoolean(KEY_RSVP_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_RSVP_ENABLED, value).apply()

    var rsvpWpm: Int
        get() = prefs.getInt(KEY_RSVP_WPM, DEFAULT_RSVP_WPM)
        set(value) = prefs.edit().putInt(KEY_RSVP_WPM, value).apply()

    var colorScheme: ColorScheme
        get() {
            val schemeName = prefs.getString(KEY_COLOR_SCHEME, DEFAULT_COLOR_SCHEME)
            return try {
                ColorScheme.valueOf(schemeName ?: DEFAULT_COLOR_SCHEME)
            } catch (e: IllegalArgumentException) {
                ColorScheme.TEAL
            }
        }
        set(value) = prefs.edit().putString(KEY_COLOR_SCHEME, value.name).apply()

    var disclaimerAccepted: Boolean
        get() = prefs.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_DISCLAIMER_ACCEPTED, value).apply()

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
}

/**
 * Available color schemes for the app UI.
 */
enum class ColorScheme(val accentColor: Int) {
    TEAL(0xFF26A69A.toInt()),
    BLUE(0xFF42A5F5.toInt()),
    PURPLE(0xFFAB47BC.toInt()),
    GREEN(0xFF66BB6A.toInt()),
    ORANGE(0xFFFFA726.toInt()),
    RED(0xFFEF5350.toInt())
}
