package com.shanivar.nativeapp

import com.shanivar.nativeapp.sync.AppSync
import com.shanivar.nativeapp.sync.DisabledSync
import com.shanivar.nativeapp.sync.GuestResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** DEV-47 AC1/AC2/AC3/AC6 at the AppSync level. */
class AppSyncTest {
    private val engine = FakeSyncEngine()

    @Test fun releaseFlagOffMeansDisabledAndEngineNeverBuilt() {
        var built = 0
        val s = AppSync.create(false, "https://example.invalid", "anon") { built++; engine }
        assertFalse(s.enabled); assertNull(s.status); assertEquals(0, built)
        s.onAppStart(); s.onGuestChosen(); s.onForeground(); s.onEnqueue(); runBlocking { s.signOut() }
        assertEquals(0, engine.session.ensureGuestCalls); assertEquals(0, engine.scheduler.flushRequests + engine.scheduler.periodic)
    }

    @Test fun emptyUrlOrKeyOrMissingEngineMeansDisabled() {
        assertFalse(AppSync.create(true, "", "anon") { engine }.enabled)
        assertFalse(AppSync.create(true, "https://example.invalid", " ") { engine }.enabled)
        assertFalse(AppSync.create(true, "https://example.invalid", "anon") { null }.enabled) // DEV-45 engine not bound yet
        assertFalse(AppSync.create(true, "https://example.invalid", "anon") { error("boom") }.enabled)
        assertTrue(AppSync.isEnabled(true, "https://example.invalid", "anon"))
    }

    @Test fun debugBuildHasSyncFlagOn() = assertTrue(BuildConfig.SYNC_ENABLED)

    @Test fun disabledGuestResultIsDisabled() = runBlocking { assertEquals(GuestResult.Disabled, DisabledSync.ensureGuest()) }

    @Test fun appStartWithNoSessionCallsEnsureGuestOnceAndSchedulesPeriodic() {
        val s = enabledSync(engine)
        s.onAppStart()
        assertEquals(1, engine.session.ensureGuestCalls); assertEquals(1, engine.scheduler.periodic)
        assertTrue(s.hasSession)
        s.onAppStart() // session exists now: no second sign-in
        assertEquals(1, engine.session.ensureGuestCalls)
    }

    @Test fun appStartWithSessionDoesNotCallEnsureGuest() {
        engine.session.userId = "existing"
        enabledSync(engine).onAppStart()
        assertEquals(0, engine.session.ensureGuestCalls)
    }

    @Test fun offlineGuestRetriesAtNextStartAndNeverThrows() {
        engine.session.result = GuestResult.Offline
        val s = enabledSync(engine)
        s.onAppStart(); assertFalse(s.hasSession)
        engine.session.result = GuestResult.Ok("g2")
        s.onAppStart(); assertEquals(2, engine.session.ensureGuestCalls); assertTrue(s.hasSession)
    }

    @Test fun enqueueTriggersRequestFlush() {
        val s = enabledSync(engine)
        val box = SyncOutbox(MemDraftStore(), onEnqueue = s::onEnqueue)
        box.enqueue("attempt", "a1", "{}"); box.enqueue("self_report", "s1", "{}")
        assertEquals(2, engine.scheduler.flushRequests)
        assertEquals(listOf("attempt:a1", "self_report:s1"), box.pending.map { it.key })
    }

    @Test fun enqueueHookFailureNeverLosesTheItem() {
        val box = SyncOutbox(MemDraftStore(), onEnqueue = { error("scheduler down") })
        box.enqueue("attempt", "a1", "{}")
        assertEquals(1, box.pending.size)
    }

    @Test fun foregroundRequestsFlush() {
        enabledSync(engine).onForeground(); assertEquals(1, engine.scheduler.flushRequests)
    }

    @Test fun signOutClearsSessionThroughEngine() = runBlocking {
        val s = enabledSync(engine); s.onAppStart(); assertTrue(s.hasSession)
        s.signOut()
        assertEquals(1, engine.session.signOutCalls); assertFalse(s.hasSession)
    }

    @Test fun foregroundUsesDev45FlushNowWhichResetsBackoff() {
        val e = FakeDev45Engine()
        val s = AppSync.create(true, "https://example.invalid", "anon", kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)) { e }
        s.onForeground()
        assertEquals(1, e.flushNowCalls); assertEquals(0, e.inner.scheduler.flushRequests)
        s.onEnqueue() // enqueue stays the debounced requestFlush
        assertEquals(1, e.inner.scheduler.flushRequests)
        assertEquals(true, s.sessionLost)
    }

    @Test fun resetUsesDev45ResetAndClearsTheSession() = runBlocking {
        val e = FakeDev45Engine(); e.inner.session.userId = "u1"
        val s = AppSync.create(true, "https://example.invalid", "anon") { e }
        s.reset()
        assertEquals(1, e.resetCalls); assertFalse(s.hasSession)
        val plain = FakeSyncEngine(); plain.session.userId = "u2"
        enabledSync(plain).reset() // engine without reset(): falls back to sign-out
        assertEquals(1, plain.session.signOutCalls)
        AppSync.DISABLED.reset() // sync off: no-op
    }

    @Test fun noEngineOnThisBranchMeansSyncStaysOff() {
        // dev-38 base has no Dev45SyncEngineFactory; after the DEV-45 merge this resolves to it.
        val f = com.shanivar.nativeapp.sync.Dev45Bridge.factory()
        val merged = runCatching { Class.forName("com.shanivar.nativeapp.Dev45SyncEngineFactory") }.isSuccess
        assertEquals(merged, f != null)
    }
}
