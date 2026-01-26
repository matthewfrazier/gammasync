/*
 * MIT License
 * Copyright (c) 2026 matthewfrazier
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gammasync.infra

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display

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
