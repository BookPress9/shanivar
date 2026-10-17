# Native UI integration handoff to Claude

2026-09-09. The frontend lives in android-app/, using Kotlin/Jetpack Compose. The older Capacitor scaffold is not the implementation.

## New feature handoff: Messages from Shanivar

Founder-requested personalized, response-gated text scroller and optional blocked-app alternative: [Claude implementation handoff](CLAUDE_REFLECTION_SCROLLER_HANDOFF.md). Includes proposed endpoints, durable progress, chart/transit provenance, native entry behavior and shipping acceptance criteria. Feature remains to be implemented.

## Existing API calls connected
| Method/path | Request | UI expects |
| --- | --- | --- |
| POST /auth/login | email, password | token, followed by GET /users/me |
| GET /users/me | Bearer token | id, display_name or username, streak_days |
| GET /insights/today | Bearer token | insight: insight_text, action_text, reflection_text, insight_date, id |

The all-zero insight ID is the current backend fallback sentinel. The UI labels this General reflection. A proper source/provenance field should replace that sentinel in a future agreed contract. Stale dates are displayed as saved notes. API errors preserve the server's error field.

## Native discipline seams to agree
DisciplineGateway in DailyRhythm.kt is a proposal, not an implemented engine:
- Observe protection status from the engine (unavailable, permission needed, off, active); UI must also receive scheduled end time and protected packages.
- Start a focus session with duration and selected packages. Return confirmed state; preserve permission-denied and entitlement errors distinctly.
- Release protection before exiting its session. On release failure keep the session screen open with retry.
- Alarm scheduling needs local time, IANA timezone, weekdays, sound, verified dismissal configuration, technique/session ID, and the confirmed next occurrence.
- Dismissal must expose verification in progress/succeeded/failed and support retry. UI must not treat a normal tap as movement verification.
- Alarm entry needs a validated session identifier and player deep link; a stale/missing session must have a recovery path.
- Returning from Android settings must re-read permission status. Practice remains usable if permissions are declined.

## Paid access
The founder's latest direction explicitly includes paid locks, superseding the old no-payment scope for the final product. Working interpretation: subscription entitlement for blocking. No fee for breaking a lock and no price has been assumed. Need confirmed products/prices, purchase/restore/cancel states, entitlement validity and offline policy before enabling payment controls.

## WhatsApp
Current /whatsapp/verify marks phone_verified immediately (simulated verification), so the native UI must not use it as real verification. Need OTP/send/verify, stored opt-in, notification timezone/time/quiet hours, pause/opt-out, and delivery status. Keep n8n secrets and service endpoints out of the app. No messages have been sent from this work.

## Assessment ambiguity found during frontend review
The shell can consume assigned_technique_id, routing_confidence, placeholder_flag and tags. No HTTP route or confidence thresholds are specified yet.
- routing-algorithm.md §4 says max_score <= 1 always falls back to BR.
- routing-table.md example 6 expects BS for TP_REACTIVE + EMO_NONE + STILL_MODERATE, but the remaining scores are BR=1 and BS=1, which triggers the documented BR fallback.
- Example 11 clarifies that both positive Q4-mapped categories stay candidates; do not pick only the largest Q4 delta as an unstated tie-break.
Resolve these before porting scoring or making personalized assignment claims. Content IDs are reconciled and the QA report now passes all 12, despite stale placeholder/flag language in older handoff docs.

## Practice persistence and streaks
Local technique IDs are slugs from the Content team's filenames. Legacy POST /practices/complete accepts database practice IDs, so they are not interchangeable. Need stable mapping or a native session contract, server-side retry/idempotency, completion criteria and timezone-day handling. Current local text sessions deliberately do not claim account streak/XP changes. The legacy complete route's streak update also needs a same-day multi-practice regression test before connecting native completions.

## Chart-based specificity
The current registration endpoint seeds dummy signs and the chart generator asks an LLM for exact planet positions. Daily insight generation asks the LLM to infer transits. These do not establish calculation accuracy. Need validated astronomical calculation data, birth location/timezone resolution, uncertain-birth-time behavior, and provenance/currentness metadata before a chart-accuracy UI can be truthful.

## Verification ownership
Frontend: routine behavior, navigation, accessible controls, timing, error handling, source rendering, API request/response handling, screenshots and native build.
Backend/native engine: real device blocking, alarm/reboot/OEM behavior, paid entitlement enforcement, WhatsApp delivery, chart calculations, session persistence and streak correctness.
Release requires both sets. A passing simulated UI test does not prove OS enforcement or message delivery.
