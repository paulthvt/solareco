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

echo "Building iOS App Archive..."
# Build the iOS app archive
# Note: We skip code signing for CI builds - signing should be done separately for distribution
if ! xcodebuild archive \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -sdk iphoneos \
  -arch arm64 \
  -archivePath build/iosApp.xcarchive \
  CODE_SIGN_STYLE=Manual \
  CODE_SIGN_IDENTITY="" \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGNING_ALLOWED=NO \
  DEVELOPMENT_TEAM="" \
  ONLY_ACTIVE_ARCH=NO; then
  echo "Error: iOS App archive failed"
  exit 1
fi
echo "iOS App archived successfully."

echo "Exporting iOS App IPA..."
# Export the archive to create the .ipa file
if ! xcodebuild -exportArchive \
  -archivePath build/iosApp.xcarchive \
  -exportOptionsPlist iosApp/exportOptions.plist \
  -exportPath build/ios-release; then
  echo "Error: iOS App export failed"
  exit 1
fi
echo "iOS App IPA exported successfully."

echo "Building Android Release..."
./gradlew :composeApp:assembleRelease :composeApp:bundleRelease -PVERSION_NAME="$VERSION_NAME" -PVERSION_CODE="$VERSION_CODE"
echo "Android Release built successfully."

echo "Release artifacts are ready:"
echo "  - Android APK: composeApp/build/outputs/apk/release/"
echo "  - Android Bundle: composeApp/build/outputs/bundle/release/"
echo "  - iOS IPA: build/ios-release/iosApp.ipa"
echo "  - iOS Archive: build/iosApp.xcarchive/"