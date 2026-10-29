#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
java --module jdk.compiler/com.sun.tools.javac.Main -d "$OUT" app/src/main/java/com/jameel/jvideodownloader/core/*.java tools/PolicyChecks.java tools/SecurityChecks.java
java -cp "$OUT" PolicyChecks
java -cp "$OUT" SecurityChecks
python3 tools/static-check.py
