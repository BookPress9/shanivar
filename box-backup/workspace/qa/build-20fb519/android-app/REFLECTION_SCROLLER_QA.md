# Reflection scroller — Codex QA and release boundary

Date: 2026-09-10. Kotlin/Compose implementation against Claude's route and native contracts. The implementation plan is REFLECTION_SCROLLER_PLAN.md; preview commands are REFLECTION_SCROLLER_TESTING.md.

## Implemented

- Today entry and explicit AI-processing disclosure before session creation.
- One displayed card, plain text, server-provided response options and optional validated free text.
- Server-confirmed next-card gate shared by Continue and upward gestures; no future cards fabricated locally.
- Answered history, per-card provenance/fallback labels, context timestamp/timezone, old-day label and explicit fresh-session choice.
- Five-response stopping point from the server's session_limit; explicit optional new session.
- Durable encrypted account-bound cache, local drafts and exact pending idempotency key/body. Timeout reconciliation, bounded generation polling and generation-only retry.
- Immediate local Back/offline-practice navigation, explicit server close, confirmed deletion, logout/account-switch clearing. Earlier local wording is retained separately when another device advances the card.
- Internal typed entry marker and read-only protection interface; entry/resume/periodic status checks. No lock-release or alarm-dismissal capability is given to the reflection screen.

## Evidence

Local tests use MockWebServer and in-process fixtures. State checks cover acceptance before/after lost replies, duplicate taps, invalid/oversized text, 422/429/409, polling limits, late account results, disk-write failure, restoration, deletion, fallback labels and parallel-device draft separation. Compose checks cover the unanswered swipe gate, explicit non-disclosing response, draft validation and unconditional exit.

Android 14 Pixel 2 managed-emulator checks exercise actual MainActivity login → Today → reflection → response → Continue → logout over the local HTTP fixture, five-card completion/history, 1.6x text scaling, fallback/saved-date labels and real Android Keystore encryption/account isolation. ReflectionDeviceTest deliberately supplies an ACTIVE status fixture to inspect the blocked-entry label. That label in its screenshots is NOT evidence of a real active app lock.

Screenshots inspected: reflection-card.png, reflection-large-text.png, reflection-keyboard.png and reflection-complete.png in app/build/outputs/managed_device_android_test_additional_output/debug/pixel2Api34/. The file named reflection-keyboard.png shows focused text/draft layout; the software keyboard is not visibly present in that capture, so it does not close the real IME-overlap gate.

Final counts and command result are appended after the final regression run.

## Seven shipping criteria reconciled

| Criterion | Codex evidence / remaining work |
| --- | --- |
| 1. Grounded context and degradation | Per-card provenance takes precedence over session mode; context date/timezone and saved-day choice implemented. Local fixtures verify fallback/date labels. Claude's real chart/transit evidence remains in ai_org/reflection_scroller_squad.md; not rerun as a live native session here. |
| 2. Response gate | Compose/API/state checks prevent advance until server acceptance; non-disclosing option works. Manual TalkBack and hardware-keyboard traversal remain required. |
| 3. Durable acceptance | Local tests verify lost-response reconciliation, stable retry keys, bounded polling, duplicate taps, conflicts and recreated state. Keystore survives a new store instance on Android. Full OS process-kill/network-loss matrix against live backend remains unverified. Authentication still requires re-sign-in after process death. |
| 4. Isolation/deletion | Owner-bound encrypted local cache and logout/switch clearing tested. DELETE is wired. Live backend owner isolation/cascade evidence belongs to Claude's squad; no claim that local UI mocks repeat that proof. |
| 5. Lock/alarm isolation | Read-only API seam and internal marker implemented. Claude's engine and BlockOverlayActivity arrived during this turn; the button and read-only engine adapter are now connected. Real supported-device lock persistence, alarm non-dismissal, permission loss and background/resume enforcement remain unverified on a real supported device. |
| 6. Native/accessibility | Native build, focused tests, lint and emulator rendering run. Large text and visible controls inspected. Manual TalkBack, hardware-keyboard traversal, real IME overlap and supported-device review remain open. |
| 7. Live conversation quality | Fixture text reviewed for rendering only. Representative live-generated session review with model/provider/fixture provenance remains open; backend smoke evidence does not replace it. |

**Release status: Compose implementation delivered for integration and local testing; not shipped.** No backend route, migration, generation service or Claude-owned handoff was edited. The only engine-file changes made by Codex were the BlockOverlayActivity reflection action and a malformed KDoc comment fix in DisciplineApi.kt needed for compilation. No external infrastructure was deployed.
