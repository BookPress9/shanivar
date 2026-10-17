# Shanivar VBT — App Shell Architecture Decision

**Owner:** App/UI Agent
**Status:** Decided and reconciled. This is a backend/platform-capability decision (what native constraints the build must live inside), not a UI-implementation decision — the actual frontend build is owned by a separate agent (Codex); see `CODEX_HANDOFF.md`.
**Decision:** **(A) Fully native Android (Kotlin + Jetpack Compose).**

This reverses an earlier draft of this document, which recommended a Capacitor hybrid shell. The reversal is deliberate and explained in full below — it is not a coin-flip, it is a response to two things that landed after the first pass: the Discipline Engine Agent's platform research, and the founder's decision to assign a separate agent (Codex) to own the actual frontend build.

---

## 1. The two things that changed the answer

**(a) Discipline Engine Agent's research** (`shanivar-vbt/discipline-engine/platform-research.md`) confirms both native-only features are real platform constraints, not implementation details:
- App-lockout must use `UsageStatsManager` polling + a `SYSTEM_ALERT_WINDOW`-class overlay (explicitly **not** `AccessibilityService` — Play Store tightened autonomous accessibility-API use against "convenience" apps as of Jan 28, 2026, further restricted under Advanced Protection Mode in March 2026; an app-blocker doesn't qualify as a genuine accessibility tool and this is a real rejection/suspension risk).
- The alarm needs `AlarmManager.setAlarmClock` + a `Notification.setFullScreenIntent` full-screen Activity + a foreground service to stay alive over the lock screen, gated on Play Console's "declared core functionality" allowance for alarm apps.
- Neither is reachable from a WebView. The research's own framing: hybrid "is not ruled out entirely" if the discipline slice is a native module — but it treats **"discipline engine = native module, minimum" as a hard constraint on the shell**, not a nice-to-have.

**(b) Scope change**: a separate agent (Codex) now owns the actual frontend/UI/UX implementation for Shanivar, consuming this track's backend logic and architecture. The original hybrid recommendation's central argument — *the existing Next.js session player (`frontend/src/app/(shanivar)/practice/page.tsx`) is a real, working, non-trivial piece of UI, and wrapping it in a WebView avoids re-building it* — no longer holds. Codex is building the actual UI fresh regardless of which shell architecture this doc picks. There is no reuse saving left to weigh against native's costs, because the thing being reused (the existing web session player) is not what Codex will ship — Codex builds new UI either way.

With the reuse argument gone, what's left is a straight comparison of the two remaining costs, and native wins:

## 2. Why native, now that reuse is off the table

- **No WebView/native boundary at exactly the spot that needs to be most reliable.** Under a hybrid shell, the discipline-critical UI moments — the lockout status pill, the hard-exit confirmation, the alarm-to-session handoff — would straddle a bridge call between WebView JS and native Kotlin. Platform-research.md separately flags OEM background-kill behavior (Samsung "Sleeping Apps," MIUI autostart restrictions, etc.) as a real risk to both features even in a pure-native build; adding a WebView-reclamation failure mode on top of that, for zero UI-reuse benefit, is a cost with no offsetting gain.
- **Single-process, single-lifecycle app.** A fully native Compose app lets the UI, the `LockoutMonitorService`, and the alarm's foreground service share ordinary Android process/lifecycle semantics directly — no plugin-bridge serialization, no "is the WebView process still alive" uncertainty.
- **Cleaner Play Store core-functionality story.** Platform-research.md notes the alarm's full-screen-intent auto-grant and exact-alarm scheduling depend on Play Console's "declared core functionality" allowance for alarm apps. A fully native app is a more natural single-purpose declaration than a web-wrapper-plus-plugin having to justify itself as an "alarm app" to reviewers.
- **Consistency with the discipline engine's own hard constraint.** Since the lockout/alarm code must be native regardless of shell choice, and the UI is being freshly built regardless of shell choice, the only thing "hybrid" would still be buying is... a WebView loading marketing/informational screens. Not worth the two costs above for that.

## 3. What this does NOT mean

This is not a claim that the existing Next.js frontend (`frontend/src/app/(shanivar)/`) was wasted work, or that shanivar.com stops existing. It stays the web product. This decision is scoped to the **Android app** specifically. Codex's native build can and should draw on the existing product's information architecture, session-player interaction model, and content structures as a reference (see `CODEX_HANDOFF.md` and `session-player-spec.md`, both written against the patterns already proven in `practice/page.tsx`) — "native" means new platform, not "ignore everything already learned about what works."

## 4. What's fixed vs. what's Codex's to design

Fixed by this track (not Codex's to redesign):
- Platform: native Android, Kotlin + Jetpack Compose.
- The discipline-engine native module boundary and its recommended implementation per `discipline-engine/platform-research.md`: `UsageStatsManager` + overlay for lockout (not `AccessibilityService`); `AlarmManager.setAlarmClock` + full-screen intent + foreground service for the alarm; physical/verified dismissal (QR/puzzle/movement) as the alarm activity's gating condition.
- The assessment intake/routing contract and technique content schema (owned by other agents — see `CODEX_HANDOFF.md` §3 for exact locations).

Codex's creative and implementation territory:
- Actual Compose screen layouts, navigation structure, animation implementation, component architecture.
- Visual execution of the Saturn/discipline direction (mood and constraints given in `visual-identity.md` and summarized in `CODEX_HANDOFF.md` — not a locked design system).
- Onboarding and session-player screen composition — the wireframes in `onboarding-flow.md`/`session-player-spec.md` describe the functional contract and emotional arc, not a layout Codex is bound to reproduce literally.

## 5. Residual open items for the Discipline Engine Agent / Staff Manager

- Platform-research.md itself raises founder-level open questions (accessibility-API stance never revisited, "non-dismissible" marketing language, second lockout target after Instagram, onboarding friction tolerance for the unusual multi-step permission grants) — these are that agent's/the Staff Manager's to resolve, not blocking this shell decision.
- `minSdkVersion` and exact `UsageStatsManager` polling cadence are Discipline Engine Agent implementation details, not an App/UI Agent call — flagged, not decided, here.
