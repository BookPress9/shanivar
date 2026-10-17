import sys, re; sys.path.insert(0,'.')
from to_notion import convert
r=lambda f: open(f).read()
mat=convert(r('ac_test_matrix.md'))
content = f'''<callout icon="🧪" color="yellow_bg">
	**Live: the founder confirmed DEV-82 on 17 Oct 2026.** Shanivar Tester, 2026-10-17 (Europe/Berlin). Source files are on the QA box at `/workspace/qa/dev82/` (test_plan.md, ac_test_matrix.md, defect_register.md, release_readiness_template.md, device_case_plan_dev71_dev75.md, alignment_review.md). No source code was changed, no tests were written for unconfirmed tickets, and no Jira status was changed.
</callout>
Part of <mention-page url="https://app.notion.com/p/3fc10ae18c9f81c1b877d676061428dd"/> · Jira [DEV-82](https://shanivar.atlassian.net/browse/DEV-82) · Epic F [DEV-61](https://shanivar.atlassian.net/browse/DEV-61)
# Alignment review (QA)
{convert(r('alignment_review.md').split(chr(10),2)[2])}
# Coverage summary and top gaps
{mat.split('## Epic',1)[0]}
**Top gaps (built or in-flight work with no test, or not run):**
1. **Real-device proof: all 37 real-device rows are NOT RUN (21 DEV-71/75 cases are emulator-pending).** No Pixel or OEM run for any alarm or protection ticket (DEV-1–6, 10, 21, 69–71, 75). DST fall-back on 25 Oct is an imminent case.
2. **Breath-pacing safety (DEV-89):** TechniqueContentSafetyTest scans only techniques.json/.md. The Breathing\\* screens and the block-screen "breathe" path have no test.
3. **No-snooze on the block screen (DEV-10):** nothing asserts it.
4. **Analytics / WIAC:** no events exist (DEV-65/69/83). The intervention choice and the "no package name leaves the device" rule are untested.
5. **Cascade delete (DEV-42 AC7):** only the FK is checked; there is no delete-and-verify test. This blocks the DEV-46 Play requirement.
6. **Cross-user isolation (DEV-79):** covers 5 tables + tombstones. No RPC or Edge Function exists yet; DEV-54 pgTAP 07 hasn't been run by QA.
7. **Copy claims (DEV-6 AC2, DEV-88):** no scan for "unbreakable/continuous" wording in app, site or store.
8. **Stage 1 check-in (DEV-36 AC4):** the shipped card would fail the 2–3 one-tap rule, and no test enforces it.
9. **E2E against staging (DEV-55):** SupabaseSyncTest.e2eGuestPushReadBack is skipped without a stack, and there is no staging yet.
10. **Branch-only fixes:** DEV-38/39/42/45/47/54 are not on main, and there is no CI (DEV-15).
# Defect register
{convert(r('defect_register.md').split(chr(10),2)[2])}
# Test plan
{convert(r('test_plan.md').split(chr(10),2)[2]).replace(chr(10)+'## ', chr(10)+'### ')}
# DEV-71 / DEV-75 device case plan (emulator-pending)
{convert(r('device_case_plan_dev71_dev75.md').split(chr(10),2)[2]).replace(chr(10)+'## ', chr(10)+'### ')}
# Release-readiness report template
Copy this per RC.
{convert(r('release_readiness_template.md')).replace(chr(10)+'## ', chr(10)+'### ')}
# AC-to-test matrix (all 89 DEV issues: 315 ACs + 28 planned device cases)
{('## Epic'+mat.split('## Epic',1)[1])}
'''
content=re.sub(r'`[^`\n]+`', lambda m: m.group(0).replace('\\_','_').replace('\\*','*'), content)
open('notion_content.md','w').write(content); print(len(content))
