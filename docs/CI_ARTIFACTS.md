# CI Artifact Best Practices

This document outlines best practices for APK artifacts in GitHub Actions CI.

## Current Setup

The `smoketest.yml` workflow automatically builds and uploads APK artifacts for every push and PR.

### Accessing Artifacts

**Via GitHub UI:**
1. Go to Actions tab
2. Click on the workflow run
3. Scroll to "Artifacts" section
4. Download `gammasync-debug-<PR#>` or `gammasync-debug-<SHA>`

**Via GitHub CLI:**
```bash
# List artifacts from latest run
gh run list --workflow=smoketest.yml --limit 1

# Download artifact from specific run
gh run download <run-id> -n gammasync-debug-<PR#>

# Install directly via ADB
gh run download <run-id> -n gammasync-debug-<PR#> && \
  adb install -r app-debug.apk
```

**Via PR Comment:**
On pull requests, a bot comment provides a direct download link and installation instructions.

## Artifact Types

### Debug APKs (Current)
- **When:** Every push, every PR
- **Purpose:** Manual testing of bug fixes and features
- **Retention:** 30 days
- **Naming:** `gammasync-debug-<PR#>` or `gammasync-debug-<SHA>`
- **Signing:** Debug keystore (auto-generated)
- **ProGuard:** Disabled (easier debugging)

### Release APKs (Future)
- **When:** Tags, release branches
- **Purpose:** Pre-release candidates, beta testing
- **Retention:** 90 days
- **Naming:** `gammasync-release-v<version>`
- **Signing:** Release keystore (from secrets)
- **ProGuard:** Enabled (code obfuscation)

## Best Practices

### 1. Artifact Naming
✅ **Good:** Include context in artifact names
```yaml
name: gammasync-debug-${{ github.event.pull_request.number || github.sha }}
```

❌ **Bad:** Generic names that conflict
```yaml
name: debug-apk  # Overwrites previous artifacts
```

### 2. Retention Policies
- **Debug APKs:** 30 days (sufficient for PR review cycle)
- **Test Results:** 14 days (reference for failures)
- **Release APKs:** 90 days (align with release schedule)

GitHub has artifact storage limits (500 MB free tier), so aggressive retention helps.

### 3. Conditional Upload
Always upload APKs even if tests fail (for debugging):
```yaml
if: success() || failure()  # Upload unless workflow cancelled
```

### 4. File Validation
Ensure APK was actually built:
```yaml
if-no-files-found: error  # Fail workflow if APK missing
```

### 5. PR Comments
Auto-comment on PRs with download links and testing instructions. This:
- Makes artifacts discoverable without navigating to Actions tab
- Provides installation commands (copy-paste ready)
- Includes testing checklist

### 6. Version Information
Include version info in APK for traceability:
```kotlin
// build.gradle.kts
versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
versionName = "0.1.0-${System.getenv("GITHUB_SHA")?.take(7) ?: "local"}"
```

## Advanced: Multiple Build Variants

For different build flavors (e.g., free vs pro, development vs staging):

```yaml
- name: Build All Variants
  run: ./gradlew assembleDebug assembleDevelopmentDebug assembleStagingDebug

- name: Upload Debug APKs
  uses: actions/upload-artifact@v4
  with:
    name: all-debug-apks-${{ github.sha }}
    path: |
      app/build/outputs/apk/debug/*.apk
      app/build/outputs/apk/development/debug/*.apk
      app/build/outputs/apk/staging/debug/*.apk
```

## Security Considerations

### Debug APKs
- ✅ Safe to make public (debug signing key is not secret)
- ✅ Can be uploaded as workflow artifacts
- ⚠️ Do NOT distribute publicly (not for end users)

### Release APKs
- ❌ Never commit release keystore to repo
- ✅ Store keystore in GitHub Secrets (base64 encoded)
- ✅ Sign APKs in CI using secrets
- ⚠️ Consider using Play App Signing (Google manages key)

## Example: Release Build Workflow

```yaml
# .github/workflows/release.yml
name: Release Build

on:
  push:
    tags:
      - 'v*'

jobs:
  build-release:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Decode Keystore
        run: |
          echo "${{ secrets.RELEASE_KEYSTORE_BASE64 }}" | base64 -d > release.keystore

      - name: Build Release APK
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew assembleRelease

      - name: Upload Release APK
        uses: actions/upload-artifact@v4
        with:
          name: gammasync-release-${{ github.ref_name }}
          path: app/build/outputs/apk/release/*.apk
          retention-days: 90

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          files: app/build/outputs/apk/release/*.apk
          generate_release_notes: true
```

## Troubleshooting

### "No artifacts found"
- Check workflow logs for build errors
- Verify APK path matches actual output directory
- Ensure `assembleDebug` completed successfully

### "Artifact expired"
- Artifacts auto-delete after retention period
- Re-run workflow from Actions UI to regenerate
- Consider increasing retention for important builds

### "APK too large"
- GitHub artifact size limit: 2 GB per artifact
- Check APK size: `ls -lh app/build/outputs/apk/debug/`
- Enable ProGuard/R8 to reduce size:
  ```kotlin
  buildTypes {
      debug {
          isMinifyEnabled = true
          proguardFiles("proguard-rules.pro")
      }
  }
  ```

## Future Enhancements

- [ ] Add workflow for release builds with signing
- [ ] Integrate with Firebase App Distribution for beta testing
- [ ] Add APK size tracking (fail if >50MB increase)
- [ ] Generate QR code for easy mobile download
- [ ] Automated UI testing on Firebase Test Lab before artifact upload
