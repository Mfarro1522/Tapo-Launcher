#!/bin/bash

VERSION="1.0.0"
OUTPUT_DIR="app/app/build/outputs/apk/debug"
RELEASES_DIR="releases"

echo "Building kdeLauncher v$VERSION..."

cd "$(dirname "$0")"

mkdir -p "$RELEASES_DIR"

./gradlew assembleDebug

if [ -f "$OUTPUT_DIR/app-debug.apk" ]; then
    cp "$OUTPUT_DIR/app-debug.apk" "$RELEASES_DIR/kdeLauncher$VERSION.apk"
    echo "APK created: $RELEASES_DIR/kdeLauncher$VERSION.apk"
else
    echo "Error: APK not found"
    exit 1
fi