# Comwatt (SolarEco)

A [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) application for
monitoring solar energy production and home consumption on Comwatt-connected devices.

**Targets:** Android · iOS · Desktop (JVM)

## Project Structure

```
comwatt/
├── composeApp/          # Shared Kotlin Multiplatform module
│   └── src/
│       ├── commonMain/  # Shared code (UI, data, domain, navigation)
│       ├── androidMain/ # Android-specific code (widgets, manifest)
│       ├── iosMain/     # iOS-specific code (platform expect/actual)
│       └── desktopMain/ # Desktop-specific code
├── iosApp/              # iOS entry point (Xcode project + SwiftUI bridge)
├── scripts/             # Build & release helper scripts
└── .github/workflows/   # CI/CD (GitHub Actions)
```

## Prerequisites

- **JDK 17**
- **Android SDK** (compileSdk 36, minSdk 24)
- **Xcode** (macOS only, for iOS builds)
- **Gradle** is provided via the wrapper — no separate install needed

## Build & Run

### Android

```bash
./gradlew :composeApp:assembleDebug      # Debug APK
./gradlew :composeApp:installDebug       # Install on device / emulator
./gradlew :composeApp:bundleRelease      # Release AAB (requires signing)
```

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode, select a destination, and press **⌘R**.

Or from the terminal:

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 16' build
```

### Desktop

```bash
./gradlew :composeApp:run                # Run the app
./gradlew :composeApp:packageDmg         # Package as DMG (macOS)
```

## Tests

```bash
./gradlew test                           # All tests
./gradlew :composeApp:desktopTest        # Common / desktop tests only
```

## Release & CI

Releases are automated with [Release Please](https://github.com/googleapis/release-please)
and [Conventional Commits](https://www.conventionalcommits.org/) on a single `main` branch.
See **[RELEASE.md](RELEASE.md)** for the full release process and required GitHub Secrets.

## Key Libraries

| Area            | Library                                                                                                       |
|-----------------|---------------------------------------------------------------------------------------------------------------|
| Networking      | [Ktor](https://ktor.io/)                                                                                      |
| Database        | [Room Multiplatform](https://developer.android.com/kotlin/multiplatform/room)                                 |
| Navigation      | [Navigation Compose](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-routing.html) |
| Charts          | [Vico](https://patrykandpatrick.com/vico/) / [KoalaPlot](https://koalaplot.github.io/)                        |
| Functional      | [Arrow](https://arrow-kt.io/)                                                                                 |
| Crash Reporting | [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics) (release builds only)                    |

## License

Private — all rights reserved.