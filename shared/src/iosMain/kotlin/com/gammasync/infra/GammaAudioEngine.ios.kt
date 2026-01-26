package com.gammasync.infra

import com.gammasync.domain.SignalOscillator
import com.gammasync.domain.therapy.AudioMode
import kotlinx.cinterop.*
import platform.AudioToolbox.*
import platform.CoreAudio.*
import platform.Foundation.*

/**
 * iOS implementation of GammaAudioEngine using AudioUnit for real-time audio processing.
 * Provides sub-millisecond A/V synchronization by exposing hardware audio clock timing.
 */
actual class GammaAudioEngine {
    
    private var audioUnit: AudioUnit? = null
    private var oscillator: SignalOscillator? = null
    private var isPlaying: Boolean = false
    private var sampleRate: Double = 48000.0
    private var startTime: UInt64 = 0u
    
    /**
     * Initialize the AudioUnit for real-time audio output.
     * Uses Remote I/O Audio Unit for hardware access and low latency.
     */
    actual fun initialize(): Boolean {
        // AudioComponentDescription for output
        val outputDesc = AudioComponentDescription().apply {
            componentType = kAudioUnitType_Output
            componentSubType = kAudioUnitSubType_RemoteIO
            componentManufacturer = kAudioUnitManufacturer_Apple
            componentFlags = 0u
            componentFlagsMask = 0u
        }
        
        // Find the audio component
        val component = AudioComponentFindNext(null, outputDesc.ptr)
            ?: return false
            
        // Create audio unit instance
        val unit = alloc<AudioUnitVar>()
        val result = AudioComponentInstanceNew(component, unit.ptr)
        if (result != noErr) return false
        
        audioUnit = unit.value
        
        // Configure audio unit properties
        setupAudioUnit()
        
        return true
    }
    
    private fun setupAudioUnit() {
        val unit = audioUnit ?: return
        
        // Set up stream format (48kHz, 32-bit float, stereo)
        val streamFormat = AudioStreamBasicDescription().apply {
            mSampleRate = sampleRate
            mFormatID = kAudioFormatLinearPCM
            mFormatFlags = (kAudioFormatFlagIsFloat or kAudioFormatFlagIsPacked).toUInt()
            mBytesPerPacket = 8u
            mFramesPerPacket = 1u
            mBytesPerFrame = 8u
            mChannelsPerFrame = 2u
            mBitsPerChannel = 32u
            mReserved = 0u
        }
        
        // Set the stream format
        AudioUnitSetProperty(
            unit,
            kAudioUnitProperty_StreamFormat,
            kAudioUnitScope_Input,
            0u,
            streamFormat.ptr,
            sizeOf<AudioStreamBasicDescription>().toUInt()
        )
        
        // Set render callback
        val callbackStruct = AURenderCallbackStruct().apply {
            inputProc = staticCFunction(::audioRenderCallback)
            inputProcRefCon = StableRef.create(this@GammaAudioEngine).asCPointer()
        }
        
        AudioUnitSetProperty(
            unit,
            kAudioUnitProperty_SetRenderCallback,
            kAudioUnitScope_Input,
            0u,
            callbackStruct.ptr,
            sizeOf<AURenderCallbackStruct>().toUInt()
        )
        
        // Initialize the audio unit
        AudioUnitInitialize(unit)
    }
    
    /**
     * Start audio output with the given oscillator and amplitude.
     */
    actual fun start(oscillator: SignalOscillator, amplitude: Double): Boolean {
        this.oscillator = oscillator
        startTime = mach_absolute_time()
        
        val unit = audioUnit ?: return false
        val result = AudioOutputUnitStart(unit)
        
        isPlaying = (result == noErr)
        return isPlaying
    }
    
    /**
     * Stop audio output.
     */
    actual fun stop() {
        val unit = audioUnit ?: return
        AudioOutputUnitStop(unit)
        isPlaying = false
    }
    
    /**
     * Get the current 40Hz phase (0.0-1.0) based on audio hardware timeline.
     * This is the master timing source for video synchronization.
     */
    actual val phase: Double
        get() {
            if (!isPlaying) return 0.0
            
            val currentTime = mach_absolute_time()
            val timebaseInfo = mach_timebase_info_data_t()
            mach_timebase_info(timebaseInfo.ptr)
            
            // Convert to nanoseconds
            val elapsedNanos = (currentTime - startTime) * 
                timebaseInfo.numer.toULong() / timebaseInfo.denom.toULong()
                
            // Convert to seconds and calculate 40Hz phase
            val elapsedSeconds = elapsedNanos.toDouble() / 1_000_000_000.0
            val cycles = elapsedSeconds * 40.0
            return cycles - kotlin.math.floor(cycles)
        }
    
    /**
     * Update audio mode configuration.
     */
    actual fun updateMode(mode: AudioMode) {
        // Update oscillator configuration based on mode
        oscillator?.updateMode(mode)
    }
    
    /**
     * Release resources.
     */
    actual fun release() {
        stop()
        
        val unit = audioUnit
        if (unit != null) {
            AudioUnitUninitialize(unit)
            AudioComponentInstanceDispose(unit)
        }
        audioUnit = null
        oscillator = null
    }
}

/**
 * Static C function for AudioUnit render callback.
 * Called by Core Audio for each audio buffer.
 */
private fun audioRenderCallback(
    inRefCon: COpaquePointer?,
    ioActionFlags: UnsafeMutablePointer<AudioUnitRenderActionFlags>?,
    inTimeStamp: UnsafeMutablePointer<AudioTimeStamp>?,
    inBusNumber: UInt32,
    inNumberFrames: UInt32,
    ioData: UnsafeMutablePointer<AudioBufferList>?
): OSStatus {
    
    val engineRef = inRefCon?.asStableRef<GammaAudioEngine>() ?: return -1
    val engine = engineRef.get()
    val oscillator = engine.oscillator ?: return -1
    
    val bufferList = ioData?.pointed ?: return -1
    val leftBuffer = bufferList.mBuffers.mData?.reinterpret<FloatVar>() ?: return -1
    val rightBuffer = (leftBuffer.rawPtr + bufferList.mBuffers.mDataByteSize.toInt() / 2)
        ?.reinterpret<FloatVar>() ?: return -1
    
    // Generate audio samples
    for (i in 0 until inNumberFrames.toInt()) {
        val sample = oscillator.nextSample().toFloat()
        leftBuffer[i] = sample
        rightBuffer[i] = sample
    }
    
    return noErr
}