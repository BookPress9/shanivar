# Code Style & Conventions — Shanivar

Observed conventions, not tooling-enforced. No formatter, linter (other than Android lint) or type checker config is in the repo; there is no CI (DEV-15). Reuse before inventing (`AGENTS.md` build principle).

## 1. General Principles
Prefer readable code; reuse existing components and utilities; prefer consistency; make focused changes; preserve tested code rather than delete it (`android-app/app/build.gradle.kts:17-22` keeps `LegacyTabShell` behind a flag). New entry points must make an explicit choice rather than inherit a default (`BreathingSessionOwner.kt:26`: exhaustive `when` "on purpose").

## 2. Naming
- **Kotlin:** types and composables UpperCamel; functions/vals lowerCamel; constants `UPPER_SNAKE` `const val` (`MIN_PRACTISED_SECONDS`, `SADHANA_DEFAULT_SECONDS`); one flat package `com.shanivar.nativeapp`; files named for their main type.
- **Backend JS:** camelCase variables, kebab/plain file names per domain in `routes/`; DB columns snake_case.
- **API routes:** `/api/<domain>` (`backend/src/index.js:73-91`). UNRESOLVED: response/error envelope not documented.
- **Practice IDs:** snake_case strings (`natural_anchor`, `body_scan_sitting`).
- **Docs:** decisions `NNNN-short-title.md`, never renumbered (`docs/decisions/README.md:7`). Dates ISO in file names for handoffs.

## 3. Folder Structure
See `ARCHITECTURE.md` §7. Generated or copied snapshots live under `marketing/.../source/` and the originals win (`routine-v3-onboarding/README.md:25`).

## 4. Imports
Kotlin: explicit imports, no wildcard observed in files read. Backend: CommonJS `require`. UNRESOLVED: import ordering rule.

## 5-6. Components and functions
Single responsibility; pure logic separated from UI where possible (`web-app/logic.js` is pure and tested; Android `BreathingSessionState` is a pure state machine). Give hosts a type, not a gateway, when a promise must be a fact about types (`BreathingScreen.kt:20-24`).

## 7. Error Handling
Use: result types or validation problem lists (`BreathingCatalog` returns problems, `:112-119`); fire-and-forget for telemetry; fail loudly for corrupt legacy state (`Needs attention`, test guide `:16`). Do not: block safety actions on network or let a UI promise outrun engine state.

## 8. Comments
Explain why and cite the decision or file (`SankalpStore.kt:6-15`). Mark internal-test and unreviewed content in code and UI (`InitiationSitting.kt:12-24`).

## 9. Logging
Backend uses `console.log/error`. Never log secrets or personal text (`SECURITY.md` §7). UNRESOLVED: logging library.

## 10. Formatting
Formatter: none. Linter: Android lint (`lintDebug`). Type checking: Kotlin compiler; none for JS. UNRESOLVED: adopt ktlint/eslint? Needs founder go-ahead (new dependencies).

## 11. Do
Read the relevant decision and test guide first; keep copy strings in the places the copy inventory expects (`marketing/.../source/ui-copy-inventory.md`); keep non-gamified, factual wording; mark drafts DRAFT.

## 12. Don't
Commit `ROUTINE_V3 = true` or any temporary flag; add dependencies casually; introduce streaks, counts of days, scores or levels (0009, ST-005); add health claims; use Device Admin/Accessibility (0002); change unrelated files; commit secrets; push without the founder saying push (SOP-08).
