# Git Flow Guide

This document describes the branching strategy and development workflow for the CarryZoneMap Android project.

## 🚫 Critical Rules

**NEVER push directly to `master` or `develop` branches!**

These branches are **protected** and require pull requests:
- ✅ All changes must go through pull requests
- ✅ CI checks must pass before merging
- ✅ Code review is required
- ❌ Direct pushes will be rejected by GitHub

## Branch Strategy

We use a **Git Flow** strategy with the following branch types:

```
master (production)
  ↑
  └── release/vX.Y.Z (version updates)
       ↑
       └── develop (integration)
            ↑
            └── feature/*, bugfix/*, etc. (development)
```

### Protected Branches

#### `master` - Production Branch
- **Purpose**: Production-ready code deployed to Google Play
- **Protection**: Branch protection enabled, no direct pushes
- **CI/CD**: Automatic deployment to Closed Testing - Alpha on merge
- **Updates**: Only via pull requests from `release/*` branches
- **Never**: Make changes directly to this branch

#### `develop` - Integration Branch
- **Purpose**: Main development branch where features are integrated
- **Protection**: Branch protection enabled, no direct pushes
- **CI/CD**: Full CI checks (build, tests, lint, detekt, ktlint) on PRs
- **Updates**: Only via pull requests from `feature/*`, `bugfix/*`, etc.
- **Never**: Make changes directly to this branch

### Working Branches

#### `feature/*` - New Features
- **Purpose**: Develop new features
- **Naming**: `feature/description-of-feature`
- **Branch from**: `develop`
- **Merge to**: `develop` (via PR)
- **Examples**:
  - `feature/user-profile`
  - `feature/offline-sync`
  - `feature/map-clustering`

#### `bugfix/*` - Bug Fixes
- **Purpose**: Fix bugs found during development
- **Naming**: `bugfix/description-of-bug`
- **Branch from**: `develop`
- **Merge to**: `develop` (via PR)
- **Examples**:
  - `bugfix/map-crash-on-rotation`
  - `bugfix/login-validation`

#### `hotfix/*` - Production Hotfixes
- **Purpose**: Critical fixes for production issues
- **Naming**: `hotfix/description-of-fix`
- **Branch from**: `master`
- **Merge to**: BOTH `master` AND `develop` (via PRs)
- **Examples**:
  - `hotfix/security-vulnerability`
  - `hotfix/crash-on-startup`

#### `release/*` - Release Preparation
- **Purpose**: Prepare for production release (version bump, release notes, etc.)
- **Naming**: `release/vX.Y.Z`
- **Branch from**: `develop`
- **Merge to**: BOTH `develop` AND `master` (via PRs)
- **Examples**:
  - `release/v0.2.0`
  - `release/v1.0.0`

#### Other Prefixes
- `refactor/*` - Code refactoring
- `docs/*` - Documentation changes
- `test/*` - Test additions/improvements
- `chore/*` - Build/tooling changes

## Workflows

### 1. Developing a New Feature

```bash
# 1. Start from latest develop
git checkout develop
git pull origin develop

# 2. Create feature branch
git checkout -b feature/my-awesome-feature

# 3. Make changes and commit regularly
git add .
git commit -m "feat: Add user profile screen"

# 4. Push to remote
git push origin feature/my-awesome-feature

# 5. Create Pull Request on GitHub
# - Base: develop
# - Compare: feature/my-awesome-feature
# - Wait for CI checks to pass (build, tests, lint, detekt, ktlint)
# - Request review if needed

# 6. After PR is approved and merged, delete the branch
git checkout develop
git pull origin develop
git branch -d feature/my-awesome-feature
git push origin --delete feature/my-awesome-feature
```

### 2. Fixing a Bug

Same workflow as feature development, but use `bugfix/*` prefix:

```bash
git checkout develop
git pull origin develop
git checkout -b bugfix/fix-map-crash
# Make changes, commit, push, create PR to develop
```

### 3. Creating a Release

```bash
# 1. Start from latest develop
git checkout develop
git pull origin develop

# 2. Create release branch
git checkout -b release/v0.3.0

# 3. Update version numbers
# - Edit app/build.gradle.kts (versionName, versionCode)
# - Update CHANGELOG.md or release notes
# - Any other release-specific changes

# 4. Commit version changes
git add .
git commit -m "chore: Bump version to 0.3.0"

# 5. Push to remote
git push origin release/v0.3.0

# 6. Test thoroughly from this branch
# - Run full test suite
# - Manual testing
# - Check all CI passes

# 7. Create PR to develop (merge release changes back)
# - Base: develop
# - Compare: release/v0.3.0
# - Merge when approved

# 8. Create PR to master (deploy to production)
# - Base: master
# - Compare: release/v0.3.0
# - Merge when approved
# - This will trigger automatic deployment to Google Play!

# 9. Tag the release on master
git checkout master
git pull origin master
git tag -a v0.3.0 -m "Release version 0.3.0"
git push origin v0.3.0

# 10. Clean up release branch
git branch -d release/v0.3.0
git push origin --delete release/v0.3.0
```

### 4. Hotfix for Production

```bash
# 1. Start from master (production code)
git checkout master
git pull origin master

# 2. Create hotfix branch
git checkout -b hotfix/fix-critical-crash

# 3. Make the fix
git add .
git commit -m "hotfix: Fix critical crash on startup"

# 4. Update version (patch version)
# - Edit app/build.gradle.kts (e.g., 0.2.0 → 0.2.1)
git add app/build.gradle.kts
git commit -m "chore: Bump version to 0.2.1"

# 5. Push to remote
git push origin hotfix/fix-critical-crash

# 6. Create PR to master (deploy fix)
# - Base: master
# - Compare: hotfix/fix-critical-crash
# - Merge when approved (triggers deployment)

# 7. Create PR to develop (bring fix to development)
# - Base: develop
# - Compare: hotfix/fix-critical-crash
# - Merge when approved

# 8. Tag the hotfix release
git checkout master
git pull origin master
git tag -a v0.2.1 -m "Hotfix version 0.2.1"
git push origin v0.2.1

# 9. Clean up
git branch -d hotfix/fix-critical-crash
git push origin --delete hotfix/fix-critical-crash
```

## CI/CD Integration

### Continuous Integration (CI)

**Triggers**: Pull requests to `develop` or `master`

**Pipeline** (`.github/workflows/ci.yml`):
1. ✅ Build debug APK
2. ✅ Run unit tests
3. ✅ Run Android Lint
4. ✅ Run Detekt (code quality)
5. ✅ Run KtLint (code style)

**Result**: All checks must pass before PR can be merged

### Continuous Deployment (CD)

**Triggers**: Push/merge to `master` branch ONLY

**Pipeline** (`.github/workflows/deploy.yml`):
1. ✅ Build release AAB
2. ✅ Upload to Google Play Closed Testing - Alpha

**Result**: Automatic deployment to testers

## Pull Request Guidelines

### Creating a PR

1. **Title**: Use conventional commit format
   - `feat: Add user authentication`
   - `fix: Resolve map crash on rotation`
   - `refactor: Simplify ViewModel logic`
   - `docs: Update README with setup instructions`

2. **Description**: Include
   - What changes were made
   - Why the changes were necessary
   - How to test the changes
   - Any breaking changes
   - Screenshots (if UI changes)

3. **Checklist**:
   ```markdown
   - [ ] Code builds without errors
   - [ ] All tests pass locally
   - [ ] Added tests for new features
   - [ ] Updated documentation if needed
   - [ ] Followed code style guidelines
   - [ ] No direct commits to develop/master
   ```

### Reviewing a PR

1. **Code Quality**
   - Check for code smells
   - Verify proper error handling
   - Ensure tests are comprehensive
   - Look for security vulnerabilities

2. **Architecture**
   - Follows Clean Architecture principles
   - Proper layer separation (domain/data/presentation)
   - Dependency injection used correctly

3. **Testing**
   - CI checks all pass
   - Manual testing if UI changes

4. **Approval**
   - At least one approval required
   - All comments resolved
   - CI checks passing (green checkmarks)

### Merging Strategies

**For feature/bugfix → develop:**
- **Squash and merge** (recommended) - Clean history
- Or **Merge commit** - Preserve detailed history

**For release → master:**
- **Merge commit** (recommended) - Preserve release history
- Creates a clear merge point for releases

## Troubleshooting

### "Cannot push to protected branch"

✅ **Expected behavior!** You must use pull requests.

```bash
# Instead of:
git push origin develop  # ❌ Will fail

# Do this:
git checkout -b feature/my-feature
git push origin feature/my-feature
# Create PR on GitHub
```

### "PR has conflicts"

Your branch is behind the target branch. Update it:

```bash
# Option 1: Rebase (cleaner history)
git checkout feature/my-feature
git fetch origin
git rebase origin/develop
git push --force-with-lease origin feature/my-feature

# Option 2: Merge (simpler)
git checkout feature/my-feature
git fetch origin
git merge origin/develop
git push origin feature/my-feature
```

### "CI checks failing"

1. Click **Details** on the failed check
2. Read the error logs
3. Fix the issue locally
4. Push the fix - CI re-runs automatically

Common failures:
- **Build errors**: Check compilation errors in logs
- **Test failures**: Run `./gradlew test` locally
- **Lint errors**: Run `./gradlew lint` locally
- **Detekt errors**: Run `./gradlew detekt` locally
- **KtLint errors**: Run `./gradlew ktlintCheck` (or `ktlintFormat` to auto-fix)

### "Forgot to create feature branch"

If you committed to `develop` or `master` locally:

```bash
# Assuming you're on develop with local commits
git branch feature/my-accidental-work
git reset --hard origin/develop  # Reset develop to remote
git checkout feature/my-accidental-work
# Now push feature branch and create PR
```

## Best Practices

### ✅ DO

- Create feature branches from `develop`
- Write descriptive commit messages
- Keep commits focused and atomic
- Rebase your branch on `develop` regularly to avoid conflicts
- Run tests locally before pushing
- Request reviews from teammates
- Delete branches after merging
- Tag releases on `master`

### ❌ DON'T

- **NEVER** push directly to `master` or `develop`
- Don't commit large binary files
- Don't commit secrets (API keys, passwords)
- Don't force-push to shared branches (except your own feature branches)
- Don't merge your own PRs without review (unless solo project)
- Don't leave stale branches unmerged for weeks

## Version Numbering

Follow [Semantic Versioning](https://semver.org/):

**Format**: `MAJOR.MINOR.PATCH` (e.g., `1.2.3`)

- **MAJOR**: Breaking changes, incompatible API changes
- **MINOR**: New features, backwards-compatible
- **PATCH**: Bug fixes, backwards-compatible

**Examples**:
- `0.1.0` → `0.2.0`: Added new feature
- `0.2.0` → `0.2.1`: Fixed bug
- `0.9.0` → `1.0.0`: First stable release

## Quick Reference

```bash
# Start new feature
git checkout develop && git pull origin develop
git checkout -b feature/my-feature

# Update feature branch with latest develop
git checkout feature/my-feature
git fetch origin
git rebase origin/develop

# Start release
git checkout develop && git pull origin develop
git checkout -b release/v1.0.0
# Update version, commit, push, create PRs to develop AND master

# Emergency hotfix
git checkout master && git pull origin master
git checkout -b hotfix/critical-fix
# Fix, update version, commit, push, create PRs to master AND develop
```

## Additional Resources

- [Branch Protection Setup](BRANCH_PROTECTION.md) - How to configure GitHub branch protection
- [CLAUDE.md](CLAUDE.md) - Project architecture and development guidelines
- [CD Setup Guide](CD_SETUP.md) - Continuous deployment configuration
- [Contributing Guide](README.md) - General contribution guidelines

---

**Remember**: The git flow exists to protect production code and ensure quality. Always use pull requests, never push directly to protected branches!
