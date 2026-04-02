#!/usr/bin/env bash
# Runs all Maestro E2E flows with per-test video recording.
# Usage: ./maestro/run-all.sh [output_dir]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="${1:-$SCRIPT_DIR/results}"
mkdir -p "$OUT_DIR/videos" "$OUT_DIR/screenshots"

export PATH="$HOME/.maestro/bin:$HOME/android-sdk/platform-tools:${PATH}"
export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true

PASS=0
FAIL=0
TOTAL=0

for flow in "$SCRIPT_DIR"/*.yaml; do
    name="$(basename "$flow" .yaml)"
    TOTAL=$((TOTAL + 1))
    echo ""
    echo "━━━ [$TOTAL] $name ━━━"

    # Start screen recording
    adb shell screenrecord --size 320x640 --bit-rate 2000000 "/sdcard/${name}.mp4" &
    REC_PID=$!

    if maestro --device emulator-5554 test "$flow" 2>&1; then
        echo "  ✓ PASSED"
        PASS=$((PASS + 1))
    else
        echo "  ✗ FAILED"
        FAIL=$((FAIL + 1))
    fi

    # Stop recording and pull video
    kill "$REC_PID" 2>/dev/null || true
    wait "$REC_PID" 2>/dev/null || true
    sleep 1
    adb pull "/sdcard/${name}.mp4" "$OUT_DIR/videos/${name}.mp4" 2>/dev/null || true
    adb shell rm "/sdcard/${name}.mp4" 2>/dev/null || true
done

# Pull any screenshots Maestro took
cp "$SCRIPT_DIR"/screenshots/*.png "$OUT_DIR/screenshots/" 2>/dev/null || true

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Results: $PASS passed, $FAIL failed, $TOTAL total"
echo "Videos:      $OUT_DIR/videos/"
echo "Screenshots: $OUT_DIR/screenshots/"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

[ "$FAIL" -eq 0 ]
