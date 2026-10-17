# Team brainstorm: release readiness for 20 physical testers — 2026-09-12

Called by the founder after finalising the 14-screen bilingual wireframes with Codex. Four
perspectives ran in parallel — project management, backend architecture, QA/release risk, and user
research — each reading the repo cold. Every claim below that mattered was independently verified
by the Staff Manager before being acted on; two agent claims were wrong and are corrected here.

## The finding that outranks everything else

**The alarm made no alarm sound.** A grep across all of `app/src/main` for
`Ringtone|MediaPlayer|Vibrator|WAKE_LOCK|USAGE_ALARM|setSound` returned nothing. The alarm relied
on a notification channel's default tone — the notification stream, at notification volume,
silenced outright by Do Not Disturb. For an app whose core promise is a wake-up you cannot dismiss
from bed, the alarm could not reliably wake anyone. Every scheduling detail underneath it was
correct and irrelevant.

Fixed this session: `AlarmRinger.kt` plays the device alarm tone on `USAGE_ALARM` (the stream
exempt from ringer mute and DND), loops it, vibrates on a distinct pattern, and holds a
timeout-bounded partial wake lock so Doze cannot defer the gap between the broadcast firing and the
alarm screen appearing. Added `VIBRATE` and `WAKE_LOCK` to the manifest. The alarm screen now warns
when alarm-stream volume is zero, because a muted device produces the same outcome as the bug.

Two more verified holes closed alongside it:
- **Back dismissed the alarm.** `BlockOverlayActivity` blocked the back press; `AlarmActivity` — the
  screen actually described to users as hard to dismiss — did not. Exactly inverted. Now swallowed.
- **The client timed out before the server woke.** `ShanivarApi` used a 25s read timeout against a
  cold start measured at 22.6s, with Gemini latency stacking on top. The first request of every day
  was the one most likely to fail. Now 20s connect / 60s read.

Still open and **not** fixable from here: `POST_NOTIFICATIONS` is declared but never requested at
runtime. On Android 13+ it is denied by default, and a denied notification means the full-screen
intent is never delivered — the alarm silently never appears. That request must happen in Codex's
onboarding flow.

## Corrections to the agent reports

- The PM's claim that `android-app/` is uncommitted is **wrong** — it checked the old `main` branch.
  On `shanivar-clean`, 87 files including 43 Kotlin sources are tracked.
- My own earlier alarm, that the movement-contract migrations were missing from the live database,
  was **half wrong**: the `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` and constraint-swap statements
  do exist (`discipline.js:143-162`). The live DB lacks them only because that code is still
  uncommitted and undeployed. It will self-heal on deploy.

## Where the four reports converge

1. **Nothing has ever run on real Android hardware, and no one owns changing that.** The emulator is
   banned here; neither AI agent can hold a phone. QA's minimum protocol is eight pass/fail checks
   on one real device — alarm audible with screen off, after Doze, after reboot, with notifications
   denied, Back and Home both failing to dismiss, dismissal impossible from bed — then a repeat on
   one aggressive-OEM phone without battery whitelisting. That last number is the honest reliability
   figure. This is the true long pole and it is a founder task.
2. **You cannot measure the one thing the test exists to measure.** There is no alarm-fired event
   anywhere. A killed alarm writes no row, so "the OS killed it" and "the user churned" are
   indistinguishable in the data. With a 20-person OEM matrix whose entire purpose is finding where
   alarms die, this is the highest-leverage backend build, and it is mine.
3. **Scope expanded at the worst possible moment.** The wireframes went 5 → 14 screens and added a
   second language, while the founder direction voided the only dismissal flow that had tests. The
   PM's cut list — 9 screens, Hindi, ML Kit scene check, the register screen, pricing UI — is sound.
4. **Hindi is not merely untranslated, it is actively broken.** The backend architect found silent
   failures, not gaps: `insights.js` regex-parses English `Insight:`/`Action:` labels and falls back
   to hardcoded English strings *inside a successful response* when they don't match; the
   reflection generator appends a spurious English "Prefer not to share" to every Hindi card; the
   offline fallback bank matches English substrings only. Shipping Hindi to testers now would
   produce half-English output that reads as a quality failure. All three converge on the same
   answer: English-only for this cohort.

## The cohort design worth adopting

Research lead's structure, which is better than "recruit 20 people":

- **6 first, then 14.** A one-week shakedown on six testers before spending first-run impressions
  on bugs you have no crash reporting to see.
- **Five weeks, with a week 0 baseline (locks off) and a week 4 deliberate withdrawal.** The
  withdrawal week is the most informative one: if wake times snap back, Shanivar is scaffolding, not
  habit — still a fine product, but a subscription sold on lasting change would be a false claim.
- **Recruit on device, not demographics.** Xiaomi/Realme/Vivo/Samsung in quantity, with two
  near-stock phones as control, because that is where alarms actually die.
- **Screen out** friends and family (they don't quit, and silent non-churn is the signal you most
  need), safety-critical wakers, and anyone already succeeding with a hard blocker.
- **Tell every tester in writing: do not use Shanivar as your only alarm.** It is honest, it
  protects them, and it converts missed-alarm reports from complaints into data.
- The falsification bar for the core bet: if ≥30% of alarm occurrences end in a bypass rather than a
  verified dismissal, physical friction lost. And note the subtler failure — verification succeeds,
  then the person walks to the window and opens Instagram anyway. Movement verified is not attention
  reclaimed, and the schema currently cannot tell those apart.

## Founder decisions this brainstorm surfaced

1. Who physically tests on a real phone, and when. Nothing ships past this without an answer.
2. Render's paid tier (~$7/mo) or keep absorbing a 20-30s cold start on every tester's first daily
   open. Raised timeouts stop it failing; they do not stop it feeling broken.
3. English-only for this cohort — recommended by three of four perspectives.
4. Legacy `qr`/`photo_motion` for the first testers, or hold the cohort until movement dismissal is
   real on hardware. This single decision owns the timeline.
