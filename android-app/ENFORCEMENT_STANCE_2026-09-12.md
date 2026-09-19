# Enforcement stance: alarms and app lock

Issued by the Staff Manager and Project Manager, 12 September 2026, at the founder's direction.
This is the vision both development tracks build to — Claude on the native engine, Codex on the
surfaces. It supersedes any softer reading of earlier documents about how hard these features
should push.

## The stance

**Maximise real friction. Never promise the impossible. Never trap the user in a malfunction.**

The founder's direction is that neither the alarm nor the app lock should be escapable by an
ordinary frustrated person in the ordinary moment of frustration. That is the correct product bet
and we build to it aggressively: every mechanism Android actually gives us, used to its limit.

Two boundaries hold, and they are not negotiable, because breaking either one ends the product
rather than strengthening it.

### Boundary 1 — the honest ceiling

A determined person with time and intent can always get past this. They can force-stop the app from
Settings, revoke usage-access or overlay permission, uninstall, boot into Safe Mode, open
instagram.com in a browser, or pick up a second phone. This is a property of Android, not a gap in
our implementation, and it has been a locked decision since the first platform research.

So: build as if nobody can bypass it, and **write as if anyone could**. "Deliberately difficult to
undo from bed" is the promise. "Impossible" is not, and must never appear in product copy, store
listing, onboarding or marketing. A user who discovers our claim was false stops trusting
everything else the app tells them, including the things that are true.

**The one thing we will not build, and why.** The literal technical answer to "cannot be bypassed at
any cost" is the Device Admin / device-owner API, which can block uninstall outright. We are not
using it. Play Store policy restricts those APIs to genuine enterprise management, and an app that
resists its own removal is behaviourally indistinguishable from stalkerware — it is the single
fastest route to removal from the Play Store and to a reputation this brand could not survive. Our
enforcement stops where the user's ownership of their own device begins.

### Boundary 2 — enforcement must never become malfunction

An escalating alarm that will not stop is not discipline. It is a device screaming at someone who
did the thing we asked, and it is the most damaging bug this product can ship. The founder named
this risk explicitly; it is treated here as a hard safety requirement, not a quality goal.

The same applies to the lock: someone whose step sensor is broken, or who is having a medical
emergency at 6am, must never be held hostage by our product.

---

## Alarms

V1 clarification, 13 September: alarms are required alongside focus modes. The accessible non-walking alternative must never require standing or walking; this supersedes contradictory wording in invariant 8 below. Sensor failure exposes that alternative immediately. Read [current v1 scope](INITIAL_MVP_SCOPE_2026-09-13.md); retain all ringer stop, hard-cap and offline guarantees.

### Escalation
- Start audible but not shocking: roughly 40% of the device's maximum alarm volume, with vibration
  from the first second.
- Ramp in steps every ~20 seconds to full volume by roughly 100 seconds. Stepped, not smooth — a
  perceptible increase is what creates urgency; a slow fade reads as a malfunction.
- Volume ramps within the **alarm stream**, which is exempt from ringer mute and Do Not Disturb.
  Raising the device's own alarm-volume *setting* is a separate, setup-time opt-in — changing a
  system setting a user didn't consent to is user-hostile and surprising, and we ask instead.
- The alarm screen must warn when alarm-stream volume is zero. A silent alarm and a broken alarm are
  identical from the bed.

### The invariants that stop it (safety-critical — treat any violation as a release blocker)
1. **A verified dismissal silences the ringer synchronously, before anything else happens** — before
   navigation, before any network call, before analytics. Stopping is never downstream of work that
   can fail.
2. **Silencing is never gated on the backend.** Telemetry and dismissal recording are
   fire-and-forget. A dead network must not keep a siren running.
3. **An independent watchdog enforces a hard maximum ring duration** (15 minutes), stopping audio
   regardless of what the state machine believes. This is deliberate redundancy: it must stop the
   alarm even if the dismissal logic above is itself broken.
4. **Sensor unavailable is never a trap.** If the step detector is missing or the accelerometer
   fails, the accessible alternative appears immediately — not after a timeout, not buried. A person
   must never be unable to stop an alarm because of hardware they didn't choose.
5. **A crash in the dismissal flow stops the ringer.** Audio teardown belongs in a `finally`, and in
   `onDestroy`, so process death cannot orphan a looping alarm-stream player with no UI to stop it.
6. **Wake locks are timeout-bounded.** A crash between acquire and release must not flatten the
   battery.
7. **Dismissal is idempotent per occurrence.** A verified occurrence never resumes ringing.
8. **There is always an escape, and it always costs something.** After 60 seconds, a visible "I
   can't complete this" path that still requires standing up. Every use is recorded as a bypass with
   a reason code. If more than about 30% of dismissals take it, the mechanism has failed and we
   change the mechanism — we do not remove the escape.

### What we do build to make it hard to stop
Full-screen intent over the lock screen; Back swallowed; relaunch the alarm surface if the user
leaves via Home (the overlay permission we already hold is what makes this legal from the
background); an ongoing, non-dismissible notification; a foreground service holding the process up;
reschedule after reboot. All of this is already partly built — see the native contract.

---

## App lock

### The stance
The chosen app stays shut for the whole window. No snooze, no "just five minutes", no bypass
affordance anywhere on the block surface. Ending a session early is a deliberate act performed
inside Shanivar, not a button on the screen standing between the user and Instagram.

### The enforcement ladder
1. Foreground detection via `UsageStatsManager`, polled tightly during an active session.
2. The block surface launched over the offending app — this is what the overlay permission buys.
3. Back press does not return to the blocked app.
4. Leaving via Home re-triggers detection and returns the block surface on the next poll.
5. An ongoing notification for the duration of the session.

**Known and accepted limits, to be stated plainly rather than papered over:** detection is polled,
so a brief flash of the blocked app before the block surface appears is expected and is not a bug.
The browser route (instagram.com) is not blocked — closing it would require either a VPN service or
an accessibility service, and the latter is permanently off the table for this product. A second
device is not blocked. We measure how often these routes are used; we do not pretend they don't
exist.

### The redirect — the most important part of this section
A block screen that only says "no" spends the user's frustration and gets nothing back. The founder's
direction is to convert that moment, and this is the highest-leverage idea in this document.

When someone hits the lock, they are already holding the phone, already wanting *something*. The
block surface should offer a real alternative as the primary action, not a consolation:

- **A short practice** — three to five minutes, the thing they actually opened the phone hoping to
  feel.
- **Reflect with Shanivar** — already wired (`ReflectionNavigation.fromBlockedSurface()`), a
  question waiting for an answer.
- **A blog piece** — once the content exists. Until then it does not appear; a dead link at the
  moment of peak frustration is worse than one fewer option.

Design notes for Codex: the constructive door should be the visually primary action, above "back to
Shanivar". Rotate what's offered so the same screen at the same moment doesn't become wallpaper. And
the copy holds the Hybrid Monk register — direct and warm, never smug, never scolding. The user is
not being punished; they asked us to hold this line for them, and the screen should sound like it
remembers that.

---

## What we will never do
Claim the lock or alarm is impossible to bypass. Use Device Admin to resist uninstall. Fake system
UI. Hide or obstruct the path to app settings. Shame the user in copy. Charge to unlock early — a
fee to break a lock was explicitly ruled out and remains ruled out.

## Ownership
- **Claude (native engine):** escalation, every invariant in the list above, the watchdog, the
  accessible fallback path, relaunch-on-leave, reason codes and telemetry.
- **Codex (surfaces):** the alarm screen, the block surface and its redirect, permission onboarding,
  and all copy — held to the honest-language rule throughout.
- **Founder:** the blog content that makes the third door real, and the decision on whether raising
  system alarm volume is offered at setup.

## Test matrix before any of this is called done
Dismissal verified → silence within one second. App killed mid-alarm → no orphan audio. Step sensor
absent → alternative offered immediately. Hard cap reached → stops and records. Network offline →
dismissal still silences. Escape hatch → works, and is logged. Each of these is a real-device check;
none can be satisfied by a unit test.
