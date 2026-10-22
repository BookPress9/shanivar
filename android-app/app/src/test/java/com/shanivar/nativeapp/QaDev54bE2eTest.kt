package com.shanivar.nativeapp

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class QaDev54bE2eTest {
    @Test fun dev54OverDev45EngineLocal() {
        val url = System.getProperty("supabase.e2e.url"); val anon = System.getProperty("supabase.e2e.anon")
        assumeTrue(!url.isNullOrBlank() && !anon.isNullOrBlank())
        val rig = SyncRig(url!!, anon!!, signedInAs = null)
        runBlocking { rig.session.ensureGuest() }
        val o = """{"id":"o1","type":"RETURN_DAY","local_date":"2026-10-17","source":"DEVICE_OBSERVED","entity_id":"e1"}"""
        rig.outbox.enqueue("orbit", "o1", o)
        rig.outbox.enqueue("self_report", "bad", """{"id":"bad","local_date":"2026-10-17","drift_reasons":["energy"]}""")
        rig.outbox.enqueue("self_report", "s1", """{"id":"s1","local_date":"2026-10-17","intended_minutes":5,"actual_minutes":5,"practice_id":"p","private_by_default":true}""")
        rig.outbox.enqueue("reflection", "x1", """{"attempt_id":"x1","usefulness":"neutral","note":"ok"}""")
        rig.outbox.enqueue("routine", "current", """{"id":"current","schema_version":3,"plan":{}}""")
        val f1 = rig.flusher.flush()
        println("QA54b flush1=$f1 pending=${rig.outbox.pending.map { it.key to it.parked }} dead=${rig.outbox.deadLetters}")
        // replay orbit (same id, changed type) -> ignore-duplicates, acked, server unchanged
        rig.clock.ms += 1000
        rig.outbox.enqueue("orbit", "o1", o.replace("RETURN_DAY", "ROUTINE_CREATED"))
        val f2a = rig.flusher.flush()
        println("QA54b flush2a(replay)=$f2a pending=${rig.outbox.pending.map { it.key }} dead=${rig.outbox.deadLetters}")
        rig.clock.ms += 1000
        rig.outbox.enqueue("orbit", "o1", SyncPayloads.tombstone("o1"))
        val f2 = rig.flusher.flush()
        println("QA54b flush2=$f2 pending=${rig.outbox.pending.map { it.key to it.parked }} dead=${rig.outbox.deadLetters}")
        rig.clock.ms += 1000
        rig.outbox.enqueue("reflection", "x1", SyncPayloads.tombstone("x1"))
        val f3 = rig.flusher.flush()
        println("QA54b flush3=$f3 pending=${rig.outbox.pending.map { it.key to it.parked }} dead=${rig.outbox.deadLetters} parked=${rig.flusher.parkedCount()} sendable=${rig.flusher.sendableCount()}")
        val tok = (rig.session.accessToken() as TokenResult.Valid).token
        for (p in listOf("orbit_events?select=id,type", "self_reports?select=id,private_by_default", "practice_reflections?select=attempt_id"))
            println("QA54b GET $p -> " + rig.http.call("GET", "/rest/v1/$p", null, tok, emptyMap()).let { "${it.code} ${it.body}" })
    }
}
