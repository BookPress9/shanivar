Status: founder-confirmed (Oct 17, 2026) — ready to build.

**Epic:** DEV-59 (D — Sadhana and awareness) / DEV-60 (E)

**User problem:** Shanivar must never pace or time the breath (safety rule). DEV-39 cleaned only `techniques.json`.

**Found:** the app still has `Breathing*` files (Catalog, Schedule, Screen, Session) and a "breathe" action on the block screen.

## Scope

Audit every Breathing\* file, the block-screen action, assets, strings and any legacy Render content. Remove or neutralise any timed inhale/hold/exhale, counts, pacing animation or physiological claim. Extend `TechniqueContentSafetyTest` (or add an equivalent) to cover these surfaces.

## Acceptance criteria

1. No reachable or bundled UI, animation or asset paces or times breathing.
2. No breathing-related physiological, medical or energy claims.
3. The block-screen option is renamed or re-scoped as a non-paced pause, if kept.
4. Automated safety test covers these files; it fails on the old content.
5. Tester verifies point by point.

**Owner:** Shanivar Frontend (QA: Shanivar Tester). **Priority:** Highest.
**Out of scope:** new features.