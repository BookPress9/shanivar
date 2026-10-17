package com.shanivar.nativeapp

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentReviewTest {
    @After fun reset() { ContentReviews.manifests = emptyList() }

    @Test
    fun releaseLaunchNeedsACompleteMatchingManifest() {
        val def = SadhanaCatalog.byId("natural")!!.copy(reviewManifestId = "m1")
        assertFalse(SadhanaCatalog.canLaunch(def, allowUnreviewed = false))
        assertTrue(SadhanaCatalog.canLaunch(def, allowUnreviewed = true))
        ContentReviews.manifests = listOf(ContentReviewManifest("m1", def.id, def.contentVersion, "s", "src", null, "2026-10-13"))
        assertFalse(SadhanaCatalog.canLaunch(def, false))
        ContentReviews.manifests = listOf(ContentReviewManifest("m1", def.id, "old", "s", "src", "c", "2026-10-13"))
        assertFalse(SadhanaCatalog.canLaunch(def, false))
        ContentReviews.manifests = listOf(ContentReviewManifest("m1", def.id, def.contentVersion, "s", "src", "c", "2026-10-13"))
        assertTrue(SadhanaCatalog.canLaunch(def, false))
    }
}
