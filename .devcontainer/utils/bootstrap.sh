#!/usr/bin/env bash
set -euo pipefail


# Ensure Gradle wrapper is executable (common in Windows checkouts)
if [ -f ./gradlew ]; then
chmod +x ./gradlew || true
fi


# Print versions for quick sanity check
java -version || true
./gradlew --version || gradle --version || true


echo "✅ Dev container bootstrap complete"