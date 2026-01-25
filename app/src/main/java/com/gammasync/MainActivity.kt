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
import com.gammasync.infra.ExternalDisplayManager
import com.gammasync.infra.GammaAudioEngine
import com.gammasync.infra.GammaPresentation
import com.gammasync.infra.HapticFeedback
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

    private val audioEngine = GammaAudioEngine()
    private lateinit var haptics: HapticFeedback
    private lateinit var settings: SettingsRepository

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
        homeScreen.onStartSession = { durationMinutes ->
            startSession(durationMinutes)
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
        haptics.heavyClick()

        externalPresentation = GammaPresentation(this, display).apply {
            setPhaseProvider { audioEngine.phase }
            show()
        }

        if (isRunning) {
            externalPresentation?.startRendering()
            // Hide phone UI when external display active
            subtleFlickerOverlay.stop()
            subtleFlickerOverlay.visibility = View.GONE
            therapyControlsContainer.visibility = View.GONE
            controlsVisible = false
        }

        Log.i(TAG, "Display mode: XREAL (external)")
    }

    override fun onExternalDisplayDisconnected() {
        Log.i(TAG, "External display disconnected")
        hasExternalDisplay = false
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

    private fun startSession(durationMinutes: Int) {
        sessionDurationMinutes = durationMinutes
        remainingSeconds = durationMinutes * 60

        // Initialize timer
        circularTimer.setTotalDuration(remainingSeconds)
        updateTimerDisplay()

        navigateTo(Screen.THERAPY)

        // Apply max brightness if enabled
        if (settings.maxBrightness) {
            savedBrightness = window.attributes.screenBrightness
            window.attributes = window.attributes.apply {
                screenBrightness = 1f
            }
        }

        isRunning = true
        audioEngine.start(amplitude = settings.audioAmplitude.toDouble())

        if (hasExternalDisplay) {
            externalPresentation?.startRendering()
            // Phone screen stays black when using external display
            subtleFlickerOverlay.visibility = View.GONE
            therapyControlsContainer.visibility = View.GONE
            controlsVisible = false
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
        if (!isRunning || hasExternalDisplay) return

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
        subtleFlickerOverlay.visibility = View.VISIBLE
        subtleFlickerOverlay.start()
    }

    private fun hideTherapyControls() {
        controlsVisible = false
        therapyControlsContainer.visibility = View.GONE
        subtleFlickerOverlay.stop()
        subtleFlickerOverlay.visibility = View.GONE
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
