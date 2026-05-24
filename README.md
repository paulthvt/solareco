# Comwatt (SolarEco)

A [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) application for
monitoring solar energy production and home consumption on Comwatt-connected devices.

**Targets:** Android · iOS · Desktop (JVM)

## Project Structure

```
comwatt/
├── shared/              # Kotlin Multiplatform library (common code + platform implementations)
│   └── src/
│       ├── commonMain/  # Shared UI, domain logic, data layer, navigation
│       ├── androidMain/ # Android platform implementations (expect/actual)
│       ├── iosMain/     # iOS platform implementations
│       └── desktopMain/ # Desktop platform implementations
├── androidApp/          # Android application entry point
│   └── src/main/        # MainActivity, AndroidManifest, widgets, app resources
├── desktopApp/          # Desktop (JVM) application entry point
│   └── src/main/        # Main.kt, desktop configuration
├── iosApp/              # iOS application (Xcode project + SwiftUI bridge)
├── scripts/             # Build & release helper scripts
└── .github/workflows/   # CI/CD (GitHub Actions)
```

**Architecture:** The project follows a clean separation between shared business logic (`shared/`)
and platform-specific application entry points (`androidApp/`, `desktopApp/`, `iosApp/`). This
structure aligns with Compose Multiplatform best practices and Android Gradle Plugin 9.0
requirements.

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