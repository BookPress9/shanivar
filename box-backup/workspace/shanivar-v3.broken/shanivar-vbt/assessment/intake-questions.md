# Shanivar VBT — Intake Assessment (Level-1 Technique Router)

**Owner:** Personalization / Assessment Agent
**Status:** Draft v1 — built against placeholder technique-category taxonomy (see `routing-algorithm.md` §1). `shanivar-vbt/content/README.md` and all `technique-XX-*.md` files do not exist yet as of this writing, so category slugs below are provisional and will be reconciled by the Staff Manager once the Content Agent's final list lands.

## Design constraints
- 13 questions, all multiple-choice or Likert-style (no free text) — completable in 2–3 minutes.
- Every answer option carries a machine-readable tag. The router (see `routing-algorithm.md`) consumes tags only; it never parses prose.
- One question per screen recommended for the App/UI Agent's onboarding flow, but nothing here assumes a particular UI — this is the content/logic spec.
- Q4 is the single most important question (dominant thought pattern) — it carries the heaviest scoring weight. Everything else is a modifier or a filter.

---

## Q1 — Attention state
**Prompt:** "Right now, would you describe your mind as..."

| Option | Tag | Notes |
|---|---|---|
| A. Scattered — jumping between many things | `ATT_SCATTERED` | needs a concrete external anchor |
| B. Focused but tight / tense | `ATT_FOCUSED_TENSE` | focused capacity exists but constricted |
| C. Foggy — hard to tell / spaced out | `ATT_FOGGY` | low signal, needs grounding |
| D. Calm and clear already | `ATT_CLEAR` | ready for subtler work |

## Q2 — Stimulation level
**Prompt:** "Lately, do you feel more..."

| Option | Tag | Notes |
|---|---|---|
| A. Overstimulated — wired, too much input | `STIM_OVER` | needs de-loading / expansion |
| B. Understimulated — flat, craving something to happen | `STIM_UNDER` | needs a vivid/sensory anchor |
| C. Balanced — neither | `STIM_BALANCED` | neutral, no bias |

## Q3 — Embodiment
**Prompt:** "Do you feel more..."

| Option | Tag | Notes |
|---|---|---|
| A. In your head / disconnected from your body | `EMB_DISSOCIATED` | needs grounding into sensation |
| B. In your body / physically present | `EMB_EMBODIED` | ready for subtler/still work |
| C. Not sure | `EMB_UNSURE` | neutral |

## Q4 — Dominant thought pattern (PRIMARY — single select)
**Prompt:** "Which of these best describes your mind most days right now? Pick the single closest match."

| Option | Tag | Notes |
|---|---|---|
| A. Ruminative — replaying the past | `TP_RUMINATIVE` | heaviest-weight answer in the whole assessment |
| B. Anxious — spiraling about the future | `TP_ANXIOUS` | " |
| C. Emotionally reactive — triggered easily | `TP_REACTIVE` | " |
| D. Restless / impatient — can't sit with anything | `TP_RESTLESS` | " |
| E. Numb / flat — don't feel much | `TP_NUMB` | " |
| F. Racing — many threads at once, noisy mind | `TP_RACING` | " |

## Q5 — Secondary thought pattern (single select, same list as Q4)
**Prompt:** "And which is the next-closest? (Pick a different option than Q4, or 'none of the above apply further.')"

| Option | Tag |
|---|---|
| A–F. Same six options as Q4 | `TP2_RUMINATIVE` / `TP2_ANXIOUS` / `TP2_REACTIVE` / `TP2_RESTLESS` / `TP2_NUMB` / `TP2_RACING` |
| G. None further apply | `TP2_NONE` |

## Q6 — Physical stillness comfort
**Prompt:** "How do you feel about sitting physically still for the whole session?"

| Option | Tag |
|---|---|
| A. Comfortable, I can sit still easily | `STILL_HIGH` |
| B. A little restless but manageable | `STILL_MODERATE` |
| C. I need some movement or fidget room | `STILL_LOW` |

## Q7 — Eyes open or closed
**Prompt:** "Do you prefer eyes open (focused on something) or eyes closed (going inward) for a calming practice?"

| Option | Tag |
|---|---|
| A. Open, focused on something | `SENS_EYES_OPEN` |
| B. Closed, going inward | `SENS_EYES_CLOSED` |
| C. No preference | `SENS_NEUTRAL` |

## Q8 — Sound preference
**Prompt:** "How do you feel about sound (humming, chanting, ambient tone) as part of a practice?"

| Option | Tag |
|---|---|
| A. I find it soothing / helpful | `SND_SOOTHING` |
| B. I find it distracting / prefer silence | `SND_DISTRACTING` |
| C. No strong preference | `SND_NEUTRAL` |

## Q9 — Time available
**Prompt:** "How much time can you realistically give a session, most days?"

| Option | Tag |
|---|---|
| A. Under 5 minutes | `TIME_UNDER5` |
| B. 5–10 minutes | `TIME_5_10` |
| C. 10–20 minutes | `TIME_10_20` |
| D. 20+ minutes | `TIME_20PLUS` |

## Q10 — Practice environment
**Prompt:** "Where will you usually practice?"

| Option | Tag |
|---|---|
| A. Private and quiet | `ENV_PRIVATE` |
| B. Shared space, but I can get a few quiet minutes | `ENV_SEMI` |
| C. Noisy / public / on the move | `ENV_NOISY` |

## Q11 — Prior meditation experience
**Prompt:** "How would you describe your meditation experience?"

| Option | Tag |
|---|---|
| A. None / brand new | `EXP_NONE` |
| B. Some experience, casual practice | `EXP_SOME` |
| C. Experienced / regular practitioner | `EXP_EXPERIENCED` |

## Q12 — Current emotional intensity (gates the emotion-based category)
**Prompt:** "Right now, are you in the grip of a strong emotion (anger, grief, desire, fear) that feels present and current — not just a general mood?"

| Option | Tag |
|---|---|
| A. Yes, strongly, right now | `EMO_ACTIVE_STRONG` |
| B. Somewhat | `EMO_ACTIVE_MILD` |
| C. No, fairly neutral/calm currently | `EMO_NONE` |

## Q13 — Personal draw (soft tie-breaker)
**Prompt:** "If a couple of practices seemed equally good for you, which style would you personally be drawn to try?"

| Option | Tag |
|---|---|
| A. Something with the breath | `PREF_BREATH` |
| B. Sound / vibration / humming | `PREF_SOUND` |
| C. Looking at something (light, a point, an image) | `PREF_VISUAL` |
| D. Feeling sensations in the body | `PREF_BODY` |
| E. Watching my thoughts / the space between them | `PREF_GAP` |
| F. Imagining vastness / space / infinity | `PREF_SPATIAL` |
| G. Working directly with a strong emotion | `PREF_EMOTION` |

---

**Total: 13 questions.** All answers are captured as tags in an array (e.g. `["ATT_SCATTERED","STIM_OVER","EMB_DISSOCIATED","TP_ANXIOUS","TP2_RACING","STILL_MODERATE","SENS_EYES_CLOSED","SND_NEUTRAL","TIME_5_10","ENV_PRIVATE","EXP_NONE","EMO_NONE","PREF_BREATH"]`) and passed as-is to the scoring engine in `routing-algorithm.md`.
