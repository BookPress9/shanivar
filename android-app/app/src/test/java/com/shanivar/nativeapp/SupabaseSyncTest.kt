package com.shanivar.nativeapp

import com.shanivar.nativeapp.sync.GuestResult
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.LocalDate

class SupabaseSyncTest {
    @Test fun payloadsAndTableMapping() {
        assertEquals("practice_attempts", SupabaseTables.forKind("attempt"))
        assertNull(SupabaseTables.forKind("unknown"))
        val j = SyncPayloads.selfReport(IndependentPracticeRecord("s\"1", LocalDate.of(2026, 10, 13), 3, 5))
        assertTrue(j.contains("\"id\":\"s\\\"1\"") && j.contains("\"actual_minutes\":5"))
    }

    private fun get(rig: SyncRig, path: String): Pair<Int, String> {
        val t = (rig.session.accessToken() as TokenResult.Valid).token
        return rig.http.call("GET", path, null, t).let { it.code to it.body }
    }

    /**
     * DEV-45 AC5/AC8 E2E against a LOCAL Supabase only (-PsupabaseE2eUrl / -PsupabaseE2eAnonKey):
     * guest once per install, push through the engine, 3 sends = 1 row, crash-after-ack resend, tombstone
     * removes the row, an older write is ignored, a second guest sees nothing.
     */
    @Test fun e2eGuestPushReadBack() {
        val url = System.getProperty("supabase.e2e.url"); val anon = System.getProperty("supabase.e2e.anon")
        assumeTrue("local Supabase not configured", !url.isNullOrBlank() && !anon.isNullOrBlank())
        val rig = SyncRig(url!!, anon!!, signedInAs = null, clock = FakeNow(System.currentTimeMillis()))
        val guest = rig.session.ensureGuestBlocking()
        assertTrue("anonymous guest sign-in", guest is GuestResult.Ok)
        val uid = (guest as GuestResult.Ok).userId
        // once per install: a restart over the same encrypted store reuses the user, no new sign-up
        val restarted = SupabaseSession(rig.http, rig.encrypted)
        assertEquals(GuestResult.Ok(uid), restarted.ensureGuestBlocking())

        val box = rig.outbox
        val a = SadhanaAttempt("e2e-a1", "natural", "v1", 1_760_300_000_000, "Europe/Berlin", 60, 60_000, SadhanaOutcome.COMPLETED, 1_760_300_060_000)
        box.enqueue("attempt", a.id, SyncPayloads.attempt(a))
        box.enqueue("self_report", "e2e-s1", SyncPayloads.selfReport(IndependentPracticeRecord("e2e-s1", LocalDate.of(2026, 10, 13), 3, 4)))
        val chk = Stage1Check("e2e-s1c", "e2e-a1", LocalDate.of(2026, 10, 14), Stage1Reach.PARTLY, 3, setOf(DriftReason.THOUGHTS, DriftReason.SOUNDS), "test")
        box.enqueue("self_report", chk.id, Stage1.payload(chk))
        assertEquals(FlushOutcome.Done(3, 0, 0), rig.flusher.flush())
        assertTrue(box.pending.isEmpty())

        // 3 sends give 1 row; the third is a crash-after-ack resend (sent again without a local ack)
        box.enqueue("attempt", a.id, SyncPayloads.attempt(a)); rig.flusher.flush()
        box.enqueue("attempt", a.id, SyncPayloads.attempt(a))
        val again = box.snapshot().single()
        assertEquals(SendResult.Acked, rig.sender.send(again, (rig.session.accessToken() as TokenResult.Valid).token))
        assertEquals("crash after ack: the resend is harmless", FlushOutcome.Done(1, 0, 0), rig.flusher.flush())
        val (code, rows) = get(rig, "/rest/v1/practice_attempts?select=id,outcome,client_updated_at")
        assertEquals(200, code)
        assertEquals(1, Regex("e2e-a1").findAll(rows).count())
        assertTrue(rows.contains("COMPLETED") && rows.contains("client_updated_at\":\"20"))
        assertTrue(get(rig, "/rest/v1/self_reports?select=id,actual_minutes").second.contains("\"actual_minutes\":4"))
        val s1 = get(rig, "/rest/v1/self_reports?id=eq.e2e-s1c&select=kind,reached_spine,drift_count,drift_reasons,evidence_type").second
        assertTrue(s1, s1.contains("stage1_check") && s1.contains("partly") && s1.contains("\"drift_count\":3") && s1.contains("thoughts") && s1.contains("self_report"))

        // last writer wins: an older client time is acked but ignored
        rig.clock.ms -= 3_600_000
        box.enqueue("self_report", "e2e-s1", SyncPayloads.selfReport(IndependentPracticeRecord("e2e-s1", LocalDate.of(2026, 10, 13), 3, 9)))
        assertEquals(FlushOutcome.Done(1, 0, 0), rig.flusher.flush())
        assertTrue(get(rig, "/rest/v1/self_reports?id=eq.e2e-s1&select=actual_minutes").second.contains("\"actual_minutes\":4"))
        rig.clock.ms += 7_200_000

        // a tombstone after an upsert ends with no row, and a late older upsert does not resurrect it
        box.enqueue("self_report", "e2e-s1", SyncPayloads.tombstone("e2e-s1"))
        assertEquals(FlushOutcome.Done(1, 0, 0), rig.flusher.flush())
        assertEquals("[]", get(rig, "/rest/v1/self_reports?id=eq.e2e-s1&select=id").second.trim())
        rig.clock.ms -= 60_000
        box.enqueue("self_report", "e2e-s1", SyncPayloads.selfReport(IndependentPracticeRecord("e2e-s1", LocalDate.of(2026, 10, 13), 3, 4)))
        assertEquals(FlushOutcome.Done(1, 0, 0), rig.flusher.flush())
        assertEquals("[]", get(rig, "/rest/v1/self_reports?id=eq.e2e-s1&select=id").second.trim())

        // routine: put_routine does not exist yet (DEV-52) -> parked, never dead-lettered
        box.enqueue("routine", "current", """{"id":"current","schema_version":1,"plan":{"wakeTime":"06:00"}}""")
        assertEquals(FlushOutcome.Done(0, 0, 1), rig.flusher.flush())
        assertEquals(SupabaseOutboxSender.PARK_RPC_MISSING, box.pending.single().parked)
        assertTrue(box.deadLetters.isEmpty())

        // RLS: a second guest sees none of the first guest's rows
        val other = SyncRig(url, anon, signedInAs = null)
        assertTrue(other.session.ensureGuestBlocking() is GuestResult.Ok)
        assertNotEquals(uid, other.session.userId)
        assertEquals("[]", get(other, "/rest/v1/practice_attempts?select=id").second.trim())
        assertEquals("[]", get(other, "/rest/v1/tombstones?select=id").second.trim())
    }
}
