#!/usr/bin/env bash

if [ -z "$1" ]; then
  echo "Missing version number. Usage: ./scripts/release.sh <version>"
  exit 1
fi

ARTIFACT_VERSION=${1}

# Convert semantic version to Android versionCode
VERSION_CODE=$(./scripts/version-to-android.sh "$ARTIFACT_VERSION")

echo "Building release for version: $ARTIFACT_VERSION (Android versionCode: $VERSION_CODE)"

# Export versions as environment variables for Gradle
export VERSION_NAME="$ARTIFACT_VERSION"
export VERSION_CODE="$VERSION_CODE"

echo "Updating iOS app version..."
./scripts/update-ios-version.sh "$ARTIFACT_VERSION"

echo "Building iOS Framework (required for iOS app)..."
./gradlew :composeApp:linkReleaseFrameworkIosArm64
echo "iOS Framework built successfully."

echo "Building iOS App..."
# Build the iOS app archive
xcodebuild archive \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -archivePath build/iosApp.xcarchive \
  -sdk iphoneos \
  DEVELOPMENT_TEAM="" \
  CODE_SIGN_IDENTITY="" \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGNING_ALLOWED=NO

echo "iOS App archived successfully."

echo "Building Android Release..."
./gradlew :composeApp:assembleRelease :composeApp:bundleRelease -PVERSION_NAME="$VERSION_NAME" -PVERSION_CODE="$VERSION_CODE"
echo "Android Release built successfully."

echo "Release artifacts are ready:"
echo "  - Android APK: composeApp/build/outputs/apk/release/"
echo "  - Android Bundle: composeApp/build/outputs/bundle/release/"
echo "  - iOS Archive: build/iosApp.xcarchive/"