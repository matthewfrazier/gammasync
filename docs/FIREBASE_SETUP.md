# Firebase App Distribution Setup

Firebase App Distribution is the easiest way to distribute test builds to your phone without needing ADB or file transfers.

## Benefits

-  **Direct mobile access** - Install via browser or Firebase app on phone
-  **Email notifications** - Get notified when new builds are available
-  **Build tracking** - See who installed which version
-  **Access control** - Invite-only distribution to testers
-  **Release notes** - Automatically includes commit messages

## Quick Setup (5 minutes)

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Name: `gammasync` (or any name)
4. Disable Google Analytics (optional for app distribution)
5. Click "Create project"

### 2. Add Android App

1. In Firebase console, click "Add app" → Android icon
2. **Android package name:** `com.gammasync` (from `app/build.gradle.kts`)
3. **App nickname:** `GammaSync Debug`
4. Download `google-services.json` → Save to `app/google-services.json`
5. Click "Next" → "Continue to console"

### 3. Enable App Distribution

1. In Firebase console, go to "App Distribution" (left sidebar under "Release & Monitor")
2. Click "Get started"
3. You'll see "No releases yet" - this is normal

### 4. Create Service Account

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your Firebase project
3. Navigate to "IAM & Admin" → "Service Accounts"
4. Click "Create Service Account"
5. **Name:** `github-actions-deployer`
6. **Role:** Select "Firebase App Distribution Admin"
7. Click "Done"
8. Click on the service account you just created
9. Go to "Keys" tab → "Add Key" → "Create new key" → JSON
10. Download the JSON file (keep it secret!)

### 5. Add GitHub Secrets

1. Go to your GitHub repo → Settings → Secrets and variables → Actions
2. Click "New repository secret"

Add these secrets:

**FIREBASE_APP_ID:**
```
1:123456789:android:abcdef123456  (from google-services.json → client → mobilesdk_app_id)
```

**FIREBASE_SERVICE_ACCOUNT:**
```
{
  "type": "service_account",
  "project_id": "your-project-id",
  ...entire contents of downloaded JSON file...
}
```

### 6. Update Workflow

Uncomment the Firebase distribution step in `.github/workflows/distribute-apk.yml`:

```yaml
- name: Distribute to Firebase
  uses: wzieba/Firebase-Distribution-Github-Action@v1
  with:
    appId: ${{ secrets.FIREBASE_APP_ID }}
    serviceCredentialsFileContent: ${{ secrets.FIREBASE_SERVICE_ACCOUNT }}
    groups: testers
    file: app/build/outputs/apk/debug/app-debug.apk
    releaseNotes: file://release_notes.txt
```

### 7. Add Testers

1. In Firebase console → App Distribution → "Testers & Groups"
2. Create a group called `testers`
3. Add testers by email (including yourself)
4. They'll receive an invitation email

### 8. Install Firebase App (on your phone)

**Option A: Firebase App** (Recommended)
1. Install [Firebase App Distribution](https://play.google.com/store/apps/details?id=com.google.firebase.appdistribution) from Play Store
2. Sign in with the email you added as a tester
3. You'll see "GammaSync Debug" listed
4. Tap to install new builds

**Option B: Browser**
1. Check your email for the invitation
2. Click "Get started" → Opens in mobile browser
3. Install directly from browser
4. Bookmark the link for easy access

## Usage

### After Every Commit to Main

1. GitHub Actions automatically builds APK
2. Uploads to Firebase App Distribution
3. Sends email notification to testers
4. You receive email: "New build available: GammaSync Debug"
5. Open Firebase app → Tap "Install"

### For Pull Requests

Currently PR builds go to GitHub artifacts only (requires download). To enable Firebase distribution for PRs:

```yaml
- name: Distribute to Firebase
  if: github.event_name == 'pull_request'
  with:
    groups: pr-reviewers  # Create separate group for PR testing
```

## Alternative: Simpler GitHub Releases

If Firebase setup seems too complex, the `distribute-apk.yml` workflow automatically creates GitHub Releases for main branch commits:

1. Push to main
2. Go to repo → [Releases](https://github.com/matthewfrazier/gammasync/releases)
3. Find latest "Development Build"
4. Download APK directly from browser (works on phone)
5. Install APK

This works immediately without any setup, but:
-  No automatic notifications
-  Manual process (must visit Releases page)
-  Simple, no external service needed
-  Direct download links work on phone browser

## Comparison

| Feature | Firebase | GitHub Releases | GitHub Artifacts |
|---------|----------|-----------------|------------------|
| Phone browser download |  Yes |  Yes |  No (requires login + desktop) |
| Automatic notifications |  Yes |  No |  No |
| Easy to find |  App shows all builds |  Must browse Releases |  Must browse Actions runs |
| Setup complexity |  5 minutes |  None (auto-enabled) |  None (auto-enabled) |
| Cost |  Free (generous quota) |  Free |  Free |
| Tester management |  Built-in |  Manual |  Manual |

## Recommendation

**Start with GitHub Releases** (already enabled) for immediate access, then upgrade to Firebase if you need:
- Multiple testers with automatic notifications
- Build analytics (who installed which version)
- Professional beta testing workflow

## Troubleshooting

### "Invalid service account JSON"
- Make sure you copied the entire JSON file content
- Check for extra spaces or line breaks
- The JSON should start with `{"type": "service_account"...`

### "App not found in Firebase"
- Verify `FIREBASE_APP_ID` matches the one in `google-services.json`
- Make sure you added an Android app (not iOS or Web)

### "No permission to upload"
- Service account needs "Firebase App Distribution Admin" role
- Re-create service account with correct permissions

### "Release notes file not found"
- Workflow generates `release_notes.txt` automatically
- Check workflow logs for file creation step

## Additional Resources

- [Firebase App Distribution Documentation](https://firebase.google.com/docs/app-distribution)
- [GitHub Actions Integration](https://github.com/wzieba/Firebase-Distribution-Github-Action)
- [Managing Testers](https://firebase.google.com/docs/app-distribution/manage-testers)
