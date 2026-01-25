package com.gammasync.ui

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.gammasync.R
import com.gammasync.data.SettingsRepository
import com.gammasync.domain.therapy.TherapyMode
import com.gammasync.domain.therapy.TherapyProfiles
import com.gammasync.infra.HapticFeedback

/**
 * Home screen with mode selector, duration selector and start button.
 * All controls positioned at bottom for one-handed use.
 */
class HomeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onStartSession: ((durationMinutes: Int, mode: TherapyMode) -> Unit)? = null
    var onSettingsClicked: (() -> Unit)? = null

    // Mode selector buttons
    private val modeNeuroSyncButton: Button
    private val modeMemoryButton: Button
    private val modeSleepButton: Button
    private val modeMigraineButton: Button
    private val modeMoodLiftButton: Button
    private val modeDescriptionText: TextView
    private val xrealWarningText: TextView

    // Hardware requirement icons
    private val iconMoodLiftHeadphones: ImageView
    private val iconMoodLiftGlasses: ImageView

    // Duration selector buttons
    private val duration15Button: Button
    private val duration30Button: Button
    private val duration60Button: Button
    private val startSessionButton: Button
    private val settingsButton: Button

    private var selectedMode: TherapyMode = TherapyMode.NEUROSYNC
    private var selectedDuration = 30
    private var settings: SettingsRepository? = null
    private var hasExternalDisplay = false
    private val haptics = HapticFeedback(context)

    // Material 3 dark theme colors - Teal accent
    private val selectedTextColor = 0xFF000000.toInt()  // Black on teal (primary)
    private val unselectedTextColor = 0xFF9E9E9E.toInt() // Gray on surface

    // Icon tint colors
    private val iconTintSupported = 0xFF26A69A.toInt()   // Teal - supported hardware
    private val iconTintRequired = 0xFFFF5252.toInt()    // Red - required but missing
    private val iconTintConnected = 0xFF4CAF50.toInt()   // Green - connected

    init {
        LayoutInflater.from(context).inflate(R.layout.view_home, this, true)

        // Mode selector
        modeNeuroSyncButton = findViewById(R.id.modeNeuroSyncButton)
        modeMemoryButton = findViewById(R.id.modeMemoryButton)
        modeSleepButton = findViewById(R.id.modeSleepButton)
        modeMigraineButton = findViewById(R.id.modeMigraineButton)
        modeMoodLiftButton = findViewById(R.id.modeMoodLiftButton)
        modeDescriptionText = findViewById(R.id.modeDescriptionText)
        xrealWarningText = findViewById(R.id.xrealWarningText)

        // Hardware requirement icons for Mood Lift
        iconMoodLiftHeadphones = findViewById(R.id.iconMoodLiftHeadphones)
        iconMoodLiftGlasses = findViewById(R.id.iconMoodLiftGlasses)

        // Duration selector
        duration15Button = findViewById(R.id.duration15Button)
        duration30Button = findViewById(R.id.duration30Button)
        duration60Button = findViewById(R.id.duration60Button)
        startSessionButton = findViewById(R.id.startSessionButton)
        settingsButton = findViewById(R.id.settingsButton)

        // Mode button clicks
        modeNeuroSyncButton.setOnClickListener { selectMode(TherapyMode.NEUROSYNC) }
        modeMemoryButton.setOnClickListener { selectMode(TherapyMode.MEMORY_WRITE) }
        modeSleepButton.setOnClickListener { selectMode(TherapyMode.SLEEP_RAMP) }
        modeMigraineButton.setOnClickListener { selectMode(TherapyMode.MIGRAINE) }
        modeMoodLiftButton.setOnClickListener { selectMode(TherapyMode.MOOD_LIFT) }

        // Duration button clicks
        duration15Button.setOnClickListener { selectDuration(15) }
        duration30Button.setOnClickListener { selectDuration(30) }
        duration60Button.setOnClickListener { selectDuration(60) }

        startSessionButton.setOnClickListener {
            haptics.heavyClick()
            onStartSession?.invoke(selectedDuration, selectedMode)
        }

        settingsButton.setOnClickListener {
            haptics.tick()
            onSettingsClicked?.invoke()
        }

        updateModeSelection()
        updateDurationSelection()
        updateIconColors()
    }

    fun bindSettings(settingsRepository: SettingsRepository) {
        settings = settingsRepository
        selectedDuration = settingsRepository.durationMinutes
        selectedMode = settingsRepository.therapyMode
        updateModeSelection()
        updateDurationSelection()
    }

    /**
     * Update external display connection status.
     * This affects whether Mood Lift mode (XREAL-only) is available.
     */
    fun setHasExternalDisplay(connected: Boolean) {
        hasExternalDisplay = connected
        updateXrealWarning()
        updateIconColors()
    }

    /**
     * Update icon colors based on XREAL connection status.
     * - Green when XREAL connected (hardware available)
     * - Red when XREAL required but not connected
     */
    private fun updateIconColors() {
        val glassesColor = if (hasExternalDisplay) iconTintConnected else iconTintRequired
        iconMoodLiftHeadphones.imageTintList = ColorStateList.valueOf(glassesColor)
        iconMoodLiftGlasses.imageTintList = ColorStateList.valueOf(glassesColor)
    }

    fun setSelectedDuration(minutes: Int) {
        selectedDuration = minutes
        updateDurationSelection()
    }

    private fun selectMode(mode: TherapyMode) {
        haptics.tick()
        selectedMode = mode
        settings?.therapyMode = mode

        // Update duration to mode default
        val profile = TherapyProfiles.forMode(mode)
        selectedDuration = profile.defaultDurationMinutes

        updateModeSelection()
        updateDurationSelection()
    }

    private fun selectDuration(minutes: Int) {
        haptics.tick()
        selectedDuration = minutes
        updateDurationSelection()
    }

    private fun updateModeSelection() {
        // Update button backgrounds
        val modeButtons = mapOf(
            TherapyMode.NEUROSYNC to modeNeuroSyncButton,
            TherapyMode.MEMORY_WRITE to modeMemoryButton,
            TherapyMode.SLEEP_RAMP to modeSleepButton,
            TherapyMode.MIGRAINE to modeMigraineButton,
            TherapyMode.MOOD_LIFT to modeMoodLiftButton
        )

        modeButtons.forEach { (mode, button) ->
            val isSelected = mode == selectedMode
            button.setBackgroundResource(
                if (isSelected) R.drawable.chip_background_selected
                else R.drawable.chip_background
            )
            button.setTextColor(if (isSelected) selectedTextColor else unselectedTextColor)
        }

        // Update description
        modeDescriptionText.text = when (selectedMode) {
            TherapyMode.NEUROSYNC -> context.getString(R.string.mode_neurosync_description)
            TherapyMode.MEMORY_WRITE -> context.getString(R.string.mode_memory_description)
            TherapyMode.SLEEP_RAMP -> context.getString(R.string.mode_sleep_description)
            TherapyMode.MIGRAINE -> context.getString(R.string.mode_migraine_description)
            TherapyMode.MOOD_LIFT -> context.getString(R.string.mode_mood_lift_description)
        }

        updateXrealWarning()
    }

    private fun updateXrealWarning() {
        // Show warning if Mood Lift is selected but no XREAL connected
        val showWarning = selectedMode.requiresXreal && !hasExternalDisplay
        xrealWarningText.visibility = if (showWarning) View.VISIBLE else View.GONE

        // Disable start button if XREAL required but not connected
        startSessionButton.isEnabled = !selectedMode.requiresXreal || hasExternalDisplay
        startSessionButton.alpha = if (startSessionButton.isEnabled) 1.0f else 0.5f
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
