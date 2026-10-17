package com.shanivar.nativeapp

import android.content.Context
import android.os.Looper
import android.util.Log
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.shanivar.nativeapp.sync.DisabledSync
import com.shanivar.nativeapp.sync.GuestResult
import com.shanivar.nativeapp.sync.SyncEngine
import com.shanivar.nativeapp.sync.SyncEngines
import com.shanivar.nativeapp.sync.SyncUiState
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

/** Pure-JVM DEV-45 follow-ups: head-of-line retry limit and the D6 parked status. */
class SyncHeadOfLineTest {
    private fun server(vararg routes: Pair<String, () -> MockResponse>): MockWebServer = MockWebServer().apply {
        dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                routes.firstOrNull { request.path!!.startsWith(it.first) }?.second?.invoke() ?: MockResponse().setResponseCode(201)
        }
        start()
    }

    @Test fun persistent404AtTheHeadDoesNotBlockTheItemsBehindItAndIsDeadLetteredAtTheLimit() {
        val s = server("/rest/v1/practice_attempts" to { MockResponse().setResponseCode(404).setBody("""{"code":"PGRST205"}""") })
        s.use {
            val rig = SyncRig(s)
            rig.outbox.enqueue("attempt", "a1", """{"id":"a1"}""")
            rig.outbox.enqueue("self_report", "s1", """{"id":"s1"}""")
            rig.outbox.enqueue("window", "w1", """{"id":"w1"}""")
            assertTrue("the head item is retried later", rig.flusher.flush() is FlushOutcome.RetryLater)
            assertEquals("the items behind the 404 were sent", listOf("attempt:a1"), rig.outbox.pending.map { it.key })
            val paths = generateSequence { s.takeRequest(100, java.util.concurrent.TimeUnit.MILLISECONDS) }.map { it.path!!.substringBefore("?") }.toList()
            assertEquals(listOf("/rest/v1/practice_attempts", "/rest/v1/self_reports", "/rest/v1/protection_windows"), paths)
            repeat(SyncFlushLimits.max - 2) { rig.flusher.flush() }
            assertEquals(SyncFlushLimits.max - 1, rig.outbox.pending.single().failures)
            assertEquals(FlushOutcome.Done(0, 1, 0), rig.flusher.flush())
            assertTrue(rig.outbox.pending.isEmpty())
            assertEquals(DeadLetter("attempt:a1", 404, OutboxFlusher.DEAD_RETRY_LIMIT, rig.clock.ms), rig.outbox.deadLetters.single())
            assertEquals("exactly MAX_ITEM_RETRIES sends of the head item", 2 + SyncFlushLimits.max, s.requestCount)
        }
    }

    @Test fun serverWideFailureStillStopsTheQueueAndNeverDeadLetters() {
        val s = server("/rest/v1/" to { MockResponse().setResponseCode(503) })
        s.use {
            val rig = SyncRig(s)
            rig.outbox.enqueue("attempt", "a1", "{}"); rig.outbox.enqueue("self_report", "s1", "{}")
            repeat(10) { assertTrue(rig.flusher.flush() is FlushOutcome.RetryLater) }
            assertEquals("one request per flush: later items would fail the same way", 10, s.requestCount)
            assertEquals(2, rig.outbox.pending.size); assertTrue(rig.outbox.deadLetters.isEmpty())
            assertEquals(0, rig.outbox.pending.first().failures)
        }
    }

    @Test fun reEnqueueResetsTheFailureCountAndItIsPersisted() {
        val store = MemDraftStore()
        val box = SyncOutbox(store)
        box.enqueue("attempt", "a1", "{}")
        assertEquals(1, box.recordFailure(box.pending.single())); assertEquals(2, box.recordFailure(box.pending.single()))
        assertEquals(2, SyncOutbox(store).pending.single().failures)
        box.enqueue("attempt", "a1", """{"v":2}""")
        assertEquals(0, box.pending.single().failures)
        assertEquals(0, SyncOutbox(store).pending.single().failures)
    }

    @Test fun routineAtTheRetryLimitIsParkedNotDeadLetteredAndNoLongerSent() {
        val s = server("/rest/v1/rpc/put_routine" to { MockResponse().setResponseCode(408) })
        s.use {
            val rig = SyncRig(s)
            rig.outbox.enqueue("routine", "current", """{"id":"current","schema_version":3,"plan":{"wakeTime":"06:00"}}""")
            repeat(SyncFlushLimits.max) { rig.flusher.flush() }
            assertEquals(OutboxFlusher.PARK_RETRY_LIMIT, rig.outbox.pending.single().parked)
            assertTrue(rig.outbox.deadLetters.isEmpty())
            val sent = s.requestCount
            assertEquals(FlushOutcome.Done(0, 0, 0), rig.flusher.flush())
            assertEquals("a retry-limit park is not re-sent until the routine changes", sent, s.requestCount)
            assertEquals(1, rig.flusher.parkedCount()); assertEquals(0, rig.flusher.sendableCount())
        }
    }

    @Test fun neverSyncedWhileARoutineIsParkedD6() {
        val s = server("/rest/v1/rpc/put_routine" to { MockResponse().setResponseCode(404).setBody("""{"code":"PGRST202"}""") })
        s.use {
            val rig = SyncRig(s)
            var online = true
            val tracker = SyncStatusTracker(MemDraftStore("1760700000000"), { rig.session.userId != null },
                { rig.flusher.sendableCount() }, { rig.outbox.deadLetters.size }, { online }, { rig.flusher.parkedCount() })
            assertTrue("nothing queued: Synced", tracker.state.value is SyncUiState.Synced)
            rig.outbox.enqueue("routine", "current", """{"id":"current","schema_version":3,"plan":{}}""")
            rig.outbox.enqueue("attempt", "a1", "{}")
            tracker.refresh()
            assertEquals(2, tracker.pendingCount.value); assertEquals(0, tracker.parkedCount.value)
            assertEquals(FlushOutcome.Done(1, 0, 1), rig.flusher.flush())
            tracker.markSynced(rig.clock.ms)
            assertEquals(0, tracker.pendingCount.value); assertEquals(1, tracker.parkedCount.value)
            assertEquals("online with a parked routine: not Synced ('Saved on this device')", SyncUiState.LocalOnly, tracker.state.value)
            online = false; tracker.refresh()
            assertEquals("offline with a parked routine ('Will sync when online')", SyncUiState.WaitingForNetwork, tracker.state.value)
        }
    }
}

/** Shorthand so the tests read with the production limit. */
object SyncFlushLimits { val max get() = OutboxFlusher.MAX_ITEM_RETRIES }

/** Robolectric DEV-45 follow-ups: lazy factory off the main thread, contract surface without reflection. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncFactoryLazyTest {
    @get:org.junit.Rule val outboxReset = AppSyncOutboxReset()
    private lateinit var ctx: Context
    private lateinit var server: MockWebServer

    @Before fun setUp() {
        ctx = RuntimeEnvironment.getApplication()
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx,
            Configuration.Builder().setMinimumLoggingLevel(Log.WARN).setExecutor(SynchronousExecutor()).build())
        server = MockWebServer().apply { start() }
    }
    @After fun tearDown() { server.shutdown(); Dev45Sync.current = null }

    /** Records which threads decrypt / read the outbox. */
    private class Probe {
        val threads = ConcurrentLinkedQueue<Thread>()
        val cipher = object : TokenCipher {
            val x = XorCipher()
            override fun encrypt(plain: ByteArray) = x.encrypt(plain)
            override fun decrypt(sealed: ByteArray): ByteArray { threads += Thread.currentThread(); return x.decrypt(sealed) }
        }
    }

    @Test fun buildOnTheMainThreadDoesNoKeystoreOrOutboxIoThere() {
        assertTrue("test runs on the main looper", Looper.getMainLooper().isCurrentThread)
        val main = Thread.currentThread()
        val dir = File(ctx.filesDir, "sync").apply { mkdirs() }
        EncryptedSessionStore(FileDraftStore(File(dir, "session.enc")), XorCipher()).save(StoredSession(UID_A, "acc-1", "ref-1", 4_000_000_000_000L))
        val probe = Probe()
        val queued = ConcurrentLinkedQueue<Runnable>()
        val e = Dev45SyncEngineFactory.build(ctx, server.url("/").toString(), "anon", cipher = probe.cipher,
            outbox = { probe.threads += Thread.currentThread(); SyncOutbox(MemDraftStore()) }, io = { queued += it })
        assertTrue("build returned before any decrypt / outbox read", probe.threads.isEmpty())
        assertFalse(e.isReady); assertNull(e.session.userId)
        assertEquals(SyncUiState.LocalOnly, e.status.state.value)
        e.scheduler.requestFlush()                                     // main-safe before ready

        val io = Thread { queued.forEach { it.run() } }.apply { name = "test-io"; start(); join() }
        assertTrue(e.isReady)
        assertEquals("decrypt + outbox read both ran", 2, probe.threads.size)
        assertTrue("never on the main thread", probe.threads.none { it === main }); assertTrue(probe.threads.all { it === io })
        assertEquals(UID_A, e.session.userId)
        assertEquals("restored session: no network call", GuestResult.Ok(UID_A), runBlocking { e.session.ensureGuest() })
        assertEquals(0, server.requestCount)
    }

    @Test fun defaultBuildRunsOnTheIoDispatcher() {
        val main = Thread.currentThread(); val probe = Probe()
        val dir = File(ctx.filesDir, "sync").apply { mkdirs() }
        EncryptedSessionStore(FileDraftStore(File(dir, "session.enc")), XorCipher()).save(StoredSession(UID_A, "acc-1", "ref-1", 4_000_000_000_000L))
        val e = Dev45SyncEngineFactory.build(ctx, server.url("/").toString(), "anon", cipher = probe.cipher,
            outbox = { probe.threads += Thread.currentThread(); SyncOutbox(MemDraftStore()) }).awaitReady()
        assertTrue(probe.threads.isNotEmpty() && probe.threads.none { it === main })
        assertEquals(UID_A, e.session.userId)
    }

    @Test fun contractSurfaceNeedsNoReflection() {
        assertSame(Dev45SyncEngineFactory, SyncEngines.dev45)
        server.enqueue(authReply()); server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"invalid_grant"}"""))
        // create() uses the Keystore cipher (absent in Robolectric); the same build with the test cipher
        val e: SyncEngine = Dev45SyncEngineFactory.build(ctx, server.url("/").toString(), "anon", cipher = XorCipher()).awaitReady()
        assertFalse(e.session.sessionLost)
        assertEquals(GuestResult.Ok(UID_A), runBlocking { e.session.ensureGuest() })
        e.scheduler.flushNow()
        assertEquals(0, e.status.pendingCount.value); assertEquals(0, e.status.parkedCount.value)
        (e as Dev45SyncEngine).outbox.enqueue("attempt", "a1", "{}")
        assertEquals(1, e.status.pendingCount.value)
        runBlocking { e.reset() }
        assertNull(e.session.userId); assertEquals(0, e.status.pendingCount.value)
        assertFalse(File(ctx.filesDir, "sync/session.enc").exists())
        // sync off keeps the same surface
        assertFalse(DisabledSync.sessionLost); DisabledSync.flushNow(); runBlocking { DisabledSync.reset() }
        assertEquals(0, DisabledSync.parkedCount.value)
    }
}
