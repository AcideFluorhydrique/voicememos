#!/usr/bin/env bash
# Install every APK in a folder on the running emulator, start it and check that it stays up.
#
# Usage: emulator_smoke.sh <apk dir> <results dir>
#
# Every runtime permission is granted on install, the memos list then queries media store on
# the first frame, which is where android 10 went down in issue 2. A short wav is left in the
# app folder beforehand so the list has a recording that this install does not own, the same
# as one an earlier install left behind.
#
# For each APK the results dir gets a screenshot, the full logcat and the crash buffer. The
# script exits non zero if any APK crashed or was not running at the end.

set -uo pipefail

APK_DIR=$1
OUT_DIR=$2
WAIT_SECONDS=20

mkdir -p "$OUT_DIR"

BUILD_TOOLS=$(ls -d "$ANDROID_HOME"/build-tools/*/ | sort -V | tail -1)
API=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
echo "emulator API level $API, build tools $BUILD_TOOLS"

# the app records into Recordings from android 12, into Music before that
if [ "$API" -ge 31 ]; then FOLDER=/sdcard/Recordings/RecorderApp
else FOLDER=/sdcard/Music/RecorderApp; fi

seed_recording() {
	local wav="$OUT_DIR/seed.wav"
	# one second of a 440 Hz tone, 16 kHz mono 16 bit
	python3 - "$wav" <<-'EOF'
		import math, struct, sys, wave
		with wave.open(sys.argv[1], 'wb') as w:
		    w.setnchannels(1); w.setsampwidth(2); w.setframerate(16000)
		    w.writeframes(b''.join(struct.pack('<h', int(8000 * math.sin(2 * math.pi * 440 * i / 16000)))
		                           for i in range(16000)))
	EOF
	adb shell mkdir -p "$FOLDER"
	adb push "$wav" "$FOLDER/Left behind.wav" >/dev/null
	rm -f "$wav"
	# android 10 still takes the scanner broadcast, later versions scan the volume on request
	adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
		-d "file://$FOLDER/Left%20behind.wav" >/dev/null 2>&1 || true
	adb shell content call --method scan_volume --uri content://media --arg external_primary \
		>/dev/null 2>&1 || true
	sleep 3
	echo "seeded a recording in $FOLDER"
}

seed_recording

failed=0

for apk in "$APK_DIR"/*.apk; do
	[ -e "$apk" ] || { echo "no APK in $APK_DIR"; exit 1; }
	label=$(basename "$apk" .apk)
	out="$OUT_DIR/$label"
	mkdir -p "$out"

	pkg=$("$BUILD_TOOLS/aapt2" dump packagename "$apk" 2>/dev/null | tr -d '\r')
	echo "::group::$label ($pkg) on API $API"

	adb uninstall "$pkg" >/dev/null 2>&1 || true
	if ! adb install -r -g "$apk"; then
		echo "::error::$label could not be installed on API $API"
		failed=1
		echo "::endgroup::"
		continue
	fi

	adb logcat -b all -c
	adb shell monkey -p "$pkg" -c android.intent.category.LAUNCHER 1 >/dev/null

	crashed=0
	for _ in $(seq "$WAIT_SECONDS"); do
		sleep 1
		if adb logcat -d -b crash | grep -q "Process: $pkg"; then crashed=1; break; fi
	done

	adb exec-out screencap -p > "$out/screen.png"
	adb logcat -d -b crash > "$out/crash.txt"
	adb logcat -d > "$out/logcat.txt"
	running=$(adb shell pidof "$pkg" | tr -d '\r')

	if [ "$crashed" = 1 ]; then
		echo "::error::$label crashed on API $API"
		cat "$out/crash.txt"
		failed=1
	elif [ -z "$running" ]; then
		echo "::error::$label was not running ${WAIT_SECONDS}s after it was started on API $API"
		failed=1
	else
		echo "$label is up on API $API (pid $running)"
	fi

	adb uninstall "$pkg" >/dev/null 2>&1 || true
	echo "::endgroup::"
done

exit "$failed"
