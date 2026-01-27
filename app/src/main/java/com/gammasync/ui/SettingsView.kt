package com.gammasync.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.gammasync.R
import com.gammasync.data.ColorScheme
import com.gammasync.data.SettingsRepository
import com.gammasync.infra.HapticFeedback
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Settings screen for configuring default session duration, color theme, and dark mode.
 * Uses Material 3 theming - dark/light mode is handled by AppCompatDelegate.
 */
class SettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onBackClicked: (() -> Unit)? = null
    var onColorSchemeChanged: ((ColorScheme) -> Unit)? = null
    var onDarkModeChanged: ((Boolean) -> Unit)? = null

    private val backButton: ImageButton
    private val duration15Button: MaterialButton
    private val duration30Button: MaterialButton
    private val duration60Button: MaterialButton

    // Color theme views
    private val colorTeal: View
    private val colorBlue: View
    private val colorPurple: View
    private val colorGreen: View
    private val colorOrange: View
    private val colorRed: View

    // Dark mode toggle
    private val darkModeButton: MaterialButton
    private val lightModeButton: MaterialButton

    // Background noise toggle
    private val noiseOnButton: MaterialButton
    private val noiseOffButton: MaterialButton

    // RSVP display settings
    private val rsvpSeekTextSize: SeekBar
    private val rsvpTxtTextSizeValue: TextView
    private val rsvpSwitchOrpHighlight: MaterialSwitch
    private val rsvpSwitchHyphenation: MaterialSwitch

    private val haptics = HapticFeedback(context)
    private var settings: SettingsRepository? = null
    private var selectedDuration = 30
    private var selectedColorScheme = ColorScheme.TEAL
    private var darkMode = true
    private var backgroundNoiseEnabled = true

    init {
        LayoutInflater.from(context).inflate(R.layout.view_settings, this, true)

        backButton = findViewById(R.id.backButton)
        duration15Button = findViewById(R.id.settingsDuration15)
        duration30Button = findViewById(R.id.settingsDuration30)
        duration60Button = findViewById(R.id.settingsDuration60)

        // Color theme views
        colorTeal = findViewById(R.id.colorTeal)
        colorBlue = findViewById(R.id.colorBlue)
        colorPurple = findViewById(R.id.colorPurple)
        colorGreen = findViewById(R.id.colorGreen)
        colorOrange = findViewById(R.id.colorOrange)
        colorRed = findViewById(R.id.colorRed)

        // Dark mode toggle
        darkModeButton = findViewById(R.id.darkModeButton)
        lightModeButton = findViewById(R.id.lightModeButton)

        // Background noise toggle
        noiseOnButton = findViewById(R.id.noiseOnButton)
        noiseOffButton = findViewById(R.id.noiseOffButton)

        // RSVP display settings
        rsvpSeekTextSize = findViewById(R.id.rsvpSeekTextSize)
        rsvpTxtTextSizeValue = findViewById(R.id.rsvpTxtTextSizeValue)
        rsvpSwitchOrpHighlight = findViewById(R.id.rsvpSwitchOrpHighlight)
        rsvpSwitchHyphenation = findViewById(R.id.rsvpSwitchHyphenation)

        // Set back button tint to match theme
        val onSurfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0)
        backButton.imageTintList = ColorStateList.valueOf(onSurfaceColor)

        backButton.setOnClickListener {
            haptics.tick()
            onBackClicked?.invoke()
        }

        duration15Button.setOnClickListener { selectDuration(15) }
        duration30Button.setOnClickListener { selectDuration(30) }
        duration60Button.setOnClickListener { selectDuration(60) }

        // Color theme click listeners
        colorTeal.setOnClickListener { selectColorScheme(ColorScheme.TEAL) }
        colorBlue.setOnClickListener { selectColorScheme(ColorScheme.BLUE) }
        colorPurple.setOnClickListener { selectColorScheme(ColorScheme.PURPLE) }
        colorGreen.setOnClickListener { selectColorScheme(ColorScheme.GREEN) }
        colorOrange.setOnClickListener { selectColorScheme(ColorScheme.ORANGE) }
        colorRed.setOnClickListener { selectColorScheme(ColorScheme.RED) }

        // Dark mode toggle listeners
        darkModeButton.setOnClickListener { selectDarkMode(true) }
        lightModeButton.setOnClickListener { selectDarkMode(false) }

        // Background noise toggle listeners
        noiseOnButton.setOnClickListener { selectBackgroundNoise(true) }
        noiseOffButton.setOnClickListener { selectBackgroundNoise(false) }

        // RSVP settings listeners
        rsvpSeekTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                rsvpTxtTextSizeValue.text = "$progress%"
                if (fromUser) {
                    settings?.rsvpTextSizePercent = progress / 100f
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        rsvpSwitchOrpHighlight.setOnCheckedChangeListener { _, isChecked ->
            settings?.rsvpOrpHighlightEnabled = isChecked
        }

        rsvpSwitchHyphenation.setOnCheckedChangeListener { _, isChecked ->
            settings?.rsvpHyphenationEnabled = isChecked
        }

        updateColorChips()
    }

    fun bindSettings(settingsRepository: SettingsRepository) {
        settings = settingsRepository
        selectedDuration = settingsRepository.durationMinutes
        selectedColorScheme = settingsRepository.colorScheme
        darkMode = settingsRepository.darkMode
        backgroundNoiseEnabled = settingsRepository.backgroundNoiseEnabled

        // Load RSVP settings
        rsvpSeekTextSize.progress = (settingsRepository.rsvpTextSizePercent * 100).toInt()
        rsvpTxtTextSizeValue.text = "${rsvpSeekTextSize.progress}%"
        rsvpSwitchOrpHighlight.isChecked = settingsRepository.rsvpOrpHighlightEnabled
        rsvpSwitchHyphenation.isChecked = settingsRepository.rsvpHyphenationEnabled

        // Refresh back button tint for current theme
        val onSurfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0)
        backButton.imageTintList = ColorStateList.valueOf(onSurfaceColor)

        updateDurationSelection()
        updateColorChips()
        updateDarkModeSelection()
        updateNoiseSelection()
    }

    private fun selectDuration(minutes: Int) {
        haptics.tick()
        selectedDuration = minutes
        settings?.durationMinutes = minutes
        updateDurationSelection()
    }

    private fun updateDurationSelection() {
        val accentColor = selectedColorScheme.accentColor
        val durationButtons = listOf(
            15 to duration15Button,
            30 to duration30Button,
            60 to duration60Button
        )

        // Get theme-aware colors for unselected state
        val surfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0)
        val onSurfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0)
        val onPrimaryColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF.toInt())

        durationButtons.forEach { (duration, button) ->
            val isSelected = duration == selectedDuration
            if (isSelected) {
                button.backgroundTintList = ColorStateList.valueOf(accentColor)
                button.setTextColor(onPrimaryColor)
            } else {
                button.backgroundTintList = ColorStateList.valueOf(surfaceColor)
                button.setTextColor(onSurfaceColor)
            }
        }
    }

    private fun selectColorScheme(scheme: ColorScheme) {
        if (scheme == selectedColorScheme) return

        haptics.tick()
        selectedColorScheme = scheme
        settings?.colorScheme = scheme
        updateColorChips()
        updateDurationSelection()
        updateDarkModeSelection()
        onColorSchemeChanged?.invoke(scheme)
    }

    private fun selectDarkMode(isDark: Boolean) {
        if (isDark == darkMode) return

        haptics.tick()
        darkMode = isDark
        settings?.darkMode = isDark

        // Use AppCompatDelegate to switch the entire app's theme
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        updateDarkModeSelection()
        onDarkModeChanged?.invoke(isDark)
    }

    private fun updateDarkModeSelection() {
        val accentColor = selectedColorScheme.accentColor

        // Get theme-aware colors for unselected state
        val surfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0)
        val onSurfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0)
        val onPrimaryColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF.toInt())

        if (darkMode) {
            darkModeButton.backgroundTintList = ColorStateList.valueOf(accentColor)
            darkModeButton.setTextColor(onPrimaryColor)
            lightModeButton.backgroundTintList = ColorStateList.valueOf(surfaceColor)
            lightModeButton.setTextColor(onSurfaceColor)
        } else {
            lightModeButton.backgroundTintList = ColorStateList.valueOf(accentColor)
            lightModeButton.setTextColor(onPrimaryColor)
            darkModeButton.backgroundTintList = ColorStateList.valueOf(surfaceColor)
            darkModeButton.setTextColor(onSurfaceColor)
        }
    }

    private fun selectBackgroundNoise(enabled: Boolean) {
        if (enabled == backgroundNoiseEnabled) return

        haptics.tick()
        backgroundNoiseEnabled = enabled
        settings?.backgroundNoiseEnabled = enabled
        updateNoiseSelection()
    }

    private fun updateNoiseSelection() {
        val accentColor = selectedColorScheme.accentColor

        // Get theme-aware colors for unselected state
        val surfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0)
        val onSurfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0)
        val onPrimaryColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF.toInt())

        if (backgroundNoiseEnabled) {
            noiseOnButton.backgroundTintList = ColorStateList.valueOf(accentColor)
            noiseOnButton.setTextColor(onPrimaryColor)
            noiseOffButton.backgroundTintList = ColorStateList.valueOf(surfaceColor)
            noiseOffButton.setTextColor(onSurfaceColor)
        } else {
            noiseOffButton.backgroundTintList = ColorStateList.valueOf(accentColor)
            noiseOffButton.setTextColor(onPrimaryColor)
            noiseOnButton.backgroundTintList = ColorStateList.valueOf(surfaceColor)
            noiseOnButton.setTextColor(onSurfaceColor)
        }
    }

    private fun updateColorChips() {
        val colorViews = mapOf(
            ColorScheme.TEAL to colorTeal,
            ColorScheme.BLUE to colorBlue,
            ColorScheme.PURPLE to colorPurple,
            ColorScheme.GREEN to colorGreen,
            ColorScheme.ORANGE to colorOrange,
            ColorScheme.RED to colorRed
        )

        val density = resources.displayMetrics.density
        val borderColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0xFFFFFFFF.toInt())

        colorViews.forEach { (scheme, view) ->
            val isSelected = scheme == selectedColorScheme
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * density
                setColor(scheme.accentColor)
                if (isSelected) {
                    // Theme-aware border for selected color
                    setStroke((3 * density).toInt(), borderColor)
                }
            }
            view.background = drawable
        }
    }
}
