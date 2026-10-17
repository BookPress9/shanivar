# 14. Testing plan: hooks, stories, practices

## Method
- **Preferred:** Instagram **Trial Reels** (shown to non-followers first), if available on the account. Post 2 variants of the same reel with only one variable changed. After 72 h, publish the winner to followers.
- **Fallback (matched pairs):** post variants on comparable weekdays and times (same slot ±1 h, same week), up to 2 pairs per week. Treat the results as directional; don't over-read a single pair.
- **One variable per test.** Every other element stays identical, including the cover, caption length and sound.
- **Decision rule:**
  - A variant "wins" if it beats the other by **≥20% on the test's primary metric** with reach of at least 1,000 each.
  - Otherwise: no decision. Retest or keep the cheaper version.
- **Weekly review:** Monday 10:00 IST (05:30 CET after 25 Oct). 30 minutes: fill metrics.csv, tag comments, write 3 learnings, choose next week's tests.

## Hook tests (primary metric: 3-s hold, then share rate)
| Test | Variant A | Variant B | Reel |
|---|---|---|---|
| H1 visual vs voice | Silent macro of a thumb on the reach (no VO for 3 s) | Face to camera, line spoken | R01 |
| H2 text vs none | Hook text on screen 0–3 s | No text, picture only | R03 |
| H3 explicit vs implicit Saturn | "Saturn takes 29 years to go round once." | "Something in the sky takes 29 years to go round once." (reveal at C) | R04 |
| H4 accusation vs confession | "You've started Monday 40 times." | "I've started Monday since March." | R02 |
| H5 comedy vs tension | Absurd version (exaggerated 11-minute "emergency") | Straight long take | R12 cut-down |

## Story-structure tests (primary metric: avg watch %, then saves)
- **S1:** Full engine A→E vs cold open on D (participation first, then the reframe).
- **S2:** Reframe as myth (Chhāyā story, R05) vs reframe as fact (NASA orbit, R04). The same behaviour in both.
- **S3:** Founder voice vs creator voice for the same script skeleton (R07 vs a CB4 adaptation).
- **S4:** One behaviour deep vs a 4-behaviour montage in B.

## Practice tests (primary metric: meaningful comments that report doing it, then DM rate)
None includes a breath instruction.
| Code | Practice | Asks the viewer to | Used in |
|---|---|---|---|
| P-a | Urge watch | Notice the reach; name what you intended before you decide | R01 (mirrors in-app "The moment before the reach") |
| P-b | 30-second reply pause | Wait 30 s before replying to one message | R03 |
| P-c | 20 slow steps | Walk 20 steps noticing feet | R06 cut |
| P-d | 60 seconds of nothing | Sit with no phone for one minute; count exits | R09 |
| P-e | Two minutes of the avoided task | Start the postponed thing for 2 min | R02 |
| P-f | Saturday folder | Move one app into a folder named "Saturday" | R11 |

**Question tested:** which practice gets the most *"I did it"* comments, and which carries over into Experiment Day-1 posts?

## CTA tests (P3–P4 only; primary metric: DM rate or link CTR)
- **C1:** "DM SATURN" vs "Comment SATURN" (then the founder DMs the rules manually).
- **C2:** "Link in bio" vs "DM WAIT" for waitlist sign-ups. Count both as a sign-up; record the drop-off.
- **C3:** Soft ("if you want it") vs direct ("Join the Android waitlist").

## Guardrails
- Never test truthfulness: no variant may exaggerate the product status or the results.
- Never test fear framing (e.g., "Shani is watching") even if it would "win".
- Stop any variant that draws comments offended on religious grounds. Review it with the founder and CB5.

## Learning log
`marketing/shanivar/creative-learning-log.md`, one entry per test: `date, test_id, hypothesis, variable, A, B, primary metric A/B, reach A/B, result (win/none), learning, next action`.
