# VBT breathing: release evidence

Date: 25 September 2026. Records the implementation of
[the handoff](CLAUDE_VBT_BREATHING_HANDOFF_2026-09-25.md) against
[the test guide](VBT_BREATHING_TEST_GUIDE_2026-09-25.md).

Content version: `vbt-breathe-2026-09-25.1`, packaged at `app/src/main/assets/breathing-v1.json`,
byte-identical to the approved seed `shanivar-vbt/content/breathing-mvp-v1.json`.

**Read the gate table before quoting anything else here.** The internal feature is built and its
automated checks pass. Three release gates are NOT RUN, and one of them — physical-device behaviour —
cannot be closed on this machine at all, because no emulator or managed-device task may run here.

## Gate table

| Gate | Status | Evidence |
|---|---|---|
| Catalogue / schema / source labels | **PASS** | `BreathingCatalogTest`, 14 tests against the packaged asset: eight unique ids, complete attribution on every sourced practice, `external_grounding` claims no verse, stage sums exact at 60/180/300s, banned-phrase scans for prescribed pacing and physiological promises |
| Script comfort / tradition review for exact version | **NOT RUN** | Requires a reviewer familiar with the cited tradition and a qualified breathing/health practitioner. Neither has happened. The asset's own status string is `implementation_seed_review_before_public_release`, and `reviewedForPublicRelease` returns false — a schema pass cannot flip it |
| Unit / recommender / state / regression tests | **PASS** | 160 tests, 0 failures, 0 skipped (whole app; 66 of them new). Fixtures R01-R20, T01-T07/T12, S01-S05/S07-S10/S14, and the I09/I12/I14 integration cases |
| Lint and APK build | **PASS** | `lintDebug` 0 errors; `assembleDebug` produced a 34,445,969-byte APK |
| Physical alarm / focus / background / keyguard tests | **NOT RUN** | No device was attached, and no emulator may run on this machine. Everything about real audio stopping, lock-screen behaviour, process death and OEM battery managers is unverified |
| Accessibility and language claim checks | **PARTIAL** | Structural only: no per-second live region on the countdown, polite announcement on stage text, no mandatory animation, no microphone/camera/health permission added. TalkBack order and 200% text layout need a device. Hindi guidance is not claimed anywhere; English is the only script shipped |
| Local privacy / deletion checks | **PASS** | `BreathingUiTest.i14_...` proves `clearAll()` empties the breathing namespace and leaves `rhythm` and `focus_guard` untouched; `f_theFeltWorse...` proves no feeling label reaches storage. Journal capped at 100 records. No network traffic inspection was needed — the feature makes no network calls |
| Known limitations disclosed | **PASS** | Listed below |

## Commands actually run

From the repository root, PowerShell:

```powershell
./android-app/gradlew.bat -p android-app testDebugUnitTest lintDebug --console=plain
./android-app/gradlew.bat -p android-app assembleDebug "-PshanivarApiUrl=https://hybridmonk-api.onrender.com/api" --console=plain
```

Results:

- `testDebugUnitTest`: **160 tests, 0 failures, 0 skipped** (counted from
  `app/build/test-results/testDebugUnitTest/TEST-*.xml`, not from a previous CLAUDE.md figure).
  Before this feature the suite was 94; the 66 new tests are `BreathingCatalogTest` (14),
  `BreathingRecommenderTest` (24), `BreathingScheduleTest` (7), `BreathingSessionTest` (13) and
  `BreathingUiTest` (8), with shared fixtures in `BreathingFixtures.kt`.
- `lintDebug`: **0 errors**. Report: `app/build/reports/lint-results-debug.html`.
- `assembleDebug`: **BUILD SUCCESSFUL**.
  - Size: `34,445,969` bytes
  - SHA-256: `5DCB45BB733E88D5053654EAE9B8013F99D4071A3BBAE50C452D7FDF1FE71737`
  - Built: 25 September 2026, 22:53 local
- `npm test` in `backend` was **not run and was not needed**: this feature adds no backend route,
  no schema change and no network call. Nothing in `backend/` was touched.
- No instrumented task was run. `connectedDebugAndroidTest` needs a physical phone;
  `pixel2Api34DebugAndroidTest` is forbidden on this machine and was not attempted.

## What was built

Pure, JVM-testable core:

- `BreathingModels.kt` — practices, sources, stage plans, duration resolution. Carries no field that
  could express a prescribed breath phase.
- `BreathingSchedule.kt` — the wake/intended-sleep window maths, with the 360-1200 minute plausibility
  guard. Reads a separate intended-sleep preference and never reinterprets `Rhythm.quietTime`.
- `BreathingRecommender.kt` — the deterministic candidate tables, hard constraints, and reason strings
  that describe what the person chose.
- `BreathingSessionState.kt` — the session reducer, monotonic-clock based, with idempotent terminal
  transitions and a bounded journal.

App layer:

- `BreathingCatalog.kt` — asset loading and validation; a content mistake fails the load rather than
  shipping something that reads as authoritative.
- `BreathingPreferences.kt` — its own SharedPreferences namespace; sound consent, prior breath
  comfort, the optional schedule, the journal, and the in-flight marker.
- `BreathingSessionOwner.kt` — the single session owner, process-scoped (see
  [decision 0012](../docs/decisions/0012-breathing-session-owner-is-process-scoped.md)).
- `BreathingTonePlayer.kt` — one synthesised fading tone, played once per practice stage.
- `BreathingScreen.kt` — selection, player, source drawer, quiet endings.

Host integration:

- `V1Home.kt` — third Home card, a pre-focus "Take a moment first" entry that opens Prepare only, and
  a Settings block for the optional schedule plus "Clear practice history".
- `BlockOverlayActivity.kt` — inline entry; this host owns the exit and it goes to device Home.
- `AlarmActivity.kt` — an optional "Take a quiet minute" on the verified done screen, and an
  `interrupt` call so a firing alarm stops practice cues. `dismissAlarm()`'s ordering is unchanged.
- `LocalPauseScreen.kt` — the four random prompts are gone. One of them ("Breathe out slower than you
  breathe in") was unreviewed breath advice shown before sign-in and inside the block overlay. The
  prompt now comes from the reviewed catalogue, and shows nothing at all if content is unavailable.

## Known limitations

1. **Nothing has run on a physical phone.** The tone, the lock screen, real process death, audio
   focus, and alarm-over-tone behaviour are all unverified. A fake player receiving `stop()` is not
   proof that a speaker went quiet, and this build has not even got that far — it has unit tests.
2. **No content or tradition review.** The eight practices are an implementation seed. The adaptation
   notes and the beginner instructions have had no expert reading.
3. **English only.** Hindi is structurally supported (the catalogue declares a language and a fallback
   statement) but no reviewed Hindi script exists, and the app claims none.
4. **Screen-off practice is silent.** After the instructions there is no audio guidance. Voice-guided
   background playback is a separate capability needing real recordings and the media lifecycle; it
   was not bolted onto the alarm foreground service.
5. **The tone is synthesised, not a licensed asset.** Deliberate — it keeps the sound unambiguously
   original and makes "asset unavailable" a condition the code can actually detect.
6. **Strings are in Kotlin, not `strings.xml`.** The rest of this app's V1 surface is the same way, so
   this follows the existing pattern rather than introducing a second one. Externalising them is a
   prerequisite for the Hindi work, not for this slice.
7. **The journal is not surfaced in the UI.** It is written, capped and deletable; there is no history
   screen yet. "Clear practice history" works regardless.
8. **`BreathingSessionOwner` is a process singleton**, which means tests must reset it. See decision
   0012 for why a ViewModel could not satisfy the single-session requirement.

## Before this reaches anyone outside the team

In order:

1. Run the eight-check physical-device protocol in `ENFORCEMENT_STANCE_2026-09-12.md`, which is still
   outstanding for alarms and focus too — this feature does not change that gate, it joins it.
2. On the same device, walk section D and E of the test guide: tone stop, lock screen, background,
   keyguard, process death, alarm-during-practice.
3. Get the content review. Record reviewer, date, exact content version, changes and unresolved
   points — not the word "reviewed".
4. Then observe 8-12 consenting adults on the selection and stop flows, per the guide's pilot gate.

Do not change a NOT RUN to PASS because the APK builds.
