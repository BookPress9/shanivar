package com.shanivar.nativeapp

import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.shanivar.nativeapp.sync.GuestResult
import com.shanivar.nativeapp.sync.SyncEngine
import com.shanivar.nativeapp.sync.SyncUiState
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** DEV-45 AC3/AC9: WorkManager scheduling (Robolectric + work-testing) and the engine end to end in-process. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncWorkManagerTest {
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

    private fun wm() = WorkManager.getInstance(ctx)
    private fun infos(name: String = WorkSyncScheduler.UNIQUE) = wm().getWorkInfosForUniqueWork(name).get()
    private fun driver() = WorkManagerTestInitHelper.getTestDriver(ctx)!!
    private fun engine() = Dev45SyncEngineFactory.build(ctx, server.url("/").toString(), "anon", cipher = XorCipher())

    @Test fun requestFlushEnqueuesUniqueOutboxFlushWithConnectedConstraintAndKeepsARunningOne() {
        val s = WorkSyncScheduler(ctx, SyncBackoff(), debounceMs = 10)
        repeat(5) { s.requestFlush() }
        Thread.sleep(300)
        val w = infos().single()
        assertEquals(WorkInfo.State.ENQUEUED, w.state)
        assertEquals(NetworkType.CONNECTED, w.constraints.requiredNetworkType)
        assertTrue(w.tags.contains("outbox-flush"))
        s.requestFlush(); Thread.sleep(300)
        assertEquals("KEEP: still one unique work, same id", listOf(w.id), infos().map { it.id })
    }

    @Test fun workWaitsForNetworkThenRunsTheEngine() {
        val e = engine()
        server.enqueue(authReply()); server.enqueue(MockResponse().setResponseCode(201))
        e.outbox.enqueue("attempt", "a1", """{"id":"a1"}""")       // before any session: unowned
        assertEquals(GuestResult.Ok(UID_A), runBlocking { e.session.ensureGuest() })
        assertEquals("stamped to the first guest", UID_A, e.outbox.pending.single().ownerUserId)
        e.flushNow()
        val id = infos().single { it.state == WorkInfo.State.ENQUEUED }.id
        assertEquals("not run without network", 1, server.requestCount)
        driver().setAllConstraintsMet(id)
        assertEquals(WorkInfo.State.SUCCEEDED, wm().getWorkInfoById(id).get()!!.state)
        assertEquals(2, server.requestCount)
        server.takeRequest(); assertTrue(server.takeRequest().path!!.startsWith("/rest/v1/practice_attempts"))
        assertTrue(e.outbox.pending.isEmpty())
        assertTrue(e.status.state.value is SyncUiState.Synced)
        assertTrue("lastSyncedAt persisted", java.io.File(ctx.filesDir, "sync/last_synced_at").exists())
    }

    @Test fun retryableFailureSchedulesABackoffRunAndFlushNowResetsIt() {
        val e = engine()
        server.enqueue(authReply()); server.enqueue(MockResponse().setResponseCode(503).setHeader("Retry-After", "600"))
        runBlocking { e.session.ensureGuest() }
        e.outbox.enqueue("attempt", "a1", """{"id":"a1"}""")
        e.flushNow()
        val first = infos().single { it.state == WorkInfo.State.ENQUEUED }.id
        driver().setAllConstraintsMet(first)
        val delayed = infos().single { it.state == WorkInfo.State.ENQUEUED }
        assertNotEquals(first, delayed.id)
        assertEquals("Retry-After respected", 600_000L, delayed.initialDelayMillis)
        assertEquals(1, e.outbox.pending.size)
        // foreground / network regained: reset backoff, replace the waiting run with an immediate one
        e.flushNow()
        val now = infos().single { it.state == WorkInfo.State.ENQUEUED }
        assertNotEquals(delayed.id, now.id); assertEquals(0L, now.initialDelayMillis)
        server.enqueue(MockResponse().setResponseCode(201))
        driver().setAllConstraintsMet(now.id)
        assertTrue(e.outbox.pending.isEmpty())
    }

    @Test fun schedulePeriodicIsUniqueAndConnected() {
        val s = WorkSyncScheduler(ctx, SyncBackoff())
        s.schedulePeriodic(); s.schedulePeriodic()
        val p = infos(WorkSyncScheduler.PERIODIC).single()
        assertEquals(NetworkType.CONNECTED, p.constraints.requiredNetworkType)
        assertEquals(TimeUnit.HOURS.toMillis(6), p.periodicityInfo!!.repeatIntervalMillis)
        s.cancel(); assertTrue(infos(WorkSyncScheduler.PERIODIC).all { it.state == WorkInfo.State.CANCELLED })
    }

    @Test fun workerWithoutEngineSucceedsAndSendsNothing() {
        Dev45Sync.current = null
        WorkSyncScheduler(ctx, SyncBackoff()).flushNow()
        val id = infos().single().id
        driver().setAllConstraintsMet(id)
        assertEquals(WorkInfo.State.SUCCEEDED, wm().getWorkInfoById(id).get()!!.state)
        assertEquals(0, server.requestCount)
    }

    @Test fun engineImplementsTheAgreedContractAndResetWipes() {
        val e: SyncEngine = engine()
        assertNull(e.session.userId)
        assertEquals(SyncUiState.LocalOnly, e.status.state.value)
        server.enqueue(authReply())
        runBlocking { e.session.ensureGuest() }
        val d = e as Dev45SyncEngine
        d.outbox.enqueue("attempt", "a1", "{}")
        assertTrue(java.io.File(ctx.filesDir, "sync/session.enc").readText().let { !it.contains("acc-1") && !it.contains("ref-1") })
        runBlocking { d.reset() }
        assertNull(e.session.userId); assertTrue(d.outbox.pending.isEmpty())
        assertFalse(java.io.File(ctx.filesDir, "sync/session.enc").exists())
        assertEquals(SyncUiState.LocalOnly, e.status.state.value)
    }

    @Test fun signOutKeepsItemsForTheOldUserUnsent() {
        val e = engine()
        server.enqueue(authReply())
        runBlocking { e.session.ensureGuest() }
        e.outbox.enqueue("attempt", "a1", "{}")
        runBlocking { e.session.signOut() }
        assertEquals(UID_A, e.outbox.pending.single().ownerUserId)
        server.enqueue(authReply(uid = UID_B, access = "acc-b", refresh = "ref-b"))
        runBlocking { e.session.ensureGuest() }
        e.flushNow(); infos().filter { it.state == WorkInfo.State.ENQUEUED }.forEach { driver().setAllConstraintsMet(it.id) }
        assertEquals("only the two sign-ups, nothing sent under B", 2, server.requestCount)
        assertEquals(UID_A, e.outbox.pending.single().ownerUserId)
    }

    @Test fun deadLetterCountIsExposedForDiagnostics() {
        val e = engine()
        server.enqueue(authReply()); server.enqueue(MockResponse().setResponseCode(400).setBody("""{"code":"PGRST204"}"""))
        runBlocking { e.session.ensureGuest() }
        e.outbox.enqueue("attempt", "a1", "{}")
        e.flushNow(); driver().setAllConstraintsMet(infos().single { it.state == WorkInfo.State.ENQUEUED }.id)
        assertEquals(1, e.status.deadLetterCount.value)
        assertEquals("PGRST204", e.deadLetters.single().code)
        assertTrue("dead letters do not block Synced", e.status.state.value is SyncUiState.Synced)
    }

    @Test fun routinePayloadStripsProtectedPackagesUntilDev52() {
        val plan = RoutinePlanV1(wakeTime = LocalTime.of(6, 0), protectedPackages = setOf("com.example.social"))
        val p = SyncPayloads.routine(plan)
        assertTrue(p.startsWith("""{"id":"current","schema_version":${RoutinePlanV1.SCHEMA_VERSION},"plan":{"""))
        assertFalse(p.contains("protectedPackages")); assertFalse(p.contains("com.example.social"))
        assertTrue(p.contains("06:00"))
    }

    @Test fun conflictIsPublishedAndKeepMineRequeues() {
        val e = engine()
        server.enqueue(authReply())
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"code":"revision_conflict","current_revision":3,"current_plan":{"wakeTime":"05:00"}}"""))
        runBlocking { e.session.ensureGuest() }
        e.outbox.enqueue("routine", "current", """{"id":"current","schema_version":3,"plan":{"wakeTime":"06:00"}}""")
        e.flushNow(); driver().setAllConstraintsMet(infos().single { it.state == WorkInfo.State.ENQUEUED }.id)
        assertEquals(RoutineConflictInfo(3, """{"wakeTime":"05:00"}"""), e.routineConflict.value)
        e.keepMineRoutine()
        assertNull(e.routineConflict.value); assertNull(e.outbox.pending.single().parked)
        assertEquals(RoutineConflictInfo(3, """{"wakeTime":"05:00"}""").revision,
            RoutineSyncStore(FileDraftStore(java.io.File(ctx.filesDir, "sync/routine.properties"))).serverRevision)
    }
}
