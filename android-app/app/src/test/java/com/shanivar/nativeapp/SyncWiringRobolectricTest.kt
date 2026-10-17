package com.shanivar.nativeapp

import com.shanivar.nativeapp.sync.AppSync
import com.shanivar.nativeapp.sync.SyncEngineFactory
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Application wired with a fake DEV-45 engine. */
class FakeEngineApplication : ShanivarApplication() {
    override val syncFlag = true
    override val supabaseUrl = "https://example.invalid"
    override val supabaseAnonKey = "test-anon"
    override fun engineFactory() = SyncEngineFactory { _, _, _ -> ENGINE }
    override fun watchForeground(onForeground: () -> Unit) { FOREGROUND = onForeground }
    companion object { val ENGINE = FakeSyncEngine(); var FOREGROUND: (() -> Unit)? = null }
}

/** Release-like: flag off even with URL and key. */
class FlagOffApplication : ShanivarApplication() {
    override val syncFlag = false
    override val supabaseUrl = "https://example.invalid"
    override val supabaseAnonKey = "test-anon"
    override fun engineFactory() = SyncEngineFactory { _, _, _ -> error("must not be built") }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncWiringRobolectricTest {
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AppSyncReset()).around(AppSyncOutboxReset())

    @Test @Config(application = FakeEngineApplication::class)
    fun applicationStartBuildsTheEngineAndStartsSync() {
        assertTrue(RuntimeEnvironment.getApplication() is FakeEngineApplication)
        // onCreate built the one AppSync from the factory and ran the start path (periodic is synchronous;
        // ensureGuest is dispatched to IO, covered inline by AppSyncTest.appStartWithNoSession...).
        assertEquals(1, FakeEngineApplication.ENGINE.scheduler.periodic)
        // The foreground hook is registered and requests a flush.
        val before = FakeEngineApplication.ENGINE.scheduler.flushRequests
        FakeEngineApplication.FOREGROUND!!.invoke()
        assertEquals(before + 1, FakeEngineApplication.ENGINE.scheduler.flushRequests)
    }

    @Test @Config(application = FlagOffApplication::class)
    fun releaseFlagOffApplicationIsDisabled() {
        assertTrue(RuntimeEnvironment.getApplication() is FlagOffApplication) // onCreate did not throw: factory never invoked
        assertFalse(AppSync.current.enabled)
    }

    @Test fun defaultApplicationIsDisabledUntilDev45BindsAnEngine() {
        assertTrue(RuntimeEnvironment.getApplication() is ShanivarApplication)
        assertFalse(AppSync.current.enabled)
    }

    @Test fun appOutboxEnqueueTriggersRequestFlush() {
        val e = FakeSyncEngine(); AppSync.install(enabledSync(e))
        val ctx = RuntimeEnvironment.getApplication()
        AppSyncOutbox.get(ctx).enqueue("attempt", "a1", "{}")
        assertEquals(1, e.scheduler.flushRequests)
    }

    @Test fun ownPracticeRecordTriggersRequestFlushThroughTheController() {
        val e = FakeSyncEngine(); AppSync.install(enabledSync(e))
        val ctx = RuntimeEnvironment.getApplication()
        val c = SadhanaController(MemDraftStore(), FakeSadhanaClock(), allowUnreviewed = true, outbox = AppSyncOutbox.get(ctx))
        val before = e.scheduler.flushRequests
        c.recordOwn()
        assertTrue(e.scheduler.flushRequests > before)
    }

    @Test fun disabledSyncEnqueueStillQueuesLocally() {
        val ctx = RuntimeEnvironment.getApplication()
        AppSyncOutbox.get(ctx).enqueue("attempt", "a1", "{}")
        assertEquals(1, AppSyncOutbox.get(ctx).pending.size)
    }

    @Test fun foregroundCounterFiresOnlyWhenTheAppComesToTheForeground() {
        var n = 0; val fc = ForegroundCounter { n++ }
        val a = org.robolectric.Robolectric.buildActivity(androidx.activity.ComponentActivity::class.java).get()
        fc.onActivityStarted(a); fc.onActivityStarted(a); assertEquals(1, n) // a second activity: still foreground
        fc.onActivityStopped(a); fc.onActivityStopped(a); fc.onActivityStarted(a); assertEquals(2, n)
    }
}
