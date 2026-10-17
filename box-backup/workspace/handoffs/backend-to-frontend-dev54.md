# DEV-54 backend -> Frontend: payload/field changes (branch dev-54-sadhana-record-schema, 46f47c2 + d1d482c)

Built on dev-45-sync-engine. All builders live in SupabaseSync.kt `SyncPayloads`; a contract test
(Dev54PayloadContractTest) fails if they drift from supabase/migrations.

1. attempt (practice_attempts): payload adds `"anchor":"BREATH"|"ROOM"`, `"anchor_changes":<int >= 0>`,
   `"events":[{"type": "<SadhanaEventType>", "at": "<ISO UTC>", "active_seconds": n, "detail": s|null}, ...]`.
   Server cap 32768 bytes; the builder drops oldest middle events above 32000 bytes (keeps first + latest).
2. self_report (independent record): adds `"private_by_default":true` (from IndependentPracticeRecord; stays true).
   Stage1.payload unchanged (server default true). drift_reasons must be in
   thoughts,sounds,body,phone,sleepy,emotion,other (lowercase) or the row is rejected (400 -> dead letter).
3. orbit (orbit_events): new builder `SyncPayloads.orbit(e)` -> {id,type,local_date,source,entity_id};
   local_date = OrbitEvent.occurrenceDate. Append-only: sent with `Prefer: resolution=ignore-duplicates`,
   replays are acked and ignored, never updated. Orbit can no longer be deleted: "orbit" removed from
   tombstoneKinds (a DELETE item is rejected "delete_unsupported"). type/source must be the enum names.
4. reflection (new kind, table practice_reflections): `SyncPayloads.reflection(r)` -> {attempt_id, usefulness
   ("useful"|"neutral"|"uncomfortable"|null), note (<= 500)|null}; outbox kind "reflection", id = attemptId;
   upsert on_conflict=user_id,attempt_id. Clearing = `SyncPayloads.tombstone(attemptId)` (tombstone kind
   "reflection"). NOT wired: SadhanaController.reflect() does not enqueue yet (queueing is outbox scope).
   createdAtUtc is not sent; server created_at is set on insert.
5. PM caps: practice_id <= 128, content_version <= 64, zone_id <= 64, self_reports attempt_id <= 128,
   orbit entity_id <= 128, orbit type/source <= 32.
6. String escaping fix in SyncPayloads: newlines / control chars in notes and details are now JSON-escaped.
