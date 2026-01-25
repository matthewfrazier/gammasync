package com.gammasync.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Switch
import com.gammasync.R
import com.gammasync.data.SettingsRepository
import com.gammasync.infra.HapticFeedback

/**
 * Home screen with duration selector and start button.
 * All controls positioned at bottom for one-handed use.
 */
class HomeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onStartSession: ((durationMinutes: Int) -> Unit)? = null
    var onSettingsClicked: (() -> Unit)? = null
    var onMaxBrightnessChanged: ((enabled: Boolean) -> Unit)? = null

    private val duration15Button: Button
    private val duration30Button: Button
    private val duration60Button: Button
    private val startSessionButton: Button
    private val settingsButton: Button
    private val maxBrightnessSwitch: Switch

    private var selectedDuration = 30
    private var settings: SettingsRepository? = null
    private val haptics = HapticFeedback(context)

    // Material 3 dark theme colors - Teal accent
    private val selectedTextColor = 0xFF000000.toInt()  // Black on teal (primary)
    private val unselectedTextColor = 0xFF9E9E9E.toInt() // Gray on surface

    init {
        LayoutInflater.from(context).inflate(R.layout.view_home, this, true)

        duration15Button = findViewById(R.id.duration15Button)
        duration30Button = findViewById(R.id.duration30Button)
        duration60Button = findViewById(R.id.duration60Button)
        startSessionButton = findViewById(R.id.startSessionButton)
        settingsButton = findViewById(R.id.settingsButton)
        maxBrightnessSwitch = findViewById(R.id.maxBrightnessSwitch)

        duration15Button.setOnClickListener { selectDuration(15) }
        duration30Button.setOnClickListener { selectDuration(30) }
        duration60Button.setOnClickListener { selectDuration(60) }

        startSessionButton.setOnClickListener {
            haptics.heavyClick()
            onStartSession?.invoke(selectedDuration)
        }

        settingsButton.setOnClickListener {
            haptics.tick()
            onSettingsClicked?.invoke()
        }

        maxBrightnessSwitch.setOnCheckedChangeListener { _, isChecked ->
            haptics.tick()
            settings?.maxBrightness = isChecked
            onMaxBrightnessChanged?.invoke(isChecked)
        }

        updateDurationSelection()
    }

    fun bindSettings(settingsRepository: SettingsRepository) {
        settings = settingsRepository
        selectedDuration = settingsRepository.durationMinutes
        updateDurationSelection()
        maxBrightnessSwitch.isChecked = settingsRepository.maxBrightness
    }

    fun setSelectedDuration(minutes: Int) {
        selectedDuration = minutes
        updateDurationSelection()
    }

    private fun selectDuration(minutes: Int) {
        haptics.tick()
        selectedDuration = minutes
        updateDurationSelection()
    }

    private fun updateDurationSelection() {
        duration15Button.setBackgroundResource(
            if (selectedDuration == 15) R.drawable.chip_background_selected
            else R.drawable.chip_background
        )
        duration30Button.setBackgroundResource(
            if (selectedDuration == 30) R.drawable.chip_background_selected
            else R.drawable.chip_background
        )
        duration60Button.setBackgroundResource(
            if (selectedDuration == 60) R.drawable.chip_background_selected
            else R.drawable.chip_background
        )

        duration15Button.setTextColor(if (selectedDuration == 15) selectedTextColor else unselectedTextColor)
        duration30Button.setTextColor(if (selectedDuration == 30) selectedTextColor else unselectedTextColor)
        duration60Button.setTextColor(if (selectedDuration == 60) selectedTextColor else unselectedTextColor)
    }
}
