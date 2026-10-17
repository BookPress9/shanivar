# 13. Launch funnel, CTAs and measurement

**Truth constraints:**
- The app is not on Play. Accounts/sync (DEV-25) and physical-device QA are pending.
- The live site's status needs founder confirmation. shanivar.com responded HTTP 200 with an early-access form on 17 Oct 2026, but the brief says DNS is pending, and the page header still shows a paused brand line.
- Until the founder confirms, **every destination has a DM fallback.**

## Stages
| # | Stage | Asset / CTA | Destination (labelled) | Primary metric |
|---|---|---|---|---|
| 1 | Reach → watch | P1–P2 reels (no product CTA) | Feed / Reels | Avg watch %, 3-s hold, completion |
| 2 | Engage | "Comment your…", "Send this to…", "Save" | Comments / shares / saves | Meaningful comments, shares and saves per 1k reach |
| 3 | Profile | Bio and pinned posts (R04, R08, R11) | @app.shanivar profile | Profile visits per 1k reach, follows per profile visit |
| 4 | Join the experiment (P3) | "DM SATURN" | Instagram DM, manual reply with the rules card | DMs with SATURN; % who post or report Day 1 |
| 5 | Waitlist (P4) | "Link in bio / DM WAIT" | **A)** shanivar.com early-access form with UTM, after founder confirmation; **B)** DM WAIT, logged manually | Link clicks, sign-ups, sign-up rate per click |
| 6 | Tester invite | Email or DM to waitlist, sent by the founder only | Debug/test build. **Gated on DEV-25 + device QA + founder decision** | Invites accepted |
| 7 | Activation | In-app: first boundary saved + first deliberate minute completed | App (test build) | % of testers activated within 48 h |
| 8 | Repeat practice | Shanivar Hour (Saturdays) + in-app practice | App + Instagram | % of activated testers practising on ≥3 of 7 days (in-app record, missed days allowed); % back the next Saturday |

**Bio (proposal, founder to approve; no paused brand lines):**
> Shanivar · Saturday, Shani's day
> Some things can wait.
> Android app in development · Early access ↓ (or DM WAIT)

**Waitlist form:** add a hidden `source` field filled from `utm_content` (r10/r11/r12/story/bio), plus an optional "What would you put behind the boundary first?" field. That gives qualitative insight and a quality filter. Consent text: "We'll email you about Shanivar's early access. Unsubscribe anytime."

**DM replies** are copy for the founder to send manually. Nothing is automated or sent by this plan.
- **SATURN →** "The Slow Planet Experiment: 7 days. Each day: 1 boundary hour (phone in another room or apps put away), 1 deliberate minute doing one thing on purpose, 1 honest line: 'What pulled me away?' Missing a day is part of it, so write that down too. Day 1 is Thu 12 Nov."
- **WAIT →** "You're on the list. Shanivar is still being built for Android, and we'll message you when testing opens. What would you put behind your boundary first?"

## Metric definitions
- **3-s hold** = viewers past 3 s ÷ plays. **Avg watch %** = avg watch time ÷ duration.
- **Share rate** = shares ÷ reach × 1,000. **Save rate** = saves ÷ reach × 1,000.
- **Meaningful comment** = a comment of ≥4 words that names a specific behaviour, an action taken, a number, or a reflection. Emojis, "nice", tags-only and spam don't count. **MC rate** = MC ÷ reach × 1,000.
- **Profile visit rate** = profile visits ÷ reach × 1,000. **Follow conversion** = follows ÷ profile visits.
- **Link CTR** = link taps ÷ profile visits. **Sign-up rate** = form submissions ÷ link taps (via UTM).
- **DM rate** = keyword DMs ÷ reach × 1,000.
- **Experiment completion** = participants reporting Day 7 (missed days allowed) ÷ SATURN DMs.
- **Activation** = testers with first boundary saved and first minute completed ÷ testers installed. **Repeat** = activated testers with practice on ≥3 of 7 days.
- **Views are recorded but never used alone as a success criterion.**

## Capture
- Record Instagram Insights manually at **24 h, 7 d and 28 d** into `marketing/shanivar/metrics.csv`:
  `date, asset_id, reel, phase, format, hook_variant, reach, plays, hold3s, avg_watch_pct, completion, likes, comments, meaningful_comments, shares, saves, profile_visits, follows, link_taps, dms_keyword, signups_utm, notes`.
- Netlify form export weekly (founder), with source counts.
- Leave blanks where Insights doesn't give a figure; never estimate.

## Phase targets
These are directional and set after week 1 baselines. They are not promises.
| Phase | Leading signal to watch | Decision |
|---|---|---|
| P1 Recognition | share rate and meaningful comments | The best behaviour hook becomes the P2 lead |
| P2 Reframe | save rate; comments that restate the idea in their own words | The Saturn angle (slow / shadow / receipt) that saves best leads P3 |
| P3 Experiment | SATURN DMs → Day 1 posts → Day 7 completion | If Day 1 < 30% of DMs, simplify to the 3-day version |
| P4 Launch | profile visits → link taps → sign-ups; Shanivar Hour comments; repeat Saturday | If CTR is low but comments are high, test a DM-first CTA |
