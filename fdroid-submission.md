# F-Droid Submission for GammaSync

## Request for Packaging (RFP)

**App Name:** GammaSync
**Application ID:** com.gammasync
**License:** MIT
**Source Code:** https://github.com/matthewfrazier/gammasync
**Version:** 0.1.0

## Description

GammaSync is a precision 40Hz gamma entrainment therapy application that delivers synchronized audio-visual stimulation for cognitive enhancement. The app provides Gamma Entrainment Using Sensory stimuli (GENUS) with sub-millisecond A/V synchronization accuracy.

## F-Droid Compatibility

✅ **Clean Dependencies:** Only uses F-Droid compatible libraries (AndroidX, JUnit, Espresso)
✅ **No Proprietary Dependencies:** No Google Play Services, Firebase, or closed-source libraries
✅ **Privacy Focused:** No network permissions, completely offline operation
✅ **Open Source:** MIT License with full source availability
✅ **No Anti-Features:** No tracking, ads, or non-free dependencies

## Technical Requirements

- **Target Platform:** Android 12+ (API 31+)
- **Build System:** Gradle 8.13.2 + Kotlin 1.9.22
- **Hardware:** Optimized for 120Hz/144Hz OLED displays, XREAL Air glasses support

## Medical/Therapeutic App Considerations

- Built-in epilepsy safety disclaimers (mandatory "Hold-to-Agree" on every launch)
- No medical treatment claims - presented as research/wellness tool
- Open source allows verification of safety measures
- Isoluminant flicker (modulates chrominance, not luminance) for epilepsy safety

## Categories

- Science & Education
- Health & Fitness (Wellness)

## Build Instructions

```bash
./gradlew assembleRelease
```

## F-Droid Metadata Template

```yaml
Categories:
  - Science & Education
  - Health & Fitness
License: MIT
AuthorName: matthewfrazier
SourceCode: https://github.com/matthewfrazier/gammasync
IssueTracker: https://github.com/matthewfrazier/gammasync/issues

AutoName: GammaSync
Summary: 40Hz gamma entrainment therapy for cognitive enhancement

Description: |-
    GammaSync delivers precision 40Hz gamma entrainment using synchronized audio-visual 
    therapy. This therapeutic application provides Gamma Entrainment Using Sensory 
    stimuli (GENUS) for cognitive enhancement and brain health.
    
    Key Features:
    • Precise 40Hz audio-visual synchronization (sub-millisecond accuracy)
    • Supports 120Hz/144Hz OLED displays for optimal visual therapy
    • XREAL Air glasses compatibility for immersive sessions
    • Isoluminant flicker safety (modulates chrominance, not luminance)
    • Multiple therapy profiles and customization options
    • Built-in epilepsy safety disclaimers and protections
    
    Safety First:
    • Mandatory "Hold-to-Agree" epilepsy waiver on every launch
    • No network permissions - completely offline operation
    • Open source for transparency and verification
    • No data collection or transmission
    
    GammaSync requires Android 12+ and is optimized for high-refresh rate OLED displays.

RepoType: git
Repo: https://github.com/matthewfrazier/gammasync

Builds:
  - versionName: 0.1.0
    versionCode: 1
    commit: v0.1.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
CurrentVersion: 0.1.0
CurrentVersionCode: 1
```

## Submission Steps

1. **Fork fdroiddata repository:** https://gitlab.com/fdroid/fdroiddata
2. **Create metadata file:** Create `metadata/com.gammasync.yml` with the above template
3. **Submit Merge Request:** Submit MR to fdroiddata with the metadata file
4. **Alternative - RFP Issue:** Create an issue at https://gitlab.com/fdroid/fdroiddata/-/issues

## Timeline

- **Review Process:** Typically 1-3 months from submission to publication
- **First Build:** F-Droid will build from v0.1.0 tag
- **Updates:** Automated via AutoUpdateMode watching for new version tags