package com.shanivar.nativeapp

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalTime

/** DEV-38: every record-changing action queues an outbox entry (SyncOutbox.kt contract). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncOutboxGapsTest {
    @get:org.junit.Rule val outboxReset = AppSyncOutboxReset()
    private val clock = FakeSadhanaClock()
    private val outStore = MemDraftStore()
    private fun controller(initiation: PracticeAttempt? = null) =
        SadhanaController(MemDraftStore(), clock, true, initiationAttempt = { initiation }, outbox = SyncOutbox(outStore))
    private fun pending() = SyncOutbox(outStore).pending

    @Test fun recordOwnQueuesSelfReport() {
        val c = controller(); c.setOwnMinutes(7); c.recordOwn()
        val own = c.record.independent.single()
        val item = pending().single()
        assertEquals("self_report:${own.id}", item.key)
        assertEquals(SyncPayloads.selfReport(own), item.payload)
        assertTrue(item.payload.contains("\"intended_minutes\":7") && item.payload.contains("\"local_date\":\"${clock.today()}\""))
    }

    @Test fun adjustOwnActualReplacesQueuedSelfReport() {
        val c = controller(); c.setOwnMinutes(5); c.recordOwn()
        val id = c.record.independent.single().id
        c.adjustOwnActual(id, 3)
        val item = pending().single()
        assertEquals("self_report:$id", item.key)
        assertTrue(item.payload.contains("\"actual_minutes\":8"))
        assertFalse(SyncPayloads.isTombstone(item.payload))
    }

    @Test fun deleteOwnQueuesTombstoneOverPendingUpsert() {
        val c = controller(); c.recordOwn()
        val id = c.record.independent.single().id
        c.deleteOwn(id)
        assertTrue(c.record.independent.isEmpty())
        val item = pending().single()
        assertEquals("self_report:$id", item.key)
        assertEquals("""{"id":"$id","deleted":true}""", item.payload)
        assertTrue(SyncPayloads.isTombstone(item.payload))
        c.deleteOwn("missing")
        assertEquals(1, pending().size)
    }

    @Test fun onboardingImportQueuesAttemptOnce() {
        val done = PracticeAttempt("a1", "v1", 1000, 60_000, AttemptOutcome.COMPLETED, 61_000)
        val store = MemDraftStore()
        SadhanaController(store, clock, true, initiationAttempt = { done }, outbox = SyncOutbox(outStore))
        val item = pending().single()
        assertEquals("attempt:initiation-a1", item.key)
        assertTrue(item.payload.contains("\"practice_id\":\"natural\"") && item.payload.contains("\"outcome\":\"COMPLETED\""))
        SyncOutbox(outStore).drain { true }
        SadhanaController(store, clock, true, initiationAttempt = { done }, outbox = SyncOutbox(outStore))
        assertTrue("already imported: not re-queued", pending().isEmpty())
        controller(PracticeAttempt("a2", "v1", 1000, 0, AttemptOutcome.IN_PROGRESS, null))
        assertTrue("unfinished: not imported, not queued", pending().isEmpty())
    }

    @Test fun routineChangeQueuesRoutinePlan() = runBlocking {
        val box = SyncOutbox(outStore)
        val repo = SharedPrefsRoutinePlanRepository(RuntimeEnvironment.getApplication(), box)
        val plan = RoutinePlanV1(wakeTime = LocalTime.of(6, 0),
            activities = listOf(RoutineActivityV1("gym", "Gym", LocalTime.of(7, 30), 60, (0..6).toSet(), true)))
        assertTrue(repo.save(plan).isSuccess)
        assertTrue(repo.save(plan.copy(wakeTime = LocalTime.of(6, 30))).isSuccess)
        val item = pending().single()
        assertEquals("routine:current", item.key)
        assertTrue(item.payload.startsWith("""{"id":"current","schema_version":${RoutinePlanV1.SCHEMA_VERSION},"plan":{"""))
        assertTrue(item.payload.contains("06:30") && item.payload.contains("\"Gym\""))
        assertEquals("routine_plans", SupabaseTables.forKind(item.kind))
        assertTrue(repo.save(RoutinePlanV1(activities = listOf(RoutineActivityV1("a1", "", LocalTime.of(9, 0), 30, (0..6).toSet(), true)))).isFailure)
        assertEquals("invalid plan is not queued", item.payload, pending().single().payload)
    }

    /** DEV-45 contract change: a tombstone is a tombstones-table upsert carrying client_updated_at, never a bare DELETE. */
    @Test fun senderSendsTombstonesAsUpsertsAndUpsertsTheRest() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(201)); server.enqueue(MockResponse().setResponseCode(201))
            val rig = SyncRig(server)
            val box = rig.outbox
            box.enqueue("self_report", "s1", """{"id":"s1","local_date":"2026-10-17"}""")
            box.enqueue("self_report", "s2", SyncPayloads.tombstone("s2"))
            assertEquals(FlushOutcome.Done(2, 0, 0), rig.flusher.flush())
            server.takeRequest().let { assertEquals("POST", it.method); assertTrue(it.path!!.startsWith("/rest/v1/self_reports?on_conflict=user_id,id")) }
            server.takeRequest().let {
                assertEquals("POST", it.method); assertEquals("/rest/v1/tombstones?on_conflict=user_id,kind,id", it.path)
                assertTrue(it.body.readUtf8().startsWith("""{"kind":"self_report","id":"s2","client_updated_at":"""))
            }
        }
    }

    private val done7 = PracticeAttempt("a7", "v1", 1000, 60_000, AttemptOutcome.COMPLETED, 61_000)
    /** A record written by a pre-DEV-38 build: onboarding attempt imported, a normal attempt, nothing queued, no flag. */
    private fun preFixStore(): MemDraftStore {
        val store = MemDraftStore()
        val old = SadhanaController(store, clock, true, initiationAttempt = { done7 }, outbox = null)
        old.setStationary(true); old.begin(); clock.advance(5_000); old.tick(); old.end()
        assertEquals(2, SadhanaController(store, clock, true).record.attempts.size)
        assertFalse(store.value!!.contains("outboxBackfillV1"))
        return store
    }

    @Test fun backfillQueuesOnboardingAttemptAlreadyImportedByOldBuild() {
        val store = preFixStore()
        SadhanaController(store, clock, true, initiationAttempt = { done7 }, outbox = SyncOutbox(outStore))
        val item = pending().single()
        assertEquals("attempt:initiation-a7", item.key)
        assertTrue(item.payload.contains("\"id\":\"initiation-a7\"") && item.payload.contains("\"outcome\":\"COMPLETED\""))
        assertTrue(store.value!!.contains("outboxBackfillV1=done"))
    }

    @Test fun backfillRunsOnceAcrossRestarts() {
        val store = preFixStore()
        SadhanaController(store, clock, true, initiationAttempt = { done7 }, outbox = SyncOutbox(outStore))
        assertEquals(1, SyncOutbox(outStore).drain { true })
        repeat(3) { SadhanaController(store, clock, true, initiationAttempt = { done7 }, outbox = SyncOutbox(outStore)) }
        assertTrue("flag set: nothing re-queued after the drain", pending().isEmpty())
    }

    @Test fun backfillIsIdempotentByAttemptId() {
        val store = preFixStore()
        SadhanaController(store, clock, true, initiationAttempt = { done7 }, outbox = SyncOutbox(outStore))
        // Simulate a crash before the flag reached disk: the backfill runs again over the undrained queue.
        store.value = store.value!!.replace("outboxBackfillV1=done", "")
        SadhanaController(store, clock, true, initiationAttempt = { done7 }, outbox = SyncOutbox(outStore))
        assertEquals(listOf("attempt:initiation-a7"), pending().map { it.key })
        // Fresh install: import and backfill both see the attempt; still one entry.
        val fresh = MemDraftStore(); val out2 = MemDraftStore()
        SadhanaController(fresh, clock, true, initiationAttempt = { done7 }, outbox = SyncOutbox(out2))
        assertEquals(listOf("attempt:initiation-a7"), SyncOutbox(out2).pending.map { it.key })
    }

    @Test fun backfillWaitsForAnOutbox() {
        val store = preFixStore()
        SadhanaController(store, clock, true, initiationAttempt = { done7 }, outbox = null)
        assertFalse(store.value!!.contains("outboxBackfillV1"))
        SadhanaController(store, clock, true, initiationAttempt = { done7 }, outbox = SyncOutbox(outStore))
        assertEquals(listOf("attempt:initiation-a7"), pending().map { it.key })
    }

    @Test fun appSyncOutboxResetRebindsTheSingleton() {
        val app = RuntimeEnvironment.getApplication()
        val a = AppSyncOutbox.get(app)
        assertSame(a, AppSyncOutbox.get(app))
        AppSyncOutbox.reset()
        assertNotSame(a, AppSyncOutbox.get(app))
    }
}
