package com.gammasync.data

import android.content.Context
import android.content.SharedPreferences
import com.gammasync.domain.rsvp.RsvpSettings
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
        private const val KEY_RSVP_DOCUMENT_URI = "rsvp_document_uri"
        private const val KEY_RSVP_DOCUMENT_NAME = "rsvp_document_name"
        private const val KEY_RSVP_DOCUMENT_WORD_COUNT = "rsvp_document_word_count"
        private const val KEY_RSVP_TEXT_SIZE_PERCENT = "rsvp_text_size_percent"
        private const val KEY_RSVP_HYPHENATION = "rsvp_hyphenation"
        private const val KEY_RSVP_MULTI_LINE = "rsvp_multi_line"
        private const val KEY_RSVP_ORP_HIGHLIGHT = "rsvp_orp_highlight"
        private const val KEY_RSVP_MAX_GLIMPSE_CHARS = "rsvp_max_glimpse_chars"
        private const val KEY_RSVP_MAX_GLIMPSE_WORDS = "rsvp_max_glimpse_words"
        private const val KEY_RSVP_RESUME_WORD_INDEX = "rsvp_resume_word_index"

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

    var rsvpDocumentUri: String?
        get() = prefs.getString(KEY_RSVP_DOCUMENT_URI, null)
        set(value) = prefs.edit().putString(KEY_RSVP_DOCUMENT_URI, value).apply()

    var rsvpDocumentName: String?
        get() = prefs.getString(KEY_RSVP_DOCUMENT_NAME, null)
        set(value) = prefs.edit().putString(KEY_RSVP_DOCUMENT_NAME, value).apply()

    var rsvpDocumentWordCount: Int
        get() = prefs.getInt(KEY_RSVP_DOCUMENT_WORD_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_RSVP_DOCUMENT_WORD_COUNT, value).apply()

    fun clearRsvpDocument() {
        prefs.edit()
            .remove(KEY_RSVP_DOCUMENT_URI)
            .remove(KEY_RSVP_DOCUMENT_NAME)
            .remove(KEY_RSVP_DOCUMENT_WORD_COUNT)
            .apply()
    }

    // RSVP Display Settings

    var rsvpTextSizePercent: Float
        get() = prefs.getFloat(KEY_RSVP_TEXT_SIZE_PERCENT, 0.15f)
        set(value) = prefs.edit().putFloat(KEY_RSVP_TEXT_SIZE_PERCENT,
            value.coerceIn(RsvpSettings.MIN_TEXT_SIZE_PERCENT, RsvpSettings.MAX_TEXT_SIZE_PERCENT)).apply()

    var rsvpHyphenationEnabled: Boolean
        get() = prefs.getBoolean(KEY_RSVP_HYPHENATION, true)
        set(value) = prefs.edit().putBoolean(KEY_RSVP_HYPHENATION, value).apply()

    var rsvpMultiLineEnabled: Boolean
        get() = prefs.getBoolean(KEY_RSVP_MULTI_LINE, false)
        set(value) = prefs.edit().putBoolean(KEY_RSVP_MULTI_LINE, value).apply()

    var rsvpOrpHighlightEnabled: Boolean
        get() = prefs.getBoolean(KEY_RSVP_ORP_HIGHLIGHT, true)
        set(value) = prefs.edit().putBoolean(KEY_RSVP_ORP_HIGHLIGHT, value).apply()

    var rsvpMaxGlimpseChars: Int
        get() = prefs.getInt(KEY_RSVP_MAX_GLIMPSE_CHARS, 25)
        set(value) = prefs.edit().putInt(KEY_RSVP_MAX_GLIMPSE_CHARS, value.coerceIn(10, 50)).apply()

    var rsvpMaxGlimpseWords: Int
        get() = prefs.getInt(KEY_RSVP_MAX_GLIMPSE_WORDS, 3)
        set(value) = prefs.edit().putInt(KEY_RSVP_MAX_GLIMPSE_WORDS, value.coerceIn(1, 5)).apply()

    var rsvpResumeWordIndex: Int
        get() = prefs.getInt(KEY_RSVP_RESUME_WORD_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_RSVP_RESUME_WORD_INDEX, value).apply()

    /**
     * Build RsvpSettings from stored preferences.
     */
    fun getRsvpSettings(): RsvpSettings {
        return RsvpSettings(
            baseWpm = rsvpWpm,
            maxGlimpseChars = rsvpMaxGlimpseChars,
            maxGlimpseWords = rsvpMaxGlimpseWords,
            multiLineEnabled = rsvpMultiLineEnabled,
            textSizePercent = rsvpTextSizePercent,
            hyphenationEnabled = rsvpHyphenationEnabled,
            orpHighlightEnabled = rsvpOrpHighlightEnabled
        )
    }

    /**
     * Apply RsvpSettings to stored preferences.
     */
    fun setRsvpSettings(settings: RsvpSettings) {
        rsvpWpm = settings.baseWpm
        rsvpMaxGlimpseChars = settings.maxGlimpseChars
        rsvpMaxGlimpseWords = settings.maxGlimpseWords
        rsvpMultiLineEnabled = settings.multiLineEnabled
        rsvpTextSizePercent = settings.textSizePercent
        rsvpHyphenationEnabled = settings.hyphenationEnabled
        rsvpOrpHighlightEnabled = settings.orpHighlightEnabled
    }

    /**
     * Clear RSVP resume position.
     */
    fun clearRsvpResumePosition() {
        rsvpResumeWordIndex = 0
    }
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
