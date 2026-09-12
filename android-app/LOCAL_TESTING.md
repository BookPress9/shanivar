# Local Android frontend testing

Run commands from the repository root. Requires Node.js, JDK 17, Android SDK 35, and emulator acceleration. Android Studio can open the android-app directory directly.

## Automated emulator check

Terminal 1 starts the isolated frontend fixture (no npm dependencies):

```powershell
node android-app/scripts/mock-api.cjs
```

Terminal 2 builds, runs local tests and lint, then boots the managed Pixel 2 / Android 14 emulator:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA/Android/Sdk"
./android-app/gradlew.bat -p android-app assembleDebug testDebugUnitTest lintDebug pixel2Api34DebugAndroidTest "-PshanivarApiUrl=http://10.0.2.2:5101/api" --console=plain
```

The first run downloads the emulator image. Subsequent runs reuse it. The render test exercises the real MainActivity and saves screenshots; the mock account test checks rejected credentials, successful login, profile/streak, daily note, rhythm save, reflection response and sign-out. The mock account test is explicitly skipped when built without this fixture URL.

Reports: app/build/reports/tests/testDebugUnitTest/index.html, app/build/reports/lint-results-debug.html, and app/build/reports/androidTests/managedDevice/debug/allDevices/index.html (relative to android-app).
Screenshots: app/build/outputs/managed_device_android_test_additional_output/debug/pixel2Api34/.

## Explore interactively

Keep the fixture server running. Start an emulator from Android Studio Device Manager, then:

```powershell
./android-app/gradlew.bat -p android-app installDebug "-PshanivarApiUrl=http://10.0.2.2:5101/api"
& "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" shell am start -n com.shanivar.nativeapp.dev/com.shanivar.nativeapp.MainActivity
```

Use observer@example.com / local-practice. Any other password shows the error state. New installs start with language, account, optional birth context and capability onboarding. Explore Rhythm, Today, Focus, the movement alarm, the reflection scroller, the 12 practices, pause/resume, early exit and source text. Stopping the mock server lets you check connection errors.

For an attached USB Android phone, use adb reverse tcp:5101 tcp:5101 and build with -PshanivarApiUrl=http://127.0.0.1:5101/api. The host-address-specific automated mock test is skipped on that build.

## Switch to Claude's backend

Omit shanivarApiUrl to restore http://10.0.2.2:5000/api for the emulator, or supply the actual development API URL. The override affects debug builds only. Start the backend and use an account registered there. The fixture never writes to the backend.

Account registration/profile, daily note, rhythm preference, focus-session and alarm routes are connected through the Express API to Supabase. Android enforces focus locally and schedules the alarm locally by design, then confirms/records those actions through the API. Practice completion/streak sync, payments and WhatsApp delivery remain pending. The mock note is a general reflection and establishes no chart accuracy.


## Verified on 2026-09-12

The debug APK, JVM/Robolectric suite, Android lint and Android 14 / Pixel 2 managed-device suite pass. The managed test covers the real Compose activity against the isolated fixture; the backend integration smoke covers a disposable account against Supabase. Hardware step sensing, exact alarm behavior while locked, reboot restoration and OEM-specific background policy still require a physical phone.

## Reflection scroller fixture

The local mock now includes reflection sessions. Today → Reflect with Shanivar opens the Compose flow after sign-in. See [REFLECTION_SCROLLER_TESTING.md](REFLECTION_SCROLLER_TESTING.md) for its checks and the alternative fixture port 5102.
