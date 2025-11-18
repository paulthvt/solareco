#!/usr/bin/env bash

# Converts semantic version to Android versionCode
# Format: MAJOR.MINOR.PATCH[-PRERELEASE] -> versionCode
# Example: 1.2.3 -> 1002003
# Example: 1.2.3-beta.4 -> 1002003 (prerelease suffix ignored for versionCode)

if [ -z "$1" ]; then
  echo "Missing version number. Usage: ./scripts/version-to-android.sh <version>"
  exit 1
fi

VERSION=$1

# Extract version parts (remove prerelease suffix if present)
VERSION_WITHOUT_PRERELEASE=$(echo "$VERSION" | sed -E 's/(-.*)//')
MAJOR=$(echo "$VERSION_WITHOUT_PRERELEASE" | cut -d. -f1)
MINOR=$(echo "$VERSION_WITHOUT_PRERELEASE" | cut -d. -f2)
PATCH=$(echo "$VERSION_WITHOUT_PRERELEASE" | cut -d. -f3)

# Ensure we have valid numbers (default to 0 if missing)
MAJOR=${MAJOR:-0}
MINOR=${MINOR:-0}
PATCH=${PATCH:-0}

# Calculate versionCode: MAJOR * 1000000 + MINOR * 1000 + PATCH
# This allows up to version 99.999.999
VERSION_CODE=$((MAJOR * 1000000 + MINOR * 1000 + PATCH))

echo "$VERSION_CODE"
