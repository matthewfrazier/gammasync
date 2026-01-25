package com.gammasync

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import com.gammasync.data.SettingsRepository
import com.gammasync.domain.therapy.TherapyMode
import com.gammasync.domain.therapy.TherapyProfile
import com.gammasync.domain.therapy.TherapyProfiles
import com.gammasync.infra.ExternalDisplayManager
import com.gammasync.infra.GammaPresentation
import com.gammasync.infra.HapticFeedback
import com.gammasync.infra.UniversalAudioEngine
import com.gammasync.ui.CircularTimerView
import com.gammasync.ui.SubtleFlickerOverlay
import com.gammasync.ui.HomeView
import com.gammasync.ui.SafetyDisclaimerView
import com.gammasync.ui.SessionCompleteView
import com.gammasync.ui.SettingsView

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
    private lateinit var stopButton: Button
    private lateinit var subtleFlickerOverlay: SubtleFlickerOverlay
    private lateinit var therapyControlsContainer: View
    private var controlsVisible = false

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
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        haptics = HapticFeedback(this)
        settings = SettingsRepository(this)

        initViews()
        setupScreens()

        // Start listening for external displays (XREAL, etc.)
        externalDisplayManager = ExternalDisplayManager(this)
        externalDisplayManager.startListening(this)

        // Always start at disclaimer screen
        navigateTo(Screen.DISCLAIMER)
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
        stopButton = findViewById(R.id.stopButton)
        subtleFlickerOverlay = findViewById(R.id.subtleFlickerOverlay)
        therapyControlsContainer = findViewById(R.id.therapyControlsContainer)

        // Connect subtle flicker overlay to audio engine phase
        subtleFlickerOverlay.setPhaseProvider { audioEngine.phase }

        // Tap therapy screen to toggle controls visibility
        therapyScreen.setOnClickListener { toggleTherapyControls() }
    }

    private fun setupScreens() {
        // Disclaimer screen
        disclaimerScreen.onDisclaimerAccepted = {
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
        homeScreen.onMaxBrightnessChanged = { enabled ->
            window.attributes = window.attributes.apply {
                screenBrightness = if (enabled) 1f else -1f
            }
        }

        // Therapy screen - stop button
        stopButton.setOnClickListener { stopSession() }

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
    }

    private fun navigateTo(screen: Screen) {
        viewFlipper.displayedChild = screen.ordinal
        Log.i(TAG, "Navigated to: $screen")
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
            // Stop subtle flicker on phone (XREAL shows real flicker)
            subtleFlickerOverlay.stop()
            subtleFlickerOverlay.visibility = View.GONE
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

        // Restore phone UI if session running
        if (isRunning) {
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
        audioEngine.start(currentProfile, amplitude = settings.audioAmplitude.toDouble())

        if (hasExternalDisplay) {
            externalPresentation?.apply {
                configure(currentProfile)
                setTotalDuration(remainingSeconds)
                setRemainingTime(remainingSeconds)
                startRendering()
            }
            // Show controls initially (on both phone and XREAL)
            showTherapyControls()
        } else {
            // Show controls initially on phone
            showTherapyControls()
        }

        handler.post(timerRunnable)
    }

    private fun stopSession() {
        if (isRunning) {
            haptics.heavyClick()
            isRunning = false

            subtleFlickerOverlay.stop()
            externalPresentation?.stopRendering()

            audioEngine.stop()
            handler.removeCallbacks(timerRunnable)

            restoreBrightness()

            subtleFlickerOverlay.visibility = View.GONE
            therapyControlsContainer.visibility = View.GONE
            controlsVisible = false

            // Show complete screen with actual duration
            val completedMinutes = sessionDurationMinutes - (remainingSeconds / 60)
            completeScreen.setSessionDuration(if (completedMinutes > 0) completedMinutes else 1)
            navigateTo(Screen.COMPLETE)
        }
    }

    private fun onSessionTimerComplete() {
        haptics.heavyClick()
        isRunning = false

        subtleFlickerOverlay.stop()
        externalPresentation?.stopRendering()

        audioEngine.stop()
        handler.removeCallbacks(timerRunnable)

        restoreBrightness()

        subtleFlickerOverlay.visibility = View.GONE
        therapyControlsContainer.visibility = View.GONE
        controlsVisible = false

        completeScreen.setSessionDuration(sessionDurationMinutes)
        navigateTo(Screen.COMPLETE)
    }

    private fun toggleTherapyControls() {
        if (!isRunning) return

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
        } else {
            // Show subtle flicker on phone when no external display
            // But only if the mode has visual flicker
            if (currentProfile.hasVisualFlicker) {
                subtleFlickerOverlay.visibility = View.VISIBLE
                subtleFlickerOverlay.start()
            }
        }
    }

    private fun hideTherapyControls() {
        controlsVisible = false
        therapyControlsContainer.visibility = View.GONE
        subtleFlickerOverlay.stop()
        subtleFlickerOverlay.visibility = View.GONE

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
        subtleFlickerOverlay.stop()
        audioEngine.release()
        handler.removeCallbacks(timerRunnable)
    }
}
