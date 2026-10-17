# Claude implementation handoff: VBT breathing and meditation

**Visual and naming update (2026-09-25):** the feature is now **Steady**, descriptor **Breathing & attention**, brand line **Breathe. Return. Begin.** Read the [visual implementation contract](wireframes/steady/README.md), [interactive wireframe](wireframes/steady/index.html) and [17-screen board](wireframes/steady/exports/index.html) before building UI. This replaces the working name ?Breathe & reset? and splits selection into Moment ? Comfort. The behavioral safety/lifecycle requirements below remain authoritative. Internal IDs and current work in progress stay intact.

Date: 25 September 2026. Owner of the next build: Claude, including the necessary Compose integration. Deliverable from this research turn: build specification, starter content and test requirements. App implementation has not been performed.

## 1. Authority and outcome

The founder explicitly requested a VBT-based breathing/meditation feature embedded alongside action-oriented alarms and app locks, adapted to feelings, circumstances and time of day, plus this build handoff and testing guidelines. This authorizes this narrowly scoped third feature despite older documents deferring the practice library. It does not authorize restoring the reflection feed, astrology, gamification, paid plans or the entire old library. The audience is everyone seeking more deliberate attention; adult-first evaluation does not redefine that ambition.

Build **Steady** inside the existing native Android app. A person can choose a current need, receive an understandable suggestion, follow a short original VBT-derived or clearly labeled modern practice, stop freely and return to life. Source lineage stays accessible without dominating the flow.

Read in order:

1. Root `CLAUDE.md`, especially alarm silence/escape invariants and the emulator prohibition.
2. [Catalogue and source audit](../research/shanivar-vbt-breathing-catalog-2026-09-25.md).
3. [Starter JSON](../shanivar-vbt/content/breathing-mvp-v1.json), the only approved implementation seed for this feature.
4. [Test guide](VBT_BREATHING_TEST_GUIDE_2026-09-25.md).
5. [Decision 0011](../docs/decisions/0011-vbt-breathing-third-pillar.md), [enforcement stance](ENFORCEMENT_STANCE_2026-09-12.md), and current code at the seams below.

Use this handoff over older player/router documents where they conflict. Do not start another Capacitor shell in `shanivar-vbt/app-shell/scaffold`. Do not require the 13-question historical assessment. Do not run an emulator or a Gradle managed-device task on this machine.

## 2. Current code and intended seams

Paths below are under `app/src/main/java/com/shanivar/nativeapp/` unless otherwise stated. Findings are from source inspection on 2026-09-25, not a new runtime verification.

| Existing component | Observed behavior | Required integration |
| --- | --- | --- |
| `V1Home.kt` | Home, focus, alarm, settings and a local pause route | Add a third Home card and a dedicated local route. Retain the existing independent alarm/focus controls. |
| `ShanivarApp.kt` → `HomeScreen` | Supplies Focus/Alarm cards to V1Home | Supply breathing host and optional schedule configuration. Avoid a large unrelated refactor. |
| `LocalPauseScreen.kt` | One-minute offline prompt, used in onboarding/Home/overlay; prompt is random | Preserve its caller contract while replacing the random breathing instruction with the reviewed external-grounding preview or delegating to the new player. No unreviewed random breath advice. |
| `BlockOverlayActivity.kt` | Offers a pause inline, then returns to device Home | Offer the 60-second choice inline. Completing/stopping must not release focus or reopen the blocked package. |
| `AlarmActivity.kt` → `dismissAlarm`, `AlarmDoneScreen` | Stops ringer, stops service, advances recurrence, then displays done screen | Add optional post-dismissal entry only after that sequence. No meditation in dismissal verification or teardown. |
| `DailyRhythm.kt` → `Rhythm` | Contains wakeTime and quietTime; `moment()` uses clock-relative periods | Reuse chosen wake time where appropriate; introduce a separate local intended-sleep preference. Do not silently interpret quietTime as bedtime or change existing alarm scheduling. |
| Legacy `PracticePlayer` in `ShanivarApp.kt` | Text passages, screen-on behavior, fixed “App lock off” copy | Use as a reference only. It does not satisfy source metadata, protection truth, stop behavior or bedtime requirements. |
| `app/src/main/assets/techniques.json` and `scripts/compile-content.cjs` | Package the older 12 scripts | Do not feed those scripts into the new feature. Use a separate versioned JSON asset. |
| `SessionTiming.kt`, tests under `src/test` | Existing pure passage timing helper and Robolectric setup | Add explicit session-state tests; do not assume the existing helper handles lifecycle restoration. |

Suggested new files (names are recommendations): `BreathingModels.kt`, `BreathingCatalog.kt`, `BreathingRecommender.kt`, `BreathingSchedule.kt`, `BreathingSessionState.kt`, `BreathingViewModel.kt`, `BreathingScreen.kt`, `BreathingPreferences.kt`, `BreathingTonePlayer.kt`. Keep recommendation and session logic pure and independently testable. Reuse UI primitives and theme.

No backend schema/API is necessary for this first slice. It must work offline and before sign-in. Do not call the legacy practice-completion endpoint with these string IDs; it expects a different identity model. No XP, payment dependency, family reporting or uploading feelings.

## 3. Product surface

Home card: **Steady**. Supporting copy: “A short practice for this moment.” Descriptor: **Breathing & attention**. Primary button: **Find my pause**. A contextual shortcut may read “Before focus · 1 min” with a change option. Do not add eight cards to Home.

Selection sheet:

- Need: Scattered / Tense / Angry or frustrated / Low energy / Overstimulated / Winding down / Just practice. These are self-reports, not clinical categories.
- Optional context override: Just woke up / Daytime / Winding down / Going to sleep. Explain inferred context with “Based on the schedule you chose.”
- Time: 1 / 3 / 5 minutes, filtered to what the selected practice supports. Never silently lengthen the chosen budget.
- Comfort: “Prefer to notice breathing, listen, or notice the room?” Choosing breath sets breathComfort=true for this selection; otherwise unknown remains unknown. “Breath attention feels uncomfortable” excludes breath methods. Sound is a separate opt-in preference, not an Android permission.
- Confirm a safe stationary setting before starting. If driving, in water or doing an attention-critical task, offer **Come back later**; do not launch immersive practice. No continuous sensing or location permission.

Show one suggested practice and at most two eligible alternatives, plus **All suitable options**. Each exposes title, duration, short purpose, **Start**, and **Origin & adaptation**. No diagnostic intake, promised cortisol changes or “engineered for your nervous system” claim.

MVP library is exactly the eight IDs in the JSON. `breath_turn` requires an explicit “Comfortable with breath awareness” choice; never infer readiness from minutes completed or age. `sleep_threshold` requires an explicit sleep intention and currently feeling sleepy. Do not schedule it just because it is night. All current practices are unpaced; no inhale/exhale animation that imposes a hidden ratio. A static attention marker or session-progress arc is appropriate. Do not count breaths or call a completed timer successful meditation.

Plain wording is default. Respect existing optional Shani framing without changing technique eligibility, promises, safety behavior or access. Hindi is a supported content path, but do not claim Hindi guidance until reviewed strings/scripts exist. Implement English seed now, externalize text, and clearly disclose English fallback when Hindi content is missing.

## 4. Recommendation rules: deterministic and explainable

Inputs: `feeling?`, `explicitContext?`, `sleepIntent`, `sleepyNow`, `breathComfort: Boolean?`, `soundConsent: Boolean`, `priorBreathComfort: Boolean`, `safeSettingConfirmed`, chosen duration budget, optional wake/sleep schedule, local ZoneId and injected clock. Feelings are ephemeral. No ML, LLM, birth chart, health inference or sensor-derived state.

Order:

1. Unsafe/unconfirmed setting → no Start action; show the safe-place prompt. Keep Exit available.
2. Load only valid, enabled catalogue entries; filter by budget and required capabilities/preferences. `requiresBreathComfort` needs true, never null. Humming/listening require soundConsent. `breath_turn` needs priorBreathComfort. `sleep_threshold` needs sleepIntent AND sleepyNow. Invalid/missing sound asset excludes `sound_listen`.
3. Explicit goal/context takes precedence over schedule. Going-to-sleep without sleepyNow suggests ordinary rest/grounding, not sleep-threshold practice. No daytime restriction on an explicit sleep goal for a shift worker.
4. Use the candidate order below, dropping ineligible entries. Select the first and show the next two as alternatives. Do not sort randomly.
5. Unknown or contradictory inputs → `external_grounding`, with a plain invitation to choose another option. If no duration fits, explain and offer the shortest supported choice for explicit confirmation; never silently extend time.

| Need/goal | Ordered candidate IDs |
| --- | --- |
| Explicit sleepIntent + sleepyNow | `sleep_threshold`, `external_grounding` |
| Scattered / before focus | `natural_anchor`, `breath_word`, `external_grounding` |
| Tense | `natural_anchor`, `hum_listen`, `emotion_space`, `external_grounding` |
| Angry or frustrated / distracting urge | `emotion_space`, `external_grounding`, `natural_anchor` |
| Low energy / just woke up | `external_grounding`, `natural_anchor` |
| Overstimulated | `external_grounding`, `sound_listen`, `natural_anchor` |
| Winding down, not yet sleepy | `natural_anchor`, `hum_listen`, `breath_word`, `external_grounding` |
| Steady / just practice | `natural_anchor`, `breath_word`, `breath_turn`, `external_grounding` |
| No need supplied | Infer just-woke or winding-down suggestion from valid schedule; otherwise `external_grounding`, `natural_anchor` |

Explicit selection from All suitable options is allowed after the same safety/comfort filters. `suitableContexts`/`suitableFeelings` in the JSON are ranking hints, not diagnosis exclusions. Hard constraints are the `requires*` fields and safe-setting flag. A prior “felt worse” response suppresses that practice's automatic suggestion for the rest of this app visit; manual selection remains available after a comfort reminder. No automatic intensification.

Reason strings describe inputs: “You chose a short pause before focus” or “You preferred an option without breath attention.” Never say the algorithm knows the person's biological state.

### Schedule calculation

Store optional local wake and intended-sleep `LocalTime` values in a separate `breathing_preferences` namespace; local-only, user-editable, following device timezone. Let `span = floorMod(sleepMinute - wakeMinute, 1440)` and `elapsed = floorMod(nowMinute - wakeMinute, 1440)`. Valid schedule for suggestions: span 360..1200 minutes. This range is an engineering plausibility guard, not a recommended sleep duration. Invalid/absent values yield UNKNOWN and a quiet edit option.

- `elapsed >= span` → REST_WINDOW, not proof of sleep.
- Otherwise `elapsed < 120` → JUST_WOKE_WINDOW.
- Otherwise `elapsed >= span - 120` → WIND_DOWN_WINDOW.
- Otherwise → DAYTIME.

REST_WINDOW only suggests a winding-down choice; `sleep_threshold` still requires user sleep intent/sleepiness. Windows are configurable product heuristics. Resolve explicit need first; “low energy” at 23:00 does not prescribe sleep if the person chose to work. Re-evaluate suggestions when returning to selection, never change the active practice when clock/timezone/preferences change.

Examples: wake 07:00/sleep 23:00 → 08:00 just woke, 14:00 daytime, 22:00 wind down, 01:00 rest window. Wake 16:00/sleep 08:00 → 17:00 just woke, 02:00 daytime, 07:00 wind down, 10:00 rest window. Duration uses a monotonic clock, never these local clock calculations. Daylight-saving transitions affect the displayed schedule context, not active session elapsed time.

## 5. Content and player contract

Read the seed JSON and create a deterministic packaged copy, e.g. `assets/breathing-v1.json`. Add a schema/catalogue validator that fails the build/check for duplicate IDs, invalid durations, empty required source notes, a missing fallback, malformed verse labels, unrecognized kinds or prescribed breath phases. It must not mark “implementation_seed” as clinically reviewed automatically. All required source metadata must remain in the shipped asset even when the user never opens the details panel.

Retain `source=null` for `external_grounding`; show “Modern attention exercise,” not VBT. In other cards display “Modern adaptation · VBT verse …”; details include edition, link, omissions/substitutions and manuscript note where relevant. Do not display copied translation passages or imply that the authors endorse Shanivar.

The JSON contains four stages and timing rules. For ordinary practices: arrival 10 s, instruction 20 s, practice `duration - 40` s, closing 10 s. For sleep: arrival 10 s, instruction 20 s, practice `duration - 30` s, closing 0. Stage durations sum exactly to the selected duration. These are UI pacing values, never breath-phase durations. Show Pause/Resume and End throughout. Allow users to pause for reading or choose a longer duration. No obligation to wait for the timer.

Foreground MVP is reviewed text with an optional original local fading tone for `sound_listen`. Produce/license that simple tone as an app asset, low default volume, no special-frequency claims; play it once on entry to the practice stage, not repeatedly. Humming is performed by the user; do not request a microphone or analyze their voice. Generic haptics, if offered, signal a stage transition only; default off, always off in sleep mode. No breath pacing by vibration.

Do not pretend that text is eyes-closed audio guidance. In this MVP, screen-off practice is silent after reading the instructions. Voice-guided background playback is a separate later capability, requiring real reviewed recordings and the Android media lifecycle. Do not bolt it onto the alarm foreground service or use a fake silent media track to keep a timer alive. [Android service-type guidance](https://developer.android.com/develop/background-work/services/fgs/service-types).

### State and lifecycle

Use one session owner independent of recomposition. Suggested states: READY, RUNNING, PAUSED, RESTING_SILENTLY, ENDED_TIMER, ENDED_USER, INTERRUPTED. An injected monotonic clock plus accumulated active milliseconds determines remaining time; clamp at zero. “Timer ended” is the only automatic outcome, not “practice succeeded.”

- Start is idempotent. Pause freezes elapsed time. Resume continues remaining time; it never resets to the full duration.
- End is immediate, idempotent, cancels all tone/haptic callbacks and requires no confirmation dialog. Back while practicing behaves as End before navigating.
- Rotation preserves the session in the ViewModel; no duplicate cues or new record. Source-detail opening pauses a running session; returning requires explicit Resume, without replaying completed cues.
- Normal foreground practice: on app background/screen lock/audio-focus interruption, stop cues and enter PAUSED. Explain on return. Never catch up missed tones.
- Sleep practice: after the instructions, automatically enter RESTING_SILENTLY. Do not keep the display awake. The silent interval may elapse while backgrounded; calculate elapsed time on return. No guaranteed background callback, wake lock, notification or sound is required. If the process survives and time elapsed, show a quiet ended state only when the user returns.
- Process death: on next launch mark a persisted RUNNING/PAUSED/RESTING record INTERRUPTED; no automatic audio/start/resume or presumed completion. Never compare persisted elapsedRealtime across reboot. Retain selected practice/preferences but require a fresh Start.
- Another alarm firing preempts/stops practice cues and marks the practice PAUSED/INTERRUPTED as appropriate; no automatic resumption after dismissal. The alarm must remain audible according to its existing configuration. Test same-app audio competition explicitly.
- A single active breathing session across Home, overlay and post-alarm entries: reopening focuses the existing session or offers to end it, never stacks sounds/timers. If an existing session is surfaced by the block overlay, that host takes exit ownership; exit still goes to device Home, not the blocked package. Alarm entry has priority over all breathing hosts.

Keep a bounded local journal (at most 100 records) containing random session ID, practice/content version, entry context, selected duration, elapsed-active duration, terminal outcome and optional feedback. No raw feeling labels, free text, blocked package names or uploaded events. Offer Clear practice history; clearing preferences/history must not clear alarms or locks. Current feeling, comfort answers and safe-setting confirmation are ephemeral per visit; only explicit preference saves persist. Backup remains consistent with the app's existing policy.

## 6. Alarm and focus invariants

Breathing code must have no authority to dismiss alarms, release protection, alter entitlements or change schedules. Pass a read-only protection snapshot and navigation callbacks, not a mutable `DisciplineGateway`, into the player. If status cannot be verified, do not show “off” or “protected”; say status is unavailable or omit the badge.

Home: ordinary entry exits to Home. Pre-focus: an optional secondary “Take a moment first” opens Prepare without starting focus; on return refresh actual protection and let the person explicitly press Start protected focus. No automatic network start at the end of meditation.

Overlay: completion, End, Back and source-panel close must never open the blocked package or release focus. A focus window ending while the practice is active is legitimate: refresh the status; it is not the meditation's achievement. Exit always uses the overlay's existing device-Home path.

Post-alarm: add an optional “Take a quiet minute” to the verified done screen, preserving “Return to my day.” The ringer/service stop order in `dismissAlarm()` must remain unconditional. Do not offer this as a way to finish the dismissal task, and do not treat hard-cap abandonment as completion. Route into the normal app after device unlock; do not display private history or mood input over the keyguard. A malformed entry Intent cannot bypass verification or alter alarm state. Do not mutate future recurrences when leaving meditation.

## 7. Implementation sequence and completion criteria

1. Add catalogue loading/validation, pure schedule/recommendation logic and tests. Resolve no old content automatically.
2. Build local selection/player/history with eight seed entries, source drawer, immediate stop, real local tone and lifecycle handling. Keep network dependencies out.
3. Add third Home card, optional pre-focus entry, reviewed pre-account preview, inline block-overlay entry and post-dismissal entry. Reuse host callbacks and preserve enforcement state.
4. Add accessible strings, large-text layouts, TalkBack behavior, English/Hindi resource structure and honest fallback. Honor reduced motion. No frequent live-region countdown announcements.
5. Run the required automated checks, then physical-device scenarios. Create a release evidence file with raw commands, actual results and outstanding gates. Address regressions; do not report green based on old test counts.

The internal feature is done when all eight entries are reachable under the correct conditions, unsafe/research-only practices cannot be routed, source labels are accurate, session/host transitions meet this contract, and required automated tests pass. Public release additionally needs the content/comfort review, language claims review and physical-device evidence described in the test guide. Proceed with implementation while these release reviews are pending; they are not permission requests to start coding.

No commit/push/deployment unless separately requested. Do not edit secrets or repurpose existing backend endpoints to manufacture completion metrics. Report exactly which tests ran, what failed, what is device-verified and what remains unverified.

## Copyable task for Claude

> Implement the VBT breathing/meditation third pillar in the current Kotlin/Compose Shanivar app using `android-app/CLAUDE_VBT_BREATHING_HANDOFF_2026-09-25.md`. Read its linked catalogue, starter JSON and test guide first. This founder-authorized feature extends the old library deferral only for the specified eight practices. Deliver the local feature, Home/focus/overlay/post-alarm integration, source details, deterministic schedule/feeling recommendations, lifecycle handling and tests. Preserve alarm silence/escape and focus enforcement invariants. Do not use an emulator on this machine. Complete the build and authorized fixes, record actual evidence, and distinguish internal completion from outstanding public-release reviews. Do not commit, push or deploy.
