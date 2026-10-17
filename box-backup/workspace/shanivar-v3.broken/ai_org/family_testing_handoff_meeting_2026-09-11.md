# Staff Manager: internal meeting — handoff for family testing (2026-09-11)

Called by the founder. Goal: get the first version in front of family members to test. Attendees
(reporting up to the Staff Manager, as in every prior phase): Backend & Content squad, Reflection
Scroller squad, Discipline Engine squad, Codex (frontend/UI/UX), QA. Before convening, the Staff
Manager independently re-verified the two claims on the table rather than taking them at face
value — see "Staff Manager verification" under each report.

## Reports

### Backend & Content squad
Hardened backend (bcrypt, JWT, rate limiting, sanitization, real astrology engine, content
gating), 285+ tests passing against the live Supabase DB, all 12 VBT techniques QA-signed-off.
Live and reachable — `/api/health` answers on both localhost and the LAN IP. No new backend work
this cycle.

### Reflection Scroller squad
Backend and native contract were already complete. Nothing new to report — this phase's motion was
entirely on the Codex side (below).

### Discipline Engine squad
Backend (285 backend tests) and native engine (58 JVM tests) complete, per the last handoff.
**Unchanged this cycle**: still confirmed via grep that nothing in `ShanivarApp.kt` calls
`DisciplineEngine.startFocus/releaseFocus` or `AlarmScheduler.schedule()` outside the boot-reschedule
path. The Focus and Rhythm tabs still show their original placeholder copy. This has not moved since
the last report.

### Codex (frontend/UI/UX)
Reports the Compose reflection-scroller integration passing local verification: fixed a CameraX lint
opt-in, added debug-only cleartext networking for a local mock fixture, fixed the instrumentation
test's `BuildConfig` reference. Claims `assembleDebug`, unit tests, lint, and all 5 `pixel2Api34`
managed-device (emulator) instrumentation test classes passing, against a local mock API on
`10.0.2.2:5102`. Record: `android-app/REFLECTION_SCROLLER_VERIFICATION_2026-09-11.md`.

**Staff Manager verification**: re-ran `assembleDebug testDebugUnitTest lintDebug` locally myself
(JVM/static only — the emulator ban on this machine stands, and was not touched). Clean:
`BUILD SUCCESSFUL`, 58/58 unit tests, 0 failures, 0 errors, including three new Compose-UI test
classes Codex added (`NativeUiTest`, `ReflectionUiTest`, `ShanivarApiTest`). Confirmed my own
device-reachability fix from the last cycle (LAN IP in `gradle.properties`) is still intact and
untouched by Codex's changes.

Two caveats worth naming plainly, not to discredit the result but to be precise about what it does
and doesn't cover:
- The emulator run happened in Codex's own environment, not this laptop — the permanent local ban
  is specific to this machine, which crashed once already; I neither ran nor need to run that task
  here. Flagging this only so it's an explicit, confirmed fact rather than an assumption.
- It verified against a **mock** API (`10.0.2.2:5102`), not the real live backend, and on an
  **emulator**, not a physical phone. That means the reflection scroller's UI-to-contract wiring is
  now genuinely proven — but nobody has yet run this app against the real Supabase-backed backend,
  and nobody has run it on real hardware. Family members will hit both of those untested paths
  immediately.

### QA
No new findings this cycle beyond the standing ones already on record (VBT content sign-off,
backend test suite). QA has not yet had a build to test against the live backend on a real device —
nothing to newly report until that exists.

## Staff Manager synthesis: what remains before family testing

**Ready today:**
- Backend: hardened, live, reachable.
- Sign-in, Today, Practice, Reflection Scroller: built, wired end-to-end, and (as of this cycle)
  verified against a mock by Codex and independently re-verified at the unit-test level by me.

**Not ready — in priority order:**

1. **Nobody has run this app against the real backend yet.** Every verification so far is either a
   raw `curl` against the backend (me) or a UI test against a mock (Codex). The first real
   end-to-end run — real login, real DB, real Gemini-backed reflection generation — has not
   happened. This should happen before family members are the ones finding out.
2. **How will family members actually reach the backend?** This is a real, practical gap nobody has
   named yet. Today's setup only works if a phone is on the same Wi-Fi as this machine while
   `backend/` is running — fine for the founder, not realistic for family members testing from their
   own homes. There's leftover deployment infrastructure in `archive_deployment/` (Docker, nginx,
   an AWS script, and what looks like an earlier Render.com attempt) from a prior iteration of this
   project. Standing this back up, or something simpler and cheaper, is a founder decision — see
   below — not something to spin up unilaterally given it touches external services and possibly
   cost.
3. **Discipline Engine (Focus lock + alarms) is still not reachable from the UI at all.** Same gap
   as last cycle, unchanged. If family testing starts before Codex wires this in, testers will see
   "App lock is not connected in this build" and "Alarm not scheduled" — reading as broken features,
   not as a scoped v0.
4. **No real-device verification of anything.** Emulator (this machine: banned; Codex: mock-only)
   is as far as verification has gone. Family members' own phones will be the actual first real
   hardware this has ever run on.
5. **Auth token is in-memory only** — testers will get logged out on any app process death and may
   read that as a bug rather than a known limitation.
6. **No permission-grant onboarding** exists yet for usage access / overlay / exact-alarm / camera —
   relevant once (3) is wired, not before.
7. **The entire `android-app/` directory is untracked in git** (145 uncommitted change entries
   repo-wide, `android-app/` shows as entirely new/untracked). Nothing here has ever been committed.
   Before handing this to anyone outside this room, there should be a real commit checkpoint to
   recover to — flagging this as a recommendation, not doing it myself without being asked.

## Design & wireframes — the founder's flagged surprise, addressed directly

Checked the repo for any design artifact — wireframes, mockups, a design system doc, a Figma link.
**There is none.** Every visual decision in the app today (the Void/Bone/Gold palette, the Saturn-
ring motif, spacing, copy tone) was made directly in Compose code by the agent building the UI, from
`PRODUCT_DIRECTION.md`'s prose description of tone ("Bronze/black structure... direct, warm and
spare") — not from a mockup anyone reviewed and approved first. That's a real process gap, not a
misunderstanding: no one on this roster was ever assigned a design/wireframe step, so it's fair that
this is a surprise rather than something you missed.

Three ways to handle it from here, named plainly rather than picking one for you:
- **Treat the existing build as the wireframe.** It's already a working, high-fidelity artifact —
  cheaper to react to directly than to backfill low-fidelity mockups for something already built.
  Fastest path to family testing; costs nothing extra; risk is that feedback now means reworking
  real code instead of a sketch.
- **Stop and insert a real design pass now**, before any more UI work or family testing — have
  Codex (or a dedicated design pass) produce static mockups for sign-off first. Matches normal
  process; catches tone/structure mismatches before more code sits on top of them; costs real time
  against the "get it to family members" goal.
- **Hybrid (my recommendation)**: don't block family testing on this. Family testing at this stage
  is itself a cheap, fast design review — let your own and your family's reaction to the real app be
  the first checkpoint, with an explicit understanding going in that nothing was pre-approved
  visually. Loop back with Codex on tone/visual changes after that feedback lands, rather than
  guessing at changes to something no one has reacted to yet. The one thing I wouldn't do is let
  this surprise repeat silently again — worth deciding now whether future features get a quick
  screenshot/description checkpoint with you before Codex builds them, or continue as-is.

## Immediate next actions
1. **Founder decision needed**: local Wi-Fi-only testing (fast, free, limited to people on your
   network) vs. standing up minimal hosting so family members can reach the backend from anywhere
   (real setup, possibly small cost, broader reach) — item 2 above.
2. Claude: run one real end-to-end pass against the live backend (not a mock) before family testing
   opens, and report back honestly if anything breaks.
3. Codex: wire Focus/Rhythm to the real Discipline Engine — the one feature gap that will otherwise
   visibly read as broken to testers.
4. Founder: say the word on committing the current uncommitted work as a checkpoint (item 7) —
   not done without being asked.
