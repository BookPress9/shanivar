# Shanivar "Messages from Shanivar" Reflection Scroller — Delivery Squad

Full spec: [android-app/CLAUDE_REFLECTION_SCROLLER_HANDOFF.md](../android-app/CLAUDE_REFLECTION_SCROLLER_HANDOFF.md) (Codex → Claude feature handoff, 2026-09-09). Supporting context: [android-app/PRODUCT_DIRECTION.md](../android-app/PRODUCT_DIRECTION.md), [marketing/shanivar/audience-direction.md](../marketing/shanivar/audience-direction.md), [android-app/NATIVE_HANDOFF.md](../android-app/NATIVE_HANDOFF.md).

This supersedes the earlier blanket "no content feed" constraint, per explicit founder direction. Claude owns backend persistence, generation, chart/transit context, and the native discipline integration contract. Codex owns the Kotlin/Compose frontend. This is not authorization to deploy infrastructure or send external messages.

## Founder decision resolved same day: registration chart seeding
`auth.js` no longer seeds a dummy chart (was: hardcoded Leo/Scorpio/Aries, lat/lon 0,0, on any `birth_date`). Now: a real chart is computed via `computeNatalChart` only when `birth_date` + `birth_time` + `utcOffset` + numeric `lat`/`lon` are all present at registration; otherwise no chart row is created at all (no fake data), and the user can generate a real one later via `POST /chart/generate`. This matters for the scroller's mode determination (`personalized` requires a real chart on file).

## Staff Manager
Sequences the squad, reconciles the Creative Agent's voice/content guidelines into the Generation Service Agent's prompts (built in parallel against a placeholder voice spec, same pattern as the VBT content track), owns the native integration contract doc for Codex, and runs the final live-DB smoke test + release checklist required by the handoff doc's "Delivery expected from Claude" section.

## Agent 1: Creative / Audience Psychology
**Owns**: genuine-engagement psychology research (not manipulative dark patterns) translated into concrete card-writing guidelines, response-option design, and a real bank of example cards — both `general` mode and chart-informed-mode patterns — consistent with the patient-teacher stance and the explicit "avoid" list in both the handoff doc and audience-direction.md (no obedience framing, no "I know you better than yourself," no guaranteed self-realization, no fear-based hooks). Deliverable feeds both the Generation Service Agent's prompts and Codex's UI copy/interaction pacing.

## Agent 2: Backend Data / API
**Owns**: migrations (sessions/cards/responses, idempotency keys) and the full authenticated `/api/reflections/*` route contract exactly as specified in the handoff doc's API table — concurrency, version guards, ownership isolation, exact status codes.

## Agent 3: Backend Generation Service
**Owns**: context-snapshot construction (chart revision, transit-as-of, mode determination), natal-to-transit aspect computation in code (not claimed by the LLM), schema-validated Gemini generation, provenance labeling, fallback behavior on provider failure. Builds against Agent 2's documented schema (spec'd fully in the handoff doc, so no blocking dependency) and a placeholder voice spec pending Agent 1.

## Native integration contract (Staff Manager, not delegated)
A concise handoff doc for Codex covering: how the scroller is entered as an alternative destination on the blocked-app surface (validated internal entry-point marker, no trust of external deep-link extras), and confirming reconciliation with the existing `DisciplineGateway` interface in `DailyRhythm.kt` (starting/answering/closing the scroller never releases locks or satisfies alarm dismissal).

## Release checklist against the handoff doc's 7 shipping acceptance criteria (2026-09-09)

1. **Real chart/transit fixtures, correct attribution, honest degradation** — ✅ backend. Verified live: a user with a real chart gets `mode: personalized` and a card citing a genuinely computed aspect (transiting Uranus conjunct natal Mercury); mode determination (`personalized`/`limited_chart`/`general`) is unit-tested including the legacy-dummy-chart detector so no old fake-seeded chart can be misread as real. Provider failure falls back to a labeled `general_fallback` card (observed live during testing — one real transient Gemini error correctly fell back, next attempt succeeded).
2. **Card cannot be bypassed** — ✅ backend enforces via version/card_id checks (409 on stale version or wrong card_id) regardless of client path (swipe/keyboard/API); ⚠️ **not yet verified**: the actual Compose UI's swipe/accessibility/keyboard paths all routing through this the same way is Codex's implementation, not yet built/tested.
3. **Response acceptance survives restart/timeout/duplicate/parallel/retry-key-conflict** — ✅ verified live: real two-way parallel `POST /responses` race resolved to exactly one winner (200) and one 409 via row-level locking, not just sequential mocks; idempotency-key replay confirmed non-duplicating.
4. **Cross-user isolation** — ✅ verified live: second user gets 404 (not 403) on every route against the first user's session; delete confirmed via direct DB query to remove session/cards/responses/idempotency-key rows.
5. **Blocked-app entry, lock non-interference, alarm non-interference** — ⚠️ **not verified** — this requires the native discipline engine (still pending per `NATIVE_HANDOFF.md`) and Codex's Compose integration against `android-app/CLAUDE_REFLECTION_SCROLLER_NATIVE_CONTRACT.md`. Nothing in the backend can grant/release locks (confirmed by design — no route touches lock/entitlement state), but the actual on-device guarantee is unverified.
6. **Native build, focused Compose/API tests, accessibility inspection** — ✅ backend Jest suite (251/251, 20 suites); ⚠️ **not done**: Compose UI doesn't exist yet, no native build/accessibility pass possible until Codex implements it.
7. **Review of representative generated conversations, fixture vs. live evidence separated** — partial: this document + squad reports are the live evidence trail (real Gemini calls, real chart data, tagged test accounts); a dedicated content-quality review pass across many real generated sessions has not been run as a separate step.

**Overall status: backend is feature-complete and live-verified. This is NOT a shipped feature** — per the handoff doc's own instruction ("Report the feature as shipped only after native integration and required live/device checks pass"). Remaining blockers are entirely on the native/device side: Codex's Compose UI implementation against the API contract and native contract doc, and the underlying discipline engine's real device verification (a pre-existing, larger blocker this feature inherits, not one it introduces).

## Shipping acceptance criteria
See the handoff doc's "Shipping acceptance criteria" section (7 items) — this is the actual release gate, not a formality. The feature is reported "shipped" only after native integration and required live/device checks pass; a passing mocked-only test suite is not sufficient per the handoff doc's own instruction.
