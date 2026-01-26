package com.gammasync.data

import android.content.Context
import android.content.SharedPreferences
import com.gammasync.domain.therapy.TherapyMode

/**
 * SharedPreferences wrapper for persisting user settings.
 */
class SettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "gammasync_settings"
        private const val KEY_DURATION_MINUTES = "duration_minutes"
        private const val KEY_AUDIO_AMPLITUDE = "audio_amplitude"
        private const val KEY_MAX_BRIGHTNESS = "max_brightness"
        private const val KEY_THERAPY_MODE = "therapy_mode"
        private const val KEY_RSVP_ENABLED = "rsvp_enabled"
        private const val KEY_RSVP_WPM = "rsvp_wpm"
        private const val KEY_COLOR_SCHEME = "color_scheme"
        private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_BACKGROUND_NOISE = "background_noise"
        private const val KEY_RSVP_TEXT_SIZE = "rsvp_text_size"
        private const val KEY_RSVP_ANCHOR_HIGHLIGHT = "rsvp_anchor_highlight"
        private const val KEY_RSVP_ANNOTATIONS = "rsvp_annotations"

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

    var therapyMode: TherapyMode
        get() {
            val modeName = prefs.getString(KEY_THERAPY_MODE, TherapyMode.NEUROSYNC.name)
            return try {
                TherapyMode.valueOf(modeName ?: TherapyMode.NEUROSYNC.name)
            } catch (e: IllegalArgumentException) {
                TherapyMode.NEUROSYNC
            }
        }
        set(value) = prefs.edit().putString(KEY_THERAPY_MODE, value.name).apply()

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

    var backgroundNoiseEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_NOISE, true)
        set(value) = prefs.edit().putBoolean(KEY_BACKGROUND_NOISE, value).apply()

    /**
     * RSVP text size: SMALL (10%), MEDIUM (15%), LARGE (20%) of screen height.
     */
    var rsvpTextSize: RsvpTextSize
        get() {
            val sizeName = prefs.getString(KEY_RSVP_TEXT_SIZE, RsvpTextSize.MEDIUM.name)
            return try {
                RsvpTextSize.valueOf(sizeName ?: RsvpTextSize.MEDIUM.name)
            } catch (e: IllegalArgumentException) {
                RsvpTextSize.MEDIUM
            }
        }
        set(value) = prefs.edit().putString(KEY_RSVP_TEXT_SIZE, value.name).apply()

    /**
     * Whether to highlight the anchor letter (ORP - Optimal Recognition Point).
     */
    var rsvpAnchorHighlight: Boolean
        get() = prefs.getBoolean(KEY_RSVP_ANCHOR_HIGHLIGHT, true)
        set(value) = prefs.edit().putBoolean(KEY_RSVP_ANCHOR_HIGHLIGHT, value).apply()

    /**
     * Whether to show annotations (progress bar, word count).
     */
    var rsvpAnnotations: Boolean
        get() = prefs.getBoolean(KEY_RSVP_ANNOTATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_RSVP_ANNOTATIONS, value).apply()
}

/**
 * RSVP text size options as percentage of screen height.
 */
enum class RsvpTextSize(val heightPercent: Float) {
    SMALL(0.10f),
    MEDIUM(0.15f),
    LARGE(0.20f)
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
