package com.shanivar.nativeapp

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

    /** Runs only with -PsupabaseE2eUrl=... -PsupabaseE2eAnonKey=... (local Supabase). Guest sign-in, push outbox, read back. */
    @Test fun e2eGuestPushReadBack() {
        val url = System.getProperty("supabase.e2e.url"); val anon = System.getProperty("supabase.e2e.anon")
        assumeTrue("local Supabase not configured", !url.isNullOrBlank() && !anon.isNullOrBlank())
        val client = SupabaseClient(url!!, anon!!)
        assertTrue("anonymous guest sign-in", client.signInAnonymously())
        val box = SyncOutbox(MemDraftStore())
        val a = SadhanaAttempt("e2e-a1", "natural", "v1", 1_760_300_000_000, "Europe/Berlin", 60, 60_000, SadhanaOutcome.COMPLETED, 1_760_300_060_000)
        box.enqueue("attempt", a.id, SyncPayloads.attempt(a))
        box.enqueue("self_report", "e2e-s1", SyncPayloads.selfReport(IndependentPracticeRecord("e2e-s1", LocalDate.of(2026, 10, 13), 3, 4)))
        val chk = Stage1Check("e2e-s1c", "e2e-a1", LocalDate.of(2026, 10, 14), Stage1Reach.PARTLY, 3, setOf(DriftReason.THOUGHTS, DriftReason.SOUNDS), "test")
        box.enqueue("self_report", chk.id, Stage1.payload(chk))
        assertEquals(3, box.drain(client.sender()))
        box.enqueue("attempt", a.id, SyncPayloads.attempt(a)) // idempotent retry
        assertEquals(1, box.drain(client.sender()))
        val (code, rows) = client.select("practice_attempts", "select=id,outcome,active_ms")
        assertEquals(200, code)
        assertEquals(1, Regex("e2e-a1").findAll(rows).count())
        assertTrue(rows.contains("COMPLETED"))
        assertTrue(client.select("self_reports", "select=id,actual_minutes").second.contains("\"actual_minutes\":4"))
        val s1 = client.select("self_reports", "id=eq.e2e-s1c&select=kind,reached_spine,drift_count,drift_reasons").second
        assertTrue(s1, s1.contains("stage1_check") && s1.contains("partly") && s1.contains("\"drift_count\":3") && s1.contains("thoughts"))
        // RLS: a second guest sees none of the first guest's rows.
        val other = SupabaseClient(url, anon); assertTrue(other.signInAnonymously())
        assertEquals("[]", other.select("practice_attempts").second.trim())
    }
}
