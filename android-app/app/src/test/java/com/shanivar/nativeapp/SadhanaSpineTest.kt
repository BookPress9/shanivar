package com.shanivar.nativeapp

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class SadhanaSpineTest {
    private val banned = listOf("tantr", "yoga", "buddh", "vedānta", "kuṇḍalinī", "kundalini") +
        SadhanaSpine.centres.map { it.deepStudyName.lowercase() }

    @Test fun sevenCentresBaseToCrown() {
        assertEquals(listOf("Crown", "Brow", "Throat", "Chest", "Mid torso", "Lower abdomen", "Base"), SadhanaSpine.centres.map { it.plainLabel })
    }
    @Test fun plainLayerNamesNoTraditionOrSanskrit() {
        SadhanaSpine.centres.forEach { c -> banned.forEach { b ->
            assertFalse("${c.id}: $b", c.plainLabel.lowercase().contains(b) || c.plainNote.lowercase().contains(b))
        } }
    }
    @Test fun noCentreCanLaunchPracticeUntilVerified() {
        SadhanaSpine.centres.forEach { assertEquals(PracticeStatus.STUDY_ONLY, it.status); assertFalse(it.canLaunchPractice) }
        val c = SadhanaSpine.centres.first().copy(status = PracticeStatus.RELEASED)
        assertFalse("released without verified passage", c.canLaunchPractice)
    }
    @Test fun noEnergyOrHealthReadingInPlainText() {
        SadhanaSpine.centres.forEach { c -> listOf("your energy is", "blocked", "activated", "balanced").forEach { assertFalse(c.plainNote.lowercase().contains(it)) } }
    }
    @Test fun traceComesOnlyFromRecord() {
        assertEquals(20, SadhanaSpine.tracePercent(SadhanaRecord()))
        val r = SadhanaRecord().addIndependent(LocalDate.of(2026, 10, 12), 3)
        assertEquals(28, SadhanaSpine.tracePercent(r))
    }
}
