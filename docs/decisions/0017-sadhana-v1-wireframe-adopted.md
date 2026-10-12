# 0017 - Sadhana V1 wireframe adopted as Sadhana design source

- **Date:** 2026-10-12
- **Status:** Proposed. Founder prototype received 12 October 2026. Adopted as the design source, with the open questions below for the founder. Build waits for the founder's reply.
- **Design source of truth:** `android-app/wireframes/sadhana-v1/index.html` (with `app.js`, `style.css`, `spine.css`, `exports/`), `HANDOFF.md`, `README.md`, `QA.md`.
- **Build plan:** `android-app/wireframes/sadhana-v1/BUILD_PLAN.md`.

## Decision
1. **Living Spine** is the main layout of the Sadhana destination: seven centres, from Muladhara to Sahasrara, around a vertical bronze practice trace. Three layers stay separate (HANDOFF "Living Spine architecture"): anatomical orientation, which is not a scan; the tradition-specific subtle-body map; and the recorded practice trace.
2. **Capabilities** are `Arrive → Return → See → Choose → Carry`: verbs, not levels. Rules are deterministic and auditable. Any earlier practice stays open, and a manual "use fewer prompts" control is always available.
3. **Factual tracking:** attempts, active foreground seconds (monotonic clock), outcomes (completed, ended or interrupted), and self-reported practice without the app. There is no composite score.
4. **Guidance reduces** as the user chooses (full, reduced or minimal), never silently.
5. **Deep study** is open from day one. Reading a source creates no readiness signal.
6. **Research-only separation:** intensive methods are contextual study only, never unlocked by attempts, minutes, completion, self-report or payment.
7. **Copy and look:** as with 0016, keep the wireframe's copy and tokens; do not rewrite. Production text maps to released content versions.

## Supersedes
- **0013** progression proposal (options A/B/C). The prototype's "progression without status" is now the concrete design. 0013's bans stay in force: no streaks, XP, rank or "spend time in the app" metric.
- **0011** catalogue placement. Sadhana practices move from a breathing-pillar catalogue to `PracticeDefinition` records with status, capability, risk class and review manifest. The breath-awareness seed survives as the `natural` practice.
- **0016 item 4**, partly. The fixed 60-second practice becomes one duration among reviewed options (1/3/5/10 min per practice). The 60-second initiation sitting stays as the first-challenge entry.
- **0014 Sadhana placeholder content** inside the two-destination shell. The shell itself stays.
- The "Steady" breathing player UI is replaced by the Sadhana player. `BreathingSessionOwner` process ownership (0012) is kept and extended.

## Still binding
- `research/deep-foundation/PRACTICE_SAFETY.md`: no breath pacing. Motion is ornamental, never linked to inhale or exhale, and static under reduced motion. Stationary confirmation, End always reachable (top right, large text, TalkBack), equal external-attention anchor.
- No medical, diagnostic or energy-detector claims. No chakra health, blockage, energy level or kundalini movement. Centres are not attached to nerves or glands.
- `TANTRA_PRACTICE_MAP.md` exclusions; protection honesty rules; no streaks, scores or health claims.
- ST-010 and ST-011 naming rules (see question 1).

## Founder questions
1. **RESOLVED (founder, 12 Oct 2026): show tradition sources only inside deep study.** ST-010, ST-011, SOP-05, voice-direction and AGENTS are amended. The main app, website and marketing name no teacher or tradition. Deep study may name traditions, texts and passages. Spine waypoints outside deep study use plain labels (Base, Lower abdomen, Mid torso, Chest, Throat, Brow, Crown), and Sanskrit names and attribution appear only in deep study. Private influences stay internal. Original question: **ST-011 vs named maps:** chakra names are tradition-specific, and the prototype names "Vijñāna-bhairava-tantra 24", "Yoga / Buddhist / Vedānta" lanes, and tantric sources. HANDOFF requires every centre to cite a named system and passage. ST-011 forbids naming a teacher, lineage or tradition in public or app copy. Does citing a source text or tradition's map count as naming a tradition? Options: (a) sources inside the in-app study and citation layer only; (b) generic labels ("a seven-centre teaching map"); (c) an ST-011 amendment.
2. **Progression meaning:** HANDOFF acceptance gate 8 says the founder signs off on capability meaning and visual language.
3. **Centre-specific practice:** none until passages are verified. Who verifies, and is study-only acceptable for the pilot?
4. **Durations beyond 60 seconds** need a practice review before release.
5. **The "Choose" capability** links to a Routine boundary. Confirm that linkage with the initiation protection window.
6. **Sync:** /v1 Sadhana routes depend on DEV-25.

## Research-only (never shipped unattended)
Breath retention, forceful breathwork, fasting, sleep restriction, sensory deprivation, strong somatic work, kundalini-oriented induction, sexuality-related practice, altered-state techniques. These remain study records with status `research_only` or `qualified_context`. Reviewers: a qualified practice reviewer (teacher or clinician), a source/scholarly reviewer for passages, plus a language review (SOP-05) and an accessibility review. The founder approves release. Nothing ships without a complete `ContentReviewManifest`.

## Consequences
Another design source to keep in sync. Build waits on the founder questions above. The breathing pillar UI becomes legacy. Practice content beyond the 60-second foundation cannot ship until it is reviewed.

## Revisit when
The founder answers the questions, or a qualified reviewer rejects the centre model.

## Build notes (DEV-31 to DEV-33): edits made to the wireframe for honesty and the naming rule
- Spine waypoints, the readout and the practice source line outside deep study use plain labels and notes. Sanskrit names and named sources are listed only in Deep study. Two plain notes drop tradition words ("tantric and later yoga", "kuṇḍalinī").
- Player: the planet stays still, so "The orbit moves. Your breath does not follow it." becomes "The orbit stays still. Your breath does not follow anything."
- "I'll practise now · hide guidance" switches to minimal guidance and records nothing. The wireframe counted a practice that had not happened yet.
- Source "Inspect lane": the prototype boundary text becomes "The source reader is not available yet. No unreviewed text is shown."
- Home clock shows the real local time. Demo history fixtures are removed; the path comes from capability-criteria-v1.
- Additions: "Use fewer prompts" control on Path (HANDOFF requires it); a "Produced by" criteria label on the invitation; delete for self-reports; tabs hidden during an active practice (End is the exit); a Routine tab when hosted in the initiation shell; an optional reflection that can be cleared.

## Loading surface (re-sync 13 Oct 2026): founder question and licence flag
- **Founder question (open):** HANDOFF "Loading state contract" specifies a 5.5 s in/out zoom (1.015× to 1.065×, alternating) named after breathing. PRACTICE_SAFETY and DESIGN_SYSTEM forbid any visual that can pace breath, and a repeating in/out cycle near breathing tempo is a pacer. Built instead: one slow one-way zoom from 1.015× to 1.065× over 20 s, linear, played once and never reversed or repeated. Under reduced motion it is a static 1.03× crop. It is never named "breathing" in code or copy and is never shown while a practice is active. It appears only after a 400 ms threshold; a 12 s timeout gives a recoverable error (Try again / Continue without it). Does the founder accept this, or want a different non-cyclic motion?
- **Image licence: unverified.** `assets/shanivar-loading-kaleidoscope.jpg` is the founder-supplied fourth reference image. It carries no EXIF/IPTC copyright or creator metadata, and HANDOFF says ownership is unverified. It is bundled in debug only (`app/src/debug/res/drawable-nodpi/sadhana_loading_debug_only.jpg`). Release falls back to a plain black surface. Before public release, confirm the rights or replace it with an owned asset.
- **Re-sync note:** the update also brings a 33-segment articulated spine, a symbolic vagal layer with firefly pulses, a self-check, and revised voice copy. These are not built yet and need founder confirmation: a vagal layer and a self-check near health claims need SOP-05 review.
