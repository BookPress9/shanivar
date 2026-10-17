package com.shanivar.nativeapp

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * DEV-54 AC5: the Kotlin payload builders and the SQL schema (supabase/migrations, replayed in order) must name
 * the same columns. Fails when either side drifts: a builder key the table lacks (PostgREST would 400), or a
 * client-writable column no builder sends.
 */
class Dev54PayloadContractTest {
    /** Server-managed columns the client never sends (client_updated_at is added by the sender at send time). */
    private val serverManaged = setOf("user_id", "created_at", "updated_at", "client_updated_at", "evidence_type")

    private fun migrationsDir(): File = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "supabase/migrations") }.first { it.isDirectory }

    /** Columns per public table after replaying create table / add column / drop column in file order. */
    private fun schema(): Map<String, Set<String>> {
        val tables = mutableMapOf<String, MutableSet<String>>()
        val skip = setOf("primary", "constraint", "unique", "check", "foreign", "exclude")
        migrationsDir().listFiles { f -> f.name.endsWith(".sql") }!!.sortedBy { it.name }.forEach { f ->
            var creating: String? = null; var altering: String? = null
            f.readLines().map { it.substringBefore("--").trim() }.forEach { line ->
                Regex("""^create table (?:if not exists )?public\.(\w+)\s*\(""", RegexOption.IGNORE_CASE).find(line)?.let {
                    creating = it.groupValues[1]; tables[creating!!] = mutableSetOf(); return@forEach
                }
                if (creating != null) {
                    if (line.startsWith(")")) { creating = null; return@forEach }
                    val name = line.split(Regex("\\s+")).first().lowercase()
                    if (name.isNotEmpty() && name !in skip) tables[creating]!! += name
                    return@forEach
                }
                Regex("""^alter table (?:only )?public\.(\w+)""", RegexOption.IGNORE_CASE).find(line)?.let { altering = it.groupValues[1] }
                val t = altering ?: return@forEach
                Regex("""add column (?:if not exists )?(\w+)""", RegexOption.IGNORE_CASE).findAll(line).forEach { tables[t]?.add(it.groupValues[1]) }
                Regex("""drop column (?:if exists )?(\w+)""", RegexOption.IGNORE_CASE).findAll(line).forEach { tables[t]?.remove(it.groupValues[1]) }
                if (line.endsWith(";")) altering = null
            }
        }
        return tables
    }

    /** Top-level keys of a JSON object (no org.json in plain unit tests). */
    private fun keys(json: String): Set<String> {
        val out = mutableSetOf<String>(); var depth = 0; var i = 0; var inStr = false; var start = -1
        while (i < json.length) {
            val c = json[i]
            if (inStr) {
                if (c == '\\') i++ else if (c == '"') {
                    inStr = false
                    if (depth == 1 && start >= 0) { val k = json.substring(start, i); val j = json.indexOfFirst(i + 1) { !it.isWhitespace() }; if (j >= 0 && json[j] == ':') out += k }
                    start = -1
                }
            } else when (c) { '"' -> { inStr = true; start = i + 1 }; '{', '[' -> depth++; '}', ']' -> depth-- }
            i++
        }
        return out
    }
    private fun String.indexOfFirst(from: Int, p: (Char) -> Boolean): Int { for (k in from until length) if (p(this[k])) return k; return -1 }

    private val attempt = SadhanaAttempt("a1", "natural", "v1", 1_760_300_000_000, "Europe/Berlin", 60, 60_000, SadhanaOutcome.COMPLETED, 1_760_300_060_000,
        SadhanaAnchor.ROOM, 1, listOf(SadhanaEvent(SadhanaEventType.PRACTICE_STARTED, 1_760_300_000_000, 0), SadhanaEvent(SadhanaEventType.ANCHOR_CHANGED, 1_760_300_010_000, 10, "ROOM")))

    /** table -> payloads its builders produce (self_reports has two: independent record and Stage 1 check). */
    private fun builders(): Map<String, List<String>> = mapOf(
        "practice_attempts" to listOf(SyncPayloads.attempt(attempt)),
        "self_reports" to listOf(
            SyncPayloads.selfReport(IndependentPracticeRecord("s1", LocalDate.of(2026, 10, 13), 3, 5, "natural")),
            Stage1.payload(Stage1Check("c1", "a1", LocalDate.of(2026, 10, 14), Stage1Reach.PARTLY, 3, setOf(DriftReason.THOUGHTS), "n"))),
        "orbit_events" to listOf(SyncPayloads.orbit(OrbitEvent("o1", OrbitEventType.PRACTICE_ATTEMPTED, LocalDate.of(2026, 10, 14), OrbitSource.DEVICE_OBSERVED, "a1"))),
        "practice_reflections" to listOf(SyncPayloads.reflection(PracticeReflectionRecord("a1", Usefulness.UNCOMFORTABLE, "note", 1L))),
    )

    @Test fun builderKeysMatchSqlColumns() {
        val schema = schema()
        builders().forEach { (table, payloads) ->
            val cols = schema[table] ?: fail("no table $table in migrations") as Nothing
            val writable = cols - serverManaged
            val sent = payloads.flatMap { keys(it) }.toSet()
            payloads.forEach { p -> assertEquals("$table: builder sends columns the table lacks", emptySet<String>(), keys(p) - cols) }
            assertEquals("$table: client-writable columns no builder sends", emptySet<String>(), writable - sent)
            assertEquals("$table: builder sends server-managed columns", emptySet<String>(), sent intersect serverManaged)
        }
    }

    @Test fun parserSeesDev54Columns() {
        val s = schema()
        assertTrue(s["practice_attempts"]!!.containsAll(listOf("anchor", "anchor_changes", "events", "created_at")))
        assertTrue(s["orbit_events"]!!.containsAll(listOf("entity_id", "created_at", "local_date")))
        assertTrue("private_by_default" in s["self_reports"]!!)
        assertEquals(setOf("user_id", "attempt_id", "usefulness", "note", "client_updated_at", "created_at", "updated_at"), s["practice_reflections"])
        assertFalse(s.keys.any { it.contains("self_check") })
    }

    @Test fun payloadValues() {
        val a = SyncPayloads.attempt(attempt)
        assertTrue(a, a.contains("\"anchor\":\"ROOM\"") && a.contains("\"anchor_changes\":1"))
        assertTrue(a, a.contains("""{"type": "ANCHOR_CHANGED", "at": "2025-10-12T20:13:30Z", "active_seconds": 10, "detail": "ROOM"}"""))
        assertTrue(SyncPayloads.selfReport(IndependentPracticeRecord("s1", LocalDate.of(2026, 10, 13), 3)).contains("\"private_by_default\":true"))
        assertEquals("""{"id":"o1","type":"RETURN_DAY","local_date":"2026-10-14","source":"SELF_REPORTED","entity_id":null}""",
            SyncPayloads.orbit(OrbitEvent("o1", OrbitEventType.RETURN_DAY, LocalDate.of(2026, 10, 14), OrbitSource.SELF_REPORTED)))
        assertEquals("""{"attempt_id":"a1","usefulness":"uncomfortable","note":"line1\nline2"}""".replace("\n", "\\n"),
            SyncPayloads.reflection(PracticeReflectionRecord("a1", Usefulness.UNCOMFORTABLE, "line1\nline2", 1L)))
        assertEquals("""{"attempt_id":"a1","usefulness":null,"note":null}""", SyncPayloads.reflection(PracticeReflectionRecord("a1", null, null, 1L)))
    }

    @Test fun eventsStayUnderServerCapKeepingFirstAndLatest() {
        val many = (0 until 2_000).map { SadhanaEvent(SadhanaEventType.PRACTICE_PAUSED, 1_760_300_000_000 + it * 1000L, it.toLong(), "d$it") }
        val e = SyncPayloads.events(many)
        assertTrue(e.toByteArray().size <= SyncPayloads.EVENTS_MAX_BYTES)
        assertTrue(e.contains("\"d0\"") && e.contains("\"d1999\"") && !e.contains("\"d1\""))
        assertEquals("[]", SyncPayloads.events(emptyList()))
    }

    @Test fun senderUsesPerKindConflictTargetAndAppendOnlyOrbit() {
        val server = MockWebServer(); server.start()
        try {
            val rig = SyncRig(server)
            repeat(3) { server.enqueue(MockResponse().setResponseCode(201)) }
            rig.outbox.enqueue("reflection", "a1", SyncPayloads.reflection(PracticeReflectionRecord("a1", Usefulness.USEFUL, null, 1L)))
            rig.outbox.enqueue("orbit", "o1", SyncPayloads.orbit(OrbitEvent("o1", OrbitEventType.PRACTICE_ATTEMPTED, LocalDate.of(2026, 10, 14), OrbitSource.DEVICE_OBSERVED, "a1")))
            rig.outbox.enqueue("reflection", "a2", SyncPayloads.tombstone("a2"))
            rig.flusher.flush()
            val reqs = List(3) { server.takeRequest() }.associateBy { it.path!! }
            val refl = reqs["/rest/v1/practice_reflections?on_conflict=user_id,attempt_id"]!!
            assertEquals("resolution=merge-duplicates,return=minimal", refl.getHeader("Prefer"))
            assertEquals("resolution=ignore-duplicates,return=minimal", reqs["/rest/v1/orbit_events?on_conflict=user_id,id"]!!.getHeader("Prefer"))
            assertTrue(reqs["/rest/v1/tombstones?on_conflict=user_id,kind,id"]!!.body.readUtf8().contains("\"kind\":\"reflection\""))
        } finally { server.shutdown() }
        assertFalse("orbit is append-only: no tombstone", "orbit" in SupabaseTables.tombstoneKinds)
        assertEquals("practice_reflections", SupabaseTables.forKind("reflection"))
    }
}
