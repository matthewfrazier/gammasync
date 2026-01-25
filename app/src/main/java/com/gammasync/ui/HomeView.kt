package com.gammasync.ui

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.gammasync.R
import com.gammasync.data.ColorScheme
import com.gammasync.data.SettingsRepository
import com.gammasync.domain.entrainment.EntrainmentMode
import com.gammasync.domain.entrainment.EntrainmentProfiles
import com.gammasync.infra.HapticFeedback
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors

/**
 * Home screen with mode selector, duration selector and start button.
 * Uses Material 3 theming for automatic dark/light mode support.
 */
class HomeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onStartSession: ((durationMinutes: Int, mode: EntrainmentMode) -> Unit)? = null
    var onSettingsClicked: (() -> Unit)? = null

    // Mode selector buttons
    private val modeNeuroSyncButton: MaterialButton
    private val modeMemoryButton: MaterialButton
    private val modeSleepButton: MaterialButton
    private val modeMigraineButton: MaterialButton
    private val modeMoodLiftButton: MaterialButton
    private val modeDescriptionText: TextView
    private val xrealWarningText: TextView

    // Hardware requirement icons
    private val iconMoodLiftHeadphones: ImageView
    private val iconMoodLiftGlasses: ImageView

    // Duration selector buttons
    private val duration15Button: MaterialButton
    private val duration30Button: MaterialButton
    private val duration60Button: MaterialButton
    private val startSessionButton: MaterialButton

    private var selectedMode: EntrainmentMode = EntrainmentMode.NEUROSYNC
    private var selectedDuration = 30
    private var settings: SettingsRepository? = null
    private var hasExternalDisplay = false
    private val haptics = HapticFeedback(context)

    // Accent color for selected state
    private var accentColor = 0xFF26A69A.toInt()

    // Icon tint colors
    private val iconTintRequired = 0xFFFF5252.toInt()
    private val iconTintConnected = 0xFF4CAF50.toInt()

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

        // Mode button clicks
        modeNeuroSyncButton.setOnClickListener { selectMode(EntrainmentMode.NEUROSYNC) }
        modeMemoryButton.setOnClickListener { selectMode(EntrainmentMode.MEMORY_WRITE) }
        modeSleepButton.setOnClickListener { selectMode(EntrainmentMode.SLEEP_RAMP) }
        modeMigraineButton.setOnClickListener { selectMode(EntrainmentMode.MIGRAINE) }
        modeMoodLiftButton.setOnClickListener { selectMode(EntrainmentMode.MOOD_LIFT) }

        // Duration button clicks
        duration15Button.setOnClickListener { selectDuration(15) }
        duration30Button.setOnClickListener { selectDuration(30) }
        duration60Button.setOnClickListener { selectDuration(60) }

        startSessionButton.setOnClickListener {
            haptics.heavyClick()
            onStartSession?.invoke(selectedDuration, selectedMode)
        }

        findViewById<View>(R.id.settingsButton).setOnClickListener {
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
        accentColor = settingsRepository.colorScheme.accentColor
        updateAccentColor()
        updateModeSelection()
        updateDurationSelection()
    }

    private fun updateAccentColor() {
        // Update start button with accent color
        startSessionButton.backgroundTintList = ColorStateList.valueOf(accentColor)
    }

    /**
     * Update external display connection status.
     */
    fun setHasExternalDisplay(connected: Boolean) {
        hasExternalDisplay = connected
        updateXrealWarning()
        updateIconColors()
    }

    private fun updateIconColors() {
        val glassesColor = if (hasExternalDisplay) iconTintConnected else iconTintRequired
        iconMoodLiftHeadphones.imageTintList = ColorStateList.valueOf(glassesColor)
        iconMoodLiftGlasses.imageTintList = ColorStateList.valueOf(glassesColor)
    }

    fun setSelectedDuration(minutes: Int) {
        selectedDuration = minutes
        updateDurationSelection()
    }

    private fun selectMode(mode: EntrainmentMode) {
        haptics.tick()
        selectedMode = mode
        settings?.therapyMode = mode

        // Update duration to mode default
        val profile = EntrainmentProfiles.forMode(mode)
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
        val modeButtons = mapOf(
            EntrainmentMode.NEUROSYNC to modeNeuroSyncButton,
            EntrainmentMode.MEMORY_WRITE to modeMemoryButton,
            EntrainmentMode.SLEEP_RAMP to modeSleepButton,
            EntrainmentMode.MIGRAINE to modeMigraineButton,
            EntrainmentMode.MOOD_LIFT to modeMoodLiftButton
        )

        // Get theme-aware colors for unselected state
        val surfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0)
        val onSurfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0)

        modeButtons.forEach { (mode, button) ->
            val isSelected = mode == selectedMode
            if (isSelected) {
                button.backgroundTintList = ColorStateList.valueOf(accentColor)
                button.setTextColor(0xFFFFFFFF.toInt())
            } else {
                // Transparent background with theme-appropriate text color
                button.backgroundTintList = ColorStateList.valueOf(surfaceColor)
                button.setTextColor(onSurfaceColor)
            }
        }

        // Update description
        modeDescriptionText.text = when (selectedMode) {
            EntrainmentMode.NEUROSYNC -> context.getString(R.string.mode_neurosync_description)
            EntrainmentMode.MEMORY_WRITE -> context.getString(R.string.mode_memory_description)
            EntrainmentMode.SLEEP_RAMP -> context.getString(R.string.mode_sleep_description)
            EntrainmentMode.MIGRAINE -> context.getString(R.string.mode_migraine_description)
            EntrainmentMode.MOOD_LIFT -> context.getString(R.string.mode_mood_lift_description)
        }

        updateXrealWarning()
    }

    private fun updateXrealWarning() {
        val showWarning = selectedMode.requiresXreal && !hasExternalDisplay
        xrealWarningText.visibility = if (showWarning) View.VISIBLE else View.GONE

        startSessionButton.isEnabled = !selectedMode.requiresXreal || hasExternalDisplay
        startSessionButton.alpha = if (startSessionButton.isEnabled) 1.0f else 0.5f
    }

    private fun updateDurationSelection() {
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
}
