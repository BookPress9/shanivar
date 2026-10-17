package com.shanivar.nativeapp

import com.shanivar.nativeapp.sync.AppSync
import com.shanivar.nativeapp.sync.GuestResult
import com.shanivar.nativeapp.sync.SyncEngine
import com.shanivar.nativeapp.sync.SyncScheduler
import com.shanivar.nativeapp.sync.SyncSession
import com.shanivar.nativeapp.sync.SyncStatusSource
import com.shanivar.nativeapp.sync.SyncUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.rules.ExternalResource

/** DEV-47 test fakes for the DEV-45 contract (sync/SyncContracts.kt). */
class FakeSyncSession(override var userId: String? = null, var result: GuestResult = GuestResult.Ok("guest-1")) : SyncSession {
    var ensureGuestCalls = 0; var signOutCalls = 0
    var log: MutableList<String>? = null
    override suspend fun ensureGuest(): GuestResult {
        ensureGuestCalls++
        (result as? GuestResult.Ok)?.let { userId = it.userId }
        return result
    }
    override suspend fun signOut() { signOutCalls++; userId = null; log?.add("signOut") }
}

class FakeSyncScheduler : SyncScheduler {
    var flushRequests = 0; var periodic = 0
    var log: MutableList<String>? = null
    var failFlush = false
    override fun requestFlush() { flushRequests++; log?.add("requestFlush"); if (failFlush) error("flush failed") }
    override fun schedulePeriodic() { periodic++ }
}

class FakeSyncStatus(initial: SyncUiState = SyncUiState.LocalOnly, dead: Int = 0) : SyncStatusSource {
    override val state = MutableStateFlow(initial)
    override val deadLetterCount = MutableStateFlow(dead)
}

class FakeSyncEngine(
    override val session: FakeSyncSession = FakeSyncSession(),
    override val scheduler: FakeSyncScheduler = FakeSyncScheduler(),
    override val status: FakeSyncStatus = FakeSyncStatus(),
) : SyncEngine

/** An enabled AppSync over [engine]; ensureGuest runs inline so tests see it immediately. */
fun enabledSync(engine: FakeSyncEngine = FakeSyncEngine()): AppSync =
    AppSync.create(true, "https://example.invalid", "test-anon", CoroutineScope(Dispatchers.Unconfined)) { engine }

/** Restores the disabled process-wide AppSync around each test. */
class AppSyncReset : ExternalResource() {
    override fun before() = AppSync.resetForTests()
    override fun after() = AppSync.resetForTests()
}

/** Shaped like DEV-45's Dev45SyncEngine extras (flushNow / reset / sessionLost), found by Dev45Bridge. */
class FakeDev45Engine(val inner: FakeSyncEngine = FakeSyncEngine()) : com.shanivar.nativeapp.sync.SyncEngine by inner {
    var flushNowCalls = 0; var resetCalls = 0
    val log = mutableListOf<String>().also { inner.session.log = it; inner.scheduler.log = it }
    var failFlushNow = false
    /** What the status reports once flushNow ran (null = unchanged, e.g. to force the timeout). */
    var afterFlush: com.shanivar.nativeapp.sync.SyncUiState? = com.shanivar.nativeapp.sync.SyncUiState.Synced(java.time.Instant.EPOCH)
    val sessionLost: Boolean get() = true
    fun flushNow() {
        flushNowCalls++; log += "flushNow"
        if (failFlushNow) error("flush failed")
        afterFlush?.let { inner.status.state.value = it }
    }
    fun reset() { resetCalls++; inner.session.userId = null }
}
