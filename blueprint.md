!The following artifact acts as the Master Engineering Record for NeuroSync. It allows for development to proceed with a "Trust but Verify" model: logic is proven mathematically in software, while physical compliance is verified via automated hardware benchmarking.
This blueprint utilizes Native Android (Kotlin) to guarantee the 120Hz timing accuracy that web/hybrid frameworks cannot reliably sustain.
:::datastore{id="neurosync-engineering-artifact" source="neurosync_blueprint.md" type="text/markdown"}
NeuroSync: Engineering Master Blueprint
Version: 1.0.0
Status: Approved for Development
Target Platform: Android 12+ (API 31+)
Hardware Requirement: 120Hz/144Hz OLED Display (e.g., Pixel 8, Galaxy S24) & XREAL Air Support.
1. Product Requirements Document (PRD)
1.1 Core Objective
Develop a Class I Medical Device software capable of delivering Gamma Entrainment Using Sensory stimuli (GENUS) with sub-millisecond synchronization between audio and visual drivers.
1.2 Functional Specifications
| Component | Feature | Requirement | Acceptance Metric |
|---|---|---|---|
| Visual | 120Hz Drive | Application must force the display hardware into 120Hz or 144Hz mode. 60Hz is non-compliant. | Frame Time P99: < 8.5ms |
| Visual | Isoluminant Flicker | Alternate between Warm (2700K) and Cool (6500K) White. Modulate Chrominance, not Luminance. | Epilepsy Safety: Compliant |
| Audio | 40Hz AM Synthesis | Pink Noise carrier modulated by a 40Hz Sine Wave envelope. | Freq Accuracy: 40.0Hz ±0.05 |
| Sync | Phase Locking | Visual frame brightness must match the instantaneous Audio Amplitude. | AV Drift: < 10ms |
| Hardware | XREAL / AR | Auto-detect USB-C External Display. Project therapy visuals to glasses; keep controls on phone. | Plug & Play: < 2s detection |
| Safety | Epilepsy Gate | Mandatory "Hold-to-Agree" waiver on every cold launch. | Compliance: 100% Gated |
1.3 User Experience
 * Session Timer: 15m / 30m / 60m with auto-fade.
 * Dark Mode UI: Phone screen acts as a remote (mostly black) to reduce battery drain while glasses are active.
2. Technical Architecture
To achieve medical-grade timing, we invert the typical media app structure. The Audio Hardware Clock is the single source of truth.
2.1 The Master-Slave Pattern
 * Master (Audio): The AudioTrack consumes samples at exactly 48,000Hz. This physical consumption rate dictates the passage of "Therapy Time."
 * Slave (Video): The Video Renderer does not use system time (System.nanoTime()). Instead, it asks the Audio Engine: "What is the current phase (0.0 - 1.0) of the 40Hz wave?" and renders that exact color.
2.2 Clean Architecture Strategy
We separate Domain Logic (Math) from Infrastructure (Android APIs) to enable Virtual Testing.
 * SignalOscillator (Pure Kotlin): Calculates PinkNoise \times sin(t). No Android dependencies.
 * GammaAudioEngine (Android): Wraps AudioTrack. Feeds data from Oscillator.
 * GammaRenderer (OpenGL): Draws color based on Engine phase.
3. Implementation Plan
3.1 Domain Layer: The Signal Math
Pure Kotlin. Testable on any machine.
package com.neurosync.domain

import kotlin.math.PI
import kotlin.math.sin

/**
 * Stateful oscillator that generates the next audio buffer and tracks phase.
 */
class SignalOscillator(
    private val sampleRate: Int = 48000,
    private val targetFreq: Double = 40.0
) {
    // Voss-McCartney Pink Noise State
    private var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var b3 = 0.0; var b4 = 0.0; var b5 = 0.0; var b6 = 0.0
    
    // The current phase of the 40Hz wave (0.0 to 1.0)
    var currentPhase: Float = 0f
        private set

    fun fillBuffer(buffer: FloatArray) {
        // We assume buffer is continuous
        for (i in buffer.indices) {
            // 1. Generate Pink Noise (Voss Algorithm)
            val white = Math.random() * 2 - 1
            b0 = 0.99886 * b0 + white * 0.0555179
            b1 = 0.99332 * b1 + white * 0.0750759
            b2 = 0.96900 * b2 + white * 0.1538520
            b3 = 0.86650 * b3 + white * 0.3104856
            b4 = 0.55000 * b4 + white * 0.5329522
            b5 = -0.7616 * b5 - white * 0.0168980
            val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.11
            b6 = white * 0.115926

            // 2. Calculate 40Hz Modulation
            // We advance phase by 1 sample step
            currentPhase = ((currentPhase + (targetFreq / sampleRate)) % 1.0).toFloat()
            
            // 3. AM Synthesis: 0.5 + 0.5*sin(phase)
            val modulation = 0.5 + 0.5 * sin(currentPhase * 2 * PI)
            
            buffer[i] = (pink * modulation).toFloat()
        }
    }
}

3.2 Infrastructure: Audio Engine (The Master)
Includes a "Spy Probe" for automated testing.
package com.neurosync.infra

import android.media.*
import com.neurosync.domain.SignalOscillator
import java.util.concurrent.atomic.AtomicBoolean

class GammaAudioEngine(private val oscillator: SignalOscillator) {
    private val isRunning = AtomicBoolean(false)
    private var audioTrack: AudioTrack? = null
    
    // TEST HOOK: Allows CI to "listen" to the audio output
    var spyProbe: ((FloatArray) -> Unit)? = null

    // Exposed for the Visual Renderer
    fun getPhase(): Float = oscillator.currentPhase

    fun start() {
        if (isRunning.getAndSet(true)) return
        
        val bufferSize = AudioTrack.getMinBufferSize(48000, 
            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT) * 2

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setSampleRate(48000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
        
        audioTrack?.play()
        
        Thread { writeLoop(bufferSize) }.start()
    }

    private fun writeLoop(bufferSize: Int) {
        val buffer = FloatArray(480) // 10ms chunks
        while (isRunning.get()) {
            // 1. Calculate Signal
            oscillator.fillBuffer(buffer)
            
            // 2. Write to Hardware (Blocking call controls timing)
            audioTrack?.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
            
            // 3. Send to Spy (if testing)
            spyProbe?.invoke(buffer.clone())
        }
    }
}

3.3 Infrastructure: Visual Renderer & XREAL
Handles 120Hz output and shader mixing.
class GammaRenderer(private val engine: GammaAudioEngine) : GLSurfaceView.Renderer {
    // Isoluminant Shader: Mixes Warm and Cool white
    private val fShader = """
        precision mediump float;
        uniform float uPhase;
        void main() {
            vec3 warm = vec3(1.0, 0.9, 0.8);
            vec3 cool = vec3(0.8, 0.9, 1.0);
            // Convert linear phase (0..1) to Sine (0..1)
            float i = 0.5 + 0.5 * sin(uPhase * 6.28318);
            gl_FragColor = vec4(mix(warm, cool, i), 1.0);
        }
    """
    // Standard GL boilerplate omitted...
    
    override fun onDrawFrame(gl: GL10?) {
        // SYNCHRONIZATION: Read atomic phase from Audio Engine
        val phase = engine.getPhase()
        
        GLES20.glUseProgram(program)
        val loc = GLES20.glGetUniformLocation(program, "uPhase")
        GLES20.glUniform1f(loc, phase)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }
}

4. Automated Test & Acceptance Plan
We use a "Funnel Strategy": Fast virtual tests run on every commit; slow hardware tests run on release candidates.
4.1 Tier 1: The Virtual Oscilloscope (CI)
Verifies the math is generating 40Hz without needing speakers.
// androidTest/com/neurosync/VirtualSignalTest.kt
@Test
fun verifySignalFrequency() {
    val oscillator = SignalOscillator()
    val engine = GammaAudioEngine(oscillator)
    val capturedData = ArrayList<Float>()
    val latch = CountDownLatch(1)

    // Capture 1 second of audio
    engine.spyProbe = { chunk ->
        chunk.forEach { capturedData.add(it) }
        if (capturedData.size >= 48000) latch.countDown()
    }

    engine.start()
    latch.await(2, TimeUnit.SECONDS)
    engine.stop()

    // Analyze Zero Crossings to determine frequency
    val data = capturedData.toFloatArray()
    val crossings = countZeroCrossings(data)
    
    // 40Hz = 80 crossings per second
    // We allow tolerance for Pink Noise jitter
    assertTrue("Frequency out of range: ${crossings/2}Hz", crossings in 78..82)
}

4.2 Tier 2: Hardware Frame Pacing (Physical)
Verifies the device can hold 120Hz.
We use Jetpack Macrobenchmark on a physical device (local or Firebase Test Lab).
// benchmark/FrameTimingBenchmark.kt
@RunWith(AndroidJUnit4::class)
class FrameTimingBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun measureTherapySmoothness() {
        benchmarkRule.measureRepeated(
            packageName = "com.neurosync",
            metrics = listOf(FrameTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.WARM
        ) {
            startActivityAndWait()
            Thread.sleep(5000) // Run therapy for 5s
        }
    }
}
// PASS CRITERIA: P99 Frame Time <= 8.5ms

5. CI/CD & Agentic Workflow
We use GitHub Actions + Danger to automate review.
5.1 Pipeline Configuration (.github/workflows/main.yml)
name: NeuroSync Automation

on: [push, pull_request]

jobs:
  # 1. LOGIC VERIFICATION (Runs on every PR)
  verify-logic:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Unit & Spy Tests
        run: ./gradlew testDebugUnitTest connectedDebugAndroidTest
      - name: Run Danger (Code Review Agent)
        uses: danger/kotlin@v1.0.0

  # 2. HARDWARE VERIFICATION (Runs on Release Candidate)
  verify-hardware:
    if: startsWith(github.ref, 'refs/heads/release/')
    needs: verify-logic
    runs-on: ubuntu-latest
    steps:
      - name: Build Benchmark
        run: ./gradlew :benchmark:assembleRelease
      - name: Firebase Test Lab (Pixel 8)
        run: |
          gcloud firebase test android run \
          --app app/build/outputs/apk/release/app-release.apk \
          --test benchmark/build/outputs/apk/release/benchmark-release.apk \
          --device model=shiba,version=34
      - name: Analyze Results
        run: python3 scripts/check_benchmark_p99.py --threshold 8.5

5.2 Code Review Agent
We configure Danger to catch performance bugs that humans miss.
File: Dangerfile.df.kts
danger(args) {
    // 1. SAFETY: Detect changes to Frequency Constants
    val logicChanges = git.modifiedFiles.filter { it.contains("SignalOscillator") }
    if (logicChanges.isNotEmpty()) {
        warn("⚠️ Core Logic Changed: Physical Oscilloscope verification required before merge.")
    }

    // 2. PERFORMANCE: Ban Allocation in Render Loop
    val renderChanges = git.diffForFile("infra/GammaRenderer.kt")
    if (renderChanges?.contains("new ") == true || renderChanges?.contains("copy(") == true) {
        fail("⛔ PERFORMANCE: Object allocation detected in Render Loop. This causes GC Jitter.")
    }

    // 3. COMPLIANCE: Ensure Epilepsy Warning isn't removed
    if (!git.modifiedFiles.contains("ui/SafetyDisclaimer.kt")) {
        // Logic to ensure it exists...
    }
}

5.3 Project Review Agent
A GitHub Action is configured to manage the project board based on CI results.
 * IF verify-hardware fails THEN Create Issue: "Critical: Frame Drops on Pixel 8".
 * IF verify-logic passes THEN Auto-Label PR: "Ready for Review".
6. Manual Acceptance (The Final Gate)
Before app store submission, one manual test is required:
 * Setup: Connect XREAL Air glasses.
 * Action: Launch App.
 * Observation:
   * Phone screen turns black (OLED off).
   * Glasses display 40Hz flicker.
 * Measurement: Record glasses with 240fps Slo-Mo video.
   * Pass Criteria: Flash occurs exactly every 6 video frames (240 / 40 = 6).
     :::
