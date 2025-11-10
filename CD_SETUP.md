# Continuous Deployment Setup Guide (Closed Testing - Alpha)

This guide explains how to set up automated deployment to Google Play Store **Closed Testing - Alpha track** using GitHub Actions.

> **Note:** This pipeline is configured for the **Closed Testing - Alpha** track only. Production deployments are done manually via Google Play Console when ready.

## Overview

The CD pipeline automatically:
- ✅ Builds a signed release AAB (Android App Bundle)
- ✅ Uploads to Google Play Console
- ✅ Deploys to **Closed Testing - Alpha** track
- ✅ Manages release artifacts and ProGuard mappings

## Understanding Google Play Tracks

In Google Play Console, the release tracks are organized as:
- **Internal testing** (track: `internal`) - Quick testing with internal team
- **Closed testing** - Testing with specific users
  - **Alpha** (track: `alpha`) ← **This is what we're using**
  - **Beta** (track: `beta`)
- **Open testing** - Public testing program
- **Production** - Public release

This pipeline targets the **Closed Testing - Alpha** track specifically.

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

### 4. Set Up Branch Protection (Critical for Safety)

**⚠️ IMPORTANT:** Since the CD pipeline automatically deploys on every push to `master`, you should protect this branch to prevent accidental releases.

**Quick Setup:**
1. Go to repository **Settings** → **Branches**
2. Click **Add branch protection rule**
3. Branch name pattern: `master`
4. Enable:
   - ✅ **Require a pull request before merging** (with 1 approval)
   - ✅ **Require status checks to pass before merging**
   - ✅ **Require conversation resolution before merging**
5. Click **Create**

**For detailed instructions and recommended settings, see [BRANCH_PROTECTION.md](BRANCH_PROTECTION.md)**

This ensures:
- No accidental direct pushes to `master`
- All changes go through pull request review
- CI tests must pass before deployment
- Prevents unintended releases to Closed Testing - Alpha

### 5. Verify Workflow Configuration

The deployment workflow is configured in `.github/workflows/deploy.yml` and triggers on:

- **Push to `main`/`master` branch** → Deploys to **Closed Testing - Alpha** track
- **Manual trigger** → Deploys to **Closed Testing - Alpha** track

> **Production releases:** When ready for production, you'll manually promote the alpha build via Google Play Console.

## Using the CD Pipeline

### Recommended Workflow (with Branch Protection)

If you've set up branch protection (recommended), use this pull request workflow:

1. **Create a feature branch**:
   ```bash
   git checkout master
   git pull origin master
   git checkout -b feature/my-new-feature
   ```

2. **Make changes and push**:
   ```bash
   # Make your changes
   git add .
   git commit -m "feat: Add new feature"
   git push origin feature/my-new-feature
   ```

3. **Create Pull Request on GitHub**:
   - Go to your repository on GitHub
   - Click **Pull requests** → **New pull request**
   - Base: `master`, Compare: `feature/my-new-feature`
   - Fill in description
   - Click **Create pull request**

4. **Wait for CI to pass**:
   - GitHub Actions will automatically run tests
   - All checks must pass (green checkmarks)

5. **Get approval and merge**:
   - Request review if required
   - Once approved, click **Squash and merge** or **Merge pull request**
   - Delete the feature branch

6. **Automatic deployment**:
   - When PR merges to `master`, CD pipeline automatically triggers
   - Builds and deploys to **Closed Testing - Alpha**
   - Monitor progress in **Actions** tab

### Direct Push (if branch protection not enabled)

```bash
# Make changes, commit, and push to master
git checkout master
git add .
git commit -m "feat: Add new feature"
git push origin master

# This automatically deploys to Closed Testing - Alpha track
```

**⚠️ Warning:** Direct pushes to `master` immediately trigger deployment. Use branch protection to prevent this.

### Manual Deployment

1. Go to your GitHub repository
2. Click **Actions** tab
3. Select **Deploy to Google Play (Closed Testing - Alpha)** workflow
4. Click **Run workflow**
5. Click **Run workflow** again to confirm

This deploys the current `master` branch to **Closed Testing - Alpha** track.

### Add Testers to Closed Testing - Alpha

After deployment, you need to add testers who can access your app:

1. Go to [Google Play Console](https://play.google.com/console/)
2. Select your app
3. Navigate to **Testing** → **Closed testing** → **Alpha**
4. Go to the **Testers** tab
5. Choose one of these methods:
   - **Email list**: Add testers by email address
   - **Create list**: Create a reusable list of testers
   - **Copy testing link**: Get a link to share with testers

Testers will receive an email invite or can use the testing link to opt-in.

### Promote to Production (Manual via Play Console)

When your app is ready for production release:

1. Go to [Google Play Console](https://play.google.com/console/)
2. Select your app
3. Navigate to **Testing** → **Closed testing** → **Alpha**
4. Find the release you want to promote
5. Click **Promote release** → **Production**
6. Review release details and release notes
7. Click **Start rollout to Production**

This ensures you have full control over production releases and can thoroughly test before going live.

## Release Tracks Explained

| Track | Google Play Console Name | Purpose | Who Can Access |
|-------|--------------------------|---------|----------------|
| **Internal** | Internal testing | Quick testing with internal team | Up to 100 specific email addresses |
| **Alpha** | Closed testing - Alpha | Early testing with selected users | Opt-in testers via email or link |
| **Beta** | Closed testing - Beta | Broader testing before release | Opt-in testers via email or link |
| **Production** | Production | Public release | All users on Google Play |

**Your Current Setup:**
- ✅ Automated deployment to **Closed Testing - Alpha** track
- ✅ Manual promotion to Production when ready

**Recommended workflow:**
1. Push to `main` → **Closed Testing - Alpha** (automated)
2. Add testers via email or testing link
3. Gather feedback, fix bugs, iterate
4. When stable and ready, manually promote to **Production** via Play Console

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

1. ✅ **Protect your master branch** - Prevent accidental releases (see [BRANCH_PROTECTION.md](BRANCH_PROTECTION.md))
   - Require pull requests before merging
   - Require CI status checks to pass
   - Require code review approvals
2. ✅ **Never commit secrets** to git (keystore, passwords, API keys)
3. ✅ **Rotate service account keys** periodically (every 6-12 months)
4. ✅ **Use GitHub environment protection rules** for production deployments
5. ✅ **Back up your keystore** to multiple secure locations
6. ✅ **Use strong passwords** for keystore and keys
7. ✅ **Limit service account permissions** to only what's needed
8. ✅ **Enable 2FA** on your Google Play Console account

## Monitoring Deployments

### View Deployment Status

1. Go to GitHub repository → **Actions** tab
2. Click on the latest workflow run
3. Monitor each step in real-time
4. Check the **Deployment Summary** at the bottom

### View in Play Console

1. Go to [Google Play Console](https://play.google.com/console/)
2. Select your app
3. Go to **Testing** → **Closed testing** → **Alpha**
4. You'll see the new release with status (Draft/In review/Live)

## Advanced Configuration

### Switch to a Different Testing Track

If you want to deploy to a different track (e.g., Internal or Beta instead of Alpha):

1. Edit `.github/workflows/deploy.yml`
2. Find the line with `track: alpha`
3. Change to your desired track:
   - `track: internal` - For Internal testing (up to 100 users)
   - `track: beta` - For Closed testing - Beta
   - `track: alpha` - For Closed testing - Alpha (current)

Example:
```yaml
- name: Upload to Google Play
  uses: r0adkll/upload-google-play@v1
  with:
    # ... other config
    track: beta  # Changed from alpha to beta
```

### Add Multiple Track Options (Manual Selection)

To choose the track manually when running the workflow:

1. Edit `.github/workflows/deploy.yml`
2. Add this to the `workflow_dispatch` section:
```yaml
workflow_dispatch:
  inputs:
    track:
      description: 'Testing track'
      required: true
      type: choice
      default: 'alpha'
      options:
        - internal
        - alpha
        - beta
```

3. Update the track configuration:
```yaml
- name: Upload to Google Play
  uses: r0adkll/upload-google-play@v1
  with:
    # ... other config
    track: ${{ github.event.inputs.track || 'alpha' }}  # Use input or default to alpha
```

### Enable Production Deployments (When Ready)

**⚠️ Important:** Only enable automated production deployments after:
- Thorough testing in all testing tracks
- Setting up staged rollouts (see below)
- Configuring environment protection rules
- Establishing a robust testing and QA process

When ready, you can add `production` as an option and configure tag-based releases similar to the manual track selection above.

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

1. ✅ **Complete setup steps above** (service account, keystore, secrets)
2. ✅ **Set up branch protection** (see [BRANCH_PROTECTION.md](BRANCH_PROTECTION.md)) - Critical!
3. ✅ **Test deployment** with Closed Testing - Alpha track
4. ✅ **Add testers** in Google Play Console
5. ✅ **Verify ProGuard mappings** are uploaded (for crash reports)
6. ✅ **Set up crash reporting** (Firebase Crashlytics recommended)
7. ✅ **Document your release process** for your team
8. ✅ **Configure staged rollouts** for production releases (when ready)

---

**Need help?** Check the troubleshooting section above or open an issue in this repository.
