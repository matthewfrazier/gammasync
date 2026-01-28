package com.gammasync

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.gammasync.data.SettingsRepository
import com.gammasync.domain.rsvp.ProcessedDocument
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
import com.gammasync.ui.RsvpOverlay
import com.gammasync.ui.RsvpDetailsView
import com.gammasync.utils.DocumentLoader
import com.gammasync.utils.TextProcessor
import com.google.android.material.button.MaterialButton
import android.content.res.ColorStateList

class MainActivity : AppCompatActivity(), ExternalDisplayManager.DisplayListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    enum class Screen {
        DISCLAIMER,    // 0
        HOME,          // 1
        THERAPY,       // 2
        COMPLETE,      // 3
        RSVP_DETAILS   // 4
    }

    // Views
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var disclaimerScreen: SafetyDisclaimerView
    private lateinit var homeScreen: HomeView
    private lateinit var therapyScreen: FrameLayout
    private lateinit var completeScreen: SessionCompleteView
    private lateinit var rsvpDetailsScreen: RsvpDetailsView

    // Session screen views
    private lateinit var circularTimer: CircularTimerView
    private lateinit var pauseButton: MaterialButton
    private lateinit var noiseToggleButton: MaterialButton
    private lateinit var pauseOverlay: View
    private lateinit var pauseDurationText: TextView
    private lateinit var resumeButton: MaterialButton
    private lateinit var doneButton: MaterialButton
    private lateinit var phoneVisualRenderer: UniversalVisualRenderer
    private lateinit var rsvpOverlay: RsvpOverlay
    private lateinit var therapyControlsContainer: View
    private var controlsVisible = false
    private var isPaused = false

    // RSVP WPM controls on session screen
    private lateinit var rsvpWpmContainer: View
    private lateinit var therapyWpmDisplay: TextView
    private lateinit var therapyThetaDisplay: TextView
    private lateinit var therapyRsvpSpeedDown: MaterialButton
    private lateinit var therapyRsvpSpeedUp: MaterialButton

    // RSVP document state
    private var loadedWords: List<String> = emptyList()

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

    // Current session profile
    private var currentProfile: TherapyProfile = TherapyProfiles.NEUROSYNC

    // External display (XREAL) support
    private lateinit var externalDisplayManager: ExternalDisplayManager
    private var externalPresentation: GammaPresentation? = null
    private var hasExternalDisplay = false

    // Document picker for RSVP text loading
    private val documentPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleDocumentSelected(it) }
    }

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

        // Handle shared content from other apps
        handleSharedContent(intent)

        // Skip disclaimer if already accepted, otherwise show it
        if (settings.disclaimerAccepted) {
            navigateTo(Screen.HOME)
        } else {
            navigateTo(Screen.DISCLAIMER)
        }
    }

    /**
     * Handle shared text/URLs from other apps via share target.
     */
    private fun handleSharedContent(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain") {
            return
        }

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        Log.i(TAG, "Received shared content: ${sharedText.take(100)}...")

        // Navigate to RSVP details screen after disclaimer check
        handler.post {
            if (settings.disclaimerAccepted) {
                rsvpDetailsScreen.loadSettings(settings)
                rsvpDetailsScreen.handleSharedContent(sharedText)
                navigateTo(Screen.RSVP_DETAILS)
            } else {
                // Store for later processing after disclaimer
                disclaimerScreen.onDisclaimerAccepted = {
                    settings.disclaimerAccepted = true
                    rsvpDetailsScreen.loadSettings(settings)
                    rsvpDetailsScreen.handleSharedContent(sharedText)
                    navigateTo(Screen.RSVP_DETAILS)
                }
            }
        }
    }

    private fun initViews() {
        viewFlipper = findViewById(R.id.viewFlipper)
        disclaimerScreen = findViewById(R.id.disclaimerScreen)
        homeScreen = findViewById(R.id.homeScreen)
        therapyScreen = findViewById(R.id.therapyScreen)
        completeScreen = findViewById(R.id.completeScreen)
        rsvpDetailsScreen = findViewById(R.id.rsvpDetailsScreen)

        // Session screen views
        circularTimer = findViewById(R.id.circularTimer)
        pauseButton = findViewById(R.id.pauseButton)
        noiseToggleButton = findViewById(R.id.noiseToggleButton)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        pauseDurationText = findViewById(R.id.pauseDurationText)
        resumeButton = findViewById(R.id.resumeButton)
        doneButton = findViewById(R.id.doneButton)
        phoneVisualRenderer = findViewById(R.id.phoneVisualRenderer)
        rsvpOverlay = findViewById(R.id.rsvpOverlay)
        therapyControlsContainer = findViewById(R.id.therapyControlsContainer)

        // RSVP WPM controls on session screen
        rsvpWpmContainer = findViewById(R.id.rsvpWpmContainer)
        therapyWpmDisplay = findViewById(R.id.therapyWpmDisplay)
        therapyThetaDisplay = findViewById(R.id.therapyThetaDisplay)
        therapyRsvpSpeedDown = findViewById(R.id.therapyRsvpSpeedDown)
        therapyRsvpSpeedUp = findViewById(R.id.therapyRsvpSpeedUp)

        // Connect phone visual renderer to audio engine phase
        phoneVisualRenderer.setPhaseProvider { audioEngine.phase }
        phoneVisualRenderer.setSecondaryPhaseProvider { audioEngine.secondaryPhase }

        // Tap session screen to toggle controls visibility
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
        homeScreen.onLoadTextClicked = {
            rsvpDetailsScreen.loadSettings(settings)
            navigateTo(Screen.RSVP_DETAILS)
        }
        homeScreen.onClearDocumentClicked = {
            clearDocument()
        }
        homeScreen.onColorSchemeChanged = {
            // Settings changed from embedded tab - rebind to refresh
            homeScreen.bindSettings(settings)
        }
        homeScreen.onDarkModeChanged = {
            // Theme changed from embedded tab - rebind to refresh
            homeScreen.bindSettings(settings)
        }

        // Restore previously loaded document if any
        restoreSavedDocument()

        // Session screen - pause/resume/done buttons
        pauseButton.setOnClickListener { pauseSession() }
        noiseToggleButton.setOnClickListener { toggleBackgroundNoise() }
        resumeButton.setOnClickListener { resumeSession() }
        doneButton.setOnClickListener { stopSession() }

        // Session screen - RSVP WPM controls
        therapyRsvpSpeedDown.setOnClickListener { adjustTherapyRsvpSpeed(-1) }
        therapyRsvpSpeedUp.setOnClickListener { adjustTherapyRsvpSpeed(1) }

        // HomeView RSVP WPM change callback
        homeScreen.onRsvpWpmChanged = { wpm ->
            updateRsvpWpm(wpm)
        }

        // Complete screen
        completeScreen.onStartAnother = {
            navigateTo(Screen.HOME)
        }
        completeScreen.onExit = {
            finish()
        }

        // RSVP Details screen
        rsvpDetailsScreen.onBackPressed = {
            navigateTo(Screen.HOME)
        }
        rsvpDetailsScreen.onFileSelectRequested = {
            openDocumentPicker()
        }
        rsvpDetailsScreen.onDocumentSelected = { document ->
            handleProcessedDocument(document)
            navigateTo(Screen.HOME)
        }
        rsvpDetailsScreen.onRsvpSettingsClicked = {
            navigateTo(Screen.HOME)
            homeScreen.navigateToSettingsTab()
        }
    }

    private fun navigateTo(screen: Screen) {
        viewFlipper.displayedChild = screen.ordinal

        // Enter immersive mode for sessions, exit for other screens
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
            // Stop phone renderer (XREAL shows the session visual now)
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
        // Get the experience profile for the selected mode
        currentProfile = TherapyProfiles.forMode(mode)
        sessionDurationMinutes = durationMinutes
        remainingSeconds = durationMinutes * 60

        Log.i(TAG, "Starting session: ${mode.displayName}, duration=${durationMinutes}min")

        // Apply accent color to session screen buttons
        val accentColor = settings.colorScheme.accentColor
        resumeButton.backgroundTintList = ColorStateList.valueOf(accentColor)
        doneButton.strokeColor = ColorStateList.valueOf(accentColor)
        doneButton.setTextColor(accentColor)
        pauseButton.strokeColor = ColorStateList.valueOf(accentColor)
        pauseButton.setTextColor(accentColor)
        noiseToggleButton.strokeColor = ColorStateList.valueOf(accentColor)
        noiseToggleButton.setTextColor(accentColor)
        circularTimer.setAccentColor(accentColor)

        // Initialize noise toggle button state
        updateNoiseToggleButton()

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
            // Show full session visual on phone when no external display
            phoneVisualRenderer.configure(currentProfile)
            phoneVisualRenderer.visibility = View.VISIBLE
            phoneVisualRenderer.start()
        }

        // Show controls initially
        showTherapyControls()

        // Start RSVP if in Learning mode with loaded document
        startRsvp()

        handler.post(timerRunnable)
    }

    private fun pauseSession() {
        if (isRunning && !isPaused) {
            haptics.heavyClick()
            isPaused = true

            // Stop audio and rendering but keep timer paused
            audioEngine.stop()
            phoneVisualRenderer.stop()
            rsvpOverlay.stop()
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

            // Resume RSVP if applicable
            if (currentProfile.mode == TherapyMode.MEMORY_WRITE && loadedWords.isNotEmpty()) {
                rsvpOverlay.start()
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
        stopRsvp()
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
        stopRsvp()
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

    private fun toggleBackgroundNoise() {
        if (!isRunning) return

        haptics.tick()

        // Toggle the setting
        settings.backgroundNoiseEnabled = !settings.backgroundNoiseEnabled

        // Restart audio engine to immediately apply the noise setting change
        // This ensures the setting takes effect without waiting for the next buffer cycle
        audioEngine.stop()
        audioEngine.start(
            currentProfile,
            amplitude = settings.audioAmplitude.toDouble(),
            noiseEnabled = settings.backgroundNoiseEnabled
        )

        // Update the button icon
        updateNoiseToggleButton()

        Log.i(TAG, "Background noise toggled: ${settings.backgroundNoiseEnabled}")
    }

    private fun updateNoiseToggleButton() {
        val iconRes = if (settings.backgroundNoiseEnabled) {
            R.drawable.ic_noise_on
        } else {
            R.drawable.ic_noise_off
        }
        noiseToggleButton.setIconResource(iconRes)
    }

    // --- RSVP WPM Controls ---

    private fun adjustTherapyRsvpSpeed(direction: Int) {
        if (!isRunning) return

        haptics.tick()

        val wpmValues = HomeView.THETA_WPM_VALUES
        val currentWpm = settings.rsvpWpm
        val currentIndex = wpmValues.indexOfFirst { it >= currentWpm }.takeIf { it >= 0 }
            ?: (wpmValues.size - 1)

        val newIndex = (currentIndex + direction).coerceIn(0, wpmValues.size - 1)
        if (newIndex != currentIndex) {
            val newWpm = wpmValues[newIndex]
            updateRsvpWpm(newWpm)
        }
    }

    private fun updateRsvpWpm(wpm: Int) {
        settings.rsvpWpm = wpm

        // Update session screen display
        therapyWpmDisplay.text = "$wpm WPM"
        therapyThetaDisplay.text = HomeView.formatThetaMultiple(wpm)

        // Update button states
        val wpmValues = HomeView.THETA_WPM_VALUES
        val currentIndex = wpmValues.indexOfFirst { it >= wpm }.takeIf { it >= 0 }
            ?: (wpmValues.size - 1)
        therapyRsvpSpeedDown.isEnabled = currentIndex > 0
        therapyRsvpSpeedUp.isEnabled = currentIndex < wpmValues.size - 1
        therapyRsvpSpeedDown.alpha = if (therapyRsvpSpeedDown.isEnabled) 1.0f else 0.3f
        therapyRsvpSpeedUp.alpha = if (therapyRsvpSpeedUp.isEnabled) 1.0f else 0.3f

        // Update RSVP overlay if running
        if (rsvpOverlay.playing) {
            rsvpOverlay.setWpm(wpm)
        }

        Log.i(TAG, "RSVP WPM changed to $wpm (${HomeView.formatThetaMultiple(wpm)})")
    }

    private fun updateTherapyWpmDisplay() {
        val wpm = settings.rsvpWpm
        therapyWpmDisplay.text = "$wpm WPM"
        therapyThetaDisplay.text = HomeView.formatThetaMultiple(wpm)

        // Update button states
        val wpmValues = HomeView.THETA_WPM_VALUES
        val currentIndex = wpmValues.indexOfFirst { it >= wpm }.takeIf { it >= 0 }
            ?: (wpmValues.size - 1)
        therapyRsvpSpeedDown.isEnabled = currentIndex > 0
        therapyRsvpSpeedUp.isEnabled = currentIndex < wpmValues.size - 1
        therapyRsvpSpeedDown.alpha = if (therapyRsvpSpeedDown.isEnabled) 1.0f else 0.3f
        therapyRsvpSpeedUp.alpha = if (therapyRsvpSpeedUp.isEnabled) 1.0f else 0.3f
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

    // --- RSVP Document Management ---

    private fun openDocumentPicker() {
        documentPickerLauncher.launch(arrayOf("text/plain"))
    }

    private fun handleDocumentSelected(uri: Uri) {
        lifecycleScope.launch {
            try {
                val docInfo = DocumentLoader.loadDocument(this@MainActivity, uri)
                if (docInfo != null) {
                    // Take persistent permission to access this URI
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    // Pass to RsvpDetailsView for preview
                    rsvpDetailsScreen.onFileSelected(uri, docInfo.filename, docInfo.text)

                    Log.i(TAG, "Document selected: ${docInfo.filename} (${docInfo.wordCount} words)")
                } else {
                    Log.w(TAG, "Failed to load document")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to get persistent permission", e)
            }
        }
    }

    private fun handleProcessedDocument(document: ProcessedDocument) {
        // Save document metadata
        settings.rsvpDocumentUri = document.sourceId
        settings.rsvpDocumentName = document.displayName
        settings.rsvpDocumentWordCount = document.totalWords

        // Store words for RSVP display (join glimpse texts for legacy mode)
        loadedWords = document.glimpses.map { it.text.split(" ") }.flatten()

        // Update HomeView
        homeScreen.setDocumentLoaded(document.displayName, document.totalWords)

        Log.i(TAG, "Document processed: ${document.displayName} (${document.totalWords} words, ${document.glimpses.size} glimpses)")
    }

    private fun clearDocument() {
        settings.clearRsvpDocument()
        loadedWords = emptyList()
        homeScreen.clearDocument()
        Log.i(TAG, "Document cleared")
    }

    private fun restoreSavedDocument() {
        val uriString = settings.rsvpDocumentUri ?: return
        val docName = settings.rsvpDocumentName ?: return
        val wordCount = settings.rsvpDocumentWordCount
        if (wordCount == 0) return

        // Try to reload the document content
        lifecycleScope.launch {
            try {
                val uri = Uri.parse(uriString)
                val docInfo = DocumentLoader.loadDocument(this@MainActivity, uri)
                if (docInfo != null) {
                    loadedWords = TextProcessor.getWords(docInfo.text)
                    homeScreen.setDocumentLoaded(docName, loadedWords.size)
                    Log.i(TAG, "Restored document: $docName (${loadedWords.size} words)")
                } else {
                    // Document no longer accessible, clear saved reference
                    clearDocument()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to restore document: ${e.message}")
                clearDocument()
            }
        }
    }

    /**
     * Load sample document from assets for testing.
     * Call from ADB: adb shell am broadcast -a com.gammasync.LOAD_SAMPLE
     */
    fun loadSampleDocument() {
        lifecycleScope.launch {
            try {
                val text = assets.open("samples/the_waste_land.txt").bufferedReader().use { it.readText() }
                val cleanedText = TextProcessor.sanitize(text)
                loadedWords = TextProcessor.getWords(cleanedText)

                settings.rsvpDocumentName = "The Waste Land"
                settings.rsvpDocumentWordCount = loadedWords.size

                homeScreen.setDocumentLoaded("The Waste Land", loadedWords.size)
                Log.i(TAG, "Sample document loaded: ${loadedWords.size} words")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load sample document", e)
            }
        }
    }

    private fun startRsvp() {
        if (loadedWords.isEmpty()) return
        if (currentProfile.mode != TherapyMode.MEMORY_WRITE) return

        rsvpOverlay.setWpm(settings.rsvpWpm)
        rsvpOverlay.setText(loadedWords.joinToString(" "))

        // Configure phase-lock mode
        if (settings.rsvpPhaseLockEnabled) {
            rsvpOverlay.enablePhaseLock { audioEngine.phase }
        } else {
            rsvpOverlay.disablePhaseLock()
        }

        rsvpOverlay.visibility = View.VISIBLE
        rsvpOverlay.start()

        // Show WPM controls on session screen
        rsvpWpmContainer.visibility = View.VISIBLE
        updateTherapyWpmDisplay()

        Log.i(TAG, "RSVP started: ${loadedWords.size} words at ${settings.rsvpWpm} WPM (phase-lock: ${settings.rsvpPhaseLockEnabled})")
    }

    private fun stopRsvp() {
        rsvpOverlay.stop()
        rsvpOverlay.visibility = View.GONE
        rsvpWpmContainer.visibility = View.GONE
    }
}
