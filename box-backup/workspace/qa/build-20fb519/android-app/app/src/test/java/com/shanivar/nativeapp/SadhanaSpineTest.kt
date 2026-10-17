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

    @Test fun thirtyThreeStagesWithFounderThresholds() {
        assertEquals(33, SadhanaSpine.STAGES)
        assertEquals(listOf(33, 29, 24, 17, 11, 5, 1), SadhanaSpine.centres.map { SadhanaSpine.threshold(it.id) })
        assertEquals(listOf(4, 5, 5, 12, 7), SadhanaSpine.Region.entries.map { r -> (1..33).count { SadhanaSpine.region(it) == r } })
    }
    @Test fun stageAdvancesOnlyOnCompletedOrSelfReportAndCaps() {
        fun a(id: String, o: SadhanaOutcome) = SadhanaAttempt(id, "natural", "v", 0, "UTC", 60, 30_000, o)
        var r = SadhanaRecord().withAttempt(a("e", SadhanaOutcome.ENDED)).withAttempt(a("i", SadhanaOutcome.INTERRUPTED))
        assertEquals(1, SadhanaSpine.stageLevel(r))
        r = r.withAttempt(a("c", SadhanaOutcome.COMPLETED)).addIndependent(LocalDate.of(2026, 10, 13), 3)
        assertEquals(3, SadhanaSpine.stageLevel(r))
        repeat(40) { r = r.addIndependent(LocalDate.of(2026, 10, 13), 3) }
        assertEquals(33, SadhanaSpine.stageLevel(r))
        assertNull(SadhanaSpine.nextCentre(33)); assertEquals("Lower abdomen", SadhanaSpine.nextCentre(1)!!.plainLabel)
    }
    @Test fun shimmerIsRandomisedNotPeriodic() {
        val src = java.io.File("src/main/java/com/shanivar/nativeapp/SadhanaTheme.kt").readText()
        val spine = src.substring(src.indexOf("fun SpineMap"), src.indexOf("fun SMeter"))
        assertTrue(spine.contains("rnd.nextInt") && spine.contains("if (!reduceMotion)"))
        assertFalse(spine.contains("RepeatMode.Reverse"))
    }

    @Test fun rootBranchesAreDeterministicFanBothWaysAndThin() {
        val a = SadhanaSpine.roots(42); assertEquals(a, SadhanaSpine.roots(42))
        assertTrue(a.isNotEmpty() && a.all { it.depth <= 3 })
        assertTrue(a.any { it.x1 < 0 } && a.any { it.x1 > 0 })
        assertTrue(a.filter { it.depth > 0 }.all { s -> a.any { p -> p.depth == s.depth - 1 } })
    }
    @Test fun rootPulseIsRandomisedNotPeriodicAndStaticUnderReducedMotion() {
        val src = java.io.File("src/main/java/com/shanivar/nativeapp/SadhanaTheme.kt").readText()
        val spine = src.substring(src.indexOf("fun SpineMap"), src.indexOf("fun SMeter"))
        assertTrue(spine.contains("3_000L + rnd.nextInt(6_000)"))
        assertTrue(spine.contains("Animatable(if (reduceMotion) 1f else 0f)"))
        val player = java.io.File("src/main/java/com/shanivar/nativeapp/SadhanaPracticeScreens.kt").readText()
        assertFalse("never during practice", player.contains("SpineMap"))
    }

    @Test fun vagalLayerMatchesFounderGeometryAndIsNonRhythmic() {
        assertEquals(7, SadhanaSpine.fireflies.size); assertEquals(8, SadhanaSpine.vagusPaths.size)
        val r = java.util.Random(1)
        val gaps = List(50) { SadhanaSpine.fireflyGapMs(r) }
        assertTrue(gaps.all { it in 1_500L..7_000L } && gaps.toSet().size > 10)
        val src = java.io.File("src/main/java/com/shanivar/nativeapp/SadhanaTheme.kt").readText()
        val spine = src.substring(src.indexOf("fun SpineMap"), src.indexOf("fun SelfCheckCard"))
        assertTrue(spine.contains("if (!reduceMotion) flies.forEachIndexed"))
        assertFalse("no reversing pulse", spine.contains("RepeatMode.Reverse"))
    }
    @Test fun selfCheckCopyMakesNoHealthClaim() {
        val banned = listOf("vagal tone", "regulat", "stimulat", "heal", "measure your", "diagnos")
        SelfCheck.entries.forEach { st -> banned.forEach { assertFalse(st.label.lowercase().contains(it)) } }
    }
}
