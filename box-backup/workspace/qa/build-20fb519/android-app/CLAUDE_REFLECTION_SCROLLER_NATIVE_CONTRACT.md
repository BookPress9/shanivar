# Native integration contract: reflection scroller ↔ discipline engine

Claude's response to the native-integration-contract portion of [CLAUDE_REFLECTION_SCROLLER_HANDOFF.md](CLAUDE_REFLECTION_SCROLLER_HANDOFF.md). Backend routes/schema/generation are being implemented in parallel (see `ai_org/reflection_scroller_squad.md`); this document is the seam Codex's Compose UI and `DailyRhythm.kt`'s `DisciplineGateway` need to agree on. Status: contract proposal, not yet verified on device — the native discipline engine itself (real app-lockout, real alarms) is still pending per `NATIVE_HANDOFF.md`, so nothing here claims OS-level enforcement works today.

## Entry-point validation

The scroller is offered as an alternative destination on the existing blocked-app surface, opened only by explicit user action from inside the app — never from an external deep link. Concretely:

- Add an internal-only marker, e.g. `EntryPoint.BLOCKED_APP_SURFACE` vs. `EntryPoint.TODAY`, passed as a same-process navigation argument (Compose `NavController` route argument or a sealed-class UI-state field), never as an externally-resolvable deep link URI or `Intent` extra a third-party app could forge.
- If a deep link or external `Intent` ever needs to reach the scroller in the future, it must resolve to a neutral "open the app to Today" destination and require the user's own in-app tap to actually enter the scroller — do not let an external caller specify `entry_point: blocked_app` directly.
- The backend's `POST /api/reflections/sessions` accepts `entry_point: today | blocked_app` purely as an analytics/UX-shaping hint (may influence the opening card's framing) — it has **no effect on lock state** and must never be treated as authorization for anything. Ownership and session access are derived entirely from the bearer token, never from `entry_point`.

## Non-interference with locks — the one rule that matters most

Per the handoff doc, this is absolute and must be true even if every other part of the feature is broken:

- Starting, answering, finishing, retrying, or closing a reflection session **never** releases a `DisciplineGateway`-managed lock, extends/grants social-media time, changes subscription entitlement, or satisfies alarm dismissal (which the product direction already scopes to a separate verified-physical-action flow, not this feature).
- On entering the scroller from the blocked-app surface, call `DisciplineGateway.status()` and render the *current* engine-confirmed state (e.g. a small "Focus protected · Xm remaining" indicator) — do not cache or assume protection is still active once the user has been in the scroller a while; re-check on resume from background, matching the existing pattern `NATIVE_HANDOFF.md` already specifies ("Returning from Android settings must re-read permission status").
- If the scroller's own network calls fail or time out, the user must still have a working way back to Focus/Today and to existing offline practices — the scroller must never become a trap. This mirrors the handoff doc's "No answer or network dependency may trap a user inside the scroller."

## Suggested UI-layer seam (illustrative, Codex owns the real implementation)

```kotlin
enum class ReflectionEntryPoint { TODAY, BLOCKED_APP_SURFACE }

// Session/card shapes mirror the backend envelope described in
// CLAUDE_REFLECTION_SCROLLER_HANDOFF.md's "Proposed API contract" table exactly -
// session_id, version, status, mode, context_as_of, local_date, timezone,
// answered_count, session_limit, current_card, answered_history. Do not add fields
// the client infers locally (e.g. a locally-computed "next index") - the server is
// the sole source of truth for what's unlocked.

interface ReflectionScrollerEntry {
    // Called only from an in-app user action (Today's card action, or the blocked-app
    // surface's explicit "Reflect with Shanivar" button) - never from a deep link.
    suspend fun open(entryPoint: ReflectionEntryPoint, timezone: String): ReflectionSessionState
}
```

## What Codex should NOT need to build

Server-side session/card/response persistence, idempotency handling, chart/transit context computation, and Gemini generation are entirely backend-owned (see the squad doc). Codex's job is: render the card/response UI per the handoff doc's MVP-experience section (one card at a time, response required to advance, swipe/keyboard/accessibility all obey the same gate, resume-on-restart, exit-anytime), call the `/api/reflections/*` endpoints once they land, and wire the entry-point + lock-status seams above.

## Open questions for the founder (not resolved here)

Same two items `NATIVE_HANDOFF.md` already lists as unresolved for the discipline engine broadly — this feature doesn't add new ones, but inherits them: the real app-lockout mechanism and alarm dismissal are not yet implemented natively, so "the protected app remains blocked throughout" (shipping acceptance criterion 5) cannot be verified until that engine exists and is tested on a real device.
