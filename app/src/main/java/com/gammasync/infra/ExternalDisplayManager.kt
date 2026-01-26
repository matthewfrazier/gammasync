package com.gammasync.infra

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import android.view.Surface

/**
 * Detects and manages external displays (XREAL Air glasses, etc.)
 *
 * XREAL Air connects via USB-C DisplayPort Alt Mode and appears
 * as a standard external display to Android.
 */
class ExternalDisplayManager(context: Context) {

    companion object {
        private const val TAG = "ExternalDisplayManager"
    }

    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private var listener: DisplayListener? = null
    private var displayCallback: DisplayManager.DisplayListener? = null

    interface DisplayListener {
        fun onExternalDisplayConnected(display: Display)
        fun onExternalDisplayDisconnected()
    }

    /**
     * Get the currently connected external display, if any.
     */
    fun getExternalDisplay(): Display? {
        val displays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        return displays.firstOrNull()?.also {
            Log.i(TAG, "Found external display: ${it.name} (${it.mode.physicalWidth}x${it.mode.physicalHeight} @ ${it.refreshRate}Hz)")
        }
    }

    /**
     * Check if an external display is connected.
     */
    fun isExternalDisplayConnected(): Boolean {
        return getExternalDisplay() != null
    }

    /**
     * Get the refresh rate of the external display.
     */
    fun getExternalDisplayRefreshRate(): Float {
        return getExternalDisplay()?.refreshRate ?: 0f
    }

    /**
     * Get the highest available refresh rate mode for the external display.
     * Prefers 120Hz or higher for gamma entrainment therapy requirements.
     */
    fun getPreferredDisplayMode(): Display.Mode? {
        val display = getExternalDisplay() ?: return null
        val supportedModes = display.supportedModes
        
        Log.i(TAG, "Available display modes:")
        supportedModes.forEach { mode ->
            Log.i(TAG, "  ${mode.physicalWidth}x${mode.physicalHeight} @ ${mode.refreshRate}Hz")
        }
        
        // Find highest refresh rate mode that meets therapy requirements
        val preferredMode = supportedModes
            .filter { it.refreshRate >= 120f } // Prefer 120Hz+ for optimal therapy
            .maxByOrNull { it.refreshRate }
            ?: supportedModes.maxByOrNull { it.refreshRate } // Fallback to highest available
            
        preferredMode?.let {
            Log.i(TAG, "Selected preferred mode: ${it.physicalWidth}x${it.physicalHeight} @ ${it.refreshRate}Hz")
        }
        
        return preferredMode
    }

    /**
     * Configure a surface to use the preferred display mode for optimal refresh rate.
     * Returns true if a high refresh rate mode (120Hz+) was configured.
     */
    fun configureOptimalRefreshRate(surface: Surface): Boolean {
        val preferredMode = getPreferredDisplayMode() ?: return false
        
        try {
            surface.setFrameRate(preferredMode.refreshRate, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
            Log.i(TAG, "Configured surface for ${preferredMode.refreshRate}Hz refresh rate")
            return preferredMode.refreshRate >= 120f
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure surface frame rate: ${e.message}")
            return false
        }
    }

    /**
     * Start listening for display connection changes.
     */
    fun startListening(listener: DisplayListener) {
        this.listener = listener

        displayCallback = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                val display = displayManager.getDisplay(displayId)
                if (display != null && display.displayId != Display.DEFAULT_DISPLAY) {
                    Log.i(TAG, "External display connected: ${display.name}")
                    listener.onExternalDisplayConnected(display)
                }
            }

            override fun onDisplayRemoved(displayId: Int) {
                if (displayId != Display.DEFAULT_DISPLAY) {
                    Log.i(TAG, "External display disconnected")
                    listener.onExternalDisplayDisconnected()
                }
            }

            override fun onDisplayChanged(displayId: Int) {
                // Display properties changed (resolution, refresh rate, etc.)
                val display = displayManager.getDisplay(displayId)
                if (display != null && displayId != Display.DEFAULT_DISPLAY) {
                    Log.i(TAG, "External display changed: ${display.name} @ ${display.refreshRate}Hz")
                }
            }
        }

        displayManager.registerDisplayListener(displayCallback, null)

        // Check for already-connected display
        getExternalDisplay()?.let { display ->
            listener.onExternalDisplayConnected(display)
        }
    }

    /**
     * Stop listening for display changes.
     */
    fun stopListening() {
        displayCallback?.let { displayManager.unregisterDisplayListener(it) }
        displayCallback = null
        listener = null
    }
}
