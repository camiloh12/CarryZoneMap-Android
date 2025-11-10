# Continuous Deployment Setup Guide

This guide explains how to set up automated deployment to Google Play Store using GitHub Actions.

## Overview

The CD pipeline automatically:
- ✅ Builds a signed release AAB (Android App Bundle)
- ✅ Uploads to Google Play Console
- ✅ Deploys to different tracks (internal/alpha/beta/production)
- ✅ Manages release artifacts and ProGuard mappings

## Prerequisites

1. **Google Play Console account** with an existing app listing
2. **Android keystore** for signing releases
3. **GitHub repository** with Actions enabled

## Setup Steps

### 1. Create a Google Play Service Account

The service account allows GitHub Actions to upload builds to Google Play on your behalf.

#### a) Enable Google Play Android Developer API
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select or create a project
3. Navigate to **APIs & Services** → **Library**
4. Search for "Google Play Android Developer API"
5. Click **Enable**

#### b) Create Service Account
1. In Google Cloud Console, go to **IAM & Admin** → **Service Accounts**
2. Click **Create Service Account**
3. Name: `github-actions-deploy` (or any name)
4. Click **Create and Continue**
5. Skip granting roles (we'll configure in Play Console)
6. Click **Done**

#### c) Create JSON Key
1. Click on the newly created service account
2. Go to **Keys** tab
3. Click **Add Key** → **Create new key**
4. Select **JSON** format
5. Click **Create** - this downloads the JSON key file
6. **IMPORTANT:** Keep this file secure! Never commit it to git.

#### d) Grant Access in Play Console
1. Go to [Google Play Console](https://play.google.com/console/)
2. Select your app
3. Go to **Setup** → **API access**
4. Click **Link** next to your Google Cloud project (if not already linked)
5. Under **Service accounts**, find your service account
6. Click **Manage Play Console permissions**
7. Go to **App permissions** tab
8. Select your app
9. Grant the following permissions:
   - **Releases** → Manage production releases, Manage testing track releases
   - **App access** → View app information
10. Click **Invite user** → **Send invite**

### 2. Prepare Your Android Keystore

#### If you already have a keystore:

Base64 encode it for GitHub secrets:
```bash
# On Linux/macOS
base64 -i your-release-key.keystore | tr -d '\n' > keystore.txt

# On Windows (PowerShell)
certutil -encode your-release-key.keystore keystore.txt
# Then remove the header/footer lines and newlines
```

#### If you need to create a new keystore:

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_KEYSTORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=Your Name, OU=YourOrg, O=YourOrg, L=City, S=State, C=US"

# Then base64 encode it
base64 -i release.keystore | tr -d '\n' > keystore.txt
```

**IMPORTANT:**
- Keep the keystore file secure and backed up
- Never commit the keystore to git
- If you lose the keystore, you cannot update your app on Play Store
- Record the passwords in a secure password manager

### 3. Configure GitHub Secrets

Go to your GitHub repository → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

Add the following secrets:

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Contents of the JSON key file from step 1c | `{"type": "service_account", "project_id": "..."}` |
| `KEYSTORE_BASE64` | Base64-encoded keystore from step 2 | `MIIKWgIBAzCCCh4GCSqGSIb3DQE...` |
| `KEYSTORE_PASSWORD` | Keystore password | `my_keystore_pass` |
| `KEY_ALIAS` | Key alias from keystore | `upload` |
| `KEY_PASSWORD` | Key password | `my_key_pass` |
| `SUPABASE_URL` | Your Supabase project URL | `https://xxx.supabase.co` |
| `SUPABASE_ANON_KEY` | Your Supabase anonymous key | `eyJhbGciOiJIUzI1NiIs...` |
| `MAPTILER_API_KEY` | MapTiler API key (optional) | `your_maptiler_key` |

**To add a secret:**
1. Click **New repository secret**
2. Enter the **Name** (exactly as shown above)
3. Paste the **Value**
4. Click **Add secret**

### 4. Verify Workflow Configuration

The deployment workflow is configured in `.github/workflows/deploy.yml` and triggers on:

- **Push to `main`/`master` branch** → Deploys to **internal** track
- **Version tags** (`v1.0.0`) → Deploys to **production** track
- **Manual trigger** → Choose any track (internal/alpha/beta/production)

## Using the CD Pipeline

### Automatic Deployment (Push to Main)

```bash
# Make changes, commit, and push to main
git checkout main
git add .
git commit -m "feat: Add new feature"
git push origin main

# This automatically deploys to internal track
```

### Release to Production (Using Tags)

```bash
# Create and push a version tag
git tag v1.0.0
git push origin v1.0.0

# This automatically deploys to production track
```

### Manual Deployment

1. Go to your GitHub repository
2. Click **Actions** tab
3. Select **Deploy to Google Play** workflow
4. Click **Run workflow**
5. Choose the track (internal/alpha/beta/production)
6. Click **Run workflow**

## Release Tracks Explained

| Track | Purpose | Who Can Access |
|-------|---------|----------------|
| **Internal** | Quick testing with internal team | Specific email addresses |
| **Alpha** | Early testing with small group | Opt-in testers via link |
| **Beta** | Broader testing before release | Opt-in testers via link |
| **Production** | Public release | All users |

**Recommended flow:**
1. Push to `main` → **Internal** (automated)
2. Test internally, verify everything works
3. Manually promote to **Alpha** or **Beta** via Play Console
4. Gather feedback, fix bugs
5. Create version tag → **Production** (automated)

## Optional: Release Notes

To add release notes (What's New):

1. Create directory structure:
```bash
mkdir -p distribution/whatsnew
```

2. Add text files for each language (use BCP 47 language codes):
```bash
# English
echo "• New feature: POI-based pin creation
• Improved sync performance
• Bug fixes and stability improvements" > distribution/whatsnew/en-US.txt

# Spanish
echo "• Nueva función: creación de pines basada en POI
• Rendimiento de sincronización mejorado
• Corrección de errores y mejoras de estabilidad" > distribution/whatsnew/es-ES.txt
```

3. Commit and push:
```bash
git add distribution/whatsnew/
git commit -m "docs: Add release notes"
git push
```

The workflow will automatically include these notes in the Play Console release.

## Troubleshooting

### "Service account not found" Error

**Solution:** Ensure you've completed step 1d (Grant Access in Play Console) and waited a few minutes for permissions to propagate.

### "Invalid keystore format" Error

**Solution:** Verify your base64-encoded keystore is correct:
```bash
# Test decoding locally
echo "$KEYSTORE_BASE64" | base64 --decode > test.keystore
keytool -list -keystore test.keystore
# If this works, the encoding is correct
```

### "Signature mismatch" Error

**Solution:** You're using a different keystore than the one used to initially upload the app. You must use the original keystore. If lost, you'll need to create a new app listing.

### "Version code must be greater" Error

**Solution:** Update `versionCode` in `app/build.gradle.kts` to a higher number than the current Play Store version.

### Build Fails with ProGuard Errors

**Solution:** Check ProGuard rules in `app/proguard-rules.pro`. Common fixes:
- Keep specific classes: `-keep class com.your.Class { *; }`
- Keep library classes: `-keep class io.github.jan-tennert.supabase.** { *; }`

### Deployment Succeeds but App Not Visible

**Solution:**
- Internal track requires adding test users in Play Console → **Testing** → **Internal testing**
- Alpha/Beta require users to opt-in via testing link
- Production releases may take 1-2 hours to appear (review required for first release)

## Security Best Practices

1. ✅ **Never commit secrets** to git (keystore, passwords, API keys)
2. ✅ **Rotate service account keys** periodically (every 6-12 months)
3. ✅ **Use GitHub environment protection rules** for production deployments
4. ✅ **Back up your keystore** to multiple secure locations
5. ✅ **Use strong passwords** for keystore and keys
6. ✅ **Limit service account permissions** to only what's needed
7. ✅ **Enable 2FA** on your Google Play Console account

## Monitoring Deployments

### View Deployment Status

1. Go to GitHub repository → **Actions** tab
2. Click on the latest workflow run
3. Monitor each step in real-time
4. Check the **Deployment Summary** at the bottom

### View in Play Console

1. Go to [Google Play Console](https://play.google.com/console/)
2. Select your app
3. Go to **Release** → **[Track name]**
4. You'll see the new release with status (Draft/In review/Live)

## Advanced Configuration

### Deploy Different Flavors

If you add product flavors to your app:

```kotlin
// In app/build.gradle.kts
android {
    flavorDimensions += "version"
    productFlavors {
        create("free") {
            dimension = "version"
            applicationIdSuffix = ".free"
        }
        create("pro") {
            dimension = "version"
            applicationIdSuffix = ".pro"
        }
    }
}
```

Update workflow to build specific flavor:
```yaml
- name: Build release AAB
  run: ./gradlew bundleProRelease --stacktrace  # or bundleFreeRelease
```

### Staged Rollout

To release to only a percentage of users:

```yaml
- name: Upload to Google Play
  uses: r0adkll/upload-google-play@v1
  with:
    # ... other config
    track: production
    releaseStatus: inProgress
    userFraction: 0.1  # 10% rollout
```

Then increase the percentage in Play Console as you monitor for issues.

### Environment Protection Rules

For extra safety before production deploys:

1. Go to repository **Settings** → **Environments**
2. Create environment named `production`
3. Add **Required reviewers** (must approve before deploy)
4. Add **Wait timer** (delay before deploy)
5. Update workflow to use environment:

```yaml
jobs:
  deploy:
    environment: production  # Add this line
    runs-on: ubuntu-latest
    # ... rest of job
```

## Resources

- [Google Play Console Help](https://support.google.com/googleplay/android-developer/)
- [Android App Bundle Documentation](https://developer.android.com/guide/app-bundle)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [upload-google-play Action](https://github.com/r0adkll/upload-google-play)

## Next Steps

1. ✅ Complete setup steps above
2. ✅ Test with internal track first
3. ✅ Verify ProGuard mappings are uploaded (for crash reports)
4. ✅ Set up crash reporting (Firebase Crashlytics recommended)
5. ✅ Configure staged rollouts for production releases
6. ✅ Set up environment protection rules for production
7. ✅ Document your release process in team wiki

---

**Need help?** Check the troubleshooting section above or open an issue in this repository.
