# Shanivar event dictionary and analytics contract (DEV-65)

- **Schema version:** 1 (oldest readable: 0)  **Status:** DRAFT FOR REVIEW. Built by Backend on 17 Oct 2026; not yet reviewed or signed (see §8).
- **Sources:** DEV-65 (founder-confirmed 17 Oct 2026); thesis §3, §5 and §6; *Measurement plan: WIAC and event taxonomy* (Notion, draft); SOP-09.
- **Machine-readable copy:** `event-dictionary.v1.json`, generated from `android-app/.../analytics/AnalyticsDictionary.kt`. A unit test fails if the two drift apart.
- **Executable definitions:** `analytics/AnalyticsContract.kt` (parser and validator) and `analytics/Metrics.kt` (WIAC and companion metrics). They are checked against `sample-event-log.v1.jsonl` (§7).
- **Out of scope (ticket):** choosing a vendor, dashboards (G1) and building pipelines. No event is emitted and nothing is stored yet. Storage is an open founder question (Measurement plan: a Supabase `events` table, insert-only for own rows), so this ticket adds **no table and no migration**.

Self-reports are never scores, capability evidence or health data. Behaviour change is not evidence of awareness or wellbeing, and no health inference is ever drawn.

## 1. Common envelope (every event)
| Field | Type | PII | Rule |
|---|---|---|---|
| `schema_version` | int | none | 1 today. 0 = Measurement-plan draft names, upgraded on read (§6). Higher than current: rejected `unsupported_schema_version`. |
| `event_id` | UUID | pseudonymous | Generated on the client once per event and reused on every retry. **Dedupe key**: a store must keep it under a unique constraint. |
| `occasion_id` | UUID or null | pseudonymous | Minted at the trigger (intervention shown, routine slot, practice launch) and inherited by every event of the same journey. WIAC counts each occasion once, across features. |
| `user_id` | UUID | pseudonymous | Supabase auth uid (guest or account). Never an email, name or device id. |
| `name` | enum | none | One of §4. Unknown names: rejected `unknown_event`. |
| `ts_utc` | ISO-8601 instant ending in `Z` | quasi | When it happened (UTC). |
| `local_date` | YYYY-MM-DD | quasi | Device-local date. Must equal `ts_utc` in `tz`, else rejected `local_date_mismatch`. |
| `tz` | IANA zone | quasi | Device time zone at the event. **Missing: rejected `missing_tz`**; unknown zone: `invalid_tz`. Weeks and days are always local. |
| `app_version` | `[0-9A-Za-z.+-]{1,32}` | quasi | Build version name. |
| `content_version` | id or null | none | Practice content version (practice events). |
| `source` | enum | none | Fixed per event: initiation, routine, protection, sadhana or system. |
| `props` | object | see §4 | Only the properties listed for the event. **An unknown property is rejected**, never silently dropped. |

**Consent rule (proposed, needs founder and privacy sign-off).** Analytics is opt-in. Nothing is recorded or sent until the user turns on "Share anonymous usage events" (default off, SOP-09 step 1). Revoking consent stops recording at once and deletes unsent events from the device. The validator rejects an event with `no_consent` when the consent at event time was off. Local features (Living Orbit, the record, the self-check) do not depend on consent.

## 2. PII classes and what is never sent
| Class | Meaning |
|---|---|
| `none` | Enum, bounded number or boolean. Identifies nobody. |
| `pseudonymous` | Random app-generated id (auth uid, event, occasion, window or step id) or catalog id (practice id). Links the events of one install and names nobody. |
| `quasi` | Time, time zone or app version. Needed for the metrics; combined, it could narrow down a person, so it is never joined with contact data. |

An event's class is the highest class of its fields. Because of the envelope, every event is `quasi`. **No event may hold a higher class.**

**Never sent** (enforced by the property whitelist; the dictionary has no free-text type): free-text notes or feedback text; self-check answers, usefulness answers, drift reasons or any other self-report value; Sankalp or commitment text; contact data (email, phone, name); **protected-app package names or the protected-app list** (an `app_category` enum is sent instead, mapped on the device from Android `ApplicationInfo.category`); location; device identifiers.

## 3. Dedupe rule (WIAC)
1. Same `event_id`: one event (retries, rerenders).
2. A completion followed by `routine_step_uncompleted` for the same step and scheduled date is retracted.
3. Qualifying events with the same `occasion_id` count **once**, across features. For example, intervention, then practice, then routine step = 1.
4. Without `occasion_id`: same user and same action key (`routine:<step_id>`, `practice:<practice_id>`, `intervention:<window_id>`) within 30 minutes of the first = 1 [H].

## 4. Events (v1)
| Event | Owner | Source | Trigger | Properties [PII] | Event PII | Feeds |
|---|---|---|---|---|---|---|
| `app_first_opened` | Shanivar Frontend | system | first launch after install (once per install) | none | quasi | activation, time_to_first_value, retention_w1_w4 |
| `onboarding_step_completed` | Shanivar Frontend | initiation | an Initiation step is completed | `step` enum(welcome/goal/routine/protection/first_practice/done) [none] | quasi | activation |
| `goal_selected` | Shanivar Frontend | initiation | the user picks a primary goal (segment) | `segment` enum(distraction/routine/meditation/inquiry) [none] | quasi | activation |
| `permission_state_changed` | Shanivar Frontend | system | a needed permission is granted or revoked (checked at start and resume) | `permission` enum(usage/overlay/exact_alarm/notifications/activity) [none]; `granted` bool [none] | quasi | technical_reliability |
| `routine_created` | Shanivar Frontend | routine | the user saves a routine for the first time | `step_count` int 0..50 (optional) [none]; `window_count` int 0..8 [none]; `overnight` bool [none] | quasi | activation |
| `protection_window_saved` | Shanivar Frontend | routine | a protection window is created or edited | `window_id` id [pseudonymous]; `overnight` bool [none] | quasi | activation |
| `routine_step_completed` | Shanivar Frontend | routine | the user marks a planned activity done | `step_id` id [pseudonymous]; `scheduled_local_date` local_date [quasi]; `scheduled_local_time` local_time [quasi] | quasi | wiac, routine_completion_rate, activation, time_to_first_value, retention_w1_w4, recovery_after_missed_days |
| `routine_step_uncompleted` | Shanivar Frontend | routine | the user undoes a done mark (retracts the completion) | `step_id` id [pseudonymous]; `scheduled_local_date` local_date [quasi] | quasi | wiac, routine_completion_rate |
| `routine_step_missed` | Shanivar Frontend | routine | a planned activity's slot ends without a done mark (device-side, once per step and date) | `step_id` id [pseudonymous]; `scheduled_local_date` local_date [quasi]; `scheduled_local_time` local_time [quasi] | quasi | routine_completion_rate |
| `routine_recovered` | Shanivar Frontend | routine | the first step completed after one or more missed steps | `missed_steps` int 1..1000 [none]; `gap_days` int 0..365 [none] | quasi | recovery_after_missed_days |
| `protection_window_started` | Shanivar Frontend | protection | a protection window becomes active | `window_id` id [pseudonymous]; `armed_by_engine` bool [none] | quasi | technical_reliability |
| `protection_window_ended` | Shanivar Frontend | protection | a protection window ends | `window_id` id [pseudonymous]; `end_reason` enum(scheduled/user_stop/failure) [none]; `scheduled_minutes` int 0..1440 [none]; `active_minutes` int 0..1440 [none] | quasi | protection_window_effectiveness, technical_reliability, less_dependence_signal |
| `protection_failure_detected` | Shanivar Frontend | protection | enforcement stops while a window is scheduled | `cause` enum(service_killed/permission_revoked/doze/other) [none] | quasi | technical_reliability |
| `intervention_shown` | Shanivar Frontend | protection | the block overlay is shown (mints occasion_id) | `window_id` id [pseudonymous]; `app_category` enum(social/video/games/messaging/browser/shopping/news/other) [none] | quasi | protection_window_effectiveness, less_dependence_signal |
| `intervention_choice` | Shanivar Frontend | protection | the user picks an option on the block overlay (same occasion_id) | `window_id` id [pseudonymous]; `app_category` enum(social/video/games/messaging/browser/shopping/news/other) [none]; `choice` enum(return/pause/practice/stop_protection/essential) [none] | quasi | wiac, protection_window_effectiveness, activation, time_to_first_value, retention_w1_w4, recovery_after_missed_days |
| `practice_started` | Shanivar Frontend | sadhana | a practice the user chose starts (mints occasion_id unless one is inherited) | `practice_id` id [pseudonymous]; `launched_from` enum(home/intervention/routine/initiation) [none] | quasi | meditation_return_rate, activation |
| `practice_ended` | Shanivar Frontend | sadhana | a practice attempt ends (same occasion_id as its start) | `practice_id` id [pseudonymous]; `launched_from` enum(home/intervention/routine/initiation) [none]; `outcome` enum(completed/ended/interrupted) [none]; `active_seconds` int 0..86400 [none] | quasi | wiac, activation, time_to_first_value, retention_w1_w4, recovery_after_missed_days |
| `self_report_submitted` | Shanivar Frontend | sadhana | a self-report is saved (kind only; the answers stay in the user-owned record) | `kind` enum(stage1_check/independent/reflection) [none] | quasi | wiac_sr, activation, self_reported_usefulness |
| `alarm_fired` | Shanivar Frontend | routine | the wake alarm rings | `delay_seconds` int -3600..86400 [none] | quasi | technical_reliability |
| `alarm_dismissed` | Shanivar Frontend | routine | the wake alarm is dismissed (never a WIAC action by itself) | `method` enum(movement/qr/photo/accessible) [none] | quasi | activation, technical_reliability |
| `app_returned_after_gap` | Shanivar Frontend | system | the app is opened after 2+ local days without opening | `gap_days` int 2..3650 [none] | quasi | recovery_after_missed_days |
| `guidance_level_changed` | Shanivar Frontend | sadhana | the user changes the guidance level | `from` enum(full/reduced/minimal) [none]; `to` enum(full/reduced/minimal) [none] | quasi | less_dependence_signal |
| `sync_flush_completed` | Shanivar Backend | system | a DEV-45 outbox flush finishes (one per run) | `sent` int 0..10000 [none]; `retried` int 0..10000 [none]; `dead_lettered` int 0..10000 [none] | quasi | technical_reliability |
| `sync_dead_lettered` | Shanivar Backend | system | an outbox item is dead-lettered (no payload) | `kind` enum(attempt/self_report/routine/window/orbit/reflection/other) [none]; `error_class` enum(http_4xx/retry_limit/delete_unsupported/bad_payload/unknown_kind/other) [none] | quasi | technical_reliability |
| `feedback_submitted` | Shanivar Frontend | system | the user sends in-app feedback (category only; the text goes to the CS channel, not analytics) | `category` enum(bug/idea/content/protection/alarm/other) [none] | quasi | cs_insights |

**Qualifying intentional actions (WIAC):**
- `routine_step_completed`, unless it was retracted.
- `practice_ended` with outcome `completed`, or `ended` with `active_seconds` ≥ 10 [H: threshold to validate]. `interrupted` never counts.
- `intervention_choice` with choice `return`, `pause` or `practice`.
- **Not counted:** app opens; alarm dismissal by itself (it counts only through the planned step that follows); minutes meditated; `stop_protection`; `essential`.

## 5. Metrics (thesis §6) and traceability
Each metric id below is implemented in `Metrics.kt`, and its events are listed in `event-dictionary.v1.json` under `metrics`.

| Id | Brief metric | Definition (reference implementation) | Events / other sources |
|---|---|---|---|
| `wiac` | **North Star: WIAC** | Distinct qualifying occasions per user per **local ISO week** (Mon to Sun, device `tz`), after the §3 dedupe. Also reported: the median per *active* user that week (an active user has any accepted event; inactive users are not counted as zeros). | routine_step_completed, routine_step_uncompleted, practice_ended, intervention_choice |
| `wiac_sr` | WIAC-SR: practised without the app | Count of `self_report_submitted` with kind `independent` per user per week. **Reported separately, never added to WIAC**; a count of reports, never their values. | self_report_submitted |
| `activation` | Activation rate per segment | Among users with `app_first_opened` and `goal_selected`: the share whose segment's first meaningful action (thesis §3) happens within 7 days of the first open. A distraction: the first `intervention_choice` is an alternative. B routine: `routine_created` plus a live step completion or an `alarm_dismissed`. C meditation: a completed practice, then a `practice_started` on another local day. D inquiry: a qualifying practice launched from a routine window, or an independent self-report. | app_first_opened, goal_selected, intervention_choice, routine_created, routine_step_completed, alarm_dismissed, practice_started, practice_ended, self_report_submitted |
| `time_to_first_value` | Time to first value | `app_first_opened` to the first qualifying occasion, per user. Users without one are absent (not infinite). | app_first_opened + WIAC events |
| `routine_completion_rate` | Routine completion rate | Live completions ÷ (live completions + missed), counted per (user, step, scheduled date). A retracted completion with no miss is neither. | routine_step_completed, routine_step_uncompleted, routine_step_missed |
| `protection_window_effectiveness` | Protection-window effectiveness | Interventions resolved with an alternative (one per occasion) ÷ `intervention_shown`. Plus interventions per protected hour (Σ `active_minutes`). | intervention_shown, intervention_choice, protection_window_ended |
| `meditation_return_rate` | Meditation return rate | Practice starters who start again on a different local day within 7 days of their first start. | practice_started |
| `retention_w1_w4` | W1 / W4 retention | Share of users (with a first open) who have a qualifying occasion on local days 0 to 6 (W1) or 21 to 27 (W4) after the first open. An app open alone never counts. | app_first_opened + WIAC events |
| `recovery_after_missed_days` | Recovery after missed days | Users with a gap of 2+ local days without a qualifying occasion (after at least one) who complete one within 7 days after the gap. Gaps not yet observable at the cut-off are left out. Reported next to the number of `routine_recovered` events. | WIAC events, routine_recovered, app_returned_after_gap |
| `self_reported_usefulness` | Self-reported intentionality and usefulness | **A distribution only** (counts per category: useful / neutral / uncomfortable / not answered), read from the user-owned `practice_reflections.usefulness` records (DEV-54), never from analytics. Never a mean, a score or a per-user value, never joined into WIAC, and `uncomfortable` is not escalated. Reflection rate = `self_report_submitted` per kind. **Intentionality needs the Epic G question record (not built).** | self_report_submitted; practice_reflections.usefulness; Epic G record |
| `technical_reliability` | Technical reliability | Alarm on-time rate (\|delay\| ≤ 60 s); protection active ÷ scheduled minutes; protection failures; permission revocations; sync success = sent ÷ (sent + dead-lettered). Crash-free sessions come from crash reporting (DEV-14), not from analytics events. | alarm_fired, protection_window_started, protection_window_ended, protection_failure_detected, permission_state_changed, sync_flush_completed, sync_dead_lettered |
| `less_dependence_signal` | Guardrail: needs less intervention over time | Interventions per protected hour by ISO week, always read next to WIAC (stable or rising) and retention; plus `guidance_level_changed`. Never read alone. | intervention_shown, protection_window_ended, guidance_level_changed |

## 6. Versioning
- Additive changes (a new event, or a new *optional* property) keep the version.
- Renaming or removing an event or property, changing a type, or making a property required bumps `schema_version` and adds an upgrader in `EventContract`. The parser reads every version from `OLDEST_READABLE_VERSION` up to the current one.
- **v0 to v1** (the Measurement-plan draft): `routine_activity_completed` / `_uncompleted` become `routine_step_completed` / `_uncompleted` (`activity_id` becomes `step_id`; `scheduled_local_date` defaults to `local_date`), and `routine_saved` becomes `routine_created`. No v0 events were ever emitted; the upgrader exists so the mechanism is tested.
- A store keeps the original `schema_version` it received alongside the upgraded record.

## 7. Test cases (DEV-65) and evidence
`AnalyticsContractTest` runs on `sample-event-log.v1.jsonl`: 62 lines, 5 users, one per segment plus a second distraction user, ISO weeks 2026-W42 and W43.

| Case | Test | Result on the sample log |
|---|---|---|
| Normal: each metric computed from a sample event log | `everyMetricFromTheSampleLog` | 58 accepted, 57 after event_id dedupe. **WIAC**: A {W42: 2, W43: 1}, B {W42: 3}, C {W42: 3}, E {W42: 1}; median W42 = 2.0 (5 active users), W43 = 1.0. WIAC-SR: D {W42: 1}. Activation: distraction 1/2, routine 1/1, meditation 1/1, inquiry 1/1. TTFV: A 2 h, B 24 h 10 min, C 2 min, E 30 h. Routine completion 3/5. Effectiveness 4/6, 1.5 interventions per protected hour (W42: 1.25). Meditation return 1/2. W1 4/5, W4 0/5. Recovery 2/4 (as of 2026-10-25), 1 routine_recovered. Self-reports by kind: 1/1/1. Usefulness distribution useful 2, neutral 0, uncomfortable 1, not answered 1. Reliability: alarm on time 1/2, protection active 0.8, 1 failure, sync 8/9, 1 revocation. |
| Edge: same action logged by two features counts once | `sameActionFromTwoFeaturesCountsOnce` | Intervention choice "practice" plus the completed practice (one occasion) plus a retried duplicate = 1. A routine double tap 10 min apart = 1. A retracted completion = 0. |
| Failure: missing timezone is rejected or flagged | `missingOrInvalidTimezoneIsRejected` | Missing or empty tz: `missing_tz`; unknown zone: `invalid_tz`; a timestamp without `Z`: `invalid_ts_utc`. |
| Recovery: old events still parse after schema versioning | `oldSchemaEventsStillParseAndFutureOnesAreRejected` | A v0 `routine_activity_completed` parses as v1 `routine_step_completed`, every v1 event round-trips, and v2 is rejected. |
| Privacy | `dictionaryCannotCarryFreeTextPackagesOrSelfReportValues`, `validatorRejectsPackageNamesFreeTextAndUnknownFields`, `selfReportsAreNeverScored` | A package name, a free-text note, an extra envelope field, a package in `app_category` and free text in an id are all rejected; without consent the event is rejected `no_consent`. |
| Traceability | `everyBriefMetricIsTraceableToDictionaryEvents`, `docListsEveryEventWithOwnerTriggerPropertiesAndPiiAndEveryMetric`, `jsonDictionaryIsGeneratedFromTheKotlinSource` | All 12 §6 metrics map to dictionary events; every event feeds a metric; this doc, the JSON and the Kotlin agree. |

## 8. Review and sign-off (AC: reviewed by FE, BE, Tester, CS; privacy review signed)
| Review | Who | Status |
|---|---|---|
| Backend | Shanivar Backend | Authored 17 Oct 2026 |
| Frontend (emission points, occasion_id minting, app_category mapping) | Shanivar Frontend | **Pending** |
| Tester (test cases above, DEV-83 verification later) | Tester | **Pending** |
| Customer Success (metric definitions, DEV-66 baselines) | CS | **Pending** |
| Privacy / data-minimisation review (SOP-09; founder approves any new collection) | Founder + privacy reviewer | **Pending: not signed** |

Privacy checklist for the reviewer: (1) every property is an enum, bounded number, boolean or app-generated id; (2) no protected-app package or list leaves the device; (3) no free text or self-report value is sent; (4) consent is opt-in and revocation deletes unsent events; (5) `user_id` is the pseudonymous auth uid only; (6) retention period for stored events (open); (7) the privacy notice is updated before any collection (0016).

## 9. Open questions (founder / PM)
1. Storage: a Supabase `events` table (insert-only own rows, unique `event_id`) or none in the first release? Vendor: none (out of scope).
2. Consent default: opt-in as proposed?
3. Does `intervention_choice = essential` count as intentional? The Measurement plan says "choice ≠ stop_protection" but lists only Return / pause / practice as alternatives. **Built: essential does not count.**
4. App category versus package (Measurement plan open item). **Built: category only**, per the ticket rule "no package names of protected apps".
5. Thresholds [H]: `ended` practice ≥ 10 s active; 30-minute fallback window; alarm on time within 60 s.
6. Intentionality question (Epic G): which record holds it? It is not measurable until that exists.
7. Event retention period.
