# VBT breathing feature: test and release guide

**Visual acceptance reference (2026-09-25):** use the [Steady screen contract](wireframes/steady/README.md) and [screen board](wireframes/steady/exports/index.html). Check the new name, two-step selection, safe-setting gate, static attention marker, source-to-paused return and host-specific exits. Browser QA is prototype evidence only; all native checks below remain required.

Date: 25 September 2026. Applies to [Claude's handoff](CLAUDE_VBT_BREATHING_HANDOFF_2026-09-25.md) and [content version vbt-breathe-2026-09-25.1](../shanivar-vbt/content/breathing-mvp-v1.json).

Status: acceptance specification. The new feature has not been built or tested in the research turn that created this file. Existing repository test counts and old emulator reports are not evidence for it.

## Test boundaries

Use pure JVM tests for recommendation, schedule and state reducers; Robolectric/Compose tests for UI and host integration; physical Android phones for actual audio, screen lock, background behavior, keyguard and alarm/focus interaction. A unit-test pass cannot establish physical-device reliability, medical benefit or translation integrity.

**No emulator or managed-device tasks on this machine.** In particular do not run `pixel2Api34DebugAndroidTest` or an aggregate task that launches managed devices. The historical `LOCAL_TESTING.md` emulator commands are not the workflow for this feature.

## A. Source and content checks

| ID | Check | Required result |
| --- | --- | --- |
| C01 | Validate packaged JSON against the source seed | Eight unique IDs; version retained; changes reviewed rather than silent. |
| C02 | Inspect all VBT source drawers | Work, verse string(s), edition, URL and adaptation boundary present; no technique ordinal presented as a verse. |
| C03 | Inspect `external_grounding` | Modern exercise, no fabricated VBT attribution. |
| C04 | Inspect `breath_word` | 156-A and its variant/manuscript qualification retained; no daily breath target. |
| C05 | Search UI assets for legacy retention/pacing instructions | No prescribed holds, forced emptying, fixed gaps or instructions to delay inhalation. Source notes may discuss omitted retention without prescribing it. |
| C06 | Audit every duration | At 60/180/300 where supported, stages sum exactly; sleep has no spoken/text closing demand. Unsupported durations rejected or explicitly reselected. |
| C07 | Corrupt/miss the asset, version, source or tone | Fail safely; no guessed instructions. Show an unavailable state with Exit; retain a small built-in external-grounding fallback only if its text is independently validated. |
| C08 | Check traditional vs modern origin | Sound/sleep/emotion cards are not labeled breath-control techniques. No 4-7-8/box/sigh imported as VBT. |
| C09 | Hindi selected with untranslated script | Explicit English fallback; no Hindi guidance claim. Reviewed Hindi, when supplied, conveys no-hold/stop permission accurately. |
| C10 | Search compiled strings/assets and telemetry | No dopamine reset, guaranteed sleep, healing, hormone/cortisol inference, breath/HRV measurement, or spiritual-result guarantee. |

Automated schema checks cannot certify clinical suitability or faithful adaptation. Before public release, a reviewer familiar with the cited tradition checks source correspondence and a qualified breathing/health practitioner checks beginner instructions and discomfort handling. Record reviewer, date, exact content version, changes and unresolved points; do not merely write “reviewed.” This is a release-quality requirement from this handoff, not a claim that review has already happened.

## B. Recommendation fixtures

Use an injected clock, preference object and validated catalogue. Never make tests depend on the host's actual timezone or present time.

| ID | Input | Expected |
| --- | --- | --- |
| R01 | Unknown feeling/context, breathComfort=null, no sound consent | `external_grounding` first. |
| R02 | Scattered, breath comfortable, 60 s, safe setting | `natural_anchor` first, then `breath_word`, then grounding. |
| R03 | Scattered, breath uncomfortable, no sound | Grounding; no breath candidate, even if previously used. |
| R04 | Tense, breath uncomfortable, sound allowed | `emotion_space` before grounding; humming excluded because it requires breath comfort. |
| R05 | Tense, breath comfortable, sound denied | Natural anchor; no hum or tone entry. |
| R06 | Agitated/urge, ordinary valid schedule | `emotion_space` first; no advanced intensity escalation. |
| R07 | Low energy at either 08:00 or 23:00, explicit awake goal | Grounding/short arrival; no forced energizing breathwork or automatic sleep assignment. |
| R08 | Overstimulated, no sound consent | Grounding first; listening excluded. |
| R09 | Steady, priorBreathComfort=false | Turn-awareness excluded, including from manual list. |
| R10 | Steady, priorBreathComfort=true and breathComfort=true | Turn-awareness eligible; never compulsory. |
| R11 | Going to sleep, sleepIntent=true, sleepyNow=true, 3 min | `sleep_threshold`, then grounding; silent ending. |
| R12 | Rest-window clock context but no sleep intent or not sleepy | Sleep-threshold excluded. |
| R13 | Daytime clock context + explicit sleep intent/sleepiness | Sleep-threshold allowed; support shift workers. |
| R14 | Unsafe setting or not confirmed | Start unavailable for all practices; Exit works. |
| R15 | 5-minute budget with a 1/3-minute practice selected | Offer supported shorter duration; do not construct a nonexistent five-minute script. |
| R16 | Budget shorter than 60 s or malformed input | No silent extension; explicit shortest-duration choice or exit. |
| R17 | No source-valid candidate / disabled catalogue entry | No invalid practice in results; safe fallback/unavailable behavior. |
| R18 | “Felt worse” for current practice during this visit | Suppress it from automatic recommendation for this visit; no saved emotional profile. |
| R19 | Repeat identical inputs 100 times | Same ordering and explanation every time. |
| R20 | Unsupported feeling/context enum | Unknown handling; no crash or diagnosis inference. |

## C. Schedule and clock fixtures

Boundary semantics follow the half-open intervals in the handoff. Keep schedule context separate from session duration.

| ID | Schedule/input | Expected |
| --- | --- | --- |
| T01 | Wake 07:00, sleep 23:00; 07:00 / 08:59 / 09:00 | Just-woke / just-woke / daytime. |
| T02 | Same schedule; 20:59 / 21:00 / 22:59 / 23:00 / 00:00 / 06:59 | Daytime / wind-down / wind-down / rest / rest / rest. |
| T03 | Wake 16:00, sleep 08:00; 17:00 / 02:00 / 07:00 / 10:00 | Just-woke / daytime / wind-down / rest. |
| T04 | Wake equals sleep; invalid HH:mm; missing one preference | UNKNOWN, no crash or default-biological-state claim. |
| T05 | Awake span 359 / 360 / 1200 / 1201 minutes | Invalid / valid / valid / invalid under the engineering guard. |
| T06 | Quiet time exists, sleep time absent | Do not assume quietTime is sleep onset. |
| T07 | Explicit context conflicts with inferred context | Explicit context wins, subject to sleepIntent/sleepyNow gates. |
| T08 | Berlin spring DST 2026-03-29 and autumn DST 2026-10-25 | Recommendation uses local wall time; no duplicate session or reminder. Monotonic duration stays unchanged. |
| T09 | Berlin → Asia/Kolkata timezone change during active practice | Active practice and remaining duration unchanged; new recommendations use updated local zone on next selection. |
| T10 | Manual clock moved ±12 hours mid-session | No countdown jump or repeated cue. |
| T11 | Schedule edited mid-session | No change to running content; next selection uses new schedule. |
| T12 | Invalid stale preferences loaded | Neutral fallback; never rewrite existing alarm/focus preferences. |

## D. Session state and lifecycle

| ID | Action | Required result |
| --- | --- | --- |
| S01 | Double tap Start | One session ID, timer and tone owner. |
| S02 | Pause at 20 s of a 60 s session, wait 30 s, resume | Approximately 40 s remains; paused interval excluded. |
| S03 | Pause/resume repeatedly or near zero | No negative remaining time, skipped finish or duplicate terminal record. |
| S04 | End/Back in every stage and while tone plays | Cues stop immediately, correct host exit, one ENDED_USER record. No confirmation barrier. |
| S05 | Rotate repeatedly | Preserve state/remaining; no replayed tone, duplicate journal entry or restarted session. |
| S06 | Open Origin & adaptation while running | Session pauses; return waits for explicit Resume. |
| S07 | Background/lock screen in ordinary practice | PAUSED; tone stopped. Resume does not replay missed stage cues. |
| S08 | Sleep practice reaches silent-rest stage; screen locks | No keep-screen-on flag, closing bell, notification or required response. |
| S09 | Return after sleep interval elapsed, process alive | Quiet ENDED_TIMER based on monotonic elapsed time; no claim the person slept. |
| S10 | Kill process while RUNNING/PAUSED/RESTING | Next launch records/recognizes INTERRUPTED once; no automatic playback, completion or resume. |
| S11 | Reboot after partial practice | No persisted elapsedRealtime comparison; no resumed audio. Alarm restoration behavior remains intact. |
| S12 | Call/audio-focus loss/headphones disconnected | Stop tone and pause ordinary practice; no unexpected speaker playback; require explicit resume. |
| S13 | Open same session from another host | One owner. Overlay exit ownership becomes device Home; no stacked timer/tone or blocked-app exit. |
| S14 | Switch practice before starting | New READY state, no orphaned callbacks. Switching during practice requires ending the current session. |
| S15 | Asset read fails / tone cannot load | Safe visible error and fallback; Start for sound practice unavailable, End always works. |
| S16 | Phone battery saver / low memory | No claim of uninterrupted background guidance; graceful pause/interruption. |

Pure tests should cover transition ordering and terminal-event idempotency. Device tests must confirm actual audible stopping. A fake player receiving stop() is not proof that hardware playback stopped.

## E. Alarm/focus integration regressions

| ID | Scenario | Required result |
| --- | --- | --- |
| I01 | Start/end each practice during active focus | Same protected package set and expiry unless the existing focus engine independently changes them; zero release calls from player. |
| I02 | Enter from block overlay; finish, End or Back | Device Home, never target package; no bypass. |
| I03 | Focus naturally expires during meditation | Status refresh reflects expiry; no false credit to breathing or stale protection claim. |
| I04 | Protection status unavailable/offline | No static “App lock off”/“protected” assertion derived from practice state. |
| I05 | Pre-focus session completes or is skipped | Focus is not auto-started; user explicitly starts it through existing controls. |
| I06 | Verified alarm task completes; post-alarm practice offered | Ringer and alarm service stop first. Recurrence advancement remains unchanged. Breathing never gates silence. |
| I07 | Alarm hard cap / unverified dismissal result | No fabricated successful dismissal; no post-success breathing offer. |
| I08 | Breathing content throws during post-alarm navigation | Alarm stays silenced; next occurrence not cancelled. |
| I09 | Alarm fires while local tone/session is active | Alarm behavior wins; practice stops cues and pauses/interruption state is truthful. No alarm muted by media handling. |
| I10 | Decline breathing after alarm | Can leave immediately; alarm does not relaunch. |
| I11 | Locked device + post-alarm entry | Private breathing/history UI waits for normal unlock. Dismissal remains complete even if unlock is cancelled. |
| I12 | Forged/malformed entry Intent | Cannot dismiss alarm, cancel recurrence or release focus. Unknown context safely defaults. |
| I13 | Offline/sign-out/missing account | Breathing works locally; existing account/enforcement rules remain intact. |
| I14 | Clear breathing history/preferences | No mutation of alarm, focus, account or recurrence stores. |
| I15 | Existing scheduled focus starts while practice active | Correct engine protection, player remains independent; no duplicate foreground service changes. |
| I16 | Existing alarm escape/accessibility path | Unchanged and usable; feature adds no physical requirement. |

Add focused negative assertions using a fake gateway/event sink to demonstrate that the player has no write access to enforcement. Re-run existing alarm recurrence, ringer, dismissal, focus scheduling and foreground-blocker suites. If failure predates this change, demonstrate the baseline and document it; do not silently mark the feature green.

## F. Accessibility, privacy and usability

- At 200% text and large display size, Start, Pause/Resume, End and Back remain visible/reachable without clipping or overlap.
- TalkBack reads title, instructions, state and controls in order; countdown does not announce every second. Stage changes may announce politely once.
- Reduced motion/static presentation works; no mandatory animation, flashing, color-only meaning or inhale-sized visual pacer.
- Hearing-impaired and sound-sensitive users can reach text/room alternatives. No microphone, camera or health permissions for this feature.
- Eyes-open practice is supported. Do not instruct sun-gazing, balance-challenging movement or simulated breathlessness.
- Bedtime has no bright completion screen, celebratory haptics, bell, survey or notification. Normal app brightness can be restored on exit; do not change system-wide settings.
- Preference and journal storage match the handoff: max 100 records, local deletion, no feelings/free text/package names or unintended analytics uploads. Check network traffic, not just UI wording.
- Beginner script review explicitly checks “silence while breathing normally,” no mandatory pause, and permission to stop without shame.
- Observed sessions include younger and older adults, different text sizes, Hindi preference and people who decline breath attention. Do not infer support for children from adult testing.

## Commands and evidence

From repository root in PowerShell, after implementation:

```powershell
./android-app/gradlew.bat -p android-app testDebugUnitTest lintDebug assembleDebug "-PshanivarApiUrl=https://hybridmonk-api.onrender.com/api" --console=plain
```

Building with that URL is not authorization to run tests that write to production. For isolated fixture work on an attached USB phone:

```powershell
node android-app/scripts/mock-api.cjs
```

In a second terminal, inspect attached devices and set `ANDROID_SERIAL` to the selected **physical** phone before forwarding/building. Do not use an `emulator-*` serial. The fixture listens on port 5101; confirm actual startup output.

```powershell
& "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" devices -l
$env:ANDROID_SERIAL = "<physical-device-serial>"
& "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" -s $env:ANDROID_SERIAL reverse tcp:5101 tcp:5101
./android-app/gradlew.bat -p android-app connectedDebugAndroidTest "-PshanivarApiUrl=http://127.0.0.1:5101/api" --console=plain
```

Confirm the runner actually targeted the selected phone. Add/adapt new instrumented fixtures so localhost via adb reverse is supported; legacy tests may skip when they expect the emulator-only host `10.0.2.2`. Inspect skip counts. A skipped integration test is not a pass. The new core feature should not require a live backend at all. If backend code is changed for a demonstrated reason, run `npm test` from `backend` as well and document why local-only scope was insufficient.

Automated reports: `android-app/app/build/reports/tests/testDebugUnitTest/index.html`, `android-app/app/build/reports/lint-results-debug.html`, and the connected-device report path printed by Gradle. Save physical-device model/API/build, app commit or dirty-diff identifier, catalogue version, exact command, pass/fail/skip counts and log/screenshot references. Do not copy older counts from CLAUDE.md.

Record runtime tone stop, lock-screen alarm, background, keyguard, process death and focus-overlay behavior on at least one physical phone; before broad release, include a second OEM and the supported Android-version range or explicitly narrow the support claim. No attached device means mark these checks NOT RUN and leave the release gate open; complete independent code/unit/lint work.

## Pilot and public-release gates

First observe 8–12 consenting adults trying the selection and stop flows. Check whether they understand the practice, that they may breathe normally, and that timing is not a performance test. Record discomfort, confusion and exits as useful findings. Fix material issues before a broader pilot.

For a subsequent feasibility pilot, compare a short guided practice with a matched-duration ordinary pause before the same intended action. Measure user-reported usefulness and whether they began/returned to the task, voluntary repeat, opt-outs and “felt worse.” Mood improvement and a completed timer are not proof of attention improvement. Do not claim that emotional/time-of-day matching is validated by this pilot alone.

Release evidence template:

| Gate | Status: PASS / FAIL / NOT RUN | Evidence |
| --- | --- | --- |
| Catalogue/schema/source labels | | |
| Script comfort/tradition review for exact version | | |
| Unit/recommender/state/regression tests | | |
| Lint and APK build | | |
| Physical alarm/focus/background/keyguard tests | | |
| Accessibility and language claim checks | | |
| Local privacy/deletion checks | | |
| Known limitations disclosed | | |

Do not change NOT RUN to PASS because an APK builds. Do not call this a treatment, circadian optimizer or clinically validated intervention when reporting the release.
