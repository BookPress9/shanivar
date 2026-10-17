# 4. Product truth: feature verification

Verified on 17 Oct 2026 against the repo `/workspace/shanivar-v3` (`main` @ `20fb519`):
- code in `android-app/app/src/main/java/com/shanivar/nativeapp/`
- `docs/HANDOFF_NEXT.md` and `docs/FINALIZATION_CHECKLIST.md` (13 Oct audit; 496 unit tests, 0 failures)
- build flags in `android-app/app/build.gradle.kts`
- `docs/strategy/shanivar-launch-backlog.md`
- marketing pack `marketing/shanivar/routine-v3-onboarding/`
- Notion pages *Sadhana* and *Mano Vijnana (App Content)*

**What the labels mean**
- **Built** = the code is on `main`, passes unit tests and runs in the founder's debug test build. It is **not released**, and physical-device QA (alarms, reboot, process death, TalkBack) is still outstanding.
- **In development** = partly built, flagged off, or blocked on services or founder review.
- **Planned** = backlog only.

**Marketing rule:** even "Built" features may only be described as "being built" or "in the test build", never as available.

| Feature | Status | Evidence | How the campaign may use it |
|---|---|---|---|
| Initiation onboarding: name, guest start, wake/sleep anchors, first boundary, first practice, Living Orbit, first-week invitation | Built (debug only; release flag `INITIATION_V1 = false`) | `InitiationOnboardingScreens.kt`, `InitiationRoutineScreens.kt`, `InitiationOrbitScreens.kt`; HANDOFF_NEXT | Labelled test-build footage (R10). Lines: "Some things can wait.", "Tomorrow begins with a decision tonight." |
| First boundary / app protection: choose apps to make less accessible during protected time; essential and emergency apps remain; local stop; needs Usage access + Display over other apps | Built (device QA pending) | Lines "Choose apps to make less accessible during your protected time.", "Essential and emergency apps remain accessible.", "Make room for one uninterrupted hour."; `ForegroundAppBlocker.kt`, `BlockOverlayActivity.kt` | R10, R11. Never say "blocks", "unbreakable" or "impossible to bypass". The browser and other devices are not covered. |
| Routine: wake/sleep anchors, protection windows with repeat days, timeline, reconcile on reboot, time change and update | Built in debug; Routine V3 shell `ROUTINE_V3 = false` in release | `RoutinePlan.kt`, `RoutineTimeline.kt`, `FocusWindowRecurrence.kt`; B1/B2 commits | Concept only: "give the day a shape". No "set multiple windows tonight". |
| Wake alarm: one editable alarm, repeat days, test sound, movement or non-walking dismissal, reboot reschedule | Built (physical-device QA pending) | `AlarmScheduler.kt`, `MovementDismissal.kt`, `BootRescheduleReceiver.kt`; "Make your chosen wake time your alarm?" | Not featured. It is off-theme, and reliability is unverified on real phones. |
| "One deliberate minute": first 60-second practice in onboarding | Built (debug) | "One deliberate minute.", "Give yourself one minute.", "I'm stationary and in a suitable place." | R09, R10. Show the concept; no breath instruction in campaign copy. |
| Sadhana: 33-stage Living Spine; Path Arrive → Return → See → Choose → Carry; Saturn loading surface (NASA PIA06193, credited) | Built (debug, `SADHANA_V1`); release launches **no** practice until founder content-review manifests exist (B4) | `SadhanaSpine.kt`, `SadhanaPractice.kt`, `SadhanaLoading.kt` | Mention only as "Sadhana, an initiation, in development". |
| Sadhana practices: Stage 1 · The base (60 s); Receive the room; The moment before the reach; Protect one chosen action; Return to one ordinary breath | Built (debug); public release gated on content review | `SadhanaCatalog` in `SadhanaPractice.kt` | "The moment before the reach" (Notice the reach. Name what you intended to do before you decide.) is the in-app twin of R01's practice. Do not promote the breath practice (brand rule). |
| Stage 1 self-check: "Where did your attention go?"; drift estimate; reasons incl. "Phone / notification urge"; "Reflection only, not a measurement." | Built (debug) | `SadhanaStage1.kt`; decision 0018 | R10: show as the honest question "What pulled it away?" |
| Sadhana focus lock during practice: all-apps block; Do Not Disturb if granted; opt-out; "Focus lock is off: the permissions it needs are not granted. Nothing is blocked." | Built (debug) | `SadhanaFocusLock.kt`; decision 0017 | May say: "the minute is protected while you practise", with the test-build label. |
| Record: "Practise without the player" / record a practice done without the app; "Missed days remove nothing."; "Continuity, not a streak." | Built (debug) | `SadhanaRecordScreens.kt`, `SadhanaHomeScreens.kt`, `InitiationOrbitScreens.kt` | R07, R10. Strong proof that there are no streaks and no shame. |
| Accounts (Google, email) and cloud sync (Supabase) | In development (DEV-25; local outbox only; Google/email show "not available yet") | HANDOFF_NEXT; `SyncOutbox.kt`, `SupabaseSync.kt` (flagged) | Don't mention. |
| Hindi UI in the new onboarding/Routine/Sadhana | In development / not verified (no `values-hi` resources; only the legacy onboarding has a हिंदी toggle) | `res/` has only `values`; `OnboardingFlow.kt:87` | No Hindi product claims. Hindi editorial is fine with fluent review. |
| Play Store release / public download | Planned (DEV-17; no release) | marketing pack README | Never say "download" or "available". |
| Waitlist on shanivar.com (Netlify Forms, UTM copied into hidden `source` field) | Live as of 17 Oct (HTTP 200, "early-access" form), but **founder says DNS is pending** | curl check; `website/README.md` | Use only after founder confirmation. Until then: profile + DM keyword. |
| Breathing (VBT) pillar | Built in older code, outside the current shell | `Breathing*.kt`, decision 0011 | Excluded (brand rule: no breath pacing). |
| QR/camera alarm missions, multiple alarms, sleep tracking | Planned / deferred (some code exists) | backlog | Don't mention. |
| AI reflection / "scroller", astrology and birth charts, payments, WhatsApp/referrals, family dashboards | Planned (later) | backlog; live site lists "Reflection" as planned | Don't mention. Astrology is never implied as a feature. |
| XP, levels, ranks, streaks | Hold (decisions 0009/0013) | backlog | Never. |
