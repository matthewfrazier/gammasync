package com.gammasync

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gammasync.infra.ExternalDisplayManager
import com.gammasync.infra.GammaAudioEngine
import com.gammasync.infra.GammaPresentation
import com.gammasync.infra.GammaRenderer
import com.gammasync.infra.HapticFeedback

class MainActivity : AppCompatActivity(), ExternalDisplayManager.DisplayListener {

    companion object {
        private const val TAG = "MainActivity"
        private const val FADE_DURATION = 400L      // Smooth fade
        private const val AUTO_HIDE_DELAY = 4000L   // Hide controls after 4s
    }

    private lateinit var timerText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resetButton: Button
    private lateinit var gammaRenderer: GammaRenderer
    private lateinit var controlsOverlay: LinearLayout
    private lateinit var powerSaveOverlay: View

    private val handler = Handler(Looper.getMainLooper())
    private var elapsedSeconds = 0
    private var isRunning = false
    private var controlsVisible = true

    private val audioEngine = GammaAudioEngine()
    private lateinit var haptics: HapticFeedback

    // External display (XREAL) support
    private lateinit var externalDisplayManager: ExternalDisplayManager
    private var externalPresentation: GammaPresentation? = null
    private var hasExternalDisplay = false

    private val timerRunnable = object : Runnable {
        override fun run() {
            elapsedSeconds++
            updateTimerDisplay()
            handler.postDelayed(this, 1000)
        }
    }

    private val autoHideRunnable = Runnable {
        if (isRunning) {
            hideControls()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Keep screen on during session
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        haptics = HapticFeedback(this)

        timerText = findViewById(R.id.timerText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        resetButton = findViewById(R.id.resetButton)
        gammaRenderer = findViewById(R.id.gammaRenderer)
        controlsOverlay = findViewById(R.id.controlsOverlay)
        powerSaveOverlay = findViewById(R.id.powerSaveOverlay)

        // Connect renderer to audio engine phase
        gammaRenderer.setPhaseProvider { audioEngine.phase }

        startButton.setOnClickListener { startSession() }
        stopButton.setOnClickListener { stopSession() }
        resetButton.setOnClickListener { resetSession() }

        // Tap anywhere to toggle controls
        findViewById<View>(R.id.rootLayout).setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                toggleControls()
            }
            false
        }

        // Start listening for external displays (XREAL, etc.)
        externalDisplayManager = ExternalDisplayManager(this)
        externalDisplayManager.startListening(this)
    }

    // --- External Display Callbacks ---

    override fun onExternalDisplayConnected(display: Display) {
        Log.i(TAG, "External display connected: ${display.name} @ ${display.refreshRate}Hz")
        hasExternalDisplay = true
        haptics.heavyClick()

        // Create presentation for external display
        externalPresentation = GammaPresentation(this, display).apply {
            setPhaseProvider { audioEngine.phase }
            show()
        }

        // If session is running, start rendering on external display
        if (isRunning) {
            externalPresentation?.startRendering()
            // Stop phone screen flicker (external takes over)
            gammaRenderer.stop()
            gammaRenderer.visibility = View.GONE
        }

        updateDisplayMode()
    }

    override fun onExternalDisplayDisconnected() {
        Log.i(TAG, "External display disconnected")
        hasExternalDisplay = false
        haptics.click()

        // Dismiss and cleanup presentation
        externalPresentation?.dismiss()
        externalPresentation = null

        // If session is running, resume phone screen flicker
        if (isRunning) {
            gammaRenderer.visibility = View.VISIBLE
            gammaRenderer.start()
        }

        updateDisplayMode()
    }

    private fun updateDisplayMode() {
        val mode = if (hasExternalDisplay) "XREAL (external)" else "Phone"
        Log.i(TAG, "Display mode: $mode")

        // When XREAL connected, hide phone's flicker renderer
        if (hasExternalDisplay) {
            gammaRenderer.visibility = View.GONE
        } else if (!isRunning) {
            gammaRenderer.visibility = View.VISIBLE
        }
    }

    // --- Controls Visibility ---

    private fun toggleControls() {
        if (controlsVisible) {
            // Only hide if session is running
            if (isRunning) {
                haptics.tick()
                hideControls()
            }
        } else {
            // Strong haptic to wake up
            haptics.heavyClick()
            showControls()
        }
    }

    private fun showControls() {
        controlsVisible = true
        handler.removeCallbacks(autoHideRunnable)

        // Fade out power save overlay
        powerSaveOverlay.animate()
            .alpha(0f)
            .setDuration(FADE_DURATION)
            .withEndAction { powerSaveOverlay.visibility = View.GONE }
            .start()

        // Fade in controls
        controlsOverlay.animate()
            .alpha(1f)
            .setDuration(FADE_DURATION)
            .withStartAction { controlsOverlay.visibility = View.VISIBLE }
            .start()

        // If phone-only mode, make sure flicker is visible
        if (!hasExternalDisplay && isRunning) {
            gammaRenderer.visibility = View.VISIBLE
        }

        scheduleAutoHide()
    }

    private fun hideControls() {
        controlsVisible = false
        handler.removeCallbacks(autoHideRunnable)

        // Fade out controls
        controlsOverlay.animate()
            .alpha(0f)
            .setDuration(FADE_DURATION)
            .withEndAction { controlsOverlay.visibility = View.INVISIBLE }
            .start()

        // When XREAL connected, fade to black for power save
        if (hasExternalDisplay) {
            powerSaveOverlay.visibility = View.VISIBLE
            powerSaveOverlay.animate()
                .alpha(1f)
                .setDuration(FADE_DURATION * 2)  // Slower fade to black
                .start()
        }
    }

    private fun scheduleAutoHide() {
        handler.removeCallbacks(autoHideRunnable)
        if (isRunning) {
            handler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY)
        }
    }

    // --- Session Control ---

    private fun startSession() {
        if (!isRunning) {
            haptics.heavyClick()
            isRunning = true
            audioEngine.start(amplitude = 0.3)

            // Start rendering on appropriate display
            if (hasExternalDisplay) {
                externalPresentation?.startRendering()
                gammaRenderer.visibility = View.GONE
            } else {
                gammaRenderer.visibility = View.VISIBLE
                gammaRenderer.start()
            }

            handler.post(timerRunnable)
            startButton.isEnabled = false
            stopButton.isEnabled = true
            scheduleAutoHide()
        }
    }

    private fun stopSession() {
        if (isRunning) {
            haptics.heavyClick()
            isRunning = false

            // Stop rendering on all displays
            gammaRenderer.stop()
            externalPresentation?.stopRendering()

            audioEngine.stop()
            handler.removeCallbacks(timerRunnable)
            handler.removeCallbacks(autoHideRunnable)
            startButton.isEnabled = true
            stopButton.isEnabled = false

            // Make sure UI is visible and power save is off
            gammaRenderer.visibility = View.VISIBLE
            powerSaveOverlay.visibility = View.GONE
            powerSaveOverlay.alpha = 0f
            showControls()
        }
    }

    private fun resetSession() {
        haptics.click()
        stopSession()
        elapsedSeconds = 0
        updateTimerDisplay()
    }

    private fun updateTimerDisplay() {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        timerText.text = String.format("%02d:%02d", minutes, seconds)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.i(TAG, "Configuration changed: ${newConfig.orientation}")
        // SurfaceView handles resize via surfaceChanged callback
    }

    override fun onDestroy() {
        super.onDestroy()
        externalDisplayManager.stopListening()
        externalPresentation?.dismiss()
        gammaRenderer.stop()
        audioEngine.release()
        handler.removeCallbacks(timerRunnable)
        handler.removeCallbacks(autoHideRunnable)
    }
}
