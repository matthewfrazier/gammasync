# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GammaSync is an Android application delivering Gamma Entrainment Using Sensory stimuli (GENUS) - synchronized 40Hz audio-visual therapy. The app requires sub-millisecond A/V synchronization and 120Hz display output.

**Target Platform:** Android 12+ (API 31+)
**Hardware Requirements:** 120Hz/144Hz OLED display, XREAL Air glasses support

## Build Commands

```bash
# Build and run unit tests
./gradlew testDebugUnitTest

# Run instrumentation tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Build release APK
./gradlew assembleRelease

# Build benchmark APK
./gradlew :benchmark:assembleRelease
```

## Architecture

### Master-Slave Timing Pattern

The audio hardware clock is the single source of truth for timing - this is critical for precise synchronization:

- **Master (Audio):** `GammaAudioEngine` wraps `AudioTrack` consuming samples at 48kHz. This dictates "Therapy Time."
- **Slave (Video):** `GammaRenderer` queries the audio engine for current 40Hz phase (0.0-1.0) and renders that exact color. Never uses `System.nanoTime()`.

### Layer Separation

- **Domain Layer** (`com.neurosync.domain`): Pure Kotlin signal math with no Android dependencies. `SignalOscillator` calculates pink noise × sin(t).
- **Infrastructure Layer** (`com.neurosync.infra`): Android-specific implementations (`GammaAudioEngine`, `GammaRenderer`).

### Key Technical Constraints

- **Frame Time:** P99 must be < 8.5ms to maintain 120Hz
- **Audio Frequency:** 40.0Hz ±0.05Hz accuracy required
- **A/V Drift:** Must stay under 10ms
- **Visual Stimulus:** Isoluminant flicker (warm 2700K ↔ cool 6500K) - modulates chrominance, not luminance (epilepsy safety)

## Testing Strategy

### Tier 1: Virtual Oscilloscope (CI)
Tests verify 40Hz signal generation via `spyProbe` hook on `GammaAudioEngine` - no physical audio hardware needed.

### Tier 2: Hardware Frame Pacing
Jetpack Macrobenchmark tests run on physical devices (Firebase Test Lab on release branches) to verify 120Hz frame timing.

## Safety Requirements

- Mandatory "Hold-to-Agree" epilepsy waiver on every cold launch (100% gated)
- `SafetyDisclaimer.kt` must never be removed or bypassed
- Changes to `SignalOscillator` require physical verification before merge