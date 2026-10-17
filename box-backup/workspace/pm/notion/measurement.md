Part of <mention-page url="https://app.notion.com/p/3fc10ae18c9f8158abd7e59528eee0a6">Shanivar Product Thesis — Protection, Awareness & Flow</mention-page>. Shared with CS, Product and Marketing. Owner: PM (definition); Backend (DEV-65); CS (DEV-66, DEV-85). Status: **draft for founder approval**. Last updated: <mention-date start="2026-10-17"/>.
<callout icon="⚠️" color="yellow_bg">
	No analytics exist today (`SessionReview.kt` is local-only). Nothing on this page is measurable until [DEV-65](https://shanivar.atlassian.net/browse/DEV-65) and [DEV-69](https://shanivar.atlassian.net/browse/DEV-69) ship and [DEV-83](https://shanivar.atlassian.net/browse/DEV-83) verifies them.
</callout>
# 1. WIAC: Weekly Intentional Actions Completed
**Definition:** the number of distinct *occasions* in a user's local ISO week (Monday–Sunday, device time zone) on which the user completed an action they had deliberately selected through Shanivar.
**Qualifying actions (first release):**
- **Routine:** a planned activity the user marks done (`routine_activity_completed`).
- **Practice:** a practice the user chose, ending with outcome `completed`, or `ended` by the user after ≥ 10 s active time \[H: threshold to validate\]. `interrupted` does not count.
- **Intervention:** at the block overlay, the user picks an alternative (Not now/Return, one-minute pause, short practice) instead of using the protected app (`intervention_choice`, choice ≠ `stop_protection`).
- **Excluded:** app opens, alarm dismissal alone, minutes meditated, reading deep study, stopping protection. Alarm dismissal counts only when it is followed by the planned activity.
- **Reported separately, not merged:** "practised without the app" self-reports (WIAC-SR). They are facts the user tells us, never scores (founder decision 0018, 17 Oct).
**Dedupe rule**
1. Every event carries a client-generated `event_id` (UUID). The server stores it under a unique constraint, so retries and rerenders cannot double-count.
2. A journey shares one `occasion_id`, minted at the trigger: an intervention, a routine slot or a practice launch. All qualifying events with the same `occasion_id` count **once**, across features. Example: intervention → practice → routine activity marked done = 1.
3. Fallback when there is no `occasion_id`: same user + same `action_key` within 30 minutes = 1 \[H\].
4. Edits and deletes of a completion retract it (the sync outbox carries the delete; DEV-38).
# 2. Event taxonomy (draft for DEV-65)
Common properties: `event_id`, `occasion_id`, `user_id` (Supabase auth uid, pseudonymous), `ts_utc`, `local_date`, `tz`, `app_version`, `content_version`, `source` (routine / protection / sadhana / initiation). **Never sent:** free-text notes, self-check answers, commitment text, contact data, or exact package names. Use an app category instead \[open: confirm category vs package\].
<table header-row="true" fit-page-width="true">
	<tr>
		<td>Event</td>
		<td>Key properties</td>
		<td>Feeds</td>
	</tr>
	<tr>
		<td>`onboarding_step_completed`, `goal_selected`</td>
		<td>step, segment (distraction / routine / meditation / inquiry)</td>
		<td>Activation, time to first value, segments</td>
	</tr>
	<tr>
		<td>`permission_state_changed`</td>
		<td>permission (usage, overlay, exact_alarm, notifications, activity), granted</td>
		<td>Reliability, funnel drop-off</td>
	</tr>
	<tr>
		<td>`routine_saved`, `protection_window_saved`</td>
		<td>window_count, overnight (bool)</td>
		<td>Activation (B)</td>
	</tr>
	<tr>
		<td>`protection_window_started` / `_ended`</td>
		<td>window_id, armed_by_engine, end_reason (scheduled, user_stop, failure)</td>
		<td>Reliability, interventions per protected hour</td>
	</tr>
	<tr>
		<td>`protection_failure_detected`</td>
		<td>cause (service_killed, permission_revoked, doze)</td>
		<td>Reliability (DEV-70)</td>
	</tr>
	<tr>
		<td>`intervention_shown`, `intervention_choice`</td>
		<td>window_id, app_category, choice (return, pause, practice, stop_protection, essential)</td>
		<td>WIAC, protection-window effectiveness (DEV-69)</td>
	</tr>
	<tr>
		<td>`practice_started`, `practice_ended`</td>
		<td>practice_id, content_version, outcome, active_seconds</td>
		<td>WIAC, meditation return</td>
	</tr>
	<tr>
		<td>`self_check_submitted`</td>
		<td>kind only (no answers)</td>
		<td>Reflection rate</td>
	</tr>
	<tr>
		<td>`routine_activity_completed` / `_uncompleted`</td>
		<td>activity_id, scheduled_local_time</td>
		<td>WIAC, routine completion</td>
	</tr>
	<tr>
		<td>`alarm_fired`, `alarm_dismissed`</td>
		<td>scheduled_vs_actual_s, method (movement, qr, photo, accessible)</td>
		<td>Reliability only</td>
	</tr>
	<tr>
		<td>`app_returned_after_gap`</td>
		<td>gap_days</td>
		<td>Recovery after missed days</td>
	</tr>
	<tr>
		<td>`guidance_level_changed`</td>
		<td>from, to</td>
		<td>Less-dependence signal</td>
	</tr>
	<tr>
		<td>`sync_failed`, `sync_dead_lettered`</td>
		<td>table, error_class</td>
		<td>Reliability</td>
	</tr>
	<tr>
		<td>`feedback_submitted`</td>
		<td>category (taxonomy, DEV-86)</td>
		<td>CS insights</td>
	</tr>
</table>
Storage proposal: a Supabase `events` table, insert-only for own rows under RLS, with no third-party analytics SDK in the first release \[open: founder\].
# 3. Metrics
<table header-row="true" fit-page-width="true">
	<tr>
		<td>Metric</td>
		<td>Definition (draft)</td>
	</tr>
	<tr>
		<td>**WIAC** (North Star)</td>
		<td>Per active user per week; also the median per user</td>
	</tr>
	<tr>
		<td>Activation rate</td>
		<td>Share of new users who hit their segment's first meaningful action within 7 days (see the thesis §3)</td>
	</tr>
	<tr>
		<td>Time to first value</td>
		<td>Install → first qualifying WIAC action</td>
	</tr>
	<tr>
		<td>Routine completion rate</td>
		<td>Completed ÷ scheduled planned activities</td>
	</tr>
	<tr>
		<td>Protection-window effectiveness</td>
		<td>Interventions resolved with an alternative ÷ interventions shown; plus interventions per protected hour over time</td>
	</tr>
	<tr>
		<td>Meditation return rate</td>
		<td>Share of practice starters who practise again on a different local day within 7 days</td>
	</tr>
	<tr>
		<td>W1 / W4 retention</td>
		<td>≥ 1 qualifying action in week 1 / week 4 after install (not merely an app open)</td>
	</tr>
	<tr>
		<td>Recovery after missed days</td>
		<td>Share of users with a gap of 2+ days who complete a qualifying action within 7 days of the gap</td>
	</tr>
	<tr>
		<td>Self-reported intentionality / usefulness</td>
		<td>Optional in-app question, monthly. Reported as a distribution, never a score</td>
	</tr>
	<tr>
		<td>Technical reliability</td>
		<td>Alarm on-time rate; protection active ÷ scheduled minutes; crash-free sessions (DEV-14); sync success</td>
	</tr>
</table>
# 4. Baseline approach
- No pre-launch baseline exists. Weeks 1–2 of the Play closed test (12+ testers × 14 days) are the descriptive baseline.
- Compare within users across weeks, not against competitors.
- Experiments (DEV-66) start only after events are verified. One variable at a time; preregister the hypothesis in the assumption register.
- Qualitative pairing: 5–8 interviews per cohort (active, inactive, returning).
# 5. Limitations
- Pilot n is tiny, so results are directional only.
- Novelty effects dominate the early weeks.
- Polling misses some opens and protection can be killed by OEM savers, so intervention counts undercount.
- Self-report is subjective and optional, with selection bias.
- WIAC cannot see off-app actions except through self-report.
- Fewer interventions can mean less distraction or an uninstall-like disengagement; read alongside retention and interviews.
- Behavior change is not evidence of awareness or wellbeing; no health inference is ever drawn.
