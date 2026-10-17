# 0019 — Sync conflict rule (DEV-45)

Date: 18 Oct 2026 · Status: adopted (DEV-45 founder-confirmed AC6; contract agreed with Frontend 17 Oct)

## Rule
- The device is the authority for its own records. Every outbox item carries `client_updated_at`
  (UTC, stamped by the device at enqueue, inside the local edit).
- Last writer wins on `client_updated_at`, decided **server-side** (`public.sync_guard()` trigger,
  migration `20261018090000_dev45_sync_conflicts.sql`): an update older than the stored row, or without a
  client time when the stored row has one, is skipped. The request still succeeds, so the device treats it
  as acknowledged and drops it. Equal times apply (harmless resend).
- Deletes are tombstones, not bare `DELETE`: the device upserts `public.tombstones(user_id, kind, id,
  client_updated_at)`; the server deletes rows not newer than the tombstone, and any later write not newer
  than the tombstone is skipped, so a late older upsert never resurrects a row. A genuinely newer write
  (after the delete) is allowed.
- `routine_plans` goes through `put_routine` with `expected_revision` (DEV-52). A 409
  `{code:"revision_conflict", current_revision, current_plan}` surfaces to the caller (the item is parked,
  the canonical row published; DEV-52 resolves with keepMine / takeServer, never silently). Until
  `put_routine` exists the routine item stays queued (parked), never dead-lettered.

## Not covered
Realtime, sharing between users, multi-device merge beyond last-writer-wins, restore (DEV-45 AC7, waits for
the founder's multi-device answer).
