# Shanivar VBT — Visual Identity: Saturn / Discipline Direction

**Owner:** App/UI Agent

## Starting point: what already exists

`frontend/src/app/globals.css` already defines a dark, gold-accented aesthetic for the existing Shanivar/Brahma Labs web product: void-black background (`#060608`), warm temple-gold accent (`#c9a84c`), Outfit for headings, Inter for body, soft glowing pulses (`breathe`, `nebula-pulse`, `float`), rounded glass cards, and 999px pill buttons throughout. It reads as premium and calm — closer to "mystical observatory" than the soft-pastel cliché the brief warns against, which is a reasonable base — but it is **one register**, and the VBT/discipline track needs a **second, distinct register within the same family**: something that feels like restriction and weight, not glow and float. Saturn is the planet of restraint, structure, time, and consequence — the identity should feel like that, not like another guided-breathing app.

The goal here is not to replace the existing palette but to define a **discipline-specific extension** of it, used specifically for VBT session chrome, lockout state, and alarm/discipline UI — so the two tracks read as one brand family (same base black, same font family) while feeling like distinctly different modes: contemplative vs. enforced.

## 1. Palette

| Token | Hex | Use |
|---|---|---|
| `--sv-void` | `#0a0a0c` | Base background — marginally cooler/darker than existing `#060608`, sets the discipline surfaces slightly apart from Brahma Labs' warmer black |
| `--sv-lead` | `#2c2e33` | Card/surface fill — a structural grey, not the existing warm `#161616` |
| `--sv-iron` | `#4b4f56` | Borders, dividers, secondary icon strokes — reads as machined metal, not "glass" |
| `--sv-ring-gold` | `#a68a4a` | Primary accent — deliberately more desaturated/heavier than the existing `#c9a84c`. Same family (still gold), but reads as aged bronze / a Saturn-ring metal rather than a temple glow. Used for structure: progress rings, active-state borders, primary CTA text |
| `--sv-signal-amber` | `#d97706` | Reserved *exclusively* for discipline-critical live states — lockout engaged, alarm countdown. Never used decoratively. Its rarity is what gives it meaning: seeing this color means something is actively being enforced right now |
| `--sv-restraint-red` | `#8a2e2e` | Streak-break / session-abandoned / violation states. Deliberately muted compared to the existing bright `#ef4444` danger-red — this isn't a form-validation error, it's a consequence, and should feel heavier/quieter, not alarming-red |
| `--sv-bone` | `#e8e3d8` | Primary text — slightly more desaturated than the existing ivory `#f5f0e8`, avoids feeling "warm/soft" |

Rationale for keeping it in the same hue family as the existing site (black + gold) rather than inventing a new palette from scratch: this is still Shanivar, one brand — the VBT track should feel like a different room in the same building, not a different building.

## 2. Typography

Keep the existing font pair (Outfit / Inter) for continuity — do not introduce a third typeface. Push the **weight range** further apart than the existing site does, to create a structure-vs-stillness contrast that maps directly to the Saturn theme:

- **Discipline chrome** (lockout pill, streak counters, "Session Locked," countdown numerals, alarm screen): Outfit **700–800**, slightly increased letter-spacing (0.5–1px), all-caps for short labels. Should feel carved/monumental — the opposite of a soft app notification.
- **Contemplative content** (the guided-session stage text itself, reflection prompts): Outfit **300**, generous line-height (1.6–1.8), sentence case. This is already the existing player's pattern for stage text (`fontWeight: 300` seen in `practice/page.tsx`) — keep it, don't touch the meditative register, contrast it against the heavier discipline chrome around it.
- Body/UI copy stays Inter at existing weights (400–600).

## 3. Iconography motifs

**Avoid:** lotus, mandala, chakra wheel, generic "zen" imagery — already used elsewhere in the existing Brahma Labs UI (chakra chant protocol, sacred-geometry background dots) and explicitly what the brief asks to differentiate away from for this track.

**Use instead**, all rendered as thin (1px) stroke line icons, not filled/rounded blob icons:
- **The ring** — Saturn's literal visual signature, and already present functionally in the existing player as the circular progress/countdown ring. Lean into this harder: use concentric-ring motifs as the master shape across the discipline layer — a ring closing = a lockout engaging, a ring completing = a session finishing, a ring with a gap = time remaining. One shape, three meanings, all Saturn-coherent.
- **The gate/lock glyph** — a simple geometric gate (two verticals + a crossbar), not a padlock icon (too generic-app-security). Used only for the lockout indicator.
- **The hourglass or sand-grain mark** — for alarm/time-pressure states. Avoids clock-face cliché, keeps the "sand/time running out" feeling tied to discipline rather than generic scheduling.
- **A single vertical line** — used sparingly as a structural divider motif (a staff, a boundary) instead of the soft rounded dividers used elsewhere. Reinforces restraint visually.

**Shape language:** the existing site uses 999px full-pill buttons almost everywhere. For discipline-specific chrome (lockout pill, alarm screen buttons, streak-break states), square off the corners to 4–8px radius instead — deliberately less soft, more structural. Keep full pills for the meditative/session-content chrome to preserve continuity with the existing Brahma Labs product.

## 4. Motion

The existing site's animation vocabulary (`breathe`, `nebula-pulse`, `float`, continuous soft pulsing) is right for contemplative content and should stay for it. For discipline-layer elements specifically, motion should read as **mechanism engaging**, not ambient glow:
- Lockout pill: a single ring-closing rotation animation when the lock engages (not a continuous pulse) — it happens once, like a mechanism latching, then holds still.
- Alarm countdown: a hard, discrete tick per second (numeral change, ring segment fill), not an eased/soft transition — should feel like a real clock, not a breathing animation.
- Streak-break state: no animation at all — a plain, static, muted-red state. Absence of motion here is deliberate; over-animating a "you broke your streak" moment risks making it feel like a game-over screen rather than a real consequence.

Reserve the soft/floating/pulsing animation vocabulary entirely for meditative content, so the two feelings (stillness vs. structure) stay perceptually distinct even though they share a color family and a font family.

## 5. Summary intent

One brand, two rooms: Brahma Labs' existing gold-glow-and-float register stays as-is for general Shanivar web content. The VBT/discipline track gets a **cooler, heavier, quieter variant of the same palette and type family** — Saturn as structure and consequence, not Saturn as "spiritual aesthetic." The test for any new discipline-layer UI element going forward: does it feel like a mechanism (ring latching, gate closing, sand running out), or does it feel like a wellness-app decoration? If the latter, it belongs in Brahma Labs' existing register, not here.
