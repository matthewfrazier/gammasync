package com.gammasync.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import com.gammasync.R
import com.gammasync.data.SettingsRepository
import com.gammasync.infra.HapticFeedback

/**
 * Settings screen for configuring default session duration.
 */
class SettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onBackClicked: (() -> Unit)? = null

    private val backButton: ImageButton
    private val duration15Button: Button
    private val duration30Button: Button
    private val duration60Button: Button

    private val haptics = HapticFeedback(context)
    private var settings: SettingsRepository? = null
    private var selectedDuration = 30

    // Match HomeView colors
    private val selectedTextColor = 0xFF000000.toInt()  // Black on teal
    private val unselectedTextColor = 0xFF9E9E9E.toInt() // Gray

    init {
        LayoutInflater.from(context).inflate(R.layout.view_settings, this, true)

        backButton = findViewById(R.id.backButton)
        duration15Button = findViewById(R.id.settingsDuration15)
        duration30Button = findViewById(R.id.settingsDuration30)
        duration60Button = findViewById(R.id.settingsDuration60)

        backButton.setOnClickListener {
            haptics.tick()
            onBackClicked?.invoke()
        }

        duration15Button.setOnClickListener { selectDuration(15) }
        duration30Button.setOnClickListener { selectDuration(30) }
        duration60Button.setOnClickListener { selectDuration(60) }
    }

    fun bindSettings(settingsRepository: SettingsRepository) {
        settings = settingsRepository
        selectedDuration = settingsRepository.durationMinutes
        updateDurationSelection()
    }

    private fun selectDuration(minutes: Int) {
        haptics.tick()
        selectedDuration = minutes
        settings?.durationMinutes = minutes
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
