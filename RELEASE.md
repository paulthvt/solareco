# Release Process Documentation

## Overview

This project uses [semantic-release](https://semantic-release.gitbook.io/) to automate version management and release creation. The release process is triggered automatically when code is merged to `main` or `develop` branches.

## How It Works

### Version Management

The project follows [Semantic Versioning](https://semver.org/) (MAJOR.MINOR.PATCH):
- **MAJOR**: Breaking changes
- **MINOR**: New features (backwards compatible)
- **PATCH**: Bug fixes and minor changes

### Android Version Code

Android requires both a `versionName` (e.g., "1.2.3") and a numeric `versionCode`. The version code is automatically calculated from the semantic version using the formula:

```
versionCode = MAJOR * 1000000 + MINOR * 1000 + PATCH
```

**Examples:**
- `1.0.0` → versionCode: `1000000`
- `1.2.3` → versionCode: `1002003`
- `2.5.10` → versionCode: `2005010`

This allows up to version `99.999.999` before requiring changes.

### Commit Convention

The project uses [Conventional Commits](https://www.conventionalcommits.org/) to determine version bumps:

- `feat:` → MINOR version bump (e.g., 1.0.0 → 1.1.0)
- `fix:` → PATCH version bump (e.g., 1.0.0 → 1.0.1)
- `perf:` → PATCH version bump
- `refactor:` → PATCH version bump
- `BREAKING CHANGE:` → MAJOR version bump (e.g., 1.0.0 → 2.0.0)

**Examples:**
```bash
git commit -m "feat: add user authentication"
git commit -m "fix: resolve crash on startup"
git commit -m "feat!: migrate to new API" # Breaking change
git commit -m "refactor: optimize battery usage"
```

## Release Process Flow

When you merge to `main` or `develop`:

1. **Analyze Commits**: semantic-release analyzes commits since the last release
2. **Determine Version**: Calculates the next version based on commit types
3. **Bump Version**: Updates `version` in `composeApp/build.gradle.kts`
4. **Generate Changelog**: Creates/updates `CHANGELOG.md`
5. **Commit Changes**: Commits version bump and changelog
6. **Build Artifacts**: Runs `scripts/release.sh` to build:
   - Android APK (`composeApp/build/outputs/apk/release/`)
   - Android Bundle (`composeApp/build/outputs/bundle/release/`)
   - iOS Framework (`build/ios/Release-iphoneos/`)
7. **Create Release**: Creates a GitHub release with:
   - Release notes
   - Attached APK and AAB files
8. **Archive Artifacts**: Uploads artifacts to GitHub Actions

## Branches

- **`main`**: Production releases (e.g., `1.2.3`)
- **`develop`**: Pre-releases (e.g., `1.2.3-beta.1`)

## GitHub Secrets Required

Configure these secrets in your GitHub repository settings:

### Semantic Release (Optional)
**For personal repositories:** No secrets needed - uses built-in `GITHUB_TOKEN`

**For enterprise/organization only:**
- `SEMANTIC_RELEASE_APP_ID`: GitHub App ID for authentication
- `SEMANTIC_RELEASE_APP_PRIVATE_KEY`: GitHub App private key

### Android Signing (Optional)
- `RELEASE_STORE_FILE`: Path to keystore file
- `RELEASE_STORE_PASSWORD`: Keystore password
- `RELEASE_KEY_ALIAS`: Key alias
- `RELEASE_KEY_PASSWORD`: Key password

### Firebase (Optional)
- `GOOGLE_SERVICES_JSON`: Android Firebase config
- `GOOGLE_SERVICE_INFO_PLIST`: iOS Firebase config

## Manual Release (if needed)

To manually trigger a release:

1. Ensure you're on `main` or `develop` branch
2. Go to GitHub Actions
3. Select "Release" workflow
4. Click "Run workflow"

## Testing the Version System

Test version code calculation:
```bash
./scripts/version-to-android.sh "1.2.3"
# Output: 1002003

./scripts/version-to-android.sh "2.0.0"
# Output: 2000000
```

## Local Development

To test the build process locally:
```bash
# Build release artifacts
./scripts/release.sh "1.2.3"

# Or manually with specific versions
./gradlew :composeApp:assembleRelease \
  -PVERSION_NAME="1.2.3" \
  -PVERSION_CODE="1002003"
```

## Troubleshooting

### No release created
- Check that commits follow the conventional commit format
- Verify the branch is `main` or `develop`
- Review GitHub Actions logs

### Build fails
- Ensure all dependencies are installed
- Check that signing configuration is correct
- Verify Firebase configs are in place (if using Firebase)

### Version mismatch
- The version in `build.gradle.kts` is the source of truth
- After semantic-release runs, this file is updated automatically
- Don't manually edit the version unless necessary

## Migration Notes

### Cleaned Up
- ✅ Old manual version management removed
- ✅ `scripts/release.sh` updated to use semantic versioning
- ✅ Android version code auto-generated from semantic version
- ✅ Version passed via environment variables (no file modifications needed)

### New Files
- `.github/workflows/release.yml`: Main release workflow
- `.github/workflows/ci.yml`: CI checks for PRs
- `scripts/version-to-android.sh`: Version code calculator
- `RELEASE.md`: This documentation

### Updated Files
- `.releaserc.json`: Complete semantic-release configuration
- `composeApp/build.gradle.kts`: Auto version code calculation
- `scripts/release.sh`: Version conversion and build
