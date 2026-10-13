# HANDOFF_NEXT: continue from a phone session (13 Oct 2026)

## Current state
- **Android app** (`android-app/`, Kotlin and Compose). Debug builds turn on two flags (release is unchanged):
  - `INITIATION_V1`: the initiation onboarding flow (guest works end to end; Google and email show "not available yet").
  - `SADHANA_V1`: the Sadhana experience. Living Spine with 33 stages and 7 waypoints, Path (Arrive → Return → See → Choose → Carry), practice detail, prepare, a still-planet player, return, record, practice without the app, deep study, and a NASA Saturn loading surface.
- **Sadhana visual v1 approved by the founder (13 Oct; tag `sadhana-visual-v1`):** 33-stage Living Spine with root pulse, Saturn loading surface. Still pending: vagal layer, self-check, voice copy, practice reviews, device QA.
- Living Spine tip pulse kept as non-rhythmic root branches (founder, 13 Oct; 0017).
- 482 unit tests pass (`testDebugUnitTest`). Review screenshots are on the agent box at `/workspace/sadhana-shots/` (not in the repo).
- Research-only practices can never launch. Release builds refuse practices without a content review manifest.

## Decisions (`docs/decisions/`)
- **0015** Sadhak and Hybrid Monk core ideology (ST-010). Naming rule amended 13 Oct: the main app, website and marketing name no teacher or tradition; only in-app deep study may name traditions, texts and passages; chakras use plain labels outside deep study; private influences (ST-011) stay internal.
- **0016** initiation-v1 wireframe adopted (accounts and cloud sync at launch, Orbit glow, 60 s practice).
- **0017** sadhana-v1 wireframe adopted, with build notes, honesty edits and the 13 Oct decisions: kaleidoscope zoom rejected; NASA PIA06193 Saturn with a steady ring spin; 33-stage spine with founder thresholds 1/5/11/17/24/29/33.

## Open founder questions
1. Vagal firefly layer, self-check and new voice copy: not built; waiting on the founder plus an SOP-05 claims review.
2. (Tip pulse resolved: root form, non-rhythmic.) Spine orientation: app.js lights stages from the top while thresholds count from the base. The build counts from the base; confirm.
3. Progression meaning and visual language sign-off (HANDOFF gate 8).
4. Practices longer than 60 s need a practice review; who verifies chakra source passages?
5. The "Choose" capability needs a Routine-boundary link (not wired yet).
6. NASA image in a commercial app: credit is shown and no endorsement is implied; confirm you're comfortable with that.

## Jira (Shanivar site, project DEV)
| Key | Summary | Status |
|---|---|---|
| DEV-22 | Future sign-in methods | Open (backlog) |
| DEV-23 | Circles visual system | Open (backlog) |
| DEV-24 | Initiation HANDOFF steps 1-5 domain logic (DST parked) | Work in progress |
| DEV-25 | Accounts + cloud sync: real providers, backend /v1, hosting (HIGH) | Open, needs founder |
| DEV-26 | Initiation Compose screens | Work in progress |
| DEV-27 | Sadhana S1 practice domain + research-only lock | Work in progress |
| DEV-28 | S2 attempt timing + events | Work in progress |
| DEV-29 | S3 record, reflection, capability evidence | Work in progress |
| DEV-30 | S4 Living Spine data | Work in progress |
| DEV-31 | S5 screens A (home, spine, path, loader) | Work in progress |
| DEV-32 | S6 screens B (detail, prepare, player, return) | Work in progress |
| DEV-33 | S7 screens C (record, without app, study) | Work in progress |
| DEV-34 | S8 content reviews, /v1 sync, device and accessibility QA | Open, needs founder |

## Next steps
1. Founder review of the debug APK (initiation, then the Sadhana tab after the first minute).
2. Answer the open questions above, then build the vagal layer, self-check and copy if approved.
3. DEV-25: Google OAuth client, email provider, passwordless backend, /v1 routes, hosting and database, privacy notice update.
4. DEV-34: practice and source reviews, physical-device QA (process death, lock, calls), TalkBack, 200% font.
5. HANDOFF step 6 accessibility review for initiation.

## Build
```bash
cd android-app
export ANDROID_HOME=/path/to/Android/Sdk JAVA_HOME=/path/to/jdk17
bash gradlew testDebugUnitTest assembleDebug      # APK: app/build/outputs/apk/debug/app-debug.apk
bash gradlew testDebugUnitTest -PsadhanaShots=/tmp/shots   # optional review screenshots
```
Do not use `--offline`. Commit as "Hybrid Monk <admin@thehybridmonk.com>". Run the decision gate first (AGENTS.md, SOP-00).

## Where things live
- Wireframes: `android-app/wireframes/initiation-v1/` and `android-app/wireframes/sadhana-v1/` (`HANDOFF.md`, `BUILD_PLAN.md`, `exports/`).
- Sadhana code: `android-app/app/src/main/java/com/shanivar/nativeapp/Sadhana*.kt`. Initiation code: `Initiation*.kt`.
- Asset licences: `android-app/app/ASSET_NOTES.md`.
- Stance and safety: `SHANIVAR_STANCE.md`, `research/deep-foundation/PRACTICE_SAFETY.md`, `docs/sop/`.
