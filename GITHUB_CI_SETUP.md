# GitHub CI/CD Setup Guide

This document explains how to configure GitHub Actions CI and branch protection for the CarryZoneMap Android project.

## Overview

The CI workflow (`.github/workflows/ci.yml`) automatically:
- Builds the debug APK
- Runs all unit tests (98 tests)
- Runs Android Lint checks
- Uploads test reports and APK artifacts
- Runs on every push to master and every pull request targeting master

## Step 1: Configure GitHub Secrets

The workflow requires Supabase credentials to build the project. You need to add these as GitHub repository secrets.

### Adding Secrets

1. Go to your GitHub repository
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add the following secrets:

| Secret Name | Description | Required |
|-------------|-------------|----------|
| `SUPABASE_URL` | Your Supabase project URL (e.g., `https://your-project.supabase.co`) | Yes |
| `SUPABASE_ANON_KEY` | Your Supabase anonymous API key | Yes |
| `MAPTILER_API_KEY` | MapTiler API key for custom map tiles | No (optional) |

**To get your Supabase credentials:**
- Go to your Supabase dashboard
- Navigate to **Settings** → **API**
- Copy the **Project URL** and **anon/public key**

## Step 2: Enable Branch Protection

To prevent merging PRs with failing tests/lint, configure branch protection rules:

### Setting Up Branch Protection

1. Go to your GitHub repository
2. Click **Settings** → **Branches**
3. Under **Branch protection rules**, click **Add rule**
4. Configure the following:

**Branch name pattern:** `master`

**Protect matching branches - Enable:**
- ✅ **Require a pull request before merging**
  - Optionally: Require approvals (recommended: 1 approval)
  - Optionally: Dismiss stale pull request approvals when new commits are pushed
- ✅ **Require status checks to pass before merging**
  - ✅ **Require branches to be up to date before merging**
  - **Status checks that are required:**
    - Search and select: `Build and Test`
- ✅ **Do not allow bypassing the above settings** (recommended)

5. Click **Create** or **Save changes**

### What This Does

With branch protection enabled:
- Pull requests **cannot be merged** if the CI build fails
- Pull requests **cannot be merged** if tests fail
- Pull requests **cannot be merged** if lint checks fail
- The **Merge** button will be disabled until all checks pass

## Step 3: Verify Setup

1. **Push the workflow file** to your repository:
   ```bash
   git add .github/workflows/ci.yml
   git commit -m "Add GitHub Actions CI workflow"
   git push origin master
   ```

2. **Check the Actions tab** on GitHub to see the workflow run

3. **Test with a pull request:**
   - Create a new branch
   - Make a small change
   - Open a PR to master
   - Verify the CI workflow runs and all checks pass

## Workflow Details

### What Gets Tested

The CI workflow runs:
```bash
./gradlew assembleDebug  # Build debug APK
./gradlew test           # Run 98 unit tests
./gradlew lint           # Run Android Lint
```

### Build Time

Expected CI runtime: **3-5 minutes** per run
- Java setup + dependency caching: ~30 seconds
- Build: ~1-2 minutes
- Unit tests: ~20 seconds (98 tests)
- Lint checks: ~30 seconds

### Artifacts

On **successful builds**, the workflow uploads:
- Debug APK (available for 7 days)

On **failed builds**, the workflow uploads:
- Test reports (HTML)
- Lint reports (HTML)

### Viewing Results

- **Actions tab**: See all workflow runs and logs
- **Pull request checks**: See status of required checks
- **Job summaries**: Each run includes a summary with build status

## Troubleshooting

### Build fails with "SUPABASE_URL not found"
**Solution:** Verify secrets are configured correctly in repository settings.

### Status check "Build and Test" not showing up
**Solution:** Wait for the workflow to run at least once. Status checks only appear after the first workflow run.

### Can't find "Build and Test" when configuring branch protection
**Solution:** The workflow must run at least once before the status check appears in the dropdown.

### Tests pass locally but fail on CI
**Possible causes:**
- Timezone differences (CI runs in UTC)
- Missing dependencies (verify `build.gradle` includes all test dependencies)
- File path issues (use relative paths)

### Gradle build fails with "Could not determine java version"
**Solution:** This is handled by the workflow (Java 21 setup), but verify the `java-version: '21'` is correct in the workflow file.

## Future Enhancements

Consider adding these to the CI workflow in the future:

1. **Code coverage reporting** (JaCoCo)
2. **Static analysis** (Detekt, KtLint - planned for Phase 4)
3. **Instrumentation tests** (requires emulator setup, ~10-15 min runtime)
4. **Release builds** (for production deployments)
5. **Automated versioning** (bump version on merge)
6. **Slack/Discord notifications** (on build failures)

## Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Branch Protection Rules](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
- [Android CI Best Practices](https://developer.android.com/studio/build/building-cmdline)
