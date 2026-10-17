# Design System — Shanivar

Sources: `android-app/wireframes/shanivar-v3-wireframe/shanivar-v3/README.md` (founder-selected reference, `:3`), website `style.css:7-14`, `ShanivarApp.kt:45-55`, `docs/strategy/shanivar-tone-and-values.md`.

## 1. Design Direction
- **Style:** Editorial, serious, high-contrast; alternating light paper and black panels (wireframe README `:29`).
- **Mood:** Direct, real, serious without shame (README `:48-54`).
- **Principles:** one primary action per screen (tone-and-values `:25`); UI follows true engine state (`:17`); light surfaces take dark text, dark surfaces light text, never dark-on-dark (README `:29`).

## 2. Brand
- **Name:** Shanivar — by The Hybrid Monk. Keep separate from the Hybrid Monk clothing business (`ai_org/agents/instagram_keeper.md:44`).
- **Tagline:** "Discipline is freedom." (founder, 2026-10-11; `marketing/shanivar/brand/tagline.md`). Caution recorded there: do not use it to imply one right way for everyone.
- **Logo/mark:** Saturn motif: thin gold ringed-planet outline with "SHANIVAR" in spaced capitals (`instagram_keeper.md:61`). Decoration only, never a breathing pacer; must not pulse in breath rhythm (`:41`). UNRESOLVED: no repo source file for the full lock-up.
- **Brand voice:** see §Voice below.

## 3. Color Palette
Two token sets exist; they differ.
| Token | Website/wireframe (`style.css:8-10`) | Android (`ShanivarApp.kt:45-49`) |
|---|---|---|
| Background | `#0b0d0b` (panel `#0f120f`) | Void `#101111` |
| Surface | `#151915` | `#191B1A` |
| Text | ink `#f7f5ee` | Bone `#E8E3D8` |
| Muted | `#c9cec7` | `#B6B3AD` |
| Accent (primary) | sage `#d8e5b7` | "Gold" `#D1DCAA` (sage-like) |
| Accent (secondary) | gold `#c9aa73` (on paper `#7a5c2b`) | none |
| Paper | `#f2efe6`, ink `#111510`, muted `#4a5148`, line `#cbc7bb` | none |
| Border | `#343a33` | outline `#686B72` |
- UNRESOLVED: success/warning/error colors are not defined as tokens. Which values?
- UNRESOLVED: should Android adopt the website palette (paper panels)? The V3 wireframe calls for light and dark panels.

## 4. Typography
- Headings: serif stack (Iowan Old Style, Palatino, Georgia…); body: system sans (`style.css:11-12`). H1 `clamp(2.3rem,7vw,4.4rem)`, H2 `clamp(1.75rem,4.4vw,2.8rem)`, H3 1.25rem bold sans (`:20-22`); body 1rem/1.6 (`:17`).
- UNRESOLVED: Android typography is not documented here (Material3 defaults assumed, not verified).

## 5-7. Spacing, radius, shadows
UNRESOLVED: no spacing scale, radius or shadow tokens are recorded. Web content width `--wrap: 68rem` (`style.css:13`). Touch targets at least 44 px on web, 52 dp on the Sitting primary button (`InitiationSitting.kt` `PrimaryActionEnabled`).

## 8-10. Buttons, inputs, cards
- Primary action: full-width, large target (`InitiationSitting.kt`). End/Skip always reachable in practice (PRACTICE_SAFETY.md:34).
- Focus: 3 px gold outline, offset 3 px (`style.css:27`); darker `#6b4f22` on paper (`:28`).
- UNRESOLVED: disabled/loading/error styles for buttons and inputs; card padding/border.

## 11. Navigation
Two bottom destinations: Routine and Sadhana. No Dashboard. Settings secondary (0014:16).

## 12. States
Say what is known: "Protection did not start." then a repair; unknown status reads "Checking protection." (tone-and-values `:29`, `:46`). Protection states: saved, access granted, ready, active, needs attention, off (wireframe README `:66-75`). Practice End screen text: "However that felt, it's information. Nothing to fix." (`BreathingScreen.kt:302`, `:418`). UNRESOLVED: empty/loading patterns.

## 13. Icons
UNRESOLVED: icon library not documented. Saturn motif is the only recorded mark.

## 14. Responsive Design
Mobile reference 390 x 844 CSS px (README `:111`); web must not overflow at 320 px or at 200% zoom on 1280 px (website README accessibility). UNRESOLVED: breakpoints.

## 15. Accessibility
Semantic landmarks, skip link, labelled inputs, AA contrast, visible focus (website README). Reduced-motion stops Saturn animation; End always available (wireframe README `:114-115`). Large text, TalkBack, landscape and physical-device review are not yet done (README `:116`; `web-app/README.md` open item 6).

## Voice (tagline and writing rules)
- **Tagline:** Discipline is freedom. (see Brand.)
- **Application voice:** direct, serious, action-oriented; pattern truth → contradiction → instruction → action (README `:56-58`). Founder positioning: "very strict, high-velocity" is translated as high standards, never lock-in (website README Positioning note).
- **Sadhana/editorial voice:** patient teacher (`SHANIVAR_STANCE.md:11`; `voice-direction.md:34`). Whether one voice or a split is used is open (D1, `instagram_keeper.md:69`). UNRESOLVED: D1.
- **Ideology tone (ST-010, decision 0015):** quiet, short lines; Hybrid Monk (connected with everything, attached to nothing) and Sadhak (face suffering with the right stance; discomfort accepted, stopping valid). Presence = "headless, just the body". Never physical risk, never a health promise, no Osho quotations.
- **Rules:** ordinary verbs (Choose, Start, Allow, End, Skip); a missed session is information; never speak as Lord Shani; no "unbreakable", "impossible to bypass" (`ENFORCEMENT_STANCE_2026-09-12.md:26-29`); no streaks, scores, levels, no health or transformation claims (PRACTICE_SAFETY.md:49-51).
