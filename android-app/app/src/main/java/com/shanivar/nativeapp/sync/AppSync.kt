package com.shanivar.nativeapp.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * DEV-47: the one process-wide sync wiring, created in ShanivarApplication.onCreate. It only calls the
 * DEV-45 engine ([SyncContracts.kt]); it never touches the network itself. Every entry point is
 * main-thread safe: ensureGuest runs on [scope] (IO by default), and requestFlush is non-blocking by
 * contract. When disabled, every call is a no-op and [status] is null, so screens are unchanged.
 */
class AppSync internal constructor(
    val enabled: Boolean,
    private val engine: SyncEngine,
    private val scope: CoroutineScope,
    /** Upper bound for the best-effort flush before a logout sign-out. */
    private val logoutFlushTimeoutMs: Long = LOGOUT_FLUSH_TIMEOUT_MS,
) {
    private val guestInFlight = AtomicBoolean(false)

    /** Null when sync is off: the status line is not shown at all. */
    val status: SyncStatusSource? get() = if (enabled) engine.status else null
    val hasSession: Boolean get() = enabled && engine.session.userId != null

    /**
     * App start: periodic flush. Never a first sign-in (AC2: the guest step in Initiation does that).
     * The only sign-in here is a retry of an earlier failed guest step: [guestRetryAllowed] must say
     * Initiation is complete as a guest. It is evaluated off the main thread, and only when there is
     * no session.
     */
    fun onAppStart(guestRetryAllowed: () -> Boolean = { false }) {
        if (!enabled) return
        runCatching { engine.scheduler.schedulePeriodic() }
        requestGuest(guestRetryAllowed)
    }

    /** The person left Proposition as a guest (the Initiation guest step). Never blocks Initiation. */
    fun onGuestChosen() = requestGuest { true }

    /**
     * Initiation just completed as a guest: one more guest sign-in try if the Proposition-exit call
     * failed (offline). No call when a session exists or that call is still in flight. Next app start
     * stays the fallback.
     */
    fun onInitiationCompleteAsGuest() = requestGuest { true }

    /** App foreground resets backoff and requests an immediate flush through the agreed contract. */
    fun onForeground() {
        if (!enabled) return
        runCatching { engine.scheduler.flushNow() }
    }

    /** Outbox enqueue hook (AppSyncOutbox). Debounce lives in the scheduler. */
    fun onEnqueue() { if (enabled) runCatching { engine.scheduler.requestFlush() } }

    /** Sign-out hook (AC6): clears tokens; the engine parks the old user's items, never sends them under a new user. */
    suspend fun signOut() { if (enabled) runCatching { engine.session.signOut() } }

    /**
     * Logout (AccountState.logout), off the main thread: a best-effort flush first via `flushNow()`,
     * waiting at most [logoutFlushTimeoutMs] for the engine to report nothing
     * sendable (Synced) or no network, then [signOut] regardless. Anything still unsent stays queued
     * under the old user and is never sent under a new guest (privacy rule).
     */
    fun requestSignOut(): Job? {
        if (!enabled) return null
        return scope.launch {
            try { withTimeoutOrNull(logoutFlushTimeoutMs) { flushBestEffort() } } catch (_: Exception) {}
            signOut()
        }
    }

    private suspend fun flushBestEffort() {
        runCatching { engine.scheduler.flushNow() }
        engine.status.state.first { it is SyncUiState.Synced || it is SyncUiState.WaitingForNetwork }
    }

    /** Reset / wipe this device (AC6): cancel work, clear tokens and drop the queue. */
    suspend fun reset() {
        if (!enabled) return
        runCatching { engine.reset() }
    }

    /** Diagnostics only: the engine lost its refresh token (contract Q3). Null when unknown. */
    val sessionLost: Boolean? get() = if (enabled) engine.session.sessionLost else null

    private fun requestGuest(allowed: () -> Boolean) {
        if (!enabled || engine.session.userId != null) return
        if (!guestInFlight.compareAndSet(false, true)) return
        scope.launch {
            try {
                if (runCatching(allowed).getOrDefault(false)) runCatching { engine.session.ensureGuest() }
            } finally { guestInFlight.set(false) }
        }
    }

    companion object {
        /** Sync runs only when the build flag is on and both URL and anon key are set. */
        const val LOGOUT_FLUSH_TIMEOUT_MS = 3_000L

        fun isEnabled(flag: Boolean, url: String, anonKey: String) = flag && url.isNotBlank() && anonKey.isNotBlank()

        val DISABLED = AppSync(false, DisabledSync, CoroutineScope(Dispatchers.Unconfined))

        /** [engine] is only invoked when sync is enabled; a null or throwing engine leaves sync off. */
        fun create(
            flag: Boolean, url: String, anonKey: String,
            scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            logoutFlushTimeoutMs: Long = LOGOUT_FLUSH_TIMEOUT_MS,
            engine: () -> SyncEngine?,
        ): AppSync {
            if (!isEnabled(flag, url, anonKey)) return DISABLED
            val e = runCatching { engine() }.getOrNull() ?: return DISABLED
            return AppSync(true, e, scope, logoutFlushTimeoutMs)
        }

        @Volatile var current: AppSync = DISABLED
            private set

        fun install(sync: AppSync) { current = sync }
        @androidx.annotation.VisibleForTesting fun resetForTests() { current = DISABLED }
    }
}
