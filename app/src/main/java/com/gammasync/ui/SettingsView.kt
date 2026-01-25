package com.gammasync.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import com.gammasync.R
import com.gammasync.data.ColorScheme
import com.gammasync.data.SettingsRepository
import com.gammasync.infra.HapticFeedback

/**
 * Settings screen for configuring default session duration and color theme.
 */
class SettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onBackClicked: (() -> Unit)? = null
    var onColorSchemeChanged: ((ColorScheme) -> Unit)? = null

    private val backButton: ImageButton
    private val duration15Button: Button
    private val duration30Button: Button
    private val duration60Button: Button

    // Color theme views
    private val colorTeal: View
    private val colorBlue: View
    private val colorPurple: View
    private val colorGreen: View
    private val colorOrange: View
    private val colorRed: View

    private val haptics = HapticFeedback(context)
    private var settings: SettingsRepository? = null
    private var selectedDuration = 30
    private var selectedColorScheme = ColorScheme.TEAL

    // Match HomeView colors
    private val selectedTextColor = 0xFF000000.toInt()  // Black on accent
    private val unselectedTextColor = 0xFF9E9E9E.toInt() // Gray

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

        // Set initial colors for chips
        updateColorChips()
    }

    fun bindSettings(settingsRepository: SettingsRepository) {
        settings = settingsRepository
        selectedDuration = settingsRepository.durationMinutes
        selectedColorScheme = settingsRepository.colorScheme
        updateDurationSelection()
        updateColorChips()
    }

    private fun selectDuration(minutes: Int) {
        haptics.tick()
        selectedDuration = minutes
        settings?.durationMinutes = minutes
        updateDurationSelection()
    }

    private fun updateDurationSelection() {
        duration15Button.background = createChipBackground(selectedDuration == 15)
        duration30Button.background = createChipBackground(selectedDuration == 30)
        duration60Button.background = createChipBackground(selectedDuration == 60)

        duration15Button.setTextColor(if (selectedDuration == 15) selectedTextColor else unselectedTextColor)
        duration30Button.setTextColor(if (selectedDuration == 30) selectedTextColor else unselectedTextColor)
        duration60Button.setTextColor(if (selectedDuration == 60) selectedTextColor else unselectedTextColor)
    }

    private fun createChipBackground(selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16 * resources.displayMetrics.density
            if (selected) {
                setColor(selectedColorScheme.accentColor)
            } else {
                setColor(0xFF2A2A2A.toInt())
            }
        }
    }

    private fun selectColorScheme(scheme: ColorScheme) {
        if (scheme == selectedColorScheme) return

        haptics.tick()
        selectedColorScheme = scheme
        settings?.colorScheme = scheme
        updateColorChips()
        onColorSchemeChanged?.invoke(scheme)
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

        colorViews.forEach { (scheme, view) ->
            val isSelected = scheme == selectedColorScheme
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * resources.displayMetrics.density
                setColor(scheme.accentColor)
                if (isSelected) {
                    setStroke((3 * resources.displayMetrics.density).toInt(), 0xFFFFFFFF.toInt())
                }
            }
            view.background = drawable
        }
    }
}
