package com.gammasync.infra

import android.app.Presentation
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Display
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import com.gammasync.R
import com.gammasync.ui.CircularTimerView

/**
 * Full-screen gamma flicker presentation for external displays (XREAL Air glasses).
 *
 * Renders synchronized 40Hz visual stimulus on connected AR glasses
 * while the phone displays the control UI.
 */
class GammaPresentation(
    context: Context,
    display: Display
) : Presentation(context, display) {

    private lateinit var gammaRenderer: GammaRenderer
    private lateinit var externalTimer: CircularTimerView
    private var phaseProvider: (() -> Double)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.presentation_gamma)

        // Full screen immersive mode - hide all system UI
        window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(false)
                insetsController?.apply {
                    hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
            }
        }

        gammaRenderer = findViewById(R.id.externalGammaRenderer)
        externalTimer = findViewById(R.id.externalTimer)
        phaseProvider?.let { gammaRenderer.setPhaseProvider(it) }

        // Size timer to 50% of display for legibility
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        val timerSize = minOf(metrics.widthPixels, metrics.heightPixels) / 2
        externalTimer.layoutParams = FrameLayout.LayoutParams(timerSize, timerSize).apply {
            gravity = android.view.Gravity.CENTER
        }
    }

    /**
     * Set the phase provider for synchronized rendering.
     * Must be called before show() or after onCreate().
     */
    fun setPhaseProvider(provider: () -> Double) {
        phaseProvider = provider
        if (::gammaRenderer.isInitialized) {
            gammaRenderer.setPhaseProvider(provider)
        }
    }

    /**
     * Start rendering the gamma flicker.
     */
    fun startRendering() {
        if (::gammaRenderer.isInitialized) {
            gammaRenderer.start()
        }
    }

    /**
     * Stop rendering.
     */
    fun stopRendering() {
        if (::gammaRenderer.isInitialized) {
            gammaRenderer.stop()
        }
    }

    /**
     * Show the timer overlay on the external display.
     */
    fun showTimer() {
        if (::externalTimer.isInitialized) {
            externalTimer.visibility = View.VISIBLE
        }
    }

    /**
     * Hide the timer overlay on the external display.
     */
    fun hideTimer() {
        if (::externalTimer.isInitialized) {
            externalTimer.visibility = View.GONE
        }
    }

    /**
     * Set the total session duration for the timer.
     */
    fun setTotalDuration(totalSeconds: Int) {
        if (::externalTimer.isInitialized) {
            externalTimer.setTotalDuration(totalSeconds)
        }
    }

    /**
     * Update the remaining time on the timer.
     */
    fun setRemainingTime(remainingSeconds: Int) {
        if (::externalTimer.isInitialized) {
            externalTimer.setRemainingTime(remainingSeconds)
        }
    }

    override fun onStop() {
        super.onStop()
        stopRendering()
    }
}
