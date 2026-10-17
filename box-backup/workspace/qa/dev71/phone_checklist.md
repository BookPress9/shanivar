# Founder phone checklist: DEV-71 protection (18 checks)
For: Shubham · About 2.5 h of active time, plus one overnight (D2) and one DST night (R4b, **Sun 25 Oct 2026**). No computer or cables needed.
Before you start:
- [ ] Install the debug build QA sends you (release-candidate hash: ________). Write down the phone model and Android version: ________
- [ ] Install **YouTube** and stay signed out. It is the test app, so no personal app list is involved.
- [ ] Shanivar: grant Usage access, Display over other apps, Alarms & reminders and Notifications. Keep automatic time and time zone on.
- [ ] For each line: tick PASS or FAIL and take a screenshot (Power + Volume down). If something looks wrong, a 10-second screen recording is better.

Tip: a "test window" means Routine › App Protection › a window starting 2 minutes from now and lasting 5 minutes, with YouTube selected.

**Permissions**
- [ ] P1 Start a test window. Settings › Usage access › Shanivar **off**. Open YouTube, then Shanivar. ✔ Shanivar says it needs access (not "Active"). Note what the Shanivar notification says. Turn access back on afterwards.
- [ ] P2 Same, with **Display over other apps** off. ✔ Not "Active"; "Stop protection now" still works.
- [ ] P3 Settings › Apps › Shanivar › **Alarms & reminders off**. Save a window. ✔ Window is not "Ready"; the app tells you what to turn on.
- [ ] P4 (fresh install, or Clear storage) In onboarding, **refuse** Usage access and overlay. ✔ You can finish; it says saved but not active and shows how to fix it later.

**Sessions**
- [ ] S1 Test window: open YouTube before, during and after. ✔ Blocked only during (a short flash is OK); free after.
- [ ] S2 Two windows back to back (e.g. 5 min then 5 min). ✔ Still blocked after the first ends.
- [ ] S3 A 22:00→06:00 window. Check at 23:59 and 00:01. ✔ Blocked both times.
- [ ] S4 Airplane mode on, signed out, mid-window: tap **Stop protection now**. ✔ YouTube opens at once; the block screen never offers "snooze" or "5 more minutes".
- [ ] S5 Mid-window open YouTube and try each button (Return to my day / one-minute pause / short practice / Back). ✔ Each lands on the phone's home screen, never back in YouTube. Note what "short practice" shows.

**Recovery**
- [ ] R1 Restart the phone mid-window and unlock it. ✔ YouTube still blocked, no sign-in needed. Restart again after the window ended. ✔ Not blocked.
- [ ] R2 (a) Swipe Shanivar out of Recents mid-window. ✔ Still blocked. (b) Settings › Apps › Shanivar › **Force stop**. Note whether YouTube opens, then open Shanivar and note what it says.
- [ ] R3 Unplug, screen off for 1 h across a window START, then use YouTube. ✔ Blocked only until the window's own end.
- [ ] R4 (a) Mid-window, change the time zone by hand to India. Note the next window time shown. (b) **Night of Sat 24 → Sun 25 Oct**: window 01:30–03:30. ✔ One start and one end, at the right local times.
- [ ] R5 Reinstall the same build over itself mid-window. ✔ Window still active and next one armed.
- [ ] R6 Automatic time off. Mid-window, move the clock **back 3 h**, then forward past the end. Note what happens. ✔ Stop always works. Turn automatic time back on.

**Phone-specific**
- [ ] D1 With battery saver on, run a 2-hour window and check YouTube every ~30 min. ✔ Blocked throughout, or Shanivar shows that protection stopped.
- [ ] D2 Overnight window, phone unplugged in low-power mode. ✔ Blocked in the morning until the end time.
- [ ] D3 If you have a second phone (different brand or Android version), repeat P1–R6 on it. DEV-71 needs 2 phones to sign off.

Send back: screenshots + this list with ticks to QA (Shanivar Tester). QA files FAILs as bugs, with severity.
