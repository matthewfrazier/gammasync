package com.gammasync

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.gammasync.data.SettingsRepository
import com.gammasync.domain.therapy.TherapyMode
import com.gammasync.domain.therapy.TherapyProfile
import com.gammasync.domain.therapy.TherapyProfiles
import com.gammasync.infra.ExternalDisplayManager
import com.gammasync.infra.GammaPresentation
import com.gammasync.infra.HapticFeedback
import com.gammasync.infra.UniversalAudioEngine
import com.gammasync.infra.UniversalVisualRenderer
import com.gammasync.ui.CircularTimerView
import com.gammasync.ui.HomeView
import com.gammasync.ui.SafetyDisclaimerView
import com.gammasync.ui.SessionCompleteView
import com.gammasync.ui.SettingsView
import com.google.android.material.button.MaterialButton
import android.content.res.ColorStateList

class MainActivity : AppCompatActivity(), ExternalDisplayManager.DisplayListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    enum class Screen {
        DISCLAIMER,  // 0
        HOME,        // 1
        THERAPY,     // 2
        COMPLETE,    // 3
        SETTINGS     // 4
    }

    // Views
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var disclaimerScreen: SafetyDisclaimerView
    private lateinit var homeScreen: HomeView
    private lateinit var therapyScreen: FrameLayout
    private lateinit var completeScreen: SessionCompleteView
    private lateinit var settingsScreen: SettingsView

    // Therapy screen views
    private lateinit var circularTimer: CircularTimerView
    private lateinit var pauseButton: MaterialButton
    private lateinit var pauseOverlay: View
    private lateinit var pauseDurationText: TextView
    private lateinit var resumeButton: MaterialButton
    private lateinit var doneButton: MaterialButton
    private lateinit var phoneVisualRenderer: UniversalVisualRenderer
    private lateinit var therapyControlsContainer: View
    private var controlsVisible = false
    private var isPaused = false

    // Auto-hide timer controls
    private val autoHideRunnable = Runnable { hideTherapyControls() }
    private val autoHideDelayMs = 3000L

    private val handler = Handler(Looper.getMainLooper())
    private var remainingSeconds = 0
    private var sessionDurationMinutes = 30
    private var isRunning = false
    private var savedBrightness = -1f

    // Universal audio engine (replaces GammaAudioEngine)
    private val audioEngine = UniversalAudioEngine()
    private lateinit var haptics: HapticFeedback
    private lateinit var settings: SettingsRepository

    // Current therapy session
    private var currentProfile: TherapyProfile = TherapyProfiles.NEUROSYNC

    // External display (XREAL) support
    private lateinit var externalDisplayManager: ExternalDisplayManager
    private var externalPresentation: GammaPresentation? = null
    private var hasExternalDisplay = false

    private val timerRunnable = object : Runnable {
        override fun run() {
            remainingSeconds--
            updateTimerDisplay()
            if (remainingSeconds <= 0) {
                onSessionTimerComplete()
            } else {
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply saved dark/light mode preference before setting content view
        val prefs = getSharedPreferences("gammasync_settings", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", true)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        haptics = HapticFeedback(this)
        settings = SettingsRepository(this)

        initViews()
        setupScreens()

        // Start listening for external displays (XREAL, etc.)
        externalDisplayManager = ExternalDisplayManager(this)
        externalDisplayManager.startListening(this)

        // Skip disclaimer if already accepted, otherwise show it
        if (settings.disclaimerAccepted) {
            navigateTo(Screen.HOME)
        } else {
            navigateTo(Screen.DISCLAIMER)
        }
    }

    private fun initViews() {
        viewFlipper = findViewById(R.id.viewFlipper)
        disclaimerScreen = findViewById(R.id.disclaimerScreen)
        homeScreen = findViewById(R.id.homeScreen)
        therapyScreen = findViewById(R.id.therapyScreen)
        completeScreen = findViewById(R.id.completeScreen)
        settingsScreen = findViewById(R.id.settingsScreen)

        // Therapy screen views
        circularTimer = findViewById(R.id.circularTimer)
        pauseButton = findViewById(R.id.pauseButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        pauseDurationText = findViewById(R.id.pauseDurationText)
        resumeButton = findViewById(R.id.resumeButton)
        doneButton = findViewById(R.id.doneButton)
        phoneVisualRenderer = findViewById(R.id.phoneVisualRenderer)
        therapyControlsContainer = findViewById(R.id.therapyControlsContainer)

        // Connect phone visual renderer to audio engine phase
        phoneVisualRenderer.setPhaseProvider { audioEngine.phase }
        phoneVisualRenderer.setSecondaryPhaseProvider { audioEngine.secondaryPhase }

        // Tap therapy screen to toggle controls visibility
        therapyScreen.setOnClickListener { toggleTherapyControls() }
    }

    private fun setupScreens() {
        // Disclaimer screen
        disclaimerScreen.onDisclaimerAccepted = {
            settings.disclaimerAccepted = true
            navigateTo(Screen.HOME)
        }

        // Home screen
        homeScreen.bindSettings(settings)
        homeScreen.onStartSession = { durationMinutes, mode ->
            startSession(durationMinutes, mode)
        }
        homeScreen.onSettingsClicked = {
            navigateTo(Screen.SETTINGS)
        }

        // Therapy screen - pause/resume/done buttons
        pauseButton.setOnClickListener { pauseSession() }
        resumeButton.setOnClickListener { resumeSession() }
        doneButton.setOnClickListener { stopSession() }

        // Complete screen
        completeScreen.onStartAnother = {
            navigateTo(Screen.HOME)
        }
        completeScreen.onExit = {
            finish()
        }

        // Settings screen
        settingsScreen.bindSettings(settings)
        settingsScreen.onBackClicked = {
            homeScreen.bindSettings(settings)
            navigateTo(Screen.HOME)
        }
        settingsScreen.onColorSchemeChanged = {
            // Refresh home screen with new color scheme (no need to recreate)
            homeScreen.bindSettings(settings)
        }
        settingsScreen.onDarkModeChanged = {
            // Refresh home screen with new theme
            homeScreen.bindSettings(settings)
        }
    }

    private fun navigateTo(screen: Screen) {
        viewFlipper.displayedChild = screen.ordinal

        // Enter immersive mode for therapy, exit for other screens
        if (screen == Screen.THERAPY) {
            enterImmersiveMode()
        } else {
            exitImmersiveMode()
        }

        Log.i(TAG, "Navigated to: $screen")
    }

    private fun enterImmersiveMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    private fun exitImmersiveMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    // --- External Display Callbacks ---

    override fun onExternalDisplayConnected(display: Display) {
        Log.i(TAG, "External display connected: ${display.name} @ ${display.refreshRate}Hz")
        hasExternalDisplay = true
        homeScreen.setHasExternalDisplay(true)
        haptics.heavyClick()

        externalPresentation = GammaPresentation(this, display).apply {
            configure(currentProfile)
            setPhaseProvider { audioEngine.phase }
            setSecondaryPhaseProvider { audioEngine.secondaryPhase }
            show()
        }

        if (isRunning) {
            externalPresentation?.apply {
                setTotalDuration(sessionDurationMinutes * 60)
                setRemainingTime(remainingSeconds)
                startRendering()
                if (controlsVisible) showTimer()
            }
            // Stop phone renderer (XREAL shows the therapy visual now)
            phoneVisualRenderer.stop()
            phoneVisualRenderer.visibility = View.GONE
        }

        Log.i(TAG, "Display mode: XREAL (external), profile: ${currentProfile.mode.displayName}")
    }

    override fun onExternalDisplayDisconnected() {
        Log.i(TAG, "External display disconnected")
        hasExternalDisplay = false
        homeScreen.setHasExternalDisplay(false)
        haptics.click()

        externalPresentation?.dismiss()
        externalPresentation = null

        // Restore phone visual renderer if session running
        if (isRunning && !isPaused) {
            phoneVisualRenderer.configure(currentProfile)
            phoneVisualRenderer.visibility = View.VISIBLE
            phoneVisualRenderer.start()
            showTherapyControls()
        }

        Log.i(TAG, "Display mode: Phone")
    }

    // --- Session Control ---

    private fun startSession(durationMinutes: Int, mode: TherapyMode) {
        // Get the therapy profile for the selected mode
        currentProfile = TherapyProfiles.forMode(mode)
        sessionDurationMinutes = durationMinutes
        remainingSeconds = durationMinutes * 60

        Log.i(TAG, "Starting session: ${mode.displayName}, duration=${durationMinutes}min")

        // Apply accent color to therapy screen buttons
        val accentColor = settings.colorScheme.accentColor
        resumeButton.backgroundTintList = ColorStateList.valueOf(accentColor)
        doneButton.strokeColor = ColorStateList.valueOf(accentColor)
        doneButton.setTextColor(accentColor)
        pauseButton.strokeColor = ColorStateList.valueOf(accentColor)
        pauseButton.setTextColor(accentColor)
        circularTimer.setAccentColor(accentColor)

        // Initialize timer
        circularTimer.setTotalDuration(remainingSeconds)
        updateTimerDisplay()

        navigateTo(Screen.THERAPY)

        // Apply max brightness if enabled (or if profile requires it)
        val needsMaxBrightness = settings.maxBrightness ||
            currentProfile.visualConfig.maxBrightnessNits < Float.MAX_VALUE
        if (needsMaxBrightness) {
            savedBrightness = window.attributes.screenBrightness
            window.attributes = window.attributes.apply {
                // For migraine mode, use low brightness
                screenBrightness = if (currentProfile.visualConfig.maxBrightnessNits < Float.MAX_VALUE) {
                    // Map nits to brightness (rough approximation: 20 nits ~ 0.05)
                    (currentProfile.visualConfig.maxBrightnessNits / 400f).coerceIn(0.01f, 1f)
                } else {
                    1f
                }
            }
        }

        isRunning = true

        // Start audio with the selected profile
        audioEngine.start(
            currentProfile,
            amplitude = settings.audioAmplitude.toDouble(),
            noiseEnabled = settings.backgroundNoiseEnabled
        )

        if (hasExternalDisplay) {
            externalPresentation?.apply {
                configure(currentProfile)
                setTotalDuration(remainingSeconds)
                setRemainingTime(remainingSeconds)
                startRendering()
            }
            // Phone shows controls only when XREAL connected
            phoneVisualRenderer.visibility = View.GONE
        } else {
            // Show full therapy visual on phone when no external display
            phoneVisualRenderer.configure(currentProfile)
            phoneVisualRenderer.visibility = View.VISIBLE
            phoneVisualRenderer.start()
        }

        // Show controls initially
        showTherapyControls()

        handler.post(timerRunnable)
    }

    private fun pauseSession() {
        if (isRunning && !isPaused) {
            haptics.heavyClick()
            isPaused = true

            // Stop audio and rendering but keep timer paused
            audioEngine.stop()
            phoneVisualRenderer.stop()
            externalPresentation?.stopRendering()
            handler.removeCallbacks(timerRunnable)
            handler.removeCallbacks(autoHideRunnable)

            // Show pause overlay with session duration
            pauseDurationText.text = "$sessionDurationMinutes min session"
            pauseOverlay.visibility = View.VISIBLE
            therapyControlsContainer.visibility = View.GONE
        }
    }

    private fun resumeSession() {
        if (isRunning && isPaused) {
            haptics.heavyClick()
            isPaused = false

            // Hide pause overlay
            pauseOverlay.visibility = View.GONE

            // Restart audio and rendering
            audioEngine.start(
                currentProfile,
                amplitude = settings.audioAmplitude.toDouble(),
                noiseEnabled = settings.backgroundNoiseEnabled
            )

            if (hasExternalDisplay) {
                externalPresentation?.startRendering()
            } else {
                phoneVisualRenderer.start()
            }

            // Resume timer
            handler.post(timerRunnable)

            // Show controls briefly then auto-hide
            showTherapyControls()
        }
    }

    private fun stopSession() {
        haptics.heavyClick()
        isRunning = false
        isPaused = false

        phoneVisualRenderer.stop()
        externalPresentation?.stopRendering()

        audioEngine.stop()
        handler.removeCallbacks(timerRunnable)
        handler.removeCallbacks(autoHideRunnable)

        restoreBrightness()

        phoneVisualRenderer.visibility = View.GONE
        therapyControlsContainer.visibility = View.GONE
        pauseOverlay.visibility = View.GONE
        controlsVisible = false

        // Go straight to home menu
        navigateTo(Screen.HOME)
    }

    private fun onSessionTimerComplete() {
        haptics.heavyClick()
        isRunning = false
        isPaused = false

        phoneVisualRenderer.stop()
        externalPresentation?.stopRendering()

        audioEngine.stop()
        handler.removeCallbacks(timerRunnable)
        handler.removeCallbacks(autoHideRunnable)

        restoreBrightness()

        phoneVisualRenderer.visibility = View.GONE
        therapyControlsContainer.visibility = View.GONE
        pauseOverlay.visibility = View.GONE
        controlsVisible = false

        completeScreen.setSessionDuration(sessionDurationMinutes)
        navigateTo(Screen.COMPLETE)
    }

    private fun toggleTherapyControls() {
        if (!isRunning || isPaused) return

        if (controlsVisible) {
            hideTherapyControls()
        } else {
            showTherapyControls()
        }
        haptics.tick()
    }

    private fun showTherapyControls() {
        controlsVisible = true
        therapyControlsContainer.visibility = View.VISIBLE

        if (hasExternalDisplay) {
            // Show timer on XREAL display too
            externalPresentation?.showTimer()
        }

        // Schedule auto-hide after delay
        handler.removeCallbacks(autoHideRunnable)
        handler.postDelayed(autoHideRunnable, autoHideDelayMs)
    }

    private fun hideTherapyControls() {
        controlsVisible = false
        therapyControlsContainer.visibility = View.GONE

        // Hide timer on XREAL display too
        externalPresentation?.hideTimer()
    }

    private fun restoreBrightness() {
        if (savedBrightness >= 0f) {
            window.attributes = window.attributes.apply {
                screenBrightness = savedBrightness
            }
            savedBrightness = -1f
        }
    }

    private fun updateTimerDisplay() {
        circularTimer.setRemainingTime(remainingSeconds)
        externalPresentation?.setRemainingTime(remainingSeconds)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.i(TAG, "Configuration changed: ${newConfig.orientation}")
    }

    override fun onDestroy() {
        super.onDestroy()
        externalDisplayManager.stopListening()
        externalPresentation?.dismiss()
        phoneVisualRenderer.stop()
        audioEngine.release()
        handler.removeCallbacks(timerRunnable)
        handler.removeCallbacks(autoHideRunnable)
    }
}
