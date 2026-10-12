package com.shanivar.nativeapp

/*
 * Sadhana V1, build step S4 (HANDOFF "Living Spine architecture"; decision 0017 as amended 12 Oct 2026).
 * Three separate layers:
 *  1. ANATOMICAL_ORIENTATION: a stylised vertical body reference only; not a scan.
 *  2. TRADITION_MAP: centre records tied to a named map and source. Names and attribution appear ONLY in
 *     deep study. Outside it, waypoints use plain labels (Base ... Crown).
 *  3. PRACTICE_TRACE: derived only from the factual SadhanaRecord; never energy, blockage or health.
 * Every centre is STUDY_ONLY until source passages are verified, so none can launch a practice.
 */

enum class SpineLayer { ANATOMICAL_ORIENTATION, TRADITION_MAP, PRACTICE_TRACE }
enum class SourceRelationship { DIRECT, ADAPTATION, MODERN }

data class SourceRecord(
    val tradition: String, val work: String, val passage: String?, val edition: String?, val url: String?,
    val accessDepth: String, val relationship: SourceRelationship, val limitations: String,
) { val passageVerified: Boolean get() = passage != null && edition != null }

data class SpineCentre(
    val id: String,
    /** Plain label used everywhere outside deep study. */
    val plainLabel: String,
    /** Plain explanatory line (no tradition or Sanskrit name) used outside deep study. */
    val plainNote: String,
    val toneArgb: Long,
    val mapId: String,
    /** Deep study only. */
    val deepStudyName: String,
    /** Deep study only (verbatim wireframe text). */
    val deepStudyText: String,
    val source: SourceRecord,
    val status: PracticeStatus = PracticeStatus.STUDY_ONLY,
    val claimLimits: String = "Not anatomy. Not an energy reading. No diagnosis.",
) {
    /** Centre-specific practice requires a released status AND a verified passage. Currently never true. */
    val canLaunchPractice: Boolean get() = status == PracticeStatus.RELEASED && source.passageVerified
}

object SadhanaSpine {
    const val MAP_ID = "contemporary-seven-centre"
    private val pending = SourceRecord("Contemporary seven-centre teaching map", "Modern synthesis; historical systems differ",
        null, null, null, "overview", SourceRelationship.MODERN, "Exact source passages still need verification.")

    /** Top to bottom, as drawn. Deep-study text verbatim from app.js CHAKRAS. */
    val centres: List<SpineCentre> = listOf(
        SpineCentre("sahasrara", "Crown", "Often presented above the crown in modern seven-centre maps. Meanings and status vary by system.", 0xFFD7CCFF, MAP_ID,
            "Sahasrāra", "Often presented above the crown in modern seven-centre maps. Meanings and status vary by system.", pending),
        SpineCentre("ajna", "Brow", "A subtle centre described differently across sources; not a neurological structure.", 0xFF9F96DF, MAP_ID,
            "Ājñā", "A subtle centre described differently across tantric and later yoga sources; not a neurological structure.", pending),
        SpineCentre("vishuddha", "Throat", "A study waypoint in this interface. Voice, thyroid or health claims require separate evidence and are not inferred.", 0xFF76B5CF, MAP_ID,
            "Viśuddha", "A study waypoint in this interface. Voice, thyroid or health claims require separate evidence and are not inferred.", pending),
        SpineCentre("anahata", "Chest", "Traditional associations differ across texts. The app does not read emotion, virtue or blockage from this point.", 0xFF83B884, MAP_ID,
            "Anāhata", "Traditional associations differ across texts. The app does not read emotion, virtue or blockage from this point.", pending),
        SpineCentre("manipura", "Mid torso", "Shown as a symbolic centre in one common contemporary map, not as a measured organ or energy reading.", 0xFFD8B66D, MAP_ID,
            "Maṇipūra", "Shown as a symbolic centre in one common contemporary map, not as a measured organ or energy reading.", pending),
        SpineCentre("svadhisthana", "Lower abdomen", "Historical placement and interpretation are system-specific. No sexuality, fertility or personality diagnosis is made.", 0xFFD88A58, MAP_ID,
            "Svādhiṣṭhāna", "Historical placement and interpretation are system-specific. No sexuality, fertility or personality diagnosis is made.", pending),
        SpineCentre("muladhara", "Base", "The lower waypoint of this teaching map. It does not establish safety, grounding or readiness.", 0xFFB85F54, MAP_ID,
            "Mūlādhāra", "The lower waypoint of this teaching map. It does not establish safety, grounding or kuṇḍalinī readiness.", pending),
    )

    fun byId(id: String) = centres.firstOrNull { it.id == id } ?: centres.last()

    /** Practice-trace layer: wireframe meter min(100, 20 + attempts*12 + independent*8), from the record only. */
    fun tracePercent(r: SadhanaRecord): Int = minOf(100, 20 + r.attemptCount * 12 + r.independentCount * 8)
}
