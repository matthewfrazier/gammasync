# iOS Development Requirements for GammaSync

## Overview
This document outlines all requirements for developing and deploying an iOS version of GammaSync with identical functionality to the Android version.

## Development Environment Requirements

### Hardware Requirements
- **Mac Computer**: macOS 13.0 (Ventura) or later
  - Intel-based Mac or Apple Silicon (M1/M2/M3)
  - Minimum 8GB RAM (16GB recommended)
  - 100GB+ free disk space for Xcode, simulators, and dependencies
- **iOS Test Device**: iPhone 13 Pro or newer with 120Hz ProMotion display
  - Required for real-time 120Hz testing
  - iPad Pro with ProMotion also acceptable for testing
- **External Display (Optional)**: For XREAL Air testing

### Software Requirements
- **Xcode 15.0+**: Latest stable version
- **iOS SDK 17.0+**: Target iOS 15.0+ for broader compatibility
- **Swift 5.9+**: Included with Xcode
- **CocoaPods or Swift Package Manager**: For dependency management
- **Git**: For version control
- **Android Studio**: For Kotlin Multiplatform development

### Development Tools
- **Kotlin Multiplatform Mobile Plugin**: For Xcode integration
- **Instruments**: For performance profiling (included with Xcode)
- **Simulator**: iOS Simulator for initial testing
- **TestFlight**: For beta distribution and testing

## Project Architecture Requirements

### Kotlin Multiplatform Setup
```
gammasync/
├── shared/               # Kotlin Multiplatform module
│   ├── src/
│   │   ├── commonMain/   # Shared domain layer (existing Android domain code)
│   │   ├── androidMain/  # Android-specific implementations
│   │   └── iosMain/      # iOS-specific implementations
├── android/              # Existing Android app
└── ios/                  # New iOS app (Xcode project)
    ├── GammaSync.xcodeproj
    ├── GammaSync/        # Main app target
    └── GammaSyncTests/   # Unit tests
```

### iOS-Specific Libraries Needed
- **AudioUnit Framework**: For real-time audio processing
- **Metal Framework**: For high-performance graphics rendering
- **Core Audio**: For hardware clock synchronization
- **AVFoundation**: For audio engine management
- **Core Haptics**: For haptic feedback (equivalent to Android HapticFeedback)
- **External Accessory**: For potential XREAL integration

## Business Requirements

### Apple Developer Program
- **Apple Developer Account**: $99/year
  - Individual or Organization account
  - Required for App Store distribution
  - Required for TestFlight beta testing
- **Bundle Identifier**: Unique app identifier (e.g., com.gammasync.ios)
- **App Store Connect Access**: For app submission and management

### Legal Entity Requirements

#### Option 1: Individual Developer (Simplest)
- **Cost**: $99/year Apple Developer fee only
- **Timeline**: Immediate setup
- **Pros**: Simple, no additional legal costs
- **Cons**: Personal liability, limited business credibility

#### Option 2: LLC Formation (Recommended for Business)
- **Cost Breakdown**:
  - Delaware LLC Formation: $90-$300 (state fee)
  - Registered Agent: $100-$300/year
  - Legal Fees: $500-$2,000 (if using attorney)
  - EIN (Federal Tax ID): Free
  - Operating Agreement: $200-$1,000 (legal fees)
  - **Total First Year**: $890-$3,600
- **Timeline**: 1-3 weeks for formation
- **Ongoing**: $200-$600/year (registered agent + state fees)

### D&B DUNS Number
- **Purpose**: Business credit establishment, vendor qualification
- **Process**: Apply directly at DNB.com
- **Cost**: Free for basic number, $500+ for expedited or enhanced
- **Timeline**: 5-10 business days (free), 1-2 days (expedited)
- **Requirements**: Business legal name, address, EIN

### App Store Submission Requirements
- **App Review Guidelines Compliance**
- **Privacy Policy**: Required for health-related apps
- **Medical Device Disclaimer**: GammaSync therapeutic claims
- **Age Rating**: 17+ due to epilepsy warnings
- **Accessibility Compliance**: VoiceOver, Dynamic Type support

## Timeline Estimates

### Phase 1: Business Setup (2-4 weeks)
- LLC Formation: 1-3 weeks
- Apple Developer Account: 1-2 days
- D&B DUNS Number: 1-2 weeks
- **Parallel Activities**: Can be done simultaneously

### Phase 2: Development Environment (1 week)
- Xcode installation and setup: 1 day
- KMP project structure: 2-3 days
- iOS project template: 1-2 days

### Phase 3: iOS Infrastructure Layer (8-12 weeks)
- Audio Engine (AudioUnit): 3-4 weeks
- Visual Renderer (Metal + CADisplayLink): 3-4 weeks
- External display support: 2-3 weeks
- Testing and optimization: 2-3 weeks

### Phase 4: iOS UI Layer (4-6 weeks)
- SwiftUI app structure: 1-2 weeks
- Therapy session interfaces: 2-3 weeks
- Settings and navigation: 1-2 weeks

### Phase 5: App Store Preparation (2-3 weeks)
- App Store assets creation: 1 week
- App review preparation: 1 week
- Beta testing via TestFlight: 1-2 weeks

**Total Timeline**: 17-26 weeks (4-6 months)

## Cost Summary

### Development Costs
- **Apple Developer Program**: $99/year
- **Mac Hardware** (if needed): $1,299-$3,999
- **iOS Test Device** (if needed): $999-$1,199
- **Development Time**: 17-26 weeks @ developer rate

### Business Setup Costs
- **LLC Formation & Legal**: $890-$3,600 (first year)
- **Ongoing Business Costs**: $200-$600/year
- **D&B DUNS Number**: Free-$500

### Optional Costs
- **Legal Review**: $1,000-$5,000 (for medical app compliance)
- **App Store Optimization**: $500-$2,000
- **Beta Testing Program**: $200-$1,000

## Risk Mitigation

### Technical Risks
- **ProMotion Compatibility**: Test on actual hardware early
- **Audio Latency**: Validate AudioUnit approach with prototype
- **App Store Rejection**: Early compliance review

### Business Risks
- **Medical App Review**: Extra scrutiny for therapeutic claims
- **Epilepsy Warnings**: Strict safety disclaimer requirements
- **Competition**: Time to market considerations

## Next Steps Priority
1. **Business Entity Decision**: LLC vs Individual (legal/tax consultation recommended)
2. **Environment Setup**: Mac + Xcode + test device procurement
3. **Apple Developer Registration**: Start business entity registration process
4. **KMP Project Setup**: Begin shared code extraction
5. **iOS Proof of Concept**: Validate audio/video sync approach

## Automation Opportunities
- **CI/CD Pipeline**: GitHub Actions with fastlane for iOS
- **App Store Connect API**: Automated metadata updates
- **TestFlight Distribution**: Automated beta deployments
- **Code Signing**: Automated certificate management

## References
- [Apple Developer Documentation](https://developer.apple.com/documentation/)
- [Kotlin Multiplatform for iOS](https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html)
- [App Store Review Guidelines](https://developer.apple.com/app-store/review/guidelines/)
- [Delaware Division of Corporations](https://corp.delaware.gov/)
- [D&B DUNS Number Application](https://www.dnb.com/duns-number.html)