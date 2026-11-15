#!/usr/bin/env bash
# Runs inside the emulator step of device.yml: installs the signed release APK on a display shaped
# like the Connect display (2560 x 1440 px at 160 DPI), plays scenario 1 unattended while recording
# the screen, takes screenshots, and fails if the app crashed or is gone.
set -euo pipefail

OUT="$RUNNER_TEMP/device"
APK="$RUNNER_TEMP/DND_DND_release.apk"
PKG=io.github.dnddataneverdie.dnd
ACT="$PKG/dnd.app.MainActivity"
mkdir -p "$OUT"

# Waits until the app has logged a line containing $1 (tag DND), at most $2 seconds.
wait_for() {
  local deadline=$((SECONDS + $2))
  until adb logcat -d -s DND:I | grep -q "$1"; do
    if [ $SECONDS -ge $deadline ]; then
      echo "::error::timed out after $2 s waiting for '$1'"
      return 1
    fi
    sleep 1
  done
  echo "seen: $1 (t=${SECONDS}s)"
}

shot() {
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null || true
  sleep 1
  adb exec-out screencap -p > "$OUT/$1.png"
  echo "screenshot $1.png"
}

finish() {
  adb logcat -d > "$OUT/logcat.txt" || true
}
trap finish EXIT

# Let the freshly booted system settle (the launcher otherwise raises an ANR dialog on the slow
# software-rendered emulator), and keep system error dialogs off the recording. App crashes are
# still caught below from the crash buffer and logcat.
adb shell settings put global hide_error_dialogs 1
sleep 45
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null

adb shell wm size 2560x1440
adb shell wm density 160
adb shell wm size
adb shell wm density
adb install -r "$APK"
# The app hides the system bars; skip the system's one-time "Viewing full screen" hint.
adb shell settings put secure immersive_mode_confirmations confirmed
adb logcat -c

# Unattended scenario 1 (time-lapse, cards, 상세 정보), recorded at 1920x1080.
adb shell screenrecord --size 1920x1080 --time-limit 0 /sdcard/dnd_s1.mp4 &
REC=$!
sleep 2
adb shell am start -W -n "$ACT" --es scenario s1 --ez autoplay true
wait_for "replay playing" 180
sleep 8
shot 01_timelapse
wait_for "cards shown" 120
sleep 2
shot 02_cards
wait_for "evidence shown" 30
sleep 2
shot 03_evidence
wait_for "tour done" 30
sleep 1
# SIGINT lets screenrecord finish the mp4.
adb shell 'kill -2 $(pidof screenrecord)' || true
for _ in $(seq 1 30); do kill -0 "$REC" 2>/dev/null || break; sleep 1; done
sleep 2
adb pull /sdcard/dnd_s1.mp4 "$OUT/dnd_s1.mp4"

# Smoke test: no crash from the app, and the process is still alive.
if adb logcat -d -b crash | grep -q "$PKG"; then
  adb logcat -d -b crash | tail -40
  echo "::error::the app crashed (crash buffer names $PKG)"
  exit 1
fi
if adb logcat -d | grep -A 3 "FATAL EXCEPTION" | grep -q "$PKG"; then
  echo "::error::FATAL EXCEPTION from $PKG"
  exit 1
fi
PID=$(adb shell pidof "$PKG" || true)
if [ -z "$PID" ]; then
  echo "::error::the app process is gone"
  exit 1
fi
echo "smoke ok: process $PID alive, no crash"

# The vehicle card's detail screen (the unattended tour does not open it): back to the cards, then
# a tap on the vehicle card's "상세 정보" button (its centre on the 2560x1440 display).
adb shell input keyevent 4
sleep 2
adb shell input tap 1550 1017
sleep 3
shot 05_vehicle_detail

# Scenario 3 opens the weekly summary.
adb logcat -c
adb shell am start -S -W -n "$ACT" --es scenario s3
wait_for "ready scenario=s3" 120
sleep 3
shot 04_weekly
