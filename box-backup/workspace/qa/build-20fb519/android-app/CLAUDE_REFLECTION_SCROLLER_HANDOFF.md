# Feature handoff to Claude: Shanivar reflective text scroller

Initial-launch override, 13 September: the [streamlining task](CLAUDE_MVP_STREAMLINE_TASK_2026-09-13.md) defers this feature's entry points. Preserve its implementation, privacy rules and contracts; do not make it a dependency of focus, the local pause or short fixed review. This handoff remains the future feature reference.

Date: 9 September 2026. Status: ready for implementation; this document does not claim the feature is built or shipped.

## Founder request and outcome

Build a text-based vertical scroller for people who want the satisfaction of discovering the next item. Each item feels like a direct message from Shanivar, uses the user's birth chart and current planetary positions as context for self-reflection, and requires an input before the next item becomes available. It also provides an optional destination while other social media apps are locked by Shanivar.

Carry forward the patient teacher stance: clear, personal, gently challenging and aimed at the user's own understanding. “Dopamine” describes the founder's desired sense of interest and reward, not a measurable neurochemical or clinical claim. The user should find an interesting question, respond, and discover a relevant next message.

This explicitly supersedes the earlier blanket “no content feed” product constraint. Preserve meaningful interaction and visible stopping points. Do not silently turn the request into a static daily note or allow passive swiping past unanswered items.

## Ownership and implementation starting points

Claude: own backend persistence, generation, chart/transit context and the native discipline integration contract; coordinate delivery of the complete feature with Codex's Kotlin/Compose frontend ownership. Implement the backend and engine seams without waiting for an additional product approval; provide concrete contracts and fixtures for the UI. This local handoff is not authorization to deploy infrastructure or send external messages.

Read these current sources before changing code:

- `backend/src/utils/astrology.js`: real calculation helpers, currently tropical zodiac; reuse the calculation service and label its actual system. Do not describe it as sidereal/Vedic without implementing that system explicitly.
- `backend/src/routes/chart.js`: authenticated chart generation and read-back; birth input validation and chart persistence.
- `backend/src/routes/insights.js`: calculated current-transit context and existing daily-note generation.
- `backend/src/routes/ai.js`: existing AI integration; its in-memory conversation history and generic fallback are insufficient for durable scroller progress and provenance. Do not inherit “consciousness stage” assertions as facts about a person's spiritual attainment.
- `android-app/app/src/main/java/com/shanivar/nativeapp/ShanivarApp.kt`, `ShanivarApi.kt`, `DailyRhythm.kt`, `MainActivity.kt`: Compose UI, networking and proposed discipline gateway.
- [Native integration handoff](NATIVE_HANDOFF.md), [product direction](PRODUCT_DIRECTION.md), [marketing stance](../marketing/shanivar/audience-direction.md).

Repository inspection confirms calculation code exists. Some older handoff text says charts are LLM-generated; that is stale relative to current code. Recheck actual runtime behavior and tests; this handoff does not independently verify chart accuracy or release readiness.

## MVP experience

1. Enter “Messages from Shanivar” from Today or an explicit “Reflect with Shanivar” action on the blocked-app surface. Label the experience as AI-guided reflection on entry; these are in-app messages, not human-authored DMs or WhatsApp delivery.
2. Show one text card at a time, with sender “Shanivar,” a brief observation, one question, and optional expandable “Why this reflection?” context. Target 40–90 words, one question per card. Use readable type, generous spacing and subtle motion.
3. Offer quick responses such as “This fits,” “Not today,” and “I'm unsure,” plus optional free text (1–1,000 characters after trimming). A selected response counts as input. “Prefer not to share” is also a valid explicit response; private disclosure is never necessary to advance.
4. Keep the next item unavailable until a valid response is accepted by the server. Swiping, keyboard navigation, accessibility actions and direct API calls must obey the same rule. Previously answered messages remain readable.
5. After acceptance, reveal a Continue action and allow a vertical swipe to the next card. Use the response to shape that next question; disagreement should change direction rather than provoke insistence that the chart is right.
6. Default implementation choice: five cards per session, then a quiet summary and an optional “Continue reflecting” action to start another session. No automatic endless loading. This is a configurable starting choice, not a founder-mandated cap.
7. Exit or return to another Shanivar screen at any time without answering. Resume the current unanswered card after restart. Never equate exit or disagreement with failure.

Illustrative general card (not a real chart reading): “You came here looking for something new. Before the next thing arrives, what were you hoping it would change about this moment?”

Chart-informed cards must refer only to supplied computed facts. Pattern: one supported chart/transit detail, an explicitly interpretive connection to an everyday theme, then “Does that fit today?” Never invent an aspect, birth placement or present emotional state to make copy feel personal.

## Context and generation

- Create a server-side snapshot with chart revision, birth-data quality, calculation engine/version, zodiac/house system, transit UTC timestamp, user's IANA timezone and local date. Pin it for a session; show its date. A session resumed on a later local day offers a fresh session and labels the old one as saved.
- Feed the model only necessary computed chart facts, current transits, optional user goals and relevant responses from this session. Separate natal positions from current positions. Calculate natal-to-transit aspects in code before claiming them; omit unsupported aspects.
- Missing or uncertain birth information must produce an explicit `general` or `limited_chart` mode. Omit houses/rising or other time-sensitive facts when unsupported. Seeded dummy signs are not a personalized chart. Do not guess missing data.
- Treat astrology as a reflective interpretive framework, not proof of personality, a mental-health diagnosis or a prediction of inevitable events. Ground “where you are today” in time-stamped positions and what the user says about their current experience.
- Return schema-validated structured content: message, question, response options, mode, and factual context references. Validate every referenced fact against the snapshot. Render plain text; do not execute model markup, URLs or instructions.
- Keep the patient teacher voice. Do not demand obedience, claim privileged access to the true self, or guarantee self-realization. Treat user text as untrusted data, not generation instructions.
- On provider failure use a clearly labeled general reflection or a retry state. Never label generic fallback as personalized. A retry must not replace an already displayed card or lose its response.

## Proposed API contract — implement and reconcile with native client

All routes under `/api/reflections`, authenticated with existing bearer auth. Derive ownership from the token. The following routes are new proposals, not existing endpoints.

| Method/path | Input | Result |
| --- | --- | --- |
| POST `/sessions` | `entry_point: today | blocked_app`, IANA `timezone`, idempotency key | Create or replay a session; return session state and only the current card |
| GET `/sessions/:id` | None | Owned session, answered history, current card and generation status; never future unanswered cards |
| POST `/sessions/:id/responses` | `card_id`, `expected_version`, `response: {kind: option | text, value}`, idempotency key | Persist response once; return new version, generation state and next card if ready |
| POST `/sessions/:id/retry` | Idempotency key | Retry failed next-card generation without resubmitting or duplicating the accepted response |
| POST `/sessions/:id/close` | Idempotency key | End the session without answering; closing never changes app locks |
| DELETE `/sessions/:id` | None | Delete owned reflection content and associated responses; invalidate caches/jobs |

Session envelope: `session_id`, `version`, `status` (`awaiting_response`, `generating`, `generation_failed`, `completed`, `closed`), `mode` (`personalized`, `limited_chart`, `general`), `context_as_of`, `local_date`, `timezone`, `answered_count`, `session_limit`, `current_card`, `answered_history`. Card: `id`, `index`, `sender`, `message`, `question`, `options[{id,label}]`, `context_refs`, `provenance_label`. Client never supplies authoritative next index, chart ownership or lock state.

Response submission returns 200 when ready, or 202 with persisted progress and `generating`; poll GET with bounded backoff. Invalid response: 422. Stale version/out-of-order card or reused key with a different payload: 409. Unowned/missing session: 404. Auth failure: 401. Rate limit: 429 with retry guidance. Temporary unavailability before acceptance: 503; retain the draft.

After timeout, fetch session state or replay the same idempotency key before creating another response. Never show an unlocked next card based solely on optimistic local state. Offline MVP: read cached history/current card and retain a local draft; reconnect and acknowledge before advancing. Clearly indicate unsent input.

## Persistence and concurrency

Add versioned migrations for sessions, cards and responses. Sessions store owner, context snapshot/reference, generation status, version and timestamps. Cards have a unique `(session_id, index)`; responses have a unique `(session_id, card_id)`. Scope idempotency keys to authenticated owner and operation and retain request hashes/results.

In a transaction, validate current card/version, persist the response and enqueue/reserve the next generation once. Generate outside the database transaction; persist exactly one next card under a unique constraint and version guard. Use a durable job/outbox or equivalent recoverable database state so a crash after acceptance can resume generation. Do not use process-local chat history as source of truth. Two tabs, repeated taps and job retries must converge on the same accepted response and next card.

Store responses as private reflections, separate from public community content and analytics. Send only necessary context to the AI provider and explain that processing before first use. Do not log birth inputs, free text or model prompts in analytics/error logs. MVP responses remain until the user deletes the session or account; account deletion must cascade. If local history/drafts are cached, use protected app storage and clear them on logout/account switch. Do not claim immediate deletion from provider systems unless their configured retention supports it.

## Locked-app integration

- Offer the scroller as an alternative destination on the existing blocked-app UI, opened by explicit user action. Keep Shanivar itself out of the blocked package set.
- Pass a validated internal entry-point marker; do not trust arbitrary external deep-link extras or expose response content in links.
- Starting, answering, finishing or closing reflections does not release locks, grant extra social-media time, change subscription entitlements or satisfy alarm dismissal.
- Read active lock status from the native engine when returning to Focus or the blocked-app surface. Preserve duration, permission-loss handling and existing explicit release behavior.
- If the scroller fails/offline, keep a way back and access to existing offline practices. No answer or network dependency may trap a user inside the scroller. Do not imply OS enforcement works until verified on device.

## Shipping acceptance criteria

1. Real chart/transit fixtures produce correctly attributed context; uncertain/missing data and provider failures visibly downgrade rather than invent personalization. Date/timezone rollover and stale sessions behave as specified.
2. A card cannot be bypassed through swipe, screen reader action, modified client request or guessed card ID. Every offered non-disclosing option advances; whitespace-only/oversized text is rejected without losing the draft.
3. Response acceptance survives process restart, provider timeout, duplicate taps, parallel devices and retry-key conflicts. Exactly one answer and next card persist. Failed generation can recover without answering twice.
4. User A cannot read, respond to, retry or delete user B's sessions. Deletion invalidates queued generation and cached content; account switches do not expose drafts/history.
5. The blocked-app entry opens the scroller, the protected app remains blocked throughout, leaving works without responding, and reflection completion does not dismiss an alarm or release protection. Verify permissions, background/resume and engine errors on a real supported device.
6. Native build and focused Compose/API tests pass. Inspect actual screens for large fonts, TalkBack, keyboard overlap, reduced motion, long responses, loading/error states and five-card completion.
7. Review representative generated conversations for grounded facts, response-sensitive continuation, gentle teaching, no deterministic claims and honest fallback labels. Record fixture/provider/model context and separate mocked from live evidence.

## Delivery expected from Claude

Return backend migrations, authenticated endpoints, generation service, contract fixtures and tests; engine entry-point implementation/contract; frontend integration status coordinated with Codex; and a concise release checklist with actual evidence and remaining blockers. Report the feature as shipped only after native integration and required live/device checks pass. No social publication is part of this feature handoff.
