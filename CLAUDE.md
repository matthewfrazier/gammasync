# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Workflow (MANDATORY)

**STOP. Before writing ANY code, follow this workflow:**

### 1. Define "Done" First
For every task, explicitly state:
- What user action triggers the feature?
- What observable change should the user see/feel?
- What system state should change?

### 2. Write the Test BEFORE the Implementation
- **UI changes**: Write instrumentation test verifying visual state changes
- **Settings/toggles**: Write test verifying the setting actually affects the system
- **User interactions**: Write test verifying the full path from tap → feedback

### 3. Pre-Deploy Verification Checklist
Before ANY `installDebug`, verify:
- [ ] Does the feature do what it claims? (not just compile)
- [ ] Is the UI feedback visible/obvious? (calculate actual pixel sizes)
- [ ] Does state change trigger UI update? (invalidate/requestLayout called)
- [ ] Are proportions reasonable? (30% of 100dp = 30dp - is that visible?)

### 4. UI-Specific Requirements
- **Selection states**: Selected item must have OBVIOUS contrast (not subtle)
- **Toggles/switches**: Must immediately show effect, not just store preference
- **Progress indicators**: Must be proportionally sized (minimum 20% of container)
- **Touch targets**: Minimum 48dp for thumb-friendly interaction

### Example: Adding a Brightness Toggle
```
❌ WRONG: Add Switch, save to SharedPreferences, done
✅ RIGHT:
1. Definition of done: Toggle ON → screen brightness immediately goes to 100%
2. Write test: `onView(withId(R.id.brightnessSwitch)).perform(click())` → assert brightness == 1.0
3. Implement: Switch listener calls window.attributes.screenBrightness = 1f
4. Verify: Run test, confirm brightness actually changes
```

## Project Overview

GammaSync is an Android application delivering Gamma Entrainment Using Sensory stimuli (GENUS) - synchronized 40Hz audio-visual entrainment. The app requires sub-millisecond A/V synchronization and 120Hz display output.

**Target Platform:** Android 12+ (API 31+)
**Hardware Requirements:** 120Hz/144Hz OLED display, XREAL Air glasses support

## Build Commands

**IMPORTANT:** This machine has no system Java. Use Android Studio's bundled JBR:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

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

- **Master (Audio):** `GammaAudioEngine` wraps `AudioTrack` consuming samples at 48kHz. This dictates "Entrainment Time."
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

### Tier 0: UI State Verification (REQUIRED for all UI changes)
Before deploying UI changes, instrumentation tests MUST verify:
- Selection states visually change (background drawable, text color)
- Toggles affect system state immediately (brightness, volume)
- Progress indicators are proportionally sized
- Touch targets meet 48dp minimum

Run with: `./gradlew connectedDebugAndroidTest`

### Tier 1: Virtual Oscilloscope (CI)
Tests verify 40Hz signal generation via `spyProbe` hook on `GammaAudioEngine` - no physical audio hardware needed.

### Tier 2: Hardware Frame Pacing
Jetpack Macrobenchmark tests run on physical devices (Firebase Test Lab on release branches) to verify 120Hz frame timing.

## Safety Requirements

- Mandatory "Hold-to-Agree" epilepsy waiver on every cold launch (100% gated)
- `SafetyDisclaimer.kt` must never be removed or bypassed
- Changes to `SignalOscillator` require physical verification before merge