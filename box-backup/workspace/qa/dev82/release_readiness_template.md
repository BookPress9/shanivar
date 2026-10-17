# Release-readiness report — RC `<tag>` @ `<commit>`

Date (Europe/Berlin): · QA: Shanivar Tester · Recommendation: **GO / NO-GO / GO with founder-approved exceptions**

## 1. Build
| Item | Value |
|---|---|
| Commit / tag | |
| Flavour / variant (DEV-44) | |
| APK SHA-256 | |
| Supabase target (staging only for QA) | |
| Tickets in this RC (keys) | |

## 2. Gate G4 — Independent testing
| Suite | Command | Total | Pass | Fail | NOT RUN | Evidence path |
|---|---|---|---|---|---|---|
| Android JVM/Robolectric | `./gradlew :app:testDebugUnitTest` | | | | | |
| Content safety (DEV-39/89) | included above | | | | | |
| pgTAP (local) | `supabase test db` | | | | | |
| Phone↔Supabase E2E (DEV-55) | | | | | | |
| Cross-user isolation (DEV-79) | | | | | | |
| Device protocol — Pixel (DEV-7/71/75) | `connectedDebugAndroidTest` + manual checklist | | | | | |
| Device protocol — OEM (model:) | | | | | | |
| E2E journey ×2 devices (DEV-84) | | | | | | |
| Analytics expected vs actual (DEV-83) | | | | | | |
| Copy/claims audit (DEV-88, DEV-6) | | | | | | |

## 3. AC coverage for tickets in this RC
| Ticket | ACs | automated-pass | manual (evidence) | real-device (evidence) | gaps | Verdict |
|---|---|---|---|---|---|---|

## 4. Open defects
| Key | Severity | Status | Founder approval to ship (link) |
|---|---|---|---|
Rule: no unresolved Critical or High without documented founder approval.

## 5. Safety rules check (each needs evidence)
- [ ] No breath pacing on any reachable or bundled surface
- [ ] No medical, therapeutic, energy or performance claims
- [ ] Teachers stay internal
- [ ] Self-reports never shown or synced as scores or health data
- [ ] No sensitive data in analytics or logs
- [ ] Protected-app list stays on the device

## 6. Known limits disclosed to testers
(flash before block, browsers/other devices not blocked, OEM killers, …)

## 7. Sign-off
QA: · Founder (G6):
