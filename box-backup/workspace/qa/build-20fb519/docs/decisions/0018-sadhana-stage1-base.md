# 0018 — Sadhana Stage 1: the base of the spine

Date: 14 Oct 2026 · Status: adopted (founder spec), two items awaiting founder decision

## Founder (14 Oct 2026, paraphrased faithfully)
> The Stage 1 meditation brings attention down to the bottom of the spine; the question is how long you can keep it there.
> As a hook, and as feedback for understanding user types, the self-check afterwards asks: (a) were you even able to get your attention down to the spine; (b) how many times was your attention carried away from it; (c) what pulled you away — prompt them to reflect on the reasons.
> Core principle: "energy flows where attention goes". This is the starting point; more stages will come later.

## Built
- Practice `stage1-base` ("Stage 1 · The base"): fixed 60 s, end anytime, focus lock as 0017, posture guidance as 0017. Text-only guidance, no breath pacing:
  "Bring your attention down to the base of the spine. Let it rest there." → "If attention moved, notice where it went. Then bring it back down to the base." → "Stay with the base of the spine. End whenever you choose."
- Optional self-check after the sitting: reached the spine (Yes / Partly / No); drifts within this sitting, the user's own estimate (stepper 0…10+); what pulled attention away (Thoughts, Sounds, Body discomfort, Phone / notification urge, Sleepiness, Emotion, Something else) plus an optional note (≤500 chars).
- Self-report only: stored locally in the Sadhana record and queued as a `self_reports` row (kind `stage1_check`). Never a score, never capability evidence, never an attention, energy or health measurement. "How long" is not timed or scored.
- Migration `20261014090000_stage1_self_check.sql` adds the columns; RLS is unchanged (own rows only).

## Flags for the founder (not shipped as is)
1. "Energy flows where attention goes" can read as an energy claim (SOP-05 / honesty rules forbid energy claims). Proposed public wording: **"Where attention goes, you go."** His original stays internal (this doc). Not shown in the app.
2. Ambiguity: "how many times" in the day vs within the sitting. Built: within the sitting (default). A possible later daily check-in ("How many times today did attention get pulled away?") awaits his decision.
