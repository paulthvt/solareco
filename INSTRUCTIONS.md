# Comwatt (SolarEco) — Compose Multiplatform Project

## Project Overview

**Comwatt** (published as **SolarEco**) is a Kotlin Multiplatform application built with Compose
Multiplatform, targeting **Android**, **iOS**, and **Desktop** (JVM). It provides energy monitoring
and device management for Comwatt-connected homes.

| Target        | UI toolkit                         | HTTP client            |
|---------------|------------------------------------|------------------------|
| Android       | Compose Multiplatform (Material 3) | Ktor (Android engine)  |
| iOS           | Compose Multiplatform (Material 3) | Ktor (Darwin engine)   |
| Desktop (JVM) | Compose Multiplatform (Material 3) | Ktor (Apache 5 engine) |

## Tech Stack

| Category             | Technology                             | Version |
|----------------------|----------------------------------------|---------|
| Language             | Kotlin                                 | 2.3.x   |
| UI                   | Compose Multiplatform                  | 1.10.x  |
| Build tool           | Gradle (Kotlin DSL)                    | 8.14.x  |
| Networking           | Ktor                                   | 3.4.x   |
| Database             | Room (Multiplatform)                   | 2.8.x   |
| Preferences          | DataStore                              | 1.2.x   |
| Navigation           | Navigation Compose                     | 2.9.x   |
| Charting             | Vico / KoalaPlot                       | —       |
| Functional           | Arrow                                  | 2.2.x   |
| Logging              | Kermit                                 | 2.0.x   |
| Crash Reporting      | Firebase Crashlytics (via GitLive SDK) | —       |
| Dependency Injection | Manual (AppContainer)                  | —       |
| Serialization        | kotlinx.serialization                  | —       |
| Code Generation      | KSP                                    | —       |
| Versioning           | Release Please                         | —       |
| Dependency Updates   | Renovate                               | —       |
| i18n                 | Crowdin                                | —       |

## Prerequisites

- **JDK 17** — required for Android compilation (`sourceCompatibility` / `targetCompatibility`)
- **Android SDK** — compileSdk 36, minSdk 24, targetSdk 36
- **Xcode** (macOS only) — required for iOS builds (latest stable recommended)
- **Gradle** — provided via the Gradle Wrapper (`./gradlew`), no separate install needed

## Project Structure

```
comwatt/
├── build.gradle.kts              # Root build script (plugin declarations)
├── settings.gradle.kts           # Project settings, repository config
├── gradle.properties             # JVM args, Android flags, versioning
├── gradle/
│   └── libs.versions.toml        # Version catalog (single source of truth for deps)
├── composeApp/                   # Shared KMP module (main application code)
│   ├── build.gradle.kts          # Module build: targets, dependencies, Android config
│   ├── proguard-rules.pro        # R8/ProGuard rules for Android release
│   ├── schemas/                  # Room database migration schemas
│   └── src/
│       ├── commonMain/           # Shared code (UI, domain, data, navigation)
│       ├── commonTest/           # Shared tests
│       ├── androidMain/          # Android-specific code (widgets, manifest)
│       ├── iosMain/              # iOS-specific code (platform expect/actual)
│       └── desktopMain/          # Desktop-specific code
├── iosApp/                       # iOS entry point (Xcode project)
│   ├── iosApp.xcodeproj/
│   ├── iosApp/                   # Swift app delegate, ContentView, assets
│   └── ConsumptionWidget/        # iOS widget extension
├── scripts/                      # Build & release helper scripts
├── .github/workflows/            # CI/CD (GitHub Actions)
└── svg-assets/                   # Source SVG illustrations
```

### Key Source Packages (`composeApp/src/commonMain/`)

```
net.thevenot.comwatt/
├── App.kt                 # Root composable / app entry point
├── AppContainer.kt        # Manual DI container
├── DataRepository.kt      # Data layer facade
├── client/                # Ktor HTTP client, API models
├── database/              # Room entities, DAOs, database
├── domain/                # Use cases, domain models
├── model/                 # Shared data models (DeviceCode, etc.)
├── ui/
│   ├── common/            # Reusable UI components (LoadingView, etc.)
│   ├── dashboard/         # Dashboard screen
│   ├── devices/           # Devices list screen
│   ├── home/              # Home screen
│   ├── login/             # Authentication screen
│   ├── nav/               # Navigation graph, scaffold
│   ├── settings/          # Settings screen
│   ├── site/              # Site screen
│   ├── user/              # User screen
│   └── theme/             # Theme, colors, icons
├── utils/                 # Utility functions
└── widget/                # Cross-platform widget logic
```

## Building & Running

### Android

```bash
# Debug build
./gradlew :composeApp:assembleDebug

# Install on connected device / emulator
./gradlew :composeApp:installDebug

# Release build (requires signing config — see "Android Signing" below)
./gradlew :composeApp:assembleRelease

# Release bundle (AAB for Play Store)
./gradlew :composeApp:bundleRelease
```

### iOS

Build via Xcode:

1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. Select the **iosApp** scheme and a simulator or device destination.
3. Press **⌘R** to build and run.

Or from the command line:

```bash
# Build shared framework
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Then build the iOS app via xcodebuild
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  build
```

### Desktop (JVM)

```bash
# Run the desktop app
./gradlew :composeApp:run

# Package native distributions (DMG on macOS, MSI on Windows, DEB on Linux)
./gradlew :composeApp:packageDmg          # macOS
./gradlew :composeApp:packageMsi          # Windows
./gradlew :composeApp:packageDeb          # Linux
```

## Running Tests

```bash
# Run all tests (common + platform-specific)
./gradlew test

# Run common tests only
./gradlew :composeApp:desktopTest
```

Test results are generated at `composeApp/build/reports/tests/` and
`composeApp/build/test-results/`.

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

If all four values are present and the keystore file exists, the release build will be signed
automatically. Otherwise, the build will be unsigned.

## Firebase Configuration

Firebase (Crashlytics) is enabled **only for release builds** on Android and iOS. Configuration
files are generated from templates during CI:

- **Android**: `composeApp/google-services.json` (from `.template`)
- **iOS**: `iosApp/iosApp/GoogleService-Info.plist` (from `.template`)

Both files are `.gitignore`'d. For local release builds, create them manually from the templates
using your Firebase project values.

## CI/CD — GitHub Actions

Three workflows in `.github/workflows/`:

| Workflow              | Trigger            | Purpose                                              |
|-----------------------|--------------------|------------------------------------------------------|
| `build.yml`           | Push + PRs to main | Compile all targets (no linking) + run desktop tests |
| `release-please.yml`  | Push to main       | Manage the Release PR (version bump + CHANGELOG)     |
| `release.yml`         | Tag push (`X.Y.Z`) | Build signed artifacts and upload to GitHub Release  |

### Required GitHub Secrets

| Secret                     | Purpose                                    |
|----------------------------|--------------------------------------------|
| `ANDROID_KEYSTORE_BASE64`  | Base64-encoded release `.jks` keystore     |
| `RELEASE_STORE_PASSWORD`   | Keystore password                          |
| `RELEASE_KEY_ALIAS`        | Key alias (e.g., `comwatt`)                |
| `RELEASE_KEY_PASSWORD`     | Key password                               |
| `FIREBASE_PROJECT_NUMBER`  | Firebase project number                    |
| `FIREBASE_PROJECT_ID`      | Firebase project ID                        |
| `FIREBASE_STORAGE_BUCKET`  | Firebase storage bucket                    |
| `FIREBASE_ANDROID_APP_ID`  | Firebase Android app ID                    |
| `FIREBASE_ANDROID_API_KEY` | Firebase Android API key                   |
| `FIREBASE_IOS_API_KEY`     | Firebase iOS API key                       |
| `FIREBASE_GCM_SENDER_ID`   | Firebase GCM sender ID                     |
| `FIREBASE_IOS_APP_ID`      | Firebase iOS app ID                        |

Note: Release Please uses the built-in `GITHUB_TOKEN` — no extra PAT needed.

## Versioning & Release Process

The project uses [Release Please](https://github.com/googleapis/release-please)
with [Conventional Commits](https://www.conventionalcommits.org/) on a single `main` branch.

### Commit Convention

| Prefix                        | Version Bump          | Example                          |
|-------------------------------|-----------------------|----------------------------------|
| `feat:`                       | Minor (1.0.0 → 1.1.0) | `feat: add device toggle`        |
| `fix:`                        | Patch (1.0.0 → 1.0.1) | `fix: resolve crash on startup`  |
| `perf:`                       | Patch                 | `perf: optimize chart rendering` |
| `refactor:`                   | Patch                 | `refactor: simplify navigation`  |
| `feat!:` / `BREAKING CHANGE:` | Major (1.0.0 → 2.0.0) | `feat!: new authentication flow` |

### How to Release

1. Push conventional commits to `main` (directly or via merged PRs)
2. Release Please auto-creates/updates a "Release PR" with version bump + CHANGELOG
3. Merge the Release PR → creates a git tag + GitHub Release
4. Tag push triggers `release.yml` which builds and uploads artifacts

### Android Version Code

Automatically calculated from the semantic version:

```
versionCode = MAJOR × 1,000,000 + MINOR × 1,000 + PATCH
```

Example: `1.2.3` → `1002003`

## i18n / Localization

Translations are managed via [Crowdin](https://crowdin.com/). The source strings are at:

```
composeApp/src/commonMain/composeResources/values/strings.xml
```

Translated files follow the pattern `values-{locale}/strings.xml` and are synced by Crowdin
automatically.

## Dependency Management

- **Version catalog**: All dependency versions are centralized in `gradle/libs.versions.toml`.
- **Renovate**: Automated dependency update PRs are configured via `renovate.json`.

## Useful Gradle Tasks

```bash
./gradlew tasks                              # List all available tasks
./gradlew :composeApp:dependencies           # Show dependency tree
./gradlew :composeApp:assembleDebug          # Android debug APK
./gradlew :composeApp:assembleRelease        # Android release APK
./gradlew :composeApp:bundleRelease          # Android release AAB
./gradlew :composeApp:run                    # Run desktop app
./gradlew :composeApp:desktopTest            # Run desktop/common tests
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64   # iOS simulator framework
./gradlew build -x test                      # Build all targets (skip tests)
```

## Troubleshooting

### Out of memory during build

Increase Gradle daemon heap in `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx8g -XX:MaxMetaspaceSize=1024m
kotlin.daemon.jvmargs=-Xmx8g -XX:MaxMetaspaceSize=1024m
```

### iOS build fails — framework not found

Ensure the shared framework is built first:

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

Then build the Xcode project.

### Release build unsigned

Verify all four signing properties (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`,
`RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) are set in `local.properties` or environment variables,
and that the keystore file exists at the specified path.

### Firebase / Crashlytics not working in debug

Firebase is intentionally disabled for debug builds. It is only activated for release builds to
avoid polluting crash reports during development.
