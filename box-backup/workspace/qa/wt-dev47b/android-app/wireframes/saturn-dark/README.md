# Shanivar confirmed v1 wireframes

Open [the interactive design](index.html) or [the complete screen board](exports-v1/index.html). The active design follows alarms + focus modes, with a local pause, optional review, helper handback and changeable Shani framing.

## Backup

The previous wireframe is preserved intact in [saturn-dark-backup-20260913-221107](../saturn-dark-backup-20260913-221107/index.html). All 75 source/export files were copied and SHA-256 compared before redesign. Its bilingual broad-product screens and capture script remain available there.

## Review

Click the left screen rail to inspect states, or walk through actions inside the phone. At narrow browser widths the rail scrolls horizontally. Forms, activity presets, app selection, presentation choice, alarm time/days/action, simulated permissions and dismissal are interactive. Use Reset walkthrough for a fresh state.

The prototype includes 17 review states across arrival, everyday home, focus, alarm, recent choices and recovery. These are an inspection inventory, not 17 mandatory onboarding steps. Use either tool independently.

The approved behavioral update puts a plain commitment at focus and alarm setup, connects the block and pause screens to the chosen activity, offers a two-question review after a completed or deliberately ended focus session, and shows recent choices without streaks or rankings. The review is skippable. The prototype keeps a local, in-memory activity history for inspection only; it resets on reload.

English copy is the active review language. Hindi translation and comprehension verification remain a delivery gate; earlier backup translations are not coverage for redesigned screens. See [design notes](V1_DESIGN_NOTES.md) for simulation boundaries.

## Files and exports

- `index.html`, `v1.css`, `v1.js`: current self-contained prototype; no external font dependency.
- `capture-v1.mjs`: interaction checks and exports.
- `exports-v1/`: current phone frames, desktop/mobile workspace captures and clickable screen board.
- Legacy `styles.css`, `prototype.js`, `capture.mjs`, `exports/` and `exports-v2/` are historical assets; the current entry does not use them. Use the backup to run the old capture script.

From repository root:

```powershell
node android-app/wireframes/saturn-dark/capture-v1.mjs
```

This uses the existing frontend Playwright installation. It verifies the guest pause, focus setup, alarm setup/non-walking dismissal, alarm/focus coexistence, screen rendering and mobile overflow. It does not test Android enforcement.

Read [confirmed scope](../../INITIAL_MVP_SCOPE_2026-09-13.md), [Claude implementation task](../../CLAUDE_MVP_STREAMLINE_TASK_2026-09-13.md) and [marketing copy](../../../marketing/shanivar/v1-product-direction-handoff.md). Wireframes do not establish that the native v1 has shipped.
