# Shanivar VBT — Session Player Spec

**Owner:** App/UI Agent
**Status:** Written against a proven interaction model, for a native (Kotlin/Compose) build owned by Codex. `frontend/src/app/(shanivar)/practice/page.tsx` (`ExperimentPlayer` component) already contains a full multi-stage guided-session player (arrival → question → practice → reflection → integration), with a breathing ring, typewriter effect, binaural preset selector, per-stage circular countdown, timeline dots, and an XP/streak completion screen. **This is real, working proof that the interaction model works — treat it as reference for the stage sequence, pacing, and emotional beats, not as code to port or wrap.** The Android build is native (see `architecture-decision.md`), so this spec describes the functional contract (data shape, new discipline-layer elements, entry points) for Codex to implement fresh in Compose, free to reinterpret the layout.

Checked `shanivar-vbt/content/` for real technique files before writing this — the directory does not exist yet (Content Agent hasn't landed its deliverable). This spec is written against the existing `Stage` shape already used in code, extended with the fields VBT content and QA's citation rule require, so Content Agent can target a known contract once its 12 techniques are ready.

---

## 1. Data contract (extends existing `Stage` type)

Existing shape (from `practice/page.tsx`):
```ts
interface Stage {
  id: number;
  name: string;
  type: 'arrival' | 'question' | 'practice' | 'reflection' | 'integration';
  durationSec: number;
  text?: string;
  subtext?: string;
}
```
Extension needed for VBT + QA's scriptural-integrity hard rule (every technique must cite verse + translation edition — `ai_org/shanivar_vbt_track.md` Agent 1 hard rule):
```ts
interface VBTStage extends Stage {
  sourceCitation?: {
    verse: string;       // e.g. "Vigyan Bhairava Tantra, Dharana 24"
    edition: string;     // e.g. "trans. Lakshman Jee, 1979 ed."
  };
}
```
Citation is attached per-stage (not just per-technique) because a single guided session may draw on more than one dharana. QA's agent verifies this field is populated and matches a known source before any technique ships — the player's job is only to render it, not validate it.

## 2. New UI elements over the existing player

### 2.1 Citation footer (new)
Small, unobtrusive tag, bottom-left of the stage content area, expandable on tap:
```
Source: VBT · Dharana 24 ⌄
```
Tapping expands to full edition citation inline. Deliberately quiet — this is a scriptural-integrity requirement, not a marketing element, so it should not compete visually with the stage text. Uses the muted `--sv-iron` tone from `visual-identity.md`, not the accent gold.

### 2.2 Lockout status pill (new — discipline layer)
Persistent, top of screen, replacing the existing player's plain stage-counter text when a session has lockout engaged:
```
┌───────────────────────────────┐
│ ✕ Exit   ⛓ Lock: ON — Social  1/8│
└───────────────────────────────┘
```
Sourced live from the discipline-engine module's status call (not local UI state) so the pill reflects the actual OS-level lock state, not an optimistic UI guess. If the user never granted the lockout permission (see `onboarding-flow.md` §6, non-blocking), this pill is simply omitted — the session plays normally, just without the enforcement.

### 2.3 Hard-exit confirmation (new — discipline layer)
The existing web player's `✕ Exit` button closes immediately. When lockout is active, exiting early needs a confirmation step that also intercepts the hardware back gesture (a plain dialog dismiss is not enough — Android's system back gesture must route through the same check):
```
End session early?
Today's discipline streak will break,
and blocked apps unlock immediately.

[Stay]   [End anyway]
```
On "End anyway": release the lockout via the discipline-engine module before closing the player, so the OS-level block is torn down in the same action that closes the UI — these must not desync (block staying active after the player thinks it closed is a real bug class to guard against explicitly).

### 2.4 Audio handling (flag, not fully speced here)
The existing web player has a binaural preset selector but no evidence of actual audio playback in the reviewed code (no `<audio>`/`Web Audio` calls found in `practice/page.tsx`) — likely a placeholder UI ahead of the audio implementation. For VBT guided narration + binaural layering to work reliably during a locked-down session (must keep playing while backgrounded behind the lockout, screen may be off), use native `MediaPlayer`/`ExoPlayer` with proper `AudioFocus` handling — standard for a native build, flagged here only because OEM battery-optimization behavior (same class of risk `discipline-engine/platform-research.md` flags for the lockout/alarm services) can still suspend background audio unless the session holds a foreground service for its duration. Not fully speced in Phase 1 — deeper Phase 2 work — but Codex should account for a foreground-service-backed audio session, not a fire-and-forget player, from the start.

### 2.5 Completion screen addition
Existing `SessionComplete` component (streak, XP, share card) gains one conditional line when lockout was active for the full session and not broken early:
```
Discipline maintained. Lock held for the full 12 minutes.
```
No new component needed — one conditional string in the existing component, driven by a `lockoutHeldFullSession: boolean` passed down from the native plugin's session-end report.

## 3. Entry points into the player

1. **Normal**: Dashboard → "Today's Practice" card → player opens with the day's assigned `VBTStage[]` for the user's routed technique.
2. **Alarm-triggered** (new, discipline-layer specific): the non-dismissible alarm, once dismissed via its required physical action (a dedicated full-screen alarm Activity — see `architecture-decision.md`), deep-links directly into this player for the scheduled session — skipping the dashboard. This is the literal enforcement mechanic: the alarm's only "off switch" is starting the session it was set for. The alarm activity hands off via an Android intent that the app's own navigation graph resolves to the session-player route with the relevant `session_id` pre-loaded — no dashboard detour.

## 4. What this spec does not cover

- The actual guided-session **content/text** for the 12 VBT techniques — Content Agent's deliverable, this spec only defines the container it renders into.
- Citation **verification** — QA/Scriptural Integrity Agent's job; this spec only defines where the citation is displayed.
- Assessment-driven **technique routing logic** — Assessment Agent's; this spec only consumes the `assigned_technique_id` handed off per `onboarding-flow.md` §4.
- Discipline-engine module **internal implementation** (`UsageStatsManager` polling detail, exact-alarm scheduling detail — see `discipline-engine/platform-research.md`) — this spec only defines the call surface the player needs (status/release), not how those calls are fulfilled internally.
- Exact Compose screen layout, navigation graph structure, and component architecture — Codex's to design; see `CODEX_HANDOFF.md`.
