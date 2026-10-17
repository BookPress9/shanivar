# Frontend alignment: "Protect. Become Aware. Enter Flow." (Shanivar Frontend, 17 Oct 2026)
Read-only review. Paths under android-app/app/src/main/java/com/shanivar/nativeapp/ unless noted. Base: dev-47-sync-wiring (contains main b372079) + dev-39.

## 1) Must change
- BlockOverlayActivity.kt: no choice recording (DEV-69); shows raw package name not app label; no essential-access stop (DEV-10); comment says pause is primary but "Return to my day" is primary.
- assets/techniques.json on main still has box-breathing + claims; fixed in DEV-39 (PR #1, unmerged). Legacy ShanivarApp.kt:1008 loads it.
- Legacy streak: ShanivarApi.kt/AccountState.kt `streak`; ShanivarApp.kt:1093 "does not update your account streak yet". Release API_URL still old Render backend (conflicts F-03 Supabase-only).

## 2) Exists
- Initiation V1: 11 persisted checkpoints, guest path, 60 s sitting; Orbit + Week; LivingOrbit.kt additive (no decay).
- Sadhana S1–S7 + Stage 1: breath/room anchor, End, no-pacing copy, research-only locked, release gate (ContentReview.kt).
- Breathing*: model can't express timed breath steps (BreathingModels.kt, BreathingCatalog.kt); one-shot tone.
- Protection: honest status incl. "window open but not protecting" (FocusScheduleStatus.kt), FocusGuardService.kt polling, SadhanaFocusLock.kt essentials allow-list, flash disclosure.
- Flags: Initiation, Sadhana, sync debug-only.

## 3) Missing
- Block-screen choice capture / events → WIAC unmeasurable (DEV-65/69).
- Missed-step recovery (DEV-74); stored goal/segment.
- Runtime reaction to revoked usage access/overlay or killed service; battery-optimisation guidance.
- Real Google/email auth (placeholder InitiationOnboarding.kt:160).
- Body-scan sitting text hardcoded (InitiationSitting.kt:24), not in reviewed catalogue.

## 4) Dependencies / risks
- Android: polling → expected flash; revocable usage/overlay/exact-alarm; Accessibility refused (decision 0002); Play limits on USE_EXACT_ALARM, specialUse FGS, full-screen intent; Doze/OEM killers untested; DEV-7 never on a real device.
- Blockers: release launches no practice until founder content manifests (DEV-34); DEV-8 credential rotation; DEV-41 Supabase plan.

## Conflicts for founder (not resolved)
- Thesis "first action without full setup" vs initiation-v1 order (anchors→alarm→apps→window→access before practice).
- "Low energy" self-check (SadhanaSpine.kt:15, BreathingModels.kt:21, BreathingRecommender.kt:168 "your energy is low") vs no-energy-claims.
- 33-segment spine counter ("%02d / 33", SadhanaHomeScreens.kt:49-51) may read as levels; decision 0013 only proposed.
- Sign-in: wireframe shows Google + email; Q5 open.
- Breathe & reset as separate entry: Q2 open.

## 5) First release
Initiation + Sadhana Stage 1 (after content review); block screen with choice capture + essentials stop; honest status + failure surfacing; DEV-39 content; guest sync. Conditional on DEV-7/71 real-device pass and founder rulings above.

## 6) Verification
Existing unit/UI tests (BreathingCatalogTest, FocusScheduleStatusTest, SadhanaStage1Test, InitiationUiTest, SyncWiringRobolectricTest); DEV-83 event-exactly-once; real-device/OEM runs DEV-7/71/75 (revocation, reboot, DST); TalkBack + 200% font; wireframe verify.mjs; founder content review.

## Ticket mapping
- DEV-89 → Become Aware + standing rules: Breathing*.kt, block-screen breathe path, LocalPauseScreen.kt, techniques.json, "energy" labels; decides Q2.
- DEV-67 → Protect (truthful status): Android limits above + release API_URL.
- DEV-70 → Protect principle 3: extend FocusScheduleStatus "not protecting" to live revoked-permission / killed-service surfacing.
- DEV-73 → Goal→Action per segment; needs wireframe-order ruling + decision-log entry (F-06).

Note: thesis cites "main @ 20fb519", but that is DEV-38; main is b372079.
