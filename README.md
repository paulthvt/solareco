This is a Kotlin Multiplatform project targeting Android, iOS.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform, 
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

---

Android release build

- Release signing is wired in composeApp/build.gradle.kts. It reads these variables from
  `local.properties` (preferred for local builds) or environment variables (for CI):
    - RELEASE_STORE_FILE (absolute path to .jks)
    - RELEASE_STORE_PASSWORD
    - RELEASE_KEY_ALIAS
    - RELEASE_KEY_PASSWORD
- If all four are present, release builds are signed; otherwise, they’re unsigned.

Local build

1) Generate a keystore once (replace values):
   keytool -genkeypair -v -keystore ~/keystores/comwatt-release.jks -keyalg RSA -keysize 2048
   -validity 10000 -alias comwatt
2) Put secrets in `local.properties` (not versioned) or export as env vars:
   RELEASE_STORE_FILE=/Users/you/keystores/comwatt-release.jks
   RELEASE_STORE_PASSWORD=******
   RELEASE_KEY_ALIAS=comwatt
   RELEASE_KEY_PASSWORD=******
3) Build the app bundle:
   ./gradlew :composeApp:bundleRelease
   Artifacts: composeApp/build/outputs/**/*.aab (and APK under apk/ if needed)

CI build (GitHub Actions)

- A workflow is provided at .github/workflows/android-release.yml.
- Add these GitHub Secrets in the repo:
    - ANDROID_KEYSTORE_BASE64: base64-encoded release .jks
    - RELEASE_STORE_PASSWORD
    - RELEASE_KEY_ALIAS
    - RELEASE_KEY_PASSWORD
- The workflow writes the keystore to RELEASE_STORE_FILE and runs :composeApp:bundleRelease, then
  uploads the AAB/APK as artifacts.

Notes and good practices

- Do not commit keystores or secret property files. .gitignore was updated to exclude them.
- Prefer Play App Signing. Your upload key (this keystore) signs the upload; Google signs the final
  artifact.
- Avoid hardcoding secrets or API keys in code. Use backend services, remote config, or inject via
  build-time vars if they’re non-sensitive.
