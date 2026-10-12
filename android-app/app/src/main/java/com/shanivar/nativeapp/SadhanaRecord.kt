package com.shanivar.nativeapp

import java.time.LocalDate
import java.util.UUID

/*
 * Sadhana V1, build step S3 (HANDOFF "Capability and progression rules", "Domain model").
 * Factual record: attempts, active minutes and self-reports stay separate. No composite score, streak
 * or loss state. Capability evidence uses deterministic, auditable criteria (version below). These are
 * product interactions, not proof of inner ability. Missing evidence stays missing. Source reading and
 * browsing never produce evidence.
 */

enum class Usefulness(val label: String) { USEFUL("Useful"), NEUTRAL("Neutral"), UNCOMFORTABLE("Uncomfortable") }
enum class GuidanceDetail { FULL, REDUCED, MINIMAL }
enum class ChangedBy { USER, SUGGESTION }
enum class CapabilityEvidenceType { APP_OBSERVED, SELF_REPORT }

data class PracticeReflectionRecord(val attemptId: String, val usefulness: Usefulness?, val note: String?, val createdAtUtc: Long)
data class GuidancePreference(val detail: GuidanceDetail = GuidanceDetail.FULL, val changedBy: ChangedBy = ChangedBy.USER)
data class IndependentPracticeRecord(
    val id: String, val localDate: LocalDate, val intendedMinutes: Int?, val actualMinutes: Int? = null,
    val practiceId: String? = null, val privateByDefault: Boolean = true,
) { val evidenceType: String get() = "self_report" }
data class CapabilityEvidence(val capability: SadhanaCapability, val type: CapabilityEvidenceType, val recordId: String, val criteriaVersion: String)

data class SadhanaRecord(
    val attempts: List<SadhanaAttempt> = emptyList(),
    val reflections: Map<String, PracticeReflectionRecord> = emptyMap(),
    val independent: List<IndependentPracticeRecord> = emptyList(),
    val guidance: GuidancePreference = GuidancePreference(),
) {
    /** Only finished attempts with real active time (an attempt ended at 0 s leaves no record, as in the wireframe). */
    fun withAttempt(a: SadhanaAttempt): SadhanaRecord =
        if (a.outcome == SadhanaOutcome.IN_PROGRESS || (a.activeSeconds == 0L && a.outcome != SadhanaOutcome.COMPLETED)) this
        else copy(attempts = attempts.filterNot { it.id == a.id } + a)

    /** Optional; selecting nothing keeps the reflection null. Tapping again clears. */
    fun reflect(attemptId: String, u: Usefulness?, now: Long): SadhanaRecord =
        if (u == null) copy(reflections = reflections - attemptId)
        else copy(reflections = reflections + (attemptId to PracticeReflectionRecord(attemptId, u, null, now)))

    fun addIndependent(date: LocalDate, intendedMinutes: Int?, id: String = UUID.randomUUID().toString()) =
        copy(independent = independent + IndependentPracticeRecord(id, date, intendedMinutes))
    fun deleteIndependent(id: String) = copy(independent = independent.filterNot { it.id == id })
    fun editIndependent(id: String, actualMinutes: Int?) =
        copy(independent = independent.map { if (it.id == id) it.copy(actualMinutes = actualMinutes) else it })

    /** Explicit only; never silent. */
    fun setGuidance(detail: GuidanceDetail, by: ChangedBy = ChangedBy.USER) = copy(guidance = GuidancePreference(detail, by))

    val attemptCount: Int get() = attempts.size
    val activeSeconds: Long get() = attempts.sumOf { it.activeSeconds }
    /** Rounded like the wireframe's Math.round(seconds / 60). */
    val activeMinutes: Long get() = Math.round(activeSeconds / 60.0)
    val independentCount: Int get() = independent.size
}

object CapabilityCriteria {
    const val VERSION = "capability-criteria-v1"

    fun evidence(r: SadhanaRecord, routineBoundaryLinked: Boolean): List<CapabilityEvidence> = buildList {
        fun cap(id: String) = SadhanaCatalog.byId(id)?.capability
        val finished = r.attempts.filter { it.outcome == SadhanaOutcome.COMPLETED || it.outcome == SadhanaOutcome.ENDED }
        finished.firstOrNull()?.let { add(CapabilityEvidence(SadhanaCapability.ARRIVE, CapabilityEvidenceType.APP_OBSERVED, it.id, VERSION)) }
        r.attempts.firstOrNull { cap(it.practiceId) == SadhanaCapability.RETURN && it.activeSeconds > 0 }
            ?.let { add(CapabilityEvidence(SadhanaCapability.RETURN, CapabilityEvidenceType.APP_OBSERVED, it.id, VERSION)) }
        r.attempts.firstOrNull { cap(it.practiceId) == SadhanaCapability.SEE && it.outcome == SadhanaOutcome.COMPLETED }
            ?.let { add(CapabilityEvidence(SadhanaCapability.SEE, CapabilityEvidenceType.APP_OBSERVED, it.id, VERSION)) }
        if (routineBoundaryLinked) r.attempts.firstOrNull { cap(it.practiceId) == SadhanaCapability.CHOOSE && it.outcome == SadhanaOutcome.COMPLETED }
            ?.let { add(CapabilityEvidence(SadhanaCapability.CHOOSE, CapabilityEvidenceType.APP_OBSERVED, it.id, VERSION)) }
        r.independent.firstOrNull()?.let { add(CapabilityEvidence(SadhanaCapability.CARRY, CapabilityEvidenceType.SELF_REPORT, it.id, VERSION)) }
            ?: if (r.guidance.detail != GuidanceDetail.FULL && r.guidance.changedBy == ChangedBy.USER)
                add(CapabilityEvidence(SadhanaCapability.CARRY, CapabilityEvidenceType.SELF_REPORT, "guidance", VERSION)) else Unit
    }

    /** The current invitation is the first capability without evidence. All earlier practice stays open. */
    fun invitation(r: SadhanaRecord, linked: Boolean): SadhanaCapability {
        val have = evidence(r, linked).map { it.capability }.toSet()
        return SadhanaCapability.entries.firstOrNull { it !in have } ?: SadhanaCapability.CARRY
    }

    fun has(r: SadhanaRecord, linked: Boolean, c: SadhanaCapability) = evidence(r, linked).any { it.capability == c }
}

object SadhanaGuidance {
    /** Verbatim from app.js guidance(). Text only; never timed to breathing. */
    fun line(p: SadhanaPracticeDefinition, detail: GuidanceDetail, carrying: Boolean, intendedSeconds: Long, elapsed: Long): String =
        if (detail != GuidanceDetail.FULL || carrying) { if (elapsed < 12) p.prompt else "No further instruction. Return when you choose." }
        else if (elapsed < 15) p.prompt
        else if (elapsed < intendedSeconds / 2) "Attention moved. That is the moment of practice. Return without force."
        else "Let the next return be yours. Nothing needs to look special."

    /** Uncomfortable reflection suggests stopping and an alternative; never diagnoses or prescribes. */
    fun afterReflection(u: Usefulness?): String? =
        if (u == Usefulness.UNCOMFORTABLE) "Consider stopping this practice for now. Look around the room or return to an ordinary action." else null
}

/** Orbit integration: concrete evidence families only; no numeric aura; missed days remove nothing. */
fun SadhanaRecord.orbitEvents(zone: java.time.ZoneId): List<OrbitEvent> =
    attempts.map { OrbitEvent("sadhana-${it.id}", OrbitEventType.PRACTICE_ATTEMPTED,
        java.time.Instant.ofEpochMilli(it.startedAtUtc).atZone(zone).toLocalDate(), OrbitSource.DEVICE_OBSERVED, it.practiceId) } +
    independent.map { OrbitEvent("own-${it.id}", OrbitEventType.INDEPENDENT_RECORD, it.localDate, OrbitSource.SELF_REPORTED) }
