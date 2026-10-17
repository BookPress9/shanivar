# 0012 — The breathing session owner is process-scoped, not a ViewModel

- **Date:** 2026-09-25
- **Status:** Accepted, implemented 2026-09-25

## Context

[Decision 0011](0011-vbt-breathing-third-pillar.md) adds the breathing pillar, and its
[handoff](../../android-app/CLAUDE_VBT_BREATHING_HANDOFF_2026-09-25.md) suggests
`BreathingViewModel` among the new files, noting that "rotation preserves the session in the
ViewModel".

The same handoff also requires **one** active session across every entry point, and lists three:
the Home card, `BlockOverlayActivity`, and the post-alarm done screen. Those are three separate
Activities in three separate task stacks. The test guide states the requirement directly in S13 —
opening the player from another host must "focus the existing session or offer to end it, never
stack sounds/timers" — and in the session-state contract: "a single active breathing session across
Home, overlay and post-alarm entries".

An `androidx.lifecycle.ViewModel` is scoped to a `ViewModelStoreOwner`, in practice one Activity.
Two Activities get two instances. So a ViewModel satisfies the rotation requirement and structurally
cannot satisfy the single-owner requirement.

## Options

1. **A ViewModel per host.** Simplest, matches the handoff's suggested file list, and is what the
   existing `AccountState`/`ReflectionState` do. Gives the block overlay its own timer and its own
   tone while a Home session is running — the exact failure S13 names.
2. **A ViewModel plus cross-Activity coordination** (a shared repository/singleton holding the
   session, injected into each ViewModel). Satisfies both, at the cost of two layers where the
   second one holds all the state and the first one forwards to it.
3. **A process-scoped `object` as the session owner**, with hosts as thin Compose callers.

## Decision

Option 3. `BreathingSessionOwner` is a Kotlin `object` holding the catalogue, the ephemeral draft and
the active `BreathingSession`, exposed as Compose snapshot state. Every host — `V1Home`'s breathing
route, the block overlay's inline entry, the post-alarm entry — renders the same `BreathingHost`
composable against it.

Rotation is preserved as a side effect rather than as a mechanism: the state never belonged to the
Activity, so there is nothing to restore.

## Consequences

- The singleton outlives every screen, so it must hold **no** `Context`, Activity or view. A Context
  arrives as a method parameter and is used immediately. This is enforced by reading, not by a tool;
  a retained Context here would be a real leak.
- Tests need an explicit reset between cases. `resetForTest()` exists for that and is called from
  `@Before`/`@After` in `BreathingUiTest`. A test that forgets it inherits the previous test's
  session — the standard singleton tax, paid knowingly.
- Only one practice can run at a time per process. That is the requirement, not a limitation.
- The clock is injected (`monotonicMillis`, `wallClock`, `newSessionId`) rather than read directly,
  which is what lets the lifecycle tests drive time without waiting for it.
- It remains true that the player cannot touch enforcement: the owner names no gateway, scheduler or
  service, and `BreathingUiTest.e_noBreathingSourceFileCanNameTheEnforcementSystemsAtAll` fails the
  build if any `Breathing*.kt` file starts to.

## Revisit when

A second concurrent practice ever becomes desirable (it should not — the product point is doing one
thing), or the breathing surface stops being reachable from more than one Activity, at which point a
ViewModel becomes the simpler answer again and this indirection should be removed rather than kept
out of habit.
