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

    @Test fun senderDeletesTombstonesAndUpsertsTheRest() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"access_token":"t","user":{"id":"00000000-0000-0000-0000-000000000000"}}"""))
            server.enqueue(MockResponse().setResponseCode(201)); server.enqueue(MockResponse().setResponseCode(204))
            val client = SupabaseClient(server.url("/").toString(), "anon")
            assertTrue(client.signInAnonymously())
            val box = SyncOutbox(MemDraftStore())
            box.enqueue("self_report", "s1", """{"id":"s1","local_date":"2026-10-17"}""")
            box.enqueue("self_report", "s2", SyncPayloads.tombstone("s2"))
            assertEquals(2, box.drain(client.sender()))
            server.takeRequest()
            server.takeRequest().let { assertEquals("POST", it.method); assertTrue(it.path!!.startsWith("/rest/v1/self_reports?on_conflict")) }
            server.takeRequest().let { assertEquals("DELETE", it.method); assertEquals("/rest/v1/self_reports?id=eq.s2", it.path) }
        }
    }
}
