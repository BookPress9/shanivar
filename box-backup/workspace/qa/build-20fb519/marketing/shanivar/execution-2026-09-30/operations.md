# Social publishing recovery — 30 September 2026

Owner: Social Media Manager. Audit scope: repository, local configuration presence, and read-only local n8n database. No authenticated Instagram access, external communications, publication, scheduling or account changes occurred in this audit.

## Why only one post

The team produced plans without completing the publishing operation. The founder reports one published post; the repository contains no evidence of subsequent publication. This is an execution gap, not evidence of poor content performance.

| Evidence checked today | Finding | Operational consequence |
| --- | --- | --- |
| `month-02/asset-manifest.json` | All 16 entries are `Not produced`; filenames and public URLs are absent. | A month of copy was mistaken for a production queue. |
| `next-instagram-2026-09-25/` | Five numbered PNGs, preview, caption, alt text, post record and upload ZIP exist. | One concrete follow-up package was prepared. |
| That package's `post.json` | `Ready for review`; approval and platform ID null; public media URLs empty. Proposed 26 September slot is past. | It was never evidenced as approved, scheduled or published. Reconcile current naming/product scope before reuse. |
| `automation/social/.env` and process environment | Instagram user ID, access token and media base URL all absent/empty. Values were checked for presence only. | The local publisher cannot authenticate or supply downloadable media. |
| Local n8n SQLite, opened read-only | One workflow, inactive; zero credential records; one historical successful execution. | No connected publishing operation exists in this inspected instance. README identifies the historical execution as an approval-guard dry run. A successful execution count is not a published post. |
| Port 5678 listener query | No listener returned. | No running local editor was observed; this is not a claim about other hosts. |
| `workflows/instagram-pinned-carousels.json` | Manual trigger; `active: false`; two hardcoded unapproved eight-slide introductions dated 13/16 September. | It is not a recurring calendar reader and cannot publish the newer five-slide post unchanged. |

Role documents are instructions invoked during sessions. They are not continuously running marketing workers. No independently verified recurring trigger, production host, monitoring or failure alerts were found in the audited workflow.

## Publication ledger truth

| Record | Actual status/evidence | Missing fields |
| --- | --- | --- |
| BASE-01 | Published, founder-reported only: https://www.instagram.com/p/DdMA9N7lAT3/ | Actual timestamp, exact creative/caption match, API media ID and Insights remain unknown. The permalink shortcode is not a verified API media ID. |
| PINNED-INTRO-EN-01 / HI-01 | Local unapproved legacy proposals | Approval, actual timestamp, platform ID and permalink. Do not release automatically or duplicate BASE-01. |
| Month-02, 16 proposed editions | Draft copy/calendar; assets not produced in manifest | Final media, approval, account schedule and publication evidence. |
| SHN-20260925-BREATHE-EN | Ready for review in the existing local record; upload files exist | Final current-scope review, approval, revised timing, platform ID and actual timestamp. |
| New execution batch | Three English carousels rendered, four slides each; package README marks Ready for review | Approval, platform ID, permalink and actual timestamp remain absent. See `posts.json` and `asset-manifest.json` for exact copy and assets. |

Known publications: **one founder-reported**. Independently verified new publications from this audit: **none**. Instagram performance, account-wide post count and delivery outside this repository have not been checked. Unknown metrics stay blank, not zero.

## Fastest route to the next actual post

1. Social Media Manager selects one exact reviewed package from the recovery batch, matching caption, numbered media and alt text. Reconcile the existing post before selecting a repeated introduction or morning hook. Product statements remain development intentions unless dated public release evidence exists; use current Steady naming where applicable.
2. Account owner or connected authorized publisher uploads those exact files to `@app.shanivar` through the existing account session. This manual path does not require n8n setup or public media hosting. This audit did not find an authenticated account surface and did not publish.
3. Record actual permalink, platform ID when available, timestamp with offset, content ID, asset revision and applicable publication authorization immediately. Retain a blank API media ID if only a manual permalink is available.
4. Capture available 24-hour, 7-day and 28-day Insights at their actual observation times. Never reconstruct missed windows from later totals.

The immediate account dependency is an authorized publishing session for `@app.shanivar`. Do not ask for passwords or tokens in chat. For automation, the account owner must connect the intended Professional account and Meta app through the official OAuth/credential flow. Current required permissions and API version must be revalidated against official Meta documentation at connection time; this local audit does not certify September 11 integration guidance as current.

## Proposed seven-day execution cadence

Window: 30 September–6 October 2026. These are proposed production/publication slots, **not live schedules**. Feed time assumption: 19:30 Asia/Kolkata / 16:00 Europe/Berlin. If today's time has elapsed or account access is absent, shift the first actual publication and subsequent observation windows; do not backdate or release a backlog at once.

| Date | Concrete deliverable/action | Owner and gate |
| --- | --- | --- |
| 30 Sep | First carousel: `SHN-20260930-STEADY-EN`, Steady product update. Final review, then publish once access and applicable authorization cover the exact package. | Social Media Manager + visual production; account publisher records evidence. |
| 1 Oct | Capture first post's 24h observation when mature; prepare one optional Story from its actual final copy. | Social Media Manager; no invented Insights or automatic replies. |
| 2 Oct | Second carousel: `SHN-20261002-LAMP-EN`, a chosen stopping point after work. | Creative/visual team finalizes media; publisher records actual post. |
| 3 Oct | Complete next asset and inspect any mature observations; no forced feed post to fill a calendar cell. | Social Media Manager; leave unavailable metrics blank. |
| 4 Oct | Review mature observations and final-check the third rendered carousel. No additional language edition is assumed ready. | Social Media Manager. |
| 5 Oct | Third carousel: `SHN-20261005-CUPS-EN`, the family conversation with room for both people. | Creative/visual team + account publisher record exact content ID and actual timestamp. |
| 6 Oct | Review actual published/planned count, missing assets/access, and mature observations; prepare the next weekly batch. | Social Media Manager records one hold/accept/narrow/reject decision and writer handoff. |

Three original feed posts is a capacity target, not a platform rule or guaranteed reach strategy. Existing month-02 paired-language comparisons remain separate; this recovery batch must not be silently counted as those planned editions. If the recovery batch changes format or language, record that explicitly. No fabricated causal experiment or platform performance explanation follows from having only one reported post.

## Automation work required before activating it

The repository README describes reliability requirements that the current exported workflow does not implement fully. It has a fixed 30-second processing wait, no explicit container-status polling, no public-media preflight stage, no persistent deduplication ledger, and no dedicated durable publication-record writer or failure-alert path. Returning a result inside an execution is not a maintained campaign ledger.

Delivery/Operations must replace the hardcoded eight-slide legacy records with a selected final content record, reject stale slots and unapproved records, verify exact media URLs/hashes, persist content-ID deduplication and returned publication evidence, poll container readiness, and reconcile uncertain publish responses before any retry. Use an always-on execution host for durable scheduling with observable failures. Do not simply flip `active` or `approved` on the old introduction workflow.

Automation blockers: missing local account credentials, unverified account/app permissions, absent public media hosting, outdated fixed content records and incomplete operational controls. Manual publishing avoids the hosting/scheduler dependency while automation is repaired. The new work is successful only when actual account publication evidence is logged; a folder, CSV or rendered preview alone is not execution on Instagram.

## Audit sources

- [Social role](../../../ai_org/agents/social_media_manager.md)
- [Month operations](../month-02/operations.md), [asset manifest](../month-02/asset-manifest.json), [first-post intake](../month-02/first-post-analysis.md)
- [Existing follow-up package](../next-instagram-2026-09-25/README.md), [post record](../next-instagram-2026-09-25/post.json)
- [Automation README](../../../automation/social/README.md), [workflow export](../../../automation/social/workflows/instagram-pinned-carousels.json)
- Local configuration presence checks and read-only SQLite aggregate queries performed 30 September 2026; no credential values or private execution payloads copied into this report.
