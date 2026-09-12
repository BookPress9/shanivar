# Shanivar Android

Native frontend owned by Codex, built against the Kotlin/Jetpack Compose decision in ../shanivar-vbt/app-shell/CODEX_HANDOFF.md. The old Capacitor scaffold is reference-only.

## Build
Requires JDK 17, Android SDK 35, Gradle 8.9 (wrapper included). Configure ANDROID_HOME or an untracked local.properties with sdk.dir.
From the repository root on Windows, run `./android-app/gradlew.bat -p android-app assembleDebug testDebugUnitTest lintDebug`. On macOS/Linux, use `bash android-app/gradlew` with the same arguments.
Debug API: http://10.0.2.2:5000/api (Android emulator host). Override debug builds with -PshanivarApiUrl=http://10.0.2.2:5101/api for the isolated local frontend fixture. Release names https://api.thehybridmonk.com/api; publish that DNS/API endpoint before a release build is distributed. Cleartext traffic is allowed only in the debug manifest.

## Content
Run node scripts/compile-content.cjs from this directory to regenerate the 12 technique assets from the Content team's Markdown. It requires verse and named translation fields. The script is packaging, not an independent citation verifier. See ../shanivar-vbt/qa/citation-verification-report.md for content QA.

## Integration status (verified 2026-09-12)
- The Express API uses the Supabase Postgres connection for account, profile, rhythm, focus, alarm, reflection and Today data. A disposable-account integration smoke test covers the real hosted database.
- Compose onboarding calls registration/profile endpoints and preserves English/Hindi plus optional birth inputs. Rhythm loads and saves account preferences. Focus calls live status/start/release routes. Alarm creation is scheduled locally and confirmed to the API only after Android accepts it.
- The movement-first dismissal surface uses the hardware step detector with a separated-motion fallback and records the outcome on a best-effort basis. QR/photo code remains only for legacy records and rollback.
- Authentication is currently in a ViewModel and survives activity recreation. App process death requires sign-in again; persistent secure session storage is outstanding. Signing out invalidates pending personal-note responses.
- Practice-player notes and completion remain local. Paid access and WhatsApp delivery still need their product/backend release contracts.
- Daily note fallback is labelled General reflection. The current backend does not establish chart/transit accuracy.
- Practice assets use technique slugs, while the legacy completion endpoint expects database practice IDs. Do not send slugs to that endpoint or award local XP/streak as if it were a server confirmation.
- Android 14 managed-emulator rendering and interaction checks are available. Real-device sensor, lock-screen, OEM battery-policy and reboot evidence remains a release gate. See [LOCAL_TESTING.md](LOCAL_TESTING.md) and [DEVICE_DISTRIBUTION.md](DEVICE_DISTRIBUTION.md).

Full direction and release evidence requirements: PRODUCT_DIRECTION.md.

## Messages from Shanivar

The Compose reflection scroller is implemented against Claude's /api/reflections contract. See [REFLECTION_SCROLLER_TESTING.md](REFLECTION_SCROLLER_TESTING.md) for the local mock flow and [REFLECTION_SCROLLER_PLAN.md](REFLECTION_SCROLLER_PLAN.md) for implementation scope. Release evidence and remaining device gates are tracked separately in REFLECTION_SCROLLER_QA.md.
