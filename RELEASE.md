# Release Process

## Overview

This project uses [semantic-release](https://semantic-release.gitbook.io/) to automate version
management and release creation. The release process is triggered automatically when code is pushed
to `main` or `develop`.

## Commit Convention

The project uses [Conventional Commits](https://www.conventionalcommits.org/) to determine version bumps:

| Prefix                        | Bump                  | Example                          |
|-------------------------------|-----------------------|----------------------------------|
| `feat:`                       | Minor (1.0.0 → 1.1.0) | `feat: add device toggle`        |
| `fix:`                        | Patch (1.0.0 → 1.0.1) | `fix: resolve crash on startup`  |
| `perf:`                       | Patch                 | `perf: optimize chart rendering` |
| `refactor:`                   | Patch                 | `refactor: simplify navigation`  |
| `feat!:` / `BREAKING CHANGE:` | Major (1.0.0 → 2.0.0) | `feat!: new authentication flow` |

## Branches

| Branch       | Purpose              | Version format |
|--------------|----------------------|----------------|
| `main`       | Production releases  | `1.2.3`        |
| `develop`    | Pre-releases / betas | `1.2.3-beta.1` |
| `release/**` | Release preparation  | —              |
| `hotfix/**`  | Hotfixes             | —              |

## Release Flow

When you push to `main` or `develop`, the **Build and Release** workflow (
`.github/workflows/build.yml`) runs:

1. **Analyze commits** — semantic-release inspects commits since the last release
2. **Determine version** — calculates the next version from commit types
3. **Build artifacts** — runs `scripts/release.sh` to produce:
   - Android APK (`composeApp/build/outputs/apk/release/`)
   - Android Bundle (`composeApp/build/outputs/bundle/release/`)
   - iOS archive (`build/`)
4. **Update changelog** — generates / updates `CHANGELOG.md`
5. **Commit & tag** — commits the version bump + changelog, creates a Git tag
6. **Create GitHub Release** — publishes release notes with APK, AAB, and iOS archive attached
7. **Back-merge** — after a production release on `main`, the workflow merges `main` into `develop`

For pull requests, only a build + test run is executed (no release).

## Android Version Code

Android requires a numeric `versionCode` alongside the human-readable `versionName`. It is
calculated automatically:

```
versionCode = MAJOR × 1,000,000 + MINOR × 1,000 + PATCH
```

Examples: `1.0.0` → `1000000` · `1.2.3` → `1002003` · `2.5.10` → `2005010`

## Android Signing

Release signing is configured in `composeApp/build.gradle.kts`. It reads credentials from
`local.properties` (local builds) or environment variables (CI):

| Property                 | Description                          |
|--------------------------|--------------------------------------|
| `RELEASE_STORE_FILE`     | Absolute path to the `.jks` keystore |
| `RELEASE_STORE_PASSWORD` | Keystore password                    |
| `RELEASE_KEY_ALIAS`      | Key alias                            |
| `RELEASE_KEY_PASSWORD`   | Key password                         |

### Local setup

1. Generate a keystore (once):
   ```bash
   keytool -genkeypair -v \
     -keystore ~/keystores/comwatt-release.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias comwatt
   ```
2. Add to `local.properties` (never committed):
   ```properties
   RELEASE_STORE_FILE=/Users/you/keystores/comwatt-release.jks
   RELEASE_STORE_PASSWORD=your-password
   RELEASE_KEY_ALIAS=comwatt
   RELEASE_KEY_PASSWORD=your-password
   ```

If all four values are present and the keystore exists, the release build is signed automatically;
otherwise it stays unsigned.

> **Tip:**
> Prefer [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756).
> Your upload key (this keystore) signs the upload; Google signs the final artifact.

## GitHub Secrets

Configure these in **Settings → Secrets and variables → Actions**:

### Release token

| Secret                   | Purpose                                                                          |
|--------------------------|----------------------------------------------------------------------------------|
| `SEMANTIC_RELEASE_TOKEN` | GitHub PAT — used by semantic-release to push commits, tags, and create releases |

### Android signing

| Secret                    | Purpose                                |
|---------------------------|----------------------------------------|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded release `.jks` keystore |
| `RELEASE_STORE_PASSWORD`  | Keystore password                      |
| `RELEASE_KEY_ALIAS`       | Key alias (e.g., `comwatt`)            |
| `RELEASE_KEY_PASSWORD`    | Key password                           |

### Firebase

| Secret                     | Purpose                 |
|----------------------------|-------------------------|
| `FIREBASE_PROJECT_NUMBER`  | Firebase project number |
| `FIREBASE_PROJECT_ID`      | Firebase project ID     |
| `FIREBASE_STORAGE_BUCKET`  | Firebase storage bucket |
| `FIREBASE_ANDROID_APP_ID`  | Android app ID          |
| `FIREBASE_ANDROID_API_KEY` | Android API key         |
| `FIREBASE_IOS_API_KEY`     | iOS API key             |
| `FIREBASE_IOS_APP_ID`      | iOS app ID              |
| `FIREBASE_GCM_SENDER_ID`   | GCM sender ID           |

Firebase config files (`google-services.json`, `GoogleService-Info.plist`) are generated at build
time from templates and these secrets. They are `.gitignore`'d.

## Manual Release

1. Go to **Actions → Build and Release**
2. Click **Run workflow**
3. Select the target branch (`main` or `develop`)

## Local Build

```bash
# Build release artifacts for a given version
./scripts/release.sh "1.2.3"

# Or pass version properties directly
./gradlew :composeApp:assembleRelease \
  -PVERSION_NAME="1.2.3" \
  -PVERSION_CODE="1002003"
```

## Testing the Version System

```bash
./scripts/version-to-android.sh "1.2.3"   # → 1002003
./scripts/version-to-android.sh "2.0.0"   # → 2000000
```

## Troubleshooting

| Issue              | Fix                                                                                |
|--------------------|------------------------------------------------------------------------------------|
| No release created | Ensure commits follow conventional format and branch is `main` or `develop`        |
| Build fails        | Check signing config and Firebase secrets in GitHub Actions logs                   |
| Version mismatch   | `composeApp/build.gradle.kts` version is the source of truth — don't edit manually |