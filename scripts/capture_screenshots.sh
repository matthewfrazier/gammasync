#!/bin/bash

# Play Store Screenshot Capture Script
# Captures required screenshots for GammaSync Play Store listing
# Requirements: Android emulator/device with GammaSync installed

set -e

# Configuration
PACKAGE_NAME="com.gammasync"
ACTIVITY_NAME="com.gammasync.MainActivity" 
OUTPUT_DIR="../store_assets/screenshots"
SCREENSHOT_DELAY=3  # seconds between screen captures
NAVIGATION_DELAY=1  # seconds between UI actions

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}GammaSync Play Store Screenshot Capture${NC}"
echo "=========================================="

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo -e "${RED}Error: ADB not found. Please install Android SDK platform-tools.${NC}"
    exit 1
fi

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo -e "${RED}Error: No Android device/emulator connected.${NC}"
    echo "Please connect a device or start an emulator."
    exit 1
fi

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo -e "${GREEN}✓ Device connected${NC}"
echo -e "${GREEN}✓ Output directory: $OUTPUT_DIR${NC}"

# Get device screen resolution
RESOLUTION=$(adb shell wm size | grep -o '[0-9]*x[0-9]*')
echo -e "${GREEN}✓ Device resolution: $RESOLUTION${NC}"

# Validate resolution for Play Store requirements
if [[ "$RESOLUTION" != "1080x1920" && "$RESOLUTION" != "1080x2400" && "$RESOLUTION" != "1080x2340" ]]; then
    echo -e "${YELLOW}Warning: Resolution $RESOLUTION may not meet Play Store requirements (1080x1920 or 1080x2400)${NC}"
fi

# Function to capture screenshot
capture_screenshot() {
    local filename="$1"
    local description="$2"
    
    echo -e "${BLUE}Capturing: $description${NC}"
    sleep $SCREENSHOT_DELAY
    
    adb exec-out screencap -p > "$OUTPUT_DIR/$filename"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Saved: $OUTPUT_DIR/$filename${NC}"
    else
        echo -e "${RED}✗ Failed to capture: $filename${NC}"
        return 1
    fi
}

# Function to tap screen coordinates
tap_screen() {
    local x="$1"
    local y="$2"
    echo -e "${BLUE}  Tapping at ($x, $y)${NC}"
    adb shell input tap $x $y
    sleep $NAVIGATION_DELAY
}

# Function to press back button
press_back() {
    echo -e "${BLUE}  Pressing back button${NC}"
    adb shell input keyevent 4
    sleep $NAVIGATION_DELAY
}

# Function to press home button
press_home() {
    echo -e "${BLUE}  Pressing home button${NC}"
    adb shell input keyevent 3
    sleep $NAVIGATION_DELAY
}

# Function to start the app
start_app() {
    echo -e "${BLUE}Starting GammaSync app...${NC}"
    adb shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME"
    sleep 3
}

# Function to wait for user input
wait_for_user() {
    local message="$1"
    echo -e "${YELLOW}$message${NC}"
    read -p "Press Enter when ready..."
}

# Function to dismiss disclaimer (if shown)
dismiss_disclaimer() {
    echo -e "${BLUE}Checking for safety disclaimer...${NC}"
    # The disclaimer likely has a "Hold to Agree" button based on CLAUDE.md
    # We'll wait for manual interaction since this is safety-critical
    sleep 2
}

# Function to navigate to settings via UI element
navigate_to_settings() {
    echo -e "${BLUE}Navigating to settings...${NC}"
    # Based on HomeView.kt, settings button is at R.id.settingsButton
    # For 1080x1920 screen, settings is typically in top-right
    tap_screen 960 120
}

# Function to select a therapy mode
select_therapy_mode() {
    local mode="$1"
    echo -e "${BLUE}Selecting therapy mode: $mode${NC}"
    
    case $mode in
        "neurosync")
            tap_screen 540 400  # Approximate center-left position
            ;;
        "memory")
            tap_screen 540 500  # Below neurosync
            ;;
        "sleep")
            tap_screen 540 600  # Below memory
            ;;
        "migraine")
            tap_screen 540 700  # Below sleep
            ;;
        "moodlift")
            tap_screen 540 800  # Bottom mode
            ;;
        *)
            echo -e "${YELLOW}Unknown mode: $mode${NC}"
            ;;
    esac
}

echo ""
echo -e "${BLUE}Starting screenshot capture process...${NC}"
echo ""

# Start fresh - kill app and restart
echo -e "${BLUE}Preparing to capture screenshots...${NC}"
adb shell am force-stop $PACKAGE_NAME
sleep 1
start_app

# Wait for app to fully load and handle disclaimer
dismiss_disclaimer
wait_for_user "If a safety disclaimer appeared, please hold the button to agree and proceed to the home screen. Press Enter when you see the mode selection screen."

# Screenshot 1: Home Screen
echo -e "${BLUE}=== Screenshot 1: Home Screen ===${NC}"
echo "Capturing home screen with default mode selection..."
capture_screenshot "01_home_screen.png" "Home screen with mode selection"

# Screenshot 2: Different Mode Selected
echo -e "${BLUE}=== Screenshot 2: Mode Selection ===${NC}"
echo "Selecting a different therapy mode for variety..."
select_therapy_mode "memory"
sleep 1
capture_screenshot "02_mode_selection.png" "Memory mode selected"

# Screenshot 3: Settings Screen  
echo -e "${BLUE}=== Screenshot 3: Settings ===${NC}"
navigate_to_settings
sleep 2
capture_screenshot "03_settings.png" "Settings screen"

# Return to home screen
press_back
sleep 1

# Screenshot 4: Therapy Session 
echo -e "${BLUE}=== Screenshot 4: Therapy Session ===${NC}"
echo "Setting up therapy session..."
# Select NeuroSync mode and 30 min duration
select_therapy_mode "neurosync"
sleep 1

# Tap duration (30 min is default, but let's make sure)
tap_screen 540 1000  # Approximate position of duration buttons

# Wait for user to manually start session since it involves safety checks
wait_for_user "Please tap the START button and wait for the therapy session to begin. The visual stimulation should be active. Press Enter when ready for screenshot."
capture_screenshot "04_therapy_session.png" "Active therapy session"

# Stop the session safely
echo -e "${BLUE}Stopping therapy session...${NC}"
tap_screen 540 960  # Tap center to show controls
sleep 1
tap_screen 540 1100 # Approximate "Done" button position
sleep 2

# Cleanup - stop the session and close app
echo ""
echo -e "${BLUE}Cleaning up...${NC}"
adb shell am force-stop $PACKAGE_NAME

echo ""
echo -e "${GREEN}=========================================="
echo "Screenshot capture completed!"
echo "==========================================${NC}"
echo ""
echo "Screenshots saved to: $OUTPUT_DIR"
echo ""
echo "Files created:"
ls -la "$OUTPUT_DIR"/*.png 2>/dev/null || echo "No PNG files found - check for errors above"

echo ""
echo -e "${BLUE}Notes:${NC}"
echo "• Verify image dimensions are correct for Play Store (1080x1920 or 1080x2400)"
echo "• Check that images show clear UI and are not blurry"  
echo "• Consider editing images to highlight key features"
echo "• Remember to follow Play Store screenshot guidelines"

exit 0