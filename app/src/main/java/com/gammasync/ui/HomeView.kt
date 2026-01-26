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
import com.gammasync.domain.therapy.TherapyMode
import com.gammasync.domain.therapy.TherapyProfiles
import com.gammasync.infra.HapticFeedback
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
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

    companion object {
        // Theta frequency in Learning mode (Hz)
        private const val THETA_HZ = 6.0

        // Theta-harmonic WPM values (words per minute synced to theta cycles)
        // Each value represents a multiple or fraction of theta frequency
        val THETA_WPM_VALUES = listOf(
            60,   // 0.167× theta (1 word per 6 cycles)
            90,   // 0.25× theta (1 word per 4 cycles)
            120,  // 0.33× theta (1 word per 3 cycles)
            180,  // 0.5× theta (1 word per 2 cycles)
            240,  // 0.67× theta
            300,  // 0.83× theta
            360,  // 1× theta (1 word per cycle)
            480,  // 1.33× theta
            540,  // 1.5× theta
            720,  // 2× theta
            900,  // 2.5× theta
            1080, // 3× theta
            1440, // 4× theta
            1800, // 5× theta
            2000  // Max (slightly over 5.5× theta)
        )

        /**
         * Calculate theta multiple for a given WPM.
         * Returns how many words per theta cycle.
         */
        fun wpmToThetaMultiple(wpm: Int): Double {
            // WPM / 60 = words per second
            // words per second / theta Hz = words per theta cycle
            return (wpm / 60.0) / THETA_HZ
        }

        /**
         * Format theta multiple for display.
         */
        fun formatThetaMultiple(wpm: Int): String {
            val multiple = wpmToThetaMultiple(wpm)
            return when {
                multiple < 1.0 -> String.format("%.2f× theta", multiple)
                multiple == multiple.toLong().toDouble() -> String.format("%.0f× theta", multiple)
                else -> String.format("%.1f× theta", multiple)
            }
        }
    }

    var onStartSession: ((durationMinutes: Int, mode: TherapyMode) -> Unit)? = null
    var onSettingsClicked: (() -> Unit)? = null
    var onLoadTextClicked: (() -> Unit)? = null
    var onClearDocumentClicked: (() -> Unit)? = null
    var onRsvpWpmChanged: ((Int) -> Unit)? = null

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

    // RSVP Text Loading
    private val loadTextRow: MaterialCardView
    private val loadTextStatus: TextView
    private val clearDocumentButton: MaterialButton

    // RSVP Speed Controls
    private val rsvpSpeedRow: MaterialCardView
    private val rsvpSpeedDown: MaterialButton
    private val rsvpSpeedUp: MaterialButton
    private val rsvpWpmDisplay: TextView
    private val rsvpThetaDisplay: TextView

    private var selectedMode: TherapyMode = TherapyMode.NEUROSYNC
    private var currentWpmIndex = THETA_WPM_VALUES.indexOf(360) // Default to 1× theta
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

        // RSVP Text Loading
        loadTextRow = findViewById(R.id.loadTextRow)
        loadTextStatus = findViewById(R.id.loadTextStatus)
        clearDocumentButton = findViewById(R.id.clearDocumentButton)

        // RSVP Speed Controls
        rsvpSpeedRow = findViewById(R.id.rsvpSpeedRow)
        rsvpSpeedDown = findViewById(R.id.rsvpSpeedDown)
        rsvpSpeedUp = findViewById(R.id.rsvpSpeedUp)
        rsvpWpmDisplay = findViewById(R.id.rsvpWpmDisplay)
        rsvpThetaDisplay = findViewById(R.id.rsvpThetaDisplay)

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

        findViewById<View>(R.id.settingsButton).setOnClickListener {
            haptics.tick()
            onSettingsClicked?.invoke()
        }

        // RSVP Text Loading clicks
        loadTextRow.setOnClickListener {
            haptics.tick()
            onLoadTextClicked?.invoke()
        }

        clearDocumentButton.setOnClickListener {
            haptics.tick()
            onClearDocumentClicked?.invoke()
        }

        // RSVP Speed Controls
        rsvpSpeedDown.setOnClickListener { adjustRsvpSpeed(-1) }
        rsvpSpeedUp.setOnClickListener { adjustRsvpSpeed(1) }

        updateModeSelection()
        updateDurationSelection()
        updateLoadTextVisibility()
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
        updateLoadTextVisibility()
        updateDocumentStatus()
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
        val modeButtons = mapOf(
            TherapyMode.NEUROSYNC to modeNeuroSyncButton,
            TherapyMode.MEMORY_WRITE to modeMemoryButton,
            TherapyMode.SLEEP_RAMP to modeSleepButton,
            TherapyMode.MIGRAINE to modeMigraineButton,
            TherapyMode.MOOD_LIFT to modeMoodLiftButton
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
            TherapyMode.NEUROSYNC -> context.getString(R.string.mode_neurosync_description)
            TherapyMode.MEMORY_WRITE -> context.getString(R.string.mode_memory_description)
            TherapyMode.SLEEP_RAMP -> context.getString(R.string.mode_sleep_description)
            TherapyMode.MIGRAINE -> context.getString(R.string.mode_migraine_description)
            TherapyMode.MOOD_LIFT -> context.getString(R.string.mode_mood_lift_description)
        }

        updateXrealWarning()
        updateLoadTextVisibility()
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

    private fun updateLoadTextVisibility() {
        val shouldShow = selectedMode == TherapyMode.MEMORY_WRITE
        loadTextRow.visibility = if (shouldShow) View.VISIBLE else View.GONE
        updateRsvpSpeedVisibility()
    }

    private fun updateRsvpSpeedVisibility() {
        // Show speed controls when in Learning mode and document is loaded
        val docLoaded = (settings?.rsvpDocumentWordCount ?: 0) > 0
        val shouldShow = selectedMode == TherapyMode.MEMORY_WRITE && docLoaded
        rsvpSpeedRow.visibility = if (shouldShow) View.VISIBLE else View.GONE

        if (shouldShow) {
            updateRsvpWpmDisplay()
        }
    }

    private fun adjustRsvpSpeed(direction: Int) {
        haptics.tick()

        val newIndex = (currentWpmIndex + direction).coerceIn(0, THETA_WPM_VALUES.size - 1)
        if (newIndex != currentWpmIndex) {
            currentWpmIndex = newIndex
            val newWpm = THETA_WPM_VALUES[currentWpmIndex]
            settings?.rsvpWpm = newWpm
            updateRsvpWpmDisplay()
            onRsvpWpmChanged?.invoke(newWpm)
        }
    }

    private fun updateRsvpWpmDisplay() {
        val wpm = settings?.rsvpWpm ?: 360
        // Find closest index for current WPM
        currentWpmIndex = THETA_WPM_VALUES.indexOfFirst { it >= wpm }.takeIf { it >= 0 }
            ?: (THETA_WPM_VALUES.size - 1)

        rsvpWpmDisplay.text = "$wpm WPM"
        rsvpThetaDisplay.text = formatThetaMultiple(wpm)

        // Update button states
        rsvpSpeedDown.isEnabled = currentWpmIndex > 0
        rsvpSpeedUp.isEnabled = currentWpmIndex < THETA_WPM_VALUES.size - 1
        rsvpSpeedDown.alpha = if (rsvpSpeedDown.isEnabled) 1.0f else 0.3f
        rsvpSpeedUp.alpha = if (rsvpSpeedUp.isEnabled) 1.0f else 0.3f
    }

    /**
     * Get current RSVP WPM setting.
     */
    fun getCurrentWpm(): Int = settings?.rsvpWpm ?: 360

    fun setDocumentLoaded(filename: String, wordCount: Int) {
        val wpm = settings?.rsvpWpm ?: 360
        val estimatedMinutes = (wordCount.toFloat() / wpm).toInt().coerceAtLeast(1)
        loadTextStatus.text = context.getString(R.string.document_loaded_format, filename, wordCount, estimatedMinutes)
        clearDocumentButton.visibility = View.VISIBLE
        updateRsvpSpeedVisibility()
    }

    fun clearDocument() {
        loadTextStatus.text = context.getString(R.string.no_document_loaded)
        clearDocumentButton.visibility = View.GONE
        rsvpSpeedRow.visibility = View.GONE
    }

    private fun updateDocumentStatus() {
        val docName = settings?.rsvpDocumentName
        val wordCount = settings?.rsvpDocumentWordCount ?: 0
        
        if (docName != null && wordCount > 0) {
            setDocumentLoaded(docName, wordCount)
        } else {
            clearDocument()
        }
    }
}
