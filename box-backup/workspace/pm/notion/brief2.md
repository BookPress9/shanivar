Part of <mention-page url="https://app.notion.com/p/3fc10ae18c9f8158abd7e59528eee0a6">Shanivar Product Thesis — Protection, Awareness & Flow</mention-page>. Scope: **first release only**. Owner: QA lead. Last updated: <mention-date start="2026-10-17"/>.
## Objective
Verify Shanivar independently across normal, failure and recovery scenarios. Never rely on developer confirmation alone.
## Top work items (first release)
1. Traceable test plan, AC-to-test matrix and defect register ([DEV-82](https://shanivar.atlassian.net/browse/DEV-82))
2. Physical-device protocol for protection and alarms, including OEM battery savers, reboot, Doze, DST and process death ([DEV-7](https://shanivar.atlassian.net/browse/DEV-7), [DEV-71](https://shanivar.atlassian.net/browse/DEV-71), [DEV-75](https://shanivar.atlassian.net/browse/DEV-75))
3. End-to-end protect → act → reflect journey and regression suite ([DEV-84](https://shanivar.atlassian.net/browse/DEV-84), [DEV-55](https://shanivar.atlassian.net/browse/DEV-55))
4. Analytics-versus-actual-actions verification ([DEV-83](https://shanivar.atlassian.net/browse/DEV-83))
5. Authz boundaries, duplicate requests and input validation, with Backend ([DEV-79](https://shanivar.atlassian.net/browse/DEV-79)); the no-breath-pacing audit ([DEV-89](https://shanivar.atlassian.net/browse/DEV-89))
6. CI gate ([DEV-15](https://shanivar.atlassian.net/browse/DEV-15)); a release recommendation for each RC
## Acceptance criteria and definition of done
- Every acceptance criterion maps to a reproducible test case with evidence.
- Alarm and protection features are verified on real devices (at least one Xiaomi/Realme/Vivo/Samsung class device plus a Pixel), not only emulators.
- Regression passes before every RC; analytics are verified.
- No unresolved critical or high defect remains without documented founder approval.
- **DoD:** a feature is done when its ACs, integration and regression pass, not when it appears on screen.
## Alignment review: answer by the next weekly status
1. What must change?
2. What already exists?
3. What is missing?
4. What are the dependencies and risks?
5. What can ship in the first release?
6. How will the result be verified?
Record any major interpretation of the thesis in the thesis Decision log before building.