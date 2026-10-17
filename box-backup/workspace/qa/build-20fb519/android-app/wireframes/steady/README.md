# Steady by Shanivar ? visual implementation handoff

Prepared 25 September 2026. Status: functional browser wireframe, proposed product name and visual direction. This supplies the missing visual contract for Claude; it is not a native app implementation or a public release.

## Name and design idea

**Steady**  
Descriptor: **Breathing & attention**  
Brand line: **Breathe. Return. Begin.**  
Home supporting copy: **A short practice for this moment.**  
Primary entry: **Find my pause**

Steady expresses the patient, repeated effort in Shanivar's chosen Shani-inspired discipline framing. The movement is returning attention, not achieving a perfect state. It works in ordinary English without requiring religious knowledge or belief. It also accommodates the sound and external-attention options in this collection. This is an original naming proposal, not a scriptural term or a trademark-availability claim.

Use **Steady** as the feature name and **Shanivar** as the app name. Do not rename catalogue IDs, data classes or storage namespaces just to match marketing. ?Breathe & reset? was the earlier working label and is superseded in user-facing English copy. Preserve the optional Shani wording setting; no deity imagery or spiritual assertions are required in the default flow.

## Review artifacts

- [Interactive prototype](index.html): opens directly from disk, works offline, uses local scripts and fonts.
- [Full screen board](exports/index.html): 17 captured screen states with links into the prototype.
- [Desktop workspace](exports/workspace.png) and [mobile workspace](exports/mobile.png).
- [Browser verification](QA.md), [behavioral build specification](../../CLAUDE_VBT_BREATHING_HANDOFF_2026-09-25.md) and [native testing guide](../../VBT_BREATHING_TEST_GUIDE_2026-09-25.md).

The left rail is a designer navigation tool, not app navigation. The right rail explains behavior and offers state simulations; neither belongs in the shipped app. Fixture times and protection states are explicitly marked as simulated. Do not copy them as hardcoded app status.

## Screen contract

| Screen | Native behavior and visual priority |
| --- | --- |
| Home | Third card below existing alarm/focus utilities. Steady name, plain descriptor, one entry action. No eight-card home library. |
| Choose | First step: optional feeling, available time, optional context. No selected feeling by default; Not sure is valid. Context override stays available. |
| Comfort | Second step: explicit breath choice, separate sound consent, prior comfort only where relevant. Going to sleep reveals intent/sleepiness confirmation. |
| Suggestion | One primary card and up to two alternatives. Explain the inputs behind the suggestion. All suitable options is secondary. |
| All suitable | Apply the same hard eligibility filters. No advanced breathing or retention practices. |
| Ready | Name, selected duration, source disclosure, safe-setting checkbox, disabled Begin until confirmed, Come back later always available. Show a supported shorter duration explicitly; do not silently increase the user's budget. |
| Active | Static concentric circles and attention point. Real stage text from the starter JSON. Remaining time is quiet; End is top-right and Pause is a full-width action. Never pulse the circle as a breathing ratio. |
| Paused | Explain that time is retained. Resume and End; no automatic restart. |
| Source | Exact source metadata and adaptation note. Modern fallback is never attributed to VBT. Opening during a session pauses it; closing goes to Paused. |
| End | Timer ended versus Practice ended. Optional Helpful / No change / Felt worse; one return action. No score, streak, confetti or claim of successful meditation. |
| Schedule | Optional wake and intended-sleep preferences. Explain local schedule inference; do not substitute quietTime. Editing must not change alarms. |
| Sleep | After instructions, dim silent resting state. No ending bell, vibration, mandatory feedback or screen-on requirement. |
| Blocked entry | One-minute limit in this host. Both stopping and leaving return to device Home. No breathing-based unlock. |
| Alarm done | Only after verified dismissal and keyguard unlock. Optional entry; existing exit remains. |
| Pre-focus | Prepare optionally. Returning does not start focus; the person still chooses Start protected focus. |
| Device Home | Routing schematic, not a new app screen. |
| Unavailable | Clear load failure, retry or leave. No stale invalid content. |

## Visual and accessibility specification

Use existing Compose theme primitives; do not introduce a parallel design system. Reference colors follow the current V1 wireframe: background #101211; raised surface #1B1E1B; text #F2F0E7; secondary text #AEB6A8; sage action #D1DCAA; bronze accent #C3A776. The practice marker is static; no mandatory animation or haptics.

Reference phone: 390 ? 844 CSS pixels. Native layouts must adapt to small devices, system bars, landscape and large text rather than fixing those dimensions. Horizontal padding approximately 22 dp; card radius 18 dp; primary buttons minimum 48 dp tall. Web chips are at least 44 CSS pixels; native touch targets must be at least 48 dp. Browser micro-label sizes are annotation/style guidance, not an excuse for inaccessible native text. Use scalable sp, allow wrapping, and avoid clipping at 200% font scale.

Use accessible single-selection controls for feelings, duration and context; expose selected state. Checkboxes have full-row touch targets. Give Back and End explicit accessible labels. Decorative circles have no semantic focus. Do not announce countdown updates every second. Source links are named and keyboard accessible. Preserve English fallback disclosure where reviewed Hindi content does not exist.

Screen content scrolls when needed. Controls must remain reachable at large text sizes; keep End visible in the native player using a stable top bar. Do not hide safety controls behind gestures. Display live verified focus status only; the native player may omit unavailable protection status.

## Prototype scope and implementation boundaries

Implemented here: screen navigation, feeling/time/comfort choices, safe-place gate, catalogue-backed scripts, eligibility filtering, schedule entry, timer/pause/resume, source pause, simulated blocked/alarm/focus hosts, optional feedback and visit-level suppression after Felt worse. All data is in memory, not persisted or uploaded.

Prototype shortcuts: direct rail navigation can bypass setup to inspect states; this is not permission for native routes to do so. The sound practice is a visual/content preview; no audio asset or tone playback is provided here. Sleep simulation shows the silent state without implementing Android background lifecycle or a completion callback. No native history, process-death recovery, keyguard check, global session owner, physical alarm, app enforcement or backend call is implemented. Browser tests do not prove those capabilities. Follow the behavioral handoff where this illustration is less complete.

The public source links require internet if opened; the prototype itself does not. Source metadata is copied from the versioned starter JSON into catalog.js for local-file operation. Regenerate that file if the source catalogue changes; do not edit its scripts independently.

## Claude implementation order

1. Read this visual reference alongside the behavioral handoff and inspect Home ? Choose ? Comfort ? Suggestion ? Ready ? Active ? End.
2. Use Steady consistently in user-facing labels; retain stable internal IDs and existing work in progress.
3. Implement the Compose screens using existing theme components. Split selection into the two steps shown here; no new navigation tab or account gate.
4. Wire the real session state and host-specific exits. Source opening must pause; the overlay must return to device Home; post-alarm entry must follow verified dismissal.
5. Implement schedule, sleep, error, interruption, English fallback and accessibility states. Preserve all test requirements in the main guide.
6. Compare native screenshots with this reference and report intentional adaptations for font scaling/device size. Run JVM/lint/build checks, then physical-device checks. No emulator on this machine.

Reproduce browser verification with `node android-app/wireframes/steady/verify.mjs`. It uses the existing local Playwright installation under frontend/node_modules, solely as a development tool. No dependency is added to the app. Do not commit/push or deploy without a separate instruction.
