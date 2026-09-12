# Shanivar Android

Native frontend owned by Codex, built against the Kotlin/Jetpack Compose decision in ../shanivar-vbt/app-shell/CODEX_HANDOFF.md. The old Capacitor scaffold is reference-only.

## Build
Requires JDK 17, Android SDK 35, Gradle 8.9 (wrapper included). Configure ANDROID_HOME or an untracked local.properties with sdk.dir.
From the repository root on Windows, run `./android-app/gradlew.bat -p android-app assembleDebug testDebugUnitTest lintDebug`. On macOS/Linux, use `bash android-app/gradlew` with the same arguments.
Debug API: http://10.0.2.2:5000/api (Android emulator host). Override debug builds with -PshanivarApiUrl=http://10.0.2.2:5101/api for the isolated local frontend fixture. Release currently names https://shanivar.com/api and must be verified before shipping. Cleartext traffic is allowed only in the debug manifest.

## Content
Run node scripts/compile-content.cjs from this directory to regenerate the 12 technique assets from the Content team's Markdown. It requires verse and named translation fields. The script is packaging, not an independent citation verifier. See ../shanivar-vbt/qa/citation-verification-report.md for content QA.

## Integration status
- Existing Express routes: POST /auth/login, GET /users/me, GET /insights/today.
- Authentication is currently in a ViewModel and survives activity recreation. App process death requires sign-in again; persistent secure session storage is outstanding. Signing out invalidates pending personal-note responses.
- Routine preferences and reflections are local to the device. Alarms, app locks, paid access and WhatsApp are not activated by saving preferences.
- Daily note fallback is labelled General reflection. The current backend does not establish chart/transit accuracy.
- Practice assets use technique slugs, while the legacy completion endpoint expects database practice IDs. Do not send slugs to that endpoint or award local XP/streak as if it were a server confirmation.
- DisciplineGateway is a proposed native UI seam; Claude must provide the implementation and agree its permission/scheduling/release semantics.
- Android 14 managed-emulator rendering is available. See [LOCAL_TESTING.md](LOCAL_TESTING.md) for automated checks, interactive Android Studio testing, fixture credentials and screenshot/report locations. Windows security blocked the Robolectric native graphics DLL, so JVM UI tests use legacy graphics; real visual checks run on the Android emulator.

Full direction and release evidence requirements: PRODUCT_DIRECTION.md.

## Messages from Shanivar

The Compose reflection scroller is implemented against Claude's /api/reflections contract. See [REFLECTION_SCROLLER_TESTING.md](REFLECTION_SCROLLER_TESTING.md) for the local mock flow and [REFLECTION_SCROLLER_PLAN.md](REFLECTION_SCROLLER_PLAN.md) for implementation scope. Release evidence and remaining device gates are tracked separately in REFLECTION_SCROLLER_QA.md.
