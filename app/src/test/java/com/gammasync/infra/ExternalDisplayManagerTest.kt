package com.gammasync.infra

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class ExternalDisplayManagerTest {

    private lateinit var context: Context
    private lateinit var displayManager: DisplayManager
    private lateinit var externalDisplayManager: ExternalDisplayManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        displayManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.DISPLAY_SERVICE) } returns displayManager
        
        externalDisplayManager = ExternalDisplayManager(context)
    }

    @Test
    fun getExternalDisplay_returnsNullWhenNoDisplaysAvailable() {
        every { displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) } returns emptyArray()
        
        val result = externalDisplayManager.getExternalDisplay()
        
        assertNull(result)
    }

    @Test
    fun getExternalDisplay_returnsFirstDisplayWhenAvailable() {
        val mockDisplay = mockk<Display>(relaxed = true)
        val mockMode = mockk<Display.Mode>(relaxed = true)
        
        every { mockDisplay.name } returns "XREAL Air"
        every { mockDisplay.mode } returns mockMode
        every { mockMode.physicalWidth } returns 3840
        every { mockMode.physicalHeight } returns 1080
        every { mockDisplay.refreshRate } returns 120.0f
        
        every { displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) } returns arrayOf(mockDisplay)
        
        val result = externalDisplayManager.getExternalDisplay()
        
        assertNotNull(result)
        assertEquals("XREAL Air", result?.name)
    }

    @Test
    fun isExternalDisplayConnected_returnsFalseWhenNoDisplay() {
        every { displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) } returns emptyArray()
        
        val result = externalDisplayManager.isExternalDisplayConnected()
        
        assertFalse(result)
    }

    @Test
    fun isExternalDisplayConnected_returnsTrueWhenDisplayPresent() {
        val mockDisplay = mockk<Display>(relaxed = true)
        every { displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) } returns arrayOf(mockDisplay)
        
        val result = externalDisplayManager.isExternalDisplayConnected()
        
        assertTrue(result)
    }

    @Test
    fun getExternalDisplayRefreshRate_returns0WhenNoDisplay() {
        every { displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) } returns emptyArray()
        
        val result = externalDisplayManager.getExternalDisplayRefreshRate()
        
        assertEquals(0f, result, 0.01f)
    }

    @Test
    fun getExternalDisplayRefreshRate_returnsCorrectRateWhenDisplayPresent() {
        val mockDisplay = mockk<Display>(relaxed = true)
        every { mockDisplay.refreshRate } returns 120.0f
        every { displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) } returns arrayOf(mockDisplay)
        
        val result = externalDisplayManager.getExternalDisplayRefreshRate()
        
        assertEquals(120.0f, result, 0.01f)
    }

    @Test
    fun getPreferredDisplayMode_returnsNullWhenNoDisplay() {
        every { displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) } returns emptyArray()
        
        val result = externalDisplayManager.getPreferredDisplayMode()
        
        assertNull(result)
    }

    @Test
    fun getPreferredDisplayMode_prefers120HzOverLowerRates() {
        val mockDisplay = mockk<Display>(relaxed = true)
        
        val mode60Hz = mockk<Display.Mode>(relaxed = true)
        every { mode60Hz.refreshRate } returns 60.0f
        every { mode60Hz.physicalWidth } returns 1920
        every { mode60Hz.physicalHeight } returns 1080
        
        val mode120Hz = mockk<Display.Mode>(relaxed = true)
        every { mode120Hz.refreshRate } returns 120.0f
        every { mode120Hz.physicalWidth } returns 1920
        every { mode120Hz.physicalHeight } returns 1080
        
        val mode144Hz = mockk<Display.Mode>(relaxed = true)
        every { mode144Hz.refreshRate } returns 144.0f
        every { mode144Hz.physicalWidth } returns 1920
        every { mode144Hz.physicalHeight } returns 1080
        
        every { mockDisplay.supportedModes } returns arrayOf(mode60Hz, mode120Hz, mode144Hz)
        every { displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) } returns arrayOf(mockDisplay)
        
        val result = externalDisplayManager.getPreferredDisplayMode()
        
        assertNotNull(result)
        assertEquals(144.0f, result?.refreshRate, 0.01f) // Should pick highest rate
    }

    @Test
    fun getPreferredDisplayMode_fallsBackToHighestWhenNo120Hz() {
        val mockDisplay = mockk<Display>(relaxed = true)
        
        val mode30Hz = mockk<Display.Mode>(relaxed = true)
        every { mode30Hz.refreshRate } returns 30.0f
        
        val mode60Hz = mockk<Display.Mode>(relaxed = true)
        every { mode60Hz.refreshRate } returns 60.0f
        every { mode60Hz.physicalWidth } returns 1920
        every { mode60Hz.physicalHeight } returns 1080
        
        every { mockDisplay.supportedModes } returns arrayOf(mode30Hz, mode60Hz)
        every { displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) } returns arrayOf(mockDisplay)
        
        val result = externalDisplayManager.getPreferredDisplayMode()
        
        assertNotNull(result)
        assertEquals(60.0f, result?.refreshRate, 0.01f) // Should pick highest available
    }

    @Test
    fun configureOptimalRefreshRate_returnsFalseWhenNoDisplay() {
        every { displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) } returns emptyArray()
        val mockSurface = mockk<Surface>(relaxed = true)
        
        val result = externalDisplayManager.configureOptimalRefreshRate(mockSurface)
        
        assertFalse(result)
    }

    @Test
    fun configureOptimalRefreshRate_configuresSurfaceAnd returnsTrue For120Hz() {
        val mockDisplay = mockk<Display>(relaxed = true)
        val mode120Hz = mockk<Display.Mode>(relaxed = true)
        every { mode120Hz.refreshRate } returns 120.0f
        every { mode120Hz.physicalWidth } returns 1920
        every { mode120Hz.physicalHeight } returns 1080
        
        every { mockDisplay.supportedModes } returns arrayOf(mode120Hz)
        every { displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) } returns arrayOf(mockDisplay)
        
        val mockSurface = mockk<Surface>(relaxed = true)
        every { mockSurface.setFrameRate(any(), any()) } returns Unit
        
        val result = externalDisplayManager.configureOptimalRefreshRate(mockSurface)
        
        assertTrue(result)
        verify { mockSurface.setFrameRate(120.0f, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT) }
    }

    @Test
    fun configureOptimalRefreshRate_returnsFalseFor60Hz() {
        val mockDisplay = mockk<Display>(relaxed = true)
        val mode60Hz = mockk<Display.Mode>(relaxed = true)
        every { mode60Hz.refreshRate } returns 60.0f
        every { mode60Hz.physicalWidth } returns 1920
        every { mode60Hz.physicalHeight } returns 1080
        
        every { mockDisplay.supportedModes } returns arrayOf(mode60Hz)
        every { displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) } returns arrayOf(mockDisplay)
        
        val mockSurface = mockk<Surface>(relaxed = true)
        every { mockSurface.setFrameRate(any(), any()) } returns Unit
        
        val result = externalDisplayManager.configureOptimalRefreshRate(mockSurface)
        
        assertFalse(result) // Returns false because 60Hz < 120Hz requirement
        verify { mockSurface.setFrameRate(60.0f, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT) }
    }
}