# Recent Fixes

## ✅ All Hardcoded Colors Made Theme-Aware

**Problem**: Multiple UI views had hardcoded colors (primarily white `0xFFFFFFFF`) instead of respecting Material Design 3 theme attributes.

**Solution**: Systematically replaced all hardcoded colors with `MaterialColors.getColor()` calls that retrieve theme-aware colors.

### Files Fixed

#### 1. **RsvpDetailsView.kt**
- **Line 159**: Selected tab text color now uses `colorOnPrimary`

#### 2. **HomeView.kt**
- **Lines 156-159**: Icon tint colors now use `colorError` and `colorPrimary` with getter properties
- **Lines 378, 423, 533, 584, 612**: All selected button text colors now use `colorOnPrimary`
- **Line 638**: Color chip border now uses `colorOnSurface`

#### 3. **SettingsView.kt**
- **Lines 153, 201, 231**: All selected button text colors now use `colorOnPrimary`
- **Line 257**: Color chip border now uses `colorOnSurface`

#### 4. **CircularTimerView.kt**
- **Line 52**: Timer text color now uses `colorOnSurface`
- Added `MaterialColors` import

#### 5. **HoldProgressButton.kt**
- **Line 43**: Progress ring color now uses `colorPrimary`
- Added `MaterialColors` import

### Theme Attribute Mapping

| Old Hardcoded Color | New Theme Attribute | Usage |
|---------------------|---------------------|-------|
| `0xFFFFFFFF` (white) | `colorOnPrimary` | Text on accent-colored backgrounds |
| `0xFFFFFFFF` (white) | `colorOnSurface` | Text on surface backgrounds, borders |
| `0xFF26A69A` (teal) | `colorPrimary` | Progress indicators, accents |
| `0xFFFF5252` (red) | `colorError` | Error/required state indicators |
| `0xFF4CAF50` (green) | `colorPrimary` | Success/connected state indicators |

### Benefits

1. ✅ **Proper dark/light mode support**: Colors automatically adapt when theme changes
2. ✅ **User-selected accent colors**: Theme attributes respect custom color schemes
3. ✅ **Material Design 3 compliance**: Uses standard MD3 color roles
4. ✅ **Accessibility**: Maintains proper contrast ratios across themes
5. ✅ **Consistency**: All UI elements follow the same color system

### Code Example

**Before:**
```kotlin
button.setTextColor(0xFFFFFFFF.toInt()) // Hardcoded white
```

**After:**
```kotlin
val onPrimaryColor = MaterialColors.getColor(this,
    com.google.android.material.R.attr.colorOnPrimary,
    0xFFFFFFFF.toInt())
button.setTextColor(onPrimaryColor) // Theme-aware with fallback
```

## 📷 Image Dimension API Error Workaround

**Problem**: Screenshot analysis failed with:
```
API Error: 400 - At least one of the image dimensions exceed max allowed size
for many-image requests: 2000 pixels
```

**Solution**: Created `capture-screenshot.sh` script that:
1. Captures screenshot via ADB
2. Automatically resizes to max 1800px (safely under 2000px limit)
3. Uses `sips` (macOS) or ImageMagick if available

### Usage

```bash
./capture-screenshot.sh
```

Screenshots will be saved to `screenshots/` directory with automatic resizing.

### Manual Resize Options

**macOS (sips)**:
```bash
sips -Z 1800 input.png --out output.png
```

**ImageMagick**:
```bash
convert input.png -resize 1800x1800> output.png
```

**Python (PIL)**:
```python
from PIL import Image

img = Image.open('input.png')
img.thumbnail((1800, 1800), Image.Resampling.LANCZOS)
img.save('output.png')
```

## ✅ RSVP Display Settings Moved to Settings View

**User Request**: "Reading Material Display Settings should all be moved into the Settings view, then a link from Reading Material should jump there if needed."

### Implementation Complete

**Files Modified:**

1. **`view_settings.xml`**: Added RSVP Display section with:
   - Text Size slider (5-25%)
   - Focus Letter Highlight toggle
   - Break Long Words toggle

2. **`SettingsView.kt`**:
   - Added RSVP settings views and listeners
   - Settings auto-save to repository on change
   - Integrated with existing theme-aware styling

3. **`view_rsvp_details.xml`**:
   - Removed inline Display Settings card
   - Added bottom-aligned link card to RSVP Settings
   - Link shows icon + descriptive text

4. **`RsvpDetailsView.kt`**:
   - Removed settings UI code
   - Added `onRsvpSettingsClicked` callback
   - Updated `getRsvpSettings()` to read from repository
   - Removed `saveSettings()` method

5. **`MainActivity.kt`**:
   - Removed `saveSettings()` calls
   - Added settings link handler to navigate to Settings tab

6. **`HomeView.kt`**:
   - Added `navigateToSettingsTab()` method
   - Allows programmatic navigation to Settings tab

7. **`strings.xml`**:
   - Added RSVP-related string resources

8. **`ic_arrow_forward.xml`**:
   - Created forward arrow drawable for link indicator

### User Experience

1. User loads reading material in RSVP Details screen
2. Instead of inline settings, sees "RSVP Settings" link at bottom
3. Tapping link navigates to Home screen → Settings tab
4. RSVP Display section is now in main Settings alongside other app settings
5. Settings persist globally and apply to all RSVP sessions

**Status**: ✅ Implemented and deployed

## 🧪 Testing

To verify the theme color fixes:

1. Build and install: `./gradlew installDebug`
2. Navigate through all screens (Home, Settings, RSVP Details)
3. Switch between color schemes in Settings (Teal, Blue, Purple, etc.)
4. Toggle dark/light mode
5. Verify:
   - Selected buttons show proper text color on accent backgrounds
   - Unselected buttons show theme-appropriate text color
   - All UI elements update immediately when theme changes
   - Contrast is maintained (text readable on all backgrounds)
