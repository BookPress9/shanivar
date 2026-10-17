# Frontend → Backend: answers to the DEV-45/DEV-47 contract draft
Shanivar Frontend, 17 Oct 2026, 22:xx CEST. Reply to `backend-to-frontend-dev45-contract.md` (that file is unchanged).
Read: Jira DEV-47 and DEV-45 (both Open, no comments, no issue links), main @ b372079, `dev-38-outbox-gaps` @ 20fb519 (2 commits, **not merged**), supabase/migrations x2, supabase/config.toml.
DEV-47 worktree: `/workspace/shanivar-v3-dev47`, branch `dev-47-sync-wiring` off main. Baseline `testDebugUnitTest`: 507 tests, 0 failures, 1 skipped (the E2E test).

Short version: the draft is sound and matches the tickets. **Agreed:** §0 interfaces (with the additions below), §1 refresh flow, §2 ownership stamping, §5, §7 error classes, §8 ordering.
**Need changes:** soft delete shown as an RLS select filter (§6), tombstones sent as `DELETE` (§3), `put_routine` args that the client can't fill today (§3), and one outbox locking bug that has to be fixed in the engine (G1).

---
## Answers to the 11 questions

**Q1. Signatures: OK, with four additions.**
- `ensureGuest(): GuestResult` = `Ok(userId) | Offline | Failed(code) | Disabled`. Add `Disabled` for an empty `SUPABASE_URL`, so DEV-47 AC1 ("sync off, identical UI") can be tested without null checks scattered around. Never throws, and it is idempotent: if a session or refresh token exists, it returns `Ok` without a network call (DEV-45 AC1).
- Keep the debounce inside `requestFlush()`: 2 s, safe on the main thread, no I/O, only `enqueueUniqueWork`. Use `ExistingWorkPolicy.KEEP` and a "dirty" flag that the worker re-checks before it finishes. **Not** `REPLACE`, which would cancel a flush mid-send.
- **Add an enqueue hook on the outbox**: `SyncOutbox(store, onEnqueue: () -> Unit = {})`, or a listener that `AppSync` sets. Today every `enqueue` call site is in feature code (SadhanaController, RoutinePlanRepository). Without the hook, DEV-47 AC3/AC7 ("an enqueue triggers requestFlush") means touching every call site.
- `SyncStatusSource.state` also needs `pendingCount > 0 && online`, mapped to "Saved on this device". Map the states to the three AC4 strings like this: no session or sync off → "Saved on this device"; pending and no network → "Will sync when online"; queue empty (dead-letters excluded) with `lastSyncedAt` set → "Synced <time>"; pending, online, in backoff → "Saved on this device". Persist `lastSyncedAt` so it survives restarts.
- Ask: expose a pure-Kotlin `AppSync.create(url, anonKey, deps…)` factory so Robolectric tests can build the off and on states without BuildConfig.

**Q2. Routine: the outbox carries `routine`, the engine calls `put_routine`, and the engine persists the revision.**
Reason: on `dev-38-outbox-gaps`, `SharedPrefsRoutinePlanRepository.save()` already queues `routine:current` (RoutinePlanRepository.kt:57). `RoutineSyncCoordinator` is **never constructed in production code**; only `RoutineSyncTest` uses it, and its `serverRevision` and pending key live in memory. Keeping a single path gets offline/backoff/WorkManager behaviour for free and keeps AC3 uniform.
Proposal:
- The sender maps kind `routine` to `rpc/put_routine`. It reads `expected_revision` from a small persisted store owned by the engine (e.g. `sync/routine.properties`: `serverRevision`, `lastKey`) at **send** time, not enqueue time. On 2xx it stores the returned revision.
- The idempotency key is minted at enqueue and kept in the outbox item. A re-enqueue with an identical payload keeps it, which matches today's coordinator rule.
- On 409, the item is **parked** (not dead-lettered, not retried), and the canonical row and revision are published as `routineConflict: StateFlow<RemoteSaveResult.Conflict?>`. DEV-52 UI resolves it with `keepMine()` (re-queue at the canonical revision with a new key) or `takeServer()` (local save without enqueue, then drop the parked item).
- `RoutineSyncCoordinator` becomes the thin UI-facing adapter over that flow, or is retired. Frontend owns that in DEV-52, not DEV-47.

**Q3. Lost refresh token: no auto-guest, no "Will sync when online".**
"Will sync when online" would be false while the phone is online. Show "Saved on this device" (true), and show `session_lost` in the diagnostics view (AC5 style, not to end users). A "start fresh on this device" action belongs with DEV-25/DEV-46 and needs founder wording. It is not in DEV-47. Keep the queued items stamped to the old user; they are never re-stamped.

**Q4. Stamp unowned items to the first guest. Never re-stamp owned items.**
Reason: real pre-session items exist. These are the DEV-38 backfill of `initiation-*` attempts (SadhanaController `backfillImportedAttempts`), Sadhana records from installs that finished onboarding before DEV-47, and anything made while `ensureGuest` was offline. It is one person on one device and local-first, so discarding them would be data loss. Rule: `ownerUserId == null` → stamp on the first `Ok`. A non-null owner that differs from the current user → never sent (DEV-45 AC2).

**Q5. Sign-out vs reset.**
Reset (wipe this device): cancel `outbox-flush`, clear tokens, **drop** the queue and dead-letters, clear `lastSyncedAt`.
Sign-out (only meaningful once DEV-25 linked accounts exist): cancel work, clear tokens, **park** items stamped to the old user, and resume them only if the same `user_id` signs in again.
Note: the app has **no sign-out or reset UI today** (no Settings/About screen exists). DEV-47 will add `AppSync.reset()` and `signOut()` hooks, tests, and a debug-only diagnostics entry. Product UI is DEV-46/DEV-25.

**Q6. Prefer a tombstone table over `deleted_at` + an RLS select filter. Only `self_report` is deleted.**
- What gets deleted: only independent self-reports (`SadhanaController.deleteOwn`, which emits `SyncPayloads.tombstone`). Attempts and stage1 checks are never deleted in the app. Account deletion is DEV-46 (hard delete).
- Problem with the proposed soft delete: if `own_select` filters `deleted_at is null`, a later `INSERT … ON CONFLICT DO UPDATE` on that id checks the existing row against SELECT policies and **errors** (42501 → 403 → dead-letter) instead of being ignored. A tombstone sent as an upsert also hits NOT NULL columns (`local_date`) when the row never reached the server, giving a 400 → dead-letter.
- Proposal: `public.tombstones(user_id, kind, id, client_updated_at, primary key(user_id,kind,id))` with own-rows RLS. The client sends the tombstone as an upsert on `(user_id,kind,id)`. An AFTER trigger hard-deletes the data row if its `client_updated_at <=` the tombstone's. The BEFORE INSERT/UPDATE trigger on data tables skips writes older than a tombstone.
- If you prefer `deleted_at`: filter in the client queries or a view, **not** in RLS, and accept tombstone inserts for unseen ids (make the payload columns nullable when `deleted_at` is set, or use a `delete_row(kind,id,ts)` RPC).
- Either way the sender stops issuing `DELETE`, and `isTombstone`'s suffix match is replaced by an explicit `op` field on `OutboxItem` (see G4).

**Q7. `client_updated_at`: stamp it in the outbox item at enqueue, as UTC epoch ms, and send it as an ISO-8601 UTC string.**
Reason: none of `SadhanaAttempt`, `IndependentPracticeRecord`, `Stage1Check` or `RoutinePlanV1` has an edit timestamp. Adding one means codec and test churn across the Sadhana record and the RoutinePlan codec. Every enqueue call site runs synchronously inside the local edit, in the same function, so the enqueue time equals the edit time:
- `finish()`
- `recordOwn`
- `adjustOwnActual`
- `deleteOwn`
- `saveStage1Check`
- `RoutinePlanRepository.save`

Proposal: `OutboxItem(kind, id, payload, op, clientUpdatedAtMs, ownerUserId, idemKey?)`, persisted with the outbox. The sender injects `"client_updated_at":"<Instant>"`, the same format as `started_at`, and the column is `timestamptz`. The one exception is the DEV-38 upgrade backfill, which is stamped at backfill time; that is harmless because the server has no such rows. Note that DEV-42's Jira summary doesn't mention `client_updated_at`, so please confirm which migration owns it.

**Q8. Windows: no separate payload. They already sync inside `routine_plans.plan`. Orbit: deferred.**
- `RoutinePlanCodec.encode` (RoutinePlanRepository.kt:119/146) already writes `windows` **and `protectedPackages`** into the plan JSON. The DEV-38 branch therefore **already sends package names** via `routine:current` today, which conflicts with DEV-45's "packages only per the DEV-52 decision" (see C1).
- Frontend proposal for DEV-52's "one home": `plan` JSON is the home, and the `protection_windows` table stays unused. Until DEV-52 decides, the routine payload strips `protectedPackages` (a one-line change in `SyncPayloads.routine`).
- Orbit: not in the DEV-47 ACs and nothing enqueues it. The model field is `occurrenceDate`; map it to `local_date` (document it, no rename). Ids are semantic per user (`routine-created`, `return-YYYY-MM-DD`), which is fine with `(user_id,id)`. Append-only is OK.

**Q9. "Synced <time>" = the end of the last flush that left zero sendable items (dead-letters excluded).**
"Last successful send" would show "Synced" while items are still pending. Dead-lettered items stay intact in the local record, so "Saved on this device" stays true. They don't block "Synced", and they appear only as a count in diagnostics (AC5).

**Q10. Restore: Frontend plans no UI in DEV-47.**
It needs a founder yes plus DEV-25 linked sign-in, because an anonymous guest can't be restored on a fresh install anyway.

**Q11. `"current"`: yes, permanent.**
There is a single plan per user (`SharedPrefsRoutinePlanRepository`), and the table default is already `'current'`. `put_routine` can drop the id argument and use `'current'` server-side.

---
## [proposed] items that conflict with the client code or the ACs
- **C1** §4/§3 "packages held back pending DEV-52": already violated, because routine plan JSON includes `protectedPackages` (Q8). Fix before the first live flush.
- **C2** §6 soft delete with a select-policy filter: breaks upserts of deleted ids under RLS (Q6).
- **C3** §3/§4 tombstone delivered as `DELETE`: can't express "tombstone beats an older upsert", and it can't carry `client_updated_at` (Q6).
- **C4** §3 `put_routine` args `p_time_zone` (NOT NULL) and `p_practice_local`: `RoutinePlanV1` stores neither. `SyncedRoutine` exists only in RoutineSync.kt tests. The client can send `ZoneId.systemDefault().id` at send time. `practice_local` is not stored anywhere (`RoutineSeed.practiceFor(wake)` = wake+5 is derived), so make it nullable or drop it for v1.
- **C5** §3 409 body: PostgREST puts `details` in the error JSON as a **string**, so the canonical row must be JSON-encoded inside it, or returned via `hint`. Please pin the exact shape (`code` = your SQLSTATE, e.g. `PT409`) so the parser can be tested.
- **C6** §1 `NeedsSession` state: there is no fourth user-visible string in DEV-47 AC4. Map it to "Saved on this device" and diagnostics (Q3).
- **C7** §7 exponential backoff up to 1 h vs DEV-47 AC8 "within 1 minute of reconnecting": a run that failed with IO while "connected" (flaky network or captive portal) can sit in backoff after the network returns. On network regained (and on foreground), `requestFlush()` should re-enqueue with no delay and reset the backoff, not KEEP a 1 h backoff run.

## Gaps (not in the draft)
- **G1 (engine bug, DEV-45):** `SyncOutbox.drain()` and `enqueue()` are both `@Synchronized` on the branch. Once `drain` does network I/O (10 s connect / 15 s read per item), any main-thread `enqueue` (SadhanaController, RoutinePlanRepository) blocks for the length of the flush, which is an ANR. That violates DEV-47 AC3 in spirit. Snapshot under the lock, send outside it, then remove the key only if the payload/stamp is unchanged. The last part is also needed for the crash-after-ack and re-enqueue-during-send cases.
- **G2:** `drain()` uses `runCatching { send }.getOrDefault(false)`, so everything is a Boolean. The sender needs a result type (`Acked | Retry(after) | Refresh | DeadLetter(status, code) | Conflict(body) | Parked`).
- **G3:** missing dependencies: `androidx.work:work-runtime-ktx`, `androidx.lifecycle:lifecycle-process` (foreground trigger, Frontend), and Keystore-backed storage (security-crypto or a raw Keystore AES/GCM). Also no `Application` subclass exists yet; DEV-47 adds `ShanivarApp : Application` and `android:name` in the manifest.
- **G4:** `OutboxItem` persistence format (Properties `k/p` pairs) needs new fields (`op`, `ts`, `owner`, `idem`) with a migration from the existing file (`sadhana/outbox.properties`). Read old entries as `op=upsert`, `owner=null`, `ts=file mtime`.
- **G5 (AC ambiguity, PM):** Initiation has **no "consent" screen** (`InitiationScreen`: ARRIVAL, IDENTITY, AUTH, PROPOSITION, …). DEV-47 AC2 says to call `ensureGuest` after consent. Frontend plans to call it on `ready()` (leaving PROPOSITION) when `identityMode == GUEST`, and also at app start for drafts that are already `GUEST` and past PROPOSITION. Please confirm or correct.
- **G6 (PM):** `INITIATION_V1` is `false` in release, and users who never ran Initiation have no guest step, so nothing would ever sync for them. Decide whether `ensureGuest` also runs at app start for those installs.
- **G7:** `~/.gradle/gradle.properties` supplies `supabaseUrl` / `supabaseAnonKey` to **every** build, release included, because `app/build.gradle.kts` reads them in `defaultConfig`. Until DEV-44 flavours land, consider debug-only, or accept that release is sync-on. The E2E test reads different names (`supabaseE2eUrl` / `supabaseE2eAnonKey`).
- **G8:** `IndependentPracticeRecord.privateByDefault` isn't in `SyncPayloads.selfReport` (FE request 7). Fine as long as the server default is `true`.
- **G9:** the main checkout `/workspace/shanivar-v3` is on `dev-38-outbox-gaps`, and DEV-38 isn't merged to main. DEV-47 (and DEV-45) need DEV-38 merged first, or both must branch from it.
