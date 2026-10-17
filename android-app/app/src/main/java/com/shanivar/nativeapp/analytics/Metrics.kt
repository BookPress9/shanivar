package com.shanivar.nativeapp.analytics

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields

/*
 * DEV-65 reference computation of WIAC and the thesis §6 companion metrics over a validated event log.
 * This is the executable definition (docs/analytics/EVENT_DICTIONARY.md §5): any pipeline built later
 * (vendor or SQL, out of scope here) must reproduce these numbers on docs/analytics/sample-event-log.v1.jsonl.
 *
 * Rules that hold everywhere: input is Validation.Accepted events only; events are deduplicated by
 * event_id first; self-report values are never inputs (only "a report of kind X was submitted"), so no
 * metric can score a self-report; the usefulness distribution reads the user-owned records and returns
 * counts only.
 */

/** A qualifying intentional action after dedupe (one per occasion). */
data class Occasion(val userId: String, val key: String, val first: AnalyticsEvent, val events: List<AnalyticsEvent>) {
    val localDate: LocalDate get() = first.localDate
    val isoWeek: String get() = isoWeek(first.localDate)
}

fun isoWeek(d: LocalDate): String = "%d-W%02d".format(d.get(IsoFields.WEEK_BASED_YEAR), d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))

data class MetricDefinition(val id: String, val briefName: String, val events: List<String>, val otherSources: List<String> = emptyList())

object Metrics {
    /** [H] threshold to validate: an "ended" practice counts as intentional from this much active time. */
    const val ENDED_MIN_ACTIVE_SECONDS = 10
    /** [H] fallback dedupe window when an event has no occasion_id. */
    val FALLBACK_WINDOW: Duration = Duration.ofMinutes(30)
    /** Alarm counted on time when it rings within this many seconds of the scheduled time. */
    const val ALARM_ON_TIME_SECONDS = 60

    /** Thesis §6 metrics -> events (AC: every metric traceable). Checked against the dictionary by tests. */
    val DEFINITIONS = listOf(
        MetricDefinition("wiac", "North Star: Weekly Intentional Actions Completed", listOf("routine_step_completed", "routine_step_uncompleted", "practice_ended", "intervention_choice")),
        MetricDefinition("wiac_sr", "WIAC-SR: practised without the app (reported separately, never merged)", listOf("self_report_submitted")),
        MetricDefinition("activation", "Activation rate per segment", listOf("app_first_opened", "goal_selected", "intervention_choice", "routine_created", "routine_step_completed", "alarm_dismissed", "practice_started", "practice_ended", "self_report_submitted")),
        MetricDefinition("time_to_first_value", "Time to first value", listOf("app_first_opened", "routine_step_completed", "practice_ended", "intervention_choice")),
        MetricDefinition("routine_completion_rate", "Routine completion rate", listOf("routine_step_completed", "routine_step_uncompleted", "routine_step_missed")),
        MetricDefinition("protection_window_effectiveness", "Protection-window effectiveness (+ interventions per protected hour guardrail)", listOf("intervention_shown", "intervention_choice", "protection_window_ended")),
        MetricDefinition("meditation_return_rate", "Meditation return rate (second distinct local day within 7 days)", listOf("practice_started")),
        MetricDefinition("retention_w1_w4", "W1 / W4 retention (qualifying action, not an app open)", listOf("app_first_opened", "routine_step_completed", "practice_ended", "intervention_choice")),
        MetricDefinition("recovery_after_missed_days", "Recovery after missed days", listOf("routine_step_completed", "practice_ended", "intervention_choice", "routine_recovered", "app_returned_after_gap")),
        MetricDefinition("self_reported_usefulness", "Self-reported intentionality and usefulness (distribution, never a score)", listOf("self_report_submitted"),
            listOf("practice_reflections.usefulness (user-owned record, DEV-54)", "intentionality question record (Epic G, not built)")),
        MetricDefinition("technical_reliability", "Technical reliability (alarm on time, protection active while scheduled, sync)", listOf("alarm_fired", "protection_window_started", "protection_window_ended", "protection_failure_detected", "permission_state_changed", "sync_flush_completed", "sync_dead_lettered"),
            listOf("crash-free sessions: crash reporting (DEV-14), not an analytics event")),
        MetricDefinition("less_dependence_signal", "Guardrail: needs less intervention over time (interventions per protected hour with stable WIAC)", listOf("intervention_shown", "protection_window_ended", "guidance_level_changed")),
    )

    fun dedupe(events: List<AnalyticsEvent>): List<AnalyticsEvent> =
        events.distinctBy { it.eventId }.sortedWith(compareBy({ it.tsUtc }, { it.eventId }))

    fun isQualifying(e: AnalyticsEvent): Boolean = when (e.name) {
        "routine_step_completed" -> true
        "practice_ended" -> e.str("outcome") == "completed" || (e.str("outcome") == "ended" && (e.int("active_seconds") ?: 0) >= ENDED_MIN_ACTIVE_SECONDS)
        "intervention_choice" -> e.str("choice") in EventDictionary.ALTERNATIVE_CHOICES
        else -> false
    }

    private fun actionKey(e: AnalyticsEvent): String = when (e.name) {
        "routine_step_completed" -> "routine:${e.str("step_id")}"
        "practice_ended" -> "practice:${e.str("practice_id")}"
        "intervention_choice" -> "intervention:${e.str("window_id")}"
        else -> e.name
    }

    /** Completions not retracted by a later routine_step_uncompleted for the same step and date (dedupe rule 4). */
    fun liveCompletions(events: List<AnalyticsEvent>): List<AnalyticsEvent> {
        val out = ArrayList<AnalyticsEvent>()
        for (e in dedupe(events)) when (e.name) {
            "routine_step_completed" -> out += e
            "routine_step_uncompleted" -> out.lastOrNull { it.userId == e.userId && it.str("step_id") == e.str("step_id") &&
                it.str("scheduled_local_date") == e.str("scheduled_local_date") && !it.tsUtc.isAfter(e.tsUtc) }?.let { out.remove(it) }
        }
        return out
    }

    /** Dedupe rules 1-4: event_id, retractions, occasion_id once across features, fallback action_key within 30 min. */
    fun occasions(events: List<AnalyticsEvent>): List<Occasion> {
        val all = dedupe(events)
        val live = liveCompletions(all).map { it.eventId }.toSet()
        val qualifying = all.filter { isQualifying(it) && (it.name != "routine_step_completed" || it.eventId in live) }
        val byOccasion = LinkedHashMap<String, MutableList<AnalyticsEvent>>()
        val fallback = LinkedHashMap<String, Pair<Instant, MutableList<AnalyticsEvent>>>()   // user|action_key -> cluster start
        val result = ArrayList<Pair<String, MutableList<AnalyticsEvent>>>()
        for (e in qualifying) {
            val occ = e.occasionId
            if (occ != null) {
                val k = "${e.userId}|occ:$occ"
                byOccasion.getOrPut(k) { mutableListOf<AnalyticsEvent>().also { result += k to it } } += e
            } else {
                val fk = "${e.userId}|${actionKey(e)}"
                val cur = fallback[fk]
                if (cur != null && Duration.between(cur.first, e.tsUtc) <= FALLBACK_WINDOW) cur.second += e
                else { val l = mutableListOf(e); fallback[fk] = e.tsUtc to l; result += "$fk@${e.tsUtc}" to l }
            }
        }
        return result.map { (k, l) -> Occasion(l.first().userId, k, l.first(), l) }
    }

    /** WIAC: user -> local ISO week -> distinct occasions. */
    fun wiac(events: List<AnalyticsEvent>): Map<String, Map<String, Int>> =
        occasions(events).groupBy { it.userId }.mapValues { (_, l) -> l.groupingBy { it.isoWeek }.eachCount().toSortedMap() }.toSortedMap()

    /** Median WIAC per active user (any accepted event that week) for one ISO week; inactive users are not zeros. */
    fun wiacMedian(events: List<AnalyticsEvent>, week: String): Double? {
        val active = dedupe(events).filter { isoWeek(it.localDate) == week }.map { it.userId }.toSet()
        if (active.isEmpty()) return null
        val w = wiac(events)
        return median(active.map { (w[it]?.get(week) ?: 0).toDouble() })
    }

    /** WIAC-SR: independent practice self-reports per user and week (a count of reports, never their values). */
    fun wiacSr(events: List<AnalyticsEvent>): Map<String, Map<String, Int>> =
        dedupe(events).filter { it.name == "self_report_submitted" && it.str("kind") == "independent" }
            .groupBy { it.userId }.mapValues { (_, l) -> l.groupingBy { isoWeek(it.localDate) }.eachCount().toSortedMap() }.toSortedMap()

    data class Rate(val numerator: Int, val denominator: Int) { val value: Double? get() = if (denominator == 0) null else numerator.toDouble() / denominator }

    private fun firstOpen(events: List<AnalyticsEvent>) = dedupe(events).filter { it.name == "app_first_opened" }.groupBy { it.userId }.mapValues { it.value.first() }

    /** Activation per segment: the segment's first meaningful action (thesis §3) within 7 days of the first open. */
    fun activation(events: List<AnalyticsEvent>): Map<String, Rate> {
        val all = dedupe(events); val opens = firstOpen(all)
        val segment = all.filter { it.name == "goal_selected" }.groupBy { it.userId }.mapValues { it.value.first().str("segment") }
        val live = liveCompletions(all).map { it.eventId }.toSet()
        return EventDictionary.SEGMENTS.associateWith { seg ->
            val users = segment.filterValues { it == seg }.keys.filter { it in opens }
            Rate(users.count { u ->
                val t0 = opens.getValue(u).tsUtc; val end = t0.plus(Duration.ofDays(7))
                val mine = all.filter { it.userId == u && !it.tsUtc.isBefore(t0) && !it.tsUtc.isAfter(end) }
                when (seg) {
                    "distraction" -> mine.firstOrNull { it.name == "intervention_choice" }?.str("choice") in EventDictionary.ALTERNATIVE_CHOICES
                    "routine" -> mine.any { it.name == "routine_created" } &&
                        mine.any { (it.name == "routine_step_completed" && it.eventId in live) || it.name == "alarm_dismissed" }
                    "meditation" -> {
                        val first = mine.firstOrNull { it.name == "practice_ended" && it.str("outcome") == "completed" }
                        first != null && mine.any { it.name == "practice_started" && it.localDate != first.localDate && it.tsUtc.isAfter(first.tsUtc) }
                    }
                    else -> mine.any { it.name == "practice_ended" && isQualifying(it) && it.str("launched_from") == "routine" } ||
                        mine.any { it.name == "self_report_submitted" && it.str("kind") == "independent" }
                }
            }, users.size)
        }
    }

    /** Time to first value: first open -> first qualifying occasion, per user (users without one are absent). */
    fun timeToFirstValue(events: List<AnalyticsEvent>): Map<String, Duration> {
        val opens = firstOpen(events)
        return occasions(events).groupBy { it.userId }.mapNotNull { (u, l) ->
            val t0 = opens[u]?.tsUtc ?: return@mapNotNull null
            val first = l.minOf { it.first.tsUtc }
            u to Duration.between(t0, first)
        }.toMap().toSortedMap()
    }

    /** Routine completion: live completions / (live completions + missed), per (user, step, scheduled date). */
    fun routineCompletion(events: List<AnalyticsEvent>): Rate {
        val all = dedupe(events)
        fun key(e: AnalyticsEvent) = Triple(e.userId, e.str("step_id"), e.str("scheduled_local_date"))
        val done = liveCompletions(all).map(::key).toSet()
        val missed = all.filter { it.name == "routine_step_missed" }.map(::key).toSet() - done
        return Rate(done.size, done.size + missed.size)
    }

    data class Effectiveness(val resolvedWithAlternative: Rate, val interventionsPerProtectedHour: Double?)

    fun protectionEffectiveness(events: List<AnalyticsEvent>): Effectiveness {
        val all = dedupe(events)
        val shown = all.filter { it.name == "intervention_shown" }
        val resolved = all.filter { it.name == "intervention_choice" && it.str("choice") in EventDictionary.ALTERNATIVE_CHOICES }
            .distinctBy { it.occasionId ?: it.eventId }.size
        val hours = all.filter { it.name == "protection_window_ended" }.sumOf { it.int("active_minutes") ?: 0 } / 60.0
        return Effectiveness(Rate(resolved, shown.size), if (hours == 0.0) null else shown.size / hours)
    }

    /** Practice starters who start again on a different local day within 7 days of their first start. */
    fun meditationReturn(events: List<AnalyticsEvent>): Rate {
        val starts = dedupe(events).filter { it.name == "practice_started" }.groupBy { it.userId }
        return Rate(starts.count { (_, l) ->
            val f = l.first()
            l.any { it.localDate != f.localDate && ChronoUnit.DAYS.between(f.localDate, it.localDate) in 1..7 }
        }, starts.size)
    }

    /** Week N retention: a qualifying occasion on local days 7(N-1) .. 7N-1 after the first open's local date. */
    fun retention(events: List<AnalyticsEvent>, week: Int): Rate {
        val opens = firstOpen(events); val occ = occasions(events).groupBy { it.userId }
        return Rate(opens.count { (u, o) ->
            occ[u].orEmpty().any { ChronoUnit.DAYS.between(o.localDate, it.localDate) in (7L * (week - 1))..(7L * week - 1) }
        }, opens.size)
    }

    /**
     * Recovery after missed days: users with a gap of 2+ local days without a qualifying occasion (after at
     * least one) who complete one within 7 days after the gap. Gaps not yet 7 days old at [asOf] that have not
     * recovered are left out (not observable yet).
     */
    fun recovery(events: List<AnalyticsEvent>, asOf: LocalDate): Rate {
        var gapUsers = 0; var recovered = 0
        for ((_, l) in occasions(events).groupBy { it.userId }) {
            val days = l.map { it.localDate }.distinct().sorted()
            var hadGap = false; var rec = false
            for ((i, d) in days.withIndex()) {
                val next = days.getOrNull(i + 1)
                val gapEnd = d.plusDays(2)                      // d+1, d+2 empty -> gap of 2 days
                if (next == null) { if (!asOf.isBefore(gapEnd.plusDays(7))) hadGap = true; continue }
                val between = ChronoUnit.DAYS.between(d, next) - 1
                if (between >= 2) { hadGap = true; if (ChronoUnit.DAYS.between(gapEnd, next) in 1..7) rec = true }
            }
            if (hadGap) { gapUsers++; if (rec) recovered++ }
        }
        return Rate(recovered, gapUsers)
    }

    /** Routine-specific recovery count (routine_recovered events), reported next to [recovery]. */
    fun routineRecoveries(events: List<AnalyticsEvent>): Int = dedupe(events).count { it.name == "routine_recovered" }

    /**
     * Self-reported usefulness: a distribution of the user-owned practice_reflections.usefulness values
     * (useful / neutral / uncomfortable / not answered). Counts only: never a mean, a score or a per-user value,
     * never joined into WIAC. "uncomfortable" is just a category here (no escalation).
     */
    fun usefulnessDistribution(values: List<String?>): Map<String, Int> {
        val cats = listOf("useful", "neutral", "uncomfortable")
        val m = linkedMapOf<String, Int>(); cats.forEach { m[it] = 0 }; m["not_answered"] = 0
        values.forEach { v -> val k = if (v in cats) v!! else "not_answered"; m[k] = m.getValue(k) + 1 }
        return m
    }

    /** Reflection rate input: self-reports submitted per kind (no values). */
    fun selfReportsByKind(events: List<AnalyticsEvent>): Map<String, Int> =
        dedupe(events).filter { it.name == "self_report_submitted" }.groupingBy { it.str("kind")!! }.eachCount().toSortedMap()

    data class Reliability(val alarmOnTime: Rate, val protectionActiveShare: Double?, val protectionFailures: Int, val syncSuccess: Rate, val permissionRevocations: Int)

    fun reliability(events: List<AnalyticsEvent>): Reliability {
        val all = dedupe(events)
        val alarms = all.filter { it.name == "alarm_fired" }
        val ended = all.filter { it.name == "protection_window_ended" }
        val sched = ended.sumOf { it.int("scheduled_minutes") ?: 0 }
        val flushes = all.filter { it.name == "sync_flush_completed" }
        val sent = flushes.sumOf { it.int("sent") ?: 0 }; val dead = flushes.sumOf { it.int("dead_lettered") ?: 0 }
        return Reliability(
            Rate(alarms.count { kotlin.math.abs(it.int("delay_seconds") ?: Int.MAX_VALUE) <= ALARM_ON_TIME_SECONDS }, alarms.size),
            if (sched == 0) null else ended.sumOf { it.int("active_minutes") ?: 0 }.toDouble() / sched,
            all.count { it.name == "protection_failure_detected" },
            Rate(sent, sent + dead),
            all.count { it.name == "permission_state_changed" && it.bool("granted") == false },
        )
    }

    /** Guardrail series: interventions per protected hour by ISO week (read with WIAC, never alone). */
    fun interventionsPerProtectedHourByWeek(events: List<AnalyticsEvent>): Map<String, Double> {
        val all = dedupe(events)
        val shown = all.filter { it.name == "intervention_shown" }.groupingBy { isoWeek(it.localDate) }.eachCount()
        val hours = all.filter { it.name == "protection_window_ended" }.groupBy { isoWeek(it.localDate) }
            .mapValues { (_, l) -> l.sumOf { it.int("active_minutes") ?: 0 } / 60.0 }
        return hours.filterValues { it > 0 }.mapValues { (w, h) -> (shown[w] ?: 0) / h }.toSortedMap()
    }

    private fun median(v: List<Double>): Double { val s = v.sorted(); val n = s.size; return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2 }

}
