package com.shanivar.nativeapp

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

/** DEV-45 AC4/AC6/AC9: every error class against MockWebServer. */
class SyncEngineErrorClassTest {
    private fun SyncRig.three() { outbox.enqueue("attempt", "a1", """{"id":"a1"}"""); outbox.enqueue("self_report", "s1", """{"id":"s1"}"""); outbox.enqueue("attempt", "a2", """{"id":"a2"}""") }

    private fun retryClass(vararg responses: MockResponse, readTimeoutMs: Int = 15_000): Pair<FlushOutcome, SyncRig> = MockWebServer().use { server ->
        responses.forEach(server::enqueue)
        val rig = SyncRig(server, readTimeoutMs = readTimeoutMs); rig.three()
        val o = rig.flusher.flush()
        assertEquals("nothing lost", listOf("attempt:a1", "self_report:s1", "attempt:a2"), rig.outbox.pending.map { it.key })
        assertTrue(rig.outbox.deadLetters.isEmpty())
        val reached = generateSequence { server.takeRequest(50, TimeUnit.MILLISECONDS) }.count { it.path != null }
        assertTrue("a retryable failure ends the run (backoff): only the first item was tried", reached <= 1)
        o to rig
    }

    @Test fun ioErrorRetries() {
        // connection refused (no network / server unreachable)
        val dead = MockWebServer().apply { start() }; val url = dead.url("/").toString(); dead.shutdown()
        val rig = SyncRig(url); rig.three()
        assertEquals(FlushOutcome.RetryLater(null, 0), rig.flusher.flush())
        assertEquals(3, rig.outbox.pending.size); assertTrue(rig.outbox.deadLetters.isEmpty())
        // a dropped connection after the request reached the server (lost ack) is covered by
        // SyncOutboxEngineTest.airplaneModeTogglesMidFlushLoseNothing
    }
    @Test fun timeoutRetries() { assertEquals(FlushOutcome.RetryLater(null, 0), retryClass(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE), readTimeoutMs = 300).first) }
    @Test fun serverErrorRetries() {
        assertEquals(FlushOutcome.RetryLater(null, 0), retryClass(MockResponse().setResponseCode(500)).first)
        assertEquals(FlushOutcome.RetryLater(30_000, 0), retryClass(MockResponse().setResponseCode(503).setHeader("Retry-After", "30")).first)
    }
    @Test fun tooManyRequestsRespectsRetryAfterSeconds() {
        assertEquals(FlushOutcome.RetryLater(120_000, 0), retryClass(MockResponse().setResponseCode(429).setHeader("Retry-After", "120")).first)
    }
    @Test fun tooManyRequestsRespectsRetryAfterHttpDate() {
        val now = FakeNow().ms
        val date = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(java.time.Instant.ofEpochMilli(now + 90_000).atZone(java.time.ZoneOffset.UTC))
        assertEquals(90_000L, RetryAfter.parseMs(date, now))
        assertEquals(5_000L, RetryAfter.parseMs(" 5 ", now)); assertNull(RetryAfter.parseMs("soon", now)); assertNull(RetryAfter.parseMs(null, now))
        assertEquals(0L, RetryAfter.parseMs("-3", now))
    }
    @Test fun missingTableRetriesInsteadOfDropping() = MockWebServer().use { server ->
        // follow-up: a 404 is item-level, so the rest of the queue is still sent (head-of-line fix); the 404 item stays queued
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"code":"42P01"}""")); repeat(2) { server.enqueue(MockResponse().setResponseCode(201)) }
        val rig = SyncRig(server); rig.three()
        assertEquals(FlushOutcome.RetryLater(null, 2), rig.flusher.flush())
        assertEquals(listOf("attempt:a1"), rig.outbox.pending.map { it.key }); assertEquals(1, rig.outbox.pending.single().failures)
        assertTrue(rig.outbox.deadLetters.isEmpty())
    }

    @Test fun unauthorizedRefreshesThenRetriesOnce() = MockWebServer().use { server ->
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(authReply(access = "acc-2", refresh = "ref-2"))
        server.enqueue(MockResponse().setResponseCode(201))
        val rig = SyncRig(server); rig.outbox.enqueue("attempt", "a1", """{"id":"a1"}""")
        assertEquals(FlushOutcome.Done(1, 0, 0), rig.flusher.flush())
        assertEquals("Bearer acc-1", server.takeRequest().getHeader("Authorization"))
        assertEquals("/auth/v1/token?grant_type=refresh_token", server.takeRequest().path)
        server.takeRequest().let { assertEquals("Bearer acc-2", it.getHeader("Authorization")); assertEquals("anon", it.getHeader("apikey")) }
        assertTrue(rig.outbox.pending.isEmpty())
    }

    @Test fun unauthorizedTwiceStopsAfterOneRefresh() = MockWebServer().use { server ->
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(authReply(access = "acc-2", refresh = "ref-2"))
        server.enqueue(MockResponse().setResponseCode(401))
        val rig = SyncRig(server); rig.three()
        assertEquals(FlushOutcome.RetryLater(null, 0), rig.flusher.flush())
        assertEquals(3, server.requestCount)
        assertEquals(3, rig.outbox.pending.size); assertTrue(rig.outbox.deadLetters.isEmpty())
    }

    @Test fun rejectedItemsAreDeadLetteredAndTheRestContinue() {
        for (status in listOf(400, 403, 409, 422)) MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(status).setBody("""{"code":"23514","message":"violates check constraint","details":"Failing row contains (secret-note)"}"""))
            server.enqueue(MockResponse().setResponseCode(201)); server.enqueue(MockResponse().setResponseCode(201))
            val rig = SyncRig(server); rig.three()
            assertEquals("status $status", FlushOutcome.Done(2, 1, 0), rig.flusher.flush())
            assertTrue(rig.outbox.pending.isEmpty())
            val d = rig.outbox.deadLetters.single()
            assertEquals(DeadLetter("attempt:a1", status, "23514", rig.clock.ms), d)
            assertFalse("no payload kept", rig.outboxStore.value!!.contains("secret-note"))
            assertEquals(3, server.requestCount)
        }
    }

    @Test fun deadLetterWithoutPostgrestCodeUsesHttpCode() {
        assertEquals(SendResult.Rejected(413, "http_413"), SupabaseOutboxSender.classify(HttpReply(413, "<html>too large</html>")))
        assertEquals(SendResult.Rejected(400, "http_400"), SupabaseOutboxSender.classify(HttpReply(400, """{"code":"has spaces; drop"}""")))
        assertEquals(SendResult.Acked, SupabaseOutboxSender.classify(HttpReply(204, "")))
        assertEquals(SendResult.Unauthorized, SupabaseOutboxSender.classify(HttpReply(401, "")))
        assertEquals(SendResult.Retry(7_000, 502), SupabaseOutboxSender.classify(HttpReply(502, "", 7_000)))
    }

    @Test fun unknownKindIsDeadLetteredWithoutARequest() = MockWebServer().use { server ->
        server.enqueue(MockResponse().setResponseCode(201))
        val rig = SyncRig(server)
        rig.outbox.enqueue("mystery", "m1", """{"id":"m1"}"""); rig.outbox.enqueue("attempt", "a1", """{"id":"a1"}""")
        assertEquals(FlushOutcome.Done(1, 1, 0), rig.flusher.flush())
        assertEquals(DeadLetter("mystery:m1", 0, "unknown_kind", rig.clock.ms), rig.outbox.deadLetters.single())
        assertEquals(1, server.requestCount); assertTrue(server.takeRequest().path!!.startsWith("/rest/v1/practice_attempts"))
    }

    @Test fun upsertCarriesClientUpdatedAtAndNeverUserId() = MockWebServer().use { server ->
        server.enqueue(MockResponse().setResponseCode(201))
        val rig = SyncRig(server)
        rig.outbox.enqueue("attempt", "a1", """{"id":"a1","active_ms":5}""")
        rig.flusher.flush()
        val r = server.takeRequest()
        assertEquals("/rest/v1/practice_attempts?on_conflict=user_id,id", r.path)
        assertEquals("resolution=merge-duplicates,return=minimal", r.getHeader("Prefer"))
        assertEquals("""{"id":"a1","active_ms":5,"client_updated_at":"2025-10-17T11:20:00Z"}""", r.body.readUtf8())
    }

    @Test fun itemsOfAnotherUserAreNeverSentAndUnownedOnesAreStamped() = MockWebServer().use { server ->
        server.enqueue(MockResponse().setResponseCode(201))
        val rig = SyncRig(server, signedInAs = UID_A)
        rig.outbox.ownerProvider = { UID_B }; rig.outbox.enqueue("attempt", "b1", """{"id":"b1"}""")
        rig.outbox.ownerProvider = { null }; rig.outbox.enqueue("attempt", "n1", """{"id":"n1"}""")
        assertEquals(FlushOutcome.Done(1, 0, 0), rig.flusher.flush())
        assertTrue(server.takeRequest().body.readUtf8().startsWith("""{"id":"n1""""))
        assertEquals(1, server.requestCount)
        assertEquals(UID_B, rig.outbox.pending.single().ownerUserId)
        assertEquals(1, server.requestCount)
    }

    @Test fun noSessionSendsNothing() = MockWebServer().use { server ->
        val rig = SyncRig(server, signedInAs = null); rig.three()
        assertEquals(FlushOutcome.NoSession, rig.flusher.flush())
        assertEquals(0, server.requestCount); assertEquals(3, rig.outbox.pending.size)
    }

    @Test fun perKeyOrderingNewerVersionEnqueuedDuringSendStaysQueued() = MockWebServer().use { server ->
        lateinit var rig: SyncRig
        server.dispatcher = object : Dispatcher() {
            var first = true
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (first) { first = false; rig.outbox.enqueue("attempt", "a1", """{"id":"a1","v":2}""") }
                return MockResponse().setResponseCode(201)
            }
        }
        rig = SyncRig(server)
        rig.outbox.enqueue("attempt", "a1", """{"id":"a1","v":1}""")
        assertEquals(FlushOutcome.Done(1, 0, 0), rig.flusher.flush())
        assertEquals("""{"id":"a1","v":2}""", rig.outbox.pending.single().payload)
        rig.flusher.flush()
        assertTrue(rig.outbox.pending.isEmpty())
        assertTrue(server.takeRequest().body.readUtf8().contains("\"v\":1")); assertTrue(server.takeRequest().body.readUtf8().contains("\"v\":2"))
    }

    // ── Routine (agreed: through the outbox, put_routine, parked until DEV-52) ─────────────────
    private val routinePayload = """{"id":"current","schema_version":3,"plan":{"wakeTime":"06:00","windows":[{"id":"w"}]}}"""

    @Test fun routineParksWhilePutRoutineIsMissingAndNeverDeadLetters() = MockWebServer().use { server ->
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"code":"PGRST202","message":"Could not find the function public.put_routine"}"""))
        server.enqueue(MockResponse().setResponseCode(201))
        val rig = SyncRig(server)
        rig.outbox.enqueue("routine", "current", routinePayload); rig.outbox.enqueue("attempt", "a1", """{"id":"a1"}""")
        assertEquals(FlushOutcome.Done(1, 0, 1), rig.flusher.flush())
        assertEquals(SupabaseOutboxSender.PARK_RPC_MISSING, rig.outbox.pending.single().parked)
        assertTrue(rig.outbox.deadLetters.isEmpty())
        assertEquals("parked routine is not 'sendable' for the status line", 0, rig.flusher.sendableCount())
        server.takeRequest().let {
            assertEquals("/rest/v1/rpc/put_routine", it.path)
            val b = it.body.readUtf8()
            assertTrue(b, b.startsWith("""{"p_plan":{"wakeTime":"06:00","windows":[{"id":"w"}]},"p_schema_version":3,"p_time_zone":"Europe/Berlin","p_expected_revision":0,"p_idempotency_key":""""))
        }
        // still tried on the next flush (it starts working as soon as DEV-52 ships)
        server.enqueue(MockResponse().setResponseCode(404))
        rig.flusher.flush(); assertEquals(3, server.requestCount)
    }

    @Test fun routineSavedStoresServerRevisionAndSendsItNextTime() = MockWebServer().use { server ->
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"revision":4,"plan":{}}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"revision":5,"plan":{}}"""))
        val rig = SyncRig(server)
        rig.outbox.enqueue("routine", "current", routinePayload)
        val key1 = rig.outbox.pending.single().idemKey!!
        rig.outbox.enqueue("routine", "current", routinePayload)
        assertEquals("identical payload keeps the idempotency key", key1, rig.outbox.pending.single().idemKey)
        rig.flusher.flush(); assertEquals(4, rig.routine.serverRevision)
        rig.outbox.enqueue("routine", "current", routinePayload.replace("06:00", "06:30"))
        assertNotEquals(key1, rig.outbox.pending.single().idemKey)
        rig.flusher.flush(); assertEquals(5, rig.routine.serverRevision)
        assertTrue(server.takeRequest().body.readUtf8().contains("\"p_expected_revision\":0"))
        assertTrue(server.takeRequest().body.readUtf8().contains("\"p_expected_revision\":4"))
    }

    @Test fun routineConflictParksAndPublishesTheCanonicalRow() = MockWebServer().use { server ->
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"code":"revision_conflict","current_revision":7,"current_plan":{"wakeTime":"05:45","note":"a \"} b"}}"""))
        val rig = SyncRig(server)
        rig.outbox.enqueue("routine", "current", routinePayload)
        assertEquals(FlushOutcome.Done(0, 0, 1), rig.flusher.flush())
        val c = RoutineConflictInfo(7, """{"wakeTime":"05:45","note":"a \"} b"}""")
        assertEquals(listOf(c), rig.conflicts); assertEquals(c, rig.routine.conflict)
        assertEquals(SupabaseOutboxSender.PARK_CONFLICT, rig.outbox.pending.single().parked)
        assertTrue(rig.outbox.deadLetters.isEmpty())
        assertEquals("a conflict-parked item is not resent until resolved", FlushOutcome.Done(0, 0, 0), rig.flusher.flush())
        assertEquals(1, server.requestCount)
        // keepMine: requeue against the canonical revision with a new key
        val k = rig.outbox.pending.single().idemKey
        rig.routine.serverRevision = c.revision; rig.routine.conflict = null; rig.outbox.requeue("routine:current")
        assertNull(rig.outbox.pending.single().parked); assertNotEquals(k, rig.outbox.pending.single().idemKey)
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"revision":8}"""))
        assertEquals(FlushOutcome.Done(1, 0, 0), rig.flusher.flush())
        server.takeRequest(); assertTrue(server.takeRequest().body.readUtf8().contains("\"p_expected_revision\":7"))
        assertEquals(8, rig.routine.serverRevision)
    }

    @Test fun routineOtherRejectionsFollowTheTicketRules() = MockWebServer().use { server ->
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"code":"22023"}"""))
        val rig = SyncRig(server)
        rig.outbox.enqueue("routine", "current", routinePayload)
        assertEquals(FlushOutcome.Done(0, 1, 0), rig.flusher.flush())
        assertEquals(DeadLetter("routine:current", 422, "22023", rig.clock.ms), rig.outbox.deadLetters.single())
    }

    @Test fun deleteOfAnUnsupportedKindIsDeadLettered() = MockWebServer().use { server ->
        val rig = SyncRig(server)
        rig.outbox.enqueue("routine", "current", SyncPayloads.tombstone("current"))
        assertEquals(FlushOutcome.Done(0, 1, 0), rig.flusher.flush())
        assertEquals("delete_unsupported", rig.outbox.deadLetters.single().code)
        assertEquals(0, server.requestCount)
    }
}
