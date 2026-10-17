# SOP-04 — Practice or content change and safety review

**Purpose:** No practice reaches users without the review `research/deep-foundation/PRACTICE_SAFETY.md` requires.
**Owner:** Founder accepts; a **qualified human reviewer** reviews each exact script (`PRACTICE_SAFETY.md:41`). UNRESOLVED: who is the qualified reviewer?

## Preconditions
- The practice category is allowed: ordinary external attention and natural breath observation are candidates; retention, hyperventilation, fasting, sleep restriction and extreme concentration are research-only (`PRACTICE_SAFETY.md:11-28`).
- Read decisions 0011, 0012, 0013, ST-004. Work inside the existing catalogue process (`shanivar-vbt/content/breathing-mvp-v1.json` → `android-app/app/src/main/assets/breathing-v1.json`); do not invent a parallel path (`AGENTS.md` build principle).
- No timed breath "recipes": the catalogue validator refuses prescribed breath phases (`BreathingCatalog.kt:15-18`).

## Steps
1. Draft the script under `shanivar-vbt/content/drafts/`, name it `DRAFT-<name>-NOT-REVIEWED-...md`, and set `status` to a draft value (`draft_internal` in `InitiationSitting.kt`; seed content is `implementation_seed`).
2. Source review: what the tradition prescribes, what is omitted, what Shanivar added; disclose omitted retention (`PRACTICE_SAFETY.md:40`). Cite sources; never paste long source passages (`docs/sources/README.md`).
3. Interaction review: End/Skip visible, accessible alternative, sound consent, cannot dismiss an alarm or release focus (`:42`).
4. Content check: no instruction to suppress breath, force a sensation, lose sleep, earn status; no effect claims (`:43`; `InitiationSitting.kt:12-24`).
5. Numbers: if durations or thresholds change, update `docs/sources/SADHANA_CALCULATIONS.md` and the founder records the decision.
6. Practice review by the qualified human; file the result next to the script. Only then may status change from draft.
7. Pilot: optional "anything uncomfortable?" question, minimal data (`:44`). Adverse feedback: stop that recommendation, review, inform owner (`:45`).
8. Keep the internal-test label in every surface (Android, web-app) until step 6 is recorded.

## Approval gates
Founder: new practices, any change to PRACTICE_SAFETY, public release of any practice. Open item today: body-scan sitting, OPEN, not reviewed (`PRACTICE_SAFETY.md:55`), including the optional "I allow" exhale line ruling.

## Rollback
Revert the content change (git revert, local); set the catalogue entry back to its prior version; remove the label only forward, never silently. Withdraw any public mention.

## Evidence / links
`PRACTICE_SAFETY.md`, `SHANIVAR_STANCE.md` ST-004, `docs/decisions/0011`, `VBT_BREATHING_TEST_GUIDE_2026-09-25.md`.
