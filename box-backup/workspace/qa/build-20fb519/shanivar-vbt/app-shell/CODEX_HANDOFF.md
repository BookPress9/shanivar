# Shanivar VBT — Handoff to Codex (Frontend/UI Implementation)

You're building the real thing. Everything in this track's other docs (`architecture-decision.md`, `onboarding-flow.md`, `session-player-spec.md`, `visual-identity.md`, and the scaffold under `scaffold/`) was written by the App/UI Agent working through the platform-capability and interaction-model questions *before* a UI builder was in the loop. Now that you're the one actually building it, treat those docs as a brief from a colleague who thought hard about the constraints, not as a blueprint you're bound to trace over. Where this doc says "fixed," that's a real constraint from another agent's work you shouldn't relitigate. Where it says "yours," genuinely make it yours — better ideas than what's sketched here are not just allowed, they're the point of putting a real UI agent on this.

**Platform: native Android, Kotlin + Jetpack Compose.** See `architecture-decision.md` for the full reasoning — short version: the discipline features (app-lockout, non-dismissible alarm) are hard native-API constraints per the Discipline Engine Agent's research, and since you're building the UI fresh regardless, there's no reuse argument left for a WebView/hybrid shell. `scaffold/` contains an earlier hybrid-leaning sketch — it's superseded, marked reference-only, not something to build on (see `scaffold/README.md`).

---

## 1. The feeling to chase

Saturn is the planet of restraint, structure, time, and consequence. This is not another breathing app with a soft glow and a lotus. The existing Shanivar web product (`frontend/src/app/(shanivar)/`, especially the "Brahma Labs" practice player) already does dark-and-gold well — void-black backgrounds, warm gold accents, soft breathing/pulsing animations, glass cards, full-pill buttons. That's a good aesthetic for the *contemplative* side of the brand. This app needs a second register layered on top of it: **heavier, quieter, more structural** — the difference between a candle and a vault door.

Concretely, chase these contrasts rather than a fixed look:
- **Weight over glow.** Where the existing site uses soft radial glows and gentle pulsing, the discipline-layer chrome (lockout status, alarm, streak-break states) should feel like a mechanism — something that latches, closes, ticks — not something that breathes. Reserve the soft/floating/ambient-pulse vocabulary for meditative content only, so users learn to *feel* the difference between "you're being guided" and "you're being held to something."
- **Carved, not rounded.** The existing UI leans on 999px pill buttons and soft glass-card corners everywhere. For anything discipline-specific (a lockout indicator, the alarm screen, a broken-streak state), square the corners off — 4–8px, not a pill. Structure should look built, not squishy.
- **A quieter, heavier gold.** The existing accent gold (`#c9a84c`) reads as temple/candlelight-warm. For this layer, pull it toward something more like aged bronze or Saturn-ring metal — desaturated, heavier, less glowing. Full palette starting point below — treat the hex values as a starting direction, not a locked design system.
- **Rings, not lotuses.** Saturn's visual signature is the ring — lean on concentric-ring motifs (which the existing session player already uses functionally as a countdown timer) as the connective visual thread across the discipline layer: a ring closing = lockout engaging, a ring completing = session finishing. Avoid lotus/mandala/chakra imagery entirely for this layer — it's already used elsewhere in the existing product and is exactly the generic-wellness-app register this needs to feel distinct from.
- **One brand, two rooms.** Don't invent a whole new palette from scratch — keep the same void-black base and gold family so this still reads as Shanivar, just a heavier room in the same house.

### Starting palette (adjust freely)
| Token | Hex | Feel |
|---|---|---|
| Void | `#0a0a0c` | Base — slightly cooler than the existing site's `#060608` |
| Lead | `#2c2e33` | Structural surface fill |
| Iron | `#4b4f56` | Borders, secondary strokes |
| Ring gold | `#a68a4a` | Primary accent — heavier than the existing `#c9a84c` |
| Signal amber | `#d97706` | Reserved *only* for live discipline states (lockout on, alarm counting down) — its rarity is what gives it meaning |
| Restraint red | `#8a2e2e` | Streak-break/consequence states — muted, not alarm-red |
| Bone | `#e8e3d8` | Text |

### Typography direction
Keep the existing font pair (Outfit headings / Inter body) for brand continuity, but push the weight range further apart than the existing site does: heavy (700–800), slightly tracked-out, often all-caps for discipline chrome ("SESSION LOCKED," countdown numerals) — should feel carved/monumental. Thin (300), generous line-height, sentence case for the actual guided-session text — should feel still and unhurried. The contrast between the two *is* the Saturn concept: structure around stillness.

Full detail and rationale: `visual-identity.md`. Use it for inspiration, not compliance.

---

## 2. Onboarding — the emotional arc, not a screen-by-screen mandate

The arc: **be honest about what this app asks of the user, before asking anything of them.** Most onboarding flows front-load minimal friction and hide the hard parts. This one should do the opposite in spirit — the value-prop carousel should say plainly "this app locks distracting apps and wakes you with an alarm you can't sleep through" as a *selling point* for the specific person this is for, not a buried permission dialog. That honesty is the brand voice, and it matters more than any specific screen layout.

The functional contract (fixed, because it's the seam between your work and two other agents' work):
- **Two-stage permission handling.** Priming (explaining *why* the app needs unusual Android permissions) happens early, during onboarding. Actually *granting* them (leaving the app to Android Settings for Usage Access / exact-alarm / overlay) is deferred to the first-session-scheduling moment, not forced upfront. This isn't a design preference — cold special-access permission requests before any trust is built is a real, measured source of onboarding drop-off, and the permissions here are the "special access" kind Android doesn't offer a simple runtime dialog for. Feel free to redesign exactly how/where this shows up, just don't collapse it back into a single upfront permission wall.
- **A handoff into the Assessment Agent's intake module that you don't own the content of.** The Assessment Agent (`shanivar-vbt/assessment/`) owns the actual questions and scoring. Your onboarding flow wraps it — you own the chrome around it (progress framing, back-button behavior, exit confirmation) — and you receive back an assigned technique id + confidence + a `placeholder_flag`. See §3 for what that payload actually looks like right now.
- **Permission grants are never a hard gate on core content.** If a user declines lockout/alarm permissions, the app still works — those features just show as "off, enable anytime." This matches the founder's don't-force-adoption philosophy.

Full sketch with ASCII wireframes (genuinely just a sketch, redesign freely): `onboarding-flow.md`.

---

## 3. Session player — the functional contract + fixed data sources

The existing web session player (`frontend/src/app/(shanivar)/practice/page.tsx`, the `ExperimentPlayer` component) is real, working, tested proof that a specific interaction shape works: arrival → a held koan-style question → a sequence of practice stages → reflection → integration, with a breathing ring, a typewriter reveal for the key question, a per-stage circular countdown, timeline dots, and a completion screen with XP/streak. **Play with this in the browser if you can — it's the single best reference for pacing and emotional beats in this product.** Then build something native and your own; don't try to literally port the React component.

What's fixed, because it comes from other agents' work you shouldn't redesign:

**(a) Assessment output** — `shanivar-vbt/assessment/`: `intake-questions.md` (the actual questions), `routing-algorithm.md` (scoring logic across 7 category codes: `BR` breath, `SO` sound, `VI` visual-gazing, `BS` body-sensation, `GT` gap-between-thoughts, `SA` spatial-awareness, `EM` emotion-based), `routing-table.md` (category → technique mapping). **Note:** `routing-table.md` was written before the Content Agent's techniques landed and still has `TBD-<code>` placeholder slugs — the Staff Manager reconciles this against real technique filenames; treat the technique id you receive as an opaque identifier you resolve through the content schema below (b), not something to hardcode against today's placeholder file.

**(b) Technique content** — `shanivar-vbt/content/`: all 12 Level-1 techniques are landed (Content Agent finished this track). Each `technique-XX-*.md` file gives you, per technique: a primary-source verse citation + translation edition (e.g. "Vijñāna-bhairava-tantra, verse 24... trans. Christopher Wallis, 2017/2018"), an "Essence" summary, a full guided-session script (~3–6 min, written for modern secular guided-audio delivery), modern-adaptation notes (what was intentionally dropped from the original doctrine and why), a suggested duration, and category tags (the same 7-code vocabulary as the routing table). **The citation must be surfaced in your UI, quietly** — this is QA's hard rule (every technique ships with a verifiable source, and the product must show it, not just internally track it), not a decoration you can cut. See `session-player-spec.md` §2.1 for a starting treatment (a small expandable "Source: VBT · verse N" tag) — style it however fits your screen, just don't drop it. Three of the twelve techniques carry a QA-flagged citation caveat (single-source-only, or cross-edition verse-numbering variance) — check `content/README.md`'s summary table before shipping if you want to know which three, though QA/Staff Manager owns actually resolving those flags.

**(c) Discipline-engine native API surface** — `shanivar-vbt/discipline-engine/platform-research.md`: this is the Discipline Engine Agent's completed platform research (not yet built). Its recommended architecture: app-lockout via `UsageStatsManager` foreground-app polling + a `SYSTEM_ALERT_WINDOW`-class block overlay (**not** `AccessibilityService` — Play Store policy risk, spelled out in detail in that doc); the alarm via `AlarmManager.setAlarmClock` + a full-screen-intent Activity + a foreground service, with the physical/verified dismissal (QR scan / puzzle / movement — a locked founder decision, not yours to change) implemented as that Activity's content. Whatever module boundary the Discipline Engine Agent ends up building, your session player needs at minimum: a live lockout-status read (so a status indicator reflects real OS-level state, not optimistic UI state), a "release lock" call tied to your hard-exit-confirmation flow, and a deep-link entry point so a dismissed alarm can open your player directly at a specific session, skipping your dashboard. That last one is the actual enforcement mechanic worth preserving even if you redesign everything else: **the alarm's only real "off switch" is starting the session it was set for.**

Everything else in `session-player-spec.md` (exact stage sequence, exact wording of the hard-exit dialog, exactly where the citation tag sits on screen) is a sketch, not a spec you're bound to. Keep the three fixed seams above; redesign the rest with real creative energy.

---

## 4. What's fixed vs. what's yours — summary

**Fixed (other agents' work, integrate against it, don't redesign it):**
- Platform: native Android/Kotlin/Compose (`architecture-decision.md`)
- Assessment intake questions + scoring/routing logic (`shanivar-vbt/assessment/`)
- The 12 VBT techniques' content, scripts, and required citations (`shanivar-vbt/content/`)
- Discipline-engine's recommended native mechanism and Play Store constraints (`shanivar-vbt/discipline-engine/platform-research.md`) — and the two functional facts derived from it: lockout status must reflect real OS state, and the alarm's dismissal must lead directly into the scheduled session
- The two-stage (prime-then-grant) shape of special-access permission requests, and that permissions are never a hard content gate

**Yours (genuinely, make real decisions here):**
- Every actual screen layout, Compose component structure, navigation graph, animation implementation
- The full visual execution of the Saturn/discipline direction — palette/type/motion given above are a starting point to riff on, not a locked system
- Exact onboarding screen sequence and copy (beyond the two fixed seams above)
- Exact session-player layout, exact citation-tag treatment, exact hard-exit dialog design
- Anything not explicitly listed as fixed above

If something in `onboarding-flow.md`, `session-player-spec.md`, or `visual-identity.md` reads as a rigid instruction rather than a hint, treat it as the latter — those docs were written before this handoff existed and lean more prescriptive than intended for a UI builder with real creative ownership. When in doubt, honor the *mood* (Saturn: restraint, structure, weight) and the *fixed seams* above, and make the rest genuinely yours.
