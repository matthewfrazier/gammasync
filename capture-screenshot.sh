#!/bin/bash
# Screenshot capture tool with automatic resizing for API analysis
# Max dimension: 2000 pixels (API limit for multi-image requests)

set -e

MAX_DIMENSION=1800  # Stay safely under 2000px limit
OUTPUT_DIR="screenshots"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"

# Create output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

# Check if a device is connected
if ! "$ADB" devices | grep -q "device$"; then
    echo "Error: No Android device connected"
    echo "Connect a device or start an emulator"
    exit 1
fi

# Capture screenshot
echo "Capturing screenshot..."
TEMP_FILE="/sdcard/screenshot_temp.png"
OUTPUT_FILE="${OUTPUT_DIR}/screenshot_${TIMESTAMP}.png"

"$ADB" shell screencap -p "$TEMP_FILE"
"$ADB" pull "$TEMP_FILE" "$OUTPUT_FILE"
"$ADB" shell rm "$TEMP_FILE"

# Get image dimensions
if command -v identify &> /dev/null; then
    # Using ImageMagick
    DIMENSIONS=$(identify -format "%wx%h" "$OUTPUT_FILE")
    WIDTH=$(echo $DIMENSIONS | cut -d'x' -f1)
    HEIGHT=$(echo $DIMENSIONS | cut -d'x' -f2)

    echo "Original dimensions: ${WIDTH}x${HEIGHT}"

    # Resize if needed
    if [ "$WIDTH" -gt "$MAX_DIMENSION" ] || [ "$HEIGHT" -gt "$MAX_DIMENSION" ]; then
        echo "Resizing to fit within ${MAX_DIMENSION}px..."
        RESIZED_FILE="${OUTPUT_DIR}/screenshot_${TIMESTAMP}_resized.png"
        convert "$OUTPUT_FILE" -resize "${MAX_DIMENSION}x${MAX_DIMENSION}>" "$RESIZED_FILE"
        echo "Resized screenshot saved to: $RESIZED_FILE"

        # Show new dimensions
        NEW_DIMS=$(identify -format "%wx%h" "$RESIZED_FILE")
        echo "New dimensions: $NEW_DIMS"
        echo ""
        echo "Use this file for API analysis: $RESIZED_FILE"
    else
        echo "Image is within size limits"
        echo "Screenshot saved to: $OUTPUT_FILE"
    fi
elif command -v sips &> /dev/null; then
    # Using sips (macOS built-in)
    WIDTH=$(sips -g pixelWidth "$OUTPUT_FILE" | grep pixelWidth | awk '{print $2}')
    HEIGHT=$(sips -g pixelHeight "$OUTPUT_FILE" | grep pixelHeight | awk '{print $2}')

    echo "Original dimensions: ${WIDTH}x${HEIGHT}"

    # Resize if needed
    if [ "$WIDTH" -gt "$MAX_DIMENSION" ] || [ "$HEIGHT" -gt "$MAX_DIMENSION" ]; then
        echo "Resizing to fit within ${MAX_DIMENSION}px..."
        RESIZED_FILE="${OUTPUT_DIR}/screenshot_${TIMESTAMP}_resized.png"
        sips -Z "$MAX_DIMENSION" "$OUTPUT_FILE" --out "$RESIZED_FILE" > /dev/null
        echo "Resized screenshot saved to: $RESIZED_FILE"

        # Show new dimensions
        NEW_WIDTH=$(sips -g pixelWidth "$RESIZED_FILE" | grep pixelWidth | awk '{print $2}')
        NEW_HEIGHT=$(sips -g pixelHeight "$RESIZED_FILE" | grep pixelHeight | awk '{print $2}')
        echo "New dimensions: ${NEW_WIDTH}x${NEW_HEIGHT}"
        echo ""
        echo "Use this file for API analysis: $RESIZED_FILE"
    else
        echo "Image is within size limits"
        echo "Screenshot saved to: $OUTPUT_FILE"
    fi
else
    echo "Warning: Neither ImageMagick nor sips found - cannot auto-resize"
    echo "Screenshot saved to: $OUTPUT_FILE"
    echo ""
    echo "To resize manually:"
    echo "  macOS: sips -Z $MAX_DIMENSION $OUTPUT_FILE --out ${OUTPUT_FILE%.png}_resized.png"
    echo "  ImageMagick: convert $OUTPUT_FILE -resize ${MAX_DIMENSION}x${MAX_DIMENSION}> ${OUTPUT_FILE%.png}_resized.png"
fi

echo ""
echo "Done!"
