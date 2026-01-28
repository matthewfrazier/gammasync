#!/bin/bash
# Automated screenshot capture for Play Store listing
# Captures required screenshots: home, settings, therapy session
# Output: PNG files in store_assets/screenshots/

set -e

OUTPUT_DIR="store_assets/screenshots"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
PACKAGE_NAME="com.gammasync"

# Play Store requirements:
# - Phone: 1080x1920 (16:9) or 1080x2400 (19.5:9)
# - Minimum 2 screenshots, maximum 8
# - PNG or JPEG format

echo "=== Play Store Screenshot Automation ==="
echo ""

# Check if device is connected
if ! "$ADB" devices | grep -q "device$"; then
    echo "Error: No Android device connected"
    echo "Connect a device or start an emulator"
    exit 1
fi

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Function to capture screenshot with description
capture_screen() {
    local name="$1"
    local description="$2"
    local wait_time="${3:-2}"  # Default 2 second wait

    echo "Capturing: $description"
    echo "  Please navigate to the screen and press Enter..."
    read -r

    # Wait for UI to settle
    sleep "$wait_time"

    # Capture screenshot
    local temp_file="/sdcard/screenshot_${name}.png"
    local output_file="${OUTPUT_DIR}/${name}.png"

    "$ADB" shell screencap -p "$temp_file"
    "$ADB" pull "$temp_file" "$output_file" > /dev/null 2>&1
    "$ADB" shell rm "$temp_file"

    echo "  Saved: $output_file"
    echo ""
}

echo "This script will guide you through capturing 4 required screenshots."
echo "After each prompt, navigate to the screen on your device, then press Enter."
echo ""
echo "Make sure the app is installed:"
echo "  ./gradlew installDebug"
echo ""
read -p "Press Enter to start..." -r

# Screenshot 1: Home screen
echo ""
echo "[1/4] Home Screen"
echo "  - Launch the app"
echo "  - Ensure you're on the Experience Mode selection screen"
echo "  - Select an interesting mode (e.g., Memory Discovery or Learning)"
capture_screen "01_home" "Home screen with mode selection"

# Screenshot 2: Settings
echo "[2/4] Settings Screen"
echo "  - Navigate to Settings tab (bottom navigation)"
echo "  - Scroll to show color scheme or RSVP settings"
capture_screen "02_settings" "Settings screen showing customization options"

# Screenshot 3: Therapy session
echo "[3/4] Experience Session"
echo "  - Go back to Experience tab"
echo "  - Start a session (any mode, any duration)"
echo "  - Tap screen to show controls (timer, pause button)"
capture_screen "03_session" "Active experience session with controls"

# Screenshot 4: RSVP Details (if Learning mode)
echo "[4/4] Reading Material (Optional - for Learning mode)"
echo "  - If showing Learning mode: tap 'Load Reading Material'"
echo "  - Show the text selection screen"
echo "  - OR: Skip this if you want session controls instead"
read -p "Capture this screen? (y/n): " -r
if [[ $REPLY =~ ^[Yy]$ ]]; then
    capture_screen "04_rsvp_details" "Reading material selection screen" 1
else
    echo "  Skipped - will use 3 screenshots"
    echo ""
fi

# Show results
echo "=== Screenshot Capture Complete ==="
echo ""
echo "Screenshots saved to: $OUTPUT_DIR/"
ls -lh "$OUTPUT_DIR"/*.png 2>/dev/null || echo "No screenshots found"
echo ""

# Check dimensions
if command -v sips &> /dev/null; then
    echo "Checking dimensions (Play Store accepts 1080x1920 or 1080x2400):"
    for file in "$OUTPUT_DIR"/*.png; do
        if [ -f "$file" ]; then
            WIDTH=$(sips -g pixelWidth "$file" | grep pixelWidth | awk '{print $2}')
            HEIGHT=$(sips -g pixelHeight "$file" | grep pixelHeight | awk '{print $2}')
            echo "  $(basename "$file"): ${WIDTH}x${HEIGHT}"
        fi
    done
    echo ""
fi

echo "Next steps:"
echo "  1. Review screenshots in $OUTPUT_DIR/"
echo "  2. Retake any screenshots if needed (re-run this script)"
echo "  3. Upload to Play Console: https://play.google.com/console"
echo ""
echo "Play Store requirements met:"
echo "  - Format: PNG"
echo "  - Count: $(ls -1 "$OUTPUT_DIR"/*.png 2>/dev/null | wc -l | tr -d ' ') screenshots (minimum 2)"
echo "  - Dimensions: Use device's native resolution"
echo ""
