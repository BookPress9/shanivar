package com.shanivar.nativeapp

import android.content.Context

/**
 * Real `DisciplineGateway` implementation. `DailyRhythm.kt` only declares the interface/shapes -
 * this is the engine behind it: local Android permission state + the backend's focus-session
 * lifecycle + the local foreground-service guard, per discipline_engine_squad.md's Agent B scope.
 *
 * State-transition rules (mirrors NATIVE_HANDOFF.md's "Native discipline seams to agree"):
 * - status() never claims ACTIVE/OFF without both usage-access and overlay permission actually
 *   granted; if either is missing it reports NEEDS_PERMISSION regardless of any locally-cached
 *   "focus was on" flag, because the guard service literally cannot detect or block without them.
 * - startFocus() checks permissions locally *before* calling the backend, so a permission gap
 *   never shows up disguised as a network/entitlement failure.
 * - releaseFocus() stops the local guard **first**, then tells the server. See below.
 *
 * ## Order of authority changed in Routine V3 (decision 0014)
 *
 * `releaseFocus()` used to stop the guard only *after* the backend confirmed, deliberately, so that
 * a network failure left protection running rather than silently dropping it. That reasoning
 * protected the wrong thing: it also meant a person with no signal **could not get their own apps
 * back**, because the API call threw before `guard.stop()` was ever reached.
 *
 * 0014 settles it - "the local device state is authoritative for scheduling and recovery", and
 * immediate stop must not require a server response. So the guard stops first and the server release
 * is best-effort. A failed server release leaves a stale session that the next `status()` or
 * `startFocus()` reconciles; it does not re-lock the device.
 *
 * `status()` likewise falls back to the device's own record when the server is unreachable, instead
 * of raising a network error over a question the device can already answer truthfully.
 */
class DisciplineEngine(
    private val permissions: DisciplinePermissions,
    private val api: DisciplineApi,
    private val guard: FocusGuardController,
    /** The device's own record of what it is enforcing, used when the server cannot be reached.
     * Null only in tests that do not exercise the offline path.
     *
     * Declared before [alarmScheduled] on purpose: several call sites pass `alarmScheduled` with
     * trailing-lambda syntax, which binds to the *last* parameter, so adding a parameter after it
     * silently rebinds those lambdas to the wrong argument. */
    private val localState: ProtectionStateStore? = null,
    private val alarmScheduled: () -> Boolean = { false },
) : DisciplineGateway {

    private fun hasRequiredPermissions() = permissions.hasUsageAccess() && permissions.hasOverlayPermission()

    // The release route needs a session id in its path (`/discipline/focus/:id/release`), so the
    // engine tracks the last-known active session id from whichever of status()/startFocus()
    // observed it most recently. If a process restart wiped this (activity/process death before
    // release), releaseFocus() re-discovers it via activeFocus() rather than failing.
    private var activeSessionId: String? = null

    override suspend fun status(): DisciplineStatus {
        if (!hasRequiredPermissions()) {
            return DisciplineStatus(ProtectionState.NEEDS_PERMISSION, alarmScheduled())
        }
        val active = try {
            api.activeFocus()
        } catch (e: ApiFailure) {
            // Offline. The device still knows exactly what it is enforcing, so report that rather
            // than a network complaint - and never downgrade a live window to OFF, which would tell
            // the user their apps are available when the guard is still blocking them.
            localState?.read()?.takeIf { it.isActiveAt(System.currentTimeMillis()) }?.let {
                return DisciplineStatus(ProtectionState.ACTIVE, alarmScheduled(), it.endsAtEpochMillis, it.packages)
            }
            throw DisciplineError.Network(e.message ?: "Could not reach Shanivar. Try again.")
        }
        return if (active != null && active.endsAtEpochMillis > System.currentTimeMillis()) {
            activeSessionId = active.id
            // Recover the native guard after process recreation before claiming protection.
            guard.start(active.packageNames, active.endsAtEpochMillis)
            DisciplineStatus(ProtectionState.ACTIVE, alarmScheduled(), active.endsAtEpochMillis, active.packageNames)
        } else {
            activeSessionId = null
            guard.stop()
            DisciplineStatus(ProtectionState.OFF, alarmScheduled())
        }
    }

    override suspend fun startFocus(minutes: Int, packageNames: Set<String>): DisciplineStatus {
        if (!hasRequiredPermissions()) {
            throw DisciplineError.PermissionRequired(
                "Turn on Usage access and Display over other apps in Android settings to protect your focus window."
            )
        }
        val info = try {
            // The server retains expired sessions for a cleanup grace period. Close only an
            // actually elapsed session before starting again; never release a live session.
            api.activeFocus()?.takeIf { it.endsAtEpochMillis <= System.currentTimeMillis() }?.let {
                api.releaseFocus(it.id)
            }
            api.startFocus(minutes, packageNames)
        } catch (e: ApiFailure) {
            throw mapFailure(e)
        }
        activeSessionId = info.id
        guard.start(info.packageNames, info.endsAtEpochMillis)
        return DisciplineStatus(ProtectionState.ACTIVE, alarmScheduled(), info.endsAtEpochMillis, info.packageNames)
    }

    override suspend fun releaseFocus(): DisciplineStatus {
        // Stop enforcing first, unconditionally. The person asked for their own apps back; the
        // device can do that without anyone's permission, and no network error may stand between
        // them and it. This ordering is what makes the test guide's "stop protection works locally
        // without network, sign-in, payment, timer or practice" true, and it is the foundation the
        // essential-access path (DEV-10) has to build on.
        guard.stop()
        val sessionId = activeSessionId
        activeSessionId = null
        // Best-effort server release. A stale server session is reconciled by the next status() or
        // startFocus() - both already close an elapsed session - so failing here costs bookkeeping,
        // not protection. Deliberately swallowed rather than mapped to an error: the user-visible
        // operation already succeeded.
        runCatching { (sessionId ?: api.activeFocus()?.id)?.let { api.releaseFocus(it) } }
        return DisciplineStatus(ProtectionState.OFF, alarmScheduled())
    }

    private fun mapFailure(e: ApiFailure): DisciplineError = when (e.status) {
        401, 402, 403 -> DisciplineError.EntitlementRequired(e.message ?: "This focus lock isn't included in your plan.")
        else -> DisciplineError.Network(e.message ?: "Could not reach Shanivar. Try again.")
    }

    companion object {
        /** Wires the real Android-backed engine. Call once and keep it alive with the account session. */
        fun create(context: Context, api: ShanivarApi, alarmScheduler: AlarmScheduler): DisciplineEngine {
            val appContext = context.applicationContext
            return DisciplineEngine(
                AndroidDisciplinePermissions(appContext),
                HttpDisciplineApi(api),
                AndroidFocusGuardController(appContext),
                alarmScheduled = { alarmScheduler.hasScheduledAlarm() },
                localState = AndroidProtectionStateStore(appContext),
            )
        }
    }
}
