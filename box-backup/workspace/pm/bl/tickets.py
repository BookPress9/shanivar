import json
EK=dict(A="DEV-56",B="DEV-57",C="DEV-58",D="DEV-59",E="DEV-60",F="DEV-61",G="DEV-62")
SAFE="No breath pacing; no medical, therapeutic, energy or performance claims; teachers stay internal; self-reports are never scores or health data; no unnecessary sensitive data in analytics/logs; the protected-app list stays on the device unless the founder decides otherwise."
T=[]
def t(id,ep,title,owner,prio,problem,hyp,inn,out,req,ac,dep,an,sec,tn,te,tf,tr,dod_extra=""):
  T.append(dict(id=id,ep=ep,title=title,owner=owner,prio=prio,body=f"""Status: awaiting founder confirmation — do not build yet.

**Title:** {title}
**Epic:** Epic {ep} ({EK[ep]})
**User problem:** {problem}
**Hypothesis:** {hyp}
**Scope — in:** {inn}
**Scope — out:** {out}
**Requirements:** {req}
**Acceptance criteria:** {ac}
**Dependencies:** {dep}
**Analytics:** {an}
**Security/privacy:** {sec} Safety rules: {SAFE}
**Test cases:**
- Normal: {tn}
- Edge: {te}
- Failure: {tf}
- Recovery: {tr}
**Owner:** {owner}
**Priority:** {prio}
**Definition of done:** Code/doc reviewed; tests above pass and are linked; docs updated; telemetry verified where applicable; release gates for Epic {ep} met. {dod_extra}
**Evidence:** Strategy brief 2026-10-17 §9 Epic {ep}; links to design, implementation, test results and decisions to be added here.
"""))
U="UNVALIDATED — "
t("A1","A","Publish the canonical Notion page \"Shanivar Product Thesis — Protection, Awareness & Flow\" with decision log","Founder approves; PM maintains (no team owner in §10 list)","High",
 "Teams act on different readings of the thesis because there is no single source of truth.",
 "Not a product hypothesis — a process requirement from brief §2A.",
 "All 11 sections listed in brief §2A; links to every DEV epic/ticket; decision log with owner/status/last-updated.","Rewriting earlier decisions without logging what changed and why; new research.",
 "Page exists, links to roadmap and DEV epics DEV-56..62; earlier decisions preserved and referenced.","Every §2A section present; each epic linked; decision log has at least the 2026-10-17 entry; founder marks it approved.",
 "None.","None.","No user data on the page.","Page reviewed section by section against §2A.","A decision conflicts with an older one — both recorded with reason.","Link to a deleted ticket — flagged in review.","Page restored from Notion history if overwritten.")
t("A2","A","Map existing Shanivar features against the protect → act → reflect journey","Shanivar Frontend","High",
 "Nobody can say which existing screens (Focus/Protection, Routine, Initiation, Sadhana, Orbit) serve which journey step, so gaps and duplicates are invisible.",
 U+"most journey steps already exist but are not connected; tested by this map plus Epic F E2E walk-through.",
 "Inventory of current screens/flows in android-app (FocusGuardService, BlockOverlayActivity, RoutineProtectionScreen, InitiationOrbitScreens, LivingOrbit, Sadhana S1–S7) mapped to journey steps; user-flow diagram.","Redesign; new features.",
 "Table: feature → journey step → status (exists/partial/missing) → ticket.","Every journey step has at least one row; every missing step links a ticket; diagram attached.",
 "DEV-11, DEV-26, DEV-31/32.","None.","None.","Walk the map on a device build.","Feature serving two steps.","Screen in code but unreachable in nav — marked.","Map updated when a ticket closes.")
t("A3","A","Define the event dictionary, analytics contract and WIAC (North Star) computation","Shanivar Backend","Highest",
 "There is no analytics infrastructure (SessionReview.kt is local-only), so no outcome in the brief can be measured.",
 U+"WIAC (Weekly Intentional Actions Completed, deduplicated across features) reflects intentional behaviour; tested by pairing with self-reported intentionality (Epic G) before treating it as truth.",
 "Event names, properties, timestamps (UTC + local tz), dedup key, consent rule; WIAC and activation/time-to-first-value/routine completion/protection-window effectiveness/recovery definitions; routine events (created, step completed, step missed, recovered).","Choosing a vendor; dashboards (G1); building pipelines.",
 "One schema doc; each event has owner, trigger, properties, PII class; no package names of protected apps, no free text, no self-report values as scores.","Doc reviewed by FE, BE, Tester, CS; every metric in brief §6 traceable to events; privacy review signed.",
 "A2; DEV-53/54 (orbit_events, observations).","This ticket defines them.","Data minimisation review; protected-app list never sent; self-reports excluded or stored only as user-owned records, never scores.","Each metric computed from a sample event log.","Same action logged by two features — counted once.","Missing timezone — event rejected/flagged.","Schema versioning: old events still parse.")
t("A4","A","Set baselines, experiment design and an assumption register","Customer Success","Medium",
 "Without baselines and labelled assumptions, the team cannot tell whether a change helped.",
 U+"each strategic assumption (e.g. protection → action → awareness) can be tested with small experiments.",
 "Assumption register (assumption, evidence, test, owner, status); baseline metrics plan for activation, W1/W4; experiment template.","Running experiments; paid acquisition.",
 "Register lists every UNVALIDATED hypothesis in DEV tickets.","Register exists and is linked from Notion; each hypothesis has a test method.",
 "A3.","Uses A3 events.","No individual-level data in the register.","Add one assumption end-to-end.","Assumption with no feasible test — marked.","Baseline not computable — documented limit.","Register reviewed monthly.")
t("B1","B","Audit protection capabilities and Android platform constraints","Shanivar Frontend","Highest",
 "Users may be promised protection the platform cannot guarantee.",
 "Not a hypothesis — fact-finding.",
 "Document actual mechanism: UsageStatsManager polling in FocusGuardService + SYSTEM_ALERT_WINDOW BlockOverlayActivity (no AccessibilityService), specialUse foreground service, exact alarms, boot reschedule; known brief app flash; OEM battery killers; Android 14/15 FGS rules; permission revocation.","Switching to AccessibilityService (needs founder decision).",
 "Constraint table: capability, API, permission, failure mode, user-visible copy.","Table reviewed by Tester; every limit has user copy and a test in B5; feeds Marketing claim checklist (G4).",
 "DEV-7, DEV-10, DEV-21.","None.","Usage access data used only on device; nothing about foreground apps leaves the device.","Read code paths and verify on one device.","OEM with aggressive battery saver.","Usage access revoked mid-session.","Doc updated after DEV-7 results.")
t("B2","B","Specify protection-window behaviour and user controls","Shanivar Frontend","High",
 "Users do not always understand why an intervention appeared or what their choices are.",
 U+"a clear reason plus 2–3 explicit choices reduces frustration and bypassing; tested with usability sessions and B3 choice data.",
 "Spec for start/end, overnight windows, essential-access stop (DEV-10), no-snooze rule, copy explaining why the screen appeared, choices (return to my day / short practice / one-minute pause), permission-denied and unavailable states.","New blocking modes; web blocking.",
 "Spec + wireframe; copy free of claims.","Spec covers all states; Tester converts to test cases; founder approves copy.",
 "B1, DEV-10, DEV-12.","Defines states for B3.","Spec keeps the protected-app list local.","Walk each state on wireframe.","Window crossing midnight / DST.","Permission missing at window start.","Window resumes correctly after reboot (DEV-21).")
t("B3","B","Record the intervention choice on the block overlay (on-device)","Shanivar Frontend","High",
 "We do not know whether interventions lead to intentional choices; BlockOverlayActivity offers Return / Practice / Pause but records nothing.",
 U+"most interventions end in 'return to my day' or a practice rather than a pause; tested by measuring choice distribution.",
 "Local record per intervention: time, choice, window id — no app package name in any synced or analytics payload.","Remote sync of the protected-app list; scoring.",
 "One record per shown overlay, idempotent on rerender; event per A3.","Choice stored exactly once per overlay; analytics event fires once; no package name leaves device.",
 "A3, B2.","intervention_shown, intervention_choice{choice}.","Package name stays local.","Tap each choice.","Overlay re-shown quickly for same app.","Storage write fails — overlay still works, error logged.","Process death mid-choice — no duplicate on restart.")
t("B4","B","Detect and surface protection failures (service killed, permission revoked)","Shanivar Frontend","Highest",
 "If protection silently stops, users believe they are protected when they are not.",
 "Not a hypothesis — reliability requirement.",
 "Heartbeat from FocusGuardService; detect usage-access/overlay permission loss (LocalProtectionController.canEnforce); show 'protection is off' state; crash/ANR to DEV-14 reporting.","Server-side monitoring of app usage.",
 "Never show 'protected' when not enforcing; failure visible within one poll interval.","Killing the service shows off/unknown state; revoking permission shows the fix path; failure appears in crash reporting.",
 "DEV-14, B1.","protection_failure{reason}.","No app names in logs.","Normal window runs end to end.","Doze / battery saver.","Service force-stopped.","User re-grants permission and protection resumes.")
t("B5","B","QA suite: protection permissions, sessions, reboot and recovery on real devices","Shanivar Tester","Highest",
 "Protection has never been verified on physical devices (DEV-7).",
 "Not a hypothesis.",
 "Test cases for permission grant/deny/revoke, window start/end, overnight, reboot catch-up, essential stop, overlay choices, failure states; at least two OEMs / two Android versions.","Emulator-only sign-off.",
 "Each B-ticket AC has a reproducible test case.","Suite executed on real devices with evidence; defects logged with severity.",
 "DEV-7, B1–B4.","Verify B3 events match actions.","Test data contains no real personal app lists.","Full window on device.","Clock change during window.","Permission denied at onboarding.","Reboot mid-window resumes.")
t("C1","C","Define routine completion semantics and make completion idempotent","Shanivar Backend","Highest",
 "Repeated taps, retries or sync could duplicate completions and inflate progress.",
 "Not a hypothesis — brief §4 requirement.",
 "What counts as complete/missed/recovered; idempotency key per step-occurrence locally and in put_routine/sync (DEV-52, DEV-45).","Rewards/gamification (DEV-16 decision).",
 "Duplicate requests produce one completion; server validates; 409 on stale revision.","Integration test: 5 identical requests → 1 row; offline replay → no duplicates.",
 "DEV-52, DEV-45, DEV-38.","routine_step_completed with dedup key.","Per-user isolation via RLS.","Complete a step.","Complete same step offline twice then sync.","Server rejects — UI does not show success.","Retry after failure completes once.")
t("C2","C","First meaningful action flow: new user completes one action without configuring the whole app","Shanivar Frontend","Highest",
 "First-time users are overwhelmed by setting up protection, routine and Sadhana at once.",
 U+"choosing one goal and doing one small action in session one increases activation; tested via activation rate and time-to-first-value (A3).",
 "Initiation path (DEV-24/26) ends in one chosen action today; skip/defer for other setup; permission asks just-in-time.","Full onboarding redesign; account creation required up front.",
 "Loading, empty, error, permission-denied and offline states; accessible controls.","New user reaches first completed action in a usability test without help; failed save never shows success.",
 "DEV-24, DEV-26, C1, A3.","first_action_completed, time_to_first_value.","Guest mode; no data collected beyond the action.","Fresh install to first action.","User skips every optional step.","Offline at first launch.","App killed mid-onboarding resumes draft (DEV-24).")
t("C3","C","Missed-step recovery and non-punitive return","Shanivar Frontend","High",
 "After missing steps, users face guilt or confusing state and abandon routines.",
 U+"offering a smaller next step after a miss improves recovery after missed days; tested by recovery metric (A3).",
 "Missed state, 'start smaller today' option, no streak shaming, no endless reminder loop.","Gamified penalties.",
 "A missed action never creates a loop or unrecoverable state.","After 1, 3 and 7 missed days the app offers a clear way back; copy reviewed for guilt.",
 "C1, A3.","routine_step_missed, routine_recovered.","None beyond C1.","Miss one step, return.","Miss across timezone change.","Reminder fails to deliver.","User returns after 30 days.")
t("C4","C","Verify alarm and notification behaviour on supported real devices","Shanivar Tester","Highest",
 "A failed alarm leaves no trace and breaks the routine (AlarmScheduler/AlarmFireReceiver/AlarmForegroundService, setAlarmClock, exact-alarm permission).",
 "Not a hypothesis.",
 "Device matrix (min 2 OEMs, Android 12–15): exact alarm permission granted/denied, Doze, reboot (BootRescheduleReceiver), full-screen intent, notification permission denied, DND, movement-first dismissal.","Emulator-only sign-off.",
 "Every alarm AC has a test; results with evidence.","All cases pass or have documented defects; no unresolved critical.",
 "DEV-14, DEV-11.","Compare alarm_observations (DEV-53) with actual.","Camera frames used for dismissal not stored/sent.","Daily alarm fires.","DST change.","Exact-alarm permission revoked.","Reboot reschedules.")
t("D1","D","Map each initial practice to a user need, expected action and clarity check","Customer Success","Medium",
 "Practices may not connect to what the user does next.",
 U+"a practice tied to a specific next action is completed and followed by that action more often; tested by D-epic analytics and interviews.",
 "Table for DEV-36 Stage 1 and S1–S7 practices: need, expected action, safety check (no breath pacing, no claims, teachers internal), clarity/autonomy review with 5 users.","New content.",
 "Every shipped practice has a row and passed safety review.","Table complete; content issues ticketed (e.g. DEV-39).",
 "DEV-36, DEV-39, DEV-51.","practice_completed, next_action_taken.","Reflections are user-owned, never scores.","Review one practice.","Practice fitting no need — flagged.","Claim found in copy — blocked.","Content updated and re-reviewed.")
t("D2","D","Optional one-tap awareness prompt after a distraction or completed action","Shanivar Frontend","Low",
 "Users act without noticing the urge or mood behind it.",
 U+"a short optional prompt increases self-reported awareness without adding friction; tested by skip rate and interviews.",
 "Optional, skippable prompt after intervention or completion; uses existing check-in (DEV-36).","Mandatory prompts; mood scoring.",
 "Always skippable; never shown more than agreed frequency.","Skip works in one tap; responses stored locally as self-report, never a score.",
 "B3, DEV-36.","awareness_prompt_shown/skipped/answered (no answer content).","Answer content not sent to analytics.","Answer prompt.","Prompt triggered twice quickly.","Save fails.","Prompt state survives restart.")
t("D3","D","Connect relevant Mano Vijnana content to practical exercises","Shanivar Frontend","Low",
 "Study content feels abstract and disconnected from daily action.",
 U+"linking a concept to one exercise increases practice return rate.",
 "Links from Deep study (DEV-33) to an existing exercise.","New teachings; teacher attribution in UI.",
 "Each link reviewed for claims and teacher-internal rule.","Links work; content review manifest (DEV-34) approved.",
 "DEV-33, DEV-34, DEV-51.","study_to_practice_opened.","None.","Open link.","Content missing.","Offline.","Content restored on reconnect.")
t("E1","E","Cross-user access test suite for every Supabase table, RPC and Edge Function","Shanivar Backend","Highest",
 "A user could read or modify another user's records.",
 "Not a hypothesis.",
 "pgTAP/integration tests as user A against user B data for all sync tables, put_routine, submit_feedback, delete-account; anon access denied.","Pen test by a third party.",
 "Tests run in CI (DEV-15/DEV-43).","Every endpoint has a passing isolation test; any failure is a release blocker.",
 "DEV-42, DEV-43, DEV-15.","None.","Core of this ticket.","User reads own rows.","Guest → account migration.","Forged user_id in payload rejected.","Policy fix + regression test.")
t("E2","E","Sensitive logging, retention, deletion and backup review","Shanivar Backend","Highest",
 "Credentials have leaked before (DEV-8); logs or analytics could expose personal data.",
 "Not a hypothesis.",
 "Audit Android logs, Edge Function logs, crash reports (DEV-14), analytics (A3); retention periods; deletion path (DEV-46); backup/restore (DEV-41).","Legal policy text.",
 "No secrets/PII/protected-app names in logs or responses; documented retention and restore.","Audit report; restore drill done on staging.",
 "DEV-8, DEV-14, DEV-41, DEV-46.","None.","Core of this ticket.","Grep logs after a full session.","Crash with user text in state.","Backup restore fails — documented.","Restore drill succeeds.")
t("E3","E","Release, rollback and incident-response runbook","Shanivar Backend","High",
 "Without a runbook, a bad release or migration cannot be reversed quickly.",
 "Not a hypothesis.",
 "Android release steps (DEV-17 signing), staged rollout, Supabase migration rollback (DEV-9/43), incident severity and contacts.","Automation beyond DEV-43.",
 "Runbook in repo docs.","Rollback rehearsed on staging once; linked to G6.",
 "DEV-17, DEV-43, DEV-41.","None.","Secrets referenced, never written in runbook.","Dry-run release.","Migration not reversible — documented forward-fix.","Rollback fails.","Incident closed with postmortem.")
t("F1","F","Traceable test plan, AC-to-test matrix and defect register for all epics","Shanivar Tester","Highest",
 "Work is called done when it appears on screen, without independent verification.",
 "Not a hypothesis.",
 "Matrix: ticket → AC → test case → result; defect register (severity, evidence, expected, actual); release-readiness report template.","Writing tests for unconfirmed tickets.",
 "Every confirmed ticket has linked test cases.","Matrix covers epics A–G; template used for first RC.",
 "All epics.","None.","Test evidence free of personal data.","Trace one ticket fully.","AC not testable — returned to owner.","Test env down.","Retest after fix recorded.")
t("F2","F","Verify analytics events match actual user actions","Shanivar Tester","High",
 "Analytics may be duplicated by rerenders or miss actions, corrupting WIAC.",
 "Not a hypothesis.",
 "Scripted sessions compared with emitted events per A3.","Dashboard design.",
 "No duplicates, no missing events, no forbidden properties.","Report per event: expected vs actual.",
 "A3, B3, C1.","Core of ticket.","Check that no sensitive properties appear.","Scripted journey.","Rotation/rerender.","Offline then sync.","Events flushed once after reconnect.")
t("F3","F","End-to-end protect → act → reflect journey and regression suite","Shanivar Tester","High",
 "The connected journey has never been tested as a whole.",
 "Not a hypothesis.",
 "E2E script from first launch through intervention, action and optional reflection; regression run before every RC.","Load testing.",
 "Suite runnable on device and documented.","Pass on two devices before release recommendation.",
 "C2, B3, D2, DEV-55.","Verify journey events.","Test accounts only.","Full journey.","User skips reflection.","API failure mid-journey.","Resume after app kill.")
t("G1","G","Activation and retention dashboard with goal segments","Customer Success","Medium",
 "We cannot see who activates or returns, by primary goal.",
 U+"segments (distraction, routine consistency, meditation, self-inquiry) behave differently.",
 "Activation milestone per segment; W1/W4; WIAC; recovery; reliability.","Paid BI tools without approval.",
 "Dashboard reads only A3 events.","Baseline numbers visible; limits stated.",
 "A3, F2.","Consumes A3.","Aggregates only.","Load dashboard.","Small segment — suppressed.","Missing data.","Backfill after outage.")
t("G2","G","Feedback and churn-reason taxonomy, interview guide and monthly product review","Customer Success","Medium",
 "We do not know why users struggle, leave or return.",
 U+"most abandonment comes from friction or technical failure; tested by interviews and in-app feedback (DEV-49).",
 "Taxonomy (technical failure, friction, low relevance, unrealistic routines, unclear value, other); interview guide (active/inactive/returning); monthly cross-functional review.","Incentivised research spend.",
 "Feedback tagged with taxonomy; findings linked to tickets.","First 5 interviews done; first monthly report published.",
 "DEV-49.","feedback_submitted (category only).","Consent for interviews; no health data.","Tag a feedback item.","Item fits no category.","Feedback table unreachable.","Re-tag after taxonomy change.")
t("G3","G","Lapsed-user recovery journey offering smaller commitments","Customer Success","Low",
 "Lapsed users feel guilt and do not return.",
 U+"a gentle, smaller-commitment message improves return; tested by recovery rate.",
 "Message/flow spec, no guilt or pressure.","Aggressive re-engagement.",
 "Copy reviewed; frequency cap.","Spec approved; links to C3.",
 "C3, G1.","lapsed_message_shown/returned.","Opt-out respected.","Lapsed user returns.","User opted out.","Message fails.","Second attempt capped.")
t("G4","G","Positioning doc and feature-claim verification checklist","Sana (Marketing)","Medium",
 "Messaging could promise features or outcomes the app does not deliver.",
 U+"segment-specific messaging around 'Protect. Become Aware. Enter Flow.' is understood; tested before larger campaigns.",
 "Positioning, segment messages, claim checklist vs B1 constraints and shipped features.","Paid campaigns; competitor-gap claims.",
 "No medical/therapeutic/energy/performance claims; every claim maps to a shipped feature.","Checklist applied to site, onboarding and store listing.",
 "B1, A2.","Measure activation of acquired users, not clicks.","No tracking pixels without consent.","Check one page.","Feature behind flag.","Claim fails check — removed.","Re-check on each release.")
json.dump([{k:v for k,v in x.items()} for x in T],open("tickets.json","w"))
for x in T: open(f"t_{x['id']}.md","w").write(x['body'])
print(len(T))
