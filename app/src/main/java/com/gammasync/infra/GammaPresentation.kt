package com.gammasync.infra

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import com.gammasync.R

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
    private var phaseProvider: (() -> Double)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full screen, keep screen on
        window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContentView(R.layout.presentation_gamma)

        gammaRenderer = findViewById(R.id.externalGammaRenderer)
        phaseProvider?.let { gammaRenderer.setPhaseProvider(it) }
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

    override fun onStop() {
        super.onStop()
        stopRendering()
    }
}
