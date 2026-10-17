Part of <mention-page url="https://app.notion.com/p/3fc10ae18c9f8158abd7e59528eee0a6">Shanivar Product Thesis — Protection, Awareness & Flow</mention-page>. Requested by: PM. For: Finance. Status: **request**. No spend is authorised by this page. Last updated: <mention-date start="2026-10-17"/>.
**Ask:** build a unit-economics model, a 12-month monthly opex forecast, pricing scenarios and an assumption register, all aligned with activation and retention (Measurement plan). Label every number **\[Fact + source\]** or **\[A\]** (assumption).
# 1. Known costs (verified 17 Oct 2026)
<table header-row="true" fit-page-width="true">
	<tr>
		<td>Item</td>
		<td>Cost</td>
		<td>Notes / source</td>
	</tr>
	<tr>
		<td>Supabase **Free**</td>
		<td>$0/mo</td>
		<td>500 MB DB, 50k MAU, 5 GB egress, 1 GB storage, 500k Edge Function calls. **Pauses after 1 week of inactivity**, at most 2 active projects, **no automatic backups**.[^https://supabase.com/pricing]</td>
	</tr>
	<tr>
		<td>Supabase **Pro**</td>
		<td>$25/mo per org, including $10 compute credit (1 Micro)</td>
		<td>8 GB disk, 100k MAU (then $0.00325/MAU), 250 GB egress (then $0.09/GB), 2M Edge calls, daily backups kept 7 days, never paused. Spend cap on by default.[^https://supabase.com/pricing]</td>
	</tr>
	<tr>
		<td>Supabase staging project (DEV-41)</td>
		<td>+$10/mo Micro on Pro → **$35/mo** for prod + staging</td>
		<td>On Free, staging uses the second free project.</td>
	</tr>
	<tr>
		<td>Optional: PITR</td>
		<td>$100/mo per 7 days of retention</td>
		<td>Only if the backup review (DEV-80) requires it.</td>
	</tr>
	<tr>
		<td>Google Play developer account</td>
		<td>**$25 one-time**</td>
		<td>Unlimited apps. A new personal account needs 12 testers × 14 days before production.[^https://support.google.com/googleplay/android-developer/answer/6112435]</td>
	</tr>
	<tr>
		<td>Gemini API</td>
		<td>**Excluded**</td>
		<td>AI is not in the first release; the Render path is being retired (DEV-40; DEV-18 conflicts). Model it only if the founder keeps an AI feature.</td>
	</tr>
	<tr>
		<td>Frontend hosting</td>
		<td>$0</td>
		<td>Native Android app; no hosting needed.</td>
	</tr>
</table>
# 2. Inputs needed (with starting assumptions)
<table header-row="true" fit-page-width="true">
	<tr>
		<td>Input</td>
		<td>Starting assumption \[A\]</td>
		<td>Who confirms</td>
	</tr>
	<tr>
		<td>Supabase plan at closed test vs production</td>
		<td>Free for the internal test; Pro before external testers (no pausing, backups)</td>
		<td>Founder + Backend (DEV-41)</td>
	</tr>
	<tr>
		<td>Users: closed test → month 3 → month 12</td>
		<td>20 → 300 → 3,000 MAU; all well inside Free/Pro quotas</td>
		<td>CS / Marketing</td>
	</tr>
	<tr>
		<td>Data per user</td>
		<td>Under 1 MB/user/yr of rows and events, so DB size is not a cost driver below about 100k users</td>
		<td>Backend (after DEV-65)</td>
	</tr>
	<tr>
		<td>Transactional email (OTP, DEV-48)</td>
		<td>Supabase's built-in email is for testing only; production needs a custom SMTP provider. Use a free tier until volume grows; price to be sourced</td>
		<td>Backend</td>
	</tr>
	<tr>
		<td>Push / notifications</td>
		<td>Local notifications only, so $0</td>
		<td>Frontend</td>
	</tr>
	<tr>
		<td>Crash reporting (DEV-14)</td>
		<td>Free-tier tool; price to be sourced</td>
		<td>Frontend</td>
	</tr>
	<tr>
		<td>Audio / content licences (DEV-37)</td>
		<td>Unknown; one-time or royalty-free assumed</td>
		<td>Founder</td>
	</tr>
	<tr>
		<td>Device test fleet</td>
		<td>3–4 budget OEM Android phones (Xiaomi/Realme/Vivo/Samsung); price to be sourced</td>
		<td>QA</td>
	</tr>
	<tr>
		<td>Domain, site, legal (privacy notice)</td>
		<td>Existing shanivar.com; legal review cost unknown</td>
		<td>Founder</td>
	</tr>
	<tr>
		<td>Maintenance and support time</td>
		<td>Founder and agent time; record hours per week, not $</td>
		<td>PM</td>
	</tr>
	<tr>
		<td>Monetisation</td>
		<td>None in the first release. Scenarios to model: free; freemium (depth content paid, controls always free); subscription. Play service fee to be verified from Play policy</td>
		<td>Finance + Founder</td>
	</tr>
	<tr>
		<td>CAC / conversion / retention</td>
		<td>Organic only (campaign page). Retention from the closed-test W1/W4</td>
		<td>Marketing + CS</td>
	</tr>
</table>
# 3. Outputs requested
1. Cost per activated user and per retained (W4) user, under each plan.
2. Monthly opex forecast for 12 months with a low, base and high case.
3. Pricing scenarios with a hard rule: essential controls, Stop, accessible alarm dismissal and privacy are **never paywalled**.
4. Cost-risk register: usage growth past quotas, egress, email volume, AI (if kept), licences.
5. Willingness-to-pay test plan with target users. Do not price from competitors alone.
