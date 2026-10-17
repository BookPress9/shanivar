# 0020 — Analytics event contract: closed dictionary, executable metric definitions, no store yet

- **Date:** 2026-10-17
- **Status:** Accepted for build (DEV-65, founder-confirmed). The review and privacy sign-off are pending (docs/analytics/EVENT_DICTIONARY.md §8).

## Context
No analytics exist, so WIAC and the thesis §6 metrics cannot be measured. DEV-65 asks for one schema doc, a dedupe key, a consent rule and metric definitions. Vendors, dashboards and pipelines are out of scope. The Measurement plan proposes a Supabase `events` table but leaves storage open for the founder. SOP-09 forbids any new collection without founder approval.

## Decision
1. **A closed dictionary.** Every property is an enum, a bounded int, a boolean or an app-generated or catalog id. There is no free-text type, and unknown properties are *rejected*, not dropped. This makes "no free text, no package names, no self-report values" a property of the schema rather than a review habit. Protected apps are sent only as an `app_category` enum.
2. **Kotlin as the source of truth.** `AnalyticsDictionary.kt` holds the dictionary; the JSON copy is generated from it and tests keep the doc, the JSON and the code in step. A pure-JVM validator and the reference metric computation (`Metrics.kt`) are the executable definitions. Any later pipeline must reproduce them on `sample-event-log.v1.jsonl`.
3. **Dedupe:** a client `event_id` (unique), then `occasion_id` once across features, then a 30-minute fallback per action key, with retraction by `routine_step_uncompleted`.
4. **Local time:** every event carries `ts_utc`, `local_date` and `tz`; weeks are local ISO weeks. A missing or inconsistent tz is rejected.
5. **Versioning:** a `schema_version` on every event, with upgraders on read. v0 (the Measurement-plan draft names) is upgraded to v1.
6. **No table and no migration in DEV-65.** Storage waits for the founder (open question 1).

## Options rejected
- *Create the `events` table now:* that is a pipeline (out of scope) and a new collection without founder approval (SOP-09).
- *SQL functions as the reference computation:* they would need a store, and they could not validate events on the device before sending.
- *Drop unknown properties silently:* that hides emitter bugs and lets PII through in transit.
- *Score self-reports (for example, average usefulness):* forbidden by 0018 and the thesis. Only a distribution of the user-owned records is allowed.
