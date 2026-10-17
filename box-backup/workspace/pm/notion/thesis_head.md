<callout icon="🧭" color="blue_bg">
	**Canonical source of truth** for Shanivar product strategy. Owner: Shanivar PM. Approved by founder Shubham Kumar on 17 Oct 2026 (strategy brief). Last updated: <mention-date start="2026-10-17"/>. Implementation details still need technical validation. Hypotheses are labelled **\[H\]** and are not requirements.
</callout>
<table_of_contents/>
# 1. Tagline and thesis
> **Protect. Become Aware. Enter Flow.**
**Thesis:** Shanivar helps people reduce automatic digital behavior, build consistent routines and develop awareness through small, actionable practices.
Three connected functions:
- **Protection:** interrupt unwanted digital behavior and make space for a deliberate choice.
- **Routine:** turn intentions into small, repeatable physical and behavioral actions.
- **Awareness / Sadhana:** observe thoughts, moods and urges, then apply that in daily life.
**Success test:** users need *less* intervention to act intentionally over time. We do not optimise for screen time blocked, alarms dismissed, meditation minutes or app opens.
**Long-term ambition \[H\]:** greater self-regulation and focused, intentional engagement with life. This is a thesis to validate through behavior and research, not a proven outcome.
Links to existing context: <mention-page url="https://app.notion.com/p/3ef10ae18c9f81029568dc70f636b1f6">Shanivar</mention-page> (project) · <mention-page url="https://app.notion.com/p/3fc10ae18c9f8199a5d5db25e0382907">4. Product truth: feature verification</mention-page> · <mention-page url="https://app.notion.com/p/3fc10ae18c9f81a09007dbbd63dd8c99">Shanivar Launch Campaign: Saturn's Wisdom</mention-page> · <mention-page url="https://app.notion.com/p/3fa10ae18c9f81558d9ee9041fd6cd6e">Mano Vijnana Research</mention-page>. Repo: `BookPress9/shanivar` (main @ 20fb519), `docs/decisions/`, `SHANIVAR_STANCE.md`, `docs/FINALIZATION_CHECKLIST.md`. No separate roadmap page existed in Notion, so §7 below is the current roadmap.
# 2. Competitive landscape and research findings
Each competitor does one function well. None we reviewed joins protection, routine and awareness in one loop. That is our opportunity \[H\]. We do **not** claim that competitors lack features.
<table header-row="true" fit-page-width="true">
	<tr>
		<td>Product</td>
		<td>What it does</td>
		<td>Takeaway for Shanivar</td>
	</tr>
	<tr>
		<td>**Opal**</td>
		<td>Focus sessions, recurring schedules, app limits and open limits, Pomodoro, weekly reports, **streaks**, Hard Mode, an "Always Allowed" essentials list. Uses AccessibilityService on Android 14+.[^https://play.google.com/store/apps/details?id=com.withopal.opal][^https://help.opalapp.com/]</td>
		<td>Essentials stay reachable, as in our DEV-10. We refuse AccessibilityService (0002) and streaks (0009), so our protection is weaker but more honest.</td>
	</tr>
	<tr>
		<td>**one sec**</td>
		<td>Intercepts a chosen app with a short delay plus the option to dismiss. PNAS 2023 (n=280, 6 weeks): 36% of open attempts dismissed, 57% fewer actual opens. In the controlled test, the **option to dismiss** had the strongest effect; the message alone did not work.[^https://www.pnas.org/doi/10.1073/pnas.2213114120]</td>
		<td>Friction plus a clear alternative choice is the evidence-backed core of our intervention. Their pause is breathing-led; **ours never paces the breath**.</td>
	</tr>
	<tr>
		<td>**Freedom**</td>
		<td>Cross-device blocking of apps, websites or the whole internet, with blocklists. Premium adds recurring sessions and Locked Mode.[^https://support.freedom.to/en/articles/13764747-what-s-included-in-free-and-premium-plans]</td>
		<td>Our scope is Android-only and app-only. Copy must say that browsers and other devices are not blocked.</td>
	</tr>
	<tr>
		<td>**Alarmy**</td>
		<td>Mission alarms (math, steps, squats, QR, photo), Wake Up Check, sleep sounds and tracking. Some missions and chaining are Pro.[^https://alar.my/en/blog/how-to-choose-alarmy-mission][^https://play.google.com/store/apps/details?id=droom.sleepIfUCan]</td>
		<td>This was the behavior reference for 0003 and 0007. Our movement dismissal always keeps an accessible path. No sleep tracking.</td>
	</tr>
	<tr>
		<td>**Routinery**</td>
		<td>Step-by-step routine timer with voice prompts, skip, extend and reorder, widgets, mood tracking.[^https://www.routinery.app/features]</td>
		<td>Small steps lower the cost of starting, and skipping is normal. That supports our non-punitive missed-step recovery.</td>
	</tr>
	<tr>
		<td>**Headspace**</td>
		<td>Large guided library, courses, sleep content, focus music, AI companion.[^https://www.headspace.com/app]</td>
		<td>Library breadth is not our game. One small practice tied to an action is.</td>
	</tr>
	<tr>
		<td>**Calm**</td>
		<td>Meditations, Sleep Stories, soundscapes, music, sleep check-in. A free tier plus Premium.[^https://support.calm.com/hc/en-us/articles/360044707294-What-Free-Content-is-Available-on-the-Calm-App]</td>
		<td>Content-heavy and sleep-led. We avoid any calm or sleep outcome claims.</td>
	</tr>
	<tr>
		<td>**Waking Up**</td>
		<td>Practice plus Theory and Life lessons; a 30-day intro course pairs a daily practice with a short lesson.[^https://help.wakingup.com/article/76-how-is-the-app-organized]</td>
		<td>Closest to our Sadhana plus deep study. Our difference is the explicit link from practice to a chosen daily action.</td>
	</tr>
	<tr>
		<td>**Insight Timer**</td>
		<td>Free library, a customisable unguided timer with bells and presets, talks and courses.[^https://help-app.insighttimer.com/support/solutions/articles/67000664719-what-is-insight-timer-]</td>
		<td>Unguided timers are a commodity. Our value has to come from the journey, not the timer.</td>
	</tr>
</table>
**Findings:** (1) Friction works best with a real alternative choice (one sec). (2) Most retention mechanics are streaks or content volume, both of which we rule out (0009, 0013). (3) Strong Android blocking leans on AccessibilityService, which we refuse (0002). So reliability and honesty about limits become product features. Repo research: `research/deep-foundation/COMPETITOR_PRODUCT_RESEARCH.md`, `research/shanivar-v1-alarm-focus-patterns.md`.
# 3. Target users and primary problems
<table header-row="true" fit-page-width="true">
	<tr>
		<td>Segment (primary goal)</td>
		<td>Primary problem</td>
		<td>First meaningful action (activation, proposed)</td>
	</tr>
	<tr>
		<td>**A. Digital distraction**</td>
		<td>Opens apps automatically and loses time without choosing to.</td>
		<td>At a first intervention, chooses an alternative (Not now, pause or practice).</td>
	</tr>
	<tr>
		<td>**B. Routine consistency**</td>
		<td>Intends a routine, breaks it, then quits after a missed day.</td>
		<td>Saves a routine and completes one planned activity or wake alarm, including after a missed day.</td>
	</tr>
	<tr>
		<td>**C. Meditation adherence**</td>
		<td>Starts meditating, then stops returning.</td>
		<td>Completes the 60-second first practice, then returns on a second local day.</td>
	</tr>
	<tr>
		<td>**D. Philosophical self-inquiry**</td>
		<td>Wants depth, but content stays abstract and is never applied.</td>
		<td>Completes a practice and links it to a "Choose" Routine window, or records practice without the app.</td>
	</tr>
</table>
Existing audience decisions still hold: younger self-directed users and family introductions to parents (0006, 0011); all ages intended, with an adult pilot first (0007).
# 4. Core product principles and non-goals
**Principles**
1. Smallest useful intervention first; experience before explanation (ST-002).
2. The user stays in charge: Skip, Change, End and Stop are always available; no irreversible lock (ST-003, ST-006, DEV-10).
3. Status must be truthful: "Active" only when the engine is actually enforcing (DEV-3, DEV-6).
4. Returning without punishment: missed days never erase history or create guilt (0009, Living Orbit).
5. Facts, not scores: attempts, seconds and outcomes. **Self-reports are never scores, capability evidence or health data** (0017, 0018).
6. Less dependence is success (ST-008): guidance reduces when the user chooses.
7. Device truth stays on the device: alarms, enforcement and local stop (0016).
**Non-goals (standing rules)**
- No breath pacing anywhere (DEV-39 removes the timed box-breathing script).
- No medical, therapeutic, energy or guaranteed-performance claims (SOP-05). "Energy flows where attention goes" stays internal (0018).
- No AccessibilityService and no Device Admin enforcement (0002). No "impossible to bypass" claims.
- No streaks, ranks, leaderboards or attainment levels (0009, 0013, ST-005).
- No teacher or lineage named in app or public copy (0015, ST-011). Named influences stay internal. A published translator credit (for example Wallis) is allowed.
- No separate audio product: audio is part of the meditation experience.
- No AI coach, content feed, social comparison or family surveillance in V1 (0009).
- Public copy may say the spine has **"33 sections"**.
