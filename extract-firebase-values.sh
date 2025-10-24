#!/bin/bash

# Script to extract Firebase configuration values for GitHub Secrets
# Run this BEFORE removing your firebase config files from git

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_CONFIG="$SCRIPT_DIR/composeApp/google-services.json"
IOS_CONFIG="$SCRIPT_DIR/iosApp/iosApp/GoogleService-Info.plist"

echo "=========================================="
echo "Firebase Configuration Extractor"
echo "=========================================="
echo ""
echo "This will extract values from your Firebase config files"
echo "to help you set up GitHub Secrets."
echo ""

# Check if jq is available
if ! command -v jq &> /dev/null; then
    echo "⚠️  Warning: 'jq' is not installed. Install it with: brew install jq"
    echo ""
fi

if [[ -f "$ANDROID_CONFIG" ]]; then
    echo "📱 Android Configuration (google-services.json):"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    if command -v jq &> /dev/null; then
        echo "FIREBASE_PROJECT_NUMBER=$(jq -r '.project_info.project_number' "$ANDROID_CONFIG")"
        echo "FIREBASE_PROJECT_ID=$(jq -r '.project_info.project_id' "$ANDROID_CONFIG")"
        echo "FIREBASE_STORAGE_BUCKET=$(jq -r '.project_info.storage_bucket' "$ANDROID_CONFIG")"
        echo "FIREBASE_ANDROID_APP_ID=$(jq -r '.client[0].client_info.mobilesdk_app_id' "$ANDROID_CONFIG")"
        echo "FIREBASE_ANDROID_API_KEY=$(jq -r '.client[0].api_key[0].current_key' "$ANDROID_CONFIG")"
    else
        echo "❌ Cannot extract values without 'jq'. Please install it or manually copy from:"
        echo "   $ANDROID_CONFIG"
    fi
    echo ""
else
    echo "❌ Android config not found: $ANDROID_CONFIG"
    echo ""
fi

if [[ -f "$IOS_CONFIG" ]]; then
    echo "🍎 iOS Configuration (GoogleService-Info.plist):"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "FIREBASE_IOS_API_KEY=$(/usr/libexec/PlistBuddy -c "Print :API_KEY" "$IOS_CONFIG" 2>/dev/null || echo "ERROR")"
        echo "FIREBASE_GCM_SENDER_ID=$(/usr/libexec/PlistBuddy -c "Print :GCM_SENDER_ID" "$IOS_CONFIG" 2>/dev/null || echo "ERROR")"
        echo "FIREBASE_IOS_APP_ID=$(/usr/libexec/PlistBuddy -c "Print :GOOGLE_APP_ID" "$IOS_CONFIG" 2>/dev/null || echo "ERROR")"
    else
        echo "❌ PlistBuddy not available on this platform. Manually copy from:"
        echo "   $IOS_CONFIG"
    fi
    echo ""
else
    echo "❌ iOS config not found: $IOS_CONFIG"
    echo ""
fi

echo "=========================================="
echo "Next Steps:"
echo "=========================================="
echo ""
echo "1. Copy the values above"
echo "2. Go to GitHub: Settings → Secrets and variables → Actions"
echo "3. Add each value as a repository secret"
echo "4. See GITHUB_SECRETS.md for the complete list"
echo ""
echo "After adding secrets to GitHub:"
echo "  git rm --cached composeApp/google-services.json"
echo "  git rm --cached iosApp/iosApp/GoogleService-Info.plist"
echo "  git add .gitignore"
echo "  git commit -m 'chore: remove Firebase credentials from version control'"
echo "  git push"
echo ""
