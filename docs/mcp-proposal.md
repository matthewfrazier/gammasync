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

### Configuration: Markdown Files in `.claude/agents/`

Subagents are defined as **Markdown files** with YAML frontmatter. Place them in:
- `.claude/agents/` - Project-level (checked into version control)
- `~/.claude/agents/` - User-level (available in all projects)

#### Directory Structure
```
.claude/agents/
├── review-gate.md
├── test-writer.md
├── android-doctor.md
├── timing-expert.md
└── issue-planner.md
```

#### Example: `review-gate.md`
```markdown
---
name: review-gate
description: Code review gating agent for GammaSync. Use before commits to check for render loop allocations, timing violations, and frequency constant changes.
model: sonnet
tools: Read, Grep, Glob
---

You are a code review agent for GammaSync, a real-time 40Hz audio-visual therapy app.
Your job is to BLOCK commits that violate these rules:

## 1. Render Loop Allocations
In any file containing `Renderer` or `onDrawFrame`, flag:
- `new` keyword, `.copy()` calls, string concatenation, boxing operations

## 2. Timing Violations
In renderer code, flag any use of:
- `System.nanoTime()`, `System.currentTimeMillis()`, `SystemClock.*`
The renderer MUST only use `GammaAudioEngine.getPhase()`.

## 3. Frequency Constants
Flag any changes to: `40.0`, `48000`, `targetFreq`, `sampleRate`

Output format: PASS or FAIL with specific line numbers and violations.
```

#### Frontmatter Fields

| Field | Required | Description |
|-------|----------|-------------|
| `name` | Yes | Unique identifier (lowercase, hyphens) |
| `description` | Yes | When Claude should delegate to this subagent |
| `tools` | No | Tools the subagent can use (inherits all if omitted) |
| `disallowedTools` | No | Tools to deny |
| `model` | No | `sonnet`, `opus`, `haiku`, or `inherit` (default) |
| `permissionMode` | No | `default`, `acceptEdits`, `dontAsk`, `bypassPermissions`, `plan` |
| `skills` | No | Skills to preload into context |
| `hooks` | No | Lifecycle hooks scoped to this subagent |

### Installed Subagents

This project includes these subagents in `.claude/agents/`:

| Agent | Purpose | Tools |
|-------|---------|-------|
| `review-gate` | Pre-commit code review gating | Read, Grep, Glob |
| `test-writer` | Generate unit and UI tests | Read, Write, Grep, Glob |
| `android-doctor` | Android pitfall detection | Read, Grep, Glob, Bash |
| `timing-expert` | Real-time sync analysis | Read, Grep, Glob |
| `issue-planner` | Feature decomposition | Read, Grep, Glob, Bash |

### Usage Examples

Claude automatically delegates to subagents based on their descriptions. You can also invoke them explicitly:

```bash
# Review code before commit
"Use the review-gate agent to check src/infra/GammaRenderer.kt"

# Generate tests for a class
"Use test-writer to generate unit tests for SignalOscillator"

# Diagnose an issue
"Use android-doctor to analyze this crash: java.lang.OutOfMemoryError"

# Plan a feature
"Use issue-planner to decompose issue #3 into implementation tasks"
```

### CLI-Defined Subagents (Session Only)

For temporary/testing subagents, use the `--agents` CLI flag with JSON:

```bash
claude --agents '{
  "quick-reviewer": {
    "description": "Quick code review",
    "prompt": "Review code for obvious issues",
    "tools": ["Read", "Grep"],
    "model": "haiku"
  }
}'
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
