# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Project Identity

**Comwatt** (published as **SolarEco**) is a Kotlin Multiplatform application built with Compose
Multiplatform, targeting Android, iOS, and Desktop (JVM). It monitors solar energy production and
home consumption for Comwatt-connected devices.

### Platform Requirements

- **Android:** API 24+ (Android 7.0)
- **iOS:** 18.0+ (required by Compose Multiplatform 1.11.0)
- **Desktop:** JVM 17+

## Build Commands

### Android

```bash
./gradlew :composeApp:assembleDebug        # Debug APK
./gradlew :composeApp:installDebug         # Install on device/emulator
./gradlew :composeApp:bundleRelease        # Release AAB (requires signing)
```

### iOS

```bash
# Build shared framework first
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Then open in Xcode or use xcodebuild
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 16' build
```

### Desktop

```bash
./gradlew :composeApp:run                  # Run app
./gradlew :composeApp:packageDmg           # Package as DMG (macOS)
```

### Tests

```bash
./gradlew test                             # All tests
./gradlew :composeApp:desktopTest          # Common/desktop tests only
```

## Architecture

### Layered Structure

The app follows a layered architecture in `composeApp/src/commonMain/`:

1. **UI Layer** (`ui/`) — Compose screens, ViewModels, and navigation
    - `nav/` — Type-safe navigation with sealed `Screen` class
    - `theme/` — Material 3 theme, colors, icons
    - Each feature has its own package (e.g., `dashboard/`, `devices/`, `login/`)

2. **Domain Layer** (`domain/`) — Use cases encapsulating business logic
    - Each use case is a single-responsibility class (e.g., `FetchDevicesUseCase`,
      `UpdateDeviceUseCase`)
    - Use cases coordinate between API client and database
    - Return domain models or `Either<ApiError, T>` for error handling

3. **Data Layer**
    - `client/` — Ktor HTTP client + API models
        - `ComwattApi` is the main API facade
        - Uses `Either` from Arrow for error handling
    - `database/` — Room entities, DAOs, database
        - `UserDatabase` with Room Multiplatform
        - `SettingsRepository` uses DataStore for preferences
    - `DataRepository` — Facade coordinating data access for the UI

4. **Model Layer** (`model/`) — Shared data models (DTOs, domain objects)

### Dependency Injection

Manual DI via `AppContainer`:

- Singleton `DataRepository` instance
- Lazy initialization of database, API client, and settings repository
- Platform-specific factory (`Factory`) provides platform implementations (database builder, HTTP
  client)

### Navigation

Type-safe navigation using `androidx.navigation.compose`:

- All routes defined as sealed classes in `ui/nav/Screen.kt`
- Main navigation graph starts at `Login` → `SiteChooser` → `Main` (nested graph with `Home`,
  `Dashboard`, `Devices`)
- Use `navController.navigate(Screen.Target)` for navigation

### State Management

- ViewModels use `StateFlow`/`Flow` for reactive state
- UI observes state via `collectAsState()` or `collectAsStateWithLifecycle()`
- Side effects handled in ViewModel coroutine scopes

## Key Dependencies

| Library              | Purpose                   | Notes                                                |
|----------------------|---------------------------|------------------------------------------------------|
| Ktor                 | HTTP client               | Platform-specific engines (Android, Darwin, Apache5) |
| Room                 | Local database            | Multiplatform, KSP-generated DAOs                    |
| Arrow                | Functional error handling | `Either<L, R>` for API results                       |
| DataStore            | Settings persistence      | Preferences-only (no proto)                          |
| Navigation Compose   | Type-safe routing         | Multiplatform navigation                             |
| Vico / KoalaPlot     | Charts                    | Energy consumption/production charts                 |
| Kermit               | Logging                   | Multiplatform structured logging                     |
| Firebase Crashlytics | Crash reporting           | **Release builds only** via GitLive SDK              |

## Platform-Specific Code

Use `expect`/`actual` for platform differences:

- Database builder (`di/Factory.kt`)
- HTTP client engine selection
- Platform utilities (e.g., password hashing)

### Android-Specific

- Home screen widget (`widget/`) using Glance
- WorkManager for periodic widget updates
- Firebase Crashlytics integration (release only)

### iOS-Specific

- Swift entry point (`iosApp/iosApp/iOSApp.swift`)
- Widget extension (`iosApp/ConsumptionWidget/`)

## Release Process

### Automated via Release Please

Single-branch workflow on `main` using Google's Release Please action.

**Commit Convention:**

- `feat:` → minor bump (1.0.0 → 1.1.0)
- `fix:`, `perf:`, `refactor:` → patch bump (1.0.0 → 1.0.1)
- `feat!:` or `BREAKING CHANGE:` → major bump (1.0.0 → 2.0.0)

**Workflow:**

1. Push conventional commits to `main`
2. Release Please auto-creates/updates a "Release PR" with version bump + CHANGELOG
3. Merging that PR creates a git tag + GitHub Release
4. Tag push triggers `release.yml` which builds and uploads artifacts (APK, AAB, iOS archive)

### Android Version Code

Auto-calculated from semantic version:

```
versionCode = MAJOR × 1,000,000 + MINOR × 1,000 + PATCH
```

Example: `1.2.3` → `1002003`

### Signing

Release signing configured in `composeApp/build.gradle.kts`:

- Reads from `local.properties` (local) or environment variables (CI)
- Required properties: `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
  `RELEASE_KEY_PASSWORD`
- If missing, build is unsigned

### Firebase Configuration

Firebase (Crashlytics) enabled **only for release builds** on Android and iOS:

- Config files generated from `.template` files during CI
- `composeApp/google-services.json` (Android)
- `iosApp/iosApp/GoogleService-Info.plist` (iOS)
- Both files are `.gitignore`'d

## Development Workflow

### Adding a Feature

1. Create domain models in `model/`
2. Add use case in `domain/` (if needed)
3. Update API client in `client/ComwattApi.kt` (if API changes)
4. Add database entities/DAOs in `database/` (if persistence needed)
5. Create UI in `ui/<feature>/`
6. Add navigation route to `ui/nav/Screen.kt`
7. Wire up navigation in `App.kt`

### Testing

- Unit tests go in `commonTest/`
- Use `kotlinx-coroutines-test` for async testing
- Test resources via `com.goncalossilva:resources` plugin
- Room schema exports stored in `composeApp/schemas/`

### Debugging Build Issues

- **Out of memory:** Increase heap in `gradle.properties` (`org.gradle.jvmargs=-Xmx8g`)
- **iOS framework not found:** Run `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` first
- **Release build unsigned:** Verify all four signing properties are set and keystore exists
- **Firebase not working in debug:** Intentional — Firebase only active in release builds

## Code Conventions

- Package structure follows feature-based organization in `ui/`
- Use `Either<ApiError, T>` for API error handling (Arrow)
- ViewModels own coroutine scopes via `viewModelScope`
- Database operations run on `Dispatchers.Default` via `DataRepository.scope`
- Composables are `@Composable` functions, not classes
- Navigation uses type-safe routes (sealed classes, not string paths)
- Use `SnackbarHostState` for user-facing error messages

## Dependency Management

- All versions centralized in `gradle/libs.versions.toml`
- Renovate automatically creates PRs for dependency updates
- Review and test updates before merging

## i18n

- Translations managed via Crowdin
- Source strings: `composeApp/src/commonMain/composeResources/values/strings.xml`
- Translated files: `values-{locale}/strings.xml`
