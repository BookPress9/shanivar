# DEV-82 — Shanivar test plan (first release)

Owner: Shanivar Tester · Version 0.1, 2026-10-17 (Europe/Berlin) · Jira: [DEV-82](https://shanivar.atlassian.net/browse/DEV-82) (Epic F, [DEV-61](https://shanivar.atlassian.net/browse/DEV-61))
**Status: live.** The founder confirmed DEV-82 on 17 Oct 2026. This plan writes no new tests. Per DEV-82 scope-out, it writes no tests for unconfirmed tickets.

## 1. Objective and definition of done
QA verifies Shanivar independently across normal, failure and recovery cases. A developer's word is not enough.
**DoD (founder, 17 Oct):** a ticket is not done because it shows on screen. It is done only when all of these pass:
1. its ACs (each linked to a test in `ac_test_matrix.md`),
2. integration tests,
3. the regression suite,
4. analytics verification, where the ticket emits events,
5. a **real-device** run for every alarm or protection behaviour.
Any part not run is recorded as NOT RUN, never as a pass.

## 2. Scope
In: every DEV ticket with ACs, across epics A–G (89 issues, 315 ACs plus 32 DEV-71/75 phone cases; DEV-90..96 are QA-filed bugs; see the matrix). First-release work items come from the QA brief: DEV-82, DEV-7/71/75, DEV-84/55, DEV-83, DEV-79, DEV-89, DEV-15.
Out: writing tests for unconfirmed tickets; Later-phase tickets (DEV-13, 16, 22, 23, 51, 77, 78, 87).

## 3. Test levels and where they live
| Level | Tool / location | Command | Gate |
|---|---|---|---|
| Unit + Robolectric/Compose (JVM) | `android-app/app/src/test/java/com/shanivar/nativeapp/` | `cd android-app && ./gradlew :app:testDebugUnitTest` | every commit / RC |
| Content safety | `TechniqueContentSafetyTest`, `BreathingCatalogTest`, `SadhanaPracticeTest.noBreathPacingLanguage` | same | every RC (extend per DEV-89) |
| DB / RLS (pgTAP) | `supabase/tests/0*.test.sql` | `supabase test db` (**local stack only**) | every `supabase/**` change |
| Integration (phone ↔ Supabase) | `SupabaseSyncTest.e2eGuestPushReadBack` (needs `-Dsupabase.e2e.url/anon`) | local stack now; staging after DEV-41 | every RC |
| Device protocol | `android-app/ENFORCEMENT_STANCE_2026-09-12.md` (8 checks), `ROUTINE_V3_TEST_GUIDE_2026-09-30.md`, `ROUTINE_V3_DEVICE_QA_2026-10-05.md` | `connectedDebugAndroidTest` on a physical phone. `pixel2Api34DebugAndroidTest` is forbidden. | every RC (DEV-7/71/75). Founder phone checklists: `/workspace/qa/dev71/phone_checklist.md` (18), `/workspace/qa/dev75/phone_checklist.md` (14). **No emulator** (cancelled by the founder 17 Oct). |
| E2E journey | protect → intervention → act → reflect → recover (DEV-84) | manual script on 2 devices | before release recommendation |
| Analytics | scripted session vs emitted events (DEV-83) | after DEV-65/69 | every RC once events exist |
| Security | cross-user suite for every table, RPC and Edge Function (DEV-79) | pgTAP + staging | release blocker |
| Copy / claims / safety audit | DEV-88 checklist, DEV-89, DEV-6 copy | manual | every RC |

## 4. Environments and rules
- QA box worktrees: `/workspace/shanivar-v3` (main lineage, at 20fb519 on `dev-38-outbox-gaps`) and `-dev39/-dev42/-dev45/-dev47/-dev54`. Record the commit hash with every verdict.
- Local Supabase stack is **shared**. Never `supabase db reset` while another run uses it; use `supabase test db` only, or wait for it to be free. QA never touches the cloud project (`shanivar-prototype`) and never uses the service-role key. Staging (DEV-41) is the QA target once it exists.
- Never `git push`. Never change source code; QA scratch tests live outside the repo (e.g. `/workspace/qa/dev38/*.kt.txt`).
- **Device matrix (minimum):** one Pixel (stock Android 14/15) plus one Xiaomi/Realme/Vivo/Samsung-class phone with its battery saver ON. Record model, API level, build id, commands and raw output.
- Device cases: reboot inside and after a window; Doze (`adb shell dumpsys deviceidle force-idle`); OEM battery saver and app-killer; force-stop; revoked usage, overlay and exact-alarm access; airplane mode; process death mid-practice and mid-overlay; DST (Berlin falls back **25 Oct 2026**, next Sunday) and time-zone change; low battery; alarm during practice; full-screen intent on Android 14+.

## 5. Evidence rules
Raw output, not summaries. A skipped test is NOT RUN. Each result is stored with its build hash under `/workspace/qa/<ticket>/`. Evidence holds no personal data: no real names or emails, no package lists from a personal phone, no self-report text.

## 6. Entry and exit criteria per RC
Entry: the RC hash is tagged; all in-scope tickets are merged to main (today DEV-38/39/42/45/47/54 are still on branches); the matrix is updated.
Exit: regression green; every in-scope AC is automated-pass, or manual/real-device **with recorded evidence**; no open Critical/High without documented founder approval; analytics verified; release-readiness report (`release_readiness_template.md`) filled and signed.

## 7. Defect handling
Severities and register: `defect_register.md`. Retest after every fix. Record the failing build and the passing build. Examples: DEV-38 failed at 86b558a and passed at 20fb519. DEV-39 failed at 3471087 and passed at 8e122a8.

## 8. DEV-82's own test cases
| Case | Result |
|---|---|
| **Normal:** trace one ticket fully | DEV-38: Jira repro (found in DEV-35 B3 QA, b372079) → derived ACs ×5 → `SyncOutboxGapsTest` (11 tests) + QA scratch repro `/workspace/qa/dev38/` → failed at 86b558a (legacy-import gap) → re-tested **pass at 20fb519** → register entry. Remaining before Done: merge to main, regression on main, real sync to staging via DEV-55. |
| **Edge:** AC not testable → back to owner | 21 tickets have no AC section (ACs derived and marked `(derived)`): DEV-10, 11, 16, 18, 20, 21, 22, 23, 24, 26–34, 37, 38, 39 (DEV-35 uses its B3–B8 checklist instead). AC wording that can't be tested: DEV-3 AC3 ("understandable"), DEV-73 AC1 (needs a usability protocol), DEV-5 AC4 (no defined UI for "explains"). Owners asked to confirm (see Jira comment). |
| **Failure:** test env down | When the local Supabase/Docker stack is down or busy, pgTAP and e2e are recorded NOT RUN and the RC is not recommended. Recovery script: `dev45` branch `start-local-supabase.sh` (it brings the stack up; it does **not** reset). |
| **Recovery:** retest after a recorded fix | DEV-38 (86b558a ✗ → 20fb519 ✓), DEV-39 (3471087 ✗ → 8e122a8 ✓), DEV-42 (passed at ba38de6). |

## 9. Current coverage snapshot (from the matrix)
347 rows (315 ACs + 32 DEV-71/75 phone cases). automated-pass **76** · manual-only **65** · real-device-only **62** (all NOT RUN, pending the founder's phone) · no test **132** (109 for work not built yet, 23 on built or in-flight work) · pending **12** (DEV-45 in QA, plus DEV-25/52 rows that depend on it).
