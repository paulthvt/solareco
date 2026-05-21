# Release Process

This project uses [Release Please](https://github.com/googleapis/release-please) to automate
versioning and releases. It follows the [Conventional Commits](https://www.conventionalcommits.org/)
specification.

## How It Works

1. **Push commits to `main`** using conventional commit messages
2. **Release Please opens/updates a Release PR** with a version bump and CHANGELOG update
3. **Merge the Release PR** to create a git tag and GitHub Release
4. **Tag push triggers the release build** which produces Android APK/AAB and iOS archive

## Commit Convention

| Prefix | Effect |
|---|---|
| `feat:` | Minor version bump |
| `fix:`, `perf:`, `refactor:` | Patch version bump |
| `feat!:` or `BREAKING CHANGE:` footer | Major version bump |
| `chore:`, `docs:`, `ci:`, `test:` | No release (hidden from changelog) |

## CI Workflows

| Workflow | Trigger | Purpose |
|---|---|---|
| `build.yml` | Push to `main`, PRs | Compile all targets + run tests (no linking) |
| `release-please.yml` | Push to `main` | Manage the Release PR |
| `release.yml` | Tag push (`X.Y.Z`) | Build signed artifacts and upload to GitHub Release |

## Configuration Files

- `release-please-config.json` — Release Please settings (changelog sections, extra files)
- `.release-please-manifest.json` — Current version tracker

## Secrets Required

| Secret | Purpose |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded release keystore |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |
| `FIREBASE_*` | Firebase config values for google-services.json and GoogleService-Info.plist |

Note: Release Please uses the built-in `GITHUB_TOKEN` — no extra PAT needed.
