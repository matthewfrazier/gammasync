# GammaSync App Icon Specifications (512x512px)

## Design Requirements
- **Dimensions**: 512x512 pixels exactly
- **Format**: PNG with no transparency/alpha channel
- **Safe area**: Keep important elements within 416x416px (81% of canvas) to account for system masking
- **No text**: Google Play policy prohibits text in app icons
- **Adaptive**: Consider how icon will look with various system masks (circle, rounded square, etc.)

## Visual Concept
Based on the app's purpose (40Hz gamma brain entrainment therapy):

### Core Symbol: Stylized Brain with Gamma Waves
- **Central element**: Modern, abstract brain silhouette
- **Wave pattern**: Subtle 40Hz sine wave overlay or emanating pattern
- **Color scheme**: Teal primary (#26A69A from app theme) with supporting colors

### Alternative Concepts:
1. **Concentric circles**: Representing neural oscillations/brainwaves
2. **Stylized neuron**: Modern, geometric neuron with synaptic connections
3. **Frequency visualization**: Abstract waveform/spectrum visualization

## Color Palette
- **Primary**: Teal #26A69A (from app's accent_teal)
- **Secondary**: Deep blue #1A237E or purple #4A148C
- **Gradient**: Subtle gradient from teal to blue/purple
- **Background**: White or very light gray (#F8F9FA)

## Design Templates

### Figma Template
Use this exact template: [GammaSync App Icon Template](https://www.figma.com/community/file/1024230806574823267)
- Search for "medical app icon brain" in Figma Community
- Look for templates with brain/neural themes
- Customize colors to match GammaSync palette

### Canva Template
1. Go to [Canva App Icon Templates](https://www.canva.com/app-icons/templates/)
2. Search for "brain health" or "medical app"
3. Select template ID: "Medical Brain Icon - Blue Gradient" (or similar)
4. Customize colors to teal (#26A69A) theme
5. Remove any text elements
6. Export as PNG at 512x512px

## Alternative: AI Generation Prompt
If using AI tools like Midjourney or Dall-E:

```
App icon design, 512x512px, modern minimal brain silhouette with subtle gamma wave pattern, teal and blue gradient (#26A69A, #1A237E), medical/health theme, no text, clean professional style, white background, suitable for mobile app icon
```

## Technical Notes
- **Color profile**: sRGB
- **Compression**: Optimize for file size while maintaining quality
- **Testing**: Preview at multiple sizes (16px, 24px, 48px, 96px) to ensure readability
- **Avoid**: Gradients that become muddy at small sizes, too much detail, thin lines that disappear when scaled down

## File Naming
Save as: `icon_512.png`