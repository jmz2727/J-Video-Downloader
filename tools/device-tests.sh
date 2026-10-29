#!/usr/bin/env bash
set -euo pipefail
mkdir -p app/build/reports/androidTests
trap 'adb logcat -d > app/build/reports/androidTests/logcat.txt' EXIT
./gradlew connectedDebugAndroidTest --stacktrace
./gradlew installDebug --stacktrace
adb shell am start -W -n com.jameel.jvideodownloader/.MainActivity
adb shell screencap -p /sdcard/j-video-home.png
adb pull /sdcard/j-video-home.png app/build/reports/androidTests/home.png
