# Founder phone checklist: DEV-75 alarms (14 checks)
For: Shubham · About 1.5 h of active time, plus one overnight (D3) and the DST night (A6, **Sat 24 → Sun 25 Oct 2026**). No computer needed.
Before you start:
- [ ] Debug build from QA installed (hash: ________). Phone model / Android version: ________
- [ ] Alarm volume up. Shanivar has Alarms & reminders, Notifications and full-screen alerts allowed.
- [ ] For audio checks, take a short **video**. For the rest, a screenshot. Tick PASS or FAIL.
- [ ] Two checks are **expected to fail** today (A7 accessible option, A10 locked reboot). Please run them anyway; they confirm bugs DEV-93 and DEV-95.

**Ringing**
- [ ] A1 Alarm 2 min from now, lock the phone. ✔ The alarm screen appears over the lock screen and the sound gets louder.
- [ ] A2 Swipe Shanivar out of Recents, unplug, screen off for 1 h before an alarm. ✔ Rings on time.
- [ ] A9 Let one alarm ring without touching it. ✔ It stops by itself at 15 min.
- [ ] D2 Do Not Disturb on, ringer on vibrate, phone across the room. ✔ You hear it.

**Repeat and recovery**
- [ ] A3 (a) Restart and unlock before an alarm. (b) Reinstall the same build. (c) Let a daily alarm ring and dismiss it. ✔ Each time the card shows tomorrow's "Next" time and it rings again.
- [ ] A10 Alarm 10 min ahead → restart → **don't unlock** → wait. Note whether it rings. (Expected to fail: DEV-95.)
- [ ] D4 Power off before an alarm, power on after its time. ✔ No late ring; next one is armed.
- [ ] A6 Daily alarm at **02:30**, night of Sat 24 → Sun 25 Oct (clocks go back at 03:00). ✔ Exactly one ring. Also: change the time zone by hand once, and note the "Next" time.

**Permissions**
- [ ] A4 Schedule an alarm, then Settings › Apps › Shanivar › **Alarms & reminders off**. Reopen Shanivar and screenshot the alarm card. Wait past the time. Note: rang / didn't ring, and what the card said. (The card is expected to wrongly say "Next: …": DEV-94.)
- [ ] A5 Notifications off for Shanivar → try to schedule. ✔ It refuses with a clear message.

**Dismissing**
- [ ] A7 With "accessible movement" **off** in settings: when the alarm rings, look for an option that needs no walking. Screenshot. (Expected to fail: DEV-93.) Then try the QR and photo options once each, and check the Gallery: ✔ no photo saved.
- [ ] D1 Walk the step target. ✔ It dismisses at the target.
- [ ] A8 Start a 60-second practice, with an alarm set to ring during it. ✔ Practice stops; alarm rings.

**Phone-specific**
- [ ] D3 OEM phone (if you have one) with battery saver on: overnight alarm. ✔ Rings. DEV-75 needs 2 brands / Android 12–15 to sign off.

Send back: the ticked list + screenshots/videos to QA (Shanivar Tester).
