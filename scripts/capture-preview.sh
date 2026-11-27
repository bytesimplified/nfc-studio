#!/usr/bin/env bash
# Capture the dashboard before AGP instrumentation can uninstall the app.
set -euo pipefail
apk=app/build/outputs/apk/debug/app-debug.apk
adb wait-for-device
adb install -r "$apk"
adb shell am start -W -n com.nfcstudio.app/.MainActivity
sleep 2
mkdir -p screenshots
tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT
adb exec-out screencap -p > "$tmp"
python3 - "$tmp" <<'PY'
import sys
with open(sys.argv[1], "rb") as f:
    if f.read(8) != b"\x89PNG\r\n\x1a\n":
        raise SystemExit("Emulator did not return a PNG")
PY
mv "$tmp" screenshots/dashboard.png
