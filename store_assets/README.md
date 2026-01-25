# GammaSync Store Assets

This directory contains assets for the Google Play Store listing.

## Screenshots

Screenshots are captured using the automated script at `/scripts/capture_screenshots.sh`.

### Requirements
- Android device or emulator running GammaSync
- Screen resolution: 1080x1920 or 1080x2400 (Play Store requirement)
- ADB (Android Debug Bridge) installed and configured

### Usage
1. Connect your Android device or start an emulator
2. Install GammaSync on the device (`./gradlew installDebug`)
3. Run the screenshot capture script:
   ```bash
   cd scripts
   ./capture_screenshots.sh
   ```

### Screenshots Captured
1. **01_home_screen.png** - Main screen with mode selection
2. **02_mode_selection.png** - Different therapy mode selected
3. **03_settings.png** - Settings/preferences screen
4. **04_therapy_session.png** - Active therapy session with visual stimulation

### Play Store Guidelines
- Screenshots must be PNG format
- Phone screenshots: 1080x1920 or 1080x2400 resolution
- Minimum 4 screenshots, maximum 8 per device type
- Should showcase key app features and functionality

### Manual Coordination Required
The script automates navigation where possible but requires manual interaction for:
- Safety disclaimer acceptance (hold-to-agree button)
- Starting therapy sessions (safety-critical action)
- Verification that visual elements are properly displayed

This ensures safety protocols are respected while automating the tedious screenshot capture process.