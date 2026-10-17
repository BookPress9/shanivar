package com.shanivar.nativeapp

import com.shanivar.nativeapp.sync.AppSync
import com.shanivar.nativeapp.sync.GuestResult
import com.shanivar.nativeapp.sync.SyncEngine
import com.shanivar.nativeapp.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import com.shanivar.nativeapp.sync.SyncUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Owner-aware fake shaped like the agreed DEV-45 rules: unowned queued items are stamped to the first
 * user that flushes them (Q4), and a flush sends only items owned by the current user (DEV-45 AC2).
 */
private class OwnerAwareEngine(val outbox: SyncOutbox) : SyncEngine {
    override val session = FakeSyncSession(result = GuestResult.Ok("u1"))
    var online = false
    var flushes = 0
    val owners = HashMap<String, String>()
    val sent = mutableListOf<Pair<String, String>>() // key to user
    override val status = FakeSyncStatus()
    override val scheduler = object : SyncScheduler {
        override fun requestFlush() {
            flushes++
            val uid = session.userId ?: return
            outbox.pending.forEach { owners.putIfAbsent(it.key, uid) }
            if (!online) return
            // Like the DEV-45 flusher: other users' items are skipped, the rest continue (no removal here).
            outbox.pending.filter { owners[it.key] == uid && sent.none { s -> s.first == it.key } }.forEach { sent += it.key to uid }
        }
        override fun schedulePeriodic() {}
    }
}

/** DEV-47 AC6: legacy account logout signs the sync session out. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountSyncTest {
    @get:Rule val reset = AppSyncReset()

    @Test fun logoutSignsTheSyncSessionOutAndDoesNotWipeOrSignIn() {
        val e = FakeDev45Engine(); e.inner.session.userId = "u1"
        val s = AppSync.create(true, "https://example.invalid", "anon", CoroutineScope(Dispatchers.Unconfined)) { e }
        AccountState { s }.also { it.logout(); runBlocking { it.syncSignOut?.join() } }
        assertEquals(1, e.inner.session.signOutCalls)
        assertFalse(s.hasSession)
        assertEquals(0, e.resetCalls) // logout keeps local records: sign-out, not reset
        assertEquals(0, e.inner.session.ensureGuestCalls) // no new guest is created by logout
    }

    @Test fun logoutUsesTheProcessAppSyncByDefault() {
        val e = FakeSyncEngine(status = FakeSyncStatus(SyncUiState.Synced(java.time.Instant.EPOCH))); e.session.userId = "u1"
        AppSync.install(enabledSync(e))
        AccountState().also { it.logout(); runBlocking { it.syncSignOut?.join() } }
        assertEquals(1, e.session.signOutCalls)
        assertEquals(1, e.scheduler.flushRequests) // no flushNow on this engine: requestFlush
    }

    @Test fun logoutWithSyncOffIsANoOp() {
        AccountState().logout() // process AppSync is disabled: nothing to call, nothing thrown
        assertFalse(AppSync.current.enabled)
    }

    @Test fun itemsQueuedBeforeLogoutAreNeverSentUnderTheNextGuest() {
        lateinit var engine: OwnerAwareEngine
        val box = SyncOutbox(MemDraftStore(), onEnqueue = { AppSync.current.onEnqueue() })
        engine = OwnerAwareEngine(box)
        val s = AppSync.create(true, "https://example.invalid", "anon", CoroutineScope(Dispatchers.Unconfined), logoutFlushTimeoutMs = 100) { engine }
        AppSync.install(s)

        s.onGuestChosen()                        // guest u1 via the Initiation guest step
        box.enqueue("attempt", "a1", "{}")       // offline: queued, stamped to u1
        val flushesBefore = engine.flushes
        AccountState { s }.also { it.logout(); runBlocking { it.syncSignOut?.join() } } // flush (offline: times out), then sign-out
        assertEquals(flushesBefore + 1, engine.flushes)
        assertNull(engine.session.userId)
        s.onAppStart { false }                   // no new guest until Initiation's guest step
        assertEquals(1, engine.session.ensureGuestCalls)

        engine.session.result = GuestResult.Ok("u2")
        s.onGuestChosen()                        // a new guest u2
        engine.online = true
        box.enqueue("self_report", "s1", "{}")   // flush under u2
        assertEquals(listOf("self_report:s1" to "u2"), engine.sent)
        assertTrue(box.pending.any { it.key == "attempt:a1" }) // u1's item stays queued, never sent as u2
    }

    private fun dev45Sync(e: FakeDev45Engine, timeoutMs: Long = 3_000) =
        AppSync.create(true, "https://example.invalid", "anon", CoroutineScope(Dispatchers.Unconfined), logoutFlushTimeoutMs = timeoutMs) { e }

    @Test fun logoutFlushesFirstThenSignsOut() {
        val e = FakeDev45Engine(); e.inner.session.userId = "u1"
        AccountState { dev45Sync(e) }.also { it.logout(); runBlocking { it.syncSignOut?.join() } }
        assertEquals(listOf("flushNow", "signOut"), e.log)
    }

    @Test fun logoutSignsOutEvenWhenTheFlushFails() {
        val e = FakeDev45Engine(); e.inner.session.userId = "u1"; e.failFlushNow = true
        e.inner.scheduler.failFlush = true
        AccountState { dev45Sync(e) }.also { it.logout(); runBlocking { it.syncSignOut?.join() } }
        assertEquals("signOut", e.log.last()); assertEquals(1, e.inner.session.signOutCalls)
    }

    @Test fun logoutSignsOutAfterTheFlushTimesOut() {
        val e = FakeDev45Engine(); e.inner.session.userId = "u1"; e.afterFlush = null // never reports Synced
        val started = System.nanoTime()
        AccountState { dev45Sync(e, timeoutMs = 200) }.also { it.logout(); runBlocking { it.syncSignOut?.join() } }
        val tookMs = (System.nanoTime() - started) / 1_000_000
        assertEquals(listOf("flushNow", "signOut"), e.log)
        assertTrue("waited for the timeout ($tookMs ms)", tookMs >= 150)
    }

    @Test fun logoutDoesNotWaitWhenOffline() {
        val e = FakeDev45Engine(); e.inner.session.userId = "u1"; e.afterFlush = SyncUiState.WaitingForNetwork
        val started = System.nanoTime()
        AccountState { dev45Sync(e, timeoutMs = 2_000) }.also { it.logout(); runBlocking { it.syncSignOut?.join() } }
        assertTrue((System.nanoTime() - started) / 1_000_000 < 1_500)
        assertEquals(listOf("flushNow", "signOut"), e.log)
    }
}
