# Browser / package verification

Prepared 30 September 2026 for Shanivar V3.

## Verified in the standalone package

- PASS: `app.js` parses with Node syntax checking.
- PASS: Welcome and Rhythm copy use the new serious-work stance.
- PASS: Dashboard is absent from the primary app shell.
- PASS: Rhythm contains only wake and intended-sleep inputs; the old schedule selector, `TWO ANCHORS` label and “Not someone else's routine” copy are removed.
- PASS: App protection defines categorized app choices.
- PASS: App protection supports multiple protection windows, a bottom-right `+` add control, Every day / Weekdays / Weekends / Custom week recurrence, and Monday-Sunday custom selection.
- PASS: Routine has an Edit Routine flow for wake/sleep anchors and personal activities.
- PASS: Activity name, time, duration and Every day / Weekly / Custom occurrence are editable; custom weekdays are selectable.
- PASS: Gym, Read a book and Extend Sadhana quick adds work, and a generic bottom-right `+` adds another activity.
- PASS: Steady has one foundation anchor: natural breathing.
- PASS: The old object/chair anchor choices are absent.
- PASS: The Steady player includes Saturn motion and explicitly states that motion does not pace breathing.
- PASS: CSS defines separate light-paper and black-card foreground systems so light cards use dark text and dark cards use light text.
- PASS: Reduced-motion preference disables Saturn animation.
- PASS: Repository Playwright dependency was resolved from the nested V3 package and the browser checks ran on 30 September 2026.
- PASS: Welcome rendered, the obsolete onboarding schedule selector was absent, and the bottom-right add-window interaction created a second protection editor.
- PASS: The browser added Gym, renamed it to Evening Gym, changed its time and recurrence, saved it, and rendered it in the Routine timeline.
- PASS: Steady remained disabled until stationary-setting confirmation, then rendered the Saturn player and paused correctly.
- PASS: Welcome, Rhythm, Setup, Routine, Edit Routine, app protection, Sadhana, Ready and Practice produced no page-level overflow at a 375 x 812 viewport.
- PASS: Routine, Edit Routine, app-protection and paused-practice phone captures were rendered for visual inspection. The initial practice timer displays `1:00`.

## Native implementation still not established by this browser wireframe

This package does not test actual Android alarms, app blocking, accessibility/usage permissions, persistence, notifications, background scheduling, sensor verification or physical-device behavior.

Run:

```bash
node verify.mjs
```

Inside the original repository, the script also attempts the existing relative Playwright dependency and runs browser checks/screenshots if that dependency is available. In a standalone folder it still runs static assertions without installing anything.
