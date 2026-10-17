# AC-to-test data for DEV-82. Status codes:
# AP automated-pass | MO manual-only | RD real-device-only (NOT RUN) | EMU emulator-pending | GAP no test (gap) | PEND pending (in QA now)
M  = "main@20fb519 test/"            # android-app/app/src/test/java/com/shanivar/nativeapp/
B39= "dev39@8e122a8 test/"
B42= "dev42@ba38de6 supabase/tests/"
B45= "dev45@2fce42d "
B47= "dev47@1e69cba test/"
B54= "dev54@7a56cf6(+uncommitted) supabase/tests/"
E = "Emulator plan device_case_plan_dev71_dev75.md "
DEV7 = "DEV-7/DEV-71 device protocol (ROUTINE_V3_DEVICE_QA_2026-10-05.md) — NOT RUN"
D = {}
def T(k, *rows, note=""): D[k] = (list(rows), note)

# ---------------- Epic B — Protection ----------------
T("DEV-1",
 ("Stale START (after window closed) starts nothing", M+"FocusWindowRecurrenceTest.dev1_aStartDeliveredAfterItsWindowClosedHasNoActiveEndToMeasureAgainst", "AP"),
 ("On-time START measures correct remaining duration", M+"FocusWindowRecurrenceTest.dev1_anOnTimeStartStillMeasuresToItsOwnWindowEnd", "AP"),
 ("Regression test covers delayed delivery", M+"FocusWindowRecurrenceTest.dev1_aLateEndFindsNoActiveOccurrenceSoItReleasesRatherThanSkipping; FocusWindowEndHandlerTest.lateEndDeliveredInsideTheNextOccurrence…", "AP"),
 ("[DoD] Delayed/powered-off START on a real phone (guide cases 7–8)", DEV7, "RD"))
T("DEV-2",
 ("Earlier END does not release a later window's protection", M+"FocusWindowEndHandlerTest.earlierEndOfOverlappingWindowsDoesNotReleaseTheLaterWindow", "AP"),
 ("Overlap handled or rejected at save with a reason", M+"RoutineProtectionTest.validateNamesTheTwoOverlappingWindows; FocusSchedulerRegistryTest.replaceAllCapsAtEightAndRejectsOverlap", "AP"),
 ("Test: one window ends inside another", M+"FocusWindowEndHandlerTest.innerWindowEndingInsideAnOuterWindowLeavesTheOuterWindowProtected", "AP"),
 ("[DoD] Multi-window on a real phone", DEV7, "RD"))
T("DEV-3",
 ("Revoked usage/exact-alarm → NEEDS_DEVICE_ACCESS / NEEDS_ATTENTION", M+"FocusScheduleStatusTest.revokedUsageAccessIsNeedsDeviceAccess…; revokedExactAlarmIsNeedsDeviceAccessNotReady", "AP"),
 ("ACTIVE only when guard confirms", M+"FocusScheduleStatusTest.savedAndArmedInsideTheWindowWithoutAGuardIsNotActive; savedReadyAndActiveAreDistinctStates; RoutineProtectionTest.saveMessagesNeverClaimActive", "AP"),
 ("Repair next-step understandable from state", "Manual UX review of repair copy (no test asserts comprehension)", "MO"),
 ("[DoD] Revoke permissions in Settings on a real phone", DEV7, "RD"))
T("DEV-4",
 ("Cold-process START/END with no session and no network", M+"LocalProtectionTest.startsProtectionWithoutAnyAccountOrNetworkInvolved; FocusWindowEndHandlerTest.endHandlerNamesNoSessionOrApi; BootRescheduleCatchUpTest.catchUpDoesNotRequireASession", "AP"),
 ("Immediate local stop works offline", M+"LocalProtectionTest.stopNowIsImmediateAndIdempotent; DisciplineEngineTest.releaseFocusStopsTheGuardEvenWhenTheBackendCallFails; RoutineProtectionTest.stopIsLocalFirstAndSurvivesMirrorFailure", "AP"),
 ("Test covers cold process explicitly", M+"LocalProtectionTest.theLocalPathNamesNoApiTypeAtAll (structural)", "AP"),
 ("[DoD] Process death / force-stop / airplane mode on a real phone", DEV7, "RD"))
T("DEV-5",
 ("22:00→07:00 saves and fires both boundaries", M+"FocusWindowRecurrenceTest.mondayNightOvernightWindowIsActive…; overnightBoundariesAlternateStartThenEndAcrossMidnight; FocusWindowEndHandlerTest.overnightEndReleasesOnTheMorningAfter", "AP"),
 ("Day selection applies to start day", M+"FocusWindowRecurrenceTest.theSameOvernightWindowIsInactiveSundayNightUnlessSundayIsSelected; endDaysShiftForwardOnlyForOvernightWindowsAndWrapPastSaturday", "AP"),
 ("DST gap/overlap deterministic, no two active copies", M+"FocusWindowRecurrenceTest.daylightSavingTransitionsProduceOneDeterministicBoundary (Berlin 2026-03-29/10-25)", "AP"),
 ("Timezone change explains the changed next boundary", M+"FocusWindowRecurrenceTest.boundariesAreRecomputedFromLocalRulesRatherThanStoredInstants (recompute only — no test or UI for the *explanation*)", "GAP"),
 ("[DoD] Overnight + DST (25 Oct 2026) + TZ change on a real phone", DEV7, "RD"))
T("DEV-6",
 ("No one-minute daily gap", M+"FocusWindowRecurrenceTest.fullCalendarDayCoversEndOfSelectedDayWithNoOneMinuteGap; fullCalendarDayAllSevenDaysStaysActiveAcrossMidnightWithNoGap; FocusScheduleDraftTest.allDayAndDaysPresetsAreValidFullCalendarDays", "AP"),
 ("No UI/marketing string claims continuous/unbreakable protection", "No automated copy scan; manual review of app + site + store (DEV-88 checklist)", "GAP"),
 ("Known bypasses stay disclosed (force-stop, revoked perms, browsers, 2nd device)", "Manual copy review of block overlay/Routine protection screen", "MO"),
 ("[DoD] Full-day across midnight on a real phone", DEV7, "RD"))
T("DEV-7",
 ("Evidence recorded with raw output", "Device protocol run log (not started; founder paused 5 Oct)", "RD"),
 ("Skipped integration test recorded as NOT RUN", "QA reporting rule (this plan §5); androidTest classes never executed", "RD"),
 ("Second OEM covered or support claim narrowed", "Device matrix: Pixel + Xiaomi/Realme/Vivo/Samsung class", "RD"))
T("DEV-10",
 ("(derived) Block screen has no snooze / five-more-minutes", "No test asserts absence of snooze on BlockOverlayActivity", "GAP"),
 ("(derived) Essential-access stop works with no payment, practice or server", M+"LocalProtectionTest.stopNowIsImmediateAndIdempotent; SadhanaFocusLockTest.allModeBlocksEverythingExceptEssentialAndOwn", "AP"),
 ("[DoD] Stop path on a real phone, offline, signed out", DEV7, "RD"),
 note="No AC section in Jira — ACs derived from description; return to owner to confirm.")
T("DEV-21",
 ("(derived) Reboot inside a valid window resumes local protection with no account/network", M+"BootRescheduleCatchUpTest.rebootInsideALiveWindowResumesLocalProtectionWithNoSession; catchUpDoesNotRequireASession", "AP"),
 ("(derived) Reboot after window ended does not start protection", M+"BootRescheduleCatchUpTest.rebootAfterTheWindowEndedDoesNotStartProtectionButStillRearms; rebootBeforeTheWindowDoesNotStartProtection", "AP"),
 ("(derived) Next alarm re-armed either way", M+"BootRescheduleCatchUpTest.catchUpStillRearmsWhenTheDeviceCannotEnforce; BootReconcileActionsTest.appUpdateReconcilesAlarmsAndWindows", "AP"),
 ("[DoD] Real reboot inside/after a window", DEV7+" (DEV-21 row of device checklist)", "RD"),
 note="Passes in unit tests; real-device verification pending under DEV-7.")
T("DEV-67",
 ("Capability table reviewed by Tester", "Doc review", "MO"),
 ("Every limit has user copy and a test in B5 (DEV-71)", "Depends on DEV-71 suite", "GAP"),
 ("Feeds Marketing claim checklist (G4/DEV-88)", "Doc cross-check", "MO"))
T("DEV-68",
 ("Spec covers all states", "Doc review vs FocusScheduleStatusResolver states", "MO"),
 ("Tester converts spec to test cases", "Not started (spec not written)", "GAP"),
 ("Founder approves copy", "Founder sign-off", "MO"))
T("DEV-69",
 ("Choice stored exactly once per overlay (incl. rerender, process death)", "Not built — no test", "GAP"),
 ("Analytics event fires once", "Not built; depends on DEV-65 contract; verify in DEV-83", "GAP"),
 ("No package name leaves the device", "Not built — needs payload/network inspection test", "GAP"),
 ("[DoD] Overlay choice on a real phone", "DEV-71", "RD"))
T("DEV-70",
 ("Killed service shows off/unknown state", "Not built", "GAP"),
 ("Revoked permission shows the fix path", M+"FocusScheduleStatusTest.revoked* (status only; no detection of runtime revocation while active)", "GAP"),
 ("Failure appears in crash reporting", "Depends on DEV-14 (not built)", "GAP"),
 ("[DoD] OEM battery killer / revoke on real phones", "DEV-71", "RD"))
T("DEV-71",
 ("AC1 Suite executed on real devices with evidence (umbrella)", "Pixel + OEM run — NOT RUN", "RD"),
 ("AC2 Defects logged with severity", "Defect register (this deliverable)", "MO"),
 ("P1 Usage access revoked while window active → NEEDS_DEVICE_ACCESS + repair path", E+"P1 (adb appops set … GET_USAGE_STATS ignore)", "EMU"),
 ("P2 Overlay (SYSTEM_ALERT_WINDOW) revoked → honest state, no false Active", E+"P2", "EMU"),
 ("P3 Exact-alarm access denied → protection window not Ready", E+"P3 (appops SCHEDULE_EXACT_ALARM deny)", "EMU"),
 ("S1 Window START/END fire on time; blocked app shows overlay (flash disclosed)", E+"S1", "EMU"),
 ("S2 Two adjacent/overlapping windows; END of one keeps the other (DEV-2)", E+"S2", "EMU"),
 ("S3 Overnight 22:00→06:00 + full calendar day across midnight (DEV-5/6)", E+"S3 (adb clock set)", "EMU"),
 ("S4 Local Stop works offline and signed out (DEV-4/10); block screen has no snooze", E+"S4 (airplane mode)", "EMU"),
 ("R1 Reboot inside window resumes; after window does not (DEV-21)", E+"R1 (adb reboot)", "EMU"),
 ("R2 Process death / force-stop of FocusGuardService mid-window", E+"R2 (am kill / am force-stop)", "EMU"),
 ("R3 Doze: boundary delivered after deviceidle force-idle (DEV-1 late START)", E+"R3 (dumpsys deviceidle force-idle)", "EMU"),
 ("R4 Time-zone change + DST fall-back 25 Oct (Europe/Berlin)", E+"R4 (settings put global auto_time_zone 0; setprop persist.sys.timezone)", "EMU"),
 ("R5 App update (MY_PACKAGE_REPLACED) re-arms windows (B2)", E+"R5 (adb install -r)", "EMU"),
 ("D1 OEM battery saver / app killer (Xiaomi/Realme/Vivo/Samsung) keeps FocusGuardService", "Real OEM phone only — NOT RUN", "RD"),
 ("D2 Long run: protection survives overnight on battery, low-battery mode", "Real phone only — NOT RUN", "RD"),
 ("D3 Same suite repeated on a stock Pixel", "Real Pixel — NOT RUN", "RD"))
T("DEV-89",
 ("No reachable/bundled UI, animation or asset paces breathing", B39+"TechniqueContentSafetyTest.breathingCatalogueGuidanceNeitherPacesNorClaims (techniques + catalogue only); Breathing*.kt screens and block-screen 'breathe' path not covered", "GAP"),
 ("No breathing physiological/medical/energy claims", M+"BreathingCatalogTest.c10_noShippedTextMakesAPhysiologicalOrSpiritualPromise; "+B39+"TechniqueContentSafetyTest.techniquesNeitherPaceTheBreathNorMakeBodyClaims", "AP"),
 ("Block-screen option renamed/re-scoped as non-paced pause", "Not built; manual check of BlockOverlayActivity/LocalPauseScreen", "GAP"),
 ("Automated safety test covers these files and fails on old content", B39+"TechniqueContentSafetyTest.patternsCatchTheOriginalBoxBreathingScript (covers techniques.json + .md only)", "GAP"),
 ("Tester verifies point by point", "Manual audit — not run", "MO"))

# ---------------- Epic C — Routine ----------------
T("DEV-11",
 ("(derived) 1 Local reboot recovery, no session", "see DEV-21 / BootRescheduleCatchUpTest", "AP"),
 ("(derived) 2 All-day covers calendar day; no 'unbreakable' copy", "see DEV-6", "AP"),
 ("(derived) 3 Ending one window doesn't release another", "see DEV-2 / FocusWindowEndHandlerTest", "AP"),
 ("(derived) 4 Saved ≠ Active until engine confirms", "see DEV-3 / FocusScheduleStatusTest", "AP"),
 ("(derived) 5 Two destinations Routine + Sadhana", M+"RoutineShellTest; ShanivarShellTest", "AP"),
 ("(derived) 6 Local day plan; saving an activity does not arm an alarm", M+"RoutineProtectionTest.activityOrAnchorEditsNeverTouchTheRegistry; AndroidProtectionGatewayTest.savingThePlanReplacesTheRegistryAndActivitiesDoNot", "AP"),
 ("(derived) 7 Timeline and Edit Routine from stored data", M+"RoutineTimelineTest; RoutinePlanRepositoryTest", "AP"),
 ("(derived) 8 Multiple windows, overnight, installed apps, honest permissions, local stop", M+"RoutineProtectionTest (31 tests); FocusSchedulerRegistryTest", "AP"),
 ("(derived) 9 Physical phone proof, no emulator", DEV7, "RD"),
 note="Ordered build list used as ACs. ROUTINE_V3 flag still false on main.")
T("DEV-24",
 ("(derived) Draft survives process death, resumes at checkpoint", M+"InitiationControllerTest.resumesAtPersistedCheckpointAfterProcessDeath; InitiationOnboardingTest", "AP"),
 ("(derived) Google/email/guest adapters (real providers stubbed)", M+"InitiationControllerTest.googleAndEmailHonestlyUnavailableGuestStillWorks", "AP"),
 ("(derived) Guest→account migration without duplicates", "Real providers not built (DEV-25)", "GAP"),
 ("(derived) Cancel/error/offline/code-expiry keep the draft", M+"InitiationOnboardingTest (partial — no code-expiry provider)", "GAP"),
 note="No AC section in Jira.")
T("DEV-26",
 ("(derived) Screens faithful to initiation-v1 wireframe", "Screenshot comparison /workspace/qa/initiation/*.png", "MO"),
 ("(derived) Guest works end to end", M+"InitiationControllerTest.guestFlowEndToEndUsesRealPlan; InitiationUiTest", "AP"),
 ("(derived) No pacing, still planet, reduced motion", M+"InitiationSittingTest; InitiationControllerTest.guidanceIsTextOnlyByRemainingSeconds", "AP"),
 ("(derived) Step 6 accessibility review + device QA", "Not run", "RD"),
 note="No AC section in Jira.")
T("DEV-52",
 ("routine_plans revision/time_zone/practice_local (forward-only)", "Not built", "GAP"),
 ("put_routine RPC: match → write+increment; mismatch → 409", "Not built ("+B45+"SyncEngineErrorClassTest.routineParksWhilePutRoutineIsMissing… parks client-side)", "GAP"),
 ("Direct client insert/update revoked; only RPC writes", "Not built", "GAP"),
 ("Protection windows have one home (decision record)", "Decision pending", "MO"),
 ("Package names not sent to server without founder approval", B45+"SyncWorkManagerTest.routinePayloadStripsProtectedPackagesUntilDev52", "PEND"),
 ("409 surfaced, rebase + retry once", B45+"SyncEngineErrorClassTest.routineConflictParksAndPublishesTheCanonicalRow", "PEND"),
 ("pgTAP+integration: stale 409, increment, bad TZ, cross-user, RoutineSync contract", "Not built", "GAP"))
T("DEV-53",
 ("alarm_configs / alarm_observations / device_observations tables", "Not built", "GAP"),
 ("device_id is random per-install UUID", "Not built", "GAP"),
 ("Append-only/last-value; own-row RLS + DEV-42 baseline", "Would be enforced by "+B42+"01_baseline_guard (table missing)", "GAP"),
 ("Server never overrides local alarm state", M+"DeviceObservationsTest.userDisabledStaysOffWhateverObservationSays; oldConfigRevisionNeverApplies (client side only)", "GAP"),
 ("Retention rule documented and enforced", "Not built", "GAP"),
 ("Outbox kinds + payload builders with tests", "Not built", "GAP"),
 ("pgTAP: cross-user, checks, device_id format", "Not built", "GAP"))
T("DEV-72",
 ("Integration: 5 identical completion requests → 1 row", "Not built (completion semantics undefined)", "GAP"),
 ("Offline replay → no duplicates", "Not built", "GAP"))
T("DEV-73",
 ("New user reaches first completed action unaided (usability test)", "Moderated usability session", "MO"),
 ("Failed save never shows success", M+"RoutineProtectionTest.invalidSaveDoesNotSync; saveMessagesNeverClaimActive (protection only; no generic failed-save test)", "GAP"))
T("DEV-74",
 ("After 1, 3, 7 missed days a clear way back", "Not built", "GAP"),
 ("Copy reviewed for guilt", "Copy review", "MO"))
T("DEV-75",
 ("AC1 All alarm/notification cases pass or have documented defects (umbrella)", "Pixel + OEM run — NOT RUN", "RD"),
 ("AC2 No unresolved critical", "Defect register gate", "RD"),
 ("A1 Wake alarm fires at set time from locked screen; full-screen intent shown (Android 14+)", E+"A1", "EMU"),
 ("A2 Alarm fires in Doze (setAlarmClock) and after app swipe-away", E+"A2", "EMU"),
 ("A3 Alarm re-armed after reboot and app update; recurrence recomputed after fire (0008)", E+"A3", "EMU"),
 ("A4 Exact-alarm permission denied → honest 'permission required', no silent failure", E+"A4", "EMU"),
 ("A5 Notification permission (POST_NOTIFICATIONS) denied → state shown", E+"A5", "EMU"),
 ("A6 DST fall-back / TZ change: one alarm, at local wall-clock time", E+"A6", "EMU"),
 ("A7 Accessible dismissal path always available; QR/photo path with emulator camera", E+"A7", "EMU"),
 ("A8 Alarm during a Sadhana practice interrupts and is recorded truthfully", E+"A8", "EMU"),
 ("A9 Hard cap ends a ringing alarm; no endless ring", E+"A9", "EMU"),
 ("D1 Movement dismissal with real step detector / ACTIVITY_RECOGNITION", "Real phone sensors — NOT RUN", "RD"),
 ("D2 Alarm actually wakes a person: volume, vibration, speaker, DND interplay", "Real phone — NOT RUN", "RD"),
 ("D3 OEM battery killer does not suppress the alarm (Xiaomi/Realme/Vivo/Samsung)", "Real OEM phone — NOT RUN", "RD"),
 ("D4 Device powered off across alarm time; behaviour on power-on", "Real phone — NOT RUN", "RD"))
T("DEV-12", ("All 66 breathing tests still pass", M+"Breathing*Test", "AP"),
 ("200% font: Begin/Pause/End/Back reachable", "Not built (parked)", "GAP"),
 ("No per-second countdown announcement", "Not built (parked)", "GAP"),
 ("Native screenshots vs reference", "Not built (parked)", "MO"), note="Parked; re-scope (Sadhana V1 replaces Steady).")
T("DEV-13", ("No user-facing literal left in Kotlin", "Not built", "GAP"),
 ("All tests pass with string resources", "Not built", "GAP"),
 ("Locale switch changes a visible string E2E", "Not built", "GAP"), note="Later.")
T("DEV-23", ("(derived) No circle/mark paces breath; static under reduced motion", M+"SadhanaSpineTest.rootPulseIsRandomisedNotPeriodicAndStaticUnderReducedMotion; shimmerIsRandomisedNotPeriodic (Sadhana only)", "GAP"),
 ("(derived) No scores/ranks", "Copy review", "MO"), note="Later. No AC section.")

# ---------------- Epic D — Sadhana ----------------
T("DEV-16", ("(derived) Founder decision recorded (Option A/B/C)", "Decision — no test", "MO"), note="Decision ticket; later.")
T("DEV-27", ("(derived) Research-only lock is deterministic; activity never unlocks it", M+"SadhanaPracticeTest.researchOnlyNeverLaunchesEvenWhenUnreviewedAllowed; intensiveRiskNeverLaunchesEvenIfMislabelledReleased; SadhanaRecordTest.longPracticeAloneNeverAdvancesBeyondArrive", "AP"), note="No AC section.")
T("DEV-28", ("(derived) Active foreground seconds only; pauses excluded", M+"SadhanaAttemptTest.pausedAndBackgroundTimeExcluded; PracticeAttemptTest.pausedTimeNotCounted", "AP"),
 ("(derived) anchor_changed recorded", M+"SadhanaAttemptTest.anchorChangePausesAndIsLoggedNotFailure", "AP"),
 ("(derived) Process death → interrupted, never completed from wall clock", M+"SadhanaControllerTest.processDeathRestoresInterruptedNotCompleted", "AP"),
 ("[DoD] Process death / lock / call on a real phone", "DEV-34 device QA", "RD"), note="No AC section.")
T("DEV-29", ("(derived) Optional reflection; editable/deletable self-reports", M+"SadhanaRecordTest.reflectionOptionalAndClearable; selfReportsEditableAndDeletable", "AP"),
 ("(derived) 'Use fewer prompts' control", M+"SadhanaRecordTest.guidanceReducesOnlyByPreference", "AP"),
 ("(derived) Orbit points from evidence families; self-reports never scores", M+"SadhanaRecordTest.orbitEventsFromEvidenceFamilies; LivingOrbitTest.independentRecordMustBeSelfReported", "AP"), note="No AC section.")
T("DEV-30", ("(derived) Layers separate; centres study-only until verified", M+"SadhanaSpineTest.noCentreCanLaunchPracticeUntilVerified; plainLayerNamesNoTraditionOrSanskrit", "AP"), note="No AC section.")
T("DEV-31", ("(derived) Screens faithful to sadhana-v1", "Screenshots /workspace/qa/sadhana/*.png", "MO"),
 ("(derived) No energy/health reading", M+"SadhanaSpineTest.noEnergyOrHealthReadingInPlainText", "AP"), note="No AC section.")
T("DEV-32", ("(derived) Stationary gate, End always reachable, no navigation away", M+"SadhanaControllerTest.beginNeedsStationary; cannotNavigateAwayFromActivePractice; SadhanaAccessibilityTest", "AP"),
 ("(derived) Reduced motion / still planet", M+"SadhanaLoadingTest", "AP"), note="No AC section.")
T("DEV-33", ("(derived) Research-only methods only as locked study cards", M+"SadhanaPracticeTest.studyOnlyAndQualifiedContextNeverLaunch; SadhanaUiTest", "AP"), note="No AC section.")
T("DEV-34", ("(derived) Content review manifests recorded per practice", M+"ContentReviewTest.releaseLaunchNeedsACompleteMatchingManifest (gate only; manifests empty)", "MO"),
 ("(derived) Device QA: process death, screen lock, calls", "Not run", "RD"),
 ("(derived) TalkBack + 200% font", M+"SadhanaAccessibilityTest.at200PercentFont (font only; TalkBack not run)", "RD"), note="No AC section. Release blocker.")
T("DEV-35",
 ("B3 Local idempotent sync outbox", M+"SyncOutboxTest.queueIsIdempotentPersistedAndDrainsInOrder", "AP"),
 ("B4 Release launch needs a complete matching manifest", M+"ContentReviewTest.releaseLaunchNeedsACompleteMatchingManifest; SadhanaControllerTest.releaseBuildBlocksUnreviewed", "AP"),
 ("B5 Choose linked to a saved enabled Routine window", M+"RoutineBoundaryLinkTest.onlyAnEnabledSavedWindowLinksChoose", "AP"),
 ("B6 Onboarding 60 s attempt imported once", M+"SadhanaControllerTest.initiationAttemptImportedOnceAndUnfinishedIgnored", "AP"),
 ("B7 Edit actual minutes of a self-report", M+"SadhanaControllerTest.selfReportActualMinutesEditableAndPersisted", "AP"),
 ("B8 End visible at 200% font and landscape", M+"SadhanaAccessibilityTest.at200PercentFont; inLandscape", "AP"),
 note="QA pass, except B3 → defect DEV-38. ACs = checklist items B3–B8.")
T("DEV-36", ("1 Stage 1 identity, text guidance, audio must not pace breath", M+"SadhanaStage1Test.stage1Is60sNoBreathPacingAndHonestCopy (text only; no audio yet)", "AP"),
 ("2 60 s start, end anytime, focus lock, posture", M+"SadhanaFocusLockTest; SadhanaStage1Test", "AP"),
 ("3 Licensed audio, no claims, merged into practice", "Not built (DEV-37 spike)", "GAP"),
 ("4 Check-in = exactly 2–3 one-tap options", "Shipped 47e668a card has stepper+chips+note — would FAIL; no test asserts ≤3 taps", "GAP"),
 ("5 Founder-approved public wording only", "Copy review", "MO"),
 ("6 No build until founder answers", "Process", "MO"), note="Do not build yet (founder).")
T("DEV-37", ("(derived) Spike note: Hz options, licence, non-claim copy, go/no-go", "Doc review", "MO"), note="Spike; no AC section.")
T("DEV-39",
 ("(derived) No breath counts / timed pacing in techniques.json or .md sources", B39+"TechniqueContentSafetyTest.techniquesNeitherPaceTheBreathNorMakeBodyClaims; markdownSourcesNeitherPaceNorClaimNorTime; patternsCatchTheOriginalBoxBreathingScript", "AP"),
 ("(derived) No physiological claims", B39+"TechniqueContentSafetyTest.techniquesNeitherPaceTheBreathNorMakeBodyClaims", "AP"),
 ("(derived) md mirror matches shipped asset", B39+"TechniqueContentSafetyTest.mdMirrorMatchesTheShippedAsset", "AP"),
 note="Failed once on 'rounds' (3471087); passed at 8e122a8 incl. mutation runs (/workspace/qa/dev39-mut-*.log).")
T("DEV-51", ("Content tables mirror Kotlin", "Not built", "GAP"), ("RLS serves only approved+manifested, never research-only", "Not built", "GAP"),
 ("Tradition sources only in Deep study", "Not built", "GAP"), ("seed.sql only founder-reviewed content", "Not built", "GAP"),
 ("Seed text post-DEV-39, nothing from legacy backend", "Not built", "GAP"), ("Offline fallback never launches unreviewed", "Not built", "GAP"),
 ("pgTAP: draft/research invisible, no writes, manifest required", "Not built", "GAP"), note="Later.")
T("DEV-54",
 ("practice_reflections table + own-row RLS", B54+"07_sadhana_record.test.sql ('A sees only own reflections', 'A cannot write a reflection for B')", "GAP"),
 ("self_reports private_by_default, drift_reasons check, tombstone delete E2E", B54+"07_sadhana_record.test.sql ('an invalid drift reason is rejected', 'self-report tombstone deletes end to end')", "GAP"),
 ("practice_attempts anchor/anchor_changes/events(32 KB)/created_at; SyncPayloads.attempt", B54+"07 ('anchor outside BREATH/ROOM is rejected', 'an oversized events value is rejected')", "GAP"),
 ("orbit_events entity_id/created_at/type+source checks; append-only", B54+"07 ('invalid orbit type is rejected', 'an orbit update is denied')", "GAP"),
 ("Payload builders match every column; contract test", "Not found", "GAP"),
 ("pgTAP coverage list", B54+"07_sadhana_record.test.sql", "GAP"),
 ("SelfCheck stays local-only (no table)", B54+"07 ('no SelfCheck table (local-only)')", "GAP"),
 note="Work in progress (uncommitted in worktree); tests exist but not yet run by QA → counted as gap until QA run.")
T("DEV-76", ("Practice→need→action→clarity table complete", "Doc review", "MO"), ("Content issues ticketed", "Doc review", "MO"))
T("DEV-77", ("Skip works in one tap", "Not built", "GAP"), ("Stored locally as self-report, never a score", "Not built", "GAP"), note="Later.")
T("DEV-78", ("Links work", "Not built", "GAP"), ("Manifest (DEV-34) approved", "Founder", "MO"), note="Later.")

# ---------------- Epic E — Security/privacy/reliability ----------------
T("DEV-8", ("Every rotated credential verified rejected", "Manual API check (founder op) — not done", "MO"),
 ("Production health 200 / database ok", "Manual check", "MO"), ("Decision 0005 step 1 marked complete", "Doc", "MO"), note="Release blocker. Founder-only.")
T("DEV-9", ("All backend tests pass", "backend/test/*.test.js", "GAP"), ("Rollback demonstrated", "Not done", "GAP"),
 ("One migration through runner", "Not done", "GAP"), ("Pooler path unchanged", "Manual", "MO"), note="Re-scope: legacy Render only (DEV-40).")
T("DEV-17", ("(PM-refined) Keystore custody (founder)", "Manual", "MO"), ("signingConfig from env; build fails if missing", "Not built", "GAP"),
 ("prodRelease/stagingRelease endpoints", "Not built", "GAP"), ("DNS only if Render kept", "Decision", "MO"),
 ("release.yml draft release + SHA-256", "Not built", "GAP"), ("Key SHA-1/256 recorded", "Manual", "MO"), ("versionCode auto-increments", "Not built", "GAP"),
 ("Content-review gate respected in release", M+"ContentReviewTest; SadhanaControllerTest.releaseBuildBlocksUnreviewed", "AP"),
 ("Play declarations recorded", "Manual", "MO"), ("Signed APK installs on phone and syncs", "Real device", "RD"), note="ACs from PM comment 17 Oct (awaiting founder).")
T("DEV-18", ("(derived) Render GEMINI_API_KEY state recorded", "Manual dashboard check", "MO"), note="Not in first release.")
T("DEV-22", ("(derived) none — future", "—", "GAP"), note="Later; no ACs.")
T("DEV-25", ("Guest = anon user once per install", B45+"SyncSessionTest.guestSignsUpOncePerInstallAndSurvivesRestart", "PEND"),
 ("Email OTP + invite gate; guest upgrade keeps user_id", "Not built", "GAP"), ("Google via Credential Manager; linkIdentity keeps user_id", "Not built", "GAP"),
 ("enable_manual_linking on hosted projects", "Cloud config — Backend (QA must not touch cloud)", "MO"),
 ("Unconfigured providers hidden / 'not available yet'", M+"InitiationControllerTest.googleAndEmailHonestlyUnavailableGuestStillWorks", "AP"),
 ("Sign-out clears tokens; no cross-user send", B45+"SyncSessionTest.signOutClearsTokensAndNotifies; SyncWorkManagerTest.signOutKeepsItemsForTheOldUserUnsent", "PEND"),
 ("Tests: guest→email/Google same user_id; wrong/expired OTP fail", "Not built", "GAP"),
 ("Privacy notice updated before non-founder testers", "Founder", "MO"), ("Legacy /auth/* unused", "Code review", "MO"), note="ACs from PM comment 17 Oct.")
T("DEV-38",
 ("(derived) recordOwn queues a self-report", M+"SyncOutboxGapsTest.recordOwnQueuesSelfReport", "AP"),
 ("(derived) Edits replace the queued self-report", M+"SyncOutboxGapsTest.adjustOwnActualReplacesQueuedSelfReport", "AP"),
 ("(derived) Delete queues a tombstone over pending upsert", M+"SyncOutboxGapsTest.deleteOwnQueuesTombstoneOverPendingUpsert; senderDeletesTombstonesAndUpsertsTheRest", "AP"),
 ("(derived) Onboarding import queued once (+ legacy backfill)", M+"SyncOutboxGapsTest.onboardingImportQueuesAttemptOnce; backfillQueuesOnboardingAttemptAlreadyImportedByOldBuild; backfillIsIdempotentByAttemptId; backfillRunsOnceAcrossRestarts", "AP"),
 ("(derived) Routine changes queued", M+"SyncOutboxGapsTest.routineChangeQueuesRoutinePlan", "AP"),
 note="Failed once (legacy-import gap at 86b558a); passed at 20fb519 (/workspace/qa/dev38/rerun-*). Jira still Open.")
T("DEV-40", ("1 Decision record 0019 lists every ShanivarApi endpoint + disposition", "Doc review", "MO"), ("2 Founder confirms in writing", "Founder", "MO"),
 ("3 Tables → DEV-42; Edge Functions → own tickets", "Doc review", "MO"), ("4 Render untouched until replacement verified", "Process", "MO"),
 ("5 Legacy data carry-over stated", "Doc review", "MO"), ("6 0001/0004/DEV-9 marked legacy-only", "Doc review", "MO"))
T("DEV-41", ("1 shanivar-staging exists (ap-south-1)", "Backend/founder (QA must not touch cloud)", "MO"), ("2 Auth settings recorded for both", "Doc review", "MO"),
 ("3 Keys in password manager + GH env secrets only", "Secret scan", "MO"), ("4 Plan/pause/backup/spend cap written", "Doc review", "MO"),
 ("5 Least-privilege access; revocable CI token", "Manual", "MO"), ("6 /auth/v1/health 200 on mobile data", "Real phone", "RD"))
T("DEV-42",
 ("1 evidence_type checks (forward-only)", B42+"01_baseline_guard ('…evidence_type has a check'); 02_rls_isolation ('self_report cannot be labelled app_observed')", "AP"),
 ("2 created_at + set_updated_at() everywhere; client_updated_at", B42+"01_baseline_guard; 03_client_updated_at", "AP"),
 ("3 Size caps (plan 64 KiB, packages 200, drift 20, id 128, note 500)", B42+"04_size_caps (14 asserts) — other text columns uncapped (QA-42-03)", "AP"),
 ("4 revoke all from anon; authenticated minimal", B42+"05_privileges", "AP"),
 ("5 pgTAP: cross-user CRUD, no session, user_id immutable, spoofing, oversize", B42+"02_rls_isolation (43); 04_size_caps", "AP"),
 ("6 Baseline applied to every new table DEV-47..55", B42+"01_baseline_guard iterates all public tables (fails on new tables missing baseline)", "AP"),
 ("7 on delete cascade from auth.users verified", B42+"01_baseline_guard ('…references auth.users on delete cascade') — FK presence only; no delete-and-check test", "GAP"),
 ("8 Security/Performance Advisor 0 errors after deploy", "Cloud advisors — not deployed; QA must not touch cloud", "MO"),
 note="Passed at ba38de6 (local stack). Findings QA-42-01..03 in defect register.")
T("DEV-43", *[(a, "Not built (no .github/)", "GAP") for a in ["1 migration repair once, output pasted","2 PR workflow: reset, lint, test db","3 merge→staging deploy","4 prod via dispatch + founder reviewer","5 drift check fails job","6 forward-only; destructive label","7 secrets only in GH env","8 seed.sql fixed","9 broken migration fails CI"]])
T("DEV-44", *[(a, "Not built", "GAP") for a in ["1 env flavours staging/prod side by side","2 URL/key from properties only","3 release fails without https URL/key","4 cleartext debug-only","5 CI secret scan (gitleaks)","6 env name shown on non-prod","7 API_URL removed/flavour-aware","8 docs per flavour"]])
T("DEV-45",
 ("1 SyncSession: guest once per install, encrypted tokens, refresh", B45+"test/SyncSessionTest (8)", "PEND"),
 ("2 Items stamped with owner user_id, never sent as another user", B45+"test/SyncEngineErrorClassTest.itemsOfAnotherUserAreNeverSentAndUnownedOnesAreStamped", "PEND"),
 ("3 WorkManager outbox-flush, CONNECTED, backoff 30 s–1 h", B45+"test/SyncWorkManagerTest; SyncOutboxEngineTest.backoffIsExponentialWithJitterFrom30sTo1h", "PEND"),
 ("4 Error classes (retry / 401 refresh / dead-letter & continue / ordering)", B45+"test/SyncEngineErrorClassTest (20)", "PEND"),
 ("5 Idempotency (3 sends → 1 row, crash-after-ack, tombstone)", B45+"test/SyncOutboxEngineTest; supabase/tests/06_sync_conflicts", "PEND"),
 ("6 Conflict rule LWW on client_updated_at; tombstones; put_routine 409", B45+"supabase/tests/06_sync_conflicts (22); decision 0019", "PEND"),
 ("7 Restore (only if multi-device wanted)", "Founder decision pending", "MO"),
 ("8 Offline 7 days/50 items; airplane toggles", B45+"test/SyncOutboxEngineTest.sevenDaysOfflineFiftyItemsAllArriveOnReconnect; airplaneModeTogglesMidFlushLoseNothing", "PEND"),
 ("9 MockWebServer per error class + WorkManager test; suite green", B45+"test/SyncEngineErrorClassTest; SyncWorkManagerTest", "PEND"),
 ("10 Verified once against shanivar-prototype", "Cloud project — out of QA bounds; Backend to run", "MO"),
 note="In QA now — PENDING.")
T("DEV-46", *[(a, "Not built", "GAP") for a in ["1 delete-account verifies JWT, cascades, idempotent","2 service-role only in function env","3 Settings two-step delete; clears session+outbox","4 client-side export","5 web deletion path"]],
 ("6 Staging tests: rows gone, others untouched, old JWT rejected, logs clean", "Not built", "GAP"), ("7 Deployed only via DEV-43", "Process", "MO"))
T("DEV-47",
 ("1 One AppSync from BuildConfig; empty URL = sync off, tested", B47+"AppSyncTest.emptyUrlOrKeyOrMissingEngineMeansDisabled; SyncStatusUiTest.syncOffRecordViewIsIdentical", "AP"),
 ("2 Guest ensureGuest after consent, never blocks", B47+"InitiationSyncTest.guestStepCallsEnsureGuestExactlyOnce; offlineGuestSignInNeverBlocksTheFlow", "AP"),
 ("3 Flush triggers (enqueue, foreground, network, periodic), off main thread", B47+"AppSyncTest.enqueueTriggersRequestFlush; foregroundRequestsFlush; SyncWiringRobolectricTest", "AP"),
 ("4 Quiet status line, three strings, no counts/streaks", B47+"SyncStatusUiTest.statusFollowsTheEngineStateExactlyThreeStrings", "AP"),
 ("5 Dead-letter count only in debug diagnostics", B47+"SyncStatusUiTest.deadLetterCountOnlyInDebugDiagnostics", "AP"),
 ("6 Sign-out/reset clears session, no cross-user send", B47+"AppSyncTest.signOutClearsSessionThroughEngine; resetUsesDev45ResetAndClearsTheSession", "AP"),
 ("7 Compose/Robolectric tests listed", B47+"SyncStatusUiTest; InitiationSyncTest; AppSyncTest", "AP"),
 ("8 Manual: attempt + self-report reach tables within 1 min (+airplane)", "Needs device/emulator + staging; cloud off-limits for QA", "RD"),
 note="Developer run on branch: 548 tests, 0 fail (22:36). Not yet QA-reviewed; depends on DEV-45.")
T("DEV-48", *[(a, "Not built", "GAP") for a in ["1 invite_codes table (hash), RLS no client policy","2 before-user-created hook rejects bad codes","3 atomic redemption","4 guest→email gated, same user_id","5 email OTP; custom SMTP in prod","6 founder mint/revoke snippet","7 rate limits","8 pgTAP+integration incl. 2-signup race","9 invite-code field, plain copy"]])
T("DEV-49", *[(a, "Not built", "GAP") for a in ["1 feedback table","2 RLS insert/select own; no update/delete; no anon","3 submit_feedback RPC 10/day","4 via outbox, idempotent","5 no auto diagnostics","6 founder SQL query","7 pgTAP list","8 Settings entry, no ratings"]])
T("DEV-50", *[(a, "Not built", "GAP") for a in ["1 profiles table","2 auth.users trigger creates profile","3 guidance_preferences table","4 own-row RLS + DEV-42 baseline","5 stale revision → 409","6 payload builders + outbox kinds","7 no birth/email/phone in profiles"]])
T("DEV-79", ("Every table/RPC/Edge Function has a passing isolation test", B42+"02_rls_isolation covers 5 tables; "+B45+"06 tombstones; no RPC/Edge Function yet", "GAP"),
 ("Any failure is a release blocker", "Release gate (template §G4)", "MO"))
T("DEV-80", ("Audit report (logging, retention, deletion, backups)", "Not started", "GAP"), ("Restore drill on staging", "Needs DEV-41", "GAP"))
T("DEV-81", ("Rollback rehearsed on staging once", "Needs DEV-41; note d9168eb cannot be reverted alone (QA-42-01)", "GAP"), ("Linked to G6", "Doc", "MO"))

# ---------------- Epic F — QA & release ----------------
T("DEV-14", ("Forced crash → retrievable local report", "Not built", "GAP"), ("Export needs explicit user action", "Not built", "GAP"),
 ("No reflection/feeling/package in payload", "Not built", "GAP"), ("Network traffic inspected", "Proxy capture on device", "RD"))
T("DEV-15", ("Workflow green on main", "Not built (no .github/)", "GAP"), ("Broken test fails workflow", "Not built", "GAP"), ("No secrets needed", "Not built", "GAP"))
T("DEV-19", ("Non-interactive git push succeeds", "Out of QA scope (never push)", "MO"), ("No credential in config/URL/files", "Secret scan", "MO"), ("git fetch does not hang", "Manual", "MO"))
T("DEV-55", ("1 SupabaseSyncTest nightly vs staging", M+"SupabaseSyncTest.e2eGuestPushReadBack (local only; SKIPPED without -Dsupabase.e2e.url)", "GAP"),
 ("2 pgTAP vs staging after deploy", "Needs DEV-41/43", "GAP"), ("3 Device checklist on staging build", "Real phone", "RD"),
 ("4 Offline resilience (airplane, kill mid-flush, reboot)", "Real phone", "RD"), ("5 >1 h idle refresh, same user_id", "Real phone", "RD"),
 ("6 Mobile data + flaky network", "Real phone", "RD"), ("7 Second guest isolation; delete-account", "Needs DEV-46", "GAP"),
 ("8 put_routine 409 two devices", "Needs DEV-52", "GAP"), ("9 Tester never uses service-role key", "Process rule", "MO"))
T("DEV-82", ("Matrix covers epics A–G", "/workspace/qa/dev82/ac_test_matrix.md (this deliverable)", "MO"),
 ("Template used for first RC", "/workspace/qa/dev82/release_readiness_template.md — first RC not cut", "MO"))
T("DEV-83", ("Report per event: expected vs actual", "No events exist (DEV-65 not defined)", "GAP"))
T("DEV-84", ("E2E protect→act→reflect passes on two devices", "Not started; needs DEV-69/72/74", "RD"))

# ---------------- Epic A / G / process ----------------
T("DEV-63", ("Every §2A section present; epics linked; decision log; founder approves", "Doc review of Notion thesis (sections present; approval recorded 17 Oct)", "MO"))
T("DEV-64", ("Every journey step has ≥1 row; missing steps link tickets; diagram", "Doc review (thesis §5 has table + mermaid)", "MO"))
T("DEV-65", ("Doc reviewed by FE/BE/Tester/CS; every §6 metric traceable to events; privacy signed", "Doc review — not written", "GAP"))
T("DEV-66", ("Register exists, linked; each hypothesis has a test method", "Doc review (thesis §9 lists H1–H8 with tests)", "MO"))
T("DEV-85", ("Baseline numbers visible; limits stated", "Needs analytics (DEV-65/83)", "GAP"))
T("DEV-86", ("First 5 interviews; first monthly report", "CS deliverable review", "MO"))
T("DEV-87", ("Spec approved; links to C3", "Doc review", "MO"), note="Later.")
T("DEV-88", ("Checklist applied to site, onboarding and store listing", "Manual claim audit — not run", "MO"))
T("DEV-20", ("(derived) Founder picks option A/B/C", "Decision", "MO"), note="Process; no epic.")
