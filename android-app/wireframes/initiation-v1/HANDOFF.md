# Initiation — engineering handoff

Status: proposed interaction reference implemented as a local browser wireframe. No API or Android integration is claimed. Input: founder-attached SHANIVAR — THE INITIATION; constraints: SHANIVAR_STANCE.md and accepted V3 Routine/Sadhana shell. Do not treat this file as a new production release decision.

## Screen and transition contract

| View | Action / successful consequence | Failure / alternative |
| --- | --- | --- |
| Arrival | Take command → identity; no motion delay | Reduced motion shows static orbit |
| Identity | Trimmed display name → authentication | Empty name blocked; local guest route |
| Authentication | Google or verified email code → proposition | Cancel/error/offline stays here; preserve draft; guest available |
| Proposition | Ready → anchors | Back keeps draft |
| Anchors | Save wake/sleep; seed practice at wake + 5 min → alarm | Missing values blocked; practice placement editable later |
| Alarm | Request schedule; render returned state; continue → apps | Denied/failed shown; existing alarm skips scheduling |
| Apps | Select stable package IDs → window | No selection blocked; Not now → foundation |
| Window | Save distinct start/end + repeat + apps → access | Equal times invalid; overnight allowed; change apps |
| Access | Open individual settings and inspect on resume | Denied/partial retains saved boundary; continue available |
| Foundation | Show actual saved choices and tool statuses → Routine | No claim skipped tools are enabled |
| Routine | Edit, practice, boundary off, orbit, invitation | Local exit works without account/network/payment/practice |
| Editor | Save anchors and optional activity → Routine | Invalid time/duration blocked; changed wake invalidates schedule readiness |
| Sadhana entry | Stationary confirmation → 60s practice | Back available; no session before confirmation |
| Practice | Foreground active timer; pause/resume; completion → return | Background pauses; immediate End records ended, not completed |
| Return | Routine or orbit | No transformation/attainment inference |
| Orbit | Read action record; voluntary independent-practice record | Never declines on missed day; no spiritual score |
| Week | Optional invitation; add activity or practice | Skip has no access penalty |

Developer rail can jump anywhere for review. Production navigation must use the valid persisted checkpoint, not review routes. Prototype timeline shows chosen anchor order; production sorts occurrences by resolved local datetime, filters recurrence and handles overnight windows.

## Data model (proposed, not existing schema)

Every durable entity has server UUID, owner ID, created/updated timestamps and revision. Device state also has device ID and observed timestamp; server data cannot prove device enforcement.

| Entity | Required fields |
| --- | --- |
| OnboardingDraft | schemaVersion, checkpoint, displayName, identityMode, completedAt nullable |
| Routine | IANA timeZone, wakeLocal HH:mm, sleepLocal HH:mm, practiceLocal HH:mm, revision |
| Activity | routineId, name (1–80 chars), startLocal, durationMinutes (1–1440), recurrence + weekday set |
| AlarmConfig | deviceId, wakeLocal, repeatDays, desiredEnabled, configRevision |
| AlarmObservation | configRevision, status off/configured/permission_required/scheduled/failed, nextFireAt nullable, reason, observedAt |
| ProtectionWindow | packageIds[], startLocal, endLocal, recurrence, timeZone, desiredEnabled, revision |
| DeviceObservation | usageAccess, overlayAccess, alarmCapability, serviceHealth, effectiveProtectionState, observedAt |
| PracticeAttempt | attemptId, contentVersion, startedAt, activeSeconds, outcome completed/ended, endedAt |
| OrbitEvent | id, type, occurrenceDate, source configured/device_observed/self_reported, entityId nullable |

Never reuse existing gamification XP/level as an aura field. Orbit events are append-only action history, not consciousness evaluation. A revoked permission changes readiness immediately without deleting historical action points. Independent records are self-reported, never verified attainment. Return events require a real visit on a distinct local day; silence is not evidence. The prototype’s compact JSON is a UI fixture, not the database design.

## Proposed API boundary

These endpoints are suggested contracts requiring backend implementation/reconciliation with existing routes. They are not claimed to exist. Read `backend/src/routes/auth.js` and existing route middleware before integration: the inspected register handler requires email/password, so passwordless onboarding needs new auth support.

| Operation | Request / response |
| --- | --- |
| POST /v1/auth/google/exchange | Provider credential → validated server session + profile; verify audience/issuer/expiry server-side |
| POST /v1/auth/email/challenges | email → challengeId, expiresAt, resendAfter; rate-limit and avoid account enumeration |
| POST /v1/auth/email/verify | challengeId + single-use code → session; expiry/retry limits enforced server-side |
| GET /v1/onboarding | Current draft + revision; no remote account required for local guest |
| PUT /v1/onboarding | Draft + expectedRevision → saved draft + new revision |
| PUT /v1/routine | Timezone, anchors, activities + expectedRevision → canonical routine |
| POST /v1/protection-windows | Window + Idempotency-Key → saved rule; not device-ready |
| PUT /v1/protection-windows/:id | Revisioned edits; deletion/off propagates to local scheduler |
| POST /v1/devices/:id/observations | Actual native results with configRevision → acknowledged observation |
| POST /v1/practice-attempts | Client-generated attempt UUID + outcome + activeSeconds + contentVersion → deduplicated record |
| GET /v1/orbit | Action events / dates → derived visual flags, no numeric aura score |
| POST /v1/orbit/independent-records | Local date + explicit self-report → source-labelled event |

Example routine request:

```json
{"timeZone":"Europe/Berlin","wakeLocal":"07:00","sleepLocal":"23:00","practiceLocal":"07:05","activities":[],"expectedRevision":0}
```

Example saved boundary response, before native reconciliation:

```json
{"id":"server-generated-uuid","revision":1,"saved":true,"desiredEnabled":true,"deviceStatus":"unknown"}
```

Use owner-scoped authorization for all data; never trust a request owner ID. Use HTTPS, platform-secure token storage and validated provider credentials. Browser demo state contains no production token. Guest data lives locally; explicit sign-in migration must not duplicate existing records. Conflict returns 409 + canonical revision; invalid input 422; rate limit 429 with retry guidance; auth expiry 401. UI must retain draft on failure. Retry creation with the same idempotency key. Persist mutation queues locally and reconcile on reconnect; local disabling must take effect immediately even if sync fails. Minimize optional telemetry and do not send package lists or personal activities as generic analytics payloads.

## Android is responsible for device truth

Inspect and reuse `AlarmScheduler.kt`, `AlarmFireReceiver.kt`, `AlarmForegroundService.kt`, `BootRescheduleReceiver.kt`, `BlockOverlayActivity.kt` and existing Routine/Focus contracts. A backend cannot schedule or verify Android alarms on its own.

1. Enumerate launchable installed apps by package ID and icon; preserve selected packages when uninstalled but show unavailable. Maintain essential/emergency exclusion and recovery rules.
2. Save desired alarm locally, request applicable access, then schedule. Return configRevision + nextFireAt only after scheduler acceptance. Permission return, reboot, app update and timezone/time changes trigger reconciliation. Show missing capability or failed scheduling immediately; do not equate scheduler acceptance with a demonstrated physical ring.
3. Request usage and overlay access individually. Returning from settings must inspect actual status; clicking Open settings never grants access. Verify existing enforcement architecture and service health before deriving ready/active. Saved, ready, active, needs-attention and off are separate states.
4. Resolve recurrence in device IANA zone. Specify DST policy: for nonexistent local time, next valid local instant; for repeated time, first occurrence only. Verify this policy against existing accepted behavior before shipping. Overnight windows carry into the following day; reconcile edits atomically.
5. Essential access and local exit remain visible in an actual blocking surface. No server call is needed to stop. Network failure, process restart and stale observations cannot reactivate a user-disabled rule.
6. Practice uses a monotonic foreground timer. Persist attempt on pause/background/end, never infer completion from elapsed wall time while closed. Preserve existing V3 content version and release gates; this browser copy is an interaction reference, not a new practice release.

## Visual implementation contract

Reference viewport 390×844. Colors: ink #0d0e0f, paper #eee9de, bronze #c49a69, muted #aaa59c. Georgia/system fonts keep the artifact offline. Primary controls minimum 50px; default serif title 37px. Paper cards use dark text. Review workspace is outside the product shell. Large text may increase content height; allow scrolling, never truncate a decision or pin it under system bars.

Living Orbit: dim sphere initially; ring after routine creation; three historical points for alarm configured, boundary saved, first practice attempted. A subtle second presence follows genuine return-day or explicit independent record. Motion is ornamental, slow and unrelated to breathing. Reduced-motion uses the same static meaning. Text provides all semantics; color/motion alone never communicates readiness. Missed days do not erase history. No share flow, leaderboard or public status claim is added.

## Delivery order and production acceptance

1. Implement draft/local persistence, account adapters and guest migration. Verify interruption at every checkpoint and expiry/cancel/error paths.
2. Connect Routine mutations to existing data model; implement idempotency, revisions, recurrence and timezone policies.
3. Wire native alarm/access adapters and status reconciliation. Test denied/partial/revoked permissions, failed scheduling, actual ring/dismissal, reboot, daylight-saving transition and offline local exit on physical Android devices.
4. Connect existing practice player and records; verify pause/background/kill/end without fabricated completion.
5. Derive orbit from actual records; optional first-week prompts must be dismissible and never unlock content.
6. Review TalkBack, focus order, 200% text, contrast, small devices, landscape and reduced motion. Founder reviews the exact experience before a public release.

Browser QA confirms simulated interaction paths only. Backend team should implement the service contracts; Android team must implement and physically verify device behavior. No deployed service, real authentication, production database or new clinical/spiritual claim is supplied by this package.
