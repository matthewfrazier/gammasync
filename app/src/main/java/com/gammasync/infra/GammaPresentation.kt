package com.cognihertz.infra

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
import com.cognihertz.R
import com.cognihertz.domain.therapy.TherapyProfile
import com.cognihertz.domain.therapy.TherapyProfiles
import com.cognihertz.ui.CircularTimerView

/**
 * Full-screen visual presentation for external displays (XREAL Air glasses).
 *
 * Renders synchronized visual stimulus on connected AR glasses
 * while the phone displays the control UI.
 *
 * Supports all therapy visual modes:
 * - SINE: Smooth warm↔cool interpolation
 * - STROBE: Sharp on/off transitions
 * - STATIC: Single color, no animation
 * - SPLIT: Left/right eye independent (3840x1080 on XREAL)
 */
class GammaPresentation(
    context: Context,
    display: Display
) : Presentation(context, display) {

    private lateinit var visualRenderer: UniversalVisualRenderer
    private lateinit var externalTimer: CircularTimerView

    private var phaseProvider: (() -> Double)? = null
    private var secondaryPhaseProvider: (() -> Double)? = null
    private var currentProfile: TherapyProfile = TherapyProfiles.NEUROSYNC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.presentation_gamma)

        // Full screen immersive mode - hide all system UI
        // Keep screen on and show over lock screen for uninterrupted therapy
        window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

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

        visualRenderer = findViewById(R.id.externalGammaRenderer)
        externalTimer = findViewById(R.id.externalTimer)

        // Configure renderer with current profile
        visualRenderer.configure(currentProfile)

        // Set phase providers
        phaseProvider?.let { visualRenderer.setPhaseProvider(it) }
        secondaryPhaseProvider?.let { visualRenderer.setSecondaryPhaseProvider(it) }

        // Size timer to 50% of display for legibility
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        val timerSize = minOf(metrics.widthPixels, metrics.heightPixels) / 2
        externalTimer.layoutParams = FrameLayout.LayoutParams(timerSize, timerSize).apply {
            gravity = android.view.Gravity.CENTER
        }
    }

    /**
     * Configure the renderer with a therapy profile.
     * Must be called before startRendering() for non-default modes.
     */
    fun configure(profile: TherapyProfile) {
        currentProfile = profile
        if (::visualRenderer.isInitialized) {
            visualRenderer.configure(profile)
        }
    }

    /**
     * Set the primary phase provider for synchronized rendering.
     * Must be called before show() or after onCreate().
     */
    fun setPhaseProvider(provider: () -> Double) {
        phaseProvider = provider
        if (::visualRenderer.isInitialized) {
            visualRenderer.setPhaseProvider(provider)
        }
    }

    /**
     * Set the secondary phase provider for split/dual modes.
     * Required for Mood Lift (split-field) mode.
     */
    fun setSecondaryPhaseProvider(provider: () -> Double) {
        secondaryPhaseProvider = provider
        if (::visualRenderer.isInitialized) {
            visualRenderer.setSecondaryPhaseProvider(provider)
        }
    }

    /**
     * Start rendering the visual stimulus.
     */
    fun startRendering() {
        if (::visualRenderer.isInitialized) {
            visualRenderer.start()
        }
    }

    /**
     * Stop rendering.
     */
    fun stopRendering() {
        if (::visualRenderer.isInitialized) {
            visualRenderer.stop()
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
