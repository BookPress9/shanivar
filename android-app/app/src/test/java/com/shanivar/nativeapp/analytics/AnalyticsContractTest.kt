package com.shanivar.nativeapp.analytics

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.LocalDate

/** DEV-65: dictionary <-> JSON <-> doc consistency, privacy rules, and the ticket's four test cases. */
class AnalyticsContractTest {
    private fun docs(): File = generateSequence(File("").absoluteFile) { it.parentFile }.map { File(it, "docs/analytics") }.first { it.isDirectory }
    private fun log(): List<String> = File(docs(), "sample-event-log.v1.jsonl").readLines().filter { it.isNotBlank() }
    private fun accepted(lines: List<String> = log()) = lines.map { EventContract.parse(it) }.filterIsInstance<Validation.Accepted>().map { it.event }

    private val A = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"; private val B = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
    private val C = "cccccccc-cccc-4ccc-8ccc-cccccccccccc"; private val D = "dddddddd-dddd-4ddd-8ddd-dddddddddddd"
    private val E = "eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee"

    // ── One schema doc, machine-readable copy, traceability ─────────────────────────────────────
    @Test fun jsonDictionaryIsGeneratedFromTheKotlinSource() {
        assertEquals("regenerate docs/analytics/event-dictionary.v1.json from DictionaryJson.render()",
            DictionaryJson.render(), File(docs(), "event-dictionary.v1.json").readText())
    }

    @Test fun docListsEveryEventWithOwnerTriggerPropertiesAndPiiAndEveryMetric() {
        val doc = File(docs(), "EVENT_DICTIONARY.md").readText()
        for (e in EventDictionary.events) {
            val row = doc.lines().firstOrNull { it.startsWith("| `${e.name}` |") } ?: fail("doc has no row for ${e.name}") as Nothing
            assertTrue("${e.name}: owner", row.contains(e.owner.wire)); assertTrue("${e.name}: pii", row.contains(e.pii.wire))
            e.props.forEach { assertTrue("${e.name}: prop ${it.name}", row.contains(it.name)) }
        }
        Metrics.DEFINITIONS.forEach { assertTrue("doc covers metric ${it.id}", doc.contains("`${it.id}`")) }
    }

    @Test fun everyBriefMetricIsTraceableToDictionaryEvents() {
        val names = EventDictionary.events.map { it.name }.toSet()
        val ids = Metrics.DEFINITIONS.map { it.id }.toSet()
        // thesis §6 list (North Star, companions, self-report, reliability, guardrail)
        assertEquals(setOf("wiac", "wiac_sr", "activation", "time_to_first_value", "routine_completion_rate", "protection_window_effectiveness",
            "meditation_return_rate", "retention_w1_w4", "recovery_after_missed_days", "self_reported_usefulness", "technical_reliability", "less_dependence_signal"), ids)
        Metrics.DEFINITIONS.forEach { m -> assertTrue(m.id, m.events.isNotEmpty() && names.containsAll(m.events)) }
        EventDictionary.events.forEach { e -> assertTrue("${e.name} feeds a defined metric", e.feeds.all { it in ids || it == "cs_insights" } && e.feeds.isNotEmpty()) }
        // each metric's events list the metric back in feeds
        Metrics.DEFINITIONS.forEach { m -> m.events.forEach { n -> assertTrue("$n feeds ${m.id}", m.id in EventDictionary.spec(n)!!.feeds) } }
        // ticket scope: routine created / step completed / step missed / recovered
        assertTrue(names.containsAll(listOf("routine_created", "routine_step_completed", "routine_step_missed", "routine_recovered")))
    }

    // ── Privacy: no PII, no package names, no free text, no self-report values ────────────────
    @Test fun dictionaryCannotCarryFreeTextPackagesOrSelfReportValues() {
        val forbidden = Regex("package|note|text|comment|email|phone|name$|contact|answer|usefulness|score|reached|drift|sankalp|commitment|feeling")
        EventDictionary.events.flatMap { it.props }.forEach { p ->
            assertFalse("forbidden property ${p.name}", forbidden.containsMatchIn(p.name))
            if (p.type == PropType.Id) assertTrue("ids are app-generated / catalog only: ${p.name}", p.name in setOf("window_id", "step_id", "practice_id"))
        }
        assertTrue(EventDictionary.events.none { it.pii.ordinal > PiiClass.QUASI.ordinal })
        assertEquals(listOf("kind"), EventDictionary.spec("self_report_submitted")!!.props.map { it.name })
    }

    @Test fun validatorRejectsPackageNamesFreeTextAndUnknownFields() {
        val r = log().map { EventContract.parse(it) }.filterIsInstance<Validation.Rejected>()
        assertEquals(listOf(listOf("missing_tz"), listOf("local_date_mismatch"), listOf("unknown_prop:package_name"), listOf("unknown_prop:note")), r.map { it.reasons })
        val ok = log().first()
        assertEquals(listOf("unknown_field"), (EventContract.parse(ok.dropLast(1) + ",\"email\":\"x@y.z\"}") as Validation.Rejected).reasons)
        assertEquals(listOf("no_consent"), (EventContract.parse(ok, consented = false) as Validation.Rejected).reasons)
        val ivChoice = log().first { it.contains("\"choice\":\"practice\"") }
        assertTrue((EventContract.parse(ivChoice.replace("\"app_category\":\"social\"", "\"app_category\":\"com.instagram.android\"")) as Validation.Rejected).reasons.contains("invalid_prop:app_category"))
        assertTrue((EventContract.parse(ivChoice.replace("\"window_id\":\"w1\"", "\"window_id\":\"my secret window\"")) as Validation.Rejected).reasons.contains("invalid_prop:window_id"))
    }

    // ── Test case "Failure": missing timezone -> rejected ───────────────────────────────────────
    @Test fun missingOrInvalidTimezoneIsRejected() {
        val ok = log().first()
        assertTrue(EventContract.parse(ok) is Validation.Accepted)
        assertEquals(listOf("missing_tz"), (EventContract.parse(ok.replace("\"tz\":\"Asia/Kolkata\"", "\"tz\":\"\"")) as Validation.Rejected).reasons)
        assertEquals(listOf("invalid_tz"), (EventContract.parse(ok.replace("\"tz\":\"Asia/Kolkata\"", "\"tz\":\"Mars/Base\"")) as Validation.Rejected).reasons)
        assertTrue((EventContract.parse(ok.replace(Regex("\"ts_utc\":\"[^\"]+\""), "\"ts_utc\":\"2026-10-12T08:00:00\"")) as Validation.Rejected).reasons.contains("invalid_ts_utc"))
    }

    // ── Test case "Recovery": schema versioning, old events still parse ───────────────────────
    @Test fun oldSchemaEventsStillParseAndFutureOnesAreRejected() {
        val v0 = log().first { it.contains("routine_activity_completed") }
        val r = EventContract.parse(v0) as Validation.Accepted
        assertEquals(0, r.upgradedFrom); assertEquals("routine_step_completed", r.event.name)
        assertEquals("s1", r.event.str("step_id")); assertEquals("2026-10-14", r.event.str("scheduled_local_date"))
        assertEquals(EventDictionary.SCHEMA_VERSION, TinyJson.parse(EventContract.toJson(r.event)).let { (it as Map<*, *>)["schema_version"] as Long }.toInt())
        // round trip of every accepted v1 event
        accepted().forEach { e -> assertEquals(e, (EventContract.parse(EventContract.toJson(e)) as Validation.Accepted).event) }
        assertEquals(listOf("unsupported_schema_version"), (EventContract.parse(log().first().replace("\"schema_version\":1", "\"schema_version\":2")) as Validation.Rejected).reasons)
    }

    // ── Test case "Edge": same action logged by two features counts once ──────────────────────
    @Test fun sameActionFromTwoFeaturesCountsOnce() {
        val a = accepted().filter { it.userId == A && it.localDate == LocalDate.of(2026, 10, 12) }
        // intervention_choice(practice) + practice_ended(completed) share occasion A1; the choice is also logged twice (retry)
        assertEquals(2, a.count { it.name == "intervention_choice" })
        val occ = Metrics.occasions(a)
        assertEquals(1, occ.size); assertEquals(listOf("intervention_choice", "practice_ended"), occ.single().events.map { it.name })
        // fallback without occasion_id: a double tap 10 minutes apart = 1, a separate day = another
        val b = Metrics.occasions(accepted().filter { it.userId == B })
        assertEquals(listOf("2026-10-13", "2026-10-14", "2026-10-16"), b.map { it.localDate.toString() })
        // retracted completion (s3 done then undone) never counts
        assertTrue(b.none { it.first.str("step_id") == "s3" })
    }

    // ── Test case "Normal": every metric computed from the sample event log ────────────────────
    @Test fun everyMetricFromTheSampleLog() {
        val ev = accepted()
        assertEquals(58, ev.size); assertEquals(57, Metrics.dedupe(ev).size)
        assertEquals(mapOf(A to mapOf("2026-W42" to 2, "2026-W43" to 1), B to mapOf("2026-W42" to 3), C to mapOf("2026-W42" to 3), E to mapOf("2026-W42" to 1)), Metrics.wiac(ev))
        assertEquals(2.0, Metrics.wiacMedian(ev, "2026-W42")!!, 0.0)
        assertEquals(1.0, Metrics.wiacMedian(ev, "2026-W43")!!, 0.0)
        assertEquals("WIAC-SR is separate and never merged", mapOf(D to mapOf("2026-W42" to 1)), Metrics.wiacSr(ev))
        assertEquals(mapOf("distraction" to Metrics.Rate(1, 2), "routine" to Metrics.Rate(1, 1), "meditation" to Metrics.Rate(1, 1), "inquiry" to Metrics.Rate(1, 1)), Metrics.activation(ev))
        assertEquals(mapOf(A to Duration.ofHours(2), B to Duration.ofMinutes(24 * 60 + 10), C to Duration.ofMinutes(2), E to Duration.ofHours(30)), Metrics.timeToFirstValue(ev))
        assertEquals(Metrics.Rate(3, 5), Metrics.routineCompletion(ev))
        val p = Metrics.protectionEffectiveness(ev)
        assertEquals(Metrics.Rate(4, 6), p.resolvedWithAlternative); assertEquals(1.5, p.interventionsPerProtectedHour!!, 1e-9)
        assertEquals(mapOf("2026-W42" to 1.25), Metrics.interventionsPerProtectedHourByWeek(ev))
        assertEquals(Metrics.Rate(1, 2), Metrics.meditationReturn(ev))
        assertEquals(Metrics.Rate(4, 5), Metrics.retention(ev, 1)); assertEquals(Metrics.Rate(0, 5), Metrics.retention(ev, 4))
        assertEquals(Metrics.Rate(2, 4), Metrics.recovery(ev, LocalDate.of(2026, 10, 25)))
        assertEquals(1, Metrics.routineRecoveries(ev))
        assertEquals(mapOf("independent" to 1, "reflection" to 1, "stage1_check" to 1), Metrics.selfReportsByKind(ev))
        assertEquals(linkedMapOf("useful" to 2, "neutral" to 0, "uncomfortable" to 1, "not_answered" to 1),
            Metrics.usefulnessDistribution(listOf("useful", "uncomfortable", null, "useful")))
        val r = Metrics.reliability(ev)
        assertEquals(Metrics.Rate(1, 2), r.alarmOnTime); assertEquals(0.8, r.protectionActiveShare!!, 1e-9)
        assertEquals(1, r.protectionFailures); assertEquals(Metrics.Rate(8, 9), r.syncSuccess); assertEquals(1, r.permissionRevocations)
    }

    @Test fun localIsoWeekUsesTheDeviceTimeZone() {
        // C's last practice is 2026-10-19T03:30Z but 18 Oct 23:30 in New York: it belongs to ISO week 42 locally
        val last = accepted().last { it.userId == C && it.name == "practice_ended" }
        assertEquals("2026-10-19T03:30:00Z", last.tsUtc.toString()); assertEquals("2026-W42", isoWeek(last.localDate))
    }

    @Test fun selfReportsAreNeverScored() {
        // no metric reads a self-report value: the only self-report event has no value property at all,
        // and the usefulness distribution is counts per category (no mean / score)
        val withValue = log().first { it.contains("\"note\"") }
        assertTrue(EventContract.parse(withValue) is Validation.Rejected)
        assertEquals(setOf("useful", "neutral", "uncomfortable", "not_answered"), Metrics.usefulnessDistribution(emptyList()).keys)
    }
}
