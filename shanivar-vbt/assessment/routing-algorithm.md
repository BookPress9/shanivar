# Shanivar VBT — Routing Algorithm

**Superseded for the breathing MVP (2026-09-25):** use the deterministic rules in the [Claude handoff](../../android-app/CLAUDE_VBT_BREATHING_HANDOFF_2026-09-25.md). The historical algorithm below is retained for context; do not use its blanket breath defaults or placeholder taxonomy for the new eight-practice feature.

**Owner:** Personalization / Assessment Agent
**Status:** Draft v1, deterministic (no ML/black-box scoring). Built against a placeholder technique-category taxonomy — see §1 for the reconciliation note.

---

## 0. Reconciliation status

Checked at time of writing: `shanivar-vbt/content/` is empty — no `README.md`, no `technique-XX-*.md` files exist yet. This document therefore routes to a **technique category**, not yet a specific technique slug. Once the Content Agent lands its ~12 techniques with `Category tags` and `Suggested Duration` front matter, the Staff Manager (or this agent, re-invoked) must:

1. Map each of the Content Agent's real category tags onto the 7 categories below (they may use different names — reconcile by meaning, not string match).
2. Fill in the "within-category tiebreak" step (§5) using the real `Suggested Duration` values instead of the placeholder assumption that each category has a fast (<10 min) and slow (10+ min) variant.
3. Replace every `TBD-<code>` slug in `routing-table.md` with the real filename/slug.

Nothing else in this algorithm should need to change — the scoring runs entirely on the 7-category taxonomy, which is independent of exactly which technique fills each slot.

---

## 1. Taxonomy assumptions (placeholder — pending Content Agent reconciliation)

Seven categories, chosen to span the major dharana-groupings of the Vigyan Bhairava Tantra (breath/prana, sound/nada, gazing/drishti, body-center/sparsha, the gap between thoughts, spatial/void expansion, and emotion/mood as gateway):

| Code | Category | VBT grounding (indicative) |
|---|---|---|
| `BR` | Breath-based | breath awareness, the two gaps (after exhale/inhale), breath retention |
| `SO` | Sound-based | nada, humming/resonance, mantra as vibration, ambient sound absorption |
| `VI` | Visual-gazing | trataka/fixed-point gazing, formless staring, inner light visualization |
| `BS` | Body-sensation | body-scan, touch-point/center awareness, chakra-adjacent sensation focus |
| `GT` | Gap-between-thoughts | witnessing the gap between two thoughts or two breaths; subtler attention work |
| `SA` | Spatial-awareness | imagining vastness/infinite space, boundary dissolution, directionless awareness |
| `EM` | Emotion-based | using a currently-active strong emotion (desire, anger, fear) as the object of awareness |

If the Content Agent's final tags collapse or split these differently (e.g. splits `BR` into "breath-retention" vs "breath-observation", or merges `SA`/`GT`), the scoring logic still applies per-category; only the mapping table in §1 and the slugs in `routing-table.md` need updating.

---

## 2. Scoring model

Each of the 7 categories starts at score `0`. Every intake answer (from `intake-questions.md`) contributes a signed point delta to one or more categories. Final routing = the category with the highest score, subject to the gate in §3 and the tie-break in §4.

### 2.1 Consciousness-state modifiers (Q1–Q3)

| Tag | BR | SO | VI | BS | GT | SA | EM |
|---|---|---|---|---|---|---|---|
| `ATT_SCATTERED` | +2 | +2 | +1 | 0 | 0 | 0 | 0 |
| `ATT_FOCUSED_TENSE` | +1 | 0 | 0 | 0 | +1 | +2 | 0 |
| `ATT_FOGGY` | 0 | +1 | 0 | +2 | 0 | 0 | 0 |
| `ATT_CLEAR` | 0 | 0 | 0 | 0 | +2 | +1 | 0 |
| `STIM_OVER` | +1 | 0 | 0 | 0 | +1 | +2 | 0 |
| `STIM_UNDER` | 0 | +2 | +1 | +2 | 0 | 0 | 0 |
| `STIM_BALANCED` | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `EMB_DISSOCIATED` | +1 | 0 | 0 | +2 | 0 | 0 | 0 |
| `EMB_EMBODIED` | 0 | 0 | 0 | 0 | +1 | +1 | 0 |
| `EMB_UNSURE` | 0 | 0 | 0 | 0 | 0 | 0 | 0 |

### 2.2 Dominant thought pattern (Q4 primary, Q5 secondary) — heaviest weight

| Tag (primary, Q4) | BR | SO | VI | BS | GT | SA | EM |
|---|---|---|---|---|---|---|---|
| `TP_RUMINATIVE` | +3 | 0 | 0 | 0 | +2 | 0 | 0 |
| `TP_ANXIOUS` | +3 | 0 | 0 | 0 | 0 | +2 | 0 |
| `TP_REACTIVE` | 0 | 0 | 0 | +1 | 0 | 0 | +3 |
| `TP_RESTLESS` | +1 | 0 | 0 | +3 | 0 | 0 | 0 |
| `TP_NUMB` | 0 | +3 | 0 | +1 | 0 | 0 | 0 |
| `TP_RACING` | +2 | +3 | 0 | 0 | 0 | 0 | 0 |

Secondary (Q5, tags `TP2_*`) apply the **same category mapping as Q4** but at half weight, rounded down, minimum `+1` per affected category (e.g. `TP2_RUMINATIVE` → `+1 BR, +1 GT`). `TP2_NONE` contributes nothing.

### 2.3 Lifestyle filters (Q6–Q11) — mostly modifiers, mostly soft

| Tag | BR | SO | VI | BS | GT | SA | EM |
|---|---|---|---|---|---|---|---|
| `STILL_HIGH` | 0 | 0 | +1 | 0 | +1 | +1 | 0 |
| `STILL_MODERATE` | +1 | 0 | 0 | 0 | 0 | 0 | 0 |
| `STILL_LOW` | +1 | 0 | -2 | +2 | -2 | -1 | 0 |
| `SENS_EYES_OPEN` | 0 | 0 | +2 | 0 | 0 | 0 | 0 |
| `SENS_EYES_CLOSED` | 0 | 0 | 0 | 0 | +1 | +1 | +1 |
| `SENS_NEUTRAL` | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `SND_SOOTHING` | 0 | +2 | 0 | 0 | 0 | 0 | 0 |
| `SND_DISTRACTING` | +1 | -2 | 0 | 0 | 0 | 0 | 0 |
| `SND_NEUTRAL` | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `TIME_UNDER5` | +1 | 0 | 0 | +1 | -2 | -2 | 0 |
| `TIME_5_10` | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `TIME_10_20` | 0 | 0 | 0 | 0 | +1 | +1 | 0 |
| `TIME_20PLUS` | 0 | 0 | 0 | 0 | +2 | +2 | 0 |
| `ENV_PRIVATE` | 0 | +1 | 0 | 0 | +1 | 0 | +1 |
| `ENV_SEMI` | 0 | -1 | 0 | 0 | -1 | 0 | -1 |
| `ENV_NOISY` | +1 | -2 | -1 | +1 | -2 | 0 | -2 |
| `EXP_NONE` | +2 | 0 | 0 | +2 | -2 | -1 | 0 |
| `EXP_SOME` | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| `EXP_EXPERIENCED` | 0 | 0 | 0 | 0 | +2 | +2 | 0 |

### 2.4 Personal draw / soft tie-breaker (Q13)

Each `PREF_*` tag adds a flat `+1` to its matching category only (`PREF_BREATH`→BR, `PREF_SOUND`→SO, `PREF_VISUAL`→VI, `PREF_BODY`→BS, `PREF_GAP`→GT, `PREF_SPATIAL`→SA, `PREF_EMOTION`→EM). This is deliberately small — it should only matter when it breaks a near-tie, not override the state/pattern signal.

---

## 3. Hard gate: emotion-based category requires live raw material

`EM` is not a general-purpose category — Vigyan Bhairava Tantra's emotion-based dharanas require an actually-present strong emotion to work with; without one there's nothing to direct awareness onto.

**Rule:** After scoring, check Q12 (`EMO_*`):
- If the user answered `EMO_ACTIVE_STRONG`: add `+4` to `EM`.
- If `EMO_ACTIVE_MILD`: add `+2` to `EM`.
- If `EMO_NONE`: **remove `EM` from the candidate set entirely** for this routing run, regardless of any score it accumulated from Q4/Q5 (`TP_REACTIVE`/`TP2_REACTIVE`). A user can score high on "emotionally reactive as a general pattern" (Q4) while reporting no current active emotion (Q12) — in that case route them to `BS` or `BR` instead (reactive-pattern's secondary weight), since there's no live emotion to use as the object right now.

This is the one true hard gate in the system; everything else is soft scoring.

---

## 4. Selecting the winner

```
scores = { BR:0, SO:0, VI:0, BS:0, GT:0, SA:0, EM:0 }
apply all tag deltas from §2.1–2.4 to `scores`
if Q12 == EMO_NONE: remove EM from `scores` (exclude from argmax)

max_score = max(scores.values())
candidates = [cat for cat, s in scores.items() if s >= max_score - 1]   # epsilon = 1 point counts as a tie
```

`epsilon = 1` is intentional: the point values above are coarse (deltas of 1–4), so a 1-point gap is not a meaningfully different signal and should still be treated as a tie to route through the tie-break cascade rather than an arbitrary winner-by-one-point.

### If `candidates` has exactly one category
Route there. Done.

### If `candidates` has 2+ categories — tie-break cascade (apply in order, stop at first that resolves to one)

1. **Prefer the category matching the Q4 primary thought pattern's mapped category** (from §2.2) if it's among `candidates` — the dominant thought pattern is the single strongest signal of what actually needs addressing.
2. **Else prefer the category matching the Q13 personal-draw answer** if it's among `candidates` — when the state signal is genuinely ambiguous, user preference is a legitimate and motivating tiebreaker (more likely to be practiced).
3. **Else prefer whichever tied category has the highest score computed from §2.3 (lifestyle) tags *alone*** — i.e., break the tie on practical fit (time/environment/experience) rather than psychological state.
4. **Else fall back to a fixed priority order:** `BR > BS > SO > VI > SA > GT > EM` (breath-based first: it is the most universally deliverable technique with the fewest failure modes across time/environment/experience — the safest default when truly undecidable).

### Fully ambiguous profile (no signal at all)
If every answer was a neutral option (`STIM_BALANCED`, `EMB_UNSURE`, `SENS_NEUTRAL`, `SND_NEUTRAL`, `TIME_5_10`, `EXP_SOME`, `TP2_NONE`, no strong `PREF_*` distinguishing signal) such that `max_score <= 1`, **do not run the tie-break cascade** — route directly to the **default fallback: `BR` (breath-based), shortest/most beginner-friendly technique in that category**. Rationale: when there is no usable signal, the safest, most universally applicable, easiest-to-follow technique is the right default rather than an arbitrary tie-break winner. This is also the recommended "Technique 1 of Level 1" for any Day-1 cold start before the assessment is even taken, if the App/UI Agent wants a pre-assessment default.

---

## 5. Within-category tiebreak (once Content Agent's list lands)

If a category contains more than one technique (with ~12 techniques across 7 categories, at least a couple of categories will), break the tie using, in order:
1. **Time fit**: match the technique's `Suggested Duration` to the user's Q9 answer (closest fit, prefer shorter over longer if equidistant — under-promising session length drives completion).
2. **Experience fit**: if the technique's source content notes a beginner/advanced lean (check Content Agent's front matter once available), match to Q11 (`EXP_NONE` → beginner-flagged technique; `EXP_EXPERIENCED` → the more advanced one).
3. **Environment fit**: if one variant needs full privacy/silence and another tolerates a shared/noisy space, match to Q10.
4. If still tied, pick the technique with the lower `technique-XX` number (earliest in Content Agent's canonical ordering) for determinism.

This step is currently unimplementable in full since no technique content exists yet — documented here so the Staff Manager can close it out in one pass once `shanivar-vbt/content/` is populated.

---

## 6. Edge cases summary

| Edge case | Handling |
|---|---|
| Two or more categories score within 1 point of the max | Tie-break cascade, §4 |
| No usable signal at all (near-all-neutral answers) | Fallback default: `BR`, shortest beginner technique |
| User reports general emotional reactivity (Q4) but no live emotion (Q12) | `EM` excluded from candidates; falls through to next-best category from the reactive-pattern's secondary weight (`BS`) or the general profile |
| Contradictory answers (e.g. "restless, can't sit still" + "comfortable sitting still") | No special handling needed — both contribute their normal deltas; the additive model naturally nets out partial/mixed signals rather than erroring |
| A category has multiple candidate techniques post-Content-Agent-landing | §5 within-category tiebreak |
| User skips Q5 (no clear secondary pattern) | `TP2_NONE` — contributes 0, does not distort scoring |
