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

/** Self-check (founder approved 13 Oct). The user's own report; reflective, never diagnostic; stored locally only. */
enum class SelfCheck(val label: String, val glow: Float) {
    ACTIVATED("On edge", 0.95f), MIXED("Mixed", 0.75f), STEADY("Steady", 0.8f), LOW("Low energy", 0.45f)
}

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
    /** Deep study only (verbatim wireframe text, founder update 13 Oct). */
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
            "Sahasrāra", "Presented above the crown in many modern maps. The attached book treats it as culmination rather than an ordinary chakra.", pending),
        SpineCentre("ajna", "Brow", "A subtle centre described differently across sources; not a neurological structure.", 0xFF9F96DF, MAP_ID,
            "Ājñā", "A subtle centre described differently across tantric and later yoga sources. Rudra granthi is placed near this threshold in the attached book.", pending),
        SpineCentre("vishuddha", "Throat", "A study waypoint in this interface. Voice, thyroid or health claims require separate evidence and are not inferred.", 0xFF76B5CF, MAP_ID,
            "Viśuddha", "A study waypoint associated with the throat region in this teaching map. No thyroid, voice or health state is inferred.", pending),
        SpineCentre("anahata", "Chest", "Traditional associations differ across texts. The app does not read emotion, virtue or blockage from this point.", 0xFF83B884, MAP_ID,
            "Anāhata", "A chest-region centre in this map. Vishnu granthi is placed near this threshold. The app does not infer emotion or blockage.", pending),
        SpineCentre("manipura", "Mid torso", "Shown as a symbolic centre in one common contemporary map, not as a measured organ or energy reading.", 0xFFD8B66D, MAP_ID,
            "Maṇipūra", "A mid-torso centre in this teaching map, not a measured organ, plexus or energy reading.", pending),
        SpineCentre("svadhisthana", "Lower abdomen", "Historical placement and interpretation are system-specific. No sexuality, fertility or personality diagnosis is made.", 0xFFD88A58, MAP_ID,
            "Svādhiṣṭhāna", "A lower-centre waypoint whose placement and meaning vary by system. No sexuality, fertility or personality diagnosis is made.", pending),
        SpineCentre("muladhara", "Base", "The lower waypoint of this teaching map. It does not establish safety, grounding or readiness.", 0xFFB85F54, MAP_ID,
            "Mūlādhāra", "The base of this teaching map. Brahma granthi is associated with this region in the attached book. This does not establish kuṇḍalinī readiness.", pending),
    )

    fun byId(id: String) = centres.firstOrNull { it.id == id } ?: centres.last()

    /** 33 stages, base (1) to crown (33). Founder's updated sadhana-v1 app.js, 13 Oct 2026. */
    const val STAGES = 33

    /** Centre waypoint stage, from app.js CHAKRAS[].threshold (1, 5, 11, 17, 24, 29, 33). */
    val thresholds: Map<String, Int> = mapOf("muladhara" to 1, "svadhisthana" to 5, "manipura" to 11, "anahata" to 17,
        "vishuddha" to 24, "ajna" to 29, "sahasrara" to 33)
    fun threshold(id: String): Int = thresholds.getValue(id)

    enum class Region(val label: String) { COCCYGEAL("Co4"), SACRAL("S5"), LUMBAR("L5"), THORACIC("T12"), CERVICAL("C7") }
    /** Regions counted from the base: Co4, S5, L5, T12, C7 (4+5+5+12+7 = 33). */
    fun region(stage: Int): Region = when (stage) {
        in 1..4 -> Region.COCCYGEAL; in 5..9 -> Region.SACRAL; in 10..14 -> Region.LUMBAR; in 15..26 -> Region.THORACIC; else -> Region.CERVICAL
    }

    /**
     * Practice stage, as app.js spineLevel(): 1 + completed attempts + self-reported independent practice, capped at 33.
     * Ended or interrupted attempts stay visible in the record but do not advance a stage. Missed days remove nothing.
     * Visual only; it never unlocks practice, study or capability.
     */
    fun stageLevel(r: SadhanaRecord): Int = minOf(STAGES, 1 + r.attempts.count { it.outcome == SadhanaOutcome.COMPLETED } + r.independentCount)

    /** Next centre above the current stage, for the plain readout. */
    fun nextCentre(level: Int): SpineCentre? = centres.filter { threshold(it.id) > level }.minByOrNull { threshold(it.id) }

    /** Practice-trace layer: wireframe meter min(100, 20 + attempts*12 + independent*8), from the record only. */
    fun tracePercent(r: SadhanaRecord): Int = minOf(100, 20 + r.attemptCount * 12 + r.independentCount * 8)

    /** One root-branch line in dp, relative to its origin on the trace. [order] 0..1 = when it grows within a pass. */
    data class RootSegment(val x0: Float, val y0: Float, val x1: Float, val y1: Float, val depth: Int, val order: Float)

    /**
     * Procedural root/nerve branches (founder reference wireframes/sadhana-v1/reference/spine-roots-reference.jpg).
     * Deterministic for a seed; recursive forks fanning left and right, thinning with depth.
     */
    fun roots(seed: Long, length: Float = 34f, maxDepth: Int = 3): List<RootSegment> {
        val r = java.util.Random(seed)
        val out = mutableListOf<RootSegment>()
        fun grow(x: Float, y: Float, angle: Double, len: Float, depth: Int, start: Float) {
            if (depth > maxDepth || len < 4f) return
            val x1 = x + (kotlin.math.cos(angle) * len).toFloat(); val y1 = y + (kotlin.math.sin(angle) * len).toFloat()
            out += RootSegment(x, y, x1, y1, depth, start)
            val forks = 1 + r.nextInt(2)
            repeat(forks) { grow(x1, y1, angle + (r.nextDouble() - 0.5) * 1.1, len * (0.55f + r.nextFloat() * 0.2f), depth + 1, start + 0.25f) }
        }
        listOf(-1, 1).forEach { side ->
            val base = if (side < 0) Math.PI else 0.0
            repeat(1 + r.nextInt(2)) { grow(0f, 0f, base + side * (r.nextDouble() * 0.9 - 0.2), length * (0.7f + r.nextFloat() * 0.5f), 0, 0f) }
        }
        return out
    }

    /**
     * Symbolic vagal layer (founder app.js anatomy()): an orientation illustration in the founder's 260x430 viewBox.
     * Not anatomy-accurate, not a detector, not a measurement, no stimulation claim.
     */
    val bodyLines = listOf(
        listOf(112f to 25f, 82f to 49f, 84f to 86f, 44f to 129f, 54f to 198f, 66f to 260f, 82f to 309f, 91f to 350f, 81f to 413f),
        listOf(148f to 25f, 178f to 49f, 176f to 86f, 216f to 129f, 206f to 198f, 194f to 260f, 178f to 309f, 169f to 350f, 179f to 413f),
    )
    /** Cubic paths: start, then (c1, c2, end) triples, as in app.js. */
    val vagusPaths: List<List<Pair<Float, Float>>> = listOf(
        listOf(111f to 48f, 86f to 78f, 91f to 111f, 74f to 139f, 57f to 170f, 73f to 197f, 62f to 224f, 53f to 248f, 69f to 278f, 58f to 307f),
        listOf(149f to 48f, 174f to 78f, 169f to 111f, 186f to 139f, 203f to 170f, 187f to 197f, 198f to 224f, 207f to 248f, 191f to 278f, 202f to 307f),
        listOf(78f to 139f, 99f to 148f, 100f to 166f, 110f to 177f), listOf(182f to 139f, 161f to 148f, 160f to 166f, 150f to 177f),
        listOf(65f to 225f, 89f to 218f, 98f to 230f, 109f to 241f), listOf(195f to 225f, 171f to 218f, 162f to 230f, 151f to 241f),
        listOf(61f to 306f, 87f to 295f, 98f to 309f, 113f to 326f), listOf(199f to 306f, 173f to 295f, 162f to 309f, 147f to 326f),
    )
    /** Seven firefly points (x, y, radius) from app.js. */
    val fireflies = listOf(Triple(111f, 48f, 4f), Triple(80f, 134f, 3f), Triple(187f, 160f, 3.5f), Triple(65f, 226f, 3f),
        Triple(194f, 253f, 4f), Triple(66f, 306f, 3.5f), Triple(160f, 319f, 3f))

    /** Non-rhythmic flicker schedule: each firefly waits a random 1.5-7 s, then glows for a random 1.2-3.5 s. */
    fun fireflyGapMs(r: java.util.Random) = 1_500L + r.nextInt(5_500)
    fun fireflyGlowMs(r: java.util.Random) = 1_200 + r.nextInt(2_300)
}
