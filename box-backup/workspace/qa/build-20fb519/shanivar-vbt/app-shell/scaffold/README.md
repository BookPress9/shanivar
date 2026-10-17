# REFERENCE ONLY — this is not the project Codex will build

**This folder is not the app.** It is a sketch drafted during the App/UI
Agent's architecture analysis, before two things were settled:

1. The architecture decision reconciled to **fully native** (Kotlin +
   Jetpack Compose) rather than a Capacitor hybrid shell — see
   `../architecture-decision.md`. This folder's Capacitor project shape
   (`capacitor.config.ts`, `package.json`, a WebView-hosting `MainActivity`)
   reflects the earlier, hybrid-leaning pass and is superseded.
2. The founder assigned a separate agent (**Codex**) to own the actual
   frontend/UI/UX implementation for Shanivar. Codex will set up and own
   its own real Android project structure — do not extend this folder,
   do not treat it as a starting point to build on.

## What's still worth reading here

Even though the shell shape is superseded, two things sketched in this
folder remain useful as **reference**, not as code to ship:

- **The manifest component shape** (`android/app/src/main/AndroidManifest.xml`)
  — what needs to be declared for the discipline-engine slice regardless of
  shell architecture: a foreground service for lockout monitoring, a
  block-screen overlay activity, a full-screen non-WebView alarm activity,
  an alarm broadcast receiver, and the associated permissions. Reconciled
  against `shanivar-vbt/discipline-engine/platform-research.md` — notably,
  it does **not** declare an `AccessibilityService`, per that research's
  explicit recommendation against using it for app-lockout (Play Store
  policy risk).
- **The call-surface shape** between UI and the discipline-engine module
  (`DisciplineLockoutPlugin.kt`, `AlarmPlugin.kt`) — `getStatus()` /
  `release()` / `scheduleSessionAlarm()` / permission-request methods. The
  literal `CapacitorPlugin` wrapper is irrelevant now, but the shape of
  what the UI layer needs to ask the native discipline module for is a
  reasonable starting reference for whatever the real Compose ↔
  discipline-module boundary ends up looking like.

For the actual creative/functional brief Codex should build from, see
`../CODEX_HANDOFF.md`.
