package com.shanivar.nativeapp.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/*
 * DEV-45 / DEV-47 sync contract: the app-facing interfaces only. It follows the agreed handoff
 * (handoffs/backend-to-frontend-dev45-contract.md + frontend-answers-dev45-contract.md).
 *
 * Shanivar Backend (DEV-45) provides the real implementations: SyncSession (anonymous guest once per
 * install, encrypted tokens, refresh), SyncScheduler (WorkManager unique work "outbox-flush", CONNECTED
 * constraint, backoff, reset on reconnect/foreground) and SyncStatusSource. DEV-45 plugs them in by
 * setting [SyncEngines.dev45]. DEV-47 (app wiring, AppSync) builds only against what is in this file.
 * Keep this surface minimal; change it only by agreement with Backend.
 */

/** Result of [SyncSession.ensureGuest]. Never thrown; [Disabled] when sync is off. */
sealed interface GuestResult {
    data class Ok(val userId: String) : GuestResult
    data object Offline : GuestResult
    data class Failed(val code: String) : GuestResult
    data object Disabled : GuestResult
}

interface SyncSession {
    /** Current user id, or null when there is no session. In-memory read, safe on the main thread. */
    val userId: String?
    /** Anonymous sign-in once per install; returns Ok without a network call when a session exists. Never throws. */
    suspend fun ensureGuest(): GuestResult
    /** Clears tokens. Queued items stay stamped to the old user and are never sent under another (DEV-45 AC2). */
    suspend fun signOut()
}

interface SyncScheduler {
    /** Main-thread safe, no I/O, debounced inside the implementation. */
    fun requestFlush()
    /** Idempotent; called once per process start. */
    fun schedulePeriodic()
}

/** The quiet status line (DEV-47 AC4). */
sealed interface SyncUiState {
    /** Nothing sent yet, sync waiting for a session, or sending/backing off while online. */
    data object LocalOnly : SyncUiState
    /** Last flush that left nothing sendable (dead-letters excluded). */
    data class Synced(val at: Instant) : SyncUiState
    /** Items pending and no network. */
    data object WaitingForNetwork : SyncUiState
}

interface SyncStatusSource {
    val state: StateFlow<SyncUiState>
    /** Diagnostics only (DEV-47 AC5); never shown to end users. */
    val deadLetterCount: StateFlow<Int>
}

/** What DEV-45 hands to the app. */
interface SyncEngine {
    val session: SyncSession
    val scheduler: SyncScheduler
    val status: SyncStatusSource
}

/** DEV-45 builds its engine from the app context and BuildConfig values. */
fun interface SyncEngineFactory {
    fun create(context: android.content.Context, supabaseUrl: String, anonKey: String): SyncEngine
}

/** Binding point. DEV-45 replaces `null` with its factory; until then sync stays off even in debug. */
object SyncEngines {
    val dev45: SyncEngineFactory? = null
}

/** Sync-off implementation: no session, no network, no status line. */
object DisabledSync : SyncEngine, SyncSession, SyncScheduler, SyncStatusSource {
    override val session: SyncSession get() = this
    override val scheduler: SyncScheduler get() = this
    override val status: SyncStatusSource get() = this
    override val userId: String? get() = null
    override suspend fun ensureGuest(): GuestResult = GuestResult.Disabled
    override suspend fun signOut() {}
    override fun requestFlush() {}
    override fun schedulePeriodic() {}
    override val state: StateFlow<SyncUiState> = MutableStateFlow(SyncUiState.LocalOnly)
    override val deadLetterCount: StateFlow<Int> = MutableStateFlow(0)
}
