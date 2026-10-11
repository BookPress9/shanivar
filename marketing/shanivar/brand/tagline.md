# Shanivar tagline

**Tagline:** Discipline is freedom.
**Lockup:** Shanivar — Discipline is freedom. (em dash, as in the site titles)
**Date:** 2026-10-11
**Source:** founder-given (Shubham Kumar). **Founder-given, not yet an accepted stance entry** in `SHANIVAR_STANCE.md`.

## Where it now appears
Paths relative to repo root; site = `marketing/shanivar/routine-v3-onboarding/website`.
- `site/index.html:6` title ("Shanivar — Discipline is freedom. Android app in development"), `:14` og:title, `:20` og:image:alt, `:56` hero H1 (eyebrow at `:55` now "Shanivar · Android app in development", keeping the page pre-launch)
- `site/404.html:21`, `site/about.html:20`, `site/privacy.html:20`, `site/thanks.html:22` (og:image:alt)
- `android-app/app/src/main/java/com/shanivar/nativeapp/RoutineOnboardingFlow.kt:274` (welcome eyebrow, "DISCIPLINE IS FREEDOM"); mirrored in `marketing/shanivar/routine-v3-onboarding/source/RoutineOnboardingFlow.kt:274`
- `source/ui-copy-inventory.md:22,266`, `site/README.md:100`
- `routine-v3-onboarding/social-presence-from-scratch.md:68` (bio Option A), `routine-onboarding-marketing-plan.md:85,87,92`, `instagram/first-9-posts.md:30,46`
- `ai_org/agents/instagram_keeper.md:60` (tagline row added; bio row untouched)

## Intentionally not changed
- Live Instagram bio ("Life been drifting? Take the day back…"). To-do for Darpan, only on Shubham's go-ahead. The bio text is also recorded in `instagram_keeper.md:59`, `first-9-posts.md:7` and Option B in `social-presence-from-scratch.md`.
- `site/og-image.png` (binary): its baked-in text probably still shows the old line. Regenerate.
- `SHANIVAR_STANCE.md` and `docs/decisions/*`.
- "Build the day you intend to live." and "Stop leaving your life to chance." stay as Routine feature copy (Kotlin :275/:278, site description), not taglines.
- Historic wireframes in `android-app/wireframes/` (`shanivar-v3-wireframe/shanivar-v3/app.js:181-182` and its twin under `shanivar-v3/`, still carry the old copy; `rhythm-v2`, `saturn-dark*`, `steady`). They are reference snapshots.
- `web-app/` (Sadhana prototype; no tagline there), `voice-direction.md`, `wish-list.md` (no tagline lines found).
- `site/thanks.html:16` og:title wrongly says "Page not found · Shanivar" (pre-existing bug, not fixed here).

## Stance check
- **ST-003 / ST-004 / ST-005** (practitioner can disagree; embodiment optional; depth chosen): no direct conflict, as they cover practice, not branding. The tension is indirect: "Discipline is freedom" is an absolute that can read as one right way for everyone. Do not use it to imply that disagreeing, skipping or going lighter is failure.
- **ST-006** (discipline reversible, participant-owned) is the closest entry and is still PROPOSED. The tagline fits only if discipline stays chosen and reversible.
- **ST-009** (founder principle 1 is "Discipline", accepted 2026-10-06): consistent. The tagline extends it with "freedom", which is new wording.
- **Decision 0014** (the repo copy, `0014-routine-sadhana-shell-and-routine-v3.md`) has no text on "voluntary", "stoppable", "unbreakable" or "shame" (grep found none), so I could not verify those rules against it. Treated here as the user's description: voluntary, stoppable, never "unbreakable" language. The tagline is compatible only if it is read as a promise of choice, not of lock-in.
- **No-shame rule:** "Discipline is freedom" can be turned into a verdict ("if you're not free, you lack discipline"). Keep it out of copy that talks to someone who has just missed a day.
- **Candid tension:** "discipline" as control and a strict stance sits against the stance line "notice the mind, don't control it". The founder's earlier "very strict" positioning (6 Oct) raises this too. "Freedom" can resolve it (discipline as a structure you chose and can leave) or deepen it (freedom through control).

**Suggested guardrails**
1. Never pair it with "unbreakable", "no excuses", "streak" or "no days off" language.
2. Always near a visible stop or pause, and only claim what the app does (app protection is opt-in; the app is in development).
3. Use the tagline as the brand line, not as a claim about the person's life.
4. Let the founder decide whether to record it as a stance entry with a "freedom = chosen structure" gloss.
5. Don't attribute it to any tradition.
