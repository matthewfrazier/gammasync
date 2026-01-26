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

    // Danger zone 60Hz toggle
    private val allow60HzButton: MaterialButton
    private val deny60HzButton: MaterialButton

    private val haptics = HapticFeedback(context)
    private var settings: SettingsRepository? = null
    private var selectedDuration = 30
    private var selectedColorScheme = ColorScheme.TEAL
    private var darkMode = true
    private var backgroundNoiseEnabled = true
    private var allow60Hz = false

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

        // Danger zone 60Hz toggle
        allow60HzButton = findViewById(R.id.allow60HzButton)
        deny60HzButton = findViewById(R.id.deny60HzButton)

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

        // Danger zone 60Hz toggle listeners
        allow60HzButton.setOnClickListener { select60HzMode(true) }
        deny60HzButton.setOnClickListener { select60HzMode(false) }

        updateColorChips()
    }

    fun bindSettings(settingsRepository: SettingsRepository) {
        settings = settingsRepository
        selectedDuration = settingsRepository.durationMinutes
        selectedColorScheme = settingsRepository.colorScheme
        darkMode = settingsRepository.darkMode
        backgroundNoiseEnabled = settingsRepository.backgroundNoiseEnabled
        allow60Hz = settingsRepository.allow60HzMode

        // Refresh back button tint for current theme
        val onSurfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0)
        backButton.imageTintList = ColorStateList.valueOf(onSurfaceColor)

        updateDurationSelection()
        updateColorChips()
        updateDarkModeSelection()
        updateNoiseSelection()
        update60HzSelection()
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

    private fun select60HzMode(enabled: Boolean) {
        if (enabled == allow60Hz) return

        if (enabled) {
            // Show warning dialog when enabling 60Hz mode
            show60HzWarningDialog {
                haptics.tick()
                allow60Hz = enabled
                settings?.allow60HzMode = enabled
                update60HzSelection()
            }
        } else {
            haptics.tick()
            allow60Hz = enabled
            settings?.allow60HzMode = enabled
            update60HzSelection()
        }
    }

    private fun update60HzSelection() {
        val accentColor = selectedColorScheme.accentColor
        val errorColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorError, 0)

        // Get theme-aware colors for unselected state
        val surfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0)
        val onSurfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0)

        if (allow60Hz) {
            allow60HzButton.backgroundTintList = ColorStateList.valueOf(errorColor)
            allow60HzButton.setTextColor(0xFFFFFFFF.toInt())
            deny60HzButton.backgroundTintList = ColorStateList.valueOf(surfaceColor)
            deny60HzButton.setTextColor(onSurfaceColor)
        } else {
            deny60HzButton.backgroundTintList = ColorStateList.valueOf(accentColor)
            deny60HzButton.setTextColor(0xFFFFFFFF.toInt())
            allow60HzButton.backgroundTintList = ColorStateList.valueOf(surfaceColor)
            allow60HzButton.setTextColor(onSurfaceColor)
        }
    }

    private fun show60HzWarningDialog(onConfirm: () -> Unit) {
        val dialog = android.app.AlertDialog.Builder(context)
            .setTitle("⚠️ 60Hz Mode Warning")
            .setMessage("Enabling 60Hz mode will significantly reduce therapeutic effectiveness.\n\n" +
                    "• Optimal therapy requires 120Hz+ displays\n" +
                    "• 60Hz may cause visual artifacts and timing issues\n" +
                    "• Therapeutic benefits may be reduced by 50% or more\n\n" +
                    "This mode should only be used for testing or demonstration purposes.")
            .setPositiveButton("Enable Anyway") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel") { _, _ -> }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .create()
            
        dialog.show()
    }
}
