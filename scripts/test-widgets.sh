#!/bin/bash

# Widget Implementation Test Script
# This script helps test the widget functionality

set -e

echo "=========================================="
echo "Comwatt Widget Implementation Test Script"
echo "=========================================="
echo ""

# Check if we're in the right directory
if [ ! -f "build.gradle.kts" ]; then
    echo "Error: Please run this script from the project root directory"
    exit 1
fi

echo "1. Cleaning project..."
./gradlew clean

echo ""
echo "2. Compiling common widget code..."
./gradlew :composeApp:compileCommonMainKotlinMetadata

echo ""
echo "3. Compiling Android widget code..."
./gradlew :composeApp:compileDebugKotlin

echo ""
echo "4. Compiling iOS widget code..."
./gradlew :composeApp:compileKotlinIosSimulatorArm64

echo ""
echo "5. Running tests..."
./gradlew :composeApp:testDebugUnitTest

echo ""
echo "=========================================="
echo "✅ All widget code compiled successfully!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. For Android:"
echo "   - Run: ./gradlew :composeApp:installDebug"
echo "   - Add widget to home screen"
echo "   - Check logs: adb logcat | grep ConsumptionWidget"
echo ""
echo "2. For iOS:"
echo "   - Open Xcode: open iosApp/iosApp.xcodeproj"
echo "   - Build and run on simulator"
echo "   - Add widget to home screen"
echo "   - Check widget extension logs"
echo ""
echo "3. Test widget updates:"
echo "   - Android: Check WorkManager in device settings"
echo "   - iOS: Check Background App Refresh in Settings"
echo ""
echo "4. Debugging:"
echo "   - Check WIDGET_IMPLEMENTATION.md for details"
echo "   - Check WIDGET_README.md for troubleshooting"
echo ""
