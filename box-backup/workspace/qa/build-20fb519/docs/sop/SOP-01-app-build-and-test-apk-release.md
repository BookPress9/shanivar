# SOP-01 — App build and test-APK release

**Purpose:** Build the Android debug APK and hand it to testers by sideloading. Internal testing only; there is no signed release or Play listing (DEV-17, `SHANIVAR_TECHNICAL_MAP.md:280`).
**Owner:** Founder approves each release; the agent prepares. Lead integrator: Codex (`AGENTS.md:5`).

## Preconditions
- Clean understanding of `git status`; other agents' uncommitted work is not touched.
- Only a debug build type exists, with `applicationIdSuffix ".dev"` (`android-app/app/build.gradle.kts:35-42`); versionName `0.1.0`, versionCode 1 (`:13-14`).
- `ROUTINE_V3` defaults to `false` (`:33`). Decision 0014 keeps V1Home as launch path until the Routine UI is device-gated.
- Practice content in the build is internal-test status; body-scan sitting is NOT reviewed (`PRACTICE_SAFETY.md:55`).
- UNRESOLVED: Java/SDK toolchain for building on the agent box; the founder builds on Windows per `DEVICE_DISTRIBUTION.md`.

## Steps
1. Run checks first: `testDebugUnitTest lintDebug` (see `TESTING.md` §7). Record result and date; do not claim hardware checks.
2. **Temporary flag (only when the founder wants Routine V3 on the test phone):** flip `ROUTINE_V3` to `true` in `android-app/app/build.gradle.kts:33` in the working tree **only to build**. **Never commit it.** After the build, restore it to `false` and confirm `git diff android-app/app/build.gradle.kts` is empty.
3. Build: `./android-app/gradlew.bat -p android-app assembleDebug "-PshanivarApiUrl=<api>"` (`DEVICE_DISTRIBUTION.md:44`). Output `android-app/app/build/outputs/apk/debug/app-debug.apk`.
4. Record size and SHA-256 of the APK, and the commit hash plus whether the flag was on.
5. **Draft GitHub release** on `BookPress9/shanivar`: create it as a DRAFT, attach the APK, state "internal test build, debug, flag state, commit, SHA-256, not reviewed content". Never publish a release or mark it latest without founder approval. Creating even a draft needs the founder's explicit go for this specific release.
6. **Sideload on the phone:** download the APK on the device; allow "install unknown apps" for the browser/file manager; install. Minimum Android 8.0 (API 26).
7. **Uninstall the old build first** when moving between builds: remove the previous Shanivar install (package `com.shanivar.nativeapp.dev`) before installing, then grant the needed access again (usage access, overlay, notifications, exact alarm, activity recognition). Local Sadhana data is lost on uninstall by design.
8. Run the device checks in `ROUTINE_V3_TEST_GUIDE_2026-09-30.md` and the enforcement protocol; log results in a dated QA file (pattern `ROUTINE_V3_DEVICE_QA_2026-10-05.md`).

## Approval gates
Founder: each release/draft release; any flag flip meant for testers; sharing the APK outside the founder's devices; anything public. No push or release without the word "push"/"release" for that action (SOP-08).

## Rollback
Uninstall the build and reinstall the previous APK from its draft release; delete the draft release only with founder approval. No server state changes with an APK.

## Evidence / links
`android-app/DEVICE_DISTRIBUTION.md`, `build.gradle.kts`, `ROUTINE_V3_DEVICE_QA_2026-10-05.md`, `VBT_BREATHING_RELEASE_EVIDENCE_2026-09-25.md`, the draft release URL (add when created).
Known conflict: default `API_URL` is `https://api.thehybridmonk.com/api` (`android-app/app/build.gradle.kts:16`) and that host was NXDOMAIN (`SHANIVAR_TECHNICAL_MAP.md:280`); testers' builds pass `-PshanivarApiUrl`. UNRESOLVED: which API URL is correct for a tester build today?
