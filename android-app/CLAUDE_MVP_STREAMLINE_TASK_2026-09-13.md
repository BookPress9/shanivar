# Claude task: streamline the initial Shanivar build

Status: founder-revised v1 ready for Claude implementation; not executed by this handoff. Alarms and app locks/focus modes are both mandatory; the previous focus-only PM recommendation is superseded. Prepared at the founder's request. The latest audience correction controls: concerned adult children introducing parents, plus younger self-directed users. Parents may choose a Lord Shani connection. Do not implement the superseded parent-only brief.

## Result

Produce a pitchable debug APK with independent Alarm and Focus tools and one shared awareness loop: **plan → start protected time → notice distraction → optional review → exit**. Use [initial MVP scope](INITIAL_MVP_SCOPE_2026-09-13.md), [tone guide](../docs/strategy/shanivar-tone-and-values.md) and [staged backlog](../docs/strategy/shanivar-launch-backlog.md). Read the [marketing team's v1 handoff](../marketing/shanivar/v1-product-direction-handoff.md) for original interface copy and audience hypotheses, and the [Alarmy/one sec reference](../research/shanivar-v1-alarm-focus-patterns.md) for bounded interaction patterns. Reuse Shanivar code and create original screens/copy; competitor feature descriptions are not implementation contracts. These control launch visibility; existing safety and API invariants remain binding.

The founder requests this task for Claude to lead. It includes Compose and minimal supporting backend changes needed for this scoped implementation, overriding the earlier blanket backend-only ownership for this task. Announce touched shared files and check current working changes before editing. Do not assume another agent will finish a missing surface. Do not commit, push, deploy or contact testers as part of this task.

## Source starting points

- [Confirmed v1 interactive wireframes](wireframes/saturn-dark/index.html) and [design notes](wireframes/saturn-dark/V1_DESIGN_NOTES.md): Alarm/Focus home, helper/stance, guest pause, setup, active and recovery states. Use these over the previous broad-product design, which is preserved in [the backup](wireframes/saturn-dark-backup-20260913-221107/index.html). Prototype state, account/permissions and device home are simulations, not native implementation evidence. Hindi copy and physical-device gates remain open.

- `app/src/main/java/com/shanivar/nativeapp/OnboardingFlow.kt`: currently LANGUAGE/ACCOUNT/BIRTH/CAPABILITIES/ARRIVAL.
- `ShanivarApp.kt` in the same directory: four-tab shell, FocusScreen and mixed older copy. Existing focus starts immediately through the gateway; do not advertise future scheduling as already available.
- `DisciplineEngine.kt` and its gateway/contracts: reuse status/start/release; inspect exact implementation before changing contracts.
- `BlockOverlayActivity.kt`: existing block surface and reflection redirect; verify filename and source before editing.
- `ShanivarApi.kt`, account state and routine persistence: preserve identity, auth and migration behavior.
- `ReflectionNavigation.kt` and `ReflectionScreen.kt`: deferred feed entry points. `AlarmScheduler.kt`, `AlarmActivity.kt`, `MovementDismissalActivity.kt`, `AlarmRinger.kt`, `AlarmForegroundService.kt`, `BootRescheduleReceiver.kt`, and alarm API/preferences: mandatory v1 alarm implementation and verification.
- Root `CLAUDE.md`, `ENFORCEMENT_STANCE_2026-09-12.md`, discipline/reflection native contracts, and existing tests.

## Work packages in dependency order

### 1. Establish reversible launch routing

Use one explicit build/configuration switch for the streamlined build. Inventory visible deferred routes and active persisted services. Keep tested code and data. New installs must not start deferred services or request their permissions. On upgrade, allow deliberate completion/release/cancellation of an existing focus/alarm before changing management; never orphan an active ringing alarm or lock. Preserve stop ordering, hard caps and accessible escape invariants.

### 2. Simplify onboarding

Welcome with self-use and a secondary helper guide, then a participant-owned plan. Use existing auth only when starting protected time needs it; offer the local pause without sign-in. No child account pairing. No birth input, chart intake, consciousness scoring or general feature tour.

Retain friction-reduction recommendations from the otherwise superseded [PM review](../ai_org/product_manager_mvp_review_2026-09-13.md): expose an optional pre-account “Try a one-minute pause” using the same local exercise, with “This pause does not block other apps.” Never count preview use as protected activation. Do not force the preview before starting focus. Include editable reading, family time, one task and quiet-time presets. End the static helper guide with an explicit handback to the parent's own choices.

Core choices: language, intended activity, editable focus activity preset, supported installed app selection and duration, or alarm time/repeat/action when that tool is chosen. Spiritual presentation is optional and changeable; do not infer it from age or the helper path. Default English/plain; provide Hindi only when all visible strings and failures in the core have passed review. Put role-help and presentation choices in secondary surfaces rather than adding mandatory steps.

Make the optional Shani explanation discoverable in the helper guide and presentation control; settings-only discoverability is insufficient to test the founder's invitation hypothesis. Complete Hindi is required before Hindi-speaking parent recruitment. Confirm whole-app blocking in plan copy; never imply feed-only blocking. Do not build OTP, referral infrastructure or a new local protection engine in this task; record observed friction for the next decision.

Resolve auth field requirements honestly. Do not invent phone numbers, birth inputs or account identities to bypass the server. If unnecessary fields are required, make the smallest compatible backend change with validation and tests. Persist the last plan for the correct account; local guest preferences must not leak into another account.

### 3. One home, existing immediate focus engine

Replace four top-level tabs with one home containing Start focus and Set alarm. Show intention, selected targets, duration and actual focus status plus next alarm and edit/on-off controls. Prioritize active/ringing states while keeping both tools manageable. Secondary account/settings and deliberate early-end control remain available. Start-now sessions use existing `status/startFocus/releaseFocus`; editable Study/work, Reading/quiet and Family time presets use this same engine and its package-list contract. Default to one target, allow a small selected set, and exclude system/essential communication defaults. No automatic recurring focus windows for v1.

States must include ready, checking, missing permission, starting, active, ending, completed and failed/unavailable. Busy states disable duplicate actions. A successful save is not a successful start. “Active” requires engine confirmation. Reconcile after process restart, expiry, revocation and reconnect; do not start another session automatically after uncertain network results.

Request only permissions needed by the selected tool at point of use. Inspect focus notification/foreground-service needs independently of alarm requirements. Alarm-specific exact-alarm and movement requests must not appear in focus-only setup. Alarm setup requests only permissions its chosen method requires; no camera requirement. Keep existing no-Device-Admin/no-Accessibility-enforcement stance.

### 4. Alarms: mandatory v1 work package

Implement one editable alarm with time, selected repeat days, enable/disable, next occurrence, intention, sound preview and cancel. Reuse the current local scheduler and backend alarm contract; verify existing behavior before changes. The inspected scheduler stores one pending occurrence, so repeat-day UI alone is not recurrence: persist the rule, schedule the next valid occurrence after completion, and reconcile reboot, device time/timezone changes, edits and cancellation without duplicate or stale fires. Alarm recurrence is required; focus recurrence remains deferred.

Use the founder's movement cue and step target with a non-walking alternative. No camera, QR or larger mission catalogue for new users. Sensor failure exposes the alternative immediately. It cannot require standing or walking. Preserve bounded escalation, synchronous silence before navigation/network/analytics, offline dismissal, timeout-bounded wake locks, idempotent completion and independent ring hard cap. Do not infer destination arrival or full waking from step counts.

Inspect notification, exact-alarm, full-screen and foreground-service readiness against the supported Android versions. Show actual native scheduling and separately reconcile backend sync failures. A saved alarm is not a confirmed alarm. Expose actionable readiness failures; never fabricate confirmation.

Alarm-only users do not need focus permissions; focus-only users do not need alarm setup. Ringing must remain manageable during an active lock; alarm completion does not start/release focus. Keep legacy migrations and accessible dismissal safe.

### 5. Block, local pause and short review

The overlay recalls the participant's plan and offers “Return to my day,” routing to device home rather than the blocked app. An optional one-minute observation has pause/exit and a clear ending. It must remain local/offline, require no free-text disclosure and never release focus, change entitlement or dismiss an alarm.

Replace the scroller entry point in this build with that bounded pause. At normal completion provide one optional quick-answer review, such as “What helped you stay with your plan?” with clear “Skip.” Do not equate skipped review with failure or force another session. An unfinished session can offer a brief optional reason, without shame.

### 6. Apply voice consistently

Apply the original tone guide to static English/Hindi strings, overlay, errors, helper guide and review. No runtime LLM or model fine-tuning is necessary for this MVP. Preserve optional Shani-inspired presentation without divine impersonation, promised punishment or obligatory belief. Say focused time and awareness of thinking; do not promise hyperfocus, highest intelligence or enlightenment.

### 7. Evidence and pitch handoff

Use existing event storage patterns for minimal session outcomes. Distinguish tool chosen, setup, confirmed focus start, technical failure, block encounter, release/expiry and optional self-report. Separately record native alarm schedule, observed fire, dismissal method/failure, cancel and next occurrence; dismissal is not proof the person got up. Avoid collecting blocked-app content, private journal text or parent activity for the helper. Define denominators and missing responses. Defer broad analytics infrastructure.

Deliver the [manual pilot worksheet](../ai_org/shanivar_mvp_pilot_worksheet.md) with the build. Keep family invitations, parent willingness and young-adult activation separate; record language, chosen framing, assistance, setup time and independent repeat. Invitation data can be collected manually during an authorized pilot; do not add mandatory role intake or an analytics backend. No outreach is included in implementation.

Update README/testing instructions with exact build, changed routes, hidden feature switch, state migration behavior and known limitations. Include a short demo script for alarm-only, focus-only and combined use: set/test alarm → real fire and dismissal; goal → selected apps/duration → real focus → block/pause → review. Label a test alarm honestly rather than presenting it as overnight reliability evidence. Demonstrate the parent/helper and self-use framing without claiming different engines.

## Acceptance and tests

- Fresh install reaches either tool without charts, feed, payment or philosophy lesson; alarm and focus setup are independent.
- Parent helper cannot obtain private activity or take over a second account; self-use works without a helper.
- Every selected app is blocked while unselected apps remain available; essential calls are not included in defaults.
- Clear tool entry actions and one main action within each active state; no stale “lock not connected,” fabricated active state or false schedule.
- Large system font and TalkBack retain readable controls, explicit labels and predictable Back behavior.
- Repeat clicks, timeout/retry, expired token, process death, end/release failure and permission loss are handled by existing contracts.
- Offline pause completes/exits, does not unlock, and does not require backend availability.
- Pre-account pause works without identity or permissions and explicitly does not claim protection; direct focus entry remains available.
- Helper guide returns control to the parent; optional Shani explanation is discoverable without a mandatory faith step. Whole-app scope is clear before start.
- Every visible core string, including errors and permission explanations, passes Hindi comprehension review before a Hindi-speaking pilot; report unmet language gates explicitly.
- New installs do not start deferred features; upgrades cannot leave an active deferred service unmanaged.
- Fixed review differs from the AI scroller and has no generated-data dependency.
- Alarm tests cover repeat-day next occurrence, reboot/time/timezone changes, edit/cancel and stale/duplicate occurrences. Verify ringer stop before backend failure, hard cap, sensor failure/non-walking dismissal, and alarm/focus coexistence.
- Physical verification includes locked-screen/background alarm firing, cancellation, offline dismissal, focus overlap and a supported aggressive-OEM device when available. Report any unavailable hardware and readiness gaps.
- All ages remain the intended audience: large text, TalkBack and non-walking alternatives are required now. Document school-age account, consent, privacy and comprehension gaps before any claim of all-age readiness; the first adult pilot is a validation sequence.
- Physical-device checks are reported separately from automated results, including one aggressive-OEM device when available.

Run appropriate backend tests if backend changes. Android commands from `android-app/`:

```powershell
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug -PshanivarApiUrl=https://hybridmonk-api.onrender.com/api
```

No emulator/managed-device task on this machine. Use connected physical hardware if available. If hardware is unavailable, deliver the built debug artifact and precise outstanding checklist; do not claim production usability verification. Test claims must be based on fresh raw output.

Deliver: changed-file summary, feature visibility inventory, relevant test results, debug APK path, actual-device evidence/limitations and the current [backlog](../docs/strategy/shanivar-launch-backlog.md). A document or mockup alone does not complete this implementation task.
