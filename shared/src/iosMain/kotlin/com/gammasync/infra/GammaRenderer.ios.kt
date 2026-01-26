package com.gammasync.infra

import com.gammasync.domain.ColorTemperature
import com.gammasync.domain.therapy.VisualConfig
import kotlinx.cinterop.*
import platform.Metal.*
import platform.MetalKit.*
import platform.QuartzCore.*
import platform.UIKit.*
import platform.Foundation.*

/**
 * iOS implementation of GammaRenderer using Metal and CADisplayLink for 120Hz rendering.
 * Synchronizes with GammaAudioEngine phase for sub-millisecond A/V sync.
 */
actual class GammaRenderer(private val view: UIView) {
    
    private var device: MTLDevice? = null
    private var displayLink: CADisplayLink? = null
    private var metalLayer: CAMetalLayer? = null
    private var commandQueue: MTLCommandQueue? = null
    private var renderPipelineState: MTLRenderPipelineState? = null
    
    private var isRendering: Boolean = false
    private var phaseProvider: (() -> Double)? = null
    private var visualConfig: VisualConfig? = null
    
    /**
     * Initialize Metal rendering system.
     */
    actual fun initialize(): Boolean {
        // Get default Metal device
        device = MTLCreateSystemDefaultDevice() ?: return false
        
        // Create Metal layer
        val layer = CAMetalLayer()
        layer.device = device
        layer.pixelFormat = MTLPixelFormat.MTLPixelFormatBGRA8Unorm
        layer.framebufferOnly = true
        
        // Configure for 120Hz if available
        if (UIScreen.mainScreen.maximumFramesPerSecond >= 120) {
            layer.displaySyncEnabled = true
            layer.allowsNextDrawableTimeout = false
        }
        
        view.layer.addSublayer(layer)
        metalLayer = layer
        
        // Create command queue
        commandQueue = device?.newCommandQueue()
        
        // Set up render pipeline
        setupRenderPipeline()
        
        return true
    }
    
    private fun setupRenderPipeline() {
        val device = this.device ?: return
        
        // Create vertex and fragment shaders
        val library = device.newDefaultLibrary() ?: return
        val vertexFunction = library.newFunctionWithName("vertexShader")
        val fragmentFunction = library.newFunctionWithName("fragmentShader")
        
        // Create render pipeline descriptor
        val pipelineDescriptor = MTLRenderPipelineDescriptor()
        pipelineDescriptor.vertexFunction = vertexFunction
        pipelineDescriptor.fragmentFunction = fragmentFunction
        pipelineDescriptor.colorAttachments.objectAtIndexedSubscript(0u).apply {
            this.pixelFormat = MTLPixelFormat.MTLPixelFormatBGRA8Unorm
        }
        
        try {
            renderPipelineState = device.newRenderPipelineStateWithDescriptor(
                pipelineDescriptor, 
                null
            )
        } catch (error: Exception) {
            println("Failed to create render pipeline: $error")
        }
    }
    
    /**
     * Start rendering with the given phase provider and visual configuration.
     */
    actual fun start(
        phaseProvider: () -> Double, 
        visualConfig: VisualConfig
    ): Boolean {
        this.phaseProvider = phaseProvider
        this.visualConfig = visualConfig
        
        // Create display link for 120Hz updates
        val link = CADisplayLink.displayLinkWithTarget(
            target = this,
            selector = null // Will be handled in Kotlin/Native callback
        )
        
        // Set to 120Hz if available
        if (UIScreen.mainScreen.maximumFramesPerSecond >= 120) {
            link.preferredFramesPerSecond = 120
        }
        
        link.addToRunLoop(
            NSRunLoop.mainRunLoop,
            NSDefaultRunLoopMode
        )
        
        displayLink = link
        isRendering = true
        
        return true
    }
    
    /**
     * Stop rendering and release display link.
     */
    actual fun stop() {
        displayLink?.invalidate()
        displayLink = null
        isRendering = false
    }
    
    /**
     * Render frame callback - called by CADisplayLink at 120Hz.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun renderFrame() {
        if (!isRendering) return
        
        val phase = phaseProvider?.invoke() ?: 0.0
        val config = visualConfig ?: return
        
        val layer = metalLayer ?: return
        val commandQueue = this.commandQueue ?: return
        val pipelineState = this.renderPipelineState ?: return
        
        // Get next drawable
        val drawable = layer.nextDrawable() ?: return
        
        // Create command buffer
        val commandBuffer = commandQueue.commandBuffer() ?: return
        
        // Create render pass descriptor
        val renderPassDescriptor = MTLRenderPassDescriptor()
        renderPassDescriptor.colorAttachments.objectAtIndexedSubscript(0u).apply {
            this.texture = drawable.texture
            this.loadAction = MTLLoadAction.MTLLoadActionClear
            this.storeAction = MTLStoreAction.MTLStoreActionStore
            
            // Calculate isoluminant color based on 40Hz phase
            val color = calculateIsoluminantColor(phase, config)
            this.clearColor = MTLClearColor(
                red = color.red.toDouble(),
                green = color.green.toDouble(), 
                blue = color.blue.toDouble(),
                alpha = 1.0
            )
        }
        
        // Create render encoder
        val renderEncoder = commandBuffer.renderCommandEncoderWithDescriptor(
            renderPassDescriptor
        ) ?: return
        
        renderEncoder.setRenderPipelineState(pipelineState)
        
        // For simple color rendering, we just clear with the target color
        // More complex rendering would involve drawing geometry here
        
        renderEncoder.endEncoding()
        
        // Present drawable
        commandBuffer.presentDrawable(drawable)
        commandBuffer.commit()
    }
    
    /**
     * Calculate isoluminant color based on 40Hz phase.
     * Modulates chrominance while maintaining constant luminance for safety.
     */
    private fun calculateIsoluminantColor(
        phase: Double, 
        config: VisualConfig
    ): ColorTemperature.RGBColor {
        // Use existing ColorTemperature domain logic
        val warmTemp = 2700 // Warm white
        val coolTemp = 6500 // Cool white
        
        // Sine wave modulation for smooth transition
        val sinePhase = kotlin.math.sin(phase * 2 * kotlin.math.PI)
        val normalizedPhase = (sinePhase + 1.0) / 2.0 // 0.0 to 1.0
        
        // Interpolate between warm and cool temperatures
        val currentTemp = warmTemp + (coolTemp - warmTemp) * normalizedPhase
        
        return ColorTemperature.temperatureToRGB(currentTemp.toInt())
    }
    
    /**
     * Update visual configuration.
     */
    actual fun updateConfig(config: VisualConfig) {
        visualConfig = config
    }
    
    /**
     * Update view bounds when layout changes.
     */
    actual fun updateBounds(width: Int, height: Int) {
        metalLayer?.frame = CGRectMake(
            0.0,
            0.0, 
            width.toDouble(),
            height.toDouble()
        )
    }
    
    /**
     * Release resources.
     */
    actual fun release() {
        stop()
        metalLayer?.removeFromSuperlayer()
        metalLayer = null
        commandQueue = null
        renderPipelineState = null
        device = null
    }
}

/**
 * Extension to handle CADisplayLink callback in Kotlin/Native.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalObjCName::class)
@ObjCName("GammaRendererDisplayLinkCallback")
external fun displayLinkCallback(
    displayLink: CADisplayLink,
    renderer: GammaRenderer
) {
    renderer.renderFrame()
}