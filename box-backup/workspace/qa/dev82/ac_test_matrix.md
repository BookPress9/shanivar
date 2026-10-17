# DEV-82 — AC-to-test matrix (all DEV tickets)

Generated 2026-10-17 (Europe/Berlin) by Shanivar Tester from Jira JQL `project = DEV ORDER BY key ASC` (89 issues) and the repo worktrees. No source code changed; no tests written for unconfirmed tickets.

**Builds referenced:** `main@20fb519` = /workspace/shanivar-v3 (dev-38-outbox-gaps, 518 unit tests, 0 fail, 1 skipped = SupabaseSyncTest.e2eGuestPushReadBack, run 21:55); `dev39@8e122a8` (512/0/1, 21:59); `dev42@ba38de6` (507/0/0 Android + pgTAP 01–05 on local stack); `dev45@2fce42d` (in QA now); `dev47@1e69cba` (548/0/1 developer run 22:36, not QA-reviewed); `dev54@7a56cf6` + uncommitted work. Android test paths are under `android-app/app/src/test/java/com/shanivar/nativeapp/`; pgTAP under `supabase/tests/`.

**Status key:** automated-pass · manual-only (needs a human check; result noted) · real-device-only (needs a physical phone; NOT RUN) · emulator-pending (DEV-71/75 cases planned for the emulator; run when PM confirms it is booted — emulator results never replace the real-device rows) · no test (gap) · pending (in QA now, DEV-45 only). `(derived)` = Jira has no AC section; AC inferred from the description and **returned to the owner to confirm** (DEV-82 edge case). `[DoD]` = row added by QA under the new definition of done (real-device proof for alarms/protection).


## Summary counts

| Epic | ACs | automated-pass | manual-only | real-device-only (NOT RUN) | emulator-pending | no test (gap) | pending |
|---|---|---|---|---|---|---|---|
| A — Strategy & measurement | 4 | 0 | 3 | 0 | 0 | 1 | 0 |
| B — Protection | 66 | 19 | 7 | 17 | 12 | 11 | 0 |
| C — Routine | 61 | 13 | 6 | 8 | 9 | 23 | 2 |
| D — Sadhana | 59 | 24 | 10 | 3 | 0 | 22 | 0 |
| E — Security, privacy & reliability | 117 | 20 | 29 | 3 | 0 | 55 | 10 |
| F — QA & release | 23 | 0 | 6 | 6 | 0 | 11 | 0 |
| G — CS & growth | 12 | 0 | 3 | 0 | 0 | 9 | 0 |
| No epic | 1 | 0 | 1 | 0 | 0 | 0 | 0 |
| **Total** | **343** | **76** | **65** | **37** | **21** | **132** | **12** |

Rows: 343 = 315 Jira/derived ACs + 28 planned DEV-71/DEV-75 device cases (17 + 15 rows, minus the 4 umbrella ACs already counted).

Tickets: 89 (82 work tickets + 7 epic containers). Tickets whose ACs were derived because Jira has no AC section: 21.

## Epic A — Strategy & measurement ([DEV-56](https://shanivar.atlassian.net/browse/DEV-56))

| Ticket | Jira status | AC | Test (file · class.method / pgTAP / manual) | Status |
|---|---|---|---|---|
| [DEV-56](https://shanivar.atlassian.net/browse/DEV-56) epic container | Open | — no ACs (container) | — | — |
| [DEV-63](https://shanivar.atlassian.net/browse/DEV-63) Publish the canonical Notion page "Shanivar Product Thesis — | Open | Every §2A section present; epics linked; decision log; founder approves | Doc review of Notion thesis (sections present; approval recorded 17 Oct) | manual-only |
| [DEV-64](https://shanivar.atlassian.net/browse/DEV-64) Map existing Shanivar features against the protect → act → r | Open | Every journey step has ≥1 row; missing steps link tickets; diagram | Doc review (thesis §5 has table + mermaid) | manual-only |
| [DEV-65](https://shanivar.atlassian.net/browse/DEV-65) Define the event dictionary, analytics contract and WIAC (No | Open | Doc reviewed by FE/BE/Tester/CS; every §6 metric traceable to events; privacy signed | Doc review — not written | no test (gap) |
| [DEV-66](https://shanivar.atlassian.net/browse/DEV-66) Set baselines, experiment design and an assumption register | Open | Register exists, linked; each hypothesis has a test method | Doc review (thesis §9 lists H1–H8 with tests) | manual-only |

_Epic subtotal:_ 4 ACs — manual-only 3, no test (gap) 1

## Epic B — Protection ([DEV-57](https://shanivar.atlassian.net/browse/DEV-57))

| Ticket | Jira status | AC | Test (file · class.method / pgTAP / manual) | Status |
|---|---|---|---|---|
| [DEV-1](https://shanivar.atlassian.net/browse/DEV-1) FOCUS-1 A delayed START boundary can begin a 24-hour focus s | Completed | Stale START (after window closed) starts nothing | main@20fb519 test/FocusWindowRecurrenceTest.dev1_aStartDeliveredAfterItsWindowClosedHasNoActiveEndToMeasureAgainst | automated-pass |
| ″ |  | On-time START measures correct remaining duration | main@20fb519 test/FocusWindowRecurrenceTest.dev1_anOnTimeStartStillMeasuresToItsOwnWindowEnd | automated-pass |
| ″ |  | Regression test covers delayed delivery | main@20fb519 test/FocusWindowRecurrenceTest.dev1_aLateEndFindsNoActiveOccurrenceSoItReleasesRatherThanSkipping; FocusWindowEndHandlerTest.lateEndDeliveredInsideTheNextOccurrence… | automated-pass |
| ″ |  | [DoD] Delayed/powered-off START on a real phone (guide cases 7–8) | DEV-7/DEV-71 device protocol (ROUTINE_V3_DEVICE_QA_2026-10-05.md) — NOT RUN | real-device-only (NOT RUN) |
| [DEV-2](https://shanivar.atlassian.net/browse/DEV-2) FOCUS-5 Window END calls releaseFocus() unconditionally — bl | Completed | Earlier END does not release a later window's protection | main@20fb519 test/FocusWindowEndHandlerTest.earlierEndOfOverlappingWindowsDoesNotReleaseTheLaterWindow | automated-pass |
| ″ |  | Overlap handled or rejected at save with a reason | main@20fb519 test/RoutineProtectionTest.validateNamesTheTwoOverlappingWindows; FocusSchedulerRegistryTest.replaceAllCapsAtEightAndRejectsOverlap | automated-pass |
| ″ |  | Test: one window ends inside another | main@20fb519 test/FocusWindowEndHandlerTest.innerWindowEndingInsideAnOuterWindowLeavesTheOuterWindowProtected | automated-pass |
| ″ |  | [DoD] Multi-window on a real phone | DEV-7/DEV-71 device protocol (ROUTINE_V3_DEVICE_QA_2026-10-05.md) — NOT RUN | real-device-only (NOT RUN) |
| [DEV-3](https://shanivar.atlassian.net/browse/DEV-3) FOCUS-6 A saved schedule is reported as armed without engine | Completed | Revoked usage/exact-alarm → NEEDS_DEVICE_ACCESS / NEEDS_ATTENTION | main@20fb519 test/FocusScheduleStatusTest.revokedUsageAccessIsNeedsDeviceAccess…; revokedExactAlarmIsNeedsDeviceAccessNotReady | automated-pass |
| ″ |  | ACTIVE only when guard confirms | main@20fb519 test/FocusScheduleStatusTest.savedAndArmedInsideTheWindowWithoutAGuardIsNotActive; savedReadyAndActiveAreDistinctStates; RoutineProtectionTest.saveMessagesNeverClaimActive | automated-pass |
| ″ |  | Repair next-step understandable from state | Manual UX review of repair copy (no test asserts comprehension) | manual-only |
| ″ |  | [DoD] Revoke permissions in Settings on a real phone | DEV-7/DEV-71 device protocol (ROUTINE_V3_DEVICE_QA_2026-10-05.md) — NOT RUN | real-device-only (NOT RUN) |
| [DEV-4](https://shanivar.atlassian.net/browse/DEV-4) FOCUS-4 Scheduled boundaries depend on a nullable in-process | Completed | Cold-process START/END with no session and no network | main@20fb519 test/LocalProtectionTest.startsProtectionWithoutAnyAccountOrNetworkInvolved; FocusWindowEndHandlerTest.endHandlerNamesNoSessionOrApi; BootRescheduleCatchUpTest.catchUpDoesNotRequireASession | automated-pass |
| ″ |  | Immediate local stop works offline | main@20fb519 test/LocalProtectionTest.stopNowIsImmediateAndIdempotent; DisciplineEngineTest.releaseFocusStopsTheGuardEvenWhenTheBackendCallFails; RoutineProtectionTest.stopIsLocalFirstAndSurvivesMirrorFailure | automated-pass |
| ″ |  | Test covers cold process explicitly | main@20fb519 test/LocalProtectionTest.theLocalPathNamesNoApiTypeAtAll (structural) | automated-pass |
| ″ |  | [DoD] Process death / force-stop / airplane mode on a real phone | DEV-7/DEV-71 device protocol (ROUTINE_V3_DEVICE_QA_2026-10-05.md) — NOT RUN | real-device-only (NOT RUN) |
| [DEV-5](https://shanivar.atlassian.net/browse/DEV-5) FOCUS-2 No overnight focus windows — sleep preparation is un | Completed | 22:00→07:00 saves and fires both boundaries | main@20fb519 test/FocusWindowRecurrenceTest.mondayNightOvernightWindowIsActive…; overnightBoundariesAlternateStartThenEndAcrossMidnight; FocusWindowEndHandlerTest.overnightEndReleasesOnTheMorningAfter | automated-pass |
| ″ |  | Day selection applies to start day | main@20fb519 test/FocusWindowRecurrenceTest.theSameOvernightWindowIsInactiveSundayNightUnlessSundayIsSelected; endDaysShiftForwardOnlyForOvernightWindowsAndWrapPastSaturday | automated-pass |
| ″ |  | DST gap/overlap deterministic, no two active copies | main@20fb519 test/FocusWindowRecurrenceTest.daylightSavingTransitionsProduceOneDeterministicBoundary (Berlin 2026-03-29/10-25) | automated-pass |
| ″ |  | Timezone change explains the changed next boundary | main@20fb519 test/FocusWindowRecurrenceTest.boundariesAreRecomputedFromLocalRulesRatherThanStoredInstants (recompute only — no test or UI for the *explanation*) | no test (gap) |
| ″ |  | [DoD] Overnight + DST (25 Oct 2026) + TZ change on a real phone | DEV-7/DEV-71 device protocol (ROUTINE_V3_DEVICE_QA_2026-10-05.md) — NOT RUN | real-device-only (NOT RUN) |
| [DEV-6](https://shanivar.atlassian.net/browse/DEV-6) FOCUS-3 All-day preset is 00:00–23:59 and copy implies persi | Completed | No one-minute daily gap | main@20fb519 test/FocusWindowRecurrenceTest.fullCalendarDayCoversEndOfSelectedDayWithNoOneMinuteGap; fullCalendarDayAllSevenDaysStaysActiveAcrossMidnightWithNoGap; FocusScheduleDraftTest.allDayAndDaysPresetsAreValidFullCalendarDays | automated-pass |
| ″ |  | No UI/marketing string claims continuous/unbreakable protection | No automated copy scan; manual review of app + site + store (DEV-88 checklist) | no test (gap) |
| ″ |  | Known bypasses stay disclosed (force-stop, revoked perms, browsers, 2nd device) | Manual copy review of block overlay/Routine protection screen | manual-only |
| ″ |  | [DoD] Full-day across midnight on a real phone | DEV-7/DEV-71 device protocol (ROUTINE_V3_DEVICE_QA_2026-10-05.md) — NOT RUN | real-device-only (NOT RUN) |
| [DEV-7](https://shanivar.atlassian.net/browse/DEV-7) Run the 8-check physical-device protocol — it has never exec | Pending | Evidence recorded with raw output | Device protocol run log (not started; founder paused 5 Oct) | real-device-only (NOT RUN) |
| ″ |  | Skipped integration test recorded as NOT RUN | QA reporting rule (this plan §5); androidTest classes never executed | real-device-only (NOT RUN) |
| ″ |  | Second OEM covered or support claim narrowed | Device matrix: Pixel + Xiaomi/Realme/Vivo/Samsung class | real-device-only (NOT RUN) |
| [DEV-10](https://shanivar.atlassian.net/browse/DEV-10) Keep essential-access stop, and keep no-snooze on the block  | Open | (derived) Block screen has no snooze / five-more-minutes | No test asserts absence of snooze on BlockOverlayActivity | no test (gap) |
| ″ |  | (derived) Essential-access stop works with no payment, practice or server | main@20fb519 test/LocalProtectionTest.stopNowIsImmediateAndIdempotent; SadhanaFocusLockTest.allModeBlocksEverythingExceptEssentialAndOwn | automated-pass |
| ″ |  | [DoD] Stop path on a real phone, offline, signed out | DEV-7/DEV-71 device protocol (ROUTINE_V3_DEVICE_QA_2026-10-05.md) — NOT RUN | real-device-only (NOT RUN) |
| ″ | | _Note:_ No AC section in Jira — ACs derived from description; return to owner to confirm. | | |
| [DEV-21](https://shanivar.atlassian.net/browse/DEV-21) Reboot catch-up resumes local protection with no session — f | Completed | (derived) Reboot inside a valid window resumes local protection with no account/network | main@20fb519 test/BootRescheduleCatchUpTest.rebootInsideALiveWindowResumesLocalProtectionWithNoSession; catchUpDoesNotRequireASession | automated-pass |
| ″ |  | (derived) Reboot after window ended does not start protection | main@20fb519 test/BootRescheduleCatchUpTest.rebootAfterTheWindowEndedDoesNotStartProtectionButStillRearms; rebootBeforeTheWindowDoesNotStartProtection | automated-pass |
| ″ |  | (derived) Next alarm re-armed either way | main@20fb519 test/BootRescheduleCatchUpTest.catchUpStillRearmsWhenTheDeviceCannotEnforce; BootReconcileActionsTest.appUpdateReconcilesAlarmsAndWindows | automated-pass |
| ″ |  | [DoD] Real reboot inside/after a window | DEV-7/DEV-71 device protocol (ROUTINE_V3_DEVICE_QA_2026-10-05.md) — NOT RUN (DEV-21 row of device checklist) | real-device-only (NOT RUN) |
| ″ | | _Note:_ Passes in unit tests; real-device verification pending under DEV-7. | | |
| [DEV-57](https://shanivar.atlassian.net/browse/DEV-57) epic container | Open | — no ACs (container) | — | — |
| [DEV-67](https://shanivar.atlassian.net/browse/DEV-67) Audit protection capabilities and Android platform constrain | Open | Capability table reviewed by Tester | Doc review | manual-only |
| ″ |  | Every limit has user copy and a test in B5 (DEV-71) | Depends on DEV-71 suite | no test (gap) |
| ″ |  | Feeds Marketing claim checklist (G4/DEV-88) | Doc cross-check | manual-only |
| [DEV-68](https://shanivar.atlassian.net/browse/DEV-68) Specify protection-window behaviour and user controls | Open | Spec covers all states | Doc review vs FocusScheduleStatusResolver states | manual-only |
| ″ |  | Tester converts spec to test cases | Not started (spec not written) | no test (gap) |
| ″ |  | Founder approves copy | Founder sign-off | manual-only |
| [DEV-69](https://shanivar.atlassian.net/browse/DEV-69) Record the intervention choice on the block overlay (on-devi | Open | Choice stored exactly once per overlay (incl. rerender, process death) | Not built — no test | no test (gap) |
| ″ |  | Analytics event fires once | Not built; depends on DEV-65 contract; verify in DEV-83 | no test (gap) |
| ″ |  | No package name leaves the device | Not built — needs payload/network inspection test | no test (gap) |
| ″ |  | [DoD] Overlay choice on a real phone | DEV-71 | real-device-only (NOT RUN) |
| [DEV-70](https://shanivar.atlassian.net/browse/DEV-70) Detect and surface protection failures (service killed, perm | Open | Killed service shows off/unknown state | Not built | no test (gap) |
| ″ |  | Revoked permission shows the fix path | main@20fb519 test/FocusScheduleStatusTest.revoked* (status only; no detection of runtime revocation while active) | no test (gap) |
| ″ |  | Failure appears in crash reporting | Depends on DEV-14 (not built) | no test (gap) |
| ″ |  | [DoD] OEM battery killer / revoke on real phones | DEV-71 | real-device-only (NOT RUN) |
| [DEV-71](https://shanivar.atlassian.net/browse/DEV-71) QA suite: protection permissions, sessions, reboot and recov | Open | AC1 Suite executed on real devices with evidence (umbrella) | Pixel + OEM run — NOT RUN | real-device-only (NOT RUN) |
| ″ |  | AC2 Defects logged with severity | Defect register (this deliverable) | manual-only |
| ″ |  | P1 Usage access revoked while window active → NEEDS_DEVICE_ACCESS + repair path | Emulator plan device_case_plan_dev71_dev75.md P1 (adb appops set … GET_USAGE_STATS ignore) | emulator-pending |
| ″ |  | P2 Overlay (SYSTEM_ALERT_WINDOW) revoked → honest state, no false Active | Emulator plan device_case_plan_dev71_dev75.md P2 | emulator-pending |
| ″ |  | P3 Exact-alarm access denied → protection window not Ready | Emulator plan device_case_plan_dev71_dev75.md P3 (appops SCHEDULE_EXACT_ALARM deny) | emulator-pending |
| ″ |  | S1 Window START/END fire on time; blocked app shows overlay (flash disclosed) | Emulator plan device_case_plan_dev71_dev75.md S1 | emulator-pending |
| ″ |  | S2 Two adjacent/overlapping windows; END of one keeps the other (DEV-2) | Emulator plan device_case_plan_dev71_dev75.md S2 | emulator-pending |
| ″ |  | S3 Overnight 22:00→06:00 + full calendar day across midnight (DEV-5/6) | Emulator plan device_case_plan_dev71_dev75.md S3 (adb clock set) | emulator-pending |
| ″ |  | S4 Local Stop works offline and signed out (DEV-4/10); block screen has no snooze | Emulator plan device_case_plan_dev71_dev75.md S4 (airplane mode) | emulator-pending |
| ″ |  | R1 Reboot inside window resumes; after window does not (DEV-21) | Emulator plan device_case_plan_dev71_dev75.md R1 (adb reboot) | emulator-pending |
| ″ |  | R2 Process death / force-stop of FocusGuardService mid-window | Emulator plan device_case_plan_dev71_dev75.md R2 (am kill / am force-stop) | emulator-pending |
| ″ |  | R3 Doze: boundary delivered after deviceidle force-idle (DEV-1 late START) | Emulator plan device_case_plan_dev71_dev75.md R3 (dumpsys deviceidle force-idle) | emulator-pending |
| ″ |  | R4 Time-zone change + DST fall-back 25 Oct (Europe/Berlin) | Emulator plan device_case_plan_dev71_dev75.md R4 (settings put global auto_time_zone 0; setprop persist.sys.timezone) | emulator-pending |
| ″ |  | R5 App update (MY_PACKAGE_REPLACED) re-arms windows (B2) | Emulator plan device_case_plan_dev71_dev75.md R5 (adb install -r) | emulator-pending |
| ″ |  | D1 OEM battery saver / app killer (Xiaomi/Realme/Vivo/Samsung) keeps FocusGuardService | Real OEM phone only — NOT RUN | real-device-only (NOT RUN) |
| ″ |  | D2 Long run: protection survives overnight on battery, low-battery mode | Real phone only — NOT RUN | real-device-only (NOT RUN) |
| ″ |  | D3 Same suite repeated on a stock Pixel | Real Pixel — NOT RUN | real-device-only (NOT RUN) |

_Epic subtotal:_ 66 ACs — automated-pass 19, manual-only 7, real-device-only (NOT RUN) 17, emulator-pending 12, no test (gap) 11

## Epic C — Routine ([DEV-58](https://shanivar.atlassian.net/browse/DEV-58))

| Ticket | Jira status | AC | Test (file · class.method / pgTAP / manual) | Status |
|---|---|---|---|---|
| [DEV-11](https://shanivar.atlassian.net/browse/DEV-11) Routine V3 — build the day plan, alarm, and protection. Sadh | Work in progress | (derived) 1 Local reboot recovery, no session | see DEV-21 / BootRescheduleCatchUpTest | automated-pass |
| ″ |  | (derived) 2 All-day covers calendar day; no 'unbreakable' copy | see DEV-6 | automated-pass |
| ″ |  | (derived) 3 Ending one window doesn't release another | see DEV-2 / FocusWindowEndHandlerTest | automated-pass |
| ″ |  | (derived) 4 Saved ≠ Active until engine confirms | see DEV-3 / FocusScheduleStatusTest | automated-pass |
| ″ |  | (derived) 5 Two destinations Routine + Sadhana | main@20fb519 test/RoutineShellTest; ShanivarShellTest | automated-pass |
| ″ |  | (derived) 6 Local day plan; saving an activity does not arm an alarm | main@20fb519 test/RoutineProtectionTest.activityOrAnchorEditsNeverTouchTheRegistry; AndroidProtectionGatewayTest.savingThePlanReplacesTheRegistryAndActivitiesDoNot | automated-pass |
| ″ |  | (derived) 7 Timeline and Edit Routine from stored data | main@20fb519 test/RoutineTimelineTest; RoutinePlanRepositoryTest | automated-pass |
| ″ |  | (derived) 8 Multiple windows, overnight, installed apps, honest permissions, local stop | main@20fb519 test/RoutineProtectionTest (31 tests); FocusSchedulerRegistryTest | automated-pass |
| ″ |  | (derived) 9 Physical phone proof, no emulator | DEV-7/DEV-71 device protocol (ROUTINE_V3_DEVICE_QA_2026-10-05.md) — NOT RUN | real-device-only (NOT RUN) |
| ″ | | _Note:_ Ordered build list used as ACs. ROUTINE_V3 flag still false on main. | | |
| [DEV-12](https://shanivar.atlassian.net/browse/DEV-12) Align the Steady UI to its wireframe contract — built before | Pending | All 66 breathing tests still pass | main@20fb519 test/Breathing*Test | automated-pass |
| ″ |  | 200% font: Begin/Pause/End/Back reachable | Not built (parked) | no test (gap) |
| ″ |  | No per-second countdown announcement | Not built (parked) | no test (gap) |
| ″ |  | Native screenshots vs reference | Not built (parked) | manual-only |
| ″ | | _Note:_ Parked; re-scope (Sadhana V1 replaces Steady). | | |
| [DEV-13](https://shanivar.atlassian.net/browse/DEV-13) Extract UI strings to strings.xml — blocks Hindi and all loc | Open | No user-facing literal left in Kotlin | Not built | no test (gap) |
| ″ |  | All tests pass with string resources | Not built | no test (gap) |
| ″ |  | Locale switch changes a visible string E2E | Not built | no test (gap) |
| ″ | | _Note:_ Later. | | |
| [DEV-23](https://shanivar.atlassian.net/browse/DEV-23) Visual system: circles and connecting marks across the app | Open | (derived) No circle/mark paces breath; static under reduced motion | main@20fb519 test/SadhanaSpineTest.rootPulseIsRandomisedNotPeriodicAndStaticUnderReducedMotion; shimmerIsRandomisedNotPeriodic (Sadhana only) | no test (gap) |
| ″ |  | (derived) No scores/ranks | Copy review | manual-only |
| ″ | | _Note:_ Later. No AC section. | | |
| [DEV-24](https://shanivar.atlassian.net/browse/DEV-24) Initiation step 1: onboarding draft persistence, account ada | Work in progress | (derived) Draft survives process death, resumes at checkpoint | main@20fb519 test/InitiationControllerTest.resumesAtPersistedCheckpointAfterProcessDeath; InitiationOnboardingTest | automated-pass |
| ″ |  | (derived) Google/email/guest adapters (real providers stubbed) | main@20fb519 test/InitiationControllerTest.googleAndEmailHonestlyUnavailableGuestStillWorks | automated-pass |
| ″ |  | (derived) Guest→account migration without duplicates | Real providers not built (DEV-25) | no test (gap) |
| ″ |  | (derived) Cancel/error/offline/code-expiry keep the draft | main@20fb519 test/InitiationOnboardingTest (partial — no code-expiry provider) | no test (gap) |
| ″ | | _Note:_ No AC section in Jira. | | |
| [DEV-26](https://shanivar.atlassian.net/browse/DEV-26) Initiation Compose screens from wireframe initiation-v1 | Work in progress | (derived) Screens faithful to initiation-v1 wireframe | Screenshot comparison /workspace/qa/initiation/*.png | manual-only |
| ″ |  | (derived) Guest works end to end | main@20fb519 test/InitiationControllerTest.guestFlowEndToEndUsesRealPlan; InitiationUiTest | automated-pass |
| ″ |  | (derived) No pacing, still planet, reduced motion | main@20fb519 test/InitiationSittingTest; InitiationControllerTest.guidanceIsTextOnlyByRemainingSeconds | automated-pass |
| ″ |  | (derived) Step 6 accessibility review + device QA | Not run | real-device-only (NOT RUN) |
| ″ | | _Note:_ No AC section in Jira. | | |
| [DEV-52](https://shanivar.atlassian.net/browse/DEV-52) Routine schema v2: routine_plans revision/time_zone/practice | Open | routine_plans revision/time_zone/practice_local (forward-only) | Not built | no test (gap) |
| ″ |  | put_routine RPC: match → write+increment; mismatch → 409 | Not built (dev45@2fce42d SyncEngineErrorClassTest.routineParksWhilePutRoutineIsMissing… parks client-side) | no test (gap) |
| ″ |  | Direct client insert/update revoked; only RPC writes | Not built | no test (gap) |
| ″ |  | Protection windows have one home (decision record) | Decision pending | manual-only |
| ″ |  | Package names not sent to server without founder approval | dev45@2fce42d SyncWorkManagerTest.routinePayloadStripsProtectedPackagesUntilDev52 | pending (in QA) |
| ″ |  | 409 surfaced, rebase + retry once | dev45@2fce42d SyncEngineErrorClassTest.routineConflictParksAndPublishesTheCanonicalRow | pending (in QA) |
| ″ |  | pgTAP+integration: stale 409, increment, bad TZ, cross-user, RoutineSync contract | Not built | no test (gap) |
| [DEV-53](https://shanivar.atlassian.net/browse/DEV-53) Device-truth tables: alarm_configs, alarm_observations, devi | Open | alarm_configs / alarm_observations / device_observations tables | Not built | no test (gap) |
| ″ |  | device_id is random per-install UUID | Not built | no test (gap) |
| ″ |  | Append-only/last-value; own-row RLS + DEV-42 baseline | Would be enforced by dev42@ba38de6 supabase/tests/01_baseline_guard (table missing) | no test (gap) |
| ″ |  | Server never overrides local alarm state | main@20fb519 test/DeviceObservationsTest.userDisabledStaysOffWhateverObservationSays; oldConfigRevisionNeverApplies (client side only) | no test (gap) |
| ″ |  | Retention rule documented and enforced | Not built | no test (gap) |
| ″ |  | Outbox kinds + payload builders with tests | Not built | no test (gap) |
| ″ |  | pgTAP: cross-user, checks, device_id format | Not built | no test (gap) |
| [DEV-58](https://shanivar.atlassian.net/browse/DEV-58) epic container | Open | — no ACs (container) | — | — |
| [DEV-72](https://shanivar.atlassian.net/browse/DEV-72) Define routine completion semantics and make completion idem | Open | Integration: 5 identical completion requests → 1 row | Not built (completion semantics undefined) | no test (gap) |
| ″ |  | Offline replay → no duplicates | Not built | no test (gap) |
| [DEV-73](https://shanivar.atlassian.net/browse/DEV-73) First meaningful action flow: new user completes one action  | Open | New user reaches first completed action unaided (usability test) | Moderated usability session | manual-only |
| ″ |  | Failed save never shows success | main@20fb519 test/RoutineProtectionTest.invalidSaveDoesNotSync; saveMessagesNeverClaimActive (protection only; no generic failed-save test) | no test (gap) |
| [DEV-74](https://shanivar.atlassian.net/browse/DEV-74) Missed-step recovery and non-punitive return | Open | After 1, 3, 7 missed days a clear way back | Not built | no test (gap) |
| ″ |  | Copy reviewed for guilt | Copy review | manual-only |
| [DEV-75](https://shanivar.atlassian.net/browse/DEV-75) Verify alarm and notification behaviour on supported real de | Open | AC1 All alarm/notification cases pass or have documented defects (umbrella) | Pixel + OEM run — NOT RUN | real-device-only (NOT RUN) |
| ″ |  | AC2 No unresolved critical | Defect register gate | real-device-only (NOT RUN) |
| ″ |  | A1 Wake alarm fires at set time from locked screen; full-screen intent shown (Android 14+) | Emulator plan device_case_plan_dev71_dev75.md A1 | emulator-pending |
| ″ |  | A2 Alarm fires in Doze (setAlarmClock) and after app swipe-away | Emulator plan device_case_plan_dev71_dev75.md A2 | emulator-pending |
| ″ |  | A3 Alarm re-armed after reboot and app update; recurrence recomputed after fire (0008) | Emulator plan device_case_plan_dev71_dev75.md A3 | emulator-pending |
| ″ |  | A4 Exact-alarm permission denied → honest 'permission required', no silent failure | Emulator plan device_case_plan_dev71_dev75.md A4 | emulator-pending |
| ″ |  | A5 Notification permission (POST_NOTIFICATIONS) denied → state shown | Emulator plan device_case_plan_dev71_dev75.md A5 | emulator-pending |
| ″ |  | A6 DST fall-back / TZ change: one alarm, at local wall-clock time | Emulator plan device_case_plan_dev71_dev75.md A6 | emulator-pending |
| ″ |  | A7 Accessible dismissal path always available; QR/photo path with emulator camera | Emulator plan device_case_plan_dev71_dev75.md A7 | emulator-pending |
| ″ |  | A8 Alarm during a Sadhana practice interrupts and is recorded truthfully | Emulator plan device_case_plan_dev71_dev75.md A8 | emulator-pending |
| ″ |  | A9 Hard cap ends a ringing alarm; no endless ring | Emulator plan device_case_plan_dev71_dev75.md A9 | emulator-pending |
| ″ |  | D1 Movement dismissal with real step detector / ACTIVITY_RECOGNITION | Real phone sensors — NOT RUN | real-device-only (NOT RUN) |
| ″ |  | D2 Alarm actually wakes a person: volume, vibration, speaker, DND interplay | Real phone — NOT RUN | real-device-only (NOT RUN) |
| ″ |  | D3 OEM battery killer does not suppress the alarm (Xiaomi/Realme/Vivo/Samsung) | Real OEM phone — NOT RUN | real-device-only (NOT RUN) |
| ″ |  | D4 Device powered off across alarm time; behaviour on power-on | Real phone — NOT RUN | real-device-only (NOT RUN) |

_Epic subtotal:_ 61 ACs — automated-pass 13, manual-only 6, real-device-only (NOT RUN) 8, emulator-pending 9, no test (gap) 23, pending (in QA) 2

## Epic D — Sadhana ([DEV-59](https://shanivar.atlassian.net/browse/DEV-59))

| Ticket | Jira status | AC | Test (file · class.method / pgTAP / manual) | Status |
|---|---|---|---|---|
| [DEV-16](https://shanivar.atlassian.net/browse/DEV-16) DECISION: Sadhana Phase 2 — gamification and the intensive-p | Pending | (derived) Founder decision recorded (Option A/B/C) | Decision — no test | manual-only |
| ″ | | _Note:_ Decision ticket; later. | | |
| [DEV-27](https://shanivar.atlassian.net/browse/DEV-27) Sadhana S1: practice domain + research-only lock (decision 0 | Work in progress | (derived) Research-only lock is deterministic; activity never unlocks it | main@20fb519 test/SadhanaPracticeTest.researchOnlyNeverLaunchesEvenWhenUnreviewedAllowed; intensiveRiskNeverLaunchesEvenIfMislabelledReleased; SadhanaRecordTest.longPracticeAloneNeverAdvancesBeyondArrive | automated-pass |
| ″ | | _Note:_ No AC section. | | |
| [DEV-28](https://shanivar.atlassian.net/browse/DEV-28) Sadhana S2: attempt timing + events (monotonic active second | Work in progress | (derived) Active foreground seconds only; pauses excluded | main@20fb519 test/SadhanaAttemptTest.pausedAndBackgroundTimeExcluded; PracticeAttemptTest.pausedTimeNotCounted | automated-pass |
| ″ |  | (derived) anchor_changed recorded | main@20fb519 test/SadhanaAttemptTest.anchorChangePausesAndIsLoggedNotFailure | automated-pass |
| ″ |  | (derived) Process death → interrupted, never completed from wall clock | main@20fb519 test/SadhanaControllerTest.processDeathRestoresInterruptedNotCompleted | automated-pass |
| ″ |  | [DoD] Process death / lock / call on a real phone | DEV-34 device QA | real-device-only (NOT RUN) |
| ″ | | _Note:_ No AC section. | | |
| [DEV-29](https://shanivar.atlassian.net/browse/DEV-29) Sadhana S3: record, reflection, independent practice, guidan | Work in progress | (derived) Optional reflection; editable/deletable self-reports | main@20fb519 test/SadhanaRecordTest.reflectionOptionalAndClearable; selfReportsEditableAndDeletable | automated-pass |
| ″ |  | (derived) 'Use fewer prompts' control | main@20fb519 test/SadhanaRecordTest.guidanceReducesOnlyByPreference | automated-pass |
| ″ |  | (derived) Orbit points from evidence families; self-reports never scores | main@20fb519 test/SadhanaRecordTest.orbitEventsFromEvidenceFamilies; LivingOrbitTest.independentRecordMustBeSelfReported | automated-pass |
| ″ | | _Note:_ No AC section. | | |
| [DEV-30](https://shanivar.atlassian.net/browse/DEV-30) Sadhana S4: Living Spine data (3 layers, SourceRecord, centr | Work in progress | (derived) Layers separate; centres study-only until verified | main@20fb519 test/SadhanaSpineTest.noCentreCanLaunchPracticeUntilVerified; plainLayerNamesNoTraditionOrSanskrit | automated-pass |
| ″ | | _Note:_ No AC section. | | |
| [DEV-31](https://shanivar.atlassian.net/browse/DEV-31) Sadhana S5: screens A - home, Living Spine, centre drawer, P | Work in progress | (derived) Screens faithful to sadhana-v1 | Screenshots /workspace/qa/sadhana/*.png | manual-only |
| ″ |  | (derived) No energy/health reading | main@20fb519 test/SadhanaSpineTest.noEnergyOrHealthReadingInPlainText | automated-pass |
| ″ | | _Note:_ No AC section. | | |
| [DEV-32](https://shanivar.atlassian.net/browse/DEV-32) Sadhana S6: screens B - detail, prepare, player, return/refl | Work in progress | (derived) Stationary gate, End always reachable, no navigation away | main@20fb519 test/SadhanaControllerTest.beginNeedsStationary; cannotNavigateAwayFromActivePractice; SadhanaAccessibilityTest | automated-pass |
| ″ |  | (derived) Reduced motion / still planet | main@20fb519 test/SadhanaLoadingTest | automated-pass |
| ″ | | _Note:_ No AC section. | | |
| [DEV-33](https://shanivar.atlassian.net/browse/DEV-33) Sadhana S7: screens C - Record, Without app, Deep study + re | Work in progress | (derived) Research-only methods only as locked study cards | main@20fb519 test/SadhanaPracticeTest.studyOnlyAndQualifiedContextNeverLaunch; SadhanaUiTest | automated-pass |
| ″ | | _Note:_ No AC section. | | |
| [DEV-34](https://shanivar.atlassian.net/browse/DEV-34) Sadhana S8: content review manifests, /v1 Sadhana API + sync | Open | (derived) Content review manifests recorded per practice | main@20fb519 test/ContentReviewTest.releaseLaunchNeedsACompleteMatchingManifest (gate only; manifests empty) | manual-only |
| ″ |  | (derived) Device QA: process death, screen lock, calls | Not run | real-device-only (NOT RUN) |
| ″ |  | (derived) TalkBack + 200% font | main@20fb519 test/SadhanaAccessibilityTest.at200PercentFont (font only; TalkBack not run) | real-device-only (NOT RUN) |
| ″ | | _Note:_ No AC section. Release blocker. | | |
| [DEV-35](https://shanivar.atlassian.net/browse/DEV-35) Finalization B3–B8: sync outbox, content review gate, Choose | Done | B3 Local idempotent sync outbox | main@20fb519 test/SyncOutboxTest.queueIsIdempotentPersistedAndDrainsInOrder | automated-pass |
| ″ |  | B4 Release launch needs a complete matching manifest | main@20fb519 test/ContentReviewTest.releaseLaunchNeedsACompleteMatchingManifest; SadhanaControllerTest.releaseBuildBlocksUnreviewed | automated-pass |
| ″ |  | B5 Choose linked to a saved enabled Routine window | main@20fb519 test/RoutineBoundaryLinkTest.onlyAnEnabledSavedWindowLinksChoose | automated-pass |
| ″ |  | B6 Onboarding 60 s attempt imported once | main@20fb519 test/SadhanaControllerTest.initiationAttemptImportedOnceAndUnfinishedIgnored | automated-pass |
| ″ |  | B7 Edit actual minutes of a self-report | main@20fb519 test/SadhanaControllerTest.selfReportActualMinutesEditableAndPersisted | automated-pass |
| ″ |  | B8 End visible at 200% font and landscape | main@20fb519 test/SadhanaAccessibilityTest.at200PercentFont; inLandscape | automated-pass |
| ″ | | _Note:_ QA pass, except B3 → defect DEV-38. ACs = checklist items B3–B8. | | |
| [DEV-36](https://shanivar.atlassian.net/browse/DEV-36) Sadhana Stage 1 (Spine base): attention practice, ambient fr | Open | 1 Stage 1 identity, text guidance, audio must not pace breath | main@20fb519 test/SadhanaStage1Test.stage1Is60sNoBreathPacingAndHonestCopy (text only; no audio yet) | automated-pass |
| ″ |  | 2 60 s start, end anytime, focus lock, posture | main@20fb519 test/SadhanaFocusLockTest; SadhanaStage1Test | automated-pass |
| ″ |  | 3 Licensed audio, no claims, merged into practice | Not built (DEV-37 spike) | no test (gap) |
| ″ |  | 4 Check-in = exactly 2–3 one-tap options | Shipped 47e668a card has stepper+chips+note — would FAIL; no test asserts ≤3 taps | no test (gap) |
| ″ |  | 5 Founder-approved public wording only | Copy review | manual-only |
| ″ |  | 6 No build until founder answers | Process | manual-only |
| ″ | | _Note:_ Do not build yet (founder). | | |
| [DEV-37](https://shanivar.atlassian.net/browse/DEV-37) Spike: Stage 1 ambient frequency — sound sourcing, licensing | Open | (derived) Spike note: Hz options, licence, non-claim copy, go/no-go | Doc review | manual-only |
| ″ | | _Note:_ Spike; no AC section. | | |
| [DEV-39](https://shanivar.atlassian.net/browse/DEV-39) Bundled techniques.json contains timed box-breathing script  | Open | (derived) No breath counts / timed pacing in techniques.json or .md sources | dev39@8e122a8 test/TechniqueContentSafetyTest.techniquesNeitherPaceTheBreathNorMakeBodyClaims; markdownSourcesNeitherPaceNorClaimNorTime; patternsCatchTheOriginalBoxBreathingScript | automated-pass |
| ″ |  | (derived) No physiological claims | dev39@8e122a8 test/TechniqueContentSafetyTest.techniquesNeitherPaceTheBreathNorMakeBodyClaims | automated-pass |
| ″ |  | (derived) md mirror matches shipped asset | dev39@8e122a8 test/TechniqueContentSafetyTest.mdMirrorMatchesTheShippedAsset | automated-pass |
| ″ | | _Note:_ Failed once on 'rounds' (3471087); passed at 8e122a8 incl. mutation runs (/workspace/qa/dev39-mut-*.log). | | |
| [DEV-51](https://shanivar.atlassian.net/browse/DEV-51) Sadhana content tables (practice_definitions, content_review | Open | Content tables mirror Kotlin | Not built | no test (gap) |
| ″ |  | RLS serves only approved+manifested, never research-only | Not built | no test (gap) |
| ″ |  | Tradition sources only in Deep study | Not built | no test (gap) |
| ″ |  | seed.sql only founder-reviewed content | Not built | no test (gap) |
| ″ |  | Seed text post-DEV-39, nothing from legacy backend | Not built | no test (gap) |
| ″ |  | Offline fallback never launches unreviewed | Not built | no test (gap) |
| ″ |  | pgTAP: draft/research invisible, no writes, manifest required | Not built | no test (gap) |
| ″ | | _Note:_ Later. | | |
| [DEV-54](https://shanivar.atlassian.net/browse/DEV-54) Sadhana record schema alignment: practice_reflections table, | Open | practice_reflections table + own-row RLS | dev54@7a56cf6(+uncommitted) supabase/tests/07_sadhana_record.test.sql ('A sees only own reflections', 'A cannot write a reflection for B') | no test (gap) |
| ″ |  | self_reports private_by_default, drift_reasons check, tombstone delete E2E | dev54@7a56cf6(+uncommitted) supabase/tests/07_sadhana_record.test.sql ('an invalid drift reason is rejected', 'self-report tombstone deletes end to end') | no test (gap) |
| ″ |  | practice_attempts anchor/anchor_changes/events(32 KB)/created_at; SyncPayloads.attempt | dev54@7a56cf6(+uncommitted) supabase/tests/07 ('anchor outside BREATH/ROOM is rejected', 'an oversized events value is rejected') | no test (gap) |
| ″ |  | orbit_events entity_id/created_at/type+source checks; append-only | dev54@7a56cf6(+uncommitted) supabase/tests/07 ('invalid orbit type is rejected', 'an orbit update is denied') | no test (gap) |
| ″ |  | Payload builders match every column; contract test | Not found | no test (gap) |
| ″ |  | pgTAP coverage list | dev54@7a56cf6(+uncommitted) supabase/tests/07_sadhana_record.test.sql | no test (gap) |
| ″ |  | SelfCheck stays local-only (no table) | dev54@7a56cf6(+uncommitted) supabase/tests/07 ('no SelfCheck table (local-only)') | no test (gap) |
| ″ | | _Note:_ Work in progress (uncommitted in worktree); tests exist but not yet run by QA → counted as gap until QA run. | | |
| [DEV-59](https://shanivar.atlassian.net/browse/DEV-59) epic container | Open | — no ACs (container) | — | — |
| [DEV-76](https://shanivar.atlassian.net/browse/DEV-76) Map each initial practice to a user need, expected action an | Open | Practice→need→action→clarity table complete | Doc review | manual-only |
| ″ |  | Content issues ticketed | Doc review | manual-only |
| [DEV-77](https://shanivar.atlassian.net/browse/DEV-77) Optional one-tap awareness prompt after a distraction or com | Open | Skip works in one tap | Not built | no test (gap) |
| ″ |  | Stored locally as self-report, never a score | Not built | no test (gap) |
| ″ | | _Note:_ Later. | | |
| [DEV-78](https://shanivar.atlassian.net/browse/DEV-78) Connect relevant Mano Vijnana content to practical exercises | Open | Links work | Not built | no test (gap) |
| ″ |  | Manifest (DEV-34) approved | Founder | manual-only |
| ″ | | _Note:_ Later. | | |
| [DEV-89](https://shanivar.atlassian.net/browse/DEV-89) Safety audit: Breathing* screens and block-screen "breathe"  | Open | No reachable/bundled UI, animation or asset paces breathing | dev39@8e122a8 test/TechniqueContentSafetyTest.breathingCatalogueGuidanceNeitherPacesNorClaims (techniques + catalogue only); Breathing*.kt screens and block-screen 'breathe' path not covered | no test (gap) |
| ″ |  | No breathing physiological/medical/energy claims | main@20fb519 test/BreathingCatalogTest.c10_noShippedTextMakesAPhysiologicalOrSpiritualPromise; dev39@8e122a8 test/TechniqueContentSafetyTest.techniquesNeitherPaceTheBreathNorMakeBodyClaims | automated-pass |
| ″ |  | Block-screen option renamed/re-scoped as non-paced pause | Not built; manual check of BlockOverlayActivity/LocalPauseScreen | no test (gap) |
| ″ |  | Automated safety test covers these files and fails on old content | dev39@8e122a8 test/TechniqueContentSafetyTest.patternsCatchTheOriginalBoxBreathingScript (covers techniques.json + .md only) | no test (gap) |
| ″ |  | Tester verifies point by point | Manual audit — not run | manual-only |

_Epic subtotal:_ 59 ACs — automated-pass 24, manual-only 10, real-device-only (NOT RUN) 3, no test (gap) 22

## Epic E — Security, privacy & reliability ([DEV-60](https://shanivar.atlassian.net/browse/DEV-60))

| Ticket | Jira status | AC | Test (file · class.method / pgTAP / manual) | Status |
|---|---|---|---|---|
| [DEV-8](https://shanivar.atlassian.net/browse/DEV-8) Rotate the two leaked credentials — both verified still vali | Open | Every rotated credential verified rejected | Manual API check (founder op) — not done | manual-only |
| ″ |  | Production health 200 / database ok | Manual check | manual-only |
| ″ |  | Decision 0005 step 1 marked complete | Doc | manual-only |
| ″ | | _Note:_ Release blocker. Founder-only. | | |
| [DEV-9](https://shanivar.atlassian.net/browse/DEV-9) Add a migration runner before the first knowledge table exis | Pending | All backend tests pass | backend/test/*.test.js | no test (gap) |
| ″ |  | Rollback demonstrated | Not done | no test (gap) |
| ″ |  | One migration through runner | Not done | no test (gap) |
| ″ |  | Pooler path unchanged | Manual | manual-only |
| ″ | | _Note:_ Re-scope: legacy Render only (DEV-40). | | |
| [DEV-17](https://shanivar.atlassian.net/browse/DEV-17) Add release signing config and resolve api.thehybridmonk.com | Open | (PM-refined) Keystore custody (founder) | Manual | manual-only |
| ″ |  | signingConfig from env; build fails if missing | Not built | no test (gap) |
| ″ |  | prodRelease/stagingRelease endpoints | Not built | no test (gap) |
| ″ |  | DNS only if Render kept | Decision | manual-only |
| ″ |  | release.yml draft release + SHA-256 | Not built | no test (gap) |
| ″ |  | Key SHA-1/256 recorded | Manual | manual-only |
| ″ |  | versionCode auto-increments | Not built | no test (gap) |
| ″ |  | Content-review gate respected in release | main@20fb519 test/ContentReviewTest; SadhanaControllerTest.releaseBuildBlocksUnreviewed | automated-pass |
| ″ |  | Play declarations recorded | Manual | manual-only |
| ″ |  | Signed APK installs on phone and syncs | Real device | real-device-only (NOT RUN) |
| ″ | | _Note:_ ACs from PM comment 17 Oct (awaiting founder). | | |
| [DEV-18](https://shanivar.atlassian.net/browse/DEV-18) Confirm whether GEMINI_API_KEY is set on Render — every AI e | Pending | (derived) Render GEMINI_API_KEY state recorded | Manual dashboard check | manual-only |
| ″ | | _Note:_ Not in first release. | | |
| [DEV-22](https://shanivar.atlassian.net/browse/DEV-22) Future sign-in options: phone, WhatsApp, SMS, authenticator  | Open | (derived) none — future | — | no test (gap) |
| ″ | | _Note:_ Later; no ACs. | | |
| [DEV-25](https://shanivar.atlassian.net/browse/DEV-25) Accounts + cloud sync: real providers, backend /v1, hosting  | Open | Guest = anon user once per install | dev45@2fce42d SyncSessionTest.guestSignsUpOncePerInstallAndSurvivesRestart | pending (in QA) |
| ″ |  | Email OTP + invite gate; guest upgrade keeps user_id | Not built | no test (gap) |
| ″ |  | Google via Credential Manager; linkIdentity keeps user_id | Not built | no test (gap) |
| ″ |  | enable_manual_linking on hosted projects | Cloud config — Backend (QA must not touch cloud) | manual-only |
| ″ |  | Unconfigured providers hidden / 'not available yet' | main@20fb519 test/InitiationControllerTest.googleAndEmailHonestlyUnavailableGuestStillWorks | automated-pass |
| ″ |  | Sign-out clears tokens; no cross-user send | dev45@2fce42d SyncSessionTest.signOutClearsTokensAndNotifies; SyncWorkManagerTest.signOutKeepsItemsForTheOldUserUnsent | pending (in QA) |
| ″ |  | Tests: guest→email/Google same user_id; wrong/expired OTP fail | Not built | no test (gap) |
| ″ |  | Privacy notice updated before non-founder testers | Founder | manual-only |
| ″ |  | Legacy /auth/* unused | Code review | manual-only |
| ″ | | _Note:_ ACs from PM comment 17 Oct. | | |
| [DEV-38](https://shanivar.atlassian.net/browse/DEV-38) Sync outbox never queues own-practice self-reports, their ed | Open | (derived) recordOwn queues a self-report | main@20fb519 test/SyncOutboxGapsTest.recordOwnQueuesSelfReport | automated-pass |
| ″ |  | (derived) Edits replace the queued self-report | main@20fb519 test/SyncOutboxGapsTest.adjustOwnActualReplacesQueuedSelfReport | automated-pass |
| ″ |  | (derived) Delete queues a tombstone over pending upsert | main@20fb519 test/SyncOutboxGapsTest.deleteOwnQueuesTombstoneOverPendingUpsert; senderDeletesTombstonesAndUpsertsTheRest | automated-pass |
| ″ |  | (derived) Onboarding import queued once (+ legacy backfill) | main@20fb519 test/SyncOutboxGapsTest.onboardingImportQueuesAttemptOnce; backfillQueuesOnboardingAttemptAlreadyImportedByOldBuild; backfillIsIdempotentByAttemptId; backfillRunsOnceAcrossRestarts | automated-pass |
| ″ |  | (derived) Routine changes queued | main@20fb519 test/SyncOutboxGapsTest.routineChangeQueuesRoutinePlan | automated-pass |
| ″ | | _Note:_ Failed once (legacy-import gap at 86b558a); passed at 20fb519 (/workspace/qa/dev38/rerun-*). Jira still Open. | | |
| [DEV-40](https://shanivar.atlassian.net/browse/DEV-40) DECISION: Supabase is the only backend for the phone; retire | Open | 1 Decision record 0019 lists every ShanivarApi endpoint + disposition | Doc review | manual-only |
| ″ |  | 2 Founder confirms in writing | Founder | manual-only |
| ″ |  | 3 Tables → DEV-42; Edge Functions → own tickets | Doc review | manual-only |
| ″ |  | 4 Render untouched until replacement verified | Process | manual-only |
| ″ |  | 5 Legacy data carry-over stated | Doc review | manual-only |
| ″ |  | 6 0001/0004/DEV-9 marked legacy-only | Doc review | manual-only |
| [DEV-41](https://shanivar.atlassian.net/browse/DEV-41) Supabase: add a staging project next to shanivar-prototype,  | Open | 1 shanivar-staging exists (ap-south-1) | Backend/founder (QA must not touch cloud) | manual-only |
| ″ |  | 2 Auth settings recorded for both | Doc review | manual-only |
| ″ |  | 3 Keys in password manager + GH env secrets only | Secret scan | manual-only |
| ″ |  | 4 Plan/pause/backup/spend cap written | Doc review | manual-only |
| ″ |  | 5 Least-privilege access; revocable CI token | Manual | manual-only |
| ″ |  | 6 /auth/v1/health 200 on mobile data | Real phone | real-device-only (NOT RUN) |
| [DEV-42](https://shanivar.atlassian.net/browse/DEV-42) Schema hygiene + RLS hardening on all sync tables: updated_a | Open | 1 evidence_type checks (forward-only) | dev42@ba38de6 supabase/tests/01_baseline_guard ('…evidence_type has a check'); 02_rls_isolation ('self_report cannot be labelled app_observed') | automated-pass |
| ″ |  | 2 created_at + set_updated_at() everywhere; client_updated_at | dev42@ba38de6 supabase/tests/01_baseline_guard; 03_client_updated_at | automated-pass |
| ″ |  | 3 Size caps (plan 64 KiB, packages 200, drift 20, id 128, note 500) | dev42@ba38de6 supabase/tests/04_size_caps (14 asserts) — other text columns uncapped (QA-42-03) | automated-pass |
| ″ |  | 4 revoke all from anon; authenticated minimal | dev42@ba38de6 supabase/tests/05_privileges | automated-pass |
| ″ |  | 5 pgTAP: cross-user CRUD, no session, user_id immutable, spoofing, oversize | dev42@ba38de6 supabase/tests/02_rls_isolation (43); 04_size_caps | automated-pass |
| ″ |  | 6 Baseline applied to every new table DEV-47..55 | dev42@ba38de6 supabase/tests/01_baseline_guard iterates all public tables (fails on new tables missing baseline) | automated-pass |
| ″ |  | 7 on delete cascade from auth.users verified | dev42@ba38de6 supabase/tests/01_baseline_guard ('…references auth.users on delete cascade') — FK presence only; no delete-and-check test | no test (gap) |
| ″ |  | 8 Security/Performance Advisor 0 errors after deploy | Cloud advisors — not deployed; QA must not touch cloud | manual-only |
| ″ | | _Note:_ Passed at ba38de6 (local stack). Findings QA-42-01..03 in defect register. | | |
| [DEV-43](https://shanivar.atlassian.net/browse/DEV-43) Apply Supabase migrations and Edge Functions through CI: sta | Open | 1 migration repair once, output pasted | Not built (no .github/) | no test (gap) |
| ″ |  | 2 PR workflow: reset, lint, test db | Not built (no .github/) | no test (gap) |
| ″ |  | 3 merge→staging deploy | Not built (no .github/) | no test (gap) |
| ″ |  | 4 prod via dispatch + founder reviewer | Not built (no .github/) | no test (gap) |
| ″ |  | 5 drift check fails job | Not built (no .github/) | no test (gap) |
| ″ |  | 6 forward-only; destructive label | Not built (no .github/) | no test (gap) |
| ″ |  | 7 secrets only in GH env | Not built (no .github/) | no test (gap) |
| ″ |  | 8 seed.sql fixed | Not built (no .github/) | no test (gap) |
| ″ |  | 9 broken migration fails CI | Not built (no .github/) | no test (gap) |
| [DEV-44](https://shanivar.atlassian.net/browse/DEV-44) Android build flavours (staging/prod) carry Supabase URL and | Open | 1 env flavours staging/prod side by side | Not built | no test (gap) |
| ″ |  | 2 URL/key from properties only | Not built | no test (gap) |
| ″ |  | 3 release fails without https URL/key | Not built | no test (gap) |
| ″ |  | 4 cleartext debug-only | Not built | no test (gap) |
| ″ |  | 5 CI secret scan (gitleaks) | Not built | no test (gap) |
| ″ |  | 6 env name shown on non-prod | Not built | no test (gap) |
| ″ |  | 7 API_URL removed/flavour-aware | Not built | no test (gap) |
| ″ |  | 8 docs per flavour | Not built | no test (gap) |
| [DEV-45](https://shanivar.atlassian.net/browse/DEV-45) Sync engine (Backend half): guest session, sender loop with  | Open | 1 SyncSession: guest once per install, encrypted tokens, refresh | dev45@2fce42d test/SyncSessionTest (8) | pending (in QA) |
| ″ |  | 2 Items stamped with owner user_id, never sent as another user | dev45@2fce42d test/SyncEngineErrorClassTest.itemsOfAnotherUserAreNeverSentAndUnownedOnesAreStamped | pending (in QA) |
| ″ |  | 3 WorkManager outbox-flush, CONNECTED, backoff 30 s–1 h | dev45@2fce42d test/SyncWorkManagerTest; SyncOutboxEngineTest.backoffIsExponentialWithJitterFrom30sTo1h | pending (in QA) |
| ″ |  | 4 Error classes (retry / 401 refresh / dead-letter & continue / ordering) | dev45@2fce42d test/SyncEngineErrorClassTest (20) | pending (in QA) |
| ″ |  | 5 Idempotency (3 sends → 1 row, crash-after-ack, tombstone) | dev45@2fce42d test/SyncOutboxEngineTest; supabase/tests/06_sync_conflicts | pending (in QA) |
| ″ |  | 6 Conflict rule LWW on client_updated_at; tombstones; put_routine 409 | dev45@2fce42d supabase/tests/06_sync_conflicts (22); decision 0019 | pending (in QA) |
| ″ |  | 7 Restore (only if multi-device wanted) | Founder decision pending | manual-only |
| ″ |  | 8 Offline 7 days/50 items; airplane toggles | dev45@2fce42d test/SyncOutboxEngineTest.sevenDaysOfflineFiftyItemsAllArriveOnReconnect; airplaneModeTogglesMidFlushLoseNothing | pending (in QA) |
| ″ |  | 9 MockWebServer per error class + WorkManager test; suite green | dev45@2fce42d test/SyncEngineErrorClassTest; SyncWorkManagerTest | pending (in QA) |
| ″ |  | 10 Verified once against shanivar-prototype | Cloud project — out of QA bounds; Backend to run | manual-only |
| ″ | | _Note:_ In QA now — PENDING. | | |
| [DEV-46](https://shanivar.atlassian.net/browse/DEV-46) Edge Function delete-account: in-app account and synced-data | Open | 1 delete-account verifies JWT, cascades, idempotent | Not built | no test (gap) |
| ″ |  | 2 service-role only in function env | Not built | no test (gap) |
| ″ |  | 3 Settings two-step delete; clears session+outbox | Not built | no test (gap) |
| ″ |  | 4 client-side export | Not built | no test (gap) |
| ″ |  | 5 web deletion path | Not built | no test (gap) |
| ″ |  | 6 Staging tests: rows gone, others untouched, old JWT rejected, logs clean | Not built | no test (gap) |
| ″ |  | 7 Deployed only via DEV-43 | Process | manual-only |
| [DEV-47](https://shanivar.atlassian.net/browse/DEV-47) Wire sync into the app (Frontend half): build the client fro | Open | 1 One AppSync from BuildConfig; empty URL = sync off, tested | dev47@1e69cba test/AppSyncTest.emptyUrlOrKeyOrMissingEngineMeansDisabled; SyncStatusUiTest.syncOffRecordViewIsIdentical | automated-pass |
| ″ |  | 2 Guest ensureGuest after consent, never blocks | dev47@1e69cba test/InitiationSyncTest.guestStepCallsEnsureGuestExactlyOnce; offlineGuestSignInNeverBlocksTheFlow | automated-pass |
| ″ |  | 3 Flush triggers (enqueue, foreground, network, periodic), off main thread | dev47@1e69cba test/AppSyncTest.enqueueTriggersRequestFlush; foregroundRequestsFlush; SyncWiringRobolectricTest | automated-pass |
| ″ |  | 4 Quiet status line, three strings, no counts/streaks | dev47@1e69cba test/SyncStatusUiTest.statusFollowsTheEngineStateExactlyThreeStrings | automated-pass |
| ″ |  | 5 Dead-letter count only in debug diagnostics | dev47@1e69cba test/SyncStatusUiTest.deadLetterCountOnlyInDebugDiagnostics | automated-pass |
| ″ |  | 6 Sign-out/reset clears session, no cross-user send | dev47@1e69cba test/AppSyncTest.signOutClearsSessionThroughEngine; resetUsesDev45ResetAndClearsTheSession | automated-pass |
| ″ |  | 7 Compose/Robolectric tests listed | dev47@1e69cba test/SyncStatusUiTest; InitiationSyncTest; AppSyncTest | automated-pass |
| ″ |  | 8 Manual: attempt + self-report reach tables within 1 min (+airplane) | Needs device/emulator + staging; cloud off-limits for QA | real-device-only (NOT RUN) |
| ″ | | _Note:_ Developer run on branch: 548 tests, 0 fail (22:36). Not yet QA-reviewed; depends on DEV-45. | | |
| [DEV-48](https://shanivar.atlassian.net/browse/DEV-48) Invite-code-gated email signup (Supabase Auth email OTP + se | Open | 1 invite_codes table (hash), RLS no client policy | Not built | no test (gap) |
| ″ |  | 2 before-user-created hook rejects bad codes | Not built | no test (gap) |
| ″ |  | 3 atomic redemption | Not built | no test (gap) |
| ″ |  | 4 guest→email gated, same user_id | Not built | no test (gap) |
| ″ |  | 5 email OTP; custom SMTP in prod | Not built | no test (gap) |
| ″ |  | 6 founder mint/revoke snippet | Not built | no test (gap) |
| ″ |  | 7 rate limits | Not built | no test (gap) |
| ″ |  | 8 pgTAP+integration incl. 2-signup race | Not built | no test (gap) |
| ″ |  | 9 invite-code field, plain copy | Not built | no test (gap) |
| [DEV-50](https://shanivar.atlassian.net/browse/DEV-50) Onboarding/profile table (profiles + onboarding draft) and g | Open | 1 profiles table | Not built | no test (gap) |
| ″ |  | 2 auth.users trigger creates profile | Not built | no test (gap) |
| ″ |  | 3 guidance_preferences table | Not built | no test (gap) |
| ″ |  | 4 own-row RLS + DEV-42 baseline | Not built | no test (gap) |
| ″ |  | 5 stale revision → 409 | Not built | no test (gap) |
| ″ |  | 6 payload builders + outbox kinds | Not built | no test (gap) |
| ″ |  | 7 no birth/email/phone in profiles | Not built | no test (gap) |
| [DEV-60](https://shanivar.atlassian.net/browse/DEV-60) epic container | Open | — no ACs (container) | — | — |
| [DEV-79](https://shanivar.atlassian.net/browse/DEV-79) Cross-user access test suite for every Supabase table, RPC a | Open | Every table/RPC/Edge Function has a passing isolation test | dev42@ba38de6 supabase/tests/02_rls_isolation covers 5 tables; dev45@2fce42d 06 tombstones; no RPC/Edge Function yet | no test (gap) |
| ″ |  | Any failure is a release blocker | Release gate (template §G4) | manual-only |
| [DEV-80](https://shanivar.atlassian.net/browse/DEV-80) Sensitive logging, retention, deletion and backup review | Open | Audit report (logging, retention, deletion, backups) | Not started | no test (gap) |
| ″ |  | Restore drill on staging | Needs DEV-41 | no test (gap) |
| [DEV-81](https://shanivar.atlassian.net/browse/DEV-81) Release, rollback and incident-response runbook | Open | Rollback rehearsed on staging once | Needs DEV-41; note d9168eb cannot be reverted alone (QA-42-01) | no test (gap) |
| ″ |  | Linked to G6 | Doc | manual-only |

_Epic subtotal:_ 117 ACs — automated-pass 20, manual-only 29, real-device-only (NOT RUN) 3, no test (gap) 55, pending (in QA) 10

## Epic F — QA & release ([DEV-61](https://shanivar.atlassian.net/browse/DEV-61))

| Ticket | Jira status | AC | Test (file · class.method / pgTAP / manual) | Status |
|---|---|---|---|---|
| [DEV-14](https://shanivar.atlassian.net/browse/DEV-14) Add crash reporting — a failed alarm currently leaves no tra | Open | Forced crash → retrievable local report | Not built | no test (gap) |
| ″ |  | Export needs explicit user action | Not built | no test (gap) |
| ″ |  | No reflection/feeling/package in payload | Not built | no test (gap) |
| ″ |  | Network traffic inspected | Proxy capture on device | real-device-only (NOT RUN) |
| [DEV-15](https://shanivar.atlassian.net/browse/DEV-15) Add CI running backend and Android tests on push — two agent | Open | Workflow green on main | Not built (no .github/) | no test (gap) |
| ″ |  | Broken test fails workflow | Not built | no test (gap) |
| ″ |  | No secrets needed | Not built | no test (gap) |
| [DEV-19](https://shanivar.atlassian.net/browse/DEV-19) Git push cannot complete from a non-interactive session — cr | Open | Non-interactive git push succeeds | Out of QA scope (never push) | manual-only |
| ″ |  | No credential in config/URL/files | Secret scan | manual-only |
| ″ |  | git fetch does not hang | Manual | manual-only |
| [DEV-55](https://shanivar.atlassian.net/browse/DEV-55) E2E: phone-to-Supabase round trip against staging (automated | Open | 1 SupabaseSyncTest nightly vs staging | main@20fb519 test/SupabaseSyncTest.e2eGuestPushReadBack (local only; SKIPPED without -Dsupabase.e2e.url) | no test (gap) |
| ″ |  | 2 pgTAP vs staging after deploy | Needs DEV-41/43 | no test (gap) |
| ″ |  | 3 Device checklist on staging build | Real phone | real-device-only (NOT RUN) |
| ″ |  | 4 Offline resilience (airplane, kill mid-flush, reboot) | Real phone | real-device-only (NOT RUN) |
| ″ |  | 5 >1 h idle refresh, same user_id | Real phone | real-device-only (NOT RUN) |
| ″ |  | 6 Mobile data + flaky network | Real phone | real-device-only (NOT RUN) |
| ″ |  | 7 Second guest isolation; delete-account | Needs DEV-46 | no test (gap) |
| ″ |  | 8 put_routine 409 two devices | Needs DEV-52 | no test (gap) |
| ″ |  | 9 Tester never uses service-role key | Process rule | manual-only |
| [DEV-61](https://shanivar.atlassian.net/browse/DEV-61) epic container | Open | — no ACs (container) | — | — |
| [DEV-82](https://shanivar.atlassian.net/browse/DEV-82) Traceable test plan, AC-to-test matrix and defect register f | Open | Matrix covers epics A–G | /workspace/qa/dev82/ac_test_matrix.md (this deliverable) | manual-only |
| ″ |  | Template used for first RC | /workspace/qa/dev82/release_readiness_template.md — first RC not cut | manual-only |
| [DEV-83](https://shanivar.atlassian.net/browse/DEV-83) Verify analytics events match actual user actions | Open | Report per event: expected vs actual | No events exist (DEV-65 not defined) | no test (gap) |
| [DEV-84](https://shanivar.atlassian.net/browse/DEV-84) End-to-end protect → act → reflect journey and regression su | Open | E2E protect→act→reflect passes on two devices | Not started; needs DEV-69/72/74 | real-device-only (NOT RUN) |

_Epic subtotal:_ 23 ACs — manual-only 6, real-device-only (NOT RUN) 6, no test (gap) 11

## Epic G — CS & growth ([DEV-62](https://shanivar.atlassian.net/browse/DEV-62))

| Ticket | Jira status | AC | Test (file · class.method / pgTAP / manual) | Status |
|---|---|---|---|---|
| [DEV-49](https://shanivar.atlassian.net/browse/DEV-49) In-app feedback: write-only feedback table + submit_feedback | Open | 1 feedback table | Not built | no test (gap) |
| ″ |  | 2 RLS insert/select own; no update/delete; no anon | Not built | no test (gap) |
| ″ |  | 3 submit_feedback RPC 10/day | Not built | no test (gap) |
| ″ |  | 4 via outbox, idempotent | Not built | no test (gap) |
| ″ |  | 5 no auto diagnostics | Not built | no test (gap) |
| ″ |  | 6 founder SQL query | Not built | no test (gap) |
| ″ |  | 7 pgTAP list | Not built | no test (gap) |
| ″ |  | 8 Settings entry, no ratings | Not built | no test (gap) |
| [DEV-62](https://shanivar.atlassian.net/browse/DEV-62) epic container | Open | — no ACs (container) | — | — |
| [DEV-85](https://shanivar.atlassian.net/browse/DEV-85) Activation and retention dashboard with goal segments | Open | Baseline numbers visible; limits stated | Needs analytics (DEV-65/83) | no test (gap) |
| [DEV-86](https://shanivar.atlassian.net/browse/DEV-86) Feedback and churn-reason taxonomy, interview guide and mont | Open | First 5 interviews; first monthly report | CS deliverable review | manual-only |
| [DEV-87](https://shanivar.atlassian.net/browse/DEV-87) Lapsed-user recovery journey offering smaller commitments | Open | Spec approved; links to C3 | Doc review | manual-only |
| ″ | | _Note:_ Later. | | |
| [DEV-88](https://shanivar.atlassian.net/browse/DEV-88) Positioning doc and feature-claim verification checklist | Open | Checklist applied to site, onboarding and store listing | Manual claim audit — not run | manual-only |

_Epic subtotal:_ 12 ACs — manual-only 3, no test (gap) 9

## Epic No epic

| Ticket | Jira status | AC | Test (file · class.method / pgTAP / manual) | Status |
|---|---|---|---|---|
| [DEV-20](https://shanivar.atlassian.net/browse/DEV-20) DECISION: sprints and Fix Version are unavailable — DEV is a | Open | (derived) Founder picks option A/B/C | Decision | manual-only |
| ″ | | _Note:_ Process; no epic. | | |

_Epic subtotal:_ 1 ACs — manual-only 1
