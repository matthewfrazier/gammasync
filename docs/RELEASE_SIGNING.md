# Release Signing Configuration

This document describes how to configure release signing for GammaSync to publish to the Google Play Store.

## Overview

The app uses Android App Signing with a release keystore for production builds. The keystore contains a private key used to sign the APK, which is required for Play Store distribution.

## Prerequisites

- Android SDK installed
- Java JDK 17+ (as configured in the project)
- Access to a secure location for storing the keystore file

## Step 1: Generate Release Keystore

**IMPORTANT: Run this command in a secure location outside the project directory**

```bash
keytool -genkey -v \
  -keystore gammasync-release-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias gammasync-release
```

When prompted, provide:
- Keystore password (store securely)
- Key alias: `gammasync-release`
- Key password (store securely)
- Your organization details (name, organizational unit, organization, city, state, country)

## Step 2: Secure Keystore Storage

### Local Development

Create a `local.properties` file in your project root (this file is gitignored):

```properties
RELEASE_KEYSTORE_PATH=/absolute/path/to/gammasync-release-key.jks
RELEASE_KEYSTORE_PASSWORD=your_keystore_password
RELEASE_KEY_ALIAS=gammasync-release
RELEASE_KEY_PASSWORD=your_key_password
```

### CI/CD Environment Variables

Set the following environment variables in your CI/CD system:

- `RELEASE_KEYSTORE_PATH`: Absolute path to keystore file
- `RELEASE_KEYSTORE_PASSWORD`: Keystore password
- `RELEASE_KEY_ALIAS`: Key alias (gammasync-release)
- `RELEASE_KEY_PASSWORD`: Key password

## Step 3: Build Signed Release APK

```bash
./gradlew assembleRelease
```

The signed APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

## Step 4: Verify Signing

Check the APK signing information:

```bash
# Using apksigner (Android SDK)
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk

# Using jarsigner (JDK)
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

## Keystore Backup and Security

### Backup Procedure

1. **Store the keystore file in multiple secure locations:**
   - Encrypted cloud storage (Google Drive, Dropbox with encryption)
   - Physical storage device in a safe location
   - Company password manager or secure vault

2. **Document keystore details securely:**
   - Keystore password
   - Key alias: `gammasync-release`
   - Key password
   - Keystore creation date
   - Key validity period (27+ years from creation)

### Security Best Practices

- **NEVER commit the keystore file to version control**
- **NEVER commit passwords to version control**
- Use different passwords for keystore and key
- Restrict access to keystore file (chmod 600)
- Use strong, unique passwords
- Consider using a hardware security module (HSM) for production

## Play App Signing Setup

Google Play App Signing provides an additional layer of security and allows Google to manage your app signing key.

### Enrollment Steps

1. **Upload your release key to Play Console:**
   - Go to Play Console > App Signing
   - Choose "Use Google Play App Signing"
   - Upload your keystore file or export the certificate

2. **Generate upload key (recommended):**
   ```bash
   keytool -genkey -v \
     -keystore gammasync-upload-key.jks \
     -keyalg RSA \
     -keysize 2048 \
     -validity 10000 \
     -alias gammasync-upload
   ```

3. **Register upload certificate with Google:**
   ```bash
   keytool -export -rfc \
     -keystore gammasync-upload-key.jks \
     -alias gammasync-upload \
     -file upload_certificate.pem
   ```

4. **Update build configuration to use upload key for future releases**

### Benefits of Play App Signing

- Google manages your app signing key
- Lost or compromised upload keys can be reset
- Optimized APK delivery
- Support for APK and Android App Bundle formats

## Troubleshooting

### Common Issues

**Build fails with "Keystore was tampered with":**
- Verify keystore password is correct
- Check keystore file permissions and integrity

**APK not signed:**
- Verify all signing configuration variables are set
- Check keystore file path is absolute and file exists
- Ensure keystore and key passwords are correct

**Play Console rejects APK:**
- Verify APK is signed with the correct key
- Check APK signature using `apksigner verify`
- Ensure version code is higher than previous uploads

### Debug Commands

```bash
# Check if release build is configured
./gradlew :app:assembleRelease --dry-run

# View signing configuration
./gradlew :app:signingReport

# Verify APK contents
unzip -l app/build/outputs/apk/release/app-release.apk
```

## Emergency Key Recovery

If the keystore is lost or corrupted:

1. **Check all backup locations immediately**
2. **Contact Google Play support** if using Play App Signing
3. **Consider app republishing** as last resort (loses user data/reviews)

## References

- [Android App Signing Documentation](https://developer.android.com/studio/publish/app-signing)
- [Google Play App Signing](https://support.google.com/googleplay/android-developer/answer/7384423)
- [Keystore Security Best Practices](https://developer.android.com/studio/publish/app-signing#secure-key)