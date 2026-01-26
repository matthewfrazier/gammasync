# RSVP Integration Tracking

**Branch:** `feature/rsvp-integration`
**Status:** In Progress
**Target:** Learning Mode (MEMORY_WRITE)

## Overview

Integrate Rapid Serial Visual Presentation (RSVP) with Learning mode to enable reading text documents while receiving theta-gamma entrainment. Words are displayed one at a time, optionally phase-locked to the 40Hz gamma bursts.

## Existing Components

| Component | Status | Location |
|-----------|--------|----------|
| RsvpOverlay | ✅ Complete | `ui/RsvpOverlay.kt` |
| RSVP Settings | ✅ Storage ready | `data/SettingsRepository.kt` |
| TextProcessor | ✅ Complete | `domain/text/TextProcessor.kt` |
| DocumentLoader | ✅ Complete | `infra/DocumentLoader.kt` |
| Learning Mode | ✅ Audio/visual ready | `TherapyProfiles.MEMORY_WRITE` |

## Implementation Phases

### Phase 1: Text Loading Infrastructure ✅
- [x] Add Commonmark dependency for markdown parsing
- [x] Create `TextProcessor` for text cleaning pipeline
- [x] Create `DocumentLoader` for file picker integration
- [x] Unit tests for TextProcessor

### Phase 2: UI Integration
- [ ] Add "Load Text" button to Learning mode in HomeView
- [ ] Implement file picker using `ACTION_OPEN_DOCUMENT`
- [ ] Show loaded file name and word count
- [ ] Add reading time estimate display
- [ ] Store last loaded document URI for quick re-load

**Research needed:**
- How to persist document URI permissions across sessions
- Best UX for file selection in mode-specific context

### Phase 3: RSVP Display Integration
- [ ] Add RsvpOverlay to therapy screen layout
- [ ] Connect RsvpOverlay to audio engine phase provider
- [ ] Start RSVP when Learning session starts (if text loaded)
- [ ] Handle RSVP completion before session ends
- [ ] Sync RSVP with external display (XREAL) if connected

**Research needed:**
- Optimal phase-lock timing for word transitions
- Whether to pause RSVP on session pause

### Phase 4: Settings UI
- [ ] Add RSVP section to Settings screen
- [ ] WPM slider (100-1000, default 300)
- [ ] Phase-lock toggle (sync words to gamma bursts)
- [ ] Font size preference

**Research needed:**
- Should WPM affect estimated session duration?
- Default phase-lock behavior

### Phase 5: Polish & Testing
- [ ] Instrumentation tests for file loading flow
- [ ] Test with various markdown files
- [ ] Test with large documents (near 1MB limit)
- [ ] Accessibility review (TalkBack compatibility)
- [ ] Memory profiling for long documents

## Technical Notes

### Text Processing Pipeline
```
Raw Text (.txt/.md)
    ↓
[If .md] Commonmark → Plain Text
    ↓
Strip URLs (regex)
    ↓
Strip non-word chars (keep apostrophes)
    ↓
Normalize whitespace
    ↓
Split into word list
    ↓
RsvpOverlay.setText(words)
```

### Phase-Lock Behavior
When enabled, word transitions sync to the theta-gamma burst pattern:
- 6Hz theta cycle = 166.67ms per cycle
- Gamma burst at 30% duty = ~50ms burst window
- Word change triggers on rising edge of theta phase

At 300 WPM (200ms/word), this means ~1.2 words per theta cycle. Phase-lock adjusts timing slightly to align transitions.

### File Size Limits
- Max file: 1 MB (prevents OOM)
- ~170,000 words max (average 6 chars/word)
- At 300 WPM = ~9.4 hours reading time (way beyond session length)

## Open Questions

1. Should RSVP be exclusive to Learning mode, or available in Memory mode too?
2. Auto-pause session when RSVP completes, or continue with just audio/visual?
3. Save reading position for resume later?
4. Support epub/PDF in future phases?

## Related Files

- `app/src/main/java/com/gammasync/ui/RsvpOverlay.kt`
- `app/src/main/java/com/gammasync/domain/text/TextProcessor.kt`
- `app/src/main/java/com/gammasync/infra/DocumentLoader.kt`
- `app/src/main/java/com/gammasync/data/SettingsRepository.kt`
- `app/src/main/java/com/gammasync/domain/therapy/TherapyProfiles.kt`
