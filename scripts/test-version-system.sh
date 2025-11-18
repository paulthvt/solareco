#!/usr/bin/env bash

# Test script for version conversion system
# Usage: ./scripts/test-version-system.sh

set -e

echo "🧪 Testing Version Conversion System"
echo "===================================="
echo ""

# Test version-to-android.sh
echo "📱 Testing Android Version Code Conversion"
echo "-------------------------------------------"

test_versions=(
  "1.0.0:1000000"
  "1.2.3:1002003"
  "2.5.10:2005010"
  "1.2.3-beta.1:1002003"
  "10.20.30:10020030"
)

failed=0
passed=0

for test_case in "${test_versions[@]}"; do
  version="${test_case%%:*}"
  expected="${test_case##*:}"
  result=$(./scripts/version-to-android.sh "$version")
  
  if [ "$result" == "$expected" ]; then
    echo "✅ $version → $result (expected: $expected)"
    ((passed++))
  else
    echo "❌ $version → $result (expected: $expected)"
    ((failed++))
  fi
done

echo ""
echo "📊 Results"
echo "----------"
echo "Passed: $passed"
echo "Failed: $failed"

if [ $failed -eq 0 ]; then
  echo ""
  echo "✅ All tests passed!"
  echo ""
  echo "💡 Next steps:"
  echo "   1. Configure GitHub secrets (see QUICKSTART.md)"
  echo "   2. Make a test commit: git commit -m 'feat: test release'"
  echo "   3. Push to develop or main branch"
  echo "   4. Check GitHub Actions for workflow execution"
  exit 0
else
  echo ""
  echo "❌ Some tests failed!"
  exit 1
fi
