package com.shanivar.nativeapp

/**
 * Best-effort in-process bridge so standalone alarm/dismissal Activities - which can be entered
 * from a cold process via `AlarmFireReceiver`, with no Compose/ViewModel tree available - can still
 * record a dismissal event if a logged-in session happens to exist in this process.
 *
 * This is intentionally best-effort, not a real persisted-session fix: the app does not currently
 * persist the auth token to disk (see ShanivarApi/AccountState), so an alarm firing after the
 * process was killed overnight will have no session here and dismissal recording will simply be
 * skipped. That's an accepted gap for v1, not silently papered over - recording is documented in
 * the task spec as "records only, it never itself grants anything," so a missing/failed record
 * must never block a user out of a legitimately-verified local alarm dismissal.
 */
object ShanivarSession {
    @Volatile var api: ShanivarApi? = null
}
