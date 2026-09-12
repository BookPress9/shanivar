# Shanivar: attention returned to everyday life

Owner: Codex, frontend/UI/UX. Backend and native discipline services: Claude.
Founder direction recorded 2026-09-09. This expands the earlier no-monetization MVP brief: paid app locks are now part of the intended product. Working interpretation is a paid subscription for blocking; a fee for breaking a lock is not assumed or charged.

Current UI direction: [interactive Saturn dark wireframes](wireframes/saturn-dark/index.html), with the screen rationale and Compose translation in [the wireframe README](wireframes/saturn-dark/README.md).

## Daily experience
- Onboarding begins with English/Hindi choice, then account access, optional birth context, capability explanation and arrival. Birth details promise more relevant chart-based reflections, not better mental-health outcomes. Hindi uses **दिनचर्या (Dincharya)** for rhythm and may respectfully acknowledge Shani as a symbol of time, consequence and patient discipline without assuming faith.
- Today answers "what should I do now?" with one primary action adapted to the user's wake/quiet schedule, including night-shift schedules.
- Today leads with one personal-feeling daily direction in the spirit of a direct astrological reflection: one observation, one question/action, visible provenance and an interpretive-not-predictive stance. Its notification is opt-in, user-timed and limited; notification delivery is not proof that the direction was read or acted on.
- Focus gives a clear choice of distracting apps, duration, price/entitlement, and a review before starting. Only an engine-confirmed active status can say "protected". Show remaining time, permission loss, and exit/release failures.
- Focus suggestions start with an adjustable post-wake window, a 30–60-minute pre-sleep range, and a custom transition. The exact morning duration remains a product hypothesis; keep the default restriction to one chosen app and one window until user behavior supports more.
- Wake-up alarms let users choose time, days, sound and a movement cue outside arm's reach. The default uses sustained steps toward morning light, a kitchen, washroom or custom destination. Optional scene recognition is secondary, on-device and discarded after verification; QR scanning is no longer the default experience. Provide a setup-time non-walking alternative. Show the next scheduled occurrence only after native confirmation. Never label a saved preference as an active alarm.
- WhatsApp offers a meaningful prompt at a chosen time, direct/gentle voice, one next action, quiet hours and opt-out. Verify the number and obtain opt-in before sending. Daily delivery, duplicate prevention and delivery receipts belong to the backend.
- Daily chart reflections aim for the personal specificity of Co-Star while preserving the user's agency. Chart calculations need a real ephemeris, valid birth timezone/location, input-quality metadata and transit provenance. AI-generated placeholder positions are not an accurate natal chart.
- Practice provides the Content team's VBT adaptations, source citations, accessible text, guided audio when available, optional reflection and a server-confirmed streak. Missing a day invites a return rather than shame.
- Messages from Shanivar is an intended personalized text scroller: birth-chart and current-planet context informs AI-guided reflection, with an explicit response required before the next item. It can be opened as an alternative while social apps are locked. See the [Claude feature handoff](CLAUDE_REFLECTION_SCROLLER_HANDOFF.md) for delivery requirements. This founder addition supersedes the previous blanket no-content-feed rule.
- The Hybrid Monk voice is direct, warm and spare. Bronze/black structure around a quiet, spacious practice experience. The scroller uses a patient teacher voice, visible stopping points and user-controlled continuation.

## Current native implementation
A Kotlin/Jetpack Compose project lives in this directory, separate from the superseded Capacitor reference scaffold.
Implemented source: English/Hindi onboarding, account creation/sign-in, optional birth inputs, Today/Focus/Practice/Rhythm navigation, Supabase-backed account rhythm preferences, live focus start/status/release, Android app blocking, exact alarm scheduling and server confirmation, movement-first dismissal, reflection scroller, 12 sourced text practices, elapsed-time meditation timer and local practice notes.
The backend contract and Compose interactions are automated on Supabase, JVM/Robolectric and an Android 14 managed emulator. Physical-device evidence remains required for hardware steps, lock-screen alarms, reboot restoration and OEM background restrictions.

## Remaining requirements and acceptance evidence
| Requirement | Required evidence |
| --- | --- |
| Paid locks | Confirmed commercial model, billing/entitlement contract, purchase/error/restore UI tests, real blocking on device |
| App blocking | Claude's native engine connected; permission grant/revocation, lock start/end and early-exit tested on supported devices |
| Movement alarms | Revised movement-cue contract; step-sensor and calibrated fallback verification; optional on-device scene check; accessible alternative; lock-screen, reboot and foreground/background tests |
| WhatsApp | Verified phone, consent, schedule/quiet-hour/opt-out API; approved templates and real delivery status; no duplicate sends |
| Chart specificity | Validated chart calculations/transits and provenance; missing-birth-data and stale-note UI; no fabricated accuracy |
| Adaptive routines | Persisted preferences, timezone/day changes, overnight routines, accessible controls and small/large-font UI checks |
| Meditations | Content citation QA report, narration/audio lifecycle, assessment handoff and session timing verified |
| Streak | Stable technique/session IDs, retry-safe completion, account isolation and backend timezone/day-boundary tests |
| Release | Native build, local tests, rendered UI review and real-device integration evidence; web smoke tests are not a native release gate |
