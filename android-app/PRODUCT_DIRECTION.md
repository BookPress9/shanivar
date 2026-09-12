# Shanivar: attention returned to everyday life

Owner: Codex, frontend/UI/UX. Backend and native discipline services: Claude.
Founder direction recorded 2026-09-09. This expands the earlier no-monetization MVP brief: paid app locks are now part of the intended product. Working interpretation is a paid subscription for blocking; a fee for breaking a lock is not assumed or charged.

Current UI direction: [interactive Saturn dark wireframes](wireframes/saturn-dark/index.html), with the screen rationale and Compose translation in [the wireframe README](wireframes/saturn-dark/README.md).

## Daily experience
- Today answers "what should I do now?" with one primary action adapted to the user's wake/quiet schedule, including night-shift schedules.
- Focus gives a clear choice of distracting apps, duration, price/entitlement, and a review before starting. Only an engine-confirmed active status can say "protected". Show remaining time, permission loss, and exit/release failures.
- Wake-up alarms let users choose time, days, sound, and a verified action such as scanning a QR code away from bed or completing movement. Show the next scheduled occurrence only after native confirmation. Never label a saved preference as an active alarm.
- WhatsApp offers a meaningful prompt at a chosen time, direct/gentle voice, one next action, quiet hours and opt-out. Verify the number and obtain opt-in before sending. Daily delivery, duplicate prevention and delivery receipts belong to the backend.
- Daily chart reflections aim for the personal specificity of Co-Star while preserving the user's agency. Chart calculations need a real ephemeris, valid birth timezone/location, input-quality metadata and transit provenance. AI-generated placeholder positions are not an accurate natal chart.
- Practice provides the Content team's VBT adaptations, source citations, accessible text, guided audio when available, optional reflection and a server-confirmed streak. Missing a day invites a return rather than shame.
- Messages from Shanivar is an intended personalized text scroller: birth-chart and current-planet context informs AI-guided reflection, with an explicit response required before the next item. It can be opened as an alternative while social apps are locked. See the [Claude feature handoff](CLAUDE_REFLECTION_SCROLLER_HANDOFF.md) for delivery requirements. This founder addition supersedes the previous blanket no-content-feed rule.
- The Hybrid Monk voice is direct, warm and spare. Bronze/black structure around a quiet, spacious practice experience. The scroller uses a patient teacher voice, visible stopping points and user-controlled continuation.

## Current native implementation
A Kotlin/Jetpack Compose project lives in this directory, separate from the superseded Capacitor reference scaffold.
Implemented source: introduction, Today/Focus/Practice/Rhythm navigation, device-local routine preferences, day-phase adaptation, existing API login/daily-note reading, 12 sourced text practices, elapsed-time meditation timer and local reflection saving.
Not yet a release: build/runtime verification is tracked in README; a persisted preference is not a delivered discipline feature.

## Remaining requirements and acceptance evidence
| Requirement | Required evidence |
| --- | --- |
| Paid locks | Confirmed commercial model, billing/entitlement contract, purchase/error/restore UI tests, real blocking on device |
| App blocking | Claude's native engine connected; permission grant/revocation, lock start/end and early-exit tested on supported devices |
| Movement alarms | Scheduling and dismissal contracts; verified QR/movement; lock-screen, reboot and foreground/background tests |
| WhatsApp | Verified phone, consent, schedule/quiet-hour/opt-out API; approved templates and real delivery status; no duplicate sends |
| Chart specificity | Validated chart calculations/transits and provenance; missing-birth-data and stale-note UI; no fabricated accuracy |
| Adaptive routines | Persisted preferences, timezone/day changes, overnight routines, accessible controls and small/large-font UI checks |
| Meditations | Content citation QA report, narration/audio lifecycle, assessment handoff and session timing verified |
| Streak | Stable technique/session IDs, retry-safe completion, account isolation and backend timezone/day-boundary tests |
| Release | Native build, local tests, rendered UI review and real-device integration evidence; web smoke tests are not a native release gate |
