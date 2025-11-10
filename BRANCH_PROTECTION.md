# Branch Protection Setup Guide

This guide explains how to protect your `master` branch to prevent accidental releases and enforce code review through pull requests.

## Why Branch Protection?

Branch protection rules ensure that:
- ✅ No one can accidentally push directly to `master`
- ✅ All changes go through pull request review
- ✅ CI tests must pass before merging
- ✅ Code quality is maintained through peer review
- ✅ Accidental deployments to Closed Testing - Alpha are prevented

**Important:** Since your CD pipeline automatically deploys on every push to `master`, protecting this branch is critical to prevent unintended releases.

## Setting Up Branch Protection Rules

### Step 1: Access Branch Protection Settings

1. Go to your GitHub repository
2. Click **Settings** (top navigation)
3. In the left sidebar, click **Branches** (under "Code and automation")
4. Under "Branch protection rules", click **Add rule** or **Add branch protection rule**

### Step 2: Configure Protection for Master Branch

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

### Step 3: Save Protection Rules

1. Scroll to the bottom
2. Click **Create** (or **Save changes** if editing existing rule)

## Verification

After setting up, try to push directly to master:

```bash
git checkout master
echo "test" >> test.txt
git add test.txt
git commit -m "Test direct push"
git push origin master
```

You should see an error like:
```
remote: error: GH006: Protected branch update failed for refs/heads/master.
remote: error: Changes must be made through a pull request.
```

✅ This confirms protection is working correctly!

## Recommended Workflow

### For Regular Development

1. **Create a feature branch**:
   ```bash
   git checkout master
   git pull origin master
   git checkout -b feature/my-new-feature
   ```

2. **Make changes and commit**:
   ```bash
   # Make your changes
   git add .
   git commit -m "feat: Add new feature"
   git push origin feature/my-new-feature
   ```

3. **Create Pull Request**:
   - Go to GitHub repository
   - Click **Pull requests** → **New pull request**
   - Base: `master`, Compare: `feature/my-new-feature`
   - Fill in description
   - Click **Create pull request**

4. **Wait for CI checks to pass**:
   - GitHub Actions will run your tests automatically
   - All checks must pass (green checkmarks)

5. **Request review** (if required):
   - Add reviewers on the right sidebar
   - Wait for approval

6. **Merge when approved**:
   - Click **Squash and merge** (recommended) or **Merge pull request**
   - Delete the feature branch after merging

7. **Automatic deployment**:
   - When PR is merged to `master`, CD pipeline automatically deploys to Closed Testing - Alpha
   - Monitor the deployment in GitHub Actions

### For Hotfixes

Same workflow, but use a `hotfix/` branch:

```bash
git checkout master
git pull origin master
git checkout -b hotfix/critical-bug-fix
# Make fixes
git push origin hotfix/critical-bug-fix
# Create PR, merge when approved
```

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

Your workflow already integrates with this setup:

1. **CI Workflow** (`.github/workflows/ci.yml`):
   - Runs on all PRs to `master`
   - Runs tests, lint, builds debug APK
   - Must pass before PR can be merged

2. **CD Workflow** (`.github/workflows/deploy.yml`):
   - Triggers only when changes are pushed/merged to `master`
   - Automatically deploys to Closed Testing - Alpha
   - Only happens after PR is approved and merged

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

✅ **Recommended Protection Setup:**
- Require pull requests before merging (1 approval minimum)
- Require status checks to pass (CI workflow)
- Require conversation resolution
- Do not allow bypassing (or allow for admins only)
- Require linear history
- Disable force pushes
- Disable branch deletion

✅ **Result:**
- No accidental pushes to `master`
- All changes reviewed before deployment
- CI tests always pass before merge
- Clean git history
- Safe, controlled releases to Closed Testing - Alpha

---

**Next Steps:**
1. Set up branch protection rules following this guide
2. Test by trying to push directly to `master` (should fail)
3. Practice the PR workflow with a small change
4. Update your team on the new process
