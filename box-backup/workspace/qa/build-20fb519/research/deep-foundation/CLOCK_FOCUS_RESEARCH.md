# Clock / Focus: a boundary that remains the person's choice

Status: **PROPOSED research and redesign**, 2026-09-29. No accepted enforcement decision is replaced by this file. Owners: High-Scale Product Pattern Researcher, with engineering and accessibility review required. Companion: [product patterns](COMPETITOR_PRODUCT_RESEARCH.md), [source ledger](SOURCE_LEDGER_PRODUCTS.md).

## Current state, inspected in code

Read [enforcement stance](../../android-app/ENFORCEMENT_STANCE_2026-09-12.md), [decision 0010](../../docs/decisions/0010-focus-schedule-when-then-commitment.md), `V1Home.kt`, `FocusScheduleCard.kt`, `FocusScheduleReceiver.kt` and `FocusWindowRecurrence.kt` on 2026-09-29. This is a static inspection, not runtime/device verification.

The accepted baseline is strong friction around apps the person deliberately selected. Ordinary early ending happens inside Shanivar, not as a “five more minutes” button over Instagram. The device remains theirs: force-stop, revoked access, uninstall, browser alternatives and another phone are known bypasses. **No Device Admin or accessibility-service enforcement.** Reading policy competitors follow does not override that product decision. Alarms and focus are independent; a practice cannot release protection or keep a completed alarm sounding.

Current source contains the schedule UI (`FocusScheduleCard.kt`), so older documents calling that screen unbuilt are stale. It offers one schedule with all-day / selected-days / custom-time presets, selected apps and optional verbatim when/then reminders. Six known distracting packages constrain selection. The card warns that whole apps, including messages, are paused. This is an important limitation when a social app also carries essential work or family communication.

Reliability gaps relevant to the proposed redesign:

| Evidence | Consequence | Required gate before broader promise |
|---|---|---|
| `FocusScheduleReceiver` uses nullable in-process `ShanivarSession.api`; without it, it advances the boundary without starting/ending | A saved schedule is not proof of active protection after process loss | Persist/recover session authority or explicitly redesign local rule authority; test cold process, reboot, offline and expired credentials |
| `FocusWindowRecurrence` rejects start ≥ end | Overnight sleep-preparation windows unavailable | Explicit overnight semantics and tests; do not silently swap times |
| All-day preset is 00:00–23:59 | Not truly continuous; also depends on successful next boundary | Do not call this persistent protection; specify intervals without a one-minute daily gap |
| Receiver calculates remaining minutes using next boundary at delivery | A delayed START after the window has closed can use an inappropriate future boundary | Evaluate whether the window is active at actual delivery; no stale catch-up start |
| One schedule/lifecycle; END calls `releaseFocus()` | Extending to overlapping/manual rules risks releasing protection belonging to another rule | Ownership and effective-rule union, or explicit non-overlap policy, before multiple schedules |
| Save reports a scheduled boundary, not verified future enforcement | An “armed” success label can overstate reliability | Separate Scheduled, Active, Needs attention and Off; verify permissions and actual local guard state |

These are candidate engineering concerns from the inspected code, not claims that a particular physical-device failure was reproduced. The 0010 known limitation was accepted for V1; a stronger claim introduces a stricter release gate.

The subsequent [technical map](../../SHANIVAR_TECHNICAL_MAP.md) independently reviews these six source paths and classifies them as FOCUS-1–6. Its FOCUS-1 analysis makes the delayed-START risk concrete: treating a future START as an end can produce the 24-hour cap. The [engineering bridge](../../GPT_TECH_BRIDGE.md) and [integration plan](../../CLAUDE_TECHNICAL_INTEGRATION_V1.md) put reliability and physical-device spikes before new modes. These are colleague engineering findings/proposals, not founder acceptance or device evidence. This research did not rerun their reported test suites.

## What competitors teach, and what they do not establish

| Product / evidence | Useful mechanism | Constraint or rejected transfer |
|---|---|---|
| [Opal schedules](https://help.opalapp.com/article/how-to-use-schedules) | Explicit time/day/app selection and commitment | Documentation is for iOS; do not assume Android parity or copy current emergency rules blindly |
| [Opal Always Allowed](https://help.opalapp.com/article/how-to-designate-apps-as-always-allowed) | Make essential applications a first-class concern | Its help says an actively blocked app cannot simply be added to that list. Shanivar needs a recoverable mistaken-selection path |
| [Opal emergency help](https://help.opalapp.com/article/how-to-use-emergency-pass) | Recovery behavior is documented | Weekly quota can be exhausted; not an acceptable foundation for Shanivar emergency access |
| [Freedom Locked Mode](https://support.freedom.to/en/articles/1802927-locked-mode) | Try schedules before adding restriction; distinguish platform behavior | App-level restrictions do not equal inability to revoke OS permissions. No promise of unbreakable protection |
| [ScreenZen](https://screenzen.co/) | Schedules and limits reduce repeated manual decisions | Additional configuration may increase maintenance; accessibility-based enforcement cannot be imported |
| [Brick emergency access](https://getbrick.com/pages/emergency-unbricks) | Physical separation makes reversal deliberate | A finite emergency allowance with support reset is not robust safety. No hardware or support-service prerequisite for emergency release |
| [one sec](https://one-sec.app/) | Intervene at the opening impulse | A momentary pause is different from a protected time window; optional grounding must not become a bypass or forced breath exercise |
| [Forest](https://www.forestapp.cc/) | Visible time commitment | No withering penalty, collective punishment or moral meaning attached to a necessary interruption |

None of these official documents explains causally why users keep blocking enabled after two days. They document features and company rationale. We need dropout interviews, observed setup/recovery and longitudinal opt-in use, including users who stop using the tool.

## Proposed three-mode model

The product name can remain **Focus**. “Clock” is an organizing idea, not a demand for a new tab or a second time engine. Home should show one relevant next action and status. Users should not need to distinguish an alarm engine, rule evaluator and session lifecycle.

### Scheduled Focus: “Protect a regular time”

Flow: choose a time window → repeat days → select distracting apps → review essential access and exact consequences → confirm. Optional when/then writing appears only after the core schedule is understandable. Existing text stays verbatim, never AI-generated. A summary says, for example, “Instagram and YouTube paused Mon–Fri, 07:00–09:00. Calls remain available. Edit or end from Shanivar.” Show upcoming start, timezone and next end.

Each screen has one meaningful decision group; selecting days or apps is still several choices, and must not be marketed as “one tap.” Returning users can reuse the saved window. Before permissions, explain why each is needed; after returning from Settings, recheck rather than infer success.

**Overnight proposal:** a start-day convention: Monday 22:00–Tuesday 07:00 belongs to Monday's rule. Preview both dates; day selection applies to start days. Start is inclusive, end exclusive. A timezone change uses the current local schedule by default and explains the changed next boundary. Ambiguous/repeated DST times require explicit, tested recurrence policy consistent with the existing alarm engine; do not invent two active copies. Travel must never lengthen a manual session beyond its selected elapsed duration.

### Contextual Focus: “Protect this activity”

Work, Study, Morning, Sleep preparation, Practice and Family time are **user-chosen labels**, not diagnoses or automatically inferred locations. Pick the activity → duration or end time → apps from the person's saved choices → confirm actual availability. Once a person's preference exists, show a compact review with Change. No location/calendar/microphone access is necessary for the first version.

Examples: “Study for 25 minutes”; “Family time until 20:30”; “Sleep preparation for 30 minutes.” Do not prescribe a universal ideal period or infer circadian readiness. After verified alarm dismissal, an optional Focus offer can appear; completion of the waking action must already have silenced the alarm. A Steady practice is optional before/inside Focus and cannot silently start or stop protection.

### Persistent / Strong Focus: “Keep these apps unavailable”

This is a **later proposed capability**, not the existing all-day preset renamed. Start with ordinary schedules and verify essential access before presenting it. Show exact app coverage, known bypasses, local recovery and an accessible practice recovery exercise. Choose exceptions or planned access windows explicitly. Display a concise ongoing status; no repeated motivational notifications are required.

Proposed semantics: selected apps are unavailable until the person deliberately changes the rule, with explicit allowed windows and reliable local suspension. Never block Settings, phone/emergency communication, the launcher, accessibility tools or Shanivar's recovery surface. Do not make another person, payment, a server response, a physical activity or a forgotten object the only recovery authority. If we cannot verify recovery and persistent state across restarts, **do not ship this mode**. Founder acceptance is needed for its scope and departure from current one-schedule V1.

## Safety, overrides and recovery specification for review

Preserve the accepted distinction between ordinary distraction and necessary access. On a blocked-app surface, the ordinary primary action can be “Return to your activity,” with an optional short grounding practice. No casual extension button. Inside Shanivar, “End protection” shows the exact rule and consequence, asks for a deliberate confirmation, then releases locally. A reason is optional and never a condition of release. Editing future occurrences must be distinguished from ending this occurrence.

The founder's new emergency requirement is proposed as a clearly labelled **“Need essential access?”** path available from active status and the block surface. It is not the normal “just five minutes” bypass. It must expose emergency calling and a local safety release when an app was mistakenly restricted. This extends the older “no bypass affordance anywhere” wording; record and obtain founder acceptance of the exact behavior before implementation. Do not resolve the tension silently by hiding the safety path. No emergency quota, timer, puzzle, walking, breath retention, guilt copy, login or remote approval.

Essential-app selection should be conservative: emergency dialer, phone, system Settings, launcher, accessibility/assistive communication, authentication needed for access, health/medical tools and user-specified transport/care communications. A general installed-app selector requires OS-aware exclusions and physical-device checks; the current six-app list is not that implementation. Warn that Instagram/WhatsApp-like mixed-purpose services cannot be partially blocked by the current package-level mechanism. An emergency contact list does not prove their communication app remains usable.

Local safety release must stop the guard/overlay first, mark current protection suspended, cancel/reconcile pending local boundaries and only then sync remotely. Prevent the next timer callback from immediately re-locking the user. If the process dies midway, recovery state must be idempotent and recoverable after restart. The user's explicit safety suspension wins over stale network state; this requires a written conflict-resolution contract, not optimistic UI alone. Re-enable only after an explicit user choice. Never claim account-wide release if only this device is known to be released.

Accessibility: TalkBack labels/order, large text, high contrast, no color-only status, Switch Access and reachable controls. Confirmation cannot require a long hold as its only operation. No standing or motion requirement. Permission denial must leave useful non-enforcing practice available. For families, the actual participant controls the account and override; a helper gets no private activity history. Supporting minors requires a separate policy/safety assessment; this research does not establish readiness for all ages.

## Platform ceiling

Android UsageStatsManager supplies usage information subject to special access; it is not a system API that makes another consumer app impossible to open. Shanivar's polling/overlay approach has a detection interval and can be defeated by revocation or force-stop. Browser versions remain outside the current package guard. [Android API reference](https://developer.android.com/reference/android/app/usage/UsageStatsManager).

Exact scheduling requires appropriate permission/eligibility and runtime checks; permission changes affect pending work. Recheck access and actual scheduling state, test power/background behavior on physical devices, and do not treat a scheduled PendingIntent as proof that authentication and enforcement will work later. [Android alarm guidance](https://developer.android.com/develop/background-work/services/alarms).

Apple Family Controls is a separate entitlement/authorization framework, not a drop-in equivalent of Android overlays. Current official overview access was sparse in this audit; an iOS feasibility claim needs a dedicated FamilyControls/ManagedSettings/DeviceActivity review and entitlement validation. [Apple Family Controls](https://developer.apple.com/documentation/familycontrols). Freedom's own iOS documentation explicitly distinguishes app restrictions from revocable Screen Time access. No iOS architecture is approved here.

## Why adherence/disabling needs research

Candidate explanations are competing hypotheses: setup takes too much effort; protection fails; a legitimate message is blocked; schedule no longer matches life; repeated prompts irritate; the rule helps but another device replaces scrolling; or the person has learned the desired habit and needs the app less. “Disabled” is therefore not automatically failure, and “still installed” is not success.

Proposed study: recruit 18–24 consenting adults across self-use, parent participants, shift workers and people using assistive technology; record recruitment limits rather than claim representativeness. Start with 3 days of optional baseline diary, then 14 days of voluntary ordinary Focus. Interview at setup, day 3 and day 14; offer a withdrawal interview without pressure. No screen recording of private app contents, no actual emergency induction. Fixed compensation must not depend on maintaining restrictions. Strong mode stays out until ordinary recovery is verified.

Measure intended activity, desired window, whether protection actually operated, regretted blocks, voluntary adjustments, assistance requests, reasons for disabling and whether the person did what they meant to do. Separate telemetry from self-report. Screen-time reduction alone cannot establish improved attention or health. No sensor/usage access should be added solely to harvest richer research data.

## Source → principle → experiment → decision

All thresholds are proposed formative gates, not research results.

| ID / confidence and source | Principle / product implication | Experiment | Success and failure | Decision |
|---|---|---|---|---|
| CF01 D-high Opal schedules; I transfer | Review a comprehensible window | 12 adults set weekday 07–09 and explain it | ≥10 complete unaided; everyone identifies which apps/messages unavailable; any hidden essential restriction fails | Simplify setup before stronger modes |
| CF02 D-high local receiver/math | Reliability before persistence | Unit + physical-device matrix for reboot, cold process, late delivery, expiry, offline, DST, timezone, revoked permissions | No false Active state; no missed local release; no start outside active window | Block persistent release until resolved |
| CF03 D-high Brick/Opal emergency docs; I rejection | Safety cannot be a consumable reward | Offline simulated wrong-app lock, exhausted ordinary overrides, process death during release | Every participant can recover; zero quota/server dependence | Any trap stops pilot |
| CF04 D-high one sec; I transfer | Offer an embodied alternative without coercion | Compare return-only versus optional external-grounding block screen | Less regretted reopening without added frustration or forced practice | Keep optional only if useful |
| CF05 D-high 0010; I adherence | The participant's words may support intention | Optional when/then versus blank, qualitative 14-day pilot | Participants recall it as useful and voluntary; fail if ignored or burdensome | Remove unused fields rather than generate motivational copy |
| CF06 D-high Forest/ScreenZen; I transfer | Useful protection rather than game retention | Review voluntary repeats alongside disabling reasons | Chosen activity improves by self-report without more regrets; necessary disabling not scored as failure | Iterate fit, not punishment |
| CF07 D-high platform docs; I UX | State must describe reality | Remove each permission during/after scheduling | UI becomes Needs attention; next-step repair understandable; no false guarantees | Block misleading copy |

## Release and governance gates

Founder review: three-mode scope, emergency exception to block-surface stance, persistent-rule semantics, essential access and any telemetry. Engineering review: local authority, stale boundary suppression, session ownership, overlap, offline release and server reconciliation. Accessibility review: recovery by diverse input methods. Content review: no shaming, astrology enforcement or claims of treating addiction. Physical-device evidence: normal block, end, denied/revoked access, reboot, process death, offline, low battery/background conditions, midnight/DST/timezone, incoming call and assistive use. Do not run emulators on this machine. These gates are **NOT RUN** by this research assignment.

Marketing/UGC may test genuine choice stories after users consent: protect mornings, complete a chosen task, adjust a rule after learning. Use participant-authored accounts, redacted optional screenshots and permission per reuse. Do not fabricate before/after numbers, imply current persistent protection, or post private app/package histories. A neutral prompt is “What did you make room for?” not “How much discipline did you prove?”
