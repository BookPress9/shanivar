# Shanivar: focused time and awareness in everyday life

> **Current product-form update (30 September 2026):** [decision 0014](../docs/decisions/0014-routine-sadhana-shell-and-routine-v3.md) adopts two primary destinations, **Routine** and **Sadhana**, with Routine as the launch destination and Dashboard removed. Phase 1 implements Routine from the [V3 handoff](CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md). This file remains the outcome/audience foundation; older navigation and staged-backlog statements yield to 0014.

## Current pitch and usability scope — 13 September 2026

The corrected audiences are worried adult children introducing their parents, and younger users choosing Shanivar for their own focus and awareness of thinking. Parents may choose a respectful Lord Shani connection; belief and adoption must not be inferred from age. All ages are intended; adult testing is the first validation step, with school-age readiness gaps reported explicitly.

The [initial MVP scope](INITIAL_MVP_SCOPE_2026-09-13.md) now controls launch visibility: an independent alarm tool plus immediate focus modes, editable activity presets, selected app targets, optional local pause and a short review. Use one home, familiar onboarding and the [plain voice guide](../docs/strategy/shanivar-tone-and-values.md). “Hyperfocus” is an aspiration; metacognition means examining and regulating thinking, not an established highest-intelligence ranking.

The [Claude task](CLAUDE_MVP_STREAMLINE_TASK_2026-09-13.md) is prepared for implementation. Birth charts, AI scroller, larger practice library, automatic focus schedules and monetization move to the [staged backlog](../docs/strategy/shanivar-launch-backlog.md). They may exist in source but are not initial pitch requirements. Preserve safety, data and management of any already-active service when hiding features. These documents do not claim that the simplified build is already delivered.

Owner: Codex, frontend/UI/UX. Backend and native discipline services: Claude.
Founder direction recorded 2026-09-09. This expands the earlier no-monetization MVP brief: paid app locks are now part of the intended product. Working interpretation is a paid subscription for blocking; a fee for breaking a lock is not assumed or charged.

## Founder outcome — clarified 13 September 2026

Shanivar is for willing adults in India who want to step out of recurring mental loops, become more able to remain with boredom or an urge, and redirect attention toward curiosity, purpose, relationships and self-inquiry. Its spiritual foundation is the founder's belief in shared consciousness and loosening conditioning described as maya toward presence and cosmic flow. The product should help users examine their own experience without requiring agreement with that belief.

The smallest product outcome is a personally meaningful sequence: **notice the loop → experience the urge briefly → choose one real-world action → leave the app → reflect later if useful.** App opens, card responses, streaks and session duration do not establish this outcome.

Long-term success may include progressively fewer prompts and user-confirmed little or no need for Shanivar. Inactivity alone is unknown; distinguish confirmed independence, freely chosen ongoing support, unsuccessful departure and missing follow-up. Design the reflection scroller so it cannot quietly become a substitute feed.

Astrology is intended to improve guidance rather than lead acquisition. Treat ephemeris/calculation correctness, user-perceived interpretive fit and incremental practical usefulness as separate validation layers. Meditation before insight is a sequence to test; preserve disagreement and do not treat post-meditation receptivity as consent to an unquestionable interpretation.

See [founder direction](../docs/strategy/shanivar-founder-direction.md), [India market fit](../research/shanivar-india-market-fit.md) and the [release learning log](../ai_org/shanivar_market_fit_release_log.md).

Current UI direction: [interactive Saturn dark wireframes](wireframes/saturn-dark/index.html), with the screen rationale and Compose translation in [the wireframe README](wireframes/saturn-dark/README.md).

## Broader experience retained for later stages
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
