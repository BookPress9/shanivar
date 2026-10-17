package com.shanivar.nativeapp

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SadhanaRecordTest {
    private fun att(id: String, practice: String, out: SadhanaOutcome, sec: Long) =
        SadhanaAttempt(id, practice, SadhanaCatalog.VERSION, 0, "UTC", 60, sec * 1000, out)

    @Test fun emptyRecordInvitesArriveAndHasNoEvidence() {
        val r = SadhanaRecord()
        assertTrue(CapabilityCriteria.evidence(r, false).isEmpty())
        assertEquals(SadhanaCapability.ARRIVE, CapabilityCriteria.invitation(r, false))
    }
    @Test fun criteriaAreDeterministicAndOrdered() {
        var r = SadhanaRecord().withAttempt(att("1", "natural", SadhanaOutcome.ENDED, 10))
        assertEquals(SadhanaCapability.RETURN, CapabilityCriteria.invitation(r, false))
        r = r.withAttempt(att("2", "room", SadhanaOutcome.COMPLETED, 60))
        assertEquals(SadhanaCapability.SEE, CapabilityCriteria.invitation(r, false))
        r = r.withAttempt(att("3", "threshold", SadhanaOutcome.ENDED, 30))
        assertEquals("ended See practice is not completed evidence", SadhanaCapability.SEE, CapabilityCriteria.invitation(r, false))
        r = r.withAttempt(att("4", "threshold", SadhanaOutcome.COMPLETED, 180))
        r = r.withAttempt(att("5", "oneaction", SadhanaOutcome.COMPLETED, 300))
        assertEquals("Choose needs a Routine boundary link", SadhanaCapability.CHOOSE, CapabilityCriteria.invitation(r, false))
        assertEquals(SadhanaCapability.CARRY, CapabilityCriteria.invitation(r, true))
    }
    @Test fun longPracticeAloneNeverAdvancesBeyondArrive() {
        var r = SadhanaRecord()
        repeat(50) { r = r.withAttempt(att("n$it", "natural", SadhanaOutcome.COMPLETED, 300)) }
        assertEquals(SadhanaCapability.RETURN, CapabilityCriteria.invitation(r, true))
    }
    @Test fun carryFromSelfReportOrUserGuidanceChange() {
        assertTrue(CapabilityCriteria.has(SadhanaRecord().addIndependent(LocalDate.of(2026, 10, 12), 3), false, SadhanaCapability.CARRY))
        assertTrue(CapabilityCriteria.has(SadhanaRecord().setGuidance(GuidanceDetail.MINIMAL), false, SadhanaCapability.CARRY))
        assertFalse(CapabilityCriteria.has(SadhanaRecord().setGuidance(GuidanceDetail.MINIMAL, ChangedBy.SUGGESTION), false, SadhanaCapability.CARRY))
    }
    @Test fun zeroSecondEndLeavesNoRecordAndInProgressIgnored() {
        val r = SadhanaRecord().withAttempt(att("1", "natural", SadhanaOutcome.ENDED, 0)).withAttempt(att("2", "natural", SadhanaOutcome.IN_PROGRESS, 5))
        assertEquals(0, r.attemptCount)
    }
    @Test fun factsStaySeparateAndRounded() {
        val r = SadhanaRecord().withAttempt(att("1", "natural", SadhanaOutcome.COMPLETED, 60)).withAttempt(att("2", "room", SadhanaOutcome.ENDED, 100))
            .addIndependent(LocalDate.of(2026, 10, 12), 3)
        assertEquals(2, r.attemptCount); assertEquals(3L, r.activeMinutes); assertEquals(1, r.independentCount)
    }
    @Test fun selfReportsEditableAndDeletable() {
        var r = SadhanaRecord().addIndependent(LocalDate.of(2026, 10, 12), 3, "x")
        r = r.editIndependent("x", 5); assertEquals(5, r.independent.single().actualMinutes)
        r = r.deleteIndependent("x"); assertEquals(0, r.independentCount)
    }
    @Test fun reflectionOptionalAndClearable() {
        var r = SadhanaRecord().reflect("1", Usefulness.UNCOMFORTABLE, 0)
        assertNotNull(SadhanaGuidance.afterReflection(Usefulness.UNCOMFORTABLE))
        r = r.reflect("1", null, 0); assertTrue(r.reflections.isEmpty())
    }
    @Test fun guidanceReducesOnlyByPreference() {
        val p = SadhanaCatalog.byId("natural")!!
        assertEquals("Attention moved. That is the moment of practice. Return without force.", SadhanaGuidance.line(p, GuidanceDetail.FULL, false, 180, 30))
        assertEquals("No further instruction. Return when you choose.", SadhanaGuidance.line(p, GuidanceDetail.MINIMAL, false, 180, 30))
    }
    @Test fun orbitEventsFromEvidenceFamilies() {
        val r = SadhanaRecord().withAttempt(att("1", "natural", SadhanaOutcome.COMPLETED, 60)).addIndependent(LocalDate.of(2026, 10, 12), 3, "o")
        val ev = r.orbitEvents(ZoneId.of("UTC"))
        assertEquals(listOf(OrbitEventType.PRACTICE_ATTEMPTED, OrbitEventType.INDEPENDENT_RECORD), ev.map { it.type })
    }
}
