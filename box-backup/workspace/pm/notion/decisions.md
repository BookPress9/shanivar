# 11. Decision log
Seeded from `docs/decisions/0001–0018` and founder decisions of 17 Oct 2026. Repo files are **not** edited; changes are recorded here with the reason. Never overwrite a row: add a new one, or append to "What changed".
<table header-row="true" fit-page-width="true">
	<tr>
		<td>ID</td>
		<td>Decision</td>
		<td>Owner</td>
		<td>Status</td>
		<td>What changed / note</td>
		<td>Last updated</td>
	</tr>
	<tr>
		<td>[0001](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>Schema changes run inline at boot (Render backend)</td>
		<td>Engineering</td>
		<td>Accepted 2026-09-12, with expiry</td>
		<td>**Change recorded 17 Oct:** the phone path now uses Supabase migrations (`supabase/migrations/`, DEV-43). 0001 still governs only the legacy Render API, which DEV-40 retires. DEV-9 needs re-scoping. File unchanged.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0002](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>No Device Admin or AccessibilityService enforcement</td>
		<td>Founder / Engineering</td>
		<td>Accepted</td>
		<td>Reaffirmed. Polling + overlay; the brief flash is disclosed.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0003](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>Movement-first alarm dismissal with a mandatory accessible path</td>
		<td>Founder</td>
		<td>Accepted</td>
		<td>Unchanged.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0004](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>All DB connections through the Supavisor pooler</td>
		<td>Backend</td>
		<td>Accepted</td>
		<td>Unchanged. **Verify** that the pooler region matches ap-south-1 (0004 cites ap-northeast-2).</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0005](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>BookPress9/shanivar is the only repo</td>
		<td>Founder</td>
		<td>Accepted; steps 1 and 4 open</td>
		<td>Credential rotation still open (DEV-8).</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0006](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>One simple MVP: family introductions + younger self-use</td>
		<td>Founder</td>
		<td>Partially superseded by 0007</td>
		<td>Audiences retained in §3.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0007](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>V1 includes alarms and focus modes</td>
		<td>Founder</td>
		<td>Accepted</td>
		<td>Unchanged.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0008](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>Alarm recurrence recomputed after every fire</td>
		<td>Engineering</td>
		<td>Accepted</td>
		<td>Unchanged.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0009](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>Return for the next conscious choice, not engagement</td>
		<td>Founder</td>
		<td>Accepted 2026-09-19</td>
		<td>Basis of §5 and WIAC. Non-goals unchanged.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0010](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>Recurring focus schedules + when-then commitment</td>
		<td>Founder</td>
		<td>Accepted</td>
		<td>Unchanged.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0011](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>VBT breathing as a scoped third pillar</td>
		<td>Founder</td>
		<td>Accepted scope direction</td>
		<td>**Change recorded 17 Oct:** the standing rule *no breath pacing* applies to every Breathing\* surface. Audit in DEV-89 / DEV-39. The tagline frames Awareness/Sadhana as the third function; whether Breathe & reset stays a separate Home entry is open (Q2).</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0012](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>Breathing session owner is process-scoped</td>
		<td>Engineering</td>
		<td>Accepted, implemented</td>
		<td>Unchanged.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0013](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>Sadhana progression and gamification</td>
		<td>Founder</td>
		<td>PROPOSED</td>
		<td>Still open (DEV-16). Out of first release.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0014](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>Routine + Sadhana shell; Routine V3</td>
		<td>Founder</td>
		<td>Accepted 2026-09-30</td>
		<td>Unchanged. Routine is the launch destination.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0015](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>Sadhak and Hybrid Monk core ideology; naming rule</td>
		<td>Founder</td>
		<td>Accepted 2026-10-12</td>
		<td>**Change recorded 17 Oct:** teachers stay internal and are never named publicly; a published translator credit (for example Wallis) is allowed. **Conflict:** the launch-campaign page allows Shani and classical texts with attribution. Needs a founder ruling (Q3).</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0016](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>Initiation V1 wireframe: accounts + cloud sync at launch</td>
		<td>Founder</td>
		<td>Accepted 2026-10-12</td>
		<td>**Change recorded 17 Oct:** the backend is Supabase `shanivar-prototype` (ap-south-1). The native app needs no frontend hosting, which narrows DEV-25. Which sign-in methods ship in the first release is open (Q5). Privacy notice must be updated before release.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0017](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>Sadhana V1 wireframe adopted</td>
		<td>Founder</td>
		<td>Proposed / adopted as design source</td>
		<td>**Change recorded 17 Oct:** "33 sections" of the spine may be said publicly.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>[0018](https://github.com/BookPress9/shanivar/tree/main/docs/decisions)</td>
		<td>Sadhana Stage 1: base of the spine</td>
		<td>Founder</td>
		<td>Adopted; 2 flags open</td>
		<td>**Change recorded 17 Oct:** self-reports are never scores or health data (reaffirmed). Audio is part of the meditation experience (DEV-36/37: no separate audio feature, no frequency claims). The "energy" line stays internal; public wording is still open.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>F-01</td>
		<td>Tagline "Protect. Become Aware. Enter Flow." and the product thesis</td>
		<td>Founder</td>
		<td>Accepted 2026-10-17</td>
		<td>New. Source: strategy brief.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>F-02</td>
		<td>Standing claims rules: no breath pacing; no medical, therapeutic, energy or guaranteed-performance claims</td>
		<td>Founder</td>
		<td>Accepted 2026-10-17</td>
		<td>New. Applies to app, site, store and social.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>F-03</td>
		<td>Supabase is the only backend for the phone; retire Render from the app path</td>
		<td>Founder</td>
		<td>Direction given 2026-10-17; DEV-40 still Open in Jira</td>
		<td>Close or re-scope DEV-18 and DEV-9 once DEV-40 is accepted.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>F-04</td>
		<td>North Star = WIAC, paired with self-report; no engagement-only metrics</td>
		<td>Founder / CS</td>
		<td>Accepted 2026-10-17</td>
		<td>Measurement plan child page.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>F-05</td>
		<td>Prioritise foundation, reliability, security and the first E2E journey before feature expansion</td>
		<td>Founder</td>
		<td>Accepted 2026-10-17</td>
		<td>Roadmap §7.</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>F-06</td>
		<td>No team implements a major interpretation of the thesis without recording it here</td>
		<td>Founder / PM</td>
		<td>Accepted 2026-10-17</td>
		<td>Process rule.</td>
		<td>2026-10-17</td>
	</tr>
</table>
**Open questions (founder)**
<table header-row="true" fit-page-width="true">
	<tr>
		<td>#</td>
		<td>Question</td>
		<td>Owner</td>
		<td>Status</td>
		<td>Last updated</td>
	</tr>
	<tr>
		<td>Q1</td>
		<td>Approve the first-release scope (§7 Phases 0–3) and confirm that DEV-40 is accepted, so DEV-18 and DEV-9 can be closed or re-scoped.</td>
		<td>Founder</td>
		<td>Open</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>Q2</td>
		<td>Does Breathe & reset (0011) stay a separate entry, or fold into Sadhana under "audio merged into meditation"? DEV-89 outcome feeds this.</td>
		<td>Founder</td>
		<td>Open</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>Q3</td>
		<td>Naming conflict: may public copy reference Shani and classical texts with attribution (campaign page) under 0015's rule?</td>
		<td>Founder</td>
		<td>Open</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>Q4</td>
		<td>Where do Finance and wider Marketing live: new epics or Epic G? Who owns them?</td>
		<td>Founder</td>
		<td>Open</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>Q5</td>
		<td>First-release sign-in: guest + invite-gated email OTP only, or Google too (0016)? And Supabase Free vs Pro before testers (DEV-41)?</td>
		<td>Founder</td>
		<td>Open</td>
		<td>2026-10-17</td>
	</tr>
	<tr>
		<td>Q6</td>
		<td>Carried over: spine counting direction; progression sign-off; content reviews / source verification (DEV-34); NASA image comfort; public wording for the Stage 1 "energy" line; the safety reading of ST-010.</td>
		<td>Founder</td>
		<td>Open</td>
		<td>2026-10-17</td>
	</tr>
</table>