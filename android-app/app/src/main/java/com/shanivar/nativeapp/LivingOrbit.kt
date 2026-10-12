package com.shanivar.nativeapp

import java.time.LocalDate

/*
 * Initiation V1, build step 5 (HANDOFF.md:103): derive the Living Orbit from actual records.
 * Accepted as designed by decision 0016, glow included. HANDOFF "Living Orbit": dim sphere; ring after
 * routine creation; three historical points (alarm configured, boundary saved, first practice attempted);
 * a second presence after a genuine return on a distinct local day or an explicit independent record.
 * Append-only history: missed days and revoked permissions never erase points. No numeric score.
 * Points are history, not readiness; readiness text lives elsewhere.
 */

enum class OrbitEventType { ROUTINE_CREATED, ALARM_CONFIGURED, BOUNDARY_SAVED, PRACTICE_ATTEMPTED, RETURN_DAY, INDEPENDENT_RECORD }
enum class OrbitSource { CONFIGURED, DEVICE_OBSERVED, SELF_REPORTED }

data class OrbitEvent(val id: String, val type: OrbitEventType, val occurrenceDate: LocalDate, val source: OrbitSource, val entityId: String? = null)

data class OrbitView(
    val ring: Boolean,
    val alarmPoint: Boolean,
    val boundaryPoint: Boolean,
    val practicePoint: Boolean,
    /** The accepted glow / second presence. */
    val secondPresence: Boolean,
) {
    /** Text carries all meaning (never colour or motion alone). */
    fun description(): String = buildList {
        add(if (ring) "Routine created" else "Routine not yet created")
        if (alarmPoint) add("wake alarm configured")
        if (boundaryPoint) add("boundary saved")
        if (practicePoint) add("first practice attempted")
        if (secondPresence) add("you returned")
    }.joinToString(", ")
}

/** Append-only event log with the HANDOFF validity rules. */
class OrbitLog(initial: List<OrbitEvent> = emptyList()) {
    private val events = initial.toMutableList()
    val all: List<OrbitEvent> get() = events.toList()

    /** Duplicates by id are ignored. Nothing is ever removed. */
    fun append(e: OrbitEvent): Boolean {
        if (events.any { it.id == e.id }) return false
        if (e.type == OrbitEventType.INDEPENDENT_RECORD && e.source != OrbitSource.SELF_REPORTED) return false
        events += e
        return true
    }

    /** A real visit on a distinct local day. Silence is not evidence; only call from an actual app open. */
    fun recordVisit(today: LocalDate): Boolean {
        val firstDay = events.minOfOrNull { it.occurrenceDate } ?: return false
        if (today <= firstDay) return false
        if (events.any { it.type == OrbitEventType.RETURN_DAY && it.occurrenceDate == today }) return false
        return append(OrbitEvent("return-$today", OrbitEventType.RETURN_DAY, today, OrbitSource.DEVICE_OBSERVED))
    }
}

object LivingOrbit {
    fun derive(events: List<OrbitEvent>): OrbitView {
        fun has(t: OrbitEventType) = events.any { it.type == t }
        val returnDays = events.filter { it.type == OrbitEventType.RETURN_DAY }.map { it.occurrenceDate }.toSet()
        return OrbitView(
            ring = has(OrbitEventType.ROUTINE_CREATED),
            alarmPoint = has(OrbitEventType.ALARM_CONFIGURED),
            boundaryPoint = has(OrbitEventType.BOUNDARY_SAVED),
            practicePoint = has(OrbitEventType.PRACTICE_ATTEMPTED),
            // Wireframe rule (app.js): independent record, or returns on more than one distinct day.
            secondPresence = has(OrbitEventType.INDEPENDENT_RECORD) || returnDays.size > 1,
        )
    }
}

/** First-week invitations: optional, dismissible, never unlock content, skipping costs nothing. */
data class FirstWeekPrompt(val id: String, val day: Int, val text: String)

class FirstWeekPrompts(private val prompts: List<FirstWeekPrompt>) {
    private val dismissed = mutableSetOf<String>()
    fun dismiss(id: String) { dismissed += id }
    fun visible(dayIndex: Int): List<FirstWeekPrompt> =
        if (dayIndex !in 1..7) emptyList() else prompts.filter { it.day <= dayIndex && it.id !in dismissed }
    /** There is no gated content: every feature is available whatever happened to the prompts. */
    fun unlocksAnything(): Boolean = false
}
