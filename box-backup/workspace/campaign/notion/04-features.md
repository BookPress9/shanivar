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

<table header-row="true">
<tr><td>Feature</td><td>Status</td><td>Evidence</td><td>How the campaign may use it</td></tr>
<tr><td>Initiation onboarding: name, guest start, wake/sleep anchors, first boundary, first practice, Living Orbit, first-week invitation</td><td>Built (debug only; release flag `INITIATION_V1 = false`)</td><td>`InitiationOnboardingScreens.kt`, `InitiationRoutineScreens.kt`, `InitiationOrbitScreens.kt`; HANDOFF_NEXT</td><td>Labelled test-build footage (R10). Lines: "Some things can wait.", "Tomorrow begins with a decision tonight."</td></tr>
<tr><td>First boundary / app protection: choose apps to make less accessible during protected time; essential and emergency apps remain; local stop; needs Usage access + Display over other apps</td><td>Built (device QA pending)</td><td>Lines "Choose apps to make less accessible during your protected time.", "Essential and emergency apps remain accessible.", "Make room for one uninterrupted hour."; `ForegroundAppBlocker.kt`, `BlockOverlayActivity.kt`</td><td>R10, R11. Never say "blocks", "unbreakable" or "impossible to bypass". The browser and other devices are not covered.</td></tr>
<tr><td>Routine: wake/sleep anchors, protection windows with repeat days, timeline, reconcile on reboot, time change and update</td><td>Built in debug; Routine V3 shell `ROUTINE_V3 = false` in release</td><td>`RoutinePlan.kt`, `RoutineTimeline.kt`, `FocusWindowRecurrence.kt`; B1/B2 commits</td><td>Concept only: "give the day a shape". No "set multiple windows tonight".</td></tr>
<tr><td>Wake alarm: one editable alarm, repeat days, test sound, movement or non-walking dismissal, reboot reschedule</td><td>Built (physical-device QA pending)</td><td>`AlarmScheduler.kt`, `MovementDismissal.kt`, `BootRescheduleReceiver.kt`; "Make your chosen wake time your alarm?"</td><td>Not featured. It is off-theme, and reliability is unverified on real phones.</td></tr>
<tr><td>"One deliberate minute": first 60-second practice in onboarding</td><td>Built (debug)</td><td>"One deliberate minute.", "Give yourself one minute.", "I'm stationary and in a suitable place."</td><td>R09, R10. Show the concept; no breath instruction in campaign copy.</td></tr>
<tr><td>Sadhana: 33-stage Living Spine; Path Arrive → Return → See → Choose → Carry; Saturn loading surface (NASA PIA06193, credited)</td><td>Built (debug, `SADHANA_V1`); release launches **no** practice until founder content-review manifests exist (B4)</td><td>`SadhanaSpine.kt`, `SadhanaPractice.kt`, `SadhanaLoading.kt`</td><td>Mention only as "Sadhana, an initiation, in development".</td></tr>
<tr><td>Sadhana practices: Stage 1 · The base (60 s); Receive the room; The moment before the reach; Protect one chosen action; Return to one ordinary breath</td><td>Built (debug); public release gated on content review</td><td>`SadhanaCatalog` in `SadhanaPractice.kt`</td><td>"The moment before the reach" (Notice the reach. Name what you intended to do before you decide.) is the in-app twin of R01's practice. Do not promote the breath practice (brand rule).</td></tr>
<tr><td>Stage 1 self-check: "Where did your attention go?"; drift estimate; reasons incl. "Phone / notification urge"; "Reflection only, not a measurement."</td><td>Built (debug)</td><td>`SadhanaStage1.kt`; decision 0018</td><td>R10: show as the honest question "What pulled it away?"</td></tr>
<tr><td>Sadhana focus lock during practice: all-apps block; Do Not Disturb if granted; opt-out; "Focus lock is off: the permissions it needs are not granted. Nothing is blocked."</td><td>Built (debug)</td><td>`SadhanaFocusLock.kt`; decision 0017</td><td>May say: "the minute is protected while you practise", with the test-build label.</td></tr>
<tr><td>Record: "Practise without the player" / record a practice done without the app; "Missed days remove nothing."; "Continuity, not a streak."</td><td>Built (debug)</td><td>`SadhanaRecordScreens.kt`, `SadhanaHomeScreens.kt`, `InitiationOrbitScreens.kt`</td><td>R07, R10. Strong proof that there are no streaks and no shame.</td></tr>
<tr><td>Accounts (Google, email) and cloud sync (Supabase)</td><td>In development (DEV-25; local outbox only; Google/email show "not available yet")</td><td>HANDOFF_NEXT; `SyncOutbox.kt`, `SupabaseSync.kt` (flagged)</td><td>Don't mention.</td></tr>
<tr><td>Hindi UI in the new onboarding/Routine/Sadhana</td><td>In development / not verified (no `values-hi` resources; only the legacy onboarding has a हिंदी toggle)</td><td>`res/` has only `values`; `OnboardingFlow.kt:87`</td><td>No Hindi product claims. Hindi editorial is fine with fluent review.</td></tr>
<tr><td>Play Store release / public download</td><td>Planned (DEV-17; no release)</td><td>marketing pack README</td><td>Never say "download" or "available".</td></tr>
<tr><td>Waitlist on shanivar.com (Netlify Forms, UTM copied into hidden `source` field)</td><td>Live as of 17 Oct (HTTP 200, "early-access" form), but **founder says DNS is pending**</td><td>curl check; `website/README.md`</td><td>Use only after founder confirmation. Until then: profile + DM keyword.</td></tr>
<tr><td>Breathing (VBT) pillar</td><td>Built in older code, outside the current shell</td><td>`Breathing*.kt`, decision 0011</td><td>Excluded (brand rule: no breath pacing).</td></tr>
<tr><td>QR/camera alarm missions, multiple alarms, sleep tracking</td><td>Planned / deferred (some code exists)</td><td>backlog</td><td>Don't mention.</td></tr>
<tr><td>AI reflection / "scroller", astrology and birth charts, payments, WhatsApp/referrals, family dashboards</td><td>Planned (later)</td><td>backlog; live site lists "Reflection" as planned</td><td>Don't mention. Astrology is never implied as a feature.</td></tr>
<tr><td>XP, levels, ranks, streaks</td><td>Hold (decisions 0009/0013)</td><td>backlog</td><td>Never.</td></tr>
</table>
