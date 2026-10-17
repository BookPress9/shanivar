# Backend → Frontend: DEV-45 / DEV-47 sync contract (AGREED, implemented)
Shanivar Backend, 17 Oct 2026 · agreed 17 Oct with Frontend (`frontend-answers-dev45-contract.md`) · implemented 18 Oct on branch `dev-45-sync-engine` (from `dev-38-outbox-gaps` 20fb519, DEV-42 commits cherry-picked).
Sources: Jira DEV-45 and DEV-47 (both Open, "founder-confirmed Oct 17"; neither ticket had any comments when read), branch `dev-38-outbox-gaps` (SyncOutbox.kt, SupabaseSync.kt, RoutineSync.kt, SadhanaStage1.kt, SadhanaController.kt, RoutinePlanRepository.kt), supabase/migrations 20261013090000 and 20261014090000, handoffs/frontend-to-backend-2026-10-17.md.

Tags: **[ticket]** = DEV-45/47 requires it · **[code]** = already true on the branch · **[proposed]** = Backend proposal, open for discussion · **[agreed]** = agreed with Frontend and built · **[deviation]** = built differently, see the Deviations section at the end.

## Status (18 Oct 2026)
| Point | State |
|---|---|
| `SyncSession.ensureGuest()` with `Ok/Offline/Failed/Disabled` | **agreed**, built (`SupabaseSession`) |
| Enqueue hook that triggers `requestFlush` | **agreed**, built (`SyncOutbox(onEnqueue)` + engine `listener`) |
| Routine through the outbox, engine calls `put_routine`, stores server revision | **agreed**, built (`SupabaseOutboxSender`, `RoutineSyncStore` = `sync/routine.properties`) |
| Routine parked (stays queued, never dead-lettered) until `put_routine` exists; 409 parks too | **agreed**, built |
| Deletes = tombstone table upserted with `client_updated_at`, server applies the delete | **agreed**, built (`public.tombstones`, DEV-45 AC5/AC6 cover it) |
| `client_updated_at` (UTC) stamped on the outbox item at enqueue | **agreed**, built (`OutboxItem.clientUpdatedAtMs`) |
| Routine id `current`; `protectedPackages` stripped from the routine payload until DEV-52 | **agreed**, built (`SyncPayloads.routine`) |
| 409 body `{code:"revision_conflict", current_revision:int, current_plan:jsonb}` | **agreed**, parser built; DEV-52 must return exactly this |
| Backoff 30 s → 1 h with jitter; reconnect and foreground reset it and flush immediately | **agreed**, built (`SyncBackoff`, `WorkSyncScheduler.flushNow`) — see deviation D2 |
| G1 lock fix: snapshot under the lock, send outside it; test | **agreed**, built (`SyncOutboxEngineTest.enqueueNeverWaitsForAnInFlightSend`) |
| Error classes (IO/timeout/5xx/429 retry + Retry-After; 401 refresh once; 400/403/409/422 dead-letter except routine park; one bad item never blocks; unknown kind dead-lettered) | **agreed**, built (`SyncEngineErrorClassTest`) |
| Never a new anonymous user while a refresh token exists; tokens encrypted | **agreed**, built (Keystore AES-GCM, `SyncSessionTest`) |
| Q3 lost refresh token → "Saved on this device" + `session_lost` in diagnostics | **agreed** (`SyncSession.sessionLost`, D11) |
| Q4 stamp unowned items to the first guest, never re-stamp | **agreed**, built (`SyncOutbox.stampOwner`) |
| Q5 reset drops queue/dead-letters/lastSyncedAt; sign-out parks items of the old user | **agreed**, built (`SyncEngine.reset()` (suspend, D11), `signOut()`) |
| Q9 "Synced" = end of a flush leaving nothing sendable (dead-letters excluded) | **agreed**, built; revised: never Synced while anything is parked (D6) |
| Soft delete `deleted_at` (§6 proposal) | **dropped** in favour of the tombstone table |
| Tombstone sent as `DELETE` (§3) | **dropped**: tombstones-table upsert |


---
## 0. Interfaces between the halves (what DEV-47 builds against)
- [ticket] `SyncSession.ensureGuest()`. DEV-47 calls it once, *after* onboarding consent, and it never blocks Initiation.
- [ticket] `SyncScheduler.requestFlush()` / `schedulePeriodic()`. Frontend calls it after an enqueue (debounced), on foreground and periodically. Network regain is handled by the WorkManager constraint.
- [ticket] One process-wide `AppSync` built in `Application.onCreate` from `BuildConfig.SUPABASE_URL` / `SUPABASE_ANON_KEY`. An empty URL means sync is off and the UI is identical to today.
- [proposed] Kotlin signatures:
  ```kotlin
  interface SyncSession {
      suspend fun ensureGuest(): GuestResult      // Ok(userId) | Offline | Failed(code); never throws
      val userId: String?                          // null = no session
      suspend fun signOut()                        // clears tokens; see §7 for queued items
  }
  interface SyncScheduler {
      fun requestFlush()                           // safe from main thread, debounced internally (proposed 2 s)
      fun schedulePeriodic()                       // proposed 6 h, CONNECTED constraint
  }
  interface SyncStatusSource {                     // for the quiet status line
      val state: StateFlow<SyncUiState>            // LocalOnly | Synced(at: Instant) | WaitingForNetwork
      val deadLetterCount: StateFlow<Int>          // diagnostics view only (DEV-47 AC5)
  }
  ```
- [code] `AppSyncOutbox.get(context)` is the single outbox. `enqueue(kind, id, payload)` replaces any pending item with the same `kind:id`.

## 1. Auth
- [code] Guest is a Supabase anonymous sign-in: `POST {SUPABASE_URL}/auth/v1/signup` with body `{}`, headers `apikey: <anon>` and `Content-Type: application/json`. Today it parses `access_token` and `user.id`, keeps the token in memory only and does no refresh.
- [ticket] Anonymous sign-in happens **once per install**. Access and refresh tokens are stored encrypted (Keystore-backed) and survive restarts. No new anonymous user is ever created while a refresh token exists.
- [ticket] Refresh happens before expiry, and once on a 401 followed by a single retry.
- [proposed] Refresh call: `POST /auth/v1/token?grant_type=refresh_token` with body `{"refresh_token":"…"}`. Store `access_token`, `refresh_token`, `expires_at` and `user.id`. Refresh when fewer than 60 s remain. If the refresh fails with 400/401 (refresh token revoked), the session is gone: stop sending, keep items queued and report `NeedsSession`. Never sign in a new anonymous user automatically, because the founder or user has to decide that (open question Q3).
- [ticket] The app holds only the anon key plus the user JWT. There is no service role key, no secrets in git or logs, and no payloads in logs.
- [code] Every request carries `apikey: <anon>` and `Authorization: Bearer <access_token>`.

## 2. Ownership
- [code] Every table is keyed by `(user_id, id)`. `user_id` defaults to `auth.uid()`, and RLS policies `own_select/insert/update/delete` restrict access to own rows. The client does **not** send `user_id`.
- [ticket] Each outbox item is stamped with its owner `user_id` and is never sent under a different user. [proposed] `OutboxItem` gets `ownerUserId: String?`. Items enqueued before a guest session exists get stamped when `ensureGuest` first succeeds (Q4).

## 3. Endpoints per kind
Routing is `SupabaseTables.forKind` [code]:

| kind | table | write | delete (tombstone) |
|---|---|---|---|
| `attempt` | practice_attempts | [code] PostgREST upsert | [agreed] tombstones upsert (not emitted by app today) |
| `self_report` | self_reports | [code] upsert | [agreed] tombstones upsert (emitted by SadhanaController) |
| `routine` | routine_plans | [agreed] RPC `put_routine` (DEV-52); parked until it exists | n/a (a routine tombstone is dead-lettered `delete_unsupported`) |
| `window` | protection_windows | [code] route exists, nothing enqueues it; [ticket] syncs only per the DEV-52 decision | [agreed] tombstones upsert |
| `orbit` | orbit_events | [code] route exists, nothing enqueues it | [agreed] tombstones accepts `orbit` (unused; append-only) |
| unknown kind | — | [agreed] dead-letter `unknown_kind`, no request | |

- [code] Upsert: `POST /rest/v1/<table>?on_conflict=user_id,id` with `Prefer: resolution=merge-duplicates,return=minimal`, a single JSON object as the body, and success on 2xx.
- ~~[code] Delete: `DELETE /rest/v1/<table>?id=eq.<id>`~~ → [agreed] Delete: `POST /rest/v1/tombstones?on_conflict=user_id,kind,id`, `Prefer: resolution=merge-duplicates,return=minimal`, body `{"kind":..,"id":..,"client_updated_at":"<ISO UTC>"}`. The server deletes rows not newer than the tombstone; a tombstone for a row it never saw is stored and still beats older upserts.
- [ticket] `put_routine` via `POST /rest/v1/rpc/put_routine`. [proposed] arguments: `{"p_plan": <RoutinePlanCodec JSON>, "p_schema_version": int, "p_time_zone": "Area/City", "p_practice_local": "HH:MM:SS"|null, "p_expected_revision": int, "p_idempotency_key": "uuid"}`. It returns `{"revision": int, "plan": {...}, "time_zone":..., "practice_local":..., "updated_at":...}`. On a mismatch it raises a custom SQLSTATE, which PostgREST maps to **HTTP 409**, and the body carries the canonical row (`{"code":"routine_conflict","details": <canonical row JSON>}`). This maps onto `RemoteSaveResult.Conflict(canonical, revision)` [code]. The migration is not written yet, and the columns `revision`, `time_zone` and `practice_local` are missing [code].

## 4. Payloads (exact field names on the branch)
Timestamps are ISO-8601 UTC strings (`Instant.toString()`), dates are `YYYY-MM-DD` and ids are client-generated strings.

**attempt** [code] `SyncPayloads.attempt`:
`id` str · `practice_id` str · `content_version` str · `started_at` ts · `zone_id` str? · `intended_seconds` int · `active_ms` long ≥0 · `outcome` "COMPLETED"|"ENDED"|"INTERRUPTED" · `ended_at` ts?. The server defaults `evidence_type='app_observed'`.

**self_report / independent** [code] `SyncPayloads.selfReport`:
`id` · `local_date` date · `intended_minutes` int? · `actual_minutes` int? (DB check 1..180) · `practice_id` str?. The server defaults `kind='independent'` and `evidence_type='self_report'`.

**self_report / stage1_check** [code] `Stage1.payload`:
`id` · `kind`:"stage1_check" · `attempt_id` str · `local_date` · `practice_id` · `reached_spine` "yes"|"partly"|"no"|null · `drift_count` int 0..10? · `drift_reasons` str[] · `note` str? ≤500.
- [ticket] It is sent with `evidence_type=self_report` only and is never aggregated into scores or health data. [proposed] The client must never send `evidence_type`, so the server default stands (or a check enforces it, per FE request 12).

**routine** [code] `SyncPayloads.routine`: `{"id":"current","schema_version":int,"plan":<RoutinePlanCodec JSON>}`, one row per user. [proposed] It is replaced by the `put_routine` arguments in §3.

**tombstone** [code] `SyncPayloads.tombstone`: `{"id":"<id>","deleted":true}`, queued under the same `kind:id` key, which supersedes a pending upsert. `isTombstone` is an exact suffix match, so the client must emit it only through `SyncPayloads.tombstone`.

**window** [code schema, no payload builder yet]: `id`, `start_time` time, `end_time` time, `start_days` int[], `enabled` bool, `packages` text[] (held back pending DEV-52).

**orbit** [code schema, no payload builder yet]: `id`, `type`, `local_date`, `source`. [proposed, per FE request 4] add `entity_id` and `created_at`, check constraints on type/source, and make it append-only.

**client_updated_at** [ticket, DEV-42] [agreed]: stamped on the outbox item at enqueue (UTC epoch ms, `OutboxItem.clientUpdatedAtMs`) and injected by the sender into every upsert / tombstone body as an ISO-8601 UTC string. Payload builders must NOT add it themselves. Column added by DEV-42 (`20261017120100_dev42_client_updated_at.sql`, all five tables, nullable).

## 5. Idempotency
- [code] The key is `kind:id`. Re-enqueueing replaces the payload and moves the item to the tail. Upserts on `(user_id,id)` are idempotent.
- [ticket] Tests show 3 sends give 1 row, a crash after the ack but before local removal resends harmlessly, and a tombstone after an upsert ends with no row.
- [code] RoutineSync's `RoutineMutation.idempotencyKey` (a UUID) is reused only if the payload is identical, and `keepMine` mints a new key. [proposed] `put_routine` stores the last `(user_id, idempotency_key) → result` and returns the same result on replay, not a 409.

## 6. Conflict semantics
- [ticket] The device is the authority for its own records. Last writer wins on `client_updated_at`: the server ignores older writes, and the ignored write still returns 2xx, so the client treats it as acked. Tombstones beat older upserts.
- [agreed] Implementation (`20261018090000_dev45_sync_conflicts.sql`, decision 0019): BEFORE INSERT/UPDATE `sync_guard` skips an update older than the stored row (or unstamped when the stored row is stamped) and any write not newer than a tombstone; `public.tombstones` + AFTER trigger `apply_tombstone` deletes. No soft delete.
- [ticket] `routine_plans` only goes through `put_routine` with `expected_revision`, and a 409 surfaces to the caller (DEV-52). [code] The client resolves it with `keepMine()` (re-PUT at the server revision with a new key) or `takeServer()`, never silently.
- [agreed] The outbox carries `routine`; the sender calls `put_routine` with `p_expected_revision` read at send time from `RoutineSyncStore`, stores the returned `revision`, parks on 404/PGRST202 and on 409, and publishes `Dev45SyncEngine.routineConflict`. `keepMineRoutine()` / `takeServerRoutine()` are the DEV-52 hooks.

## 7. Errors, retry, backoff
- [ticket] IO error, timeout, 5xx or 429: retry, honouring `Retry-After` (seconds or HTTP-date).
- [ticket] 401: refresh, then retry once.
- [ticket] 400, 403, 409 or 422: move to a dead-letter list with `{status, code, key}` and **no payload**, then continue with the rest. Exception [proposed]: a `put_routine` 409 goes to the routine conflict flow, not the dead-letter list.
- [ticket] WorkManager unique work `outbox-flush` with `NetworkType.CONNECTED`, and exponential backoff with jitter from 30 s up to 1 h.
- [code, to change] `drain()` currently stops at the first failure, and HttpURLConnection timeouts are 10 s connect / 15 s read.
- [proposed] Other codes: 404 (table or RPC missing, i.e. server not migrated) is treated as retryable with a long backoff rather than dead-lettered. A `PGRST`/`23xxx` code from the body is recorded in the dead-letter `code` field.
- [proposed] Sign-out or reset (DEV-47 AC6) cancels `outbox-flush` and keeps the items stamped to the old user, unsent. They are dropped on reset and parked on sign-out (Q5).

## 8. Ordering
- [ticket] Per-key ordering is kept. [code] The outbox is FIFO by last enqueue, with one entry per key, so only the latest payload per key exists.
- [proposed] After a dead-letter or retryable failure, other keys continue. A retryable failure on key K stops the current run (backoff), because it usually means network trouble. No cross-key ordering is guaranteed, so the server must not rely on, for example, a stage1_check arriving after its attempt (`attempt_id` has no FK) [code: no FK].

## 9. What the server guarantees
- [code] Own-rows isolation via RLS for anonymous and linked users alike. A second guest sees nothing (verified in DEV-45 AC10).
- [code] Upserts and deletes are idempotent per `(user_id,id)`.
- [ticket] Older writes never overwrite newer ones (`client_updated_at`), and tombstones win over older upserts.
- [ticket] `put_routine` is atomic with a revision check, returning 409 plus the canonical row on a mismatch.
- [proposed] Server-set `created_at`/`updated_at` (trigger, FE request 9). A server-side `evidence_type` that the client cannot set to anything other than the table default.
- [ticket] Not guaranteed or out of scope: realtime, cross-user sharing, multi-device merge beyond LWW, and restore (AC7, only if the founder says yes).

## 10. Open questions for Frontend
1. Are the proposed `SyncSession` / `SyncScheduler` / `SyncStatusSource` signatures OK, especially the `ensureGuest()` result type and the debounce living inside `requestFlush()`?
2. Routine path: should `RoutineSyncCoordinator` call `put_routine` directly (outside the outbox), or should the outbox carry `routine` and hand 409s back to it? Who persists `serverRevision` and the pending key across restarts (today it's in memory)?
3. On a lost refresh token: should we show "Will sync when online" forever, or should Initiation or Settings offer "continue as new guest"? (Auto-creating a new anonymous user is forbidden.)
4. Items enqueued before consent or guest sign-in: should they be stamped to the first guest user once `ensureGuest` succeeds (proposed), or discarded?
5. On sign-out (not reset): should queued items for the old user be parked or dropped?
6. Deletes and LWW: is soft delete (`deleted_at`) acceptable, so that a late older upsert can't resurrect a row? Which kinds do you plan to delete (self_report only today, or attempts too)?
7. `client_updated_at`: can every record model carry a local edit timestamp (SadhanaAttempt, IndependentPracticeRecord, Stage1Check, RoutinePlanV1)? Is it in UTC ms?
8. Window and orbit payloads: will you add `SyncPayloads.window/orbit` under DEV-38? Is `packages` held back until DEV-52, and is orbit `local_date` or `occurrence_date`?
9. Status line: does "Synced <time>" mean the last fully empty queue or the last successful send? Do dead-lettered items count as "saved on this device"?
10. Multi-device restore (AC7) needs a founder answer. Should FE plan any UI for it?
11. Is `"routine"` id `"current"` OK as the permanent single-plan id?

## Known gaps on the branch (for planning)
- No `client_updated_at`, `revision`, `time_zone` or `practice_local` columns, no `put_routine`, no updated_at trigger [code].
- Nothing builds a `SupabaseClient` or calls `drain()` outside tests (DEV-45 audit) [ticket].
- `signInAnonymously()` runs a regex parse on JSON and does no refresh-token handling [code].

---
## Deviations and notes from the build (Backend, 18 Oct)
- **D1 Factory.** Backend did not add `AppSync.create`; DEV-47 owns `AppSync`. The engine is `Dev45SyncEngineFactory` (implements your `SyncEngineFactory`); bind it with `SyncEngines.dev45 = Dev45SyncEngineFactory` at merge. `sync/SyncContracts.kt` on `dev-45-sync-engine` is a byte-identical copy of the DEV-47 worktree file (sha1 052fc46…), so the add merges cleanly if you keep it unchanged.
- **D2 Foreground (resolved in the follow-up: `flushNow()` is now on `SyncScheduler`, see D11).** The agreed `SyncScheduler` has only `requestFlush()` / `schedulePeriodic()`. Network regained is handled inside the engine (default-network callback → `flushNow()`). For foreground, call `(engine as Dev45SyncEngine).flushNow()` (resets backoff, replaces a waiting backoff run, flushes now), or we add `flushNow()` to `SyncScheduler` by agreement. `requestFlush()` alone does not reset the backoff (so a server outage is not hammered on every edit).
- **D3 WorkManager.** Backoff is the engine's own (`SyncBackoff`, equal jitter in [exp/2, exp], 30 s floor, 1 h cap; `Retry-After` wins when longer, capped at 24 h) because WorkManager's has no jitter and caps at 5 h. The worker always returns success and schedules the delayed retry as unique work `outbox-flush` (`APPEND_OR_REPLACE`). The periodic job is unique work `outbox-flush-periodic` (6 h, CONNECTED) because WorkManager cannot share one unique name between one-time and periodic work. `requestFlush` = 2 s debounce + `KEEP`.
- **D4 Extra codes.** 404 (table missing = server not migrated) and 408 retry; for `routine`, 404 / PGRST202 parks. Any other 4xx (e.g. 413) is dead-lettered like 400. Dead-letter code = PostgREST/SQLSTATE `code` from the body if it is a plain token, else `http_<status>`; `unknown_kind`, `delete_unsupported`, `bad_payload` for client-side rejections. The list keeps the last 200, no payloads.
- **D5 put_routine arguments (C4/C5).** `{"p_plan":<plan>,"p_schema_version":int,"p_time_zone":"<ZoneId.systemDefault() at send>","p_expected_revision":int,"p_idempotency_key":"<uuid>"}`. `p_practice_local` is dropped (not stored on the device). DEV-52 must accept exactly these names (PostgREST matches functions by argument names; an extra/missing name is a 404 → parked). 2xx must return a JSON object with `"revision"`. 409 must be the agreed top-level body; a PostgREST `raise` puts `details` in a string, so DEV-52 should set `response.status` to 409 and return the JSON object.
- **D6 Status (revised in the follow-up, 17 Oct evening).** Parked items count as pending for status. `SyncStatusSource` now exposes `pendingCount` (sendable now) and `parkedCount` (routine waiting for DEV-52, unresolved conflict, or retry limit). `Synced(at)` only when both are 0 (dead letters still excluded); the engine also records lastSyncedAt only then. Pending or parked + offline -> `WaitingForNetwork` (Frontend: "Will sync when online"); pending or parked + online -> `LocalOnly` (Frontend: "Saved on this device"). Tests: `SyncHeadOfLineTest.neverSyncedWhileARoutineIsParkedD6`.
- **D7 Legacy outbox file.** Pre-DEV-45 entries are read as `op` from the payload, `owner=null` and `client_updated_at` = time of the first load (`DraftStore` exposes no file mtime). Legacy routine entries get a fresh idempotency key once.
- **D8 Unstamped writes.** A write without `client_updated_at` never overwrites a row that has one (server rule). Every write through the engine is stamped, so this only affects old builds.
- **D9 Hooks.** Both the constructor `onEnqueue` (your DEV-47 hook) and the engine's `listener` call `requestFlush()`, so it may be called twice per enqueue; the debounce collapses it.
- **D11 Contract follow-up (DEV-47 integration; patch `handoffs/dev45-synccontracts-diff.patch`, applies cleanly to the dev47 worktree's file).** Added to `sync/SyncContracts.kt`: `SyncScheduler.flushNow()`, `SyncSession.sessionLost`, `suspend SyncEngine.reset()`, `SyncStatusSource.pendingCount` / `parkedCount`, and `SyncEngines.dev45 = Dev45SyncEngineFactory` (bound; the app still decides via BuildConfig whether to create it). `DisabledSync` implements all of them as no-ops. `Dev45Bridge` can drop reflection: use `SyncEngines.dev45`, `engine.scheduler.flushNow()`, `engine.reset()` (now **suspend**; call from a coroutine), `engine.session.sessionLost`. `Dev45SyncEngine.sessionLost` stays for compatibility. Merge order: DEV-45 first, then DEV-47 rebases (take the DEV-45 version of SyncContracts.kt).
- **D12 Lazy factory.** `Dev45SyncEngineFactory.create/build` returns at once on any thread; the Keystore session decrypt, the outbox / routine / lastSyncedAt reads and the network callback registration run on `Dispatchers.IO`. Until ready: `session.userId` is null, status is `LocalOnly`, `requestFlush`/`flushNow` just enqueue WorkManager work (the worker waits for the engine), `ensureGuest()` suspends until ready. `Dev45SyncEngine.outbox` / `deadLetters` / `keepMineRoutine` / `takeServerRoutine` block until ready (don't call them on main). Signature change: `build(outbox = ...)` is now a provider `() -> SyncOutbox` (test seam; the app uses `create`).
- **D13 Head-of-line limit.** 404 / 408 are item-level: the flusher records a failure on the item (`OutboxItem.failures`, persisted, reset by any re-enqueue) and continues with the items behind it; the flush still ends `RetryLater` so backoff applies. At `OutboxFlusher.MAX_ITEM_RETRIES` (5) the item is dead-lettered with code `retry_limit` (status 404/408); a routine is parked `retry_limit` instead and not re-sent until it changes (it counts in `parkedCount`). 5xx / 429 / transport errors are server-wide: they still stop the run and never dead-letter. Note: a dead-lettered row is not re-sent unless it is edited again.
- **D14 SyncOutbox API.** Stable for DEV-47: no signature changed or removed. Additive only: `OutboxItem.failures: Int = 0` (last constructor parameter, defaulted) and `SyncOutbox.recordFailure(item): Int` (engine use). Outbox file: optional `f<i>` key, older files load with 0.
- **D15 put_routine names (for DEV-52, binding).** The RPC must take exactly `p_plan`, `p_schema_version`, `p_time_zone`, `p_expected_revision`, `p_idempotency_key` (no `p_practice_local`, no others). Any other name set makes PostgREST answer 404 / PGRST202 and the routine stays parked.
- **D10 Not done.** AC7 restore (waits for the founder's multi-device answer). AC10 cloud verification against `shanivar-prototype`: not run, because Backend's DEV-45 rules were local stack only; the same checks pass against the local stack (`SupabaseSyncTest.e2eGuestPushReadBack`).
