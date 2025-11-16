# Branch Protection Setup Guide

This guide explains how to protect your `master` and `develop` branches to prevent accidental releases and enforce code review through pull requests.

## Why Branch Protection?

Branch protection rules ensure that:
- ✅ No one can accidentally push directly to `master` or `develop`
- ✅ All changes go through pull request review
- ✅ CI tests must pass before merging
- ✅ Code quality is maintained through peer review
- ✅ Accidental deployments to Closed Testing - Alpha are prevented (master)
- ✅ Integration branch remains stable (develop)

**Important:**
- **`master`**: CD pipeline automatically deploys on every merge - protection prevents unintended releases
- **`develop`**: Main integration branch - protection ensures only tested code is integrated

## Setting Up Branch Protection Rules

### Step 1: Access Branch Protection Settings

1. Go to your GitHub repository
2. Click **Settings** (top navigation)
3. In the left sidebar, click **Branches** (under "Code and automation")
4. Under "Branch protection rules", click **Add rule** or **Add branch protection rule**

### Step 2: Configure Protection Rules

You need to create **TWO separate protection rules** - one for `master` and one for `develop`.

---

## Protection Rule #1: Master Branch

#### Basic Settings

1. **Branch name pattern**: Enter `master` (or `main` if that's your default branch)

#### Required Settings (Recommended)

Check the following options:

**Require a pull request before merging**
- ✅ **Enable this** - Enforces PR workflow
- Set **Required approvals**: `1` (at least one reviewer must approve)
- Optional sub-settings:
  - ✅ **Dismiss stale pull request approvals when new commits are pushed** - Re-review required after changes
  - ✅ **Require review from Code Owners** - If you have a CODEOWNERS file
  - ⬜ **Require approval of the most recent reviewable push** - Stricter but can slow down development

**Require status checks to pass before merging**
- ✅ **Enable this** - CI must pass before merge
- ✅ **Require branches to be up to date before merging** - Prevent merge conflicts
- In the search box, add required status checks:
  - Search for `Build and Test` (from your CI workflow)
  - Click it to add as required check
  - Any other checks you want to require

**Require conversation resolution before merging**
- ✅ **Enable this** - All PR comments must be resolved

**Do not allow bypassing the above settings**
- ✅ **Enable this** - Even admins must follow the rules
- Alternative: Leave unchecked if you need emergency bypass capability

#### Additional Recommended Settings

**Lock branch**
- ⬜ **Leave disabled** - You still need to merge PRs

**Require linear history**
- ✅ **Enable this** - Keeps git history clean (requires rebase or squash merge)

**Require deployments to succeed before merging**
- ⬜ **Leave disabled** - Not needed for your workflow

**Allow force pushes**
- ⬜ **Leave disabled** - Prevents force pushes that could break history

**Allow deletions**
- ⬜ **Leave disabled** - Prevents accidental branch deletion

### Step 3: Save Master Protection Rule

1. Scroll to the bottom
2. Click **Create** (or **Save changes** if editing existing rule)

---

## Protection Rule #2: Develop Branch

Now create a second protection rule for the `develop` branch.

#### Basic Settings

1. Click **Add rule** again (to create a second rule)
2. **Branch name pattern**: Enter `develop`

#### Required Settings (Recommended)

Check the following options:

**Require a pull request before merging**
- ✅ **Enable this** - Enforces PR workflow from feature branches
- Set **Required approvals**: `1` (at least one reviewer must approve)
- Optional sub-settings:
  - ✅ **Dismiss stale pull request approvals when new commits are pushed** - Re-review required after changes
  - ✅ **Require review from Code Owners** - If you have a CODEOWNERS file
  - ⬜ **Require approval of the most recent reviewable push** - Stricter but can slow down development

**Require status checks to pass before merging**
- ✅ **Enable this** - CI must pass before merge
- ✅ **Require branches to be up to date before merging** - Prevent merge conflicts
- In the search box, add required status checks:
  - Search for `Build and Test` (from your CI workflow)
  - Click it to add as required check
  - This ensures all PRs to `develop` run: build + tests + lint + detekt + ktlint

**Require conversation resolution before merging**
- ✅ **Enable this** - All PR comments must be resolved

**Do not allow bypassing the above settings**
- ✅ **Enable this** - Even admins must follow the rules
- Alternative: Leave unchecked if you need emergency bypass capability

#### Additional Recommended Settings

**Require linear history**
- ✅ **Enable this** - Keeps git history clean (requires rebase or squash merge)

**Allow force pushes**
- ⬜ **Leave disabled** - Prevents force pushes that could break history

**Allow deletions**
- ⬜ **Leave disabled** - Prevents accidental branch deletion

### Step 4: Save Develop Protection Rule

1. Scroll to the bottom
2. Click **Create**

---

## Verification

After setting up both protection rules, verify they're working:

### Test Master Protection

```bash
git checkout master
git pull origin master
echo "test" >> test.txt
git add test.txt
git commit -m "Test direct push to master"
git push origin master
```

You should see an error like:
```
remote: error: GH006: Protected branch update failed for refs/heads/master.
remote: error: Changes must be made through a pull request.
```

✅ Master protection is working!

### Test Develop Protection

```bash
git checkout develop
git pull origin develop
echo "test" >> test.txt
git add test.txt
git commit -m "Test direct push to develop"
git push origin develop
```

You should see the same error:
```
remote: error: GH006: Protected branch update failed for refs/heads/develop.
remote: error: Changes must be made through a pull request.
```

✅ Develop protection is working!

### Clean Up Test

If you created test commits, reset your local branches:

```bash
git checkout master
git reset --hard origin/master
git checkout develop
git reset --hard origin/develop
```

## Recommended Workflow

**⚠️ Important: Never push directly to `master` or `develop`!**

See [GIT_FLOW.md](GIT_FLOW.md) for complete git flow documentation. Quick overview:

### For New Features

1. **Create a feature branch from develop**:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feature/my-new-feature
   ```

2. **Make changes and commit**:
   ```bash
   # Make your changes
   git add .
   git commit -m "feat: Add new feature"
   git push origin feature/my-new-feature
   ```

3. **Create Pull Request to develop**:
   - Go to GitHub repository
   - Click **Pull requests** → **New pull request**
   - **Base: `develop`**, Compare: `feature/my-new-feature`
   - Fill in description
   - Click **Create pull request**

4. **Wait for CI checks to pass**:
   - GitHub Actions will run: build + tests + lint + detekt + ktlint
   - All checks must pass (green checkmarks)

5. **Request review** (if required):
   - Add reviewers on the right sidebar
   - Wait for approval

6. **Merge when approved**:
   - Click **Squash and merge** (recommended) or **Merge pull request**
   - Delete the feature branch after merging

### For Releases

1. **Create release branch from develop**:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b release/v1.0.0
   ```

2. **Update version numbers and release notes**:
   ```bash
   # Edit app/build.gradle.kts (versionName, versionCode)
   # Update CHANGELOG.md
   git add .
   git commit -m "chore: Bump version to 1.0.0"
   git push origin release/v1.0.0
   ```

3. **Create PR to develop** (merge release changes back):
   - Base: `develop`, Compare: `release/v1.0.0`
   - Merge when approved

4. **Create PR to master** (deploy to production):
   - Base: `master`, Compare: `release/v1.0.0`
   - Merge when approved
   - **This triggers automatic deployment to Google Play Closed Testing - Alpha!**

5. **Tag the release**:
   ```bash
   git checkout master
   git pull origin master
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

### For Hotfixes

1. **Create hotfix branch from master**:
   ```bash
   git checkout master
   git pull origin master
   git checkout -b hotfix/critical-bug-fix
   ```

2. **Make fixes and update version**:
   ```bash
   # Make fixes
   git add .
   git commit -m "hotfix: Fix critical bug"
   # Update version (patch bump)
   git add app/build.gradle.kts
   git commit -m "chore: Bump version to 1.0.1"
   git push origin hotfix/critical-bug-fix
   ```

3. **Create PR to master** (deploy fix):
   - Base: `master`, Compare: `hotfix/critical-bug-fix`
   - Merge when approved (triggers deployment)

4. **Create PR to develop** (bring fix back):
   - Base: `develop`, Compare: `hotfix/critical-bug-fix`
   - Merge when approved

## Branch Naming Conventions

Use clear branch naming to organize work:

- `feature/` - New features (e.g., `feature/user-authentication`)
- `bugfix/` - Bug fixes (e.g., `bugfix/fix-map-crash`)
- `hotfix/` - Critical production fixes (e.g., `hotfix/security-patch`)
- `refactor/` - Code refactoring (e.g., `refactor/cleanup-viewmodel`)
- `docs/` - Documentation changes (e.g., `docs/update-readme`)
- `test/` - Test additions/changes (e.g., `test/add-repository-tests`)

## Emergency Override (Admins Only)

If you absolutely need to bypass protection rules in an emergency:

1. Ensure "Do not allow bypassing the above settings" is **unchecked** in branch protection
2. As an admin, you can temporarily disable protection:
   - Go to **Settings** → **Branches**
   - Find the rule, click **Edit**
   - Click **Delete** (temporarily)
   - Make your emergency push
   - **Immediately recreate the protection rule**

**⚠️ Warning:** Only use this for critical emergencies. Document why you bypassed protection.

## Integrating with CI/CD

Your workflow integrates with branch protection:

1. **CI Workflow** (`.github/workflows/ci.yml`):
   - **Triggers**: PRs to `develop` OR `master`
   - **Checks**: Build + Unit Tests + Lint + Detekt + KtLint
   - **Result**: All checks must pass before PR can be merged
   - **Purpose**: Ensures code quality before integration

2. **CD Workflow** (`.github/workflows/deploy.yml`):
   - **Triggers**: Pushes/merges to `master` branch ONLY
   - **Actions**: Build release AAB, upload to Google Play Closed Testing - Alpha
   - **Result**: Automatic deployment to testers
   - **Purpose**: Automated releases when code is merged to production

**Flow**:
- Feature → Develop PR: CI runs (build/test/lint/detekt/ktlint)
- Release → Master PR: CI runs, then CD deploys after merge

## Additional Security: CODEOWNERS File

For extra control, create a `CODEOWNERS` file to require specific reviewers:

```bash
# Create CODEOWNERS file
cat > .github/CODEOWNERS << 'EOF'
# Default owners for everything in the repo
* @your-github-username

# Specific ownership for critical files
/.github/workflows/* @your-github-username
/app/build.gradle.kts @your-github-username
/gradle.properties @your-github-username

# Require multiple reviewers for sensitive areas
/app/src/main/java/com/carryzonemap/app/data/remote/* @your-github-username @teammate
EOF

git add .github/CODEOWNERS
git commit -m "docs: Add CODEOWNERS file"
git push origin master
```

Then enable **Require review from Code Owners** in branch protection settings.

## Troubleshooting

### "Status checks not found"

If the status check doesn't appear when searching:
1. Merge at least one PR to `master` first
2. The CI workflow must run at least once
3. The check name must exactly match the job name in your workflow

### "Cannot merge due to required status check"

If CI fails:
1. Click **Details** next to the failed check
2. Review the error logs
3. Fix the issue in your branch
4. Push the fix - CI will re-run automatically

### "PR conflicts with base branch"

If your branch is behind `master`:
1. Update your branch:
   ```bash
   git checkout feature/my-feature
   git fetch origin
   git rebase origin/master
   # OR: git merge origin/master
   git push --force-with-lease origin feature/my-feature
   ```

## Summary

✅ **Recommended Protection Setup (for both `master` and `develop`):**
- Require pull requests before merging (1 approval minimum)
- Require status checks to pass (CI workflow: build + tests + lint + detekt + ktlint)
- Require conversation resolution
- Do not allow bypassing (or allow for admins only)
- Require linear history
- Disable force pushes
- Disable branch deletion

✅ **Result:**
- ✅ No accidental pushes to `master` or `develop`
- ✅ All changes reviewed before integration/deployment
- ✅ CI checks always pass before merge
- ✅ Clean git history with atomic commits
- ✅ Safe, controlled releases to Google Play

**Branch Purposes:**
- **`master`**: Production code, triggers CD to Google Play
- **`develop`**: Integration branch for features
- **Feature branches**: Development work (feature/*, bugfix/*, etc.)
- **Release branches**: Version preparation before production

---

**Next Steps:**
1. Set up protection rules for **both** `master` and `develop` following this guide
2. Test by trying to push directly to each branch (should fail)
3. Practice the git flow workflow (see [GIT_FLOW.md](GIT_FLOW.md))
4. Update your team on the new process

**See Also:**
- [GIT_FLOW.md](GIT_FLOW.md) - Complete git flow documentation
- [CD_SETUP.md](CD_SETUP.md) - Continuous deployment setup
- [CLAUDE.md](CLAUDE.md) - Project architecture and development guidelines
