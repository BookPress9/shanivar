package com.shanivar.nativeapp

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** DEV-45 AC5 / AC8 / G1 and the outbox v2 format. */
class SyncOutboxEngineTest {
    /** G1: a flush blocked on the network never blocks enqueue (snapshot under the lock, send outside it). */
    @Test fun enqueueNeverWaitsForAnInFlightSend() = MockWebServer().use { server ->
        val inSend = CountDownLatch(1); val release = CountDownLatch(1)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse { inSend.countDown(); release.await(10, TimeUnit.SECONDS); return MockResponse().setResponseCode(201) }
        }
        val rig = SyncRig(server)
        rig.outbox.enqueue("attempt", "a1", """{"id":"a1"}""")
        val flush = Thread { rig.flusher.flush() }.apply { start() }
        assertTrue(inSend.await(5, TimeUnit.SECONDS))
        val t0 = System.nanoTime()
        rig.outbox.enqueue("self_report", "s1", """{"id":"s1"}""")   // the main-thread call site
        val waitedMs = (System.nanoTime() - t0) / 1_000_000
        assertTrue("enqueue waited $waitedMs ms on the network", waitedMs < 500)
        assertEquals(2, rig.outbox.pending.size)
        release.countDown(); flush.join(5_000)
        assertEquals(listOf("self_report:s1"), rig.outbox.pending.map { it.key })
    }

    @Test fun legacyDrainAlsoSendsOutsideTheLock() {
        val box = SyncOutbox(MemDraftStore())
        box.enqueue("attempt", "a1", "{}")
        val inSend = CountDownLatch(1); val release = CountDownLatch(1)
        val t = Thread { box.drain { inSend.countDown(); release.await(5, TimeUnit.SECONDS); true } }.apply { start() }
        assertTrue(inSend.await(5, TimeUnit.SECONDS))
        val done = CountDownLatch(1)
        Thread { box.enqueue("attempt", "a2", "{}"); done.countDown() }.start()
        assertTrue("enqueue completed while drain was sending", done.await(1, TimeUnit.SECONDS))
        release.countDown(); t.join(5_000)
        assertEquals(listOf("attempt:a2"), box.pending.map { it.key })
    }

    /** AC5: three sends of one key, one request per flush, identical body = one server row (pgTAP 06 proves the row count). */
    @Test fun threeSendsAreTheSameIdempotentUpsert() = MockWebServer().use { server ->
        repeat(3) { server.enqueue(MockResponse().setResponseCode(201)) }
        val rig = SyncRig(server)
        repeat(3) { rig.outbox.enqueue("attempt", "a1", """{"id":"a1"}"""); rig.flusher.flush() }
        val bodies = (1..3).map { server.takeRequest().let { r -> r.path to r.body.readUtf8() } }.toSet()
        assertEquals("same key, same row, same body", 1, bodies.size)
        assertEquals("/rest/v1/practice_attempts?on_conflict=user_id,id", bodies.single().first)
    }

    /** AC5: crash after the server ack but before the local removal -> resent after restart, harmlessly. */
    @Test fun crashAfterAckBeforeRemovalResendsTheSameUpsert() = MockWebServer().use { server ->
        repeat(2) { server.enqueue(MockResponse().setResponseCode(201)) }
        val rig = SyncRig(server)
        rig.outbox.enqueue("self_report", "s1", """{"id":"s1"}""")
        val item = rig.outbox.snapshot().single()
        assertEquals(SendResult.Acked, rig.sender.send(item, "acc-1"))      // server acked; process dies before ack()
        val restarted = SyncOutbox(rig.outboxStore, clock = rig.clock)        // same file after restart
        assertEquals(listOf("self_report:s1"), restarted.pending.map { it.key })
        assertEquals(item.clientUpdatedAtMs, restarted.pending.single().clientUpdatedAtMs)
        assertEquals(UID_A, restarted.pending.single().ownerUserId)
        val rig2 = OutboxFlusher(restarted, rig.session, rig.sender, rig.routine)
        assertEquals(FlushOutcome.Done(1, 0, 0), rig2.flush())
        assertEquals(server.takeRequest().body.readUtf8(), server.takeRequest().body.readUtf8())
        assertTrue(restarted.pending.isEmpty())
    }

    @Test fun tombstoneSupersedesAPendingUpsert() = MockWebServer().use { server ->
        server.enqueue(MockResponse().setResponseCode(201))
        val rig = SyncRig(server)
        rig.outbox.enqueue("self_report", "s1", """{"id":"s1"}""")
        rig.clock.ms += 1_000
        rig.outbox.enqueue("self_report", "s1", SyncPayloads.tombstone("s1"))
        assertEquals(OutboxOp.DELETE, rig.outbox.pending.single().op)
        rig.flusher.flush()
        server.takeRequest().let {
            assertEquals("/rest/v1/tombstones?on_conflict=user_id,kind,id", it.path)
            assertEquals("""{"kind":"self_report","id":"s1","client_updated_at":"2025-10-17T11:20:01Z"}""", it.body.readUtf8())
        }
        assertEquals(1, server.requestCount)
    }

    /** AC8: 7 days offline with 50 items, then reconnect: all 50 arrive. */
    @Test fun sevenDaysOfflineFiftyItemsAllArriveOnReconnect() = MockWebServer().use { server ->
        val offlineFlag = java.util.concurrent.atomic.AtomicBoolean(true)
        val got = ConcurrentHashMap.newKeySet<String>()
        server.dispatcher = object : Dispatcher() {
            // peek() decides per connection: offline = the connection drops before any request is read
            override fun peek() = if (offlineFlag.get()) MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START) else super.peek()
            override fun dispatch(request: RecordedRequest): MockResponse {
                Regex("\"id\":\"([^\"]+)\"").find(request.body.readUtf8())?.let { got += it.groupValues[1] }
                return MockResponse().setResponseCode(201)
            }
        }
        val rig = SyncRig(server)
        val backoff = SyncBackoff(random = { 0.5 })
        val sevenDays = 7L * 24 * 3_600_000
        for (i in 1..50) {
            rig.clock.ms += sevenDays / 50
            rig.outbox.enqueue(if (i % 2 == 0) "attempt" else "self_report", "r$i", """{"id":"r$i"}""")
            val o = rig.flusher.flush()
            assertTrue("$o", o is FlushOutcome.RetryLater)
            assertTrue(backoff.next((o as FlushOutcome.RetryLater).afterMs) <= 3_600_000)
        }
        assertEquals(50, rig.outbox.pending.size)
        // process restart while offline: queue is on disk
        val restarted = SyncOutbox(rig.outboxStore, clock = rig.clock)
        offlineFlag.set(false)
        val o = OutboxFlusher(restarted, rig.session, rig.sender, rig.routine).flush()
        assertEquals(FlushOutcome.Done(50, 0, 0), o)
        assertEquals((1..50).map { "r$it" }.toSet(), got)
        assertTrue(restarted.pending.isEmpty())
    }

    /** AC8: airplane-mode toggles mid-flush (before and after the server got the request) lose nothing. */
    @Test fun airplaneModeTogglesMidFlushLoseNothing() = MockWebServer().use { server ->
        val rnd = Random(45)
        val got = ConcurrentHashMap.newKeySet<String>()
        server.dispatcher = object : Dispatcher() {
            // airplane on before the request leaves the phone
            override fun peek() = synchronized(rnd) { if (rnd.nextInt(3) == 0) MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START) else super.peek() }
            override fun dispatch(request: RecordedRequest): MockResponse {
                val id = Regex("\"id\":\"([^\"]+)\"").find(request.body.readUtf8())?.groupValues?.get(1)
                id?.let { got += it }
                // airplane on after the server saved the row: the ack is lost
                return if (synchronized(rnd) { rnd.nextBoolean() }) MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)
                else MockResponse().setResponseCode(201)
            }
        }
        val rig = SyncRig(server)
        repeat(30) { rig.outbox.enqueue("attempt", "x$it", """{"id":"x$it"}""") }
        var runs = 0
        while (rig.outbox.pending.isNotEmpty() && runs < 500) {
            if (runs % 7 == 3) rig.outbox.enqueue("attempt", "late$runs", """{"id":"late$runs"}""")  // edits made mid-flight
            rig.flusher.flush(); runs++
        }
        assertTrue(rig.outbox.pending.isEmpty()); assertTrue(rig.outbox.deadLetters.isEmpty())
        val expected = (0 until 30).map { "x$it" } + (0 until runs).filter { it % 7 == 3 }.map { "late$it" }
        assertTrue("every record reached the server", got.containsAll(expected))
    }

    @Test fun backoffIsExponentialWithJitterFrom30sTo1h() {
        val lo = SyncBackoff(random = { 0.0 }); val hi = SyncBackoff(random = { 0.999999 })
        val los = (1..12).map { lo.next(null) }; val his = (1..12).map { hi.next(null) }
        assertEquals(30_000L, los.first()); assertEquals(30_000L, his.first())
        assertTrue(los.zip(his).all { (a, b) -> a in 30_000..3_600_000 && b in 30_000..3_600_000 && a <= b })
        assertEquals(1_800_000L, los.last()); assertTrue(his.last() in 3_599_000..3_600_000)
        assertTrue("jitter spreads retries", his[4] > los[4])
        assertEquals("Retry-After wins when longer", 7_200_000L, SyncBackoff(random = { 0.0 }).next(7_200_000))
        lo.reset(); assertEquals(30_000L, lo.next(null))
    }

    @Test fun legacyV1FileIsReadAsUnownedUpsertsAndTombstones() {
        val store = MemDraftStore("n=3\nk0=attempt\\:a1\np0={\"id\":\"a1\"}\nk1=self_report\\:s1\np1={\"id\":\"s1\",\"deleted\":true}\nk2=routine\\:current\np2={\"id\":\"current\"}\n")
        val box = SyncOutbox(store, clock = { 42L }, newKey = { "k-1" })
        assertEquals(listOf(
            OutboxItem("attempt", "a1", """{"id":"a1"}""", OutboxOp.UPSERT, 42L, null, null, 1L),
            OutboxItem("self_report", "s1", """{"id":"s1","deleted":true}""", OutboxOp.DELETE, 42L, null, null, 2L),
            OutboxItem("routine", "current", """{"id":"current"}""", OutboxOp.UPSERT, 42L, null, "k-1", 3L),
        ), box.pending)
        assertTrue(store.value!!.contains("v=2"))
        assertEquals(box.pending.map { it.copy(seq = 0) }, SyncOutbox(store, clock = { 99L }).pending.map { it.copy(seq = 0) })
    }

    @Test fun v2RoundTripKeepsMetadataAndDeadLetters() {
        val store = MemDraftStore(); var now = 1000L
        val box = SyncOutbox(store, clock = { now })
        box.ownerProvider = { UID_A }
        box.enqueue("attempt", "a1", """{"id":"a1"}"""); box.enqueue("routine", "current", "{}")
        box.park(box.pending.last(), SupabaseOutboxSender.PARK_RPC_MISSING)
        now = 2000L; box.enqueue("self_report", "s1", "{}"); box.deadLetter(box.pending.last(), 422, "23514|x")
        val again = SyncOutbox(store)
        assertEquals(box.pending.map { it.copy(seq = 0) }, again.pending.map { it.copy(seq = 0) })
        assertEquals(listOf(DeadLetter("self_report:s1", 422, "23514/x", 2000L)), again.deadLetters)
        assertEquals(SupabaseOutboxSender.PARK_RPC_MISSING, again.pending.last().parked)
        again.enqueue("routine", "current", "{}")
        assertNull("a new edit unparks", again.pending.last().parked)
        again.clearAll(); assertTrue(SyncOutbox(store).pending.isEmpty() && SyncOutbox(store).deadLetters.isEmpty())
    }

    @Test fun enqueueHooksRunOutsideTheLockAndNeverThrow() {
        var hook = 0; var listener = 0
        val box = SyncOutbox(MemDraftStore(), onEnqueue = { hook++; error("boom") })
        box.listener = { listener++; assertEquals(1, box.pending.size) }
        box.enqueue("attempt", "a1", "{}")
        assertEquals(1, hook); assertEquals(1, listener)
    }

    @Test fun stampOwnerOnlyTouchesUnownedItems() {
        val box = SyncOutbox(MemDraftStore())
        box.enqueue("attempt", "n", "{}"); box.ownerProvider = { UID_B }; box.enqueue("attempt", "b", "{}")
        assertEquals(1, box.stampOwner(UID_A))
        assertEquals(listOf(UID_A, UID_B), box.pending.map { it.ownerUserId })
        assertEquals(0, box.stampOwner(UID_A))
    }
}
