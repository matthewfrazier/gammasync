package com.gammasync.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.gammasync.R
import com.gammasync.data.ColorScheme
import com.gammasync.data.SettingsRepository
import com.gammasync.domain.experience.ExperienceMode
import com.gammasync.domain.experience.ExperienceProfiles
import com.gammasync.infra.HapticFeedback
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Home screen with mode selector, duration selector, start button, and bottom navigation tabs.
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

    var onStartSession: ((durationMinutes: Int, mode: ExperienceMode) -> Unit)? = null
    var onLoadTextClicked: (() -> Unit)? = null
    var onClearDocumentClicked: (() -> Unit)? = null
    var onRsvpWpmChanged: ((Int) -> Unit)? = null
    var onColorSchemeChanged: ((ColorScheme) -> Unit)? = null
    var onDarkModeChanged: ((Boolean) -> Unit)? = null

    // Bottom navigation
    private val bottomNavigation: BottomNavigationView
    private val experienceTabContent: ScrollView
    private val historyTabContent: LinearLayout
    private val settingsTabContent: ScrollView

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

    // Duration selector buttons (Experience tab)
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

    // Settings tab views
    private val settingsDuration15: MaterialButton
    private val settingsDuration30: MaterialButton
    private val settingsDuration60: MaterialButton
    private val colorTeal: View
    private val colorBlue: View
    private val colorPurple: View
    private val colorGreen: View
    private val colorOrange: View
    private val colorRed: View
    private val darkModeButton: MaterialButton
    private val lightModeButton: MaterialButton
    private val noiseOnButton: MaterialButton
    private val noiseOffButton: MaterialButton

    // RSVP display settings (in Settings tab)
    private val rsvpSeekTextSize: SeekBar
    private val rsvpTxtTextSizeValue: TextView
    private val rsvpSwitchOrpHighlight: MaterialSwitch
    private val rsvpSwitchHyphenation: MaterialSwitch
    private val rsvpSwitchPhaseLock: MaterialSwitch

    private var selectedMode: ExperienceMode = ExperienceMode.NEUROSYNC
    private var currentWpmIndex = THETA_WPM_VALUES.indexOf(360) // Default to 1× theta
    private var selectedDuration = 30
    private var settings: SettingsRepository? = null
    private var hasExternalDisplay = false
    private val haptics = HapticFeedback(context)

    // Accent color for selected state
    private var accentColor = 0xFF26A69A.toInt()
    private var selectedColorScheme = ColorScheme.TEAL
    private var darkMode = true
    private var backgroundNoiseEnabled = true

    // Icon tint colors - theme-aware
    private val iconTintRequired: Int
        get() = MaterialColors.getColor(this, com.google.android.material.R.attr.colorError, 0xFFFF5252.toInt())
    private val iconTintConnected: Int
        get() = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, 0xFF4CAF50.toInt())

    init {
        LayoutInflater.from(context).inflate(R.layout.view_home, this, true)

        // Bottom navigation
        bottomNavigation = findViewById(R.id.bottomNavigation)
        experienceTabContent = findViewById(R.id.experienceTabContent)
        historyTabContent = findViewById(R.id.historyTabContent)
        settingsTabContent = findViewById(R.id.settingsTabContent)

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

        // Settings tab views
        settingsDuration15 = findViewById(R.id.settingsDuration15)
        settingsDuration30 = findViewById(R.id.settingsDuration30)
        settingsDuration60 = findViewById(R.id.settingsDuration60)
        colorTeal = findViewById(R.id.colorTeal)
        colorBlue = findViewById(R.id.colorBlue)
        colorPurple = findViewById(R.id.colorPurple)
        colorGreen = findViewById(R.id.colorGreen)
        colorOrange = findViewById(R.id.colorOrange)
        colorRed = findViewById(R.id.colorRed)
        darkModeButton = findViewById(R.id.darkModeButton)
        lightModeButton = findViewById(R.id.lightModeButton)
        noiseOnButton = findViewById(R.id.noiseOnButton)
        noiseOffButton = findViewById(R.id.noiseOffButton)

        // RSVP display settings (in Settings tab)
        rsvpSeekTextSize = findViewById(R.id.rsvpSeekTextSize)
        rsvpTxtTextSizeValue = findViewById(R.id.rsvpTxtTextSizeValue)
        rsvpSwitchOrpHighlight = findViewById(R.id.rsvpSwitchOrpHighlight)
        rsvpSwitchHyphenation = findViewById(R.id.rsvpSwitchHyphenation)
        rsvpSwitchPhaseLock = findViewById(R.id.rsvpSwitchPhaseLock)

        // Bottom navigation listener
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_experience -> {
                    showTab(Tab.EXPERIENCE)
                    true
                }
                R.id.nav_history -> {
                    showTab(Tab.HISTORY)
                    true
                }
                R.id.nav_settings -> {
                    showTab(Tab.SETTINGS)
                    true
                }
                else -> false
            }
        }

        // Mode button clicks
        modeNeuroSyncButton.setOnClickListener { selectMode(ExperienceMode.NEUROSYNC) }
        modeMemoryButton.setOnClickListener { selectMode(ExperienceMode.MEMORY_WRITE) }
        modeSleepButton.setOnClickListener { selectMode(ExperienceMode.SLEEP_RAMP) }
        modeMigraineButton.setOnClickListener { selectMode(ExperienceMode.MIGRAINE) }
        modeMoodLiftButton.setOnClickListener { selectMode(ExperienceMode.MOOD_LIFT) }

        // Duration button clicks (Experience tab)
        duration15Button.setOnClickListener { selectDuration(15) }
        duration30Button.setOnClickListener { selectDuration(30) }
        duration60Button.setOnClickListener { selectDuration(60) }

        startSessionButton.setOnClickListener {
            haptics.heavyClick()
            onStartSession?.invoke(selectedDuration, selectedMode)
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

        // Settings tab clicks
        settingsDuration15.setOnClickListener { selectSettingsDuration(15) }
        settingsDuration30.setOnClickListener { selectSettingsDuration(30) }
        settingsDuration60.setOnClickListener { selectSettingsDuration(60) }

        colorTeal.setOnClickListener { selectColorScheme(ColorScheme.TEAL) }
        colorBlue.setOnClickListener { selectColorScheme(ColorScheme.BLUE) }
        colorPurple.setOnClickListener { selectColorScheme(ColorScheme.PURPLE) }
        colorGreen.setOnClickListener { selectColorScheme(ColorScheme.GREEN) }
        colorOrange.setOnClickListener { selectColorScheme(ColorScheme.ORANGE) }
        colorRed.setOnClickListener { selectColorScheme(ColorScheme.RED) }

        darkModeButton.setOnClickListener { selectDarkMode(true) }
        lightModeButton.setOnClickListener { selectDarkMode(false) }

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

        rsvpSwitchPhaseLock.setOnCheckedChangeListener { _, isChecked ->
            settings?.rsvpPhaseLockEnabled = isChecked
        }

        updateModeSelection()
        updateDurationSelection()
        updateLoadTextVisibility()
        updateIconColors()
        updateColorChips()
    }

    private enum class Tab {
        EXPERIENCE, HISTORY, SETTINGS
    }

    private fun showTab(tab: Tab) {
        experienceTabContent.visibility = if (tab == Tab.EXPERIENCE) View.VISIBLE else View.GONE
        historyTabContent.visibility = if (tab == Tab.HISTORY) View.VISIBLE else View.GONE
        settingsTabContent.visibility = if (tab == Tab.SETTINGS) View.VISIBLE else View.GONE
    }

    /**
     * Navigate to the Settings tab programmatically.
     */
    fun navigateToSettingsTab() {
        bottomNavigation.selectedItemId = R.id.nav_settings
        showTab(Tab.SETTINGS)
    }

    fun bindSettings(settingsRepository: SettingsRepository) {
        settings = settingsRepository
        selectedDuration = settingsRepository.durationMinutes
        selectedMode = settingsRepository.experienceMode
        accentColor = settingsRepository.colorScheme.accentColor
        selectedColorScheme = settingsRepository.colorScheme
        darkMode = settingsRepository.darkMode
        backgroundNoiseEnabled = settingsRepository.backgroundNoiseEnabled

        // Load RSVP settings
        rsvpSeekTextSize.progress = (settingsRepository.rsvpTextSizePercent * 100).toInt()
        rsvpTxtTextSizeValue.text = "${rsvpSeekTextSize.progress}%"
        rsvpSwitchOrpHighlight.isChecked = settingsRepository.rsvpOrpHighlightEnabled
        rsvpSwitchHyphenation.isChecked = settingsRepository.rsvpHyphenationEnabled
        rsvpSwitchPhaseLock.isChecked = settingsRepository.rsvpPhaseLockEnabled

        updateAccentColor()
        updateModeSelection()
        updateDurationSelection()
        updateLoadTextVisibility()
        updateDocumentStatus()
        updateColorChips()
        updateSettingsDurationSelection()
        updateDarkModeSelection()
        updateNoiseSelection()
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

    private fun selectMode(mode: ExperienceMode) {
        haptics.tick()
        selectedMode = mode
        settings?.experienceMode = mode

        // Update duration to mode default
        val profile = ExperienceProfiles.forMode(mode)
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
            ExperienceMode.NEUROSYNC to modeNeuroSyncButton,
            ExperienceMode.MEMORY_WRITE to modeMemoryButton,
            ExperienceMode.SLEEP_RAMP to modeSleepButton,
            ExperienceMode.MIGRAINE to modeMigraineButton,
            ExperienceMode.MOOD_LIFT to modeMoodLiftButton
        )

        // Get theme-aware colors for unselected state
        val surfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0)
        val onSurfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0)
        val onPrimaryColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF.toInt())

        modeButtons.forEach { (mode, button) ->
            val isSelected = mode == selectedMode
            if (isSelected) {
                button.backgroundTintList = ColorStateList.valueOf(accentColor)
                button.setTextColor(onPrimaryColor)
            } else {
                // Transparent background with theme-appropriate text color
                button.backgroundTintList = ColorStateList.valueOf(surfaceColor)
                button.setTextColor(onSurfaceColor)
            }
        }

        // Update description
        modeDescriptionText.text = when (selectedMode) {
            ExperienceMode.NEUROSYNC -> context.getString(R.string.mode_neurosync_description)
            ExperienceMode.MEMORY_WRITE -> context.getString(R.string.mode_memory_description)
            ExperienceMode.SLEEP_RAMP -> context.getString(R.string.mode_sleep_description)
            ExperienceMode.MIGRAINE -> context.getString(R.string.mode_migraine_description)
            ExperienceMode.MOOD_LIFT -> context.getString(R.string.mode_mood_lift_description)
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

    private fun updateLoadTextVisibility() {
        val shouldShow = selectedMode == ExperienceMode.MEMORY_WRITE
        loadTextRow.visibility = if (shouldShow) View.VISIBLE else View.GONE
        updateRsvpSpeedVisibility()
    }

    private fun updateRsvpSpeedVisibility() {
        // WPM controls are always visible in Settings tab - just update the display
        updateRsvpWpmDisplay()
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
        // WPM controls are now in Settings tab and always visible
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

    // --- Settings Tab Methods ---

    private fun selectSettingsDuration(minutes: Int) {
        haptics.tick()
        selectedDuration = minutes
        settings?.durationMinutes = minutes
        updateSettingsDurationSelection()
        updateDurationSelection() // Also update experience tab
    }

    private fun updateSettingsDurationSelection() {
        val durationButtons = listOf(
            15 to settingsDuration15,
            30 to settingsDuration30,
            60 to settingsDuration60
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
        accentColor = scheme.accentColor
        settings?.colorScheme = scheme
        updateColorChips()
        updateDurationSelection()
        updateSettingsDurationSelection()
        updateDarkModeSelection()
        updateNoiseSelection()
        updateModeSelection()
        updateAccentColor()
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
