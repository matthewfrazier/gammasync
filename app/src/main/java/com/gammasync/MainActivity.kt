package com.gammasync

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gammasync.infra.GammaAudioEngine
import com.gammasync.infra.GammaRenderer
import com.gammasync.infra.HapticFeedback

class MainActivity : AppCompatActivity() {

    private lateinit var timerText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resetButton: Button
    private lateinit var gammaRenderer: GammaRenderer

    private val handler = Handler(Looper.getMainLooper())
    private var elapsedSeconds = 0
    private var isRunning = false

    private val audioEngine = GammaAudioEngine()
    private lateinit var haptics: HapticFeedback

    private val timerRunnable = object : Runnable {
        override fun run() {
            elapsedSeconds++
            updateTimerDisplay()
            handler.postDelayed(this, 1000)
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

        // Connect renderer to audio engine phase
        gammaRenderer.setPhaseProvider { audioEngine.phase }

        startButton.setOnClickListener { startSession() }
        stopButton.setOnClickListener { stopSession() }
        resetButton.setOnClickListener { resetSession() }
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
        }
    }

    private fun stopSession() {
        if (isRunning) {
            haptics.heavyClick()
            isRunning = false
            gammaRenderer.stop()
            audioEngine.stop()
            handler.removeCallbacks(timerRunnable)
            startButton.isEnabled = true
            stopButton.isEnabled = false
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
    }
}
