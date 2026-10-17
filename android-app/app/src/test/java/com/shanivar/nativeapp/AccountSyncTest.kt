package com.shanivar.nativeapp

import com.shanivar.nativeapp.sync.AppSync
import com.shanivar.nativeapp.sync.GuestResult
import com.shanivar.nativeapp.sync.SyncEngine
import com.shanivar.nativeapp.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    val owners = HashMap<String, String>()
    val sent = mutableListOf<Pair<String, String>>() // key to user
    override val status = FakeSyncStatus()
    override val scheduler = object : SyncScheduler {
        override fun requestFlush() {
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
        AccountState { s }.logout()
        assertEquals(1, e.inner.session.signOutCalls)
        assertFalse(s.hasSession)
        assertEquals(0, e.resetCalls) // logout keeps local records: sign-out, not reset
        assertEquals(0, e.inner.session.ensureGuestCalls) // no new guest is created by logout
    }

    @Test fun logoutUsesTheProcessAppSyncByDefault() {
        val e = FakeSyncEngine(); e.session.userId = "u1"; AppSync.install(enabledSync(e))
        AccountState().logout()
        assertEquals(1, e.session.signOutCalls)
    }

    @Test fun logoutWithSyncOffIsANoOp() {
        AccountState().logout() // process AppSync is disabled: nothing to call, nothing thrown
        assertFalse(AppSync.current.enabled)
    }

    @Test fun itemsQueuedBeforeLogoutAreNeverSentUnderTheNextGuest() {
        lateinit var engine: OwnerAwareEngine
        val box = SyncOutbox(MemDraftStore(), onEnqueue = { AppSync.current.onEnqueue() })
        engine = OwnerAwareEngine(box)
        val s = AppSync.create(true, "https://example.invalid", "anon", CoroutineScope(Dispatchers.Unconfined)) { engine }
        AppSync.install(s)

        s.onGuestChosen()                        // guest u1 via the Initiation guest step
        box.enqueue("attempt", "a1", "{}")       // offline: queued, stamped to u1
        AccountState { s }.logout()              // sign-out
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
}
