package com.cognihertz.ui

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
import com.cognihertz.R
import com.cognihertz.data.ColorScheme
import com.cognihertz.data.SettingsRepository
import com.cognihertz.infra.HapticFeedback
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

    private val haptics = HapticFeedback(context)
    private var settings: SettingsRepository? = null
    private var selectedDuration = 30
    private var selectedColorScheme = ColorScheme.TEAL
    private var darkMode = true

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

        updateColorChips()
    }

    fun bindSettings(settingsRepository: SettingsRepository) {
        settings = settingsRepository
        selectedDuration = settingsRepository.durationMinutes
        selectedColorScheme = settingsRepository.colorScheme
        darkMode = settingsRepository.darkMode

        // Refresh back button tint for current theme
        val onSurfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0)
        backButton.imageTintList = ColorStateList.valueOf(onSurfaceColor)

        updateDurationSelection()
        updateColorChips()
        updateDarkModeSelection()
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
}
