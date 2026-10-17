#!/usr/bin/env bash
# Boot the shanivar_qa AVD headless. Log: /workspace/logs/emulator.log
set -u
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}" ANDROID_SDK_ROOT="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"
AVD=shanivar_qa; LOG=/workspace/logs/emulator.log; TIMEOUT="${BOOT_TIMEOUT:-300}"
mkdir -p /workspace/logs
# self-heal KVM access (device node may be recreated on box restart; no udev here)
# box is in group kvm(103); ACL is a belt-and-braces fallback (it has been seen to get reset).
sudo -n setfacl -m u:"$(id -un)":rw /dev/kvm 2>/dev/null || true
# If this shell predates the group membership, launch the emulator via `sg kvm`.
if id -nG | tr ' ' '\n' | grep -qx kvm; then RUN=(bash -c); else RUN=(sg kvm -c); fi
if adb devices | grep -q '^emulator-5554[[:space:]]*device'; then echo "emulator-5554 already running"; exit 0; fi
START=$(date +%s)
nohup "${RUN[@]}" "exec emulator -avd $AVD -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -memory 2048 -accel on -no-snapshot -port 5554" >"$LOG" 2>&1 &
echo "emulator launcher pid $!"
adb start-server >/dev/null 2>&1
timeout "$TIMEOUT" adb -s emulator-5554 wait-for-device || { echo "timeout waiting for device; see $LOG" >&2; exit 1; }
while [ "$(adb -s emulator-5554 shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
  [ $(( $(date +%s) - START )) -ge "$TIMEOUT" ] && { echo "boot timeout after ${TIMEOUT}s; see $LOG" >&2; exit 1; }
  sleep 3
done
echo "boot_completed=1 in $(( $(date +%s) - START ))s (serial emulator-5554)"
