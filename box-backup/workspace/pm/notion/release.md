Part of <mention-page url="https://app.notion.com/p/3fc10ae18c9f8158abd7e59528eee0a6">Shanivar Product Thesis — Protection, Awareness & Flow</mention-page>. Owner: PM (checklist); the release owner (founder) approves G6. Applies to every feature and to the first release (Play closed test → production). Last updated: <mention-date start="2026-10-17"/>.
<callout icon="⛔" color="red_bg">
	Any unresolved **critical security, data-isolation or critical reliability defect blocks release**. No exceptions without written founder approval recorded in the Decision log.
</callout>
# Gate 1: Product alignment
- [ ] The user problem and segment are documented, with the hypothesis \[H\] labelled
- [ ] It supports the thesis (protect → act → reflect → recover), and the ticket links to an epic DEV-56–62
- [ ] Standing rules checked: no breath pacing; no medical, therapeutic, energy or performance claims; no teacher named; self-reports not scored
# Gate 2: Design and technical review
- [ ] Flow, states (loading, empty, error, permission-denied, offline) and copy specified
- [ ] Architecture, data flow, RLS and dependencies documented
- [ ] Android limits documented: usage access, overlay, exact alarm, full-screen intent, FGS specialUse, Doze, OEM savers; no AccessibilityService (0002)
# Gate 3: Implementation
- [ ] Code review; error handling; failures never show success
- [ ] Analytics events per DEV-65, with no sensitive properties
- [ ] Migrations through CI (DEV-43); no secrets in git (DEV-44); docs updated
# Gate 4: Independent testing
- [ ] All ACs pass with evidence (DEV-82 matrix); regression passes (DEV-84)
- [ ] Security: cross-user access suite (DEV-79), duplicate requests, input validation
- [ ] Real devices for alarms and protection (DEV-7, DEV-71, DEV-75), including at least one OEM with aggressive battery saving
- [ ] Analytics verified against real actions (DEV-83); breathing audit done (DEV-89)
# Gate 5: CS readiness
- [ ] The outcome is measurable (WIAC + companion metrics live)
- [ ] Failure patterns identifiable (DEV-70 events, crash reporting DEV-14, feedback DEV-49)
- [ ] CS can explain the feature and its limits; FAQ and claim checklist (DEV-88) done
# Gate 6: Release approval
- [ ] Risks and open defects documented; rollback understood (DEV-81 runbook)
- [ ] First-release blockers closed: DEV-8 credentials, DEV-17 signing, DEV-34 content manifests (release launches no practice without them), DEV-46 delete-account, DEV-80 logging/retention, privacy notice updated (0016)
- [ ] Play: data-safety form; exact-alarm, full-screen-intent, FGS and usage-access declarations; 12 testers × 14 days closed test
- [ ] Release owner (founder) approves in writing
# Reporting cadence (proposed)
<table header-row="true" fit-page-width="true">
	<tr>
		<td>Rhythm</td>
		<td>What</td>
		<td>Who / where</td>
	</tr>
	<tr>
		<td>**Daily PM board sweep**</td>
		<td>Read every DEV ticket changed in 24 h. Flag blockers, stale "Work in progress" items (more than 3 days without change), Jira statuses that lag reality (for example DEV-38/39/42/45/47 today), and any ticket missing §10 fields. Internal note only; no ticket edits without the owner.</td>
		<td>PM; output appended to the weekly status</td>
	</tr>
	<tr>
		<td>**Weekly founder status**</td>
		<td>One page: gate status per epic, top 5 risks, decisions needed (with options), WIAC/reliability once live, and what changed in the Decision log.</td>
		<td>PM → founder, proposed every Monday (Europe/Berlin morning)</td>
	</tr>
	<tr>
		<td>Per RC</td>
		<td>QA release recommendation and outstanding-risk summary</td>
		<td>QA → founder</td>
	</tr>
	<tr>
		<td>Monthly</td>
		<td>Product-insights report (CS); finance/opex check</td>
		<td>CS, Finance → founder, PM</td>
	</tr>
</table>
