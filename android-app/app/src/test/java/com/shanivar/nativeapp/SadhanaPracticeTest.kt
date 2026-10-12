package com.shanivar.nativeapp

import org.junit.Assert.*
import org.junit.Test

class SadhanaPracticeTest {
    @Test fun researchOnlyNeverLaunchesEvenWhenUnreviewedAllowed() {
        assertTrue(SadhanaCatalog.researchOnly.isNotEmpty())
        SadhanaCatalog.researchOnly.forEach {
            assertFalse(it.id, SadhanaCatalog.canLaunch(it, allowUnreviewed = true))
            assertFalse(SadhanaCatalog.canLaunch(it.copy(reviewManifestId = "m1"), allowUnreviewed = true))
        }
    }
    @Test fun intensiveRiskNeverLaunchesEvenIfMislabelledReleased() {
        val bad = SadhanaCatalog.researchOnly.first().copy(status = PracticeStatus.RELEASED, minutes = listOf(5), reviewManifestId = "m")
        assertFalse(SadhanaCatalog.canLaunch(bad, true))
    }
    @Test fun releaseBuildNeedsReviewManifest() {
        val p = SadhanaCatalog.practices.first()
        assertFalse(SadhanaCatalog.canLaunch(p, allowUnreviewed = false))
        assertTrue(SadhanaCatalog.canLaunch(p, allowUnreviewed = true))
        assertTrue(SadhanaCatalog.canLaunch(p.copy(reviewManifestId = "m"), allowUnreviewed = false))
    }
    @Test fun studyOnlyAndQualifiedContextNeverLaunch() {
        val p = SadhanaCatalog.practices.first()
        listOf(PracticeStatus.STUDY_ONLY, PracticeStatus.QUALIFIED_CONTEXT, PracticeStatus.RESEARCH_ONLY).forEach {
            assertFalse(SadhanaCatalog.canLaunch(p.copy(status = it, reviewManifestId = "m"), true))
        }
    }
    @Test fun plainSourcesNameNoTradition() {
        val banned = listOf("tantra", "vijñāna", "bhairava", "yoga", "buddh", "vedānta", "osho", "kriya")
        SadhanaCatalog.all.forEach { d -> banned.forEach { assertFalse(d.sourcePlain, d.sourcePlain.lowercase().contains(it)) } }
    }
    @Test fun noBreathPacingLanguage() {
        SadhanaCatalog.all.forEach { d ->
            listOf("inhale", "exhale", "hold for", "count to").forEach { assertFalse(d.prompt.lowercase().contains(it)) }
        }
    }
    @Test fun durationsComeFromDefinition() {
        val p = SadhanaCatalog.byId("natural")!!
        assertTrue(SadhanaCatalog.validDuration(p, 3)); assertFalse(SadhanaCatalog.validDuration(p, 20))
    }
}
