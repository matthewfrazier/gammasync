# Proposal: Local MCP Integrations for Automated Review and Development Assistance

## Overview
A suite of local MCP (Model Context Protocol) servers and Claude Code subagents that provide specialized review, gating, testing, and development assistance for the GammaSync project.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Claude Code                               │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│  Code Review  │    │    Testing    │    │   Planning    │
│    Gateway    │    │   Specialist  │    │  Integration  │
└───────────────┘    └───────────────┘    └───────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│    Android    │    │    Issue      │    │   Timing      │
│   Pitfalls    │    │  Diagnostics  │    │   Analyzer    │
└───────────────┘    └───────────────┘    └───────────────┘
```

---

## Option A: MCP Servers (Tool-Based)

### 1. Code Review Gateway MCP

**Purpose:** Automated pre-commit and PR review with project-specific rules.

#### Tools Provided

**`review/check-render-loop`** - Scans for allocations in render-critical code paths.
- Detects: `new` keyword, `.copy()` calls, string concatenation, boxing operations

**`review/check-timing-source`** - Ensures visual code never uses system time directly.
- Detects: `System.nanoTime()`, `System.currentTimeMillis()`, `SystemClock.*`

**`review/check-frequency-constants`** - Gates changes to critical signal parameters (40Hz/48kHz).

**`review/gate-merge`** - Composite gating function that runs all checks and returns pass/fail.

#### Implementation Sketch
```typescript
// mcp-servers/code-review-gateway/src/index.ts
const RENDER_LOOP_PATTERNS = [
  /new\s+\w+\(/,
  /\.copy\(/,
  /"\s*\+\s*"/,
];

const FORBIDDEN_TIMING = [
  /System\.nanoTime\(\)/,
  /System\.currentTimeMillis\(\)/,
  /SystemClock\./
];
```

---

### 2. Test Specialist MCP

**Purpose:** Generate tests following project conventions, with special handling for timing-sensitive code.

#### Tools Provided

**`test/generate-unit`** - Creates JUnit tests for pure Kotlin domain classes.

**`test/generate-signal-validation`** - Creates frequency validation tests using FFT analysis.

**`test/generate-compose-ui`** - Creates Compose UI tests with proper test tags.

**`test/suggest-coverage-gaps`** - Analyzes existing tests and suggests missing coverage.

---

### 3. Issue Diagnostics MCP

**Purpose:** Analyze runtime issues, crashes, and performance problems specific to audio/visual sync apps.

#### Tools Provided

**`diagnose/frame-drops`** - Analyzes systrace/perfetto output for frame drop causes.
- Diagnoses: GC pauses, main thread blocking, GPU overdraw, shader compilation stalls

**`diagnose/audio-glitch`** - Diagnoses AudioTrack buffer underruns from logcat.

**`diagnose/av-drift`** - Measures audio-visual synchronization drift from test recordings.

**`diagnose/crash-analysis`** - Parses stack traces with Android lifecycle context.

---

### 4. Planning Integration MCP

**Purpose:** Integrate with GitHub Issues/Projects and provide planning assistance.

#### Tools Provided

**`plan/decompose-issue`** - Breaks down a feature issue into implementation tasks with dependencies.

**`plan/dependency-graph`** - Generates Mermaid diagram of issue dependencies.

**`plan/estimate-risk`** - Identifies technical risks (timing-critical changes, hardware deps, threading).

**`plan/sync-to-github`** - Creates/updates GitHub issues from local task definitions.

---

### 5. Android Pitfalls MCP

**Purpose:** Detect common Android development mistakes, especially for real-time audio/visual apps.

#### Tools Provided

**`pitfall/lifecycle-leak`** - Detects Activity/Fragment lifecycle leaks.
- Detects: Non-weak Context refs, unregistered listeners, ViewModel holding Views

**`pitfall/main-thread-violation`** - Identifies blocking operations on main thread.
- Detects: File I/O without Dispatchers.IO, network calls, heavy Composable computation

**`pitfall/audio-focus`** - Validates audio focus request/abandon pattern.

**`pitfall/display-refresh`** - Validates 120Hz display mode configuration.

**`pitfall/battery-drain`** - Identifies battery drain patterns (wakelocks, GPS, sensors).

---

### 6. Timing Analyzer MCP

**Purpose:** Specialized analysis for real-time audio/visual synchronization requirements.

#### Tools Provided

**`timing/validate-phase-lock`** - Verifies audio-visual phase lock implementation.

**`timing/buffer-size-recommendation`** - Recommends optimal buffer sizes for latency vs stability.

**`timing/jitter-analysis`** - Statistical analysis of frame/audio timing jitter.

---

## Option B: Claude Code Subagents (Prompt-Based)

Subagents are simpler to configure and don't require running separate server processes. They use Claude's capabilities directly with specialized prompts.

### Configuration (`.claude/settings.local.json`)

```json
{
  "agents": {
    "review-gate": {
      "description": "Code review gating agent for GammaSync. Use before commits to check for render loop allocations, timing violations, and frequency constant changes.",
      "prompt": "You are a code review agent for GammaSync, a real-time 40Hz audio-visual therapy app. Your job is to BLOCK commits that violate these rules:\n\n1. RENDER LOOP ALLOCATIONS: In any file containing 'Renderer' or 'onDrawFrame', flag: new keyword, .copy(), string concatenation, Integer.valueOf, Float.valueOf\n\n2. TIMING VIOLATIONS: In renderer code, flag any use of: System.nanoTime(), System.currentTimeMillis(), SystemClock.*. The renderer MUST only use GammaAudioEngine.getPhase().\n\n3. FREQUENCY CONSTANTS: Flag any changes to values 40.0, 48000, or targetFreq/sampleRate variables. These require physical verification.\n\nOutput format: PASS or FAIL with specific line numbers and violations."
    },
    "test-writer": {
      "description": "Test generation specialist for Android/Kotlin. Generates unit tests for domain classes and Compose UI tests.",
      "prompt": "You are a test generation specialist for GammaSync. Generate tests following these patterns:\n\n1. UNIT TESTS: Use JUnit 4, kotlinx-coroutines-test, Google Truth assertions. For timing tests, use runTest with advanceTimeBy.\n\n2. SIGNAL TESTS: For SignalOscillator, test frequency accuracy by counting zero crossings (40Hz = 80 crossings/sec).\n\n3. UI TESTS: Use Compose testing with testTag modifiers. Always test: initial state, user interactions, state changes.\n\n4. NAME FORMAT: Use backtick method names like `start sets isRunning to true`\n\nGenerate complete, runnable test files."
    },
    "android-doctor": {
      "description": "Android development pitfall detector. Identifies lifecycle leaks, main thread violations, and real-time audio/video issues.",
      "prompt": "You are an Android development expert specializing in real-time audio/visual applications. Analyze code for these pitfalls:\n\n1. LIFECYCLE LEAKS: Activity/Context references without WeakReference, listeners not unregistered in onDestroy, ViewModels holding View references\n\n2. MAIN THREAD: File I/O, network calls, or heavy computation not on Dispatchers.IO or background thread\n\n3. AUDIO ISSUES: Missing audio focus handling, wrong AudioTrack performance mode, buffer sizes causing underruns\n\n4. DISPLAY ISSUES: Not requesting 120Hz mode, not handling display disconnection for XREAL\n\n5. BATTERY: Unnecessary wakelocks, sensors left on, continuous location updates\n\nProvide specific fixes with code examples."
    },
    "timing-expert": {
      "description": "Real-time timing and synchronization specialist. Analyzes audio-visual sync, phase locking, and jitter.",
      "prompt": "You are a real-time systems expert specializing in audio-visual synchronization. For GammaSync:\n\n1. MASTER-SLAVE: Audio hardware clock (48kHz AudioTrack) is the ONLY timing source. Visual must query audio phase.\n\n2. PHASE LOCK: currentPhase must be 0.0-1.0, updated atomically, accessible cross-thread without locks.\n\n3. BUFFER SIZING: 480 samples = 10ms at 48kHz. Smaller = lower latency but more underrun risk.\n\n4. JITTER ANALYSIS: P99 frame time must be < 8.5ms for 120Hz. Identify GC, lock contention, or scheduling issues.\n\n5. A/V DRIFT: Must stay under 10ms. If drift detected, never adjust audio - visual must catch up.\n\nAnalyze timing-critical code and identify synchronization issues."
    },
    "issue-planner": {
      "description": "GitHub issue decomposition and planning agent. Breaks features into tasks with dependencies.",
      "prompt": "You are a technical planning agent for GammaSync. When given a feature or issue:\n\n1. DECOMPOSE: Break into tasks that can each be completed in under 4 hours\n\n2. DEPENDENCIES: Identify which tasks block others. Domain layer before infrastructure. Tests alongside implementation.\n\n3. RISK FLAGS: Mark tasks involving: timing-critical code, hardware dependencies, cross-thread sync, platform version requirements\n\n4. OUTPUT FORMAT: GitHub issue markdown with checkboxes, code file paths, and acceptance criteria\n\n5. ARCHITECTURE AWARENESS: Respect the master-slave pattern (audio is master), clean architecture (domain has no Android deps), and 120Hz requirement."
    }
  }
}
```

### Usage Examples

```bash
# Review code before commit
claude --agent review-gate "Review the changes in src/infra/GammaRenderer.kt"

# Generate tests for a class
claude --agent test-writer "Generate unit tests for SignalOscillator covering frequency accuracy"

# Diagnose an issue
claude --agent android-doctor "Analyze this crash: java.lang.OutOfMemoryError in GammaRenderer.onDrawFrame"

# Plan a feature
claude --agent issue-planner "Decompose issue #3 (Visual Renderer) into implementation tasks"
```

---

## Recommended Approach: Hybrid

| Capability | Implementation | Rationale |
|------------|----------------|-----------|
| Code Review Gating | **MCP Server** | Needs to run in pre-commit hook, parse AST |
| Test Generation | **Subagent** | Benefits from Claude's code generation |
| Issue Diagnostics | **Subagent** | Needs reasoning about complex logs |
| Planning | **Subagent** | Needs understanding of architecture docs |
| Android Pitfalls | **MCP Server** | Pattern matching on code, fast checks |
| Timing Analysis | **MCP Server** | Numerical analysis of timing data |

---

## MCP Server Setup

### Directory Structure
```
mcp-servers/
├── code-review-gateway/
│   ├── package.json
│   ├── tsconfig.json
│   └── src/
│       └── index.ts
├── android-pitfalls/
│   └── ...
└── timing-analyzer/
    └── ...
```

### Claude Code Configuration (`.claude/settings.json`)
```json
{
  "mcpServers": {
    "code-review-gateway": {
      "command": "node",
      "args": ["./mcp-servers/code-review-gateway/dist/index.js"],
      "env": {
        "PROJECT_ROOT": "."
      }
    },
    "android-pitfalls": {
      "command": "node",
      "args": ["./mcp-servers/android-pitfalls/dist/index.js"]
    },
    "timing-analyzer": {
      "command": "node",
      "args": ["./mcp-servers/timing-analyzer/dist/index.js"]
    }
  }
}
```

---

## Pre-commit Hook Integration

```bash
#!/bin/bash
# .git/hooks/pre-commit

CHANGED_FILES=$(git diff --cached --name-only | grep -E '\.(kt|java)$')

if [ -n "$CHANGED_FILES" ]; then
    echo "Running GammaSync review gate..."

    # Option 1: MCP tool
    # claude-code --tool "review/gate-merge" --files "$CHANGED_FILES"

    # Option 2: Subagent
    claude --agent review-gate "Review these files for merge: $CHANGED_FILES"

    if [ $? -ne 0 ]; then
        echo "Review gate BLOCKED commit. Fix issues above."
        exit 1
    fi
fi
```

---

## Implementation Priority

1. **Phase 1:** `review-gate` subagent + basic MCP for pattern matching
2. **Phase 2:** `android-doctor` and `timing-expert` subagents
3. **Phase 3:** `test-writer` subagent
4. **Phase 4:** Full MCP servers for automated CI integration
