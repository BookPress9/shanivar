# DEV-71 / DEV-75: phone case plan (index)

Owner: Shanivar Tester · Updated 2026-10-17 23:10 CEST.
**The founder cancelled the emulator (22:55 CEST).** No case is planned for or run on an emulator. All 32 DEV-71/75 cases are **real-device, NOT RUN, pending the founder's phone**, and they are in his phone checklists. A second phone (other brand or Android version) is still needed to sign off either ticket (DEV-71 scope, DEV-75 "min 2 OEMs, Android 12–15").

| Ticket | Full test cases | Founder phone checklist | Phone cases | No-phone checks done | Bugs filed |
|---|---|---|---|---|---|
| DEV-71 protection | `/workspace/qa/dev71/test_cases.md` | `/workspace/qa/dev71/phone_checklist.md` | 18 (P1–P4, S1–S5, R1–R6, D1–D3), all NOT RUN | 9: 7 PASS, 2 observations (→ DEV-70, R6) | none new (gaps already in DEV-70, DEV-89) |
| DEV-75 alarms | `/workspace/qa/dev75/test_cases.md` | `/workspace/qa/dev75/phone_checklist.md` | 14 (A1–A10, D1–D4), all NOT RUN | 11: 8 PASS, 3 FAIL | DEV-93 (High), DEV-94 (High), DEV-95 (Medium), DEV-96 (Low) |

Evidence rule: phone model, Android version, build hash, local time (CEST), screenshot or recording, PASS/FAIL. No personal app list (the test app is YouTube, signed out). Sync is off or local stack only, never the cloud project.
