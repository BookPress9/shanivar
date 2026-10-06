# Routine V3 UI copy inventory

Extracted 6 October 2026 from commit `6d26bb2` (branch `main`). Every string below is quoted verbatim from the Kotlin source. `file:line` refers to the **original** file under `android-app/app/src/main/java/com/shanivar/nativeapp/`; the snapshot copies in this `source/` folder have identical line numbers at this commit. If the original has changed since, the original wins.

**Availability warning.** This copy is on `main` but behind the build flag `ROUTINE_V3 = false` (`android-app/app/build.gradle.kts:33`). Real users get the older `OnboardingFlow.kt` (`ShanivarApp.kt:182-218`). The strings below are therefore **implemented, not released and not device-tested** (DEV-7, `android-app/ROUTINE_V3_DEVICE_QA_2026-10-05.md`, has no results recorded). Quoting them as "what the app says today" in public is not allowed; quoting them as brand language and product direction is.

Conventions:
- `\n` = a line break inside a heading. `${...}` = a value filled in at runtime (times, counts). `·` and `—` are as in source.
- "Heading", "Eyebrow", "Card title", "Button", "Body", "Message" describe the UI role as read from the composable used (`Heading(...)`, a letter-spaced primary `Text`, `SectionCard(...)`, `PrimaryAction`/`TextButton`/`OutlinedButton`, plain `Text`, or a state/error message).
- Marketing-usable lines (stance/voice lines, not mechanics) are marked **★**.

---

## 1. Welcome (onboarding step 01)

Source: `RoutineOnboardingFlow.kt` `WelcomePage` (lines 272-290) plus shared header/step label.

| Role | Verbatim copy | file:line |
| --- | --- | --- |
| Wordmark | "SHANIVAR" | RoutineOnboardingFlow.kt:112 |
| Step label | "01 · Welcome" | RoutineOnboardingFlow.kt:264 |
| Eyebrow ★ | "DISCIPLINE FOR REAL LIFE" | RoutineOnboardingFlow.kt:274 |
| Heading ★ | "Stop leaving your life\nto chance." | RoutineOnboardingFlow.kt:275 |
| Body ★ | "Work gets calendars. Deadlines get reminders. Other people get your structure. Shanivar starts giving some of it back to you." | RoutineOnboardingFlow.kt:276 |
| Card title | "ROUTINE" | RoutineOnboardingFlow.kt:277 |
| Card heading ★ | "Build the day you intend to live." | RoutineOnboardingFlow.kt:278 |
| Card body ★ | "Wake. Focus. Protect time. Put your personal work on the calendar." | RoutineOnboardingFlow.kt:279 |
| Card title | "SADHANA" | RoutineOnboardingFlow.kt:281 |
| Card heading ★ | "Do the work. Need fewer prompts." | RoutineOnboardingFlow.kt:282 |
| Card body ★ | "Train attention until the practice can leave the phone and enter your life. Progression waits — Steady is here when you are ready." | RoutineOnboardingFlow.kt:283 |
| Primary button | "Build my rhythm" | RoutineOnboardingFlow.kt:285 |
| Footer ★ | "Shanivar is not here to keep you busy. It is here to make what matters harder to ignore." | RoutineOnboardingFlow.kt:287 |

## 2. Your day (onboarding step 02, wake + intended sleep)

Source: `RoutineOnboardingFlow.kt` `RhythmPage` (lines 292-339); validation in `RoutineOnboarding.kt`.

| Role | Verbatim copy | file:line |
| --- | --- | --- |
| Step label | "02 · Your day" | RoutineOnboardingFlow.kt:265 |
| Eyebrow | "YOUR DAY" | RoutineOnboardingFlow.kt:303 |
| Heading ★ | "Give the day\ntwo fixed points." | RoutineOnboardingFlow.kt:304 |
| Body ★ | "Not an ideal morning. Your actual life." | RoutineOnboardingFlow.kt:305 |
| Card title | "Day anchors" | RoutineOnboardingFlow.kt:306 |
| Time button | "I usually wake around · ${draft.wakeTime...}" | RoutineOnboardingFlow.kt:313 |
| Time button | "I make room for sleep around · ${draft.intendedSleepTime...}" | RoutineOnboardingFlow.kt:320 |
| Card note | "These organize your calendar. Your wake alarm remains a separate, optional choice." | RoutineOnboardingFlow.kt:322 |
| Body ★ | "We use your chosen times to place the day you can actually follow. The rest can be built around reality — not someone else's ideal morning." | RoutineOnboardingFlow.kt:327 |
| Primary button | "Use these times" | RoutineOnboardingFlow.kt:334 |
| Text button | "Set this later" | RoutineOnboardingFlow.kt:336 |
| Text button | "Back" | RoutineOnboardingFlow.kt:338 |
| Error message | "Could not save your times." | RoutineOnboardingFlow.kt:96 |
| Validation | "Choose wake and intended sleep times." | RoutineOnboarding.kt:145 |
| Validation | "Choose different wake and sleep times." | RoutineOnboarding.kt:146 |

## 3. Guardrails (onboarding step 03, setup hub)

Source: `RoutineOnboardingFlow.kt` `SetupPage` (lines 341-385). The step label "03 · Guardrails" also appears on Alarm, App protection and Device access.

| Role | Verbatim copy | file:line |
| --- | --- | --- |
| Step label | "03 · Guardrails" | RoutineOnboardingFlow.kt:266 |
| Eyebrow | "SET THE GUARDRAILS" | RoutineOnboardingFlow.kt:350 |
| Heading ★ | "Set it once.\nThen get on with life." | RoutineOnboardingFlow.kt:351 |
| Body ★ | "Two tools. Both editable. Neither should need your attention every day." | RoutineOnboardingFlow.kt:352 |
| Card title | "WAKE ALARM" | RoutineOnboardingFlow.kt:353 |
| Card heading ★ | "Start when you said you would." | RoutineOnboardingFlow.kt:354 |
| Card body (armed) | "Alarm set for ${draft.wakeTime...} on this device." | RoutineOnboardingFlow.kt:356 |
| Card body (not armed) | "Use your ${draft.wakeTime...} anchor if you want Shanivar to wake you." | RoutineOnboardingFlow.kt:357 |
| Card button | "Edit" / "Set up" | RoutineOnboardingFlow.kt:360 |
| Card title | "APP PROTECTION" | RoutineOnboardingFlow.kt:363 |
| Card heading ★ | "Choose what loses access to you." | RoutineOnboardingFlow.kt:364 |
| Card body (saved) | "1 protection window saved · ${draft.packages.size} app${...}. Saved is not Active." | RoutineOnboardingFlow.kt:368 |
| Card body (default) ★ | "Choose distracting apps once. Create the window that fits your day. You can stop protection anytime." | RoutineOnboardingFlow.kt:369 |
| Card button | "Edit" / "Set up" | RoutineOnboardingFlow.kt:373 |
| Footer ★ | "This is not discipline theatre. Remove the decision when you already know what the right decision is." | RoutineOnboardingFlow.kt:377 |
| Outlined button | "Review device access" | RoutineOnboardingFlow.kt:381 |
| Primary button | "Go to Routine" | RoutineOnboardingFlow.kt:383 |
| Text button | "Back" | RoutineOnboardingFlow.kt:384 |

## 4. Alarm (onboarding, optional wake alarm)

Source: `RoutineOnboardingFlow.kt` `AlarmPage` (lines 387-449) and the save handler (lines 169-212).

| Role | Verbatim copy | file:line |
| --- | --- | --- |
| Eyebrow | "ROUTINE / ALARM" | RoutineOnboardingFlow.kt:402 |
| Heading ★ | "Start when\nyou said you would." | RoutineOnboardingFlow.kt:403 |
| Card title | "Wake alarm" | RoutineOnboardingFlow.kt:404 |
| Card body | "Time · ${draft.wakeTime...} (from your day anchor)" | RoutineOnboardingFlow.kt:405 |
| Switch label | "Enable this alarm" / "Alarm off" | RoutineOnboardingFlow.kt:408 |
| Label | "Repeat on" | RoutineOnboardingFlow.kt:410 |
| Day chips | "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa" | RoutineOnboardingFlow.kt:412 |
| Card title ★ | "Dismissal stays humane" | RoutineOnboardingFlow.kt:426 |
| Card body ★ | "The wake screen can ask you to stand or move, but an accessible non-physical exit remains available. A hard cap always ends ringing." | RoutineOnboardingFlow.kt:427 |
| Repair button | "Allow Alarms & reminders" | RoutineOnboardingFlow.kt:431 |
| Repair button | "Allow notifications" | RoutineOnboardingFlow.kt:436 |
| Primary button | "Save alarm" / "Keep alarm off" | RoutineOnboardingFlow.kt:444 |
| Text button | "Leave alarm off" | RoutineOnboardingFlow.kt:446 |
| Text button | "Back" | RoutineOnboardingFlow.kt:448 |
| Message | "Wake alarm left off. Your wake time is still an anchor." | RoutineOnboardingFlow.kt:172 |
| Message | "Choose at least one repeat day." | RoutineOnboardingFlow.kt:177 |
| Message | "Allow Alarms & reminders, then return and try again." | RoutineOnboardingFlow.kt:181 |
| Message | "Allow notifications so the wake screen can appear, then try again." | RoutineOnboardingFlow.kt:185 |
| Message | "Android did not confirm the alarm. Check Alarms & reminders, then try again." | RoutineOnboardingFlow.kt:198 |
| Message (armed) ★ | "Scheduled on this device · $whenText. Saving is not the same as ringing — the next trigger is what matters." | RoutineOnboardingFlow.kt:203 |

## 5. App protection (onboarding, first protection window)

Source: `RoutineOnboardingFlow.kt` `ProtectionPage` (lines 451-513), save handler (lines 222-248), validation in `RoutineOnboarding.kt`.

| Role | Verbatim copy | file:line |
| --- | --- | --- |
| Eyebrow | "ROUTINE / APP PROTECTION" | RoutineOnboardingFlow.kt:466 |
| Heading ★ | "Choose what\ngets cut off." | RoutineOnboardingFlow.kt:467 |
| Body ★ | "You already know which apps take more than they give. Select them once. Protection is voluntary and stoppable — never unbreakable." | RoutineOnboardingFlow.kt:468 |
| Chip suffix | " · not installed" | RoutineOnboardingFlow.kt:478 |
| Empty state | "No supported distracting apps are installed on this device. You can still skip and set this later in Routine." | RoutineOnboardingFlow.kt:485 |
| Card title | "First protection window" | RoutineOnboardingFlow.kt:487 |
| Time button | "Starts · ${window.startTime...}" | RoutineOnboardingFlow.kt:494 |
| Time button | "Ends · ${window.endTime...}" | RoutineOnboardingFlow.kt:501 |
| Card note | "Every day for this first window. Add more later in App protection. Saving schedules on this device; it does not mean Active." | RoutineOnboardingFlow.kt:502 |
| Primary button | "Save & check device access" | RoutineOnboardingFlow.kt:508 |
| Text button | "Skip for now" | RoutineOnboardingFlow.kt:510 |
| Text button | "Back" | RoutineOnboardingFlow.kt:512 |
| Message (saved) ★ | "Saved on this device. A saved window is not Active until the engine confirms it — check device access next." | RoutineOnboardingFlow.kt:240 |
| Error | "Could not save protection." | RoutineOnboarding.kt:128 |
| Validation | "Add a protection window, or skip this step." | RoutineOnboarding.kt:159 |

App category headings are rendered from `category.uppercase()` (RoutineOnboardingFlow.kt:470) and come from `ProtectionAppCatalog`, which is outside the six snapshot files. The handoff names the categories Social, Feeds & video, Communities, Entertainment, Easy escape routes (`android-app/CLAUDE_ROUTINE_V3_BUILD_HANDOFF_2026-09-30.md:246-250`); the exact runtime labels were not verified for this inventory.

## 6. Device access (onboarding, permissions)

Source: `RoutineOnboardingFlow.kt` `PermissionsPage` (lines 515-543); repair buttons from `RoutineProtectionScreen.kt` `ProtectionRepairActions` (lines 158-176).

| Role | Verbatim copy | file:line |
| --- | --- | --- |
| Eyebrow | "DEVICE ACCESS" | RoutineOnboardingFlow.kt:522 |
| Heading | "One system\ncheck remains." | RoutineOnboardingFlow.kt:523 |
| Body ★ | "These grants are optional until you want protection or a wake alarm to work. Declining never deletes your saved plan. Shanivar will say Needs device access — not pretend you are protected." | RoutineOnboardingFlow.kt:524 |
| Card title | "What each grant is for" | RoutineOnboardingFlow.kt:525 |
| Card line | "Alarms & reminders — schedule the next wake alarm and protection boundary exactly." | RoutineOnboardingFlow.kt:526 |
| Card line | "Usage access — notice when a selected app is open so protection can respond." | RoutineOnboardingFlow.kt:527 |
| Card line | "Display over other apps — show the protection screen without taking over the whole phone permanently." | RoutineOnboardingFlow.kt:528 |
| Status | "All three are granted on this device right now." | RoutineOnboardingFlow.kt:531 |
| Status | "Still missing on this device:" | RoutineOnboardingFlow.kt:533 |
| Repair button | "Allow Alarms & reminders" | RoutineProtectionScreen.kt:165 |
| Repair button | "Grant Usage access" | RoutineProtectionScreen.kt:169 |
| Repair button | "Allow display over other apps" | RoutineProtectionScreen.kt:174 |
| Footer ★ | "Essential access stays available. Stop protection anytime — no waiting, no payment, no forced practice, no server." | RoutineOnboardingFlow.kt:538 |
| Primary button | "Go to Routine" | RoutineOnboardingFlow.kt:541 |
| Text button | "Back to guardrails" | RoutineOnboardingFlow.kt:542 |

## 7. Routine (home destination)

Source: `RoutineShell.kt` (shell), `RoutineScreen.kt` `RoutineScreen` (lines 41-144), `RoutineProtectionScreen.kt` `RoutineProtectionSummary` and `StopProtection` (lines 118-187).

| Role | Verbatim copy | file:line |
| --- | --- | --- |
| Bottom nav | "Routine" | RoutineShell.kt:38 |
| Bottom nav | "Sadhana" | RoutineShell.kt:39 |
| Wordmark | "SHANIVAR" | RoutineShell.kt:90 |
| Top-bar button | "Close" / "Settings" | RoutineShell.kt:92 |
| Eyebrow | "YOUR DAY" | RoutineScreen.kt:93 |
| Heading ★ | "Not an ideal morning.\nYour actual life." | RoutineScreen.kt:98 |
| Body | "Wake, sleep, and what you put on the calendar — separate from alarms and protection." | RoutineScreen.kt:100 |
| Summary | "Wake $wake · Sleep $sleep" | RoutineScreen.kt:111 |
| Text button | "Edit Routine" | RoutineScreen.kt:115 |
| Card title | "Needs attention" | RoutineScreen.kt:119 |
| Card body | "Routine data could not be read cleanly. Recovery is still available. Re-save your day to repair." | RoutineScreen.kt:120 |
| Card title | "Today" | RoutineScreen.kt:124 |
| Empty state | "Nothing on the calendar yet. Edit Routine to set wake, sleep, and activities." | RoutineScreen.kt:126 |
| Card title | "App protection" | RoutineProtectionScreen.kt:126 |
| Card heading ★ | "Choose what loses access to you." | RoutineProtectionScreen.kt:127 |
| Card body (none) | "Choose distracting apps once. Create the windows that fit your day." | RoutineProtectionScreen.kt:130 |
| Card body (some) | "$count protection window${...} · ${plan.protectedPackages.size} app${...}" | RoutineProtectionScreen.kt:131 |
| Card button | "Set up" / "Edit" | RoutineProtectionScreen.kt:138 |
| Running status | "Protection is on until ${fmt(running.endsAtEpochMillis)}." | RoutineProtectionScreen.kt:182 |
| Stop button ★ | "Stop protection now" | RoutineProtectionScreen.kt:186 |

Timeline rows (`RoutineScreen.kt:130-134`) and the protection status line (`RoutineProtectionScreen.kt:133`) are composed in `RoutineTimeline.kt`, `RoutineProtection.kt` and `FocusScheduleStatus.kt`, which are outside the six snapshot files and are not inventoried here.

## 8. Edit Routine (anchors + personal activities)

Source: `RoutineScreen.kt` `EditRoutineScreen` and `ActivityEditor` (lines 146-327); quick-add titles, weekday names and validation in `RoutinePlan.kt`.

| Role | Verbatim copy | file:line |
| --- | --- | --- |
| Heading | "Edit Routine" | RoutineScreen.kt:165 |
| Body ★ | "Anchors and personal activities. This does not arm an alarm or start protection." | RoutineScreen.kt:166 |
| Field label | "I usually wake around (HH:mm)" | RoutineScreen.kt:170 |
| Field label | "I make room for sleep around (HH:mm)" | RoutineScreen.kt:177 |
| Section | "Personal activities" | RoutineScreen.kt:183 |
| Quick-add button | "+ ${starter.title}" → "+ Gym", "+ Read a book", "+ Extend Sadhana" | RoutineScreen.kt:204; titles RoutinePlan.kt:76-78 |
| Add button | "+" (accessibility: "Add activity") | RoutineScreen.kt:211, 210 |
| Text button | "Cancel" | RoutineScreen.kt:218 |
| Validation | "Wake time must look like 07:00." | RoutineScreen.kt:224 |
| Validation | "Sleep time must look like 22:30." | RoutineScreen.kt:227 |
| Error | "Could not save." | RoutineScreen.kt:246 |
| Button | "Saving…" / "Save" | RoutineScreen.kt:252 |
| Card title (blank) | "New activity" | RoutineScreen.kt:264 |
| Field label | "Name" | RoutineScreen.kt:268 |
| Field label | "Start (HH:mm)" | RoutineScreen.kt:277 |
| Field label | "Duration (minutes)" | RoutineScreen.kt:286 |
| Label | "Occurrence" | RoutineScreen.kt:291 |
| Chips | "Every day", "Weekly", "Custom" | RoutineScreen.kt:293, 296, 300 |
| Weekday chips | "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" | RoutinePlan.kt:66 |
| Text button | "Delete activity" | RoutineScreen.kt:320 |
| Validation | "Wake and intended sleep cannot be the same time." | RoutinePlan.kt:112 |
| Validation | "At most ${RoutinePlanV1.MAX_ACTIVITIES} personal activities." (12, RoutinePlan.kt:23) | RoutinePlan.kt:115 |
| Validation | "Activity needs a name." | RoutinePlan.kt:135 |
| Validation | "Duration must be between 1 and 1440 minutes." | RoutinePlan.kt:137 |
| Validation | "Pick at least one day for this activity." | RoutinePlan.kt:140 |

## 9. Protection editor (Routine → App protection, full screen)

Source: `RoutineProtectionScreen.kt` `AppProtectionEditor`, `ProtectionWindowCard` and save messages (lines 85-100, 189-366); window labels and validation in `RoutinePlan.kt`.

| Role | Verbatim copy | file:line |
| --- | --- | --- |
| Back button | "← Routine" | RoutineProtectionScreen.kt:208 |
| Eyebrow | "ROUTINE / APP PROTECTION" | RoutineProtectionScreen.kt:209 |
| Heading ★ | "Choose what\ngets cut off." | RoutineProtectionScreen.kt:211 |
| Body ★ | "You already know which apps take more than they give. Select them once. Shanivar handles the windows." | RoutineProtectionScreen.kt:212 |
| Chip suffix | " · not installed" | RoutineProtectionScreen.kt:225 |
| Empty state | "No supported distracting apps are installed on this device." | RoutineProtectionScreen.kt:231 |
| Note ★ | "The whole selected app is paused, including its messages. Calls and system apps are never selected." | RoutineProtectionScreen.kt:232 |
| Section heading | "Protection windows" | RoutineProtectionScreen.kt:235 |
| Body ★ | "Morning. Work. Night. Add the boundaries your 24-hour cycle needs." | RoutineProtectionScreen.kt:236 |
| Empty state | "No windows yet. Use + to add one." | RoutineProtectionScreen.kt:237 |
| Limit | "Eight windows is the limit." | RoutineProtectionScreen.kt:253 |
| Add button | "+" (accessibility: "Add another protection window") | RoutineProtectionScreen.kt:259, 258 |
| Note ★ | "Essential access stays available. Saving schedules windows on this device; it does not switch protection on." | RoutineProtectionScreen.kt:262 |
| Running status / stop | "Protection is on until ...", "Stop protection now" | RoutineProtectionScreen.kt:182, 186 |
| Error | "Could not save." | RoutineProtectionScreen.kt:284 |
| Primary button | "Saving…" / "Save & check device access" | RoutineProtectionScreen.kt:290 |
| Text button | "Turn all windows off (then save)" | RoutineProtectionScreen.kt:293 |
| Text button | "Discard changes" / "Done" | RoutineProtectionScreen.kt:297 |
| Window card title | "Protection window ${index + 1}" | RoutineProtectionScreen.kt:315 |
| Switch a11y | "Window ${index + 1} on"; state "On" / "Off" | RoutineProtectionScreen.kt:323, 324 |
| Time buttons | "From · ${...}", "Until · ${...}" | RoutineProtectionScreen.kt:330, 332 |
| Shape note | "Midnight to midnight: the whole calendar day." | RoutineProtectionScreen.kt:335 |
| Shape note | "Overnight: starts on the chosen day, ends the next morning." | RoutineProtectionScreen.kt:336 |
| Text button | "Full calendar day" | RoutineProtectionScreen.kt:339 |
| Label | "Repeat" | RoutineProtectionScreen.kt:340 |
| Repeat chips | "Every day", "Weekdays", "Weekends", "Custom week" | RoutinePlan.kt:253-256 |
| Window summary | "Full calendar day · $days" / "$start–$end$suffix · $days" (suffix " next day") | RoutinePlan.kt:243, 246-247 |
| Custom label | "No days" | RoutinePlan.kt:279 |
| Status (unsaved) | "Not saved yet." | RoutineProtectionScreen.kt:359 |
| Text button | "Remove" | RoutineProtectionScreen.kt:362 |
| Save message | "Saved on this device, but not scheduled: ${sync.message}" | RoutineProtectionScreen.kt:88 |
| Save message | "Saved. No window is on, so nothing is scheduled." | RoutineProtectionScreen.kt:90 |
| Save message | "Saved on this device, but Android did not schedule it. Allow Alarms & reminders." | RoutineProtectionScreen.kt:91 |
| Save message ★ | "Saved on this device. Status below shows what is actually armed." | RoutineProtectionScreen.kt:92 |
| Save message | "Saved. Nothing about protection changed." | RoutineProtectionScreen.kt:94 |
| Save suffix | "$base Protection already running keeps going until you stop it or it ends." | RoutineProtectionScreen.kt:97 |
| Validation | "At most ${RoutinePlanV1.MAX_WINDOWS} protection windows." (8, RoutinePlan.kt:24) | RoutinePlan.kt:121 |
| Validation ★ | "Protection windows overlap: ${...} and ${...}. Adjacent is fine; overlap is not." | RoutinePlan.kt:128 |
| Validation | "A window cannot start and end at the same time (except midnight-to-midnight all-day)." | RoutinePlan.kt:147 |
| Validation | "Pick at least one start day for this window." | RoutinePlan.kt:150 |

---

## Lines most useful for marketing (★ shortlist)

1. "DISCIPLINE FOR REAL LIFE" — RoutineOnboardingFlow.kt:274
2. "Stop leaving your life\nto chance." — RoutineOnboardingFlow.kt:275
3. "Work gets calendars. Deadlines get reminders. Other people get your structure. Shanivar starts giving some of it back to you." — RoutineOnboardingFlow.kt:276
4. "Not an ideal morning. Your actual life." — RoutineOnboardingFlow.kt:305; RoutineScreen.kt:98 (heading form)
5. "Set it once.\nThen get on with life." — RoutineOnboardingFlow.kt:351
6. "Start when you said you would." — RoutineOnboardingFlow.kt:354
7. "Choose what loses access to you." — RoutineOnboardingFlow.kt:364; RoutineProtectionScreen.kt:127
8. "This is not discipline theatre. Remove the decision when you already know what the right decision is." — RoutineOnboardingFlow.kt:377
9. "You already know which apps take more than they give. Select them once. Protection is voluntary and stoppable — never unbreakable." — RoutineOnboardingFlow.kt:468
10. "Do the work. Need fewer prompts." — RoutineOnboardingFlow.kt:282
11. "Shanivar is not here to keep you busy. It is here to make what matters harder to ignore." — RoutineOnboardingFlow.kt:287
12. "Shanivar will say Needs device access — not pretend you are protected." — RoutineOnboardingFlow.kt:524
13. "Essential access stays available. Stop protection anytime — no waiting, no payment, no forced practice, no server." — RoutineOnboardingFlow.kt:538
14. "Morning. Work. Night. Add the boundaries your 24-hour cycle needs." — RoutineProtectionScreen.kt:236
15. "Saving is not the same as ringing — the next trigger is what matters." — RoutineOnboardingFlow.kt:203
