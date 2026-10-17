# Messages from Shanivar — local testing and integration

Implementation plan: [REFLECTION_SCROLLER_PLAN.md](REFLECTION_SCROLLER_PLAN.md). Contract owner: Claude; source contracts remain unchanged.

## Interactive preview

From the repository root, start the frontend fixture in terminal 1:

```powershell
$env:PORT='5102'
node android-app/scripts/mock-api.cjs
```

In terminal 2, with an emulator running in Android Studio:

```powershell
$env:ANDROID_HOME="$env:LOCALAPPDATA/Android/Sdk"
./android-app/gradlew.bat -p android-app installDebug "-PshanivarApiUrl=http://10.0.2.2:5102/api"
& "$env:ANDROID_HOME/platform-tools/adb.exe" shell am start -n com.shanivar.nativeapp.dev/com.shanivar.nativeapp.MainActivity
```

Sign in with observer@example.com / local-practice. Today → Messages from Shanivar → Reflect with Shanivar. Read the first-use processing disclosure and begin. Choose any option, including Prefer not to share, or type 1–1,000 trimmed characters. Send confirms the response; Continue reveals the next card. Five accepted responses end the session. History remains readable; Back preserves progress; End closes the server session; Delete removes it after confirmation. Offline practices are always reachable.

The fixture is in scripts/mock-reflections.cjs and deliberately labels its cards general_fallback. It does not perform chart calculations, call Gemini, or simulate OS enforcement. To use Claude's backend, build with -PshanivarApiUrl=http://10.0.2.2:5000/api and sign in with an account on that backend. No production credentials are bundled.

## Automated checks

```powershell
./android-app/gradlew.bat -p android-app assembleDebug testDebugUnitTest lintDebug pixel2Api34DebugAndroidTest "-PshanivarApiUrl=http://10.0.2.2:5102/api" --console=plain
```

Keep the fixture running for NativeMockLoginTest; it opts in only for fixture ports 5101/5102. Other device tests use explicit in-process fixtures and no backend. JVM state tests exercise lost responses, retry-key reuse, stale versions, invalid drafts, rate limits, completion, deletion and account isolation. Device screenshots go to app/build/outputs/managed_device_android_test_additional_output/debug/pixel2Api34/reflection-*.png.

## Native discipline seam

ReflectionScreen takes ReflectionEntryPoint and a ReflectionProtectionReader. The new BlockOverlayActivity button passes BLOCKED_APP_SURFACE through ReflectionNavigation, a same-process state holder. Today passes TODAY. Both use DisciplineEngine.create(...).reflectionReader(); no request to start or release protection is made. No Intent extra or external deep-link string is converted to a blocked-app marker. MainActivity still ignores external scroller extras.

The reader exposes only status(). The scroller contains no focus start/release, entitlement mutation or alarm dismissal calls. Status is refreshed on entry/resume and periodically while visible. Claude added the engine and blocked-app Activity during this implementation turn. Their integration is now wired; the scroller shows the engine status without inventing a countdown. Real OS behavior, permission changes, background/resume and alarm non-interference still require supported-device verification.

## Persistence and lifecycle

One owner-bound encrypted snapshot is stored using Android Keystore AES-GCM and AtomicFile in noBackupFilesDir. It contains the current server envelope/history, draft, disclosure state and exact pending operation key/body; never a bearer token. It survives app process death. Existing app authentication is memory-only, so the user must sign in again after process death to resume. Logout clears local reflection data; binding a different owner discards the prior cache. An old date stays labelled with its pinned timezone/context; starting fresh is explicit.

A network timeout never locally advances the session. Recover performs GET and checks answered history/version before replaying an unchanged pending response. Generation errors retry /retry rather than sending the answer twice. Leaving the screen is local and immediate, even when the network is busy.

## Release evidence

Claude's live backend results are recorded in ../ai_org/reflection_scroller_squad.md. Codex's local evidence is recorded in REFLECTION_SCROLLER_QA.md. These are different evidence sets. No native fixture test is proof of real app blocking, alarm isolation, astronomical correctness or live generated-content quality.
