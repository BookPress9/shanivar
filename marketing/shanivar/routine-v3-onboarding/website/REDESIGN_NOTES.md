# Redesign notes: home page V4 (less text, more interactive), 2026-10-12

Decision gate (SOP-00): task = redesign pre-launch home; serves PRD Feature 5 waitlist website (`docs/blueprint/PRD.md:47-48`) and the one-line description Routine + Sadhana (`docs/blueprint/PRD.md:9`). Read: AGENTS.md, SOP-00, PRD, DESIGN_SYSTEM, SHANIVAR_STANCE, SOP-02, SOP-05, brand/tagline.md, voice-direction.md, current site.

Home visible words: 1552 before, 642 after (-59%).

## Section -> source
| Element | Source |
|---|---|
| Tagline "Discipline is freedom." (founder's line, not doctrine) | `marketing/shanivar/brand/tagline.md:3-6`, `docs/sop/SOP-05-marketing-and-claims-review.md:10` |
| Hero value line: Routine shapes the day; Sadhana until the skill survives without the app | `docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md:13-14`, `docs/blueprint/PRD.md:9` |
| "in development", not released, Android, no Play Store | `docs/sop/SOP-05-marketing-and-claims-review.md:14,16`, `docs/blueprint/PRD.md:10,58` |
| "Your day" builder: wake/sleep anchors, sample activities, chips Saved / Needs device access, "Illustration, not the app" | `docs/blueprint/PRD.md:33,38`, `docs/blueprint/DESIGN_SYSTEM.md:47`, previous site timeline (git d22864b `index.html`) |
| Wake alarm accessible way out; protection voluntary, stoppable, never unbreakable; Saved is not Active | `docs/blueprint/PRD.md:20,34-35,40-42`, `docs/blueprint/DESIGN_SYSTEM.md:47`, `marketing/shanivar/brand/tagline.md:30` |
| Stillness moment: natural breath, unpaced, untimed, end anytime, stationary/suitable setting note, nothing stored | `docs/decisions/0011-vbt-breathing-third-pillar.md:16`, `research/deep-foundation/PRACTICE_SAFETY.md:32`, `docs/blueprint/PRD.md:36` |
| End text "However that felt, it's information. Nothing to fix." | `docs/blueprint/DESIGN_SYSTEM.md:47` (`BreathingScreen.kt:302`) |
| Sadhak path: six principles verbatim, no interpretation | `SHANIVAR_STANCE.md:273-312` (list at `:291-296`, "Shanivar interpretation: None added") |
| Steady only Sadhana content; no streaks/points/ranks | `docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md:18`, `docs/blueprint/PRD.md:53` |
| "Notice the mind. Don't control it." / is-isn't toggles (detox, therapy, astrology, social, trap, needing it less) | `marketing/shanivar/brand/tagline.md:32`, `docs/blueprint/PRD.md:20,53`, `docs/sop/SOP-05-marketing-and-claims-review.md:14` |
| Form unchanged (early-access, data-netlify, bot-field, source); draft privacy note beside it; 18+, Android 8+ | `docs/sop/SOP-02-website-change-and-deploy.md:15,21`, `docs/blueprint/PRD.md:10,48` |
| FAQ trimmed to 5 (download, iPhone, lock-out, bypass, cost) | `docs/blueprint/PRD.md:10,41`, previous site FAQ |
| V3 palette, system fonts, Saturn as decoration only | `docs/blueprint/DESIGN_SYSTEM.md:13`, `style.css:1-14` |

Privacy page: wording NOT changed. SOP-02:21 makes privacy-notice changes a founder approval gate. About page: two paragraphs shortened, no new claim.

## UNRESOLVED: questions for founder
1. What does "Sadhak" mean on the public site, and what does "Become a Sadhak" invite someone to do? No document defines it (only internal role names and research files). The door is used as you asked; the section shows only the six principles.
2. What is "the Hybrid Monk way"? No document defines it. The phrase is NOT used on the site.
3. Do you want a short explanation under each of the six principles? ST-009 says "Shanivar interpretation: None added" (`SHANIVAR_STANCE.md`), so the cards say only "No interpretation added."
4. "Try 30 seconds": the docs describe Steady as one minute after a stillness check. Is a public, untimed text-only invitation acceptable before the content review (`docs/decisions/0014...:18`, Steady "still needs review")? Nothing is timed or counted.
5. May the privacy page be tightened (SOP-02:21 approval gate)?
6. Marketing is paused (SOP-05:7): this is a local commit only, not published.

# Update 2026-10-12 (evening): immersive narrative from ST-010

Questions 1 and 2 above are answered by the founder: `SHANIVAR_STANCE.md` ST-010 (line 312 onward), `docs/decisions/0015-sadhak-and-hybrid-monk-core-ideology.md`.
Narrative beats (each from ST-010 founder wording): "There is suffering." / "Becoming a Sadhak is a path to becoming free of it." (+ "A contemplative aim, not a treatment.", PRACTICE_SAFETY.md:49-51) · "The right stance." / "Face it. Grow through it." · "Sadhana is uncomfortable." / "If you cannot cope, you simply cannot. Just accept it." (+ "Stopping is always allowed.", PRACTICE_SAFETY.md:47) · "The Hybrid Monk." / "Connected with everything. Attached to nothing." · "Headless. Just the body." (interactive, replaces the earlier stillness moment; unpaced, nothing recorded, stationary note PRACTICE_SAFETY.md:32) · "Discipline is freedom." + two doors.
Not used on the site: "living dangerously", "bliss and ecstasy", Osho's name (needs founder sign-off, see below).
Home visible words: 638.

## UNRESOLVED (ST-010)
1. Sign-off on the safety reading in ST-010: "just accept it" read as "accepting includes stopping", never "endure".
2. May the site use "living dangerously in bliss and ecstasy"? Withheld: risk of being read as physical risk or a promised state.
3. Name Osho publicly as an influence, or keep it internal?
4. Sign-off on the public lines above, especially "There is suffering" and "a path to becoming free of it".
5. Still open from before: privacy-page tightening (SOP-02:21), marketing pause (SOP-05:7), Steady review before a public presence invitation.
