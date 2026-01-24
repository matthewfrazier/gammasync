package com.gammasync

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gammasync.infra.GammaAudioEngine
import com.gammasync.infra.GammaRenderer
import com.gammasync.infra.HapticFeedback

class MainActivity : AppCompatActivity() {

    companion object {
        private const val FADE_DURATION = 200L
        private const val AUTO_HIDE_DELAY = 5000L
    }

    private lateinit var timerText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resetButton: Button
    private lateinit var gammaRenderer: GammaRenderer
    private lateinit var controlsOverlay: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private var elapsedSeconds = 0
    private var isRunning = false
    private var controlsVisible = true

    private val audioEngine = GammaAudioEngine()
    private lateinit var haptics: HapticFeedback

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
    }

    private fun toggleControls() {
        haptics.tick()
        if (controlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun showControls() {
        controlsVisible = true
        controlsOverlay.animate()
            .alpha(1f)
            .setDuration(FADE_DURATION)
            .withStartAction { controlsOverlay.visibility = View.VISIBLE }
            .start()
        scheduleAutoHide()
    }

    private fun hideControls() {
        controlsVisible = false
        handler.removeCallbacks(autoHideRunnable)
        controlsOverlay.animate()
            .alpha(0f)
            .setDuration(FADE_DURATION)
            .withEndAction { controlsOverlay.visibility = View.INVISIBLE }
            .start()
    }

    private fun scheduleAutoHide() {
        handler.removeCallbacks(autoHideRunnable)
        if (isRunning) {
            handler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY)
        }
    }

    private fun startSession() {
        if (!isRunning) {
            haptics.heavyClick()
            isRunning = true
            audioEngine.start(amplitude = 0.3)
            gammaRenderer.start()
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
            gammaRenderer.stop()
            audioEngine.stop()
            handler.removeCallbacks(timerRunnable)
            handler.removeCallbacks(autoHideRunnable)
            startButton.isEnabled = true
            stopButton.isEnabled = false
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
        Log.i("MainActivity", "Configuration changed: ${newConfig.orientation}")
        // SurfaceView handles resize via surfaceChanged callback
    }

    override fun onDestroy() {
        super.onDestroy()
        gammaRenderer.stop()
        audioEngine.release()
        handler.removeCallbacks(timerRunnable)
        handler.removeCallbacks(autoHideRunnable)
    }
}
