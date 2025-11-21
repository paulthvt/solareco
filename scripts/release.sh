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

echo "Creating zip archive of iOS .xcarchive..."
cd build
zip -r -q solareco-${VERSION_NAME}-ios.xcarchive.zip iosApp.xcarchive
cd ..
echo "iOS Archive zip created: build/solareco-${VERSION_NAME}-ios.xcarchive.zip"

echo "Note: Skipping IPA export for unsigned build."
echo "The .xcarchive contains the app binary and can be used for verification."
echo "For distribution, the archive needs to be properly signed and exported."

echo "Building Android Release..."
./gradlew :composeApp:assembleRelease :composeApp:bundleRelease -PVERSION_NAME="$VERSION_NAME" -PVERSION_CODE="$VERSION_CODE"
echo "Android Release built successfully."

# Verify APK signing
echo ""
echo "Verifying APK signing status..."
APK_DIR="composeApp/build/outputs/apk/release"

if [ -f "$APK_DIR/solareco-${VERSION_NAME}-release.apk" ]; then
  echo "✅ Found signed APK: solareco-${VERSION_NAME}-release.apk"
  APK_FILE="$APK_DIR/solareco-${VERSION_NAME}-release.apk"
elif [ -f "$APK_DIR/solareco-${VERSION_NAME}-release-unsigned.apk" ]; then
  echo "⚠️  WARNING: Found unsigned APK: solareco-${VERSION_NAME}-release-unsigned.apk"
  echo "⚠️  This APK cannot be installed on devices!"
  echo "⚠️  Please ensure signing configuration is properly set up."
  APK_FILE="$APK_DIR/solareco-${VERSION_NAME}-release-unsigned.apk"
else
  echo "❌ ERROR: No APK found in $APK_DIR"
  echo "Expected: solareco-${VERSION_NAME}-release.apk"
  ls -la "$APK_DIR" || true
  exit 1
fi

# Try to verify the APK signature using apksigner if available
#if command -v apksigner &> /dev/null; then
#  echo ""
#  echo "Checking APK signature with apksigner..."
#  if apksigner verify "$APK_FILE" 2>&1 | grep -q "Verified using"; then
#    echo "✅ APK is properly signed and verified"
#  else
#    echo "⚠️  APK signature verification failed or APK is unsigned"
#  fi
#fi

echo ""
echo "Release artifacts are ready:"
echo "  - Android APK: $APK_FILE"
echo "  - Android Bundle: composeApp/build/outputs/bundle/release/solareco-${VERSION_NAME}-release.aab"
echo "  - iOS Archive (unsigned): build/iosApp.xcarchive/"
echo "  - iOS Archive (zipped): build/solareco-${VERSION_NAME}-ios.xcarchive.zip"