package com.gammasync.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.gammasync.R
import com.gammasync.data.ColorScheme
import com.gammasync.data.RsvpTextSize
import com.gammasync.data.SettingsRepository
import com.gammasync.infra.HapticFeedback
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors

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

    // RSVP settings
    private val rsvpSizeSmall: MaterialButton
    private val rsvpSizeMedium: MaterialButton
    private val rsvpSizeLarge: MaterialButton
    private val rsvpAnchorOn: MaterialButton
    private val rsvpAnchorOff: MaterialButton
    private val rsvpAnnotationsOn: MaterialButton
    private val rsvpAnnotationsOff: MaterialButton

    private val haptics = HapticFeedback(context)
    private var settings: SettingsRepository? = null
    private var selectedDuration = 30
    private var selectedColorScheme = ColorScheme.TEAL
    private var darkMode = true
    private var backgroundNoiseEnabled = true
    private var rsvpTextSize = RsvpTextSize.MEDIUM
    private var rsvpAnchorHighlight = true
    private var rsvpAnnotations = true

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

        // RSVP settings
        rsvpSizeSmall = findViewById(R.id.rsvpSizeSmall)
        rsvpSizeMedium = findViewById(R.id.rsvpSizeMedium)
        rsvpSizeLarge = findViewById(R.id.rsvpSizeLarge)
        rsvpAnchorOn = findViewById(R.id.rsvpAnchorOn)
        rsvpAnchorOff = findViewById(R.id.rsvpAnchorOff)
        rsvpAnnotationsOn = findViewById(R.id.rsvpAnnotationsOn)
        rsvpAnnotationsOff = findViewById(R.id.rsvpAnnotationsOff)

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
        rsvpSizeSmall.setOnClickListener { selectRsvpSize(RsvpTextSize.SMALL) }
        rsvpSizeMedium.setOnClickListener { selectRsvpSize(RsvpTextSize.MEDIUM) }
        rsvpSizeLarge.setOnClickListener { selectRsvpSize(RsvpTextSize.LARGE) }
        rsvpAnchorOn.setOnClickListener { selectRsvpAnchor(true) }
        rsvpAnchorOff.setOnClickListener { selectRsvpAnchor(false) }
        rsvpAnnotationsOn.setOnClickListener { selectRsvpAnnotations(true) }
        rsvpAnnotationsOff.setOnClickListener { selectRsvpAnnotations(false) }

        updateColorChips()
    }

    fun bindSettings(settingsRepository: SettingsRepository) {
        settings = settingsRepository
        selectedDuration = settingsRepository.durationMinutes
        selectedColorScheme = settingsRepository.colorScheme
        darkMode = settingsRepository.darkMode
        backgroundNoiseEnabled = settingsRepository.backgroundNoiseEnabled
        rsvpTextSize = settingsRepository.rsvpTextSize
        rsvpAnchorHighlight = settingsRepository.rsvpAnchorHighlight
        rsvpAnnotations = settingsRepository.rsvpAnnotations

        // Refresh back button tint for current theme
        val onSurfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0)
        backButton.imageTintList = ColorStateList.valueOf(onSurfaceColor)

        updateDurationSelection()
        updateColorChips()
        updateDarkModeSelection()
        updateNoiseSelection()
        updateRsvpSizeSelection()
        updateRsvpAnchorSelection()
        updateRsvpAnnotationsSelection()
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

        durationButtons.forEach { (duration, button) ->
            val isSelected = duration == selectedDuration
            if (isSelected) {
                button.backgroundTintList = ColorStateList.valueOf(accentColor)
                button.setTextColor(0xFFFFFFFF.toInt())
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

        if (darkMode) {
            darkModeButton.backgroundTintList = ColorStateList.valueOf(accentColor)
            darkModeButton.setTextColor(0xFFFFFFFF.toInt())
            lightModeButton.backgroundTintList = ColorStateList.valueOf(surfaceColor)
            lightModeButton.setTextColor(onSurfaceColor)
        } else {
            lightModeButton.backgroundTintList = ColorStateList.valueOf(accentColor)
            lightModeButton.setTextColor(0xFFFFFFFF.toInt())
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

        if (backgroundNoiseEnabled) {
            noiseOnButton.backgroundTintList = ColorStateList.valueOf(accentColor)
            noiseOnButton.setTextColor(0xFFFFFFFF.toInt())
            noiseOffButton.backgroundTintList = ColorStateList.valueOf(surfaceColor)
            noiseOffButton.setTextColor(onSurfaceColor)
        } else {
            noiseOffButton.backgroundTintList = ColorStateList.valueOf(accentColor)
            noiseOffButton.setTextColor(0xFFFFFFFF.toInt())
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

        colorViews.forEach { (scheme, view) ->
            val isSelected = scheme == selectedColorScheme
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * density
                setColor(scheme.accentColor)
                if (isSelected) {
                    // White border for selected color
                    setStroke((3 * density).toInt(), 0xFFFFFFFF.toInt())
                }
            }
            view.background = drawable
        }
    }

    // --- RSVP Settings ---

    private fun selectRsvpSize(size: RsvpTextSize) {
        if (size == rsvpTextSize) return
        haptics.tick()
        rsvpTextSize = size
        settings?.rsvpTextSize = size
        updateRsvpSizeSelection()
    }

    private fun updateRsvpSizeSelection() {
        val accentColor = selectedColorScheme.accentColor
        val surfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0)
        val onSurfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0)

        val sizeButtons = listOf(
            RsvpTextSize.SMALL to rsvpSizeSmall,
            RsvpTextSize.MEDIUM to rsvpSizeMedium,
            RsvpTextSize.LARGE to rsvpSizeLarge
        )

        sizeButtons.forEach { (size, button) ->
            val isSelected = size == rsvpTextSize
            if (isSelected) {
                button.backgroundTintList = ColorStateList.valueOf(accentColor)
                button.setTextColor(0xFFFFFFFF.toInt())
            } else {
                button.backgroundTintList = ColorStateList.valueOf(surfaceColor)
                button.setTextColor(onSurfaceColor)
            }
        }
    }

    private fun selectRsvpAnchor(enabled: Boolean) {
        if (enabled == rsvpAnchorHighlight) return
        haptics.tick()
        rsvpAnchorHighlight = enabled
        settings?.rsvpAnchorHighlight = enabled
        updateRsvpAnchorSelection()
    }

    private fun updateRsvpAnchorSelection() {
        val accentColor = selectedColorScheme.accentColor
        val surfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0)
        val onSurfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0)

        if (rsvpAnchorHighlight) {
            rsvpAnchorOn.backgroundTintList = ColorStateList.valueOf(accentColor)
            rsvpAnchorOn.setTextColor(0xFFFFFFFF.toInt())
            rsvpAnchorOff.backgroundTintList = ColorStateList.valueOf(surfaceColor)
            rsvpAnchorOff.setTextColor(onSurfaceColor)
        } else {
            rsvpAnchorOff.backgroundTintList = ColorStateList.valueOf(accentColor)
            rsvpAnchorOff.setTextColor(0xFFFFFFFF.toInt())
            rsvpAnchorOn.backgroundTintList = ColorStateList.valueOf(surfaceColor)
            rsvpAnchorOn.setTextColor(onSurfaceColor)
        }
    }

    private fun selectRsvpAnnotations(enabled: Boolean) {
        if (enabled == rsvpAnnotations) return
        haptics.tick()
        rsvpAnnotations = enabled
        settings?.rsvpAnnotations = enabled
        updateRsvpAnnotationsSelection()
    }

    private fun updateRsvpAnnotationsSelection() {
        val accentColor = selectedColorScheme.accentColor
        val surfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0)
        val onSurfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0)

        if (rsvpAnnotations) {
            rsvpAnnotationsOn.backgroundTintList = ColorStateList.valueOf(accentColor)
            rsvpAnnotationsOn.setTextColor(0xFFFFFFFF.toInt())
            rsvpAnnotationsOff.backgroundTintList = ColorStateList.valueOf(surfaceColor)
            rsvpAnnotationsOff.setTextColor(onSurfaceColor)
        } else {
            rsvpAnnotationsOff.backgroundTintList = ColorStateList.valueOf(accentColor)
            rsvpAnnotationsOff.setTextColor(0xFFFFFFFF.toInt())
            rsvpAnnotationsOn.backgroundTintList = ColorStateList.valueOf(surfaceColor)
            rsvpAnnotationsOn.setTextColor(onSurfaceColor)
        }
    }
}
