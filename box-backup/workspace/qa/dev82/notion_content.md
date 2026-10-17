<callout icon="🧪" color="yellow_bg">
	**Live: the founder confirmed DEV-82 on 17 Oct 2026.** Shanivar Tester, 2026-10-17 (Europe/Berlin). Source files are on the QA box at `/workspace/qa/dev82/` (test_plan.md, ac_test_matrix.md, defect_register.md, release_readiness_template.md, device_case_plan_dev71_dev75.md, alignment_review.md). No source code was changed, no tests were written for unconfirmed tickets, and no Jira status was changed.
</callout>
Part of <mention-page url="https://app.notion.com/p/3fc10ae18c9f81c1b877d676061428dd"/> · Jira [DEV-82](https://shanivar.atlassian.net/browse/DEV-82) · Epic F [DEV-61](https://shanivar.atlassian.net/browse/DEV-61)
# Alignment review (QA)

**1) What must change?**
- "Done" now means ACs + integration + regression + analytics pass, plus a real-device run for alarms and protection. DEV-1–6 and DEV-21 are "Completed" in Jira but are only unit-verified. Treat them as "device pending (DEV-7)", not done.
- Every ticket needs testable ACs. 21 tickets have no AC section and the 7 epics have none. QA derived ACs and returns them to the owners to confirm.
- Fixes that pass on branches (DEV-38 @20fb519, DEV-39 @8e122a8, DEV-42 @ba38de6) don't count until they're merged and regression passes on main. Every verdict names its build.
- Safety tests must cover every breathing surface, not only techniques.json. That means the Breathing\* screens and the block-screen "breathe" path (DEV-89).

**2) What already exists?**
- Main @20fb519: 518 JVM/Robolectric tests, 0 failures (1 skipped: the e2e that needs a stack). They cover scheduler correctness, honest status, offline local stop, reboot catch-up, the outbox and the Sadhana content gates. 76 ACs are automated-pass.
- pgTAP 01–05 (RLS isolation, privileges, caps, timestamps), passing at dev42@ba38de6. TechniqueContentSafetyTest with mutation runs (DEV-39). Sync engine tests and pgTAP 06 (DEV-45, in QA now).
- Device protocol docs (8-check stance, Routine V3 guide, device QA checklist). Retest history with evidence in /workspace/qa/.

**3) What's missing?**
- No real-device run yet: all 37 real-device rows are NOT RUN, and there's no OEM phone (DEV-7/71/75). 21 DEV-71/75 cases are planned for the emulator first (emulator-pending); they never replace the phone rows.
- No analytics at all: the intervention choice isn't recorded (DEV-69) and there is no event contract (DEV-65). So WIAC can't be verified (DEV-83).
- No CI gate (DEV-15/43), no staging (DEV-41), so no E2E (DEV-55). No crash reporting (DEV-14). No cross-user tests for RPCs or Edge Functions (DEV-79).
- No E2E protect → act → reflect suite (DEV-84). Completion idempotency (DEV-72) and missed-day recovery (DEV-74) aren't built.

**4) What dependencies or risks are there?**
- Founder: rotate credentials (DEV-8), staging and plan (DEV-41), DEV-40, physical Pixel + OEM phones, and resuming device testing (paused 5 Oct).
- Top risk: OEM battery killers plus polling-based blocking (H7). The DST change on 25 Oct 2026 (Berlin) is a real-world test window next week.
- Shared local Supabase and 6 worktrees can interfere with each other. With no CI, regressions can slip in. d9168eb can't be reverted on its own (rollback risk).
- Open Critical/High: DEV-8, DEV-17, DEV-89 (suspected). Each blocks an RC unless the founder approves it.

**5) What can ship in the first release?**
- Routine V3: day plan, protection windows with honest status, local stop and the wake alarm. Only after the device protocol passes on 2 devices (Pixel + 1 OEM).
- Guest sync (DEV-38/42/45/47), once it's merged to main, regression is green and the DEV-79 isolation suite is complete.
- The 60 s Sadhana Stage 1. Only with review manifests (DEV-34), DEV-89 cleared and the check-in simplified to 2–3 taps (DEV-36).
- Not yet: WIAC or effectiveness claims, auth providers, or any "works on every phone" claim.

**6) How will it be verified?**
- The DEV-82 AC-to-test matrix (315 ACs) plus a release-readiness report per RC. Regression = `testDebugUnitTest` + `supabase test db` (local) + the device protocol. Anything skipped is NOT RUN.
- Device protocol on a Pixel and an OEM phone: reboot, Doze, battery saver, force-stop, revoked access, airplane mode, DST/time-zone change, process death, and the alarm actually waking someone.
- Analytics: compare scripted journeys with the emitted events (DEV-83). Security: cross-user suite (DEV-79). Copy and safety audits (DEV-88, DEV-89).
- Exit gate: no open Critical/High without documented founder approval.

# Coverage summary and top gaps

Generated 2026-10-17 (Europe/Berlin) by Shanivar Tester from Jira JQL `project = DEV ORDER BY key ASC` (89 issues) and the repo worktrees. No source code changed; no tests written for unconfirmed tickets.

**Builds referenced:** `main@20fb519` = /workspace/shanivar-v3 (dev-38-outbox-gaps, 518 unit tests, 0 fail, 1 skipped = SupabaseSyncTest.e2eGuestPushReadBack, run 21:55); `dev39@8e122a8` (512/0/1, 21:59); `dev42@ba38de6` (507/0/0 Android + pgTAP 01–05 on local stack); `dev45@2fce42d` (in QA now); `dev47@1e69cba` (548/0/1 developer run 22:36, not QA-reviewed); `dev54@7a56cf6` + uncommitted work. Android test paths are under `android-app/app/src/test/java/com/shanivar/nativeapp/`; pgTAP under `supabase/tests/`.

**Status key:** automated-pass · manual-only (needs a human check; result noted) · real-device-only (needs a physical phone; NOT RUN) · emulator-pending (DEV-71/75 cases planned for the emulator; run when PM confirms it is booted — emulator results never replace the real-device rows) · no test (gap) · pending (in QA now, DEV-45 only). `(derived)` = Jira has no AC section; AC inferred from the description and **returned to the owner to confirm** (DEV-82 edge case). `[DoD]` = row added by QA under the new definition of done (real-device proof for alarms/protection).


## Summary counts

<table fit-page-width="true" header-row="true">
	<tr><td>Epic</td><td>ACs</td><td>automated-pass</td><td>manual-only</td><td>real-device-only (NOT RUN)</td><td>emulator-pending</td><td>no test (gap)</td><td>pending</td></tr>
	<tr><td>A — Strategy & measurement</td><td>4</td><td>0</td><td>3</td><td>0</td><td>0</td><td>1</td><td>0</td></tr>
	<tr><td>B — Protection</td><td>66</td><td>19</td><td>7</td><td>17</td><td>12</td><td>11</td><td>0</td></tr>
	<tr><td>C — Routine</td><td>61</td><td>13</td><td>6</td><td>8</td><td>9</td><td>23</td><td>2</td></tr>
	<tr><td>D — Sadhana</td><td>59</td><td>24</td><td>10</td><td>3</td><td>0</td><td>22</td><td>0</td></tr>
	<tr><td>E — Security, privacy & reliability</td><td>117</td><td>20</td><td>29</td><td>3</td><td>0</td><td>55</td><td>10</td></tr>
	<tr><td>F — QA & release</td><td>23</td><td>0</td><td>6</td><td>6</td><td>0</td><td>11</td><td>0</td></tr>
	<tr><td>G — CS & growth</td><td>12</td><td>0</td><td>3</td><td>0</td><td>0</td><td>9</td><td>0</td></tr>
	<tr><td>No epic</td><td>1</td><td>0</td><td>1</td><td>0</td><td>0</td><td>0</td><td>0</td></tr>
	<tr><td>**Total**</td><td>**343**</td><td>**76**</td><td>**65**</td><td>**37**</td><td>**21**</td><td>**132**</td><td>**12**</td></tr>
</table>

Rows: 343 = 315 Jira/derived ACs + 28 planned DEV-71/DEV-75 device cases (17 + 15 rows, minus the 4 umbrella ACs already counted).

Tickets: 89 (82 work tickets + 7 epic containers). Tickets whose ACs were derived because Jira has no AC section: 21.


**Top gaps (built or in-flight work with no test, or not run):**
1. **Real-device proof: all 37 real-device rows are NOT RUN (21 DEV-71/75 cases are emulator-pending).** No Pixel or OEM run for any alarm or protection ticket (DEV-1–6, 10, 21, 69–71, 75). DST fall-back on 25 Oct is an imminent case.
2. **Breath-pacing safety (DEV-89):** TechniqueContentSafetyTest scans only techniques.json/.md. The Breathing\* screens and the block-screen "breathe" path have no test.
3. **No-snooze on the block screen (DEV-10):** nothing asserts it.
4. **Analytics / WIAC:** no events exist (DEV-65/69/83). The intervention choice and the "no package name leaves the device" rule are untested.
5. **Cascade delete (DEV-42 AC7):** only the FK is checked; there is no delete-and-verify test. This blocks the DEV-46 Play requirement.
6. **Cross-user isolation (DEV-79):** covers 5 tables + tombstones. No RPC or Edge Function exists yet; DEV-54 pgTAP 07 hasn't been run by QA.
7. **Copy claims (DEV-6 AC2, DEV-88):** no scan for "unbreakable/continuous" wording in app, site or store.
8. **Stage 1 check-in (DEV-36 AC4):** the shipped card would fail the 2–3 one-tap rule, and no test enforces it.
9. **E2E against staging (DEV-55):** SupabaseSyncTest.e2eGuestPushReadBack is skipped without a stack, and there is no staging yet.
10. **Branch-only fixes:** DEV-38/39/42/45/47/54 are not on main, and there is no CI (DEV-15).
# Defect register
As of 2026-10-17 22:45 (Europe/Berlin). Owner: Shanivar Tester. Evidence holds no personal data (test users are `a@test.local` / `b@test.local`).

**Severity:** Critical = harm/lock-out, security breach or safety-rule breach reachable by users · High = a core promise is wrong (protection, alarm, truthful status, data loss) · Medium = wrong behaviour with a workaround, or latent · Low = hygiene/tooling.
**QA status:** Open · Fixed – unit-verified (device pending) · Verified fixed (on branch) · Verified fixed (on main) · Pending retest.
"Verified in" = the build where QA last saw the fix pass. A Jira status of Completed does **not** mean device-verified.

<table fit-page-width="true" header-row="true">
	<tr><td>Key</td><td>Title</td><td>Severity</td><td>Jira status</td><td>QA status</td><td>Found in</td><td>Verified in build</td><td>Evidence / note</td></tr>
	<tr><td>[DEV-1](https://shanivar.atlassian.net/browse/DEV-1)</td><td>Delayed START can begin a 24-hour focus session</td><td>Critical</td><td>Completed</td><td>Fixed – unit-verified, device pending (DEV-7)</td><td>source inspection 2026-09-29</td><td>main@20fb519 (unit, 518/0 fail)</td><td>FocusWindowRecurrenceTest.dev1_\* (3)</td></tr>
	<tr><td>[DEV-2](https://shanivar.atlassian.net/browse/DEV-2)</td><td>Window END releases protection owned by another window</td><td>High</td><td>Completed</td><td>Fixed – unit-verified, device pending</td><td>source inspection</td><td>main@20fb519 (fix 2914d4e)</td><td>FocusWindowEndHandlerTest (15)</td></tr>
	<tr><td>[DEV-3](https://shanivar.atlassian.net/browse/DEV-3)</td><td>Saved schedule shown as armed without engine confirmation</td><td>High</td><td>Completed</td><td>Fixed – unit-verified, device pending</td><td>source inspection</td><td>main@20fb519 (fix 2914d4e)</td><td>FocusScheduleStatusTest (16)</td></tr>
	<tr><td>[DEV-4](https://shanivar.atlassian.net/browse/DEV-4)</td><td>Boundaries depend on in-process session; stop impossible offline</td><td>High</td><td>Completed</td><td>Fixed – unit-verified, device pending</td><td>source inspection</td><td>main@20fb519</td><td>LocalProtectionTest; DisciplineEngineTest.releaseFocusStopsTheGuard…</td></tr>
	<tr><td>[DEV-5](https://shanivar.atlassian.net/browse/DEV-5)</td><td>No overnight focus windows</td><td>High</td><td>Completed</td><td>Fixed – unit-verified, device pending; AC4 (explain TZ change) has no test</td><td>source inspection</td><td>main@20fb519</td><td>FocusWindowRecurrenceTest overnight + DST</td></tr>
	<tr><td>[DEV-6](https://shanivar.atlassian.net/browse/DEV-6)</td><td>All-day preset leaves a 1-minute gap; copy implies persistence</td><td>Medium</td><td>Completed</td><td>Fixed – unit-verified; copy AC not automated</td><td>source inspection</td><td>main@20fb519 (fix 2914d4e)</td><td>FocusWindowRecurrenceTest.fullCalendarDay\*</td></tr>
	<tr><td>[DEV-21](https://shanivar.atlassian.net/browse/DEV-21)</td><td>Reboot catch-up must resume local protection with no session</td><td>High</td><td>Completed</td><td>Passes in tests; real device pending under DEV-7</td><td>dev</td><td>main@20fb519 (fix 88793bd)</td><td>BootRescheduleCatchUpTest (5)</td></tr>
	<tr><td>[DEV-38](https://shanivar.atlassian.net/browse/DEV-38)</td><td>Sync outbox never queues own self-reports, edits/deletes, onboarding import, routine changes</td><td>Medium</td><td>Open</td><td>Verified fixed (on branch dev-38-outbox-gaps, not on main)</td><td>QA of DEV-35 B3 at b372079</td><td>**20fb519** (failed once at 86b558a: legacy-import gap)</td><td>SyncOutboxGapsTest (11); /workspace/qa/dev38/rerun-\*</td></tr>
	<tr><td>[DEV-39](https://shanivar.atlassian.net/browse/DEV-39)</td><td>techniques.json has a timed box-breathing script and a nervous-system claim</td><td>High (safety rule; Jira says Medium)</td><td>Open</td><td>Verified fixed (on branch dev-39-techniques-safety)</td><td>QA safety check at b372079</td><td>**8e122a8** (failed once at 3471087: "rounds" still present)</td><td>TechniqueContentSafetyTest (5) + mutation runs /workspace/qa/dev39-mut-\*.log</td></tr>
	<tr><td>QA-42-01 (not filed)</td><td>DEV-42: commit d9168eb cannot be reverted on its own (later caps/revoke migrations depend on it)</td><td>Low</td><td>—</td><td>Open — rollback hazard</td><td>QA of DEV-42</td><td>dev42@ba38de6</td><td>Feed into DEV-81 runbook: revert the DEV-42 set as a unit or ship a forward fix</td></tr>
	<tr><td>QA-42-02 (not filed)</td><td>DEV-42: anon request returns a 401/permission hint that names the table</td><td>Low</td><td>—</td><td>Open — information disclosure</td><td>QA of DEV-42</td><td>dev42@ba38de6</td><td>05\_privileges / anon probe. Table names are not secret, but the hint confirms which tables exist</td></tr>
	<tr><td>QA-42-03 (not filed)</td><td>DEV-42: text columns other than ids and `note` have no length cap</td><td>Medium</td><td>—</td><td>Open — hardening gap</td><td>QA of DEV-42</td><td>dev42@ba38de6</td><td>04\_size\_caps covers plan/packages/drift\_reasons/id/note only. Proposed fix: cap every free text column (follow-up ticket)</td></tr>
	<tr><td>[DEV-8](https://shanivar.atlassian.net/browse/DEV-8)</td><td>Leaked GitHub PAT and Render key still valid (+ Atlassian token)</td><td>Critical</td><td>Open</td><td>Open — founder action; release blocker</td><td>audit 2026-09-24</td><td>—</td><td>Not testable by QA until rotated</td></tr>
	<tr><td>[DEV-17](https://shanivar.atlassian.net/browse/DEV-17)</td><td>No release signing; release API host NXDOMAIN</td><td>High</td><td>Open</td><td>Open — blocks any store/release build</td><td>audit 2026-09-24</td><td>—</td><td></td></tr>
	<tr><td>[DEV-89](https://shanivar.atlassian.net/browse/DEV-89)</td><td>Breathing\* screens and block-screen "breathe" path may pace breath (suspected)</td><td>High (if confirmed)</td><td>Open</td><td>Open — suspected, audit not run</td><td>DEV-39 follow-up</td><td>—</td><td>TechniqueContentSafetyTest does not scan these files</td></tr>
	<tr><td>[DEV-36](https://shanivar.atlassian.net/browse/DEV-36) (gap)</td><td>Shipped Stage 1 check-in (47e668a) has a stepper, chips and a note; spec allows 2–3 one-tap options</td><td>Medium</td><td>Open</td><td>Open — spec mismatch, do not ship as-is</td><td>ticket analysis</td><td>main@20fb519</td><td>SadhanaStage1.kt Stage1CheckCard</td></tr>
	<tr><td>[DEV-19](https://shanivar.atlassian.net/browse/DEV-19)</td><td>git push hangs in non-interactive sessions</td><td>Low (tooling)</td><td>Open</td><td>Open</td><td>2026-09-30</td><td>—</td><td>QA never pushes</td></tr>
	<tr><td>[DEV-45](https://shanivar.atlassian.net/browse/DEV-45)</td><td>Sync engine (Backend half)</td><td>—</td><td>Open</td><td>**Pending — in QA now**</td><td>—</td><td>dev45@2fce42d under test</td><td>No verdict yet</td></tr>
</table>

**Counts:** 17 entries. Critical 2 (DEV-1 fixed in logic; DEV-8 open). High 8 (DEV-2/3/4/5/21 fixed in logic; DEV-39 verified on branch; DEV-17, DEV-89 open). Medium 4 (DEV-6 fixed; DEV-38 verified on branch; QA-42-03, DEV-36 open). Low 3 (QA-42-01, QA-42-02, DEV-19 open). Plus DEV-45, pending in QA.
**Open Critical/High with no fix:** DEV-8, DEV-17, DEV-89. Each blocks an RC unless the founder gives documented approval.
**Action for owners:** file QA-42-01..03 as Jira bugs, if the founder agrees. QA did not file them.

# Test plan
Owner: Shanivar Tester · Version 0.1, 2026-10-17 (Europe/Berlin) · Jira: [DEV-82](https://shanivar.atlassian.net/browse/DEV-82) (Epic F, [DEV-61](https://shanivar.atlassian.net/browse/DEV-61))
**Status: live.** The founder confirmed DEV-82 on 17 Oct 2026. This plan writes no new tests. Per DEV-82 scope-out, it writes no tests for unconfirmed tickets.

### 1. Objective and definition of done
QA verifies Shanivar independently across normal, failure and recovery cases. A developer's word is not enough.
**DoD (founder, 17 Oct):** a ticket is not done because it shows on screen. It is done only when all of these pass:
1. its ACs (each linked to a test in `ac_test_matrix.md`),
2. integration tests,
3. the regression suite,
4. analytics verification, where the ticket emits events,
5. a **real-device** run for every alarm or protection behaviour.
Any part not run is recorded as NOT RUN, never as a pass.

### 2. Scope
In: every DEV ticket with ACs, across epics A–G (89 issues, 315 ACs plus 28 planned device cases; see the matrix). First-release work items come from the QA brief: DEV-82, DEV-7/71/75, DEV-84/55, DEV-83, DEV-79, DEV-89, DEV-15.
Out: writing tests for unconfirmed tickets; Later-phase tickets (DEV-13, 16, 22, 23, 51, 77, 78, 87).

### 3. Test levels and where they live
<table fit-page-width="true" header-row="true">
	<tr><td>Level</td><td>Tool / location</td><td>Command</td><td>Gate</td></tr>
	<tr><td>Unit + Robolectric/Compose (JVM)</td><td>`android-app/app/src/test/java/com/shanivar/nativeapp/`</td><td>`cd android-app && ./gradlew :app:testDebugUnitTest`</td><td>every commit / RC</td></tr>
	<tr><td>Content safety</td><td>`TechniqueContentSafetyTest`, `BreathingCatalogTest`, `SadhanaPracticeTest.noBreathPacingLanguage`</td><td>same</td><td>every RC (extend per DEV-89)</td></tr>
	<tr><td>DB / RLS (pgTAP)</td><td>`supabase/tests/0*.test.sql`</td><td>`supabase test db` (**local stack only**)</td><td>every `supabase/**` change</td></tr>
	<tr><td>Integration (phone ↔ Supabase)</td><td>`SupabaseSyncTest.e2eGuestPushReadBack` (needs `-Dsupabase.e2e.url/anon`)</td><td>local stack now; staging after DEV-41</td><td>every RC</td></tr>
	<tr><td>Emulator pre-run (DEV-71/75)</td><td>`device_case_plan_dev71_dev75.md`, 21 cases</td><td>adb against the PM-booted emulator; `connectedDebugAndroidTest` allowed, managed-device task forbidden</td><td>before each device run; never replaces it</td></tr>
	<tr><td>Device protocol</td><td>`android-app/ENFORCEMENT_STANCE_2026-09-12.md` (8 checks), `ROUTINE_V3_TEST_GUIDE_2026-09-30.md`, `ROUTINE_V3_DEVICE_QA_2026-10-05.md`</td><td>`connectedDebugAndroidTest` on a physical phone. `pixel2Api34DebugAndroidTest` is forbidden.</td><td>every RC (DEV-7/71/75)</td></tr>
	<tr><td>E2E journey</td><td>protect → intervention → act → reflect → recover (DEV-84)</td><td>manual script on 2 devices</td><td>before release recommendation</td></tr>
	<tr><td>Analytics</td><td>scripted session vs emitted events (DEV-83)</td><td>after DEV-65/69</td><td>every RC once events exist</td></tr>
	<tr><td>Security</td><td>cross-user suite for every table, RPC and Edge Function (DEV-79)</td><td>pgTAP + staging</td><td>release blocker</td></tr>
	<tr><td>Copy / claims / safety audit</td><td>DEV-88 checklist, DEV-89, DEV-6 copy</td><td>manual</td><td>every RC</td></tr>
</table>

### 4. Environments and rules
- QA box worktrees: `/workspace/shanivar-v3` (main lineage, at 20fb519 on `dev-38-outbox-gaps`) and `-dev39/-dev42/-dev45/-dev47/-dev54`. Record the commit hash with every verdict.
- Local Supabase stack is **shared**. Never `supabase db reset` while another run uses it; use `supabase test db` only, or wait for it to be free. QA never touches the cloud project (`shanivar-prototype`) and never uses the service-role key. Staging (DEV-41) is the QA target once it exists.
- Never `git push`. Never change source code; QA scratch tests live outside the repo (e.g. `/workspace/qa/dev38/*.kt.txt`).
- **Device matrix (minimum):** one Pixel (stock Android 14/15) plus one Xiaomi/Realme/Vivo/Samsung-class phone with its battery saver ON. Record model, API level, build id, commands and raw output.
- Device cases: reboot inside and after a window; Doze (`adb shell dumpsys deviceidle force-idle`); OEM battery saver and app-killer; force-stop; revoked usage, overlay and exact-alarm access; airplane mode; process death mid-practice and mid-overlay; DST (Berlin falls back **25 Oct 2026**, next Sunday) and time-zone change; low battery; alarm during practice; full-screen intent on Android 14+.

### 5. Evidence rules
Raw output, not summaries. A skipped test is NOT RUN. Each result is stored with its build hash under `/workspace/qa/<ticket>/`. Evidence holds no personal data: no real names or emails, no package lists from a personal phone, no self-report text.

### 6. Entry and exit criteria per RC
Entry: the RC hash is tagged; all in-scope tickets are merged to main (today DEV-38/39/42/45/47/54 are still on branches); the matrix is updated.
Exit: regression green; every in-scope AC is automated-pass, or manual/real-device **with recorded evidence**; no open Critical/High without documented founder approval; analytics verified; release-readiness report (`release_readiness_template.md`) filled and signed.

### 7. Defect handling
Severities and register: `defect_register.md`. Retest after every fix. Record the failing build and the passing build. Examples: DEV-38 failed at 86b558a and passed at 20fb519. DEV-39 failed at 3471087 and passed at 8e122a8.

### 8. DEV-82's own test cases
<table fit-page-width="true" header-row="true">
	<tr><td>Case</td><td>Result</td></tr>
	<tr><td>**Normal:** trace one ticket fully</td><td>DEV-38: Jira repro (found in DEV-35 B3 QA, b372079) → derived ACs ×5 → `SyncOutboxGapsTest` (11 tests) + QA scratch repro `/workspace/qa/dev38/` → failed at 86b558a (legacy-import gap) → re-tested **pass at 20fb519** → register entry. Remaining before Done: merge to main, regression on main, real sync to staging via DEV-55.</td></tr>
	<tr><td>**Edge:** AC not testable → back to owner</td><td>21 tickets have no AC section (ACs derived and marked `(derived)`): DEV-10, 11, 16, 18, 20, 21, 22, 23, 24, 26–34, 37, 38, 39 (DEV-35 uses its B3–B8 checklist instead). AC wording that can't be tested: DEV-3 AC3 ("understandable"), DEV-73 AC1 (needs a usability protocol), DEV-5 AC4 (no defined UI for "explains"). Owners asked to confirm (see Jira comment).</td></tr>
	<tr><td>**Failure:** test env down</td><td>When the local Supabase/Docker stack is down or busy, pgTAP and e2e are recorded NOT RUN and the RC is not recommended. Recovery script: `dev45` branch `start-local-supabase.sh` (it brings the stack up; it does **not** reset).</td></tr>
	<tr><td>**Recovery:** retest after a recorded fix</td><td>DEV-38 (86b558a ✗ → 20fb519 ✓), DEV-39 (3471087 ✗ → 8e122a8 ✓), DEV-42 (passed at ba38de6).</td></tr>
</table>

### 9. Current coverage snapshot (from the matrix)
343 rows (315 ACs + 28 planned DEV-71/75 device cases). automated-pass **76** · manual-only **65** · real-device-only **37** (all NOT RUN) · emulator-pending **21** · no test **132** (109 for work not built yet, 23 on built or in-flight work) · pending **12** (DEV-45 in QA, plus DEV-25/52 rows that depend on it).

# DEV-71 / DEV-75 device case plan (emulator-pending)
Owner: Shanivar Tester · 2026-10-17 (Europe/Berlin) · Status of every emulator case: **emulator-pending**. PM will say when the emulator is booted. Every real-device row stays **NOT RUN** until it runs on a physical phone.
**Rule:** an emulator pass is integration evidence only. Under the DoD it never replaces the Pixel and OEM rows (DEV-7 AC3, QA brief). Record emulator results as "emulator: PASS/FAIL" next to the case and leave the real-device verdict open.

### Setup (once the emulator is booted)
- Use the PM-provided emulator through `adb` (`adb devices` must list it). **Do not run** the Gradle managed-device task `pixel2Api34DebugAndroidTest`: it is forbidden on the dev machine. `./gradlew :app:connectedDebugAndroidTest` may target the attached emulator.
- Build: `./gradlew :app:assembleDebug` from the RC commit, then `adb install -r app/build/outputs/apk/debug/app-debug.apk` (package `com.shanivar.nativeapp.dev`). Turn ROUTINE\_V3 on for debug only, as the device QA checklist describes. No source change.
- Record per case: emulator image (API level, ABI), build hash, exact commands, raw `adb logcat` excerpt, screenshot (`adb exec-out screencap -p`), and PASS/FAIL/NOT RUN. Evidence goes in `/workspace/qa/dev71/` and `/workspace/qa/dev75/`, with no personal data.
- Sync is off, or local stack only. Never the cloud project. Never reset the shared local stack.
- Restore the emulator clock and time zone after R3/R4/A6 (`adb shell settings put global auto_time 1`).

### DEV-71 — protection (permissions, sessions, reboot, recovery)
<table fit-page-width="true" header-row="true">
	<tr><td>ID</td><td>Case</td><td>Steps (emulator)</td><td>Expected</td><td>Emulator</td><td>Real device</td></tr>
	<tr><td>P1</td><td>Usage access revoked mid-window</td><td>Start a window → `adb shell appops set <pkg> GET_USAGE_STATS ignore` → reopen Routine</td><td>NEEDS\_DEVICE\_ACCESS with repair step; never Active</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>P2</td><td>Overlay revoked</td><td>`appops set <pkg> SYSTEM_ALERT_WINDOW deny` during a window</td><td>Honest state, no false Active, stop still works</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>P3</td><td>Exact alarm denied</td><td>`appops set <pkg> SCHEDULE_EXACT_ALARM deny` → save window</td><td>Not Ready; repair path named</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>S1</td><td>Window START/END on time</td><td>Window now+2 → now+5 min, protected app = a test app</td><td>Overlay at START (brief flash is acceptable and disclosed), release at END</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>S2</td><td>Two windows, one ends inside the other</td><td>Two windows, adjacent and nested (overlap is rejected at save)</td><td>Earlier END does not release the later window (DEV-2)</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>S3</td><td>Overnight + full day</td><td>Set clock to 21:58 with a 22:00→06:00 window; set clock past midnight; full calendar day</td><td>Active across midnight, no 1-minute gap (DEV-5/6)</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>S4</td><td>Stop offline; no snooze</td><td>Airplane mode (`cmd connectivity airplane-mode enable`), signed out → Stop</td><td>Protection ends immediately; block screen offers no snooze/"5 more min" (DEV-4/10)</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>R1</td><td>Reboot inside / after a window</td><td>`adb reboot` inside the window; then again after END</td><td>Resumes inside; does not start after; next boundary re-armed (DEV-21)</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>R2</td><td>Process death / force-stop</td><td>`adb shell am kill <pkg>`; separately `am force-stop <pkg>`</td><td>Kill: protection persists and resumes. Force-stop: the documented bypass, shown honestly (DEV-70 gap)</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>R3</td><td>Doze, late START</td><td>`adb shell dumpsys deviceidle force-idle` across START; `unforce` after END</td><td>No 24 h session from a stale START (DEV-1)</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>R4</td><td>TZ change + DST fall-back</td><td>Zone Europe/Berlin, clock 2026-10-25 02:30 (repeated hour); zone change to Asia/Kolkata</td><td>One boundary per rule; new next-boundary explained (DEV-5 AC4 gap)</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>R5</td><td>App update</td><td>`adb install -r` same build during an active window</td><td>Windows and alarms re-armed (B2)</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>D1</td><td>OEM battery saver / app killer</td><td>Xiaomi/Realme/Vivo/Samsung with saver ON, 2 h window</td><td>FocusGuardService survives, or failure surfaced</td><td>—</td><td>NOT RUN</td></tr>
	<tr><td>D2</td><td>Overnight on battery, low-battery mode</td><td>Real phone overnight</td><td>Protection holds until END</td><td>—</td><td>NOT RUN</td></tr>
	<tr><td>D3</td><td>Repeat on stock Pixel</td><td>Full table above</td><td>As above</td><td>—</td><td>NOT RUN</td></tr>
</table>

### DEV-75 — alarms and notifications
<table fit-page-width="true" header-row="true">
	<tr><td>ID</td><td>Case</td><td>Steps (emulator)</td><td>Expected</td><td>Emulator</td><td>Real device</td></tr>
	<tr><td>A1</td><td>Fires from lock screen</td><td>Alarm now+2 min, lock (`input keyevent 26`)</td><td>Full-screen alarm UI on API 34+; sound</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>A2</td><td>Doze / swipe-away</td><td>force-idle, swipe app from recents</td><td>Fires on time (setAlarmClock)</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>A3</td><td>Reboot / update / recurrence</td><td>`adb reboot`; `install -r`; let a repeating alarm fire</td><td>Re-armed; next occurrence recomputed (0008)</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>A4</td><td>Exact alarm denied</td><td>`appops set <pkg> SCHEDULE_EXACT_ALARM deny`</td><td>"Permission required" shown, no silent fail</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>A5</td><td>Notifications denied</td><td>`pm revoke <pkg> android.permission.POST_NOTIFICATIONS`</td><td>State shown honestly</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>A6</td><td>DST / TZ</td><td>Alarm 02:30 on 2026-10-25 Europe/Berlin; zone change</td><td>Exactly one ring at local wall-clock time</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>A7</td><td>Dismiss paths</td><td>Accessible path; QR/photo with emulator virtual camera</td><td>Accessible path always reachable (0003)</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>A8</td><td>Alarm during practice</td><td>Start 60 s Sadhana, alarm fires</td><td>Practice interrupted and recorded truthfully</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>A9</td><td>Hard cap</td><td>Leave ringing</td><td>Ends at the cap; no endless ring</td><td>emulator-pending</td><td>NOT RUN</td></tr>
	<tr><td>D1</td><td>Movement dismissal</td><td>Real step detector / ACTIVITY\_RECOGNITION</td><td>Dismiss by steps; fallback reachable</td><td>—</td><td>NOT RUN</td></tr>
	<tr><td>D2</td><td>Wakes a person</td><td>Volume, vibration, speaker, DND</td><td>Audible and noticeable</td><td>—</td><td>NOT RUN</td></tr>
	<tr><td>D3</td><td>OEM killer</td><td>OEM phone, saver ON, overnight</td><td>Alarm not suppressed</td><td>—</td><td>NOT RUN</td></tr>
	<tr><td>D4</td><td>Powered off across alarm time</td><td>Power off, power on after</td><td>Behaviour documented, no ghost alarm</td><td>—</td><td>NOT RUN</td></tr>
</table>

Counts: 21 emulator-pending cases (DEV-71: 12, DEV-75: 9). Real-device rows NOT RUN: DEV-71 4 (AC1 + D1–D3), DEV-75 6 (AC1, AC2, D1–D4).

# Release-readiness report template
Copy this per RC.

Date (Europe/Berlin): · QA: Shanivar Tester · Recommendation: **GO / NO-GO / GO with founder-approved exceptions**

### 1. Build
<table fit-page-width="true" header-row="true">
	<tr><td>Item</td><td>Value</td></tr>
	<tr><td>Commit / tag</td><td></td></tr>
	<tr><td>Flavour / variant (DEV-44)</td><td></td></tr>
	<tr><td>APK SHA-256</td><td></td></tr>
	<tr><td>Supabase target (staging only for QA)</td><td></td></tr>
	<tr><td>Tickets in this RC (keys)</td><td></td></tr>
</table>

### 2. Gate G4 — Independent testing
<table fit-page-width="true" header-row="true">
	<tr><td>Suite</td><td>Command</td><td>Total</td><td>Pass</td><td>Fail</td><td>NOT RUN</td><td>Evidence path</td></tr>
	<tr><td>Android JVM/Robolectric</td><td>`./gradlew :app:testDebugUnitTest`</td><td></td><td></td><td></td><td></td><td></td></tr>
	<tr><td>Content safety (DEV-39/89)</td><td>included above</td><td></td><td></td><td></td><td></td><td></td></tr>
	<tr><td>pgTAP (local)</td><td>`supabase test db`</td><td></td><td></td><td></td><td></td><td></td></tr>
	<tr><td>Phone↔Supabase E2E (DEV-55)</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
	<tr><td>Cross-user isolation (DEV-79)</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
	<tr><td>Device protocol — Pixel (DEV-7/71/75)</td><td>`connectedDebugAndroidTest` + manual checklist</td><td></td><td></td><td></td><td></td><td></td></tr>
	<tr><td>Device protocol — OEM (model:)</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
	<tr><td>E2E journey ×2 devices (DEV-84)</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
	<tr><td>Analytics expected vs actual (DEV-83)</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
	<tr><td>Copy/claims audit (DEV-88, DEV-6)</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
</table>

### 3. AC coverage for tickets in this RC
<table fit-page-width="true" header-row="true">
	<tr><td>Ticket</td><td>ACs</td><td>automated-pass</td><td>manual (evidence)</td><td>real-device (evidence)</td><td>gaps</td><td>Verdict</td></tr>
</table>

### 4. Open defects
<table fit-page-width="true" header-row="true">
	<tr><td>Key</td><td>Severity</td><td>Status</td><td>Founder approval to ship (link)</td></tr>
</table>
Rule: no unresolved Critical or High without documented founder approval.

### 5. Safety rules check (each needs evidence)
- [ ] No breath pacing on any reachable or bundled surface
- [ ] No medical, therapeutic, energy or performance claims
- [ ] Teachers stay internal
- [ ] Self-reports never shown or synced as scores or health data
- [ ] No sensitive data in analytics or logs
- [ ] Protected-app list stays on the device

### 6. Known limits disclosed to testers
(flash before block, browsers/other devices not blocked, OEM killers, …)

### 7. Sign-off
QA: · Founder (G6):

# AC-to-test matrix (all 89 DEV issues: 315 ACs + 28 planned device cases)
## Epic A — Strategy & measurement ([DEV-56](https://shanivar.atlassian.net/browse/DEV-56))

<table fit-page-width="true" header-row="true">
	<tr><td>Ticket</td><td>Jira status</td><td>AC</td><td>Test (file · class.method / pgTAP / manual)</td><td>Status</td></tr>
	<tr><td>[DEV-56](https://shanivar.atlassian.net/browse/DEV-56) epic container</td><td>Open</td><td>— no ACs (container)</td><td>—</td><td>—</td></tr>
	<tr><td>[DEV-63](https://shanivar.atlassian.net/browse/DEV-63) Publish the canonical Notion page "Shanivar Product Thesis —</td><td>Open</td><td>Every §2A section present; epics linked; decision log; founder approves</td><td>Doc review of Notion thesis (sections present; approval recorded 17 Oct)</td><td>manual-only</td></tr>
	<tr><td>[DEV-64](https://shanivar.atlassian.net/browse/DEV-64) Map existing Shanivar features against the protect → act → r</td><td>Open</td><td>Every journey step has ≥1 row; missing steps link tickets; diagram</td><td>Doc review (thesis §5 has table + mermaid)</td><td>manual-only</td></tr>
	<tr><td>[DEV-65](https://shanivar.atlassian.net/browse/DEV-65) Define the event dictionary, analytics contract and WIAC (No</td><td>Open</td><td>Doc reviewed by FE/BE/Tester/CS; every §6 metric traceable to events; privacy signed</td><td>Doc review — not written</td><td>no test (gap)</td></tr>
	<tr><td>[DEV-66](https://shanivar.atlassian.net/browse/DEV-66) Set baselines, experiment design and an assumption register</td><td>Open</td><td>Register exists, linked; each hypothesis has a test method</td><td>Doc review (thesis §9 lists H1–H8 with tests)</td><td>manual-only</td></tr>
</table>

_Epic subtotal:_ 4 ACs — manual-only 3, no test (gap) 1

## Epic B — Protection ([DEV-57](https://shanivar.atlassian.net/browse/DEV-57))

<table fit-page-width="true" header-row="true">
	<tr><td>Ticket</td><td>Jira status</td><td>AC</td><td>Test (file · class.method / pgTAP / manual)</td><td>Status</td></tr>
	<tr><td>[DEV-1](https://shanivar.atlassian.net/browse/DEV-1) FOCUS-1 A delayed START boundary can begin a 24-hour focus s</td><td>Completed</td><td>Stale START (after window closed) starts nothing</td><td>main@20fb519 test/FocusWindowRecurrenceTest.dev1\_aStartDeliveredAfterItsWindowClosedHasNoActiveEndToMeasureAgainst</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>On-time START measures correct remaining duration</td><td>main@20fb519 test/FocusWindowRecurrenceTest.dev1\_anOnTimeStartStillMeasuresToItsOwnWindowEnd</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>Regression test covers delayed delivery</td><td>main@20fb519 test/FocusWindowRecurrenceTest.dev1\_aLateEndFindsNoActiveOccurrenceSoItReleasesRatherThanSkipping; FocusWindowEndHandlerTest.lateEndDeliveredInsideTheNextOccurrence…</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>[DoD] Delayed/powered-off START on a real phone (guide cases 7–8)</td><td>DEV-7/DEV-71 device protocol (ROUTINE\_V3\_DEVICE\_QA\_2026-10-05.md) — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>[DEV-2](https://shanivar.atlassian.net/browse/DEV-2) FOCUS-5 Window END calls releaseFocus() unconditionally — bl</td><td>Completed</td><td>Earlier END does not release a later window's protection</td><td>main@20fb519 test/FocusWindowEndHandlerTest.earlierEndOfOverlappingWindowsDoesNotReleaseTheLaterWindow</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>Overlap handled or rejected at save with a reason</td><td>main@20fb519 test/RoutineProtectionTest.validateNamesTheTwoOverlappingWindows; FocusSchedulerRegistryTest.replaceAllCapsAtEightAndRejectsOverlap</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>Test: one window ends inside another</td><td>main@20fb519 test/FocusWindowEndHandlerTest.innerWindowEndingInsideAnOuterWindowLeavesTheOuterWindowProtected</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>[DoD] Multi-window on a real phone</td><td>DEV-7/DEV-71 device protocol (ROUTINE\_V3\_DEVICE\_QA\_2026-10-05.md) — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>[DEV-3](https://shanivar.atlassian.net/browse/DEV-3) FOCUS-6 A saved schedule is reported as armed without engine</td><td>Completed</td><td>Revoked usage/exact-alarm → NEEDS\_DEVICE\_ACCESS / NEEDS\_ATTENTION</td><td>main@20fb519 test/FocusScheduleStatusTest.revokedUsageAccessIsNeedsDeviceAccess…; revokedExactAlarmIsNeedsDeviceAccessNotReady</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>ACTIVE only when guard confirms</td><td>main@20fb519 test/FocusScheduleStatusTest.savedAndArmedInsideTheWindowWithoutAGuardIsNotActive; savedReadyAndActiveAreDistinctStates; RoutineProtectionTest.saveMessagesNeverClaimActive</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>Repair next-step understandable from state</td><td>Manual UX review of repair copy (no test asserts comprehension)</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>[DoD] Revoke permissions in Settings on a real phone</td><td>DEV-7/DEV-71 device protocol (ROUTINE\_V3\_DEVICE\_QA\_2026-10-05.md) — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>[DEV-4](https://shanivar.atlassian.net/browse/DEV-4) FOCUS-4 Scheduled boundaries depend on a nullable in-process</td><td>Completed</td><td>Cold-process START/END with no session and no network</td><td>main@20fb519 test/LocalProtectionTest.startsProtectionWithoutAnyAccountOrNetworkInvolved; FocusWindowEndHandlerTest.endHandlerNamesNoSessionOrApi; BootRescheduleCatchUpTest.catchUpDoesNotRequireASession</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>Immediate local stop works offline</td><td>main@20fb519 test/LocalProtectionTest.stopNowIsImmediateAndIdempotent; DisciplineEngineTest.releaseFocusStopsTheGuardEvenWhenTheBackendCallFails; RoutineProtectionTest.stopIsLocalFirstAndSurvivesMirrorFailure</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>Test covers cold process explicitly</td><td>main@20fb519 test/LocalProtectionTest.theLocalPathNamesNoApiTypeAtAll (structural)</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>[DoD] Process death / force-stop / airplane mode on a real phone</td><td>DEV-7/DEV-71 device protocol (ROUTINE\_V3\_DEVICE\_QA\_2026-10-05.md) — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>[DEV-5](https://shanivar.atlassian.net/browse/DEV-5) FOCUS-2 No overnight focus windows — sleep preparation is un</td><td>Completed</td><td>22:00→07:00 saves and fires both boundaries</td><td>main@20fb519 test/FocusWindowRecurrenceTest.mondayNightOvernightWindowIsActive…; overnightBoundariesAlternateStartThenEndAcrossMidnight; FocusWindowEndHandlerTest.overnightEndReleasesOnTheMorningAfter</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>Day selection applies to start day</td><td>main@20fb519 test/FocusWindowRecurrenceTest.theSameOvernightWindowIsInactiveSundayNightUnlessSundayIsSelected; endDaysShiftForwardOnlyForOvernightWindowsAndWrapPastSaturday</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>DST gap/overlap deterministic, no two active copies</td><td>main@20fb519 test/FocusWindowRecurrenceTest.daylightSavingTransitionsProduceOneDeterministicBoundary (Berlin 2026-03-29/10-25)</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>Timezone change explains the changed next boundary</td><td>main@20fb519 test/FocusWindowRecurrenceTest.boundariesAreRecomputedFromLocalRulesRatherThanStoredInstants (recompute only — no test or UI for the \*explanation\*)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>[DoD] Overnight + DST (25 Oct 2026) + TZ change on a real phone</td><td>DEV-7/DEV-71 device protocol (ROUTINE\_V3\_DEVICE\_QA\_2026-10-05.md) — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>[DEV-6](https://shanivar.atlassian.net/browse/DEV-6) FOCUS-3 All-day preset is 00:00–23:59 and copy implies persi</td><td>Completed</td><td>No one-minute daily gap</td><td>main@20fb519 test/FocusWindowRecurrenceTest.fullCalendarDayCoversEndOfSelectedDayWithNoOneMinuteGap; fullCalendarDayAllSevenDaysStaysActiveAcrossMidnightWithNoGap; FocusScheduleDraftTest.allDayAndDaysPresetsAreValidFullCalendarDays</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>No UI/marketing string claims continuous/unbreakable protection</td><td>No automated copy scan; manual review of app + site + store (DEV-88 checklist)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Known bypasses stay disclosed (force-stop, revoked perms, browsers, 2nd device)</td><td>Manual copy review of block overlay/Routine protection screen</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>[DoD] Full-day across midnight on a real phone</td><td>DEV-7/DEV-71 device protocol (ROUTINE\_V3\_DEVICE\_QA\_2026-10-05.md) — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>[DEV-7](https://shanivar.atlassian.net/browse/DEV-7) Run the 8-check physical-device protocol — it has never exec</td><td>Pending</td><td>Evidence recorded with raw output</td><td>Device protocol run log (not started; founder paused 5 Oct)</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>Skipped integration test recorded as NOT RUN</td><td>QA reporting rule (this plan §5); androidTest classes never executed</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>Second OEM covered or support claim narrowed</td><td>Device matrix: Pixel + Xiaomi/Realme/Vivo/Samsung class</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>[DEV-10](https://shanivar.atlassian.net/browse/DEV-10) Keep essential-access stop, and keep no-snooze on the block</td><td>Open</td><td>(derived) Block screen has no snooze / five-more-minutes</td><td>No test asserts absence of snooze on BlockOverlayActivity</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>(derived) Essential-access stop works with no payment, practice or server</td><td>main@20fb519 test/LocalProtectionTest.stopNowIsImmediateAndIdempotent; SadhanaFocusLockTest.allModeBlocksEverythingExceptEssentialAndOwn</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>[DoD] Stop path on a real phone, offline, signed out</td><td>DEV-7/DEV-71 device protocol (ROUTINE\_V3\_DEVICE\_QA\_2026-10-05.md) — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ No AC section in Jira — ACs derived from description; return to owner to confirm.</td><td></td><td></td></tr>
	<tr><td>[DEV-21](https://shanivar.atlassian.net/browse/DEV-21) Reboot catch-up resumes local protection with no session — f</td><td>Completed</td><td>(derived) Reboot inside a valid window resumes local protection with no account/network</td><td>main@20fb519 test/BootRescheduleCatchUpTest.rebootInsideALiveWindowResumesLocalProtectionWithNoSession; catchUpDoesNotRequireASession</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) Reboot after window ended does not start protection</td><td>main@20fb519 test/BootRescheduleCatchUpTest.rebootAfterTheWindowEndedDoesNotStartProtectionButStillRearms; rebootBeforeTheWindowDoesNotStartProtection</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) Next alarm re-armed either way</td><td>main@20fb519 test/BootRescheduleCatchUpTest.catchUpStillRearmsWhenTheDeviceCannotEnforce; BootReconcileActionsTest.appUpdateReconcilesAlarmsAndWindows</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>[DoD] Real reboot inside/after a window</td><td>DEV-7/DEV-71 device protocol (ROUTINE\_V3\_DEVICE\_QA\_2026-10-05.md) — NOT RUN (DEV-21 row of device checklist)</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Passes in unit tests; real-device verification pending under DEV-7.</td><td></td><td></td></tr>
	<tr><td>[DEV-57](https://shanivar.atlassian.net/browse/DEV-57) epic container</td><td>Open</td><td>— no ACs (container)</td><td>—</td><td>—</td></tr>
	<tr><td>[DEV-67](https://shanivar.atlassian.net/browse/DEV-67) Audit protection capabilities and Android platform constrain</td><td>Open</td><td>Capability table reviewed by Tester</td><td>Doc review</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>Every limit has user copy and a test in B5 (DEV-71)</td><td>Depends on DEV-71 suite</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Feeds Marketing claim checklist (G4/DEV-88)</td><td>Doc cross-check</td><td>manual-only</td></tr>
	<tr><td>[DEV-68](https://shanivar.atlassian.net/browse/DEV-68) Specify protection-window behaviour and user controls</td><td>Open</td><td>Spec covers all states</td><td>Doc review vs FocusScheduleStatusResolver states</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>Tester converts spec to test cases</td><td>Not started (spec not written)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Founder approves copy</td><td>Founder sign-off</td><td>manual-only</td></tr>
	<tr><td>[DEV-69](https://shanivar.atlassian.net/browse/DEV-69) Record the intervention choice on the block overlay (on-devi</td><td>Open</td><td>Choice stored exactly once per overlay (incl. rerender, process death)</td><td>Not built — no test</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Analytics event fires once</td><td>Not built; depends on DEV-65 contract; verify in DEV-83</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>No package name leaves the device</td><td>Not built — needs payload/network inspection test</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>[DoD] Overlay choice on a real phone</td><td>DEV-71</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>[DEV-70](https://shanivar.atlassian.net/browse/DEV-70) Detect and surface protection failures (service killed, perm</td><td>Open</td><td>Killed service shows off/unknown state</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Revoked permission shows the fix path</td><td>main@20fb519 test/FocusScheduleStatusTest.revoked\* (status only; no detection of runtime revocation while active)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Failure appears in crash reporting</td><td>Depends on DEV-14 (not built)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>[DoD] OEM battery killer / revoke on real phones</td><td>DEV-71</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>[DEV-71](https://shanivar.atlassian.net/browse/DEV-71) QA suite: protection permissions, sessions, reboot and recov</td><td>Open</td><td>AC1 Suite executed on real devices with evidence (umbrella)</td><td>Pixel + OEM run — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>AC2 Defects logged with severity</td><td>Defect register (this deliverable)</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>P1 Usage access revoked while window active → NEEDS\_DEVICE\_ACCESS + repair path</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md P1 (adb appops set … GET\_USAGE\_STATS ignore)</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>P2 Overlay (SYSTEM\_ALERT\_WINDOW) revoked → honest state, no false Active</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md P2</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>P3 Exact-alarm access denied → protection window not Ready</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md P3 (appops SCHEDULE\_EXACT\_ALARM deny)</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>S1 Window START/END fire on time; blocked app shows overlay (flash disclosed)</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md S1</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>S2 Two adjacent/overlapping windows; END of one keeps the other (DEV-2)</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md S2</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>S3 Overnight 22:00→06:00 + full calendar day across midnight (DEV-5/6)</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md S3 (adb clock set)</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>S4 Local Stop works offline and signed out (DEV-4/10); block screen has no snooze</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md S4 (airplane mode)</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>R1 Reboot inside window resumes; after window does not (DEV-21)</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md R1 (adb reboot)</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>R2 Process death / force-stop of FocusGuardService mid-window</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md R2 (am kill / am force-stop)</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>R3 Doze: boundary delivered after deviceidle force-idle (DEV-1 late START)</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md R3 (dumpsys deviceidle force-idle)</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>R4 Time-zone change + DST fall-back 25 Oct (Europe/Berlin)</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md R4 (settings put global auto\_time\_zone 0; setprop persist.sys.timezone)</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>R5 App update (MY\_PACKAGE\_REPLACED) re-arms windows (B2)</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md R5 (adb install -r)</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>D1 OEM battery saver / app killer (Xiaomi/Realme/Vivo/Samsung) keeps FocusGuardService</td><td>Real OEM phone only — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>D2 Long run: protection survives overnight on battery, low-battery mode</td><td>Real phone only — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>D3 Same suite repeated on a stock Pixel</td><td>Real Pixel — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
</table>

_Epic subtotal:_ 66 ACs — automated-pass 19, manual-only 7, real-device-only (NOT RUN) 17, emulator-pending 12, no test (gap) 11

## Epic C — Routine ([DEV-58](https://shanivar.atlassian.net/browse/DEV-58))

<table fit-page-width="true" header-row="true">
	<tr><td>Ticket</td><td>Jira status</td><td>AC</td><td>Test (file · class.method / pgTAP / manual)</td><td>Status</td></tr>
	<tr><td>[DEV-11](https://shanivar.atlassian.net/browse/DEV-11) Routine V3 — build the day plan, alarm, and protection. Sadh</td><td>Work in progress</td><td>(derived) 1 Local reboot recovery, no session</td><td>see DEV-21 / BootRescheduleCatchUpTest</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) 2 All-day covers calendar day; no 'unbreakable' copy</td><td>see DEV-6</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) 3 Ending one window doesn't release another</td><td>see DEV-2 / FocusWindowEndHandlerTest</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) 4 Saved ≠ Active until engine confirms</td><td>see DEV-3 / FocusScheduleStatusTest</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) 5 Two destinations Routine + Sadhana</td><td>main@20fb519 test/RoutineShellTest; ShanivarShellTest</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) 6 Local day plan; saving an activity does not arm an alarm</td><td>main@20fb519 test/RoutineProtectionTest.activityOrAnchorEditsNeverTouchTheRegistry; AndroidProtectionGatewayTest.savingThePlanReplacesTheRegistryAndActivitiesDoNot</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) 7 Timeline and Edit Routine from stored data</td><td>main@20fb519 test/RoutineTimelineTest; RoutinePlanRepositoryTest</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) 8 Multiple windows, overnight, installed apps, honest permissions, local stop</td><td>main@20fb519 test/RoutineProtectionTest (31 tests); FocusSchedulerRegistryTest</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) 9 Physical phone proof, no emulator</td><td>DEV-7/DEV-71 device protocol (ROUTINE\_V3\_DEVICE\_QA\_2026-10-05.md) — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Ordered build list used as ACs. ROUTINE\_V3 flag still false on main.</td><td></td><td></td></tr>
	<tr><td>[DEV-12](https://shanivar.atlassian.net/browse/DEV-12) Align the Steady UI to its wireframe contract — built before</td><td>Pending</td><td>All 66 breathing tests still pass</td><td>main@20fb519 test/Breathing\*Test</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>200% font: Begin/Pause/End/Back reachable</td><td>Not built (parked)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>No per-second countdown announcement</td><td>Not built (parked)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Native screenshots vs reference</td><td>Not built (parked)</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Parked; re-scope (Sadhana V1 replaces Steady).</td><td></td><td></td></tr>
	<tr><td>[DEV-13](https://shanivar.atlassian.net/browse/DEV-13) Extract UI strings to strings.xml — blocks Hindi and all loc</td><td>Open</td><td>No user-facing literal left in Kotlin</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>All tests pass with string resources</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Locale switch changes a visible string E2E</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Later.</td><td></td><td></td></tr>
	<tr><td>[DEV-23](https://shanivar.atlassian.net/browse/DEV-23) Visual system: circles and connecting marks across the app</td><td>Open</td><td>(derived) No circle/mark paces breath; static under reduced motion</td><td>main@20fb519 test/SadhanaSpineTest.rootPulseIsRandomisedNotPeriodicAndStaticUnderReducedMotion; shimmerIsRandomisedNotPeriodic (Sadhana only)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>(derived) No scores/ranks</td><td>Copy review</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Later. No AC section.</td><td></td><td></td></tr>
	<tr><td>[DEV-24](https://shanivar.atlassian.net/browse/DEV-24) Initiation step 1: onboarding draft persistence, account ada</td><td>Work in progress</td><td>(derived) Draft survives process death, resumes at checkpoint</td><td>main@20fb519 test/InitiationControllerTest.resumesAtPersistedCheckpointAfterProcessDeath; InitiationOnboardingTest</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) Google/email/guest adapters (real providers stubbed)</td><td>main@20fb519 test/InitiationControllerTest.googleAndEmailHonestlyUnavailableGuestStillWorks</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) Guest→account migration without duplicates</td><td>Real providers not built (DEV-25)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>(derived) Cancel/error/offline/code-expiry keep the draft</td><td>main@20fb519 test/InitiationOnboardingTest (partial — no code-expiry provider)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ No AC section in Jira.</td><td></td><td></td></tr>
	<tr><td>[DEV-26](https://shanivar.atlassian.net/browse/DEV-26) Initiation Compose screens from wireframe initiation-v1</td><td>Work in progress</td><td>(derived) Screens faithful to initiation-v1 wireframe</td><td>Screenshot comparison /workspace/qa/initiation/\*.png</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>(derived) Guest works end to end</td><td>main@20fb519 test/InitiationControllerTest.guestFlowEndToEndUsesRealPlan; InitiationUiTest</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) No pacing, still planet, reduced motion</td><td>main@20fb519 test/InitiationSittingTest; InitiationControllerTest.guidanceIsTextOnlyByRemainingSeconds</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) Step 6 accessibility review + device QA</td><td>Not run</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ No AC section in Jira.</td><td></td><td></td></tr>
	<tr><td>[DEV-52](https://shanivar.atlassian.net/browse/DEV-52) Routine schema v2: routine\_plans revision/time\_zone/practice</td><td>Open</td><td>routine\_plans revision/time\_zone/practice\_local (forward-only)</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>put\_routine RPC: match → write+increment; mismatch → 409</td><td>Not built (dev45@2fce42d SyncEngineErrorClassTest.routineParksWhilePutRoutineIsMissing… parks client-side)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Direct client insert/update revoked; only RPC writes</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Protection windows have one home (decision record)</td><td>Decision pending</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>Package names not sent to server without founder approval</td><td>dev45@2fce42d SyncWorkManagerTest.routinePayloadStripsProtectedPackagesUntilDev52</td><td>pending (in QA)</td></tr>
	<tr><td>″</td><td></td><td>409 surfaced, rebase + retry once</td><td>dev45@2fce42d SyncEngineErrorClassTest.routineConflictParksAndPublishesTheCanonicalRow</td><td>pending (in QA)</td></tr>
	<tr><td>″</td><td></td><td>pgTAP+integration: stale 409, increment, bad TZ, cross-user, RoutineSync contract</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>[DEV-53](https://shanivar.atlassian.net/browse/DEV-53) Device-truth tables: alarm\_configs, alarm\_observations, devi</td><td>Open</td><td>alarm\_configs / alarm\_observations / device\_observations tables</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>device\_id is random per-install UUID</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Append-only/last-value; own-row RLS + DEV-42 baseline</td><td>Would be enforced by dev42@ba38de6 supabase/tests/01\_baseline\_guard (table missing)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Server never overrides local alarm state</td><td>main@20fb519 test/DeviceObservationsTest.userDisabledStaysOffWhateverObservationSays; oldConfigRevisionNeverApplies (client side only)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Retention rule documented and enforced</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Outbox kinds + payload builders with tests</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>pgTAP: cross-user, checks, device\_id format</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>[DEV-58](https://shanivar.atlassian.net/browse/DEV-58) epic container</td><td>Open</td><td>— no ACs (container)</td><td>—</td><td>—</td></tr>
	<tr><td>[DEV-72](https://shanivar.atlassian.net/browse/DEV-72) Define routine completion semantics and make completion idem</td><td>Open</td><td>Integration: 5 identical completion requests → 1 row</td><td>Not built (completion semantics undefined)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Offline replay → no duplicates</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>[DEV-73](https://shanivar.atlassian.net/browse/DEV-73) First meaningful action flow: new user completes one action</td><td>Open</td><td>New user reaches first completed action unaided (usability test)</td><td>Moderated usability session</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>Failed save never shows success</td><td>main@20fb519 test/RoutineProtectionTest.invalidSaveDoesNotSync; saveMessagesNeverClaimActive (protection only; no generic failed-save test)</td><td>no test (gap)</td></tr>
	<tr><td>[DEV-74](https://shanivar.atlassian.net/browse/DEV-74) Missed-step recovery and non-punitive return</td><td>Open</td><td>After 1, 3, 7 missed days a clear way back</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Copy reviewed for guilt</td><td>Copy review</td><td>manual-only</td></tr>
	<tr><td>[DEV-75](https://shanivar.atlassian.net/browse/DEV-75) Verify alarm and notification behaviour on supported real de</td><td>Open</td><td>AC1 All alarm/notification cases pass or have documented defects (umbrella)</td><td>Pixel + OEM run — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>AC2 No unresolved critical</td><td>Defect register gate</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>A1 Wake alarm fires at set time from locked screen; full-screen intent shown (Android 14+)</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md A1</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>A2 Alarm fires in Doze (setAlarmClock) and after app swipe-away</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md A2</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>A3 Alarm re-armed after reboot and app update; recurrence recomputed after fire (0008)</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md A3</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>A4 Exact-alarm permission denied → honest 'permission required', no silent failure</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md A4</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>A5 Notification permission (POST\_NOTIFICATIONS) denied → state shown</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md A5</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>A6 DST fall-back / TZ change: one alarm, at local wall-clock time</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md A6</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>A7 Accessible dismissal path always available; QR/photo path with emulator camera</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md A7</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>A8 Alarm during a Sadhana practice interrupts and is recorded truthfully</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md A8</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>A9 Hard cap ends a ringing alarm; no endless ring</td><td>Emulator plan device\_case\_plan\_dev71\_dev75.md A9</td><td>emulator-pending</td></tr>
	<tr><td>″</td><td></td><td>D1 Movement dismissal with real step detector / ACTIVITY\_RECOGNITION</td><td>Real phone sensors — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>D2 Alarm actually wakes a person: volume, vibration, speaker, DND interplay</td><td>Real phone — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>D3 OEM battery killer does not suppress the alarm (Xiaomi/Realme/Vivo/Samsung)</td><td>Real OEM phone — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>D4 Device powered off across alarm time; behaviour on power-on</td><td>Real phone — NOT RUN</td><td>real-device-only (NOT RUN)</td></tr>
</table>

_Epic subtotal:_ 61 ACs — automated-pass 13, manual-only 6, real-device-only (NOT RUN) 8, emulator-pending 9, no test (gap) 23, pending (in QA) 2

## Epic D — Sadhana ([DEV-59](https://shanivar.atlassian.net/browse/DEV-59))

<table fit-page-width="true" header-row="true">
	<tr><td>Ticket</td><td>Jira status</td><td>AC</td><td>Test (file · class.method / pgTAP / manual)</td><td>Status</td></tr>
	<tr><td>[DEV-16](https://shanivar.atlassian.net/browse/DEV-16) DECISION: Sadhana Phase 2 — gamification and the intensive-p</td><td>Pending</td><td>(derived) Founder decision recorded (Option A/B/C)</td><td>Decision — no test</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Decision ticket; later.</td><td></td><td></td></tr>
	<tr><td>[DEV-27](https://shanivar.atlassian.net/browse/DEV-27) Sadhana S1: practice domain + research-only lock (decision 0</td><td>Work in progress</td><td>(derived) Research-only lock is deterministic; activity never unlocks it</td><td>main@20fb519 test/SadhanaPracticeTest.researchOnlyNeverLaunchesEvenWhenUnreviewedAllowed; intensiveRiskNeverLaunchesEvenIfMislabelledReleased; SadhanaRecordTest.longPracticeAloneNeverAdvancesBeyondArrive</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ No AC section.</td><td></td><td></td></tr>
	<tr><td>[DEV-28](https://shanivar.atlassian.net/browse/DEV-28) Sadhana S2: attempt timing + events (monotonic active second</td><td>Work in progress</td><td>(derived) Active foreground seconds only; pauses excluded</td><td>main@20fb519 test/SadhanaAttemptTest.pausedAndBackgroundTimeExcluded; PracticeAttemptTest.pausedTimeNotCounted</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) anchor\_changed recorded</td><td>main@20fb519 test/SadhanaAttemptTest.anchorChangePausesAndIsLoggedNotFailure</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) Process death → interrupted, never completed from wall clock</td><td>main@20fb519 test/SadhanaControllerTest.processDeathRestoresInterruptedNotCompleted</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>[DoD] Process death / lock / call on a real phone</td><td>DEV-34 device QA</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ No AC section.</td><td></td><td></td></tr>
	<tr><td>[DEV-29](https://shanivar.atlassian.net/browse/DEV-29) Sadhana S3: record, reflection, independent practice, guidan</td><td>Work in progress</td><td>(derived) Optional reflection; editable/deletable self-reports</td><td>main@20fb519 test/SadhanaRecordTest.reflectionOptionalAndClearable; selfReportsEditableAndDeletable</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) 'Use fewer prompts' control</td><td>main@20fb519 test/SadhanaRecordTest.guidanceReducesOnlyByPreference</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) Orbit points from evidence families; self-reports never scores</td><td>main@20fb519 test/SadhanaRecordTest.orbitEventsFromEvidenceFamilies; LivingOrbitTest.independentRecordMustBeSelfReported</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ No AC section.</td><td></td><td></td></tr>
	<tr><td>[DEV-30](https://shanivar.atlassian.net/browse/DEV-30) Sadhana S4: Living Spine data (3 layers, SourceRecord, centr</td><td>Work in progress</td><td>(derived) Layers separate; centres study-only until verified</td><td>main@20fb519 test/SadhanaSpineTest.noCentreCanLaunchPracticeUntilVerified; plainLayerNamesNoTraditionOrSanskrit</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ No AC section.</td><td></td><td></td></tr>
	<tr><td>[DEV-31](https://shanivar.atlassian.net/browse/DEV-31) Sadhana S5: screens A - home, Living Spine, centre drawer, P</td><td>Work in progress</td><td>(derived) Screens faithful to sadhana-v1</td><td>Screenshots /workspace/qa/sadhana/\*.png</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>(derived) No energy/health reading</td><td>main@20fb519 test/SadhanaSpineTest.noEnergyOrHealthReadingInPlainText</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ No AC section.</td><td></td><td></td></tr>
	<tr><td>[DEV-32](https://shanivar.atlassian.net/browse/DEV-32) Sadhana S6: screens B - detail, prepare, player, return/refl</td><td>Work in progress</td><td>(derived) Stationary gate, End always reachable, no navigation away</td><td>main@20fb519 test/SadhanaControllerTest.beginNeedsStationary; cannotNavigateAwayFromActivePractice; SadhanaAccessibilityTest</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) Reduced motion / still planet</td><td>main@20fb519 test/SadhanaLoadingTest</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ No AC section.</td><td></td><td></td></tr>
	<tr><td>[DEV-33](https://shanivar.atlassian.net/browse/DEV-33) Sadhana S7: screens C - Record, Without app, Deep study + re</td><td>Work in progress</td><td>(derived) Research-only methods only as locked study cards</td><td>main@20fb519 test/SadhanaPracticeTest.studyOnlyAndQualifiedContextNeverLaunch; SadhanaUiTest</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ No AC section.</td><td></td><td></td></tr>
	<tr><td>[DEV-34](https://shanivar.atlassian.net/browse/DEV-34) Sadhana S8: content review manifests, /v1 Sadhana API + sync</td><td>Open</td><td>(derived) Content review manifests recorded per practice</td><td>main@20fb519 test/ContentReviewTest.releaseLaunchNeedsACompleteMatchingManifest (gate only; manifests empty)</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>(derived) Device QA: process death, screen lock, calls</td><td>Not run</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>(derived) TalkBack + 200% font</td><td>main@20fb519 test/SadhanaAccessibilityTest.at200PercentFont (font only; TalkBack not run)</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ No AC section. Release blocker.</td><td></td><td></td></tr>
	<tr><td>[DEV-35](https://shanivar.atlassian.net/browse/DEV-35) Finalization B3–B8: sync outbox, content review gate, Choose</td><td>Done</td><td>B3 Local idempotent sync outbox</td><td>main@20fb519 test/SyncOutboxTest.queueIsIdempotentPersistedAndDrainsInOrder</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>B4 Release launch needs a complete matching manifest</td><td>main@20fb519 test/ContentReviewTest.releaseLaunchNeedsACompleteMatchingManifest; SadhanaControllerTest.releaseBuildBlocksUnreviewed</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>B5 Choose linked to a saved enabled Routine window</td><td>main@20fb519 test/RoutineBoundaryLinkTest.onlyAnEnabledSavedWindowLinksChoose</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>B6 Onboarding 60 s attempt imported once</td><td>main@20fb519 test/SadhanaControllerTest.initiationAttemptImportedOnceAndUnfinishedIgnored</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>B7 Edit actual minutes of a self-report</td><td>main@20fb519 test/SadhanaControllerTest.selfReportActualMinutesEditableAndPersisted</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>B8 End visible at 200% font and landscape</td><td>main@20fb519 test/SadhanaAccessibilityTest.at200PercentFont; inLandscape</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ QA pass, except B3 → defect DEV-38. ACs = checklist items B3–B8.</td><td></td><td></td></tr>
	<tr><td>[DEV-36](https://shanivar.atlassian.net/browse/DEV-36) Sadhana Stage 1 (Spine base): attention practice, ambient fr</td><td>Open</td><td>1 Stage 1 identity, text guidance, audio must not pace breath</td><td>main@20fb519 test/SadhanaStage1Test.stage1Is60sNoBreathPacingAndHonestCopy (text only; no audio yet)</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>2 60 s start, end anytime, focus lock, posture</td><td>main@20fb519 test/SadhanaFocusLockTest; SadhanaStage1Test</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>3 Licensed audio, no claims, merged into practice</td><td>Not built (DEV-37 spike)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>4 Check-in = exactly 2–3 one-tap options</td><td>Shipped 47e668a card has stepper+chips+note — would FAIL; no test asserts ≤3 taps</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>5 Founder-approved public wording only</td><td>Copy review</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>6 No build until founder answers</td><td>Process</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Do not build yet (founder).</td><td></td><td></td></tr>
	<tr><td>[DEV-37](https://shanivar.atlassian.net/browse/DEV-37) Spike: Stage 1 ambient frequency — sound sourcing, licensing</td><td>Open</td><td>(derived) Spike note: Hz options, licence, non-claim copy, go/no-go</td><td>Doc review</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Spike; no AC section.</td><td></td><td></td></tr>
	<tr><td>[DEV-39](https://shanivar.atlassian.net/browse/DEV-39) Bundled techniques.json contains timed box-breathing script</td><td>Open</td><td>(derived) No breath counts / timed pacing in techniques.json or .md sources</td><td>dev39@8e122a8 test/TechniqueContentSafetyTest.techniquesNeitherPaceTheBreathNorMakeBodyClaims; markdownSourcesNeitherPaceNorClaimNorTime; patternsCatchTheOriginalBoxBreathingScript</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) No physiological claims</td><td>dev39@8e122a8 test/TechniqueContentSafetyTest.techniquesNeitherPaceTheBreathNorMakeBodyClaims</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) md mirror matches shipped asset</td><td>dev39@8e122a8 test/TechniqueContentSafetyTest.mdMirrorMatchesTheShippedAsset</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Failed once on 'rounds' (3471087); passed at 8e122a8 incl. mutation runs (/workspace/qa/dev39-mut-\*.log).</td><td></td><td></td></tr>
	<tr><td>[DEV-51](https://shanivar.atlassian.net/browse/DEV-51) Sadhana content tables (practice\_definitions, content\_review</td><td>Open</td><td>Content tables mirror Kotlin</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>RLS serves only approved+manifested, never research-only</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Tradition sources only in Deep study</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>seed.sql only founder-reviewed content</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Seed text post-DEV-39, nothing from legacy backend</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Offline fallback never launches unreviewed</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>pgTAP: draft/research invisible, no writes, manifest required</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Later.</td><td></td><td></td></tr>
	<tr><td>[DEV-54](https://shanivar.atlassian.net/browse/DEV-54) Sadhana record schema alignment: practice\_reflections table,</td><td>Open</td><td>practice\_reflections table + own-row RLS</td><td>dev54@7a56cf6(+uncommitted) supabase/tests/07\_sadhana\_record.test.sql ('A sees only own reflections', 'A cannot write a reflection for B')</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>self\_reports private\_by\_default, drift\_reasons check, tombstone delete E2E</td><td>dev54@7a56cf6(+uncommitted) supabase/tests/07\_sadhana\_record.test.sql ('an invalid drift reason is rejected', 'self-report tombstone deletes end to end')</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>practice\_attempts anchor/anchor\_changes/events(32 KB)/created\_at; SyncPayloads.attempt</td><td>dev54@7a56cf6(+uncommitted) supabase/tests/07 ('anchor outside BREATH/ROOM is rejected', 'an oversized events value is rejected')</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>orbit\_events entity\_id/created\_at/type+source checks; append-only</td><td>dev54@7a56cf6(+uncommitted) supabase/tests/07 ('invalid orbit type is rejected', 'an orbit update is denied')</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Payload builders match every column; contract test</td><td>Not found</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>pgTAP coverage list</td><td>dev54@7a56cf6(+uncommitted) supabase/tests/07\_sadhana\_record.test.sql</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>SelfCheck stays local-only (no table)</td><td>dev54@7a56cf6(+uncommitted) supabase/tests/07 ('no SelfCheck table (local-only)')</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Work in progress (uncommitted in worktree); tests exist but not yet run by QA → counted as gap until QA run.</td><td></td><td></td></tr>
	<tr><td>[DEV-59](https://shanivar.atlassian.net/browse/DEV-59) epic container</td><td>Open</td><td>— no ACs (container)</td><td>—</td><td>—</td></tr>
	<tr><td>[DEV-76](https://shanivar.atlassian.net/browse/DEV-76) Map each initial practice to a user need, expected action an</td><td>Open</td><td>Practice→need→action→clarity table complete</td><td>Doc review</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>Content issues ticketed</td><td>Doc review</td><td>manual-only</td></tr>
	<tr><td>[DEV-77](https://shanivar.atlassian.net/browse/DEV-77) Optional one-tap awareness prompt after a distraction or com</td><td>Open</td><td>Skip works in one tap</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Stored locally as self-report, never a score</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Later.</td><td></td><td></td></tr>
	<tr><td>[DEV-78](https://shanivar.atlassian.net/browse/DEV-78) Connect relevant Mano Vijnana content to practical exercises</td><td>Open</td><td>Links work</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Manifest (DEV-34) approved</td><td>Founder</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Later.</td><td></td><td></td></tr>
	<tr><td>[DEV-89](https://shanivar.atlassian.net/browse/DEV-89) Safety audit: Breathing\* screens and block-screen "breathe"</td><td>Open</td><td>No reachable/bundled UI, animation or asset paces breathing</td><td>dev39@8e122a8 test/TechniqueContentSafetyTest.breathingCatalogueGuidanceNeitherPacesNorClaims (techniques + catalogue only); Breathing\*.kt screens and block-screen 'breathe' path not covered</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>No breathing physiological/medical/energy claims</td><td>main@20fb519 test/BreathingCatalogTest.c10\_noShippedTextMakesAPhysiologicalOrSpiritualPromise; dev39@8e122a8 test/TechniqueContentSafetyTest.techniquesNeitherPaceTheBreathNorMakeBodyClaims</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>Block-screen option renamed/re-scoped as non-paced pause</td><td>Not built; manual check of BlockOverlayActivity/LocalPauseScreen</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Automated safety test covers these files and fails on old content</td><td>dev39@8e122a8 test/TechniqueContentSafetyTest.patternsCatchTheOriginalBoxBreathingScript (covers techniques.json + .md only)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Tester verifies point by point</td><td>Manual audit — not run</td><td>manual-only</td></tr>
</table>

_Epic subtotal:_ 59 ACs — automated-pass 24, manual-only 10, real-device-only (NOT RUN) 3, no test (gap) 22

## Epic E — Security, privacy & reliability ([DEV-60](https://shanivar.atlassian.net/browse/DEV-60))

<table fit-page-width="true" header-row="true">
	<tr><td>Ticket</td><td>Jira status</td><td>AC</td><td>Test (file · class.method / pgTAP / manual)</td><td>Status</td></tr>
	<tr><td>[DEV-8](https://shanivar.atlassian.net/browse/DEV-8) Rotate the two leaked credentials — both verified still vali</td><td>Open</td><td>Every rotated credential verified rejected</td><td>Manual API check (founder op) — not done</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>Production health 200 / database ok</td><td>Manual check</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>Decision 0005 step 1 marked complete</td><td>Doc</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Release blocker. Founder-only.</td><td></td><td></td></tr>
	<tr><td>[DEV-9](https://shanivar.atlassian.net/browse/DEV-9) Add a migration runner before the first knowledge table exis</td><td>Pending</td><td>All backend tests pass</td><td>backend/test/\*.test.js</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Rollback demonstrated</td><td>Not done</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>One migration through runner</td><td>Not done</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Pooler path unchanged</td><td>Manual</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Re-scope: legacy Render only (DEV-40).</td><td></td><td></td></tr>
	<tr><td>[DEV-17](https://shanivar.atlassian.net/browse/DEV-17) Add release signing config and resolve api.thehybridmonk.com</td><td>Open</td><td>(PM-refined) Keystore custody (founder)</td><td>Manual</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>signingConfig from env; build fails if missing</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>prodRelease/stagingRelease endpoints</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>DNS only if Render kept</td><td>Decision</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>release.yml draft release + SHA-256</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Key SHA-1/256 recorded</td><td>Manual</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>versionCode auto-increments</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Content-review gate respected in release</td><td>main@20fb519 test/ContentReviewTest; SadhanaControllerTest.releaseBuildBlocksUnreviewed</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>Play declarations recorded</td><td>Manual</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>Signed APK installs on phone and syncs</td><td>Real device</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ ACs from PM comment 17 Oct (awaiting founder).</td><td></td><td></td></tr>
	<tr><td>[DEV-18](https://shanivar.atlassian.net/browse/DEV-18) Confirm whether GEMINI\_API\_KEY is set on Render — every AI e</td><td>Pending</td><td>(derived) Render GEMINI\_API\_KEY state recorded</td><td>Manual dashboard check</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Not in first release.</td><td></td><td></td></tr>
	<tr><td>[DEV-22](https://shanivar.atlassian.net/browse/DEV-22) Future sign-in options: phone, WhatsApp, SMS, authenticator</td><td>Open</td><td>(derived) none — future</td><td>—</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Later; no ACs.</td><td></td><td></td></tr>
	<tr><td>[DEV-25](https://shanivar.atlassian.net/browse/DEV-25) Accounts + cloud sync: real providers, backend /v1, hosting</td><td>Open</td><td>Guest = anon user once per install</td><td>dev45@2fce42d SyncSessionTest.guestSignsUpOncePerInstallAndSurvivesRestart</td><td>pending (in QA)</td></tr>
	<tr><td>″</td><td></td><td>Email OTP + invite gate; guest upgrade keeps user\_id</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Google via Credential Manager; linkIdentity keeps user\_id</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>enable\_manual\_linking on hosted projects</td><td>Cloud config — Backend (QA must not touch cloud)</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>Unconfigured providers hidden / 'not available yet'</td><td>main@20fb519 test/InitiationControllerTest.googleAndEmailHonestlyUnavailableGuestStillWorks</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>Sign-out clears tokens; no cross-user send</td><td>dev45@2fce42d SyncSessionTest.signOutClearsTokensAndNotifies; SyncWorkManagerTest.signOutKeepsItemsForTheOldUserUnsent</td><td>pending (in QA)</td></tr>
	<tr><td>″</td><td></td><td>Tests: guest→email/Google same user\_id; wrong/expired OTP fail</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Privacy notice updated before non-founder testers</td><td>Founder</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>Legacy /auth/\* unused</td><td>Code review</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ ACs from PM comment 17 Oct.</td><td></td><td></td></tr>
	<tr><td>[DEV-38](https://shanivar.atlassian.net/browse/DEV-38) Sync outbox never queues own-practice self-reports, their ed</td><td>Open</td><td>(derived) recordOwn queues a self-report</td><td>main@20fb519 test/SyncOutboxGapsTest.recordOwnQueuesSelfReport</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) Edits replace the queued self-report</td><td>main@20fb519 test/SyncOutboxGapsTest.adjustOwnActualReplacesQueuedSelfReport</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) Delete queues a tombstone over pending upsert</td><td>main@20fb519 test/SyncOutboxGapsTest.deleteOwnQueuesTombstoneOverPendingUpsert; senderDeletesTombstonesAndUpsertsTheRest</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) Onboarding import queued once (+ legacy backfill)</td><td>main@20fb519 test/SyncOutboxGapsTest.onboardingImportQueuesAttemptOnce; backfillQueuesOnboardingAttemptAlreadyImportedByOldBuild; backfillIsIdempotentByAttemptId; backfillRunsOnceAcrossRestarts</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>(derived) Routine changes queued</td><td>main@20fb519 test/SyncOutboxGapsTest.routineChangeQueuesRoutinePlan</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Failed once (legacy-import gap at 86b558a); passed at 20fb519 (/workspace/qa/dev38/rerun-\*). Jira still Open.</td><td></td><td></td></tr>
	<tr><td>[DEV-40](https://shanivar.atlassian.net/browse/DEV-40) DECISION: Supabase is the only backend for the phone; retire</td><td>Open</td><td>1 Decision record 0019 lists every ShanivarApi endpoint + disposition</td><td>Doc review</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>2 Founder confirms in writing</td><td>Founder</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>3 Tables → DEV-42; Edge Functions → own tickets</td><td>Doc review</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>4 Render untouched until replacement verified</td><td>Process</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>5 Legacy data carry-over stated</td><td>Doc review</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>6 0001/0004/DEV-9 marked legacy-only</td><td>Doc review</td><td>manual-only</td></tr>
	<tr><td>[DEV-41](https://shanivar.atlassian.net/browse/DEV-41) Supabase: add a staging project next to shanivar-prototype,</td><td>Open</td><td>1 shanivar-staging exists (ap-south-1)</td><td>Backend/founder (QA must not touch cloud)</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>2 Auth settings recorded for both</td><td>Doc review</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>3 Keys in password manager + GH env secrets only</td><td>Secret scan</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>4 Plan/pause/backup/spend cap written</td><td>Doc review</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>5 Least-privilege access; revocable CI token</td><td>Manual</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>6 /auth/v1/health 200 on mobile data</td><td>Real phone</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>[DEV-42](https://shanivar.atlassian.net/browse/DEV-42) Schema hygiene + RLS hardening on all sync tables: updated\_a</td><td>Open</td><td>1 evidence\_type checks (forward-only)</td><td>dev42@ba38de6 supabase/tests/01\_baseline\_guard ('…evidence\_type has a check'); 02\_rls\_isolation ('self\_report cannot be labelled app\_observed')</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>2 created\_at + set\_updated\_at() everywhere; client\_updated\_at</td><td>dev42@ba38de6 supabase/tests/01\_baseline\_guard; 03\_client\_updated\_at</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>3 Size caps (plan 64 KiB, packages 200, drift 20, id 128, note 500)</td><td>dev42@ba38de6 supabase/tests/04\_size\_caps (14 asserts) — other text columns uncapped (QA-42-03)</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>4 revoke all from anon; authenticated minimal</td><td>dev42@ba38de6 supabase/tests/05\_privileges</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>5 pgTAP: cross-user CRUD, no session, user\_id immutable, spoofing, oversize</td><td>dev42@ba38de6 supabase/tests/02\_rls\_isolation (43); 04\_size\_caps</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>6 Baseline applied to every new table DEV-47..55</td><td>dev42@ba38de6 supabase/tests/01\_baseline\_guard iterates all public tables (fails on new tables missing baseline)</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>7 on delete cascade from auth.users verified</td><td>dev42@ba38de6 supabase/tests/01\_baseline\_guard ('…references auth.users on delete cascade') — FK presence only; no delete-and-check test</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>8 Security/Performance Advisor 0 errors after deploy</td><td>Cloud advisors — not deployed; QA must not touch cloud</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Passed at ba38de6 (local stack). Findings QA-42-01..03 in defect register.</td><td></td><td></td></tr>
	<tr><td>[DEV-43](https://shanivar.atlassian.net/browse/DEV-43) Apply Supabase migrations and Edge Functions through CI: sta</td><td>Open</td><td>1 migration repair once, output pasted</td><td>Not built (no .github/)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>2 PR workflow: reset, lint, test db</td><td>Not built (no .github/)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>3 merge→staging deploy</td><td>Not built (no .github/)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>4 prod via dispatch + founder reviewer</td><td>Not built (no .github/)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>5 drift check fails job</td><td>Not built (no .github/)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>6 forward-only; destructive label</td><td>Not built (no .github/)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>7 secrets only in GH env</td><td>Not built (no .github/)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>8 seed.sql fixed</td><td>Not built (no .github/)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>9 broken migration fails CI</td><td>Not built (no .github/)</td><td>no test (gap)</td></tr>
	<tr><td>[DEV-44](https://shanivar.atlassian.net/browse/DEV-44) Android build flavours (staging/prod) carry Supabase URL and</td><td>Open</td><td>1 env flavours staging/prod side by side</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>2 URL/key from properties only</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>3 release fails without https URL/key</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>4 cleartext debug-only</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>5 CI secret scan (gitleaks)</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>6 env name shown on non-prod</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>7 API\_URL removed/flavour-aware</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>8 docs per flavour</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>[DEV-45](https://shanivar.atlassian.net/browse/DEV-45) Sync engine (Backend half): guest session, sender loop with</td><td>Open</td><td>1 SyncSession: guest once per install, encrypted tokens, refresh</td><td>dev45@2fce42d test/SyncSessionTest (8)</td><td>pending (in QA)</td></tr>
	<tr><td>″</td><td></td><td>2 Items stamped with owner user\_id, never sent as another user</td><td>dev45@2fce42d test/SyncEngineErrorClassTest.itemsOfAnotherUserAreNeverSentAndUnownedOnesAreStamped</td><td>pending (in QA)</td></tr>
	<tr><td>″</td><td></td><td>3 WorkManager outbox-flush, CONNECTED, backoff 30 s–1 h</td><td>dev45@2fce42d test/SyncWorkManagerTest; SyncOutboxEngineTest.backoffIsExponentialWithJitterFrom30sTo1h</td><td>pending (in QA)</td></tr>
	<tr><td>″</td><td></td><td>4 Error classes (retry / 401 refresh / dead-letter & continue / ordering)</td><td>dev45@2fce42d test/SyncEngineErrorClassTest (20)</td><td>pending (in QA)</td></tr>
	<tr><td>″</td><td></td><td>5 Idempotency (3 sends → 1 row, crash-after-ack, tombstone)</td><td>dev45@2fce42d test/SyncOutboxEngineTest; supabase/tests/06\_sync\_conflicts</td><td>pending (in QA)</td></tr>
	<tr><td>″</td><td></td><td>6 Conflict rule LWW on client\_updated\_at; tombstones; put\_routine 409</td><td>dev45@2fce42d supabase/tests/06\_sync\_conflicts (22); decision 0019</td><td>pending (in QA)</td></tr>
	<tr><td>″</td><td></td><td>7 Restore (only if multi-device wanted)</td><td>Founder decision pending</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>8 Offline 7 days/50 items; airplane toggles</td><td>dev45@2fce42d test/SyncOutboxEngineTest.sevenDaysOfflineFiftyItemsAllArriveOnReconnect; airplaneModeTogglesMidFlushLoseNothing</td><td>pending (in QA)</td></tr>
	<tr><td>″</td><td></td><td>9 MockWebServer per error class + WorkManager test; suite green</td><td>dev45@2fce42d test/SyncEngineErrorClassTest; SyncWorkManagerTest</td><td>pending (in QA)</td></tr>
	<tr><td>″</td><td></td><td>10 Verified once against shanivar-prototype</td><td>Cloud project — out of QA bounds; Backend to run</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ In QA now — PENDING.</td><td></td><td></td></tr>
	<tr><td>[DEV-46](https://shanivar.atlassian.net/browse/DEV-46) Edge Function delete-account: in-app account and synced-data</td><td>Open</td><td>1 delete-account verifies JWT, cascades, idempotent</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>2 service-role only in function env</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>3 Settings two-step delete; clears session+outbox</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>4 client-side export</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>5 web deletion path</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>6 Staging tests: rows gone, others untouched, old JWT rejected, logs clean</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>7 Deployed only via DEV-43</td><td>Process</td><td>manual-only</td></tr>
	<tr><td>[DEV-47](https://shanivar.atlassian.net/browse/DEV-47) Wire sync into the app (Frontend half): build the client fro</td><td>Open</td><td>1 One AppSync from BuildConfig; empty URL = sync off, tested</td><td>dev47@1e69cba test/AppSyncTest.emptyUrlOrKeyOrMissingEngineMeansDisabled; SyncStatusUiTest.syncOffRecordViewIsIdentical</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>2 Guest ensureGuest after consent, never blocks</td><td>dev47@1e69cba test/InitiationSyncTest.guestStepCallsEnsureGuestExactlyOnce; offlineGuestSignInNeverBlocksTheFlow</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>3 Flush triggers (enqueue, foreground, network, periodic), off main thread</td><td>dev47@1e69cba test/AppSyncTest.enqueueTriggersRequestFlush; foregroundRequestsFlush; SyncWiringRobolectricTest</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>4 Quiet status line, three strings, no counts/streaks</td><td>dev47@1e69cba test/SyncStatusUiTest.statusFollowsTheEngineStateExactlyThreeStrings</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>5 Dead-letter count only in debug diagnostics</td><td>dev47@1e69cba test/SyncStatusUiTest.deadLetterCountOnlyInDebugDiagnostics</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>6 Sign-out/reset clears session, no cross-user send</td><td>dev47@1e69cba test/AppSyncTest.signOutClearsSessionThroughEngine; resetUsesDev45ResetAndClearsTheSession</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>7 Compose/Robolectric tests listed</td><td>dev47@1e69cba test/SyncStatusUiTest; InitiationSyncTest; AppSyncTest</td><td>automated-pass</td></tr>
	<tr><td>″</td><td></td><td>8 Manual: attempt + self-report reach tables within 1 min (+airplane)</td><td>Needs device/emulator + staging; cloud off-limits for QA</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Developer run on branch: 548 tests, 0 fail (22:36). Not yet QA-reviewed; depends on DEV-45.</td><td></td><td></td></tr>
	<tr><td>[DEV-48](https://shanivar.atlassian.net/browse/DEV-48) Invite-code-gated email signup (Supabase Auth email OTP + se</td><td>Open</td><td>1 invite\_codes table (hash), RLS no client policy</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>2 before-user-created hook rejects bad codes</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>3 atomic redemption</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>4 guest→email gated, same user\_id</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>5 email OTP; custom SMTP in prod</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>6 founder mint/revoke snippet</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>7 rate limits</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>8 pgTAP+integration incl. 2-signup race</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>9 invite-code field, plain copy</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>[DEV-50](https://shanivar.atlassian.net/browse/DEV-50) Onboarding/profile table (profiles + onboarding draft) and g</td><td>Open</td><td>1 profiles table</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>2 auth.users trigger creates profile</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>3 guidance\_preferences table</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>4 own-row RLS + DEV-42 baseline</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>5 stale revision → 409</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>6 payload builders + outbox kinds</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>7 no birth/email/phone in profiles</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>[DEV-60](https://shanivar.atlassian.net/browse/DEV-60) epic container</td><td>Open</td><td>— no ACs (container)</td><td>—</td><td>—</td></tr>
	<tr><td>[DEV-79](https://shanivar.atlassian.net/browse/DEV-79) Cross-user access test suite for every Supabase table, RPC a</td><td>Open</td><td>Every table/RPC/Edge Function has a passing isolation test</td><td>dev42@ba38de6 supabase/tests/02\_rls\_isolation covers 5 tables; dev45@2fce42d 06 tombstones; no RPC/Edge Function yet</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Any failure is a release blocker</td><td>Release gate (template §G4)</td><td>manual-only</td></tr>
	<tr><td>[DEV-80](https://shanivar.atlassian.net/browse/DEV-80) Sensitive logging, retention, deletion and backup review</td><td>Open</td><td>Audit report (logging, retention, deletion, backups)</td><td>Not started</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Restore drill on staging</td><td>Needs DEV-41</td><td>no test (gap)</td></tr>
	<tr><td>[DEV-81](https://shanivar.atlassian.net/browse/DEV-81) Release, rollback and incident-response runbook</td><td>Open</td><td>Rollback rehearsed on staging once</td><td>Needs DEV-41; note d9168eb cannot be reverted alone (QA-42-01)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Linked to G6</td><td>Doc</td><td>manual-only</td></tr>
</table>

_Epic subtotal:_ 117 ACs — automated-pass 20, manual-only 29, real-device-only (NOT RUN) 3, no test (gap) 55, pending (in QA) 10

## Epic F — QA & release ([DEV-61](https://shanivar.atlassian.net/browse/DEV-61))

<table fit-page-width="true" header-row="true">
	<tr><td>Ticket</td><td>Jira status</td><td>AC</td><td>Test (file · class.method / pgTAP / manual)</td><td>Status</td></tr>
	<tr><td>[DEV-14](https://shanivar.atlassian.net/browse/DEV-14) Add crash reporting — a failed alarm currently leaves no tra</td><td>Open</td><td>Forced crash → retrievable local report</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Export needs explicit user action</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>No reflection/feeling/package in payload</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Network traffic inspected</td><td>Proxy capture on device</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>[DEV-15](https://shanivar.atlassian.net/browse/DEV-15) Add CI running backend and Android tests on push — two agent</td><td>Open</td><td>Workflow green on main</td><td>Not built (no .github/)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>Broken test fails workflow</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>No secrets needed</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>[DEV-19](https://shanivar.atlassian.net/browse/DEV-19) Git push cannot complete from a non-interactive session — cr</td><td>Open</td><td>Non-interactive git push succeeds</td><td>Out of QA scope (never push)</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>No credential in config/URL/files</td><td>Secret scan</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>git fetch does not hang</td><td>Manual</td><td>manual-only</td></tr>
	<tr><td>[DEV-55](https://shanivar.atlassian.net/browse/DEV-55) E2E: phone-to-Supabase round trip against staging (automated</td><td>Open</td><td>1 SupabaseSyncTest nightly vs staging</td><td>main@20fb519 test/SupabaseSyncTest.e2eGuestPushReadBack (local only; SKIPPED without -Dsupabase.e2e.url)</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>2 pgTAP vs staging after deploy</td><td>Needs DEV-41/43</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>3 Device checklist on staging build</td><td>Real phone</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>4 Offline resilience (airplane, kill mid-flush, reboot)</td><td>Real phone</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>5 >1 h idle refresh, same user\_id</td><td>Real phone</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>6 Mobile data + flaky network</td><td>Real phone</td><td>real-device-only (NOT RUN)</td></tr>
	<tr><td>″</td><td></td><td>7 Second guest isolation; delete-account</td><td>Needs DEV-46</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>8 put\_routine 409 two devices</td><td>Needs DEV-52</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>9 Tester never uses service-role key</td><td>Process rule</td><td>manual-only</td></tr>
	<tr><td>[DEV-61](https://shanivar.atlassian.net/browse/DEV-61) epic container</td><td>Open</td><td>— no ACs (container)</td><td>—</td><td>—</td></tr>
	<tr><td>[DEV-82](https://shanivar.atlassian.net/browse/DEV-82) Traceable test plan, AC-to-test matrix and defect register f</td><td>Open</td><td>Matrix covers epics A–G</td><td>/workspace/qa/dev82/ac\_test\_matrix.md (this deliverable)</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>Template used for first RC</td><td>/workspace/qa/dev82/release\_readiness\_template.md — first RC not cut</td><td>manual-only</td></tr>
	<tr><td>[DEV-83](https://shanivar.atlassian.net/browse/DEV-83) Verify analytics events match actual user actions</td><td>Open</td><td>Report per event: expected vs actual</td><td>No events exist (DEV-65 not defined)</td><td>no test (gap)</td></tr>
	<tr><td>[DEV-84](https://shanivar.atlassian.net/browse/DEV-84) End-to-end protect → act → reflect journey and regression su</td><td>Open</td><td>E2E protect→act→reflect passes on two devices</td><td>Not started; needs DEV-69/72/74</td><td>real-device-only (NOT RUN)</td></tr>
</table>

_Epic subtotal:_ 23 ACs — manual-only 6, real-device-only (NOT RUN) 6, no test (gap) 11

## Epic G — CS & growth ([DEV-62](https://shanivar.atlassian.net/browse/DEV-62))

<table fit-page-width="true" header-row="true">
	<tr><td>Ticket</td><td>Jira status</td><td>AC</td><td>Test (file · class.method / pgTAP / manual)</td><td>Status</td></tr>
	<tr><td>[DEV-49](https://shanivar.atlassian.net/browse/DEV-49) In-app feedback: write-only feedback table + submit\_feedback</td><td>Open</td><td>1 feedback table</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>2 RLS insert/select own; no update/delete; no anon</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>3 submit\_feedback RPC 10/day</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>4 via outbox, idempotent</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>5 no auto diagnostics</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>6 founder SQL query</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>7 pgTAP list</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>″</td><td></td><td>8 Settings entry, no ratings</td><td>Not built</td><td>no test (gap)</td></tr>
	<tr><td>[DEV-62](https://shanivar.atlassian.net/browse/DEV-62) epic container</td><td>Open</td><td>— no ACs (container)</td><td>—</td><td>—</td></tr>
	<tr><td>[DEV-85](https://shanivar.atlassian.net/browse/DEV-85) Activation and retention dashboard with goal segments</td><td>Open</td><td>Baseline numbers visible; limits stated</td><td>Needs analytics (DEV-65/83)</td><td>no test (gap)</td></tr>
	<tr><td>[DEV-86](https://shanivar.atlassian.net/browse/DEV-86) Feedback and churn-reason taxonomy, interview guide and mont</td><td>Open</td><td>First 5 interviews; first monthly report</td><td>CS deliverable review</td><td>manual-only</td></tr>
	<tr><td>[DEV-87](https://shanivar.atlassian.net/browse/DEV-87) Lapsed-user recovery journey offering smaller commitments</td><td>Open</td><td>Spec approved; links to C3</td><td>Doc review</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Later.</td><td></td><td></td></tr>
	<tr><td>[DEV-88](https://shanivar.atlassian.net/browse/DEV-88) Positioning doc and feature-claim verification checklist</td><td>Open</td><td>Checklist applied to site, onboarding and store listing</td><td>Manual claim audit — not run</td><td>manual-only</td></tr>
</table>

_Epic subtotal:_ 12 ACs — manual-only 3, no test (gap) 9

## Epic No epic

<table fit-page-width="true" header-row="true">
	<tr><td>Ticket</td><td>Jira status</td><td>AC</td><td>Test (file · class.method / pgTAP / manual)</td><td>Status</td></tr>
	<tr><td>[DEV-20](https://shanivar.atlassian.net/browse/DEV-20) DECISION: sprints and Fix Version are unavailable — DEV is a</td><td>Open</td><td>(derived) Founder picks option A/B/C</td><td>Decision</td><td>manual-only</td></tr>
	<tr><td>″</td><td></td><td>_Note:_ Process; no epic.</td><td></td><td></td></tr>
</table>

_Epic subtotal:_ 1 ACs — manual-only 1

