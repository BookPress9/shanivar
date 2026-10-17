#!/usr/bin/env bash
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"; export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb -s emulator-5554 emu kill && echo "emulator-5554 stopped"
