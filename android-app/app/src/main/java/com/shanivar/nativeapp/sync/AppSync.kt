package com.shanivar.nativeapp.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    /** App foreground: DEV-45 `flushNow()` (resets backoff, flushes now); plain requestFlush if the engine lacks it. */
    fun onForeground() {
        if (!enabled) return
        if (!Dev45Bridge.call(engine, "flushNow")) runCatching { engine.scheduler.requestFlush() }
    }

    /** Outbox enqueue hook (AppSyncOutbox). Debounce lives in the scheduler. */
    fun onEnqueue() { if (enabled) runCatching { engine.scheduler.requestFlush() } }

    /** Sign-out hook (AC6): clears tokens; the engine parks the old user's items, never sends them under a new user. */
    suspend fun signOut() { if (enabled) runCatching { engine.session.signOut() } }

    /** Non-suspending [signOut] for UI callers (AccountState.logout); runs on [scope], never the main thread. */
    fun requestSignOut() { if (enabled) scope.launch { signOut() } }

    /** Reset / wipe this device (AC6): DEV-45 `reset()` (cancel work, clear tokens, drop queue); else sign-out. */
    suspend fun reset() {
        if (!enabled) return
        if (!Dev45Bridge.call(engine, "reset")) runCatching { engine.session.signOut() }
    }

    /** Diagnostics only: the engine lost its refresh token (contract Q3). Null when unknown. */
    val sessionLost: Boolean? get() = if (enabled) Dev45Bridge.flag(engine, "getSessionLost") else null

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
        fun isEnabled(flag: Boolean, url: String, anonKey: String) = flag && url.isNotBlank() && anonKey.isNotBlank()

        val DISABLED = AppSync(false, DisabledSync, CoroutineScope(Dispatchers.Unconfined))

        /** [engine] is only invoked when sync is enabled; a null or throwing engine leaves sync off. */
        fun create(
            flag: Boolean, url: String, anonKey: String,
            scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            engine: () -> SyncEngine?,
        ): AppSync {
            if (!isEnabled(flag, url, anonKey)) return DISABLED
            val e = runCatching { engine() }.getOrNull() ?: return DISABLED
            return AppSync(true, e, scope)
        }

        @Volatile var current: AppSync = DISABLED
            private set

        fun install(sync: AppSync) { current = sync }
        @androidx.annotation.VisibleForTesting fun resetForTests() { current = DISABLED }
    }
}
