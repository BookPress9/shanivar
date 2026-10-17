# Shanivar VBT — Routing Table

**Status:** Reconciled against the Content Agent's final 12 techniques (`shanivar-vbt/content/README.md`). The Content Agent independently used the exact same 7-code taxonomy, so no category remapping was needed — placeholders below are now real filenames. Reconciled by the Staff Manager per the procedure in `routing-algorithm.md` §0.

## 1. Category → real technique slug

| Category code | Category name | Technique slug(s) | Count |
|---|---|---|---|
| `BR` | Breath-based | `technique-01-two-pauses-of-breath` (3–5 min), `technique-02-tranquil-retention` (4–6 min) | 2 |
| `SO` | Sound-based | `technique-03-silence-after-the-hum` (3–4 min), `technique-04-the-fading-tone` (4–6 min) | 2 |
| `VI` | Visual-gazing | `technique-05-sky-gazing` (3–5 min), `technique-06-dappled-light` (3–5 min) | 2 |
| `BS` | Body-sensation | `technique-07-the-gentle-swing` (3–5 min, **citation flagged for QA**), `technique-08-savoring-fully` (3–4 min) | 2 |
| `GT` | Gap-between-thoughts | `technique-09-the-gap-between-two` (4–6 min, **verse number flagged for QA**), `technique-10-threshold-of-sleep` (3–5 min, **verse number flagged for QA**) | 2 |
| `SA` | Spatial-awareness | `technique-11-open-horizon` (3–5 min) | 1 — no tiebreak needed |
| `EM` | Emotion-based | `technique-12-steady-in-the-storm` (3–4 min) | 1 — no tiebreak needed |

No category came back empty, so the "remove category from candidate set" fallback in `routing-algorithm.md` §0 step 5 is not needed.

### Within-category tiebreak (resolves `routing-algorithm.md` §5)

All Level-1 durations cluster tightly (3–6 min), so no technique had an explicit beginner/advanced or environment lean documented in its front matter — the tiebreak reduces to time-fit only:

| Category | If `TIME_UNDER5` or `TIME_5_10` | If `TIME_10_20` or `TIME_20PLUS` |
|---|---|---|
| `BR` | `technique-01-two-pauses-of-breath` (shorter) | `technique-02-tranquil-retention` (longer) |
| `SO` | `technique-03-silence-after-the-hum` (shorter) | `technique-04-the-fading-tone` (longer) |
| `VI` | `technique-05-sky-gazing` (lower number, equal duration) | `technique-05-sky-gazing` (equal duration, no reason to switch) |
| `BS` | `technique-08-savoring-fully` (shorter) | `technique-07-the-gentle-swing` (longer) — **note:** this is the citation-flagged technique; QA must clear it before it can be a live routing target for `EXP_EXPERIENCED`/longer-session users |
| `GT` | `technique-10-threshold-of-sleep` (shorter) | `technique-09-the-gap-between-two` (longer) — both are verse-number-flagged; QA must resolve before ship |

## 2. Representative answer-pattern → category (worked examples)

These are illustrative archetypes to sanity-check the algorithm in `routing-algorithm.md`, not an exhaustive enumeration (13 tag-slots with 3–7 options each = thousands of raw combinations; the scoring model is what generalizes, not a lookup table).

| # | Archetype | Key answers | Winning category | Why |
|---|---|---|---|---|
| 1 | Anxious professional, no time, private | `TP_ANXIOUS`, `TIME_UNDER5`, `ENV_PRIVATE`, `EXP_NONE`, `STILL_MODERATE` | `BR` | Q4 anxious primary gives BR +3 (vs SA +2); `TIME_UNDER5` further boosts BR relative to SA (SA gets -2 under 5 min); beginner also favors BR. Clear breath-based winner. |
| 2 | Ruminative, experienced, plenty of time, private | `TP_RUMINATIVE`, `EXP_EXPERIENCED`, `TIME_20PLUS`, `ENV_PRIVATE`, `STILL_HIGH` | `GT` | Q4 ruminative gives BR +3 / GT +2, but `EXP_EXPERIENCED` (+2 GT), `TIME_20PLUS` (+2 GT), `STILL_HIGH` (+1 GT) push GT past BR. Rumination is fundamentally a "stuck in one thought" problem, and an experienced practitioner with time is ready for the subtler gap-work rather than just a breath anchor. |
| 3 | Numb/flat, understimulated, likes sound | `TP_NUMB`, `STIM_UNDER`, `SND_SOOTHING`, `TIME_5_10`, `ENV_SEMI` | `SO` | TP_NUMB gives SO +3, STIM_UNDER +2, SND_SOOTHING +2 — all stacking on SO. Clear winner; the flat/numb state needs a vivid external stimulus (vibration/resonance), not more silence. |
| 4 | Restless, needs movement, no time | `TP_RESTLESS`, `STILL_LOW`, `TIME_UNDER5`, `EXP_NONE` | `BS` | TP_RESTLESS gives BS +3, STILL_LOW gives BS +2 (and penalizes GT/VI/SA), EXP_NONE gives BS +2. Body-sensation is the only category that stays positive and dominant under "can't sit still." |
| 5 | Emotionally reactive, currently activated, private | `TP_REACTIVE`, `EMO_ACTIVE_STRONG`, `ENV_PRIVATE`, `SENS_EYES_CLOSED` | `EM` | TP_REACTIVE gives EM +3, EMO_ACTIVE_STRONG adds +4 (gate open), ENV_PRIVATE +1, eyes-closed +1. Strong, ungated EM win — this is the one case the technique should meet the user in the actual emotional moment. |
| 6 | Emotionally reactive pattern, but calm right now | `TP_REACTIVE`, `EMO_NONE`, `STILL_MODERATE` | `BS` | TP_REACTIVE alone would suggest EM, but `EMO_NONE` **excludes EM from candidates** (§3 hard gate — no live emotion to work with). Falls to reactive pattern's secondary weight, `BS` (+1 from Q4), which becomes the winner by elimination. Demonstrates the hard gate. |
| 7 | Overstimulated, tense, wants expansion, has time | `ATT_FOCUSED_TENSE`, `STIM_OVER`, `TIME_20PLUS`, `SENS_EYES_CLOSED`, `EXP_SOME` | `SA` | ATT_FOCUSED_TENSE (+2 SA), STIM_OVER (+2 SA), TIME_20PLUS (+2 SA), eyes-closed (+1 SA) all compound on SA. Overstimulation calls for spacious/expansive work, not another point of focus. |
| 8 | Racing mind, likes sound, moderate time | `TP_RACING`, `SND_SOOTHING`, `TIME_5_10`, `ENV_PRIVATE` | `SO` | TP_RACING gives SO +3 / BR +2; SND_SOOTHING adds +2 to SO, tipping it over BR. If this user had instead answered `SND_DISTRACTING`, BR would win instead (SO would drop, BR would gain) — a good example of a lifestyle/preference answer flipping a near-tie. |
| 9 | Dissociated, foggy, brand new, noisy environment | `EMB_DISSOCIATED`, `ATT_FOGGY`, `EXP_NONE`, `ENV_NOISY`, `STILL_LOW` | `BS` | EMB_DISSOCIATED (+2 BS), ATT_FOGGY (+2 BS), EXP_NONE (+2 BS), ENV_NOISY (+1 BS), STILL_LOW (+2 BS) — BS dominates from nearly every angle. Grounding-into-sensation is the correct fit for a dissociated, brand-new, on-the-move user. |
| 10 | Genuinely ambiguous / all-neutral profile | `STIM_BALANCED`, `EMB_UNSURE`, `SENS_NEUTRAL`, `SND_NEUTRAL`, `TIME_5_10`, `EXP_SOME`, `TP2_NONE`, no distinguishing `PREF_*` | `BR` (fallback default) | `max_score <= 1` triggers the fallback rule directly (§4, "fully ambiguous profile") rather than the tie-break cascade — routes to breath-based as the safest universal default. |
| 11 | Tie between two well-matched categories, resolved by preference | Scores end `BR: 5, GT: 5` (e.g. ruminative pattern, moderate time, mixed stillness) | Resolved by cascade | Step 1 of tie-break checks Q4's mapped category first — if Q4 was `TP_RUMINATIVE`, both BR and GT are "primary-mapped," so step 1 doesn't resolve it (both qualify). Falls to step 2: Q13 `PREF_*` breaks it — e.g. `PREF_GAP` → GT wins; `PREF_BREATH` → BR wins. |

## 3. Reconciliation — done

1. ✅ Read `shanivar-vbt/content/README.md` — Content Agent used the identical `BR/SO/VI/BS/GT/SA/EM` vocabulary, so no tag remapping was required (no finer-grained splits to reconcile).
2. ✅ Replaced every `TBD-<code>` in §1 above with the real filename slug(s).
3. ✅ Spot-checked the 11 worked examples in §2 — all still land on a category with at least one real technique; none point at an empty category.
4. ✅ No category came back empty — the "remove from candidate set" fallback was not triggered.
5. **Outstanding, not blocking MVP routing logic but blocking ship:** 3 of 12 techniques are QA-flagged (`technique-07`, citation; `technique-09` and `technique-10`, verse-number cross-edition variance — see `shanivar-vbt/content/README.md` Summary). QA/Scriptural Integrity Agent must clear these before the app can route real users to them.
