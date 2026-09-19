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
 * - releaseFocus() only stops the local guard service *after* the backend confirms release, so a
 *   network failure leaves protection running rather than silently dropping it - the caller (UI)
 *   is expected to keep the session screen open and retry per NATIVE_HANDOFF.md.
 */
class DisciplineEngine(
    private val permissions: DisciplinePermissions,
    private val api: DisciplineApi,
    private val guard: FocusGuardController,
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
        val sessionId = activeSessionId ?: api.activeFocus()?.id
        if (sessionId != null) {
            try {
                api.releaseFocus(sessionId)
            } catch (e: ApiFailure) {
                throw mapFailure(e)
            }
        }
        // No known/active session server-side is treated the same as a successful release
        // (idempotent, matching the backend's own "already released" no-op semantics) - there is
        // nothing to release, so proceed to stop the local guard rather than getting stuck.
        activeSessionId = null
        guard.stop()
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
            )
        }
    }
}
