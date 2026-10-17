# Routine V3 — physical-device QA (DEV-7)

**Date:** 5 October 2026  
**Tester:** Shubham (physical phone only — no emulator claims)  
**Related:** [ROUTINE_V3_TEST_GUIDE_2026-09-30.md](ROUTINE_V3_TEST_GUIDE_2026-09-30.md) (automated gates + broader matrix)

Mark each row **Pass / Fail / Skip** and note device + Android version at the top.

| Device | Android | Build | Notes |
|--------|---------|-------|-------|
|        |         |       |       |

---

## 0. Install / build (flag still off in tree)

`ROUTINE_V3` is **false** in `app/build.gradle.kts` (`buildConfigField("boolean", "ROUTINE_V3", "false")`). Do **not** commit a flip.

For a **local test build only**, temporarily set that field to `"true"`, then from `android-app/`:

```bash
./gradlew assembleDebug -PshanivarApiUrl=https://hybridmonk-api.onrender.com/api
```

Install the debug APK on the phone. Revert the flag before any push. Confirm Routine | Sadhana shell appears after launch.

| # | Check | Pass | Fail | Notes |
|---|-------|------|------|-------|
| 0.1 | Test build with ROUTINE_V3=true installs and opens Routine shell | | | |

---

## 1. Day plan — wake, sleep, activities

Activities must **not** arm alarms or focus/protection.

| # | Check | Pass | Fail | Notes |
|---|-------|------|------|-------|
| 1.1 | Set distinct wake + intended sleep; both persist after kill/relaunch | | | |
| 1.2 | Add Gym / custom activity; edit time/days; delete one | | | |
| 1.3 | Activity add/edit/delete does **not** start protection or change alarm | | | |

---

## 2. Protection windows

Shared app list across windows. Cap = 8.

| # | Check | Pass | Fail | Notes |
|---|-------|------|------|-------|
| 2.1 | Add windows up to 8; 9th blocked or unavailable | | | |
| 2.2 | Adjacent OK (e.g. 18:00–19:00 then 19:00–20:00) | | | |
| 2.3 | Overlap rejected with clear message | | | |
| 2.4 | Overnight window (e.g. 22:00→06:00) saves and arms | | | |
| 2.5 | Full calendar day `00:00→00:00` accepted | | | |
| 2.6 | Selected apps shared; empty selection cannot arm | | | |

---

## 3. Status honesty

| # | Check | Pass | Fail | Notes |
|---|-------|------|------|-------|
| 3.1 | After save outside window → **Saved** / Ready-ish, **not** Active | | | |
| 3.2 | **Active** only while blocking is actually on | | | |
| 3.3 | Save mid-open window → **Needs attention** (not Active) | | | |
| 3.4 | Missing Usage / overlay / exact-alarm → repair buttons, truthful state | | | |

---

## 4. Stop protection (local-first)

| # | Check | Pass | Fail | Notes |
|---|-------|------|------|-------|
| 4.1 | While Active: airplane / signed out → **Stop protection** ends block immediately | | | |
| 4.2 | Status returns to Off / Saved; no account/network required | | | |

---

## 5. Reboot (DEV-21)

| # | Check | Pass | Fail | Notes |
|---|-------|------|------|-------|
| 5.1 | Reboot **inside** an armed window → protection resumes | | | |
| 5.2 | Reboot **after** window ended → protection does **not** resume | | | |

---

## 6. Multi-window boundary (DEV-2)

| # | Check | Pass | Fail | Notes |
|---|-------|------|------|-------|
| 6.1 | Two non-overlapping windows armed; when first ends, second stays Active if still in range | | | |

---

## 7. All-day / midnight abutment (DEV-6)

| # | Check | Pass | Fail | Notes |
|---|-------|------|------|-------|
| 7.1 | All-day (`00:00→00:00`) covers through end of day | | | |
| 7.2 | Adjacent to next-day / midnight abutment does not drop early or double-fire | | | |

---

## 8. Doze / screen off / overnight (if practical)

| # | Check | Pass | Fail | Notes |
|---|-------|------|------|-------|
| 8.1 | Screen off through a START → window becomes Active on schedule | | | |
| 8.2 | Overnight sleep: leave phone idle; morning END (or next START) behaves | | | |
| 8.3 | OEM battery restrictions noted (Samsung/Xiaomi/etc.) | | | |

---

## Sign-off

| | |
|--|--|
| Overall | Pass / Fail / Partial |
| Blockers found | |
| Tester / date | |
