package com.shanivar.nativeapp

/**
 * B4: content review manifest. A release build may launch a practice only when a manifest exists for
 * that exact practice id and content version with all three sign-offs (safety, source, copy).
 * The registry is empty until the founder's reviews are recorded, so release launches nothing yet.
 */
data class ContentReviewManifest(
    val id: String,
    val practiceId: String,
    val contentVersion: String,
    val safetyReviewer: String?,
    val sourceReviewer: String?,
    val copyReviewer: String?,
    val reviewedOn: String?,
) {
    val complete: Boolean get() = listOf(safetyReviewer, sourceReviewer, copyReviewer, reviewedOn).all { !it.isNullOrBlank() }
}

object ContentReviews {
    var manifests: List<ContentReviewManifest> = emptyList()

    fun approves(def: SadhanaPracticeDefinition, list: List<ContentReviewManifest> = manifests): Boolean =
        list.any { it.id == def.reviewManifestId && it.practiceId == def.id && it.contentVersion == def.contentVersion && it.complete }
}
