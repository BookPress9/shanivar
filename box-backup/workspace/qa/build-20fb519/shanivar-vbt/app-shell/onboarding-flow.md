# Shanivar VBT — Onboarding Flow Spec

**Owner:** App/UI Agent — owns the onboarding *shell* (chrome, sequencing, permission priming, the handoff point). The intake assessment's actual questions and scoring logic belong to the Personalization/Assessment Agent (`shanivar-vbt/assessment/`); this doc defines the wrapper around that module and treats its content as a black box.

Design principle: Android "special access" permissions (Accessibility Service, exact-alarm, display-over-other-apps) cannot be granted from a normal runtime dialog — they require sending the user to a system Settings screen. Asking for them cold, on first launch, before the user has any reason to trust the app, is the single biggest onboarding-drop-off risk in this flow. So permission requests are split into two moments: **priming** (explain, during onboarding) and **granting** (actually leave the app to Settings, deferred to the point of first need — scheduling the first session).

---

## Screen sequence

```
0. Native splash
   ↓
1. Value-prop carousel (3 cards, swipeable)
   ↓
2. Account creation / login  (reuses existing web /register, /login)
   ↓
3. Permission priming (explain WHY, no OS dialogs yet)
   ↓
4. Handoff → Assessment module (owned by Assessment Agent)
   ↓
5. "Your Path" reveal (routing result from Assessment)
   ↓
6. First-session scheduling → THIS is where real OS permission
   grants happen (Accessibility Settings deep link, exact-alarm,
   display-over-other-apps)
   ↓
7. Home / Dashboard
```

## 0. Native splash

Native Android splash (not WebView — avoids the blank-white-flash while the WebView boots). Saturn ring mark, single slow rotation, ~1.5s max, then transitions into screen 1. See `visual-identity.md` for the mark itself.

## 1. Value-prop carousel — 3 cards

Sets tone immediately: this is not a soft meditation app. Copy is short and declarative.

```
┌─────────────────────────────┐
│                               │
│         (ring glyph)          │
│                               │
│   A discipline engine.        │
│   Not another meditation app. │
│                               │
│   Vigyan Bhairava Tantra,     │
│   made practicable — and      │
│   enforced.                   │
│                               │
│        ● ○ ○                  │
│                    [Skip]     │
└─────────────────────────────┘
```
Card 2: "12 techniques. One matched to you." — teases the assessment without describing it (that's the Assessment Agent's content).
Card 3: "One session a day. No easy exit." — previews the lockout/alarm concept honestly, as a selling point for the specific user this app is for, not hidden in fine print. Ends in a single CTA: `Begin`.

## 2. Account creation / login

Reuses the existing frontend routes as-is inside the WebView: `/(shanivar)/register` and `/(shanivar)/login`. No new auth UI needed — this is exactly the kind of screen that should NOT be rebuilt natively per the architecture decision. Native shell adds only: a thin top bar with a back caret disabled on this screen (can't back out of account creation into the carousel and lose the flow state), and suppresses the app's normal bottom nav (not present yet at this stage).

## 3. Permission priming

Plain-language, one screen, no jargon, no OS dialog triggered yet.

```
┌─────────────────────────────┐
│  Before you begin             │
│                               │
│  Shanivar can lock distracting│
│  apps during your session,    │
│  and wakes you with an alarm  │
│  you can't sleep through.     │
│                               │
│  Both require a couple of     │
│  Android permissions. We'll   │
│  ask for them right before    │
│  your first session — not     │
│  now.                         │
│                               │
│         [Understood →]        │
└─────────────────────────────┘
```
Deliberately does NOT trigger the Settings deep-links here. This screen exists purely to build the "why" before the user is asked to leave the app and dig through Android Settings — doing that cold, before any value has been delivered, is the classic reason hybrid/utility apps lose users at onboarding.

## 4. Handoff → Assessment module

**Contract between App/UI shell and Assessment Agent's module:**

Onboarding shell renders a persistent wrapper around whatever the Assessment Agent builds:
- Progress header: `Step 2 of 3 — Know Thyself` (generic; does not assume question count, which is the Assessment Agent's call)
- Back button **disabled** during assessment (no returning to account screen mid-assessment — avoids partial/duplicate submissions)
- Exit attempt shows a confirmation dialog: "Leave without finishing? Your answers won't be saved." — native modal, not a WebView `confirm()`, since a real device back-gesture needs to be intercepted before the WebView even sees it.

Handoff payload — shell → assessment module, on entry:
```json
{ "user_id": "<uuid>", "onboarding_session_id": "<uuid>", "started_at": "<iso8601>" }
```
Return payload — assessment module → shell, on completion:
```json
{
  "assigned_technique_id": "<string>",
  "routing_confidence": "<low|medium|high>",
  "placeholder_flag": true,
  "scores": { "...": "opaque to shell, Assessment Agent's schema" }
}
```
`placeholder_flag: true` is expected until Content Agent's finished technique list lands (per `ai_org/shanivar_vbt_track.md` §Agent 2 dependency) — the shell does not branch on this flag, it only passes `assigned_technique_id` through to screen 5 as an opaque string/id and asks the (future) Content Agent's module to resolve it to a real technique name.

## 5. "Your Path" reveal

```
┌─────────────────────────────┐
│                               │
│     Your path has been set    │
│                               │
│     [ TECHNIQUE NAME ]        │
│     "one-line teaser copy     │
│      from Content Agent"      │
│                               │
│     First session: ~12 min    │
│                               │
│      [Schedule first session] │
└─────────────────────────────┘
```
If `placeholder_flag` is true, technique name/teaser fall back to a generic "Level 1 — Return to Stillness" placeholder rather than blocking onboarding entirely on Content Agent's deliverable landing.

## 6. First-session scheduling — real permission grants happen here

This is the deferred "point of need" moment from Section 3.

```
┌─────────────────────────────┐
│  When should today's session  │
│  happen?                      │
│                               │
│   [ Time picker ]             │
│                               │
│  To make this stick, Shanivar │
│  needs two things:             │
│                               │
│  ☐ Block distracting apps      │
│    during the session          │
│    → Enable (opens Settings)  │
│                               │
│  ☐ Wake you with a real alarm  │
│    → Enable (opens Settings)  │
│                               │
│         [Confirm & Continue]  │
└─────────────────────────────┘
```
Each "Enable" triggers the discipline-engine module's permission-request call (Settings > Special app access > Usage access for lockout, per `discipline-engine/platform-research.md`'s recommendation of `UsageStatsManager` over `AccessibilityService`; exact-alarm + full-screen-intent settings for the alarm), which deep-links to the relevant system Settings screen and returns control to the app via `onResume` state check (Android's standard pattern for special-access permissions — there is no callback, the app re-checks permission state when it regains foreground). If a user declines, this screen does not hard-block — the session can still be scheduled without lockout/alarm, and the checklist item is shown as "Off — enable anytime from Settings" so the discipline layer is not a forced gate on core content access. This matches the founder's monetization-deferred, don't-force-adoption philosophy.

*(Note: the exact native call names above are illustrative of the contract, not a fixed API Codex must match literally — see `CODEX_HANDOFF.md` for what's fixed vs. Codex's to design.)*

## 7. Home / Dashboard

Existing `/(shanivar)/dashboard` route, unchanged, entry point from here on.
