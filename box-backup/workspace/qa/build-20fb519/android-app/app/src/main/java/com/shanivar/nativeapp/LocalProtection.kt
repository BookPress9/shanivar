package com.shanivar.nativeapp

import android.content.Context

/**
 * What this device is enforcing right now, according to the device itself.
 *
 * Read from the `focus_guard` preferences that `FocusGuardService` writes when it starts and clears
 * when it stops. That record already existed and was already the thing `V1Home` and
 * `BlockOverlayActivity` trusted to decide whether protection is on - this type just gives it a name
 * and one owner instead of three separate `getLong("endsAt", 0L)` reads.
 */
data class LocalProtection(val packages: Set<String>, val endsAtEpochMillis: Long) {
    fun isActiveAt(nowMillis: Long): Boolean = packages.isNotEmpty() && endsAtEpochMillis > nowMillis
}

/** Read seam so [LocalProtectionController] is unit-testable without SharedPreferences. A
 * `fun interface` so a test can supply one with a lambda rather than an object expression. */
fun interface ProtectionStateStore {
    fun read(): LocalProtection?
}

class AndroidProtectionStateStore(context: Context) : ProtectionStateStore {
    private val appContext = context.applicationContext

    override fun read(): LocalProtection? {
        // Read fresh every time: the service writes this from its own process lifecycle, so a cached
        // handle can be stale in exactly the situation that matters - after a restart.
        val prefs = appContext.getSharedPreferences("focus_guard", Context.MODE_PRIVATE)
        val endsAt = prefs.getLong("endsAt", 0L)
        if (endsAt <= 0L) return null
        return LocalProtection(prefs.getStringSet("packages", emptySet()).orEmpty(), endsAt)
    }
}

/**
 * Starts and stops app protection **on this device, with no network and no account**.
 *
 * Decision 0014 inverted the order of authority here: "the local device state is authoritative for
 * scheduling and recovery", and "a warm process, network connection or server response must not be
 * required for a scheduled boundary or immediate stop".
 *
 * Before this existed, `FocusScheduleReceiver` could only act by constructing a `DisciplineEngine`,
 * which needs `ShanivarSession.api` to be warm *and* a successful Render round-trip. So a scheduled
 * window silently did nothing after process death (DEV-4), and - worse - protection could not be
 * stopped at all while offline, because `releaseFocus()` threw on the network call before reaching
 * `guard.stop()`.
 *
 * This class has no reference to any API type. That is the point: it cannot be made to wait on a
 * server, because it has no way to talk to one. Server mirroring is `DisciplineEngine`'s job and is
 * explicitly secondary.
 */
class LocalProtectionController(
    private val store: ProtectionStateStore,
    private val guard: FocusGuardController,
    private val permissions: DisciplinePermissions,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    /** The protection currently in force, or null when none is - including when a recorded window
     * has simply elapsed, which is not an active state even though the record may survive. */
    fun active(): LocalProtection? = store.read()?.takeIf { it.isActiveAt(now()) }

    /**
     * Begins protection immediately. Returns false only when the device genuinely cannot enforce -
     * missing Usage access or overlay permission - because in that case claiming success would be
     * the "never claim enforcement we cannot deliver" failure, not a recoverable error.
     */
    fun start(packages: Set<String>, endsAtEpochMillis: Long): Boolean {
        if (packages.isEmpty()) return false
        if (endsAtEpochMillis <= now()) return false
        if (!canEnforce()) return false
        guard.start(packages, endsAtEpochMillis)
        return true
    }

    /**
     * Stops protection now. Idempotent, offline, and never gated on sign-in, payment, a timer or a
     * practice - the test guide requires exactly that, and DEV-10's essential-access path depends on
     * it being unconditional.
     */
    fun stopNow() = guard.stop()

    fun canEnforce(): Boolean = permissions.hasUsageAccess() && permissions.hasOverlayPermission()

    companion object {
        fun create(context: Context): LocalProtectionController {
            val appContext = context.applicationContext
            return LocalProtectionController(
                AndroidProtectionStateStore(appContext),
                AndroidFocusGuardController(appContext),
                AndroidDisciplinePermissions(appContext),
            )
        }
    }
}
