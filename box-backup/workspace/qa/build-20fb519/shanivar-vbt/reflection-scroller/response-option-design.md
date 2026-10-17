# Response-Option Design — Messages from Shanivar

Guidance for designing the quick-response options offered on each card. Ties directly to the API contract in [`android-app/CLAUDE_REFLECTION_SCROLLER_HANDOFF.md`](../../android-app/CLAUDE_REFLECTION_SCROLLER_HANDOFF.md) (`options[{id,label}]`, `response: {kind: option | text, value}`) and to the disagreement-handling rule in [`card-writing-guide.md`](./card-writing-guide.md).

## The standard option set

Default to four options on most cards:

1. **Affirm** — "This fits." (or a card-specific variant, see below)
2. **Decline** — "Not today." / "Not really."
3. **Uncertain** — "I'm unsure."
4. **Non-disclosing** — "Prefer not to share."

Plus always-available **free text** (1–1,000 characters after trimming; whitespace-only rejected without losing the draft, per the handoff doc). Free text is not a fifth "option" competing for space — it's a separate, always-present affordance (a text field below the options, or a "say more" toggle), because forcing a user to choose between "pick one of these labels" or "write your own" as an either/or adds friction the handoff doc doesn't ask for.

**Every offered option must count as a valid, sufficient response that unlocks the next card.** There is no hidden hierarchy where some options "really" advance and others just stall — this is explicit in the handoff doc's shipping acceptance criteria: "Every offered non-disclosing option advances." Treat all four (and free text) as equally sufficient inputs to the server; the *content* of the next card should shape itself to which one was picked, but availability of the next card must not depend on which one.

## When to offer which set

Most cards use the standard four. Deviate only with a clear reason:

- **Yes/no-shaped observation cards** ("Does that fit today?"): standard four works as-is. This is the default and should be the majority of cards.
- **Open, exploratory question with no obvious binary** (e.g., "What's one thing you noticed today?"): drop "This fits" / "Not today" (they don't map to an open question) and lead with free text, offering "I'm unsure" and "Prefer not to share" as the quick options, plus maybe one soft prompt-starter chip ("Something small," "Nothing yet") — never a chip that presupposes a specific answer.
- **Session-summary / "how did this feel" checkpoint** (end of a 5-card session): can offer a slightly different affirm/decline pair calibrated to reflection-on-the-whole-session rather than one card ("This was useful today" / "Not really, today" / "I'm unsure" / "Prefer not to share") — same structure, scaled up.
- **Never** offer a set that has no negative or neutral option — a card with only "This fits" and "Tell me more" is a leading design (see below) and is not allowed regardless of how natural it feels for a given card.

## Wording the options so they don't lead the user

The wording of the option itself can bias the answer even before free text is considered — this is a subtler failure mode than the card copy itself, and worth checking separately:

- **Avoid asymmetric framing.** "This fits" vs. "Not today" are symmetric — both are short, neutral, low-effort phrasings. Compare to a bad pairing: "Yes, that's exactly it!" vs. "No, I don't think so" — the affirmative option is longer, more enthusiastic, and implicitly the "correct"-feeling choice by virtue of energy alone. Keep option labels matched in length, register, and emotional temperature.
- **Avoid options that pre-load a value judgment.** "This fits" is fine; "This is so true" or "You really get me" is not — it recruits the user into flattering the product rather than reporting their own experience.
- **Avoid making the decline option sound like giving up.** "Not today" (neutral, situational) is correct; "Skip" or "I don't want to think about this" implies avoidance or defeat where none may be present — the user may simply disagree, which is a substantive response, not a skip.
- **Avoid stacking the deck with option order or visual weight.** Don't bold, enlarge, or color-highlight the affirming option relative to the others; equal visual weight for all options is part of not leading the user (this is a note for Codex's UI pass, flagged here because it's a response-*option* design concern even though it's implemented in layout).
- **"I'm unsure" must be a real, non-penalized option, not a placeholder for "didn't understand the question."** If users pick it disproportionately for a given card, that's a signal the card was unclear, not that the user is avoidant — see the QA note below.

## The non-disclosing option specifically

"Prefer not to share" needs to actually mean it, end to end:

- It must be visually and positionally equivalent to the other options — not smaller, not last-and-greyed, not requiring an extra tap to reveal (e.g., behind a "more options" menu while the others are one tap away).
- Selecting it must produce a next card that does **not** try again to extract the same disclosure through a different angle immediately after. (It's fine for a much later, unrelated card to eventually revisit a theme — but the very next card should not read as a second attempt at the same ask.)
- Server-side, per the handoff doc, this is a first-class valid `response.kind: option` value like any other — never treated differently in validation, storage, or generation-service prompting other than "the user chose not to disclose this," which should read to the model as complete information, not a gap to fill.
- Copy-wise, never label it in a way that implies cost ("Prefer not to share (skip this one)") — the parenthetical reframes non-disclosure as forfeiting something.

## Making disagreement actually change direction (not just get logged)

The handoff doc requires that disagreement change the next card's direction, and this is a generation-service concern as much as a copy concern, but the option design has to carry the signal cleanly enough for the model to act on it:

- Store *which* option was chosen as structured data (`option_id`, not just raw label text) so the generation service can reliably branch on "declined" vs. "affirmed" vs. "unsure" vs. "non-disclosing" without depending on fuzzy text parsing of free-text responses.
- When free text is given alongside or instead of an option, treat its sentiment as an additional signal for the *next card's angle*, but never as license to argue with it. The model should be prompted (this feeds Agent 3's prompts) with something like: "The user's last response was: {response}. If this disagrees with or complicates the prior observation, choose a different angle for this next card — do not defend or re-assert the prior observation." That instruction belongs in the generation prompt, not just this doc, but the option/response data model has to preserve enough structure (which option, or disagreeing-flavored free text) for that instruction to be followable.
- A useful internal test for whichever agent tunes the generation prompts: pull five consecutive real sessions where the user picked "Not today" or wrote clearly disagreeing free text, and check whether the next card in each actually reads as a different angle to a person unfamiliar with the system. If a reviewer can't tell the difference between the "after affirm" and "after decline" branches, the response-sensitivity requirement isn't actually being met — this is called out in the handoff doc's shipping acceptance criterion #7 ("response-sensitive continuation").

## QA signal to watch (not a metric to optimize toward)

If "I'm unsure" or "Not today" rates spike for a specific card template, treat that as a content-quality bug report (the card was confusing, presumptuous, or off-target for that mode/segment), not as evidence the user needs more encouragement to affirm. Never adjust option wording or card copy with the goal of raising the affirm rate — that inverts the entire point of offering a real disagree option in the first place, and is exactly the metric-chasing this whole feature is built to avoid (see [`psychology-research.md`](./psychology-research.md) Part B).
