#!/usr/bin/env bash

# Updates iOS app version in Info.plist
# Usage: ./scripts/update-ios-version.sh <version>

set -e

if [ -z "$1" ]; then
  echo "Missing version number. Usage: ./scripts/update-ios-version.sh <version>"
  exit 1
fi

VERSION=$1
INFO_PLIST="iosApp/iosApp/Info.plist"

# Extract version parts (remove prerelease suffix if present)
VERSION_WITHOUT_PRERELEASE=$(echo "$VERSION" | sed -E 's/(-.*)//')
MAJOR=$(echo "$VERSION_WITHOUT_PRERELEASE" | cut -d. -f1)
MINOR=$(echo "$VERSION_WITHOUT_PRERELEASE" | cut -d. -f2)
PATCH=$(echo "$VERSION_WITHOUT_PRERELEASE" | cut -d. -f3)

# Ensure we have valid numbers
MAJOR=${MAJOR:-1}
MINOR=${MINOR:-0}
PATCH=${PATCH:-0}

# Calculate build number similar to Android versionCode
BUILD_NUMBER=$((MAJOR * 1000000 + MINOR * 1000 + PATCH))

echo "Updating iOS app version to: $VERSION (build: $BUILD_NUMBER)"

# Check if Info.plist exists
if [ ! -f "$INFO_PLIST" ]; then
  echo "Error: Info.plist not found at $INFO_PLIST"
  exit 1
fi

# Update CFBundleShortVersionString (version string)
/usr/libexec/PlistBuddy -c "Set :CFBundleShortVersionString $VERSION" "$INFO_PLIST" 2>/dev/null || \
  /usr/libexec/PlistBuddy -c "Add :CFBundleShortVersionString string $VERSION" "$INFO_PLIST"

# Update CFBundleVersion (build number)
/usr/libexec/PlistBuddy -c "Set :CFBundleVersion $BUILD_NUMBER" "$INFO_PLIST" 2>/dev/null || \
  /usr/libexec/PlistBuddy -c "Add :CFBundleVersion string $BUILD_NUMBER" "$INFO_PLIST"

echo "✅ iOS version updated successfully:"
echo "   CFBundleShortVersionString: $VERSION"
echo "   CFBundleVersion: $BUILD_NUMBER"
