# 0002 — Enforcement never uses Device Admin or Accessibility Services

- **Date:** 2026-09-12
- **Status:** Accepted

## Context

Shanivar's premise is that a locked app stays locked and an alarm cannot be casually dismissed. The
strongest technical means of delivering that on Android are:

- **`DevicePolicyManager`** (Device Admin / device owner) — can enforce kiosk mode, block app
  uninstall, and genuinely prevent exit.
- **`AccessibilityService`** — can observe every window transition and reliably detect a blocked app
  opening, far more precisely than our current polling.

Both are materially better at the stated job than what we ship. The founder direction is explicit
that the blocked app should be inaccessible "at any costs."

## Options

1. Device Admin / device owner for true kiosk enforcement.
2. AccessibilityService for reliable app-launch detection.
3. `SYSTEM_ALERT_WINDOW` overlay plus usage polling — weaker, with a visible latency flash.

## Decision

Option 3. Both stronger mechanisms are refused, permanently, and not as a phased approach.

## Consequences

What this costs us — stated plainly, because it is a real cost:

- There is a **brief flash of the blocked app** before the overlay raises. We tell users this
  honestly rather than hiding it.
- A determined user can force-stop the app, revoke the overlay permission, or disable notifications,
  and all enforcement ends. **We cannot deliver "at any costs," and we must never claim we can.**
- Detection is less reliable than an AccessibilityService would be.

What it buys, and why the trade is not close:

- **Play Store survival.** Google restricts AccessibilityService to genuine accessibility use and
  removes apps that repurpose it for monitoring or blocking. Device Admin for non-enterprise
  lockdown is similarly treated. A removed app enforces nothing for anyone.
- **Not being stalkerware.** These are the exact APIs abused by coercive-control software. An app
  that takes ownership of someone's device and blocks uninstall is one stolen phone, or one
  controlling partner, away from doing real harm. Shanivar is a tool a person points at themselves,
  and it must stay one they can put down.
- **Uninstallability is a feature.** Discipline that depends on the user being technically trapped
  is not discipline; it is a hostage situation that ends in a factory reset and a one-star review.

The product answer to the gap is **not** stronger enforcement. It is making the block screen a door:
the moment of friction redirects into a practice. See the block-screen inversion in the enforcement
stance.

## Revisit when

- Google introduces a first-party, policy-sanctioned focus/blocking API with real teeth
  (the direction `DigitalWellbeing` and app-timer APIs have been drifting).
- Shanivar ships a genuine enterprise or institutional offering where the device is *organisation*
  owned, provisioned as device owner with informed consent, and the user is not the subject of
  someone else's control. That is a different product with a different consent model — it does not
  reopen this decision for the consumer app.
