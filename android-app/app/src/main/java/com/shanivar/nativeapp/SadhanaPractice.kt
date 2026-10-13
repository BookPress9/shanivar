package com.shanivar.nativeapp

/*
 * Sadhana V1, build step S1 (decision 0017, wireframes/sadhana-v1/HANDOFF.md "Domain model",
 * "Content and safety gates"). Practice definitions with an explicit status and risk class.
 * Research-only and qualified-context records can never be launched. No attempts, minutes,
 * completions, self-reports, capability evidence or payment unlock them. This rule is a pure function
 * of the definition, so activity data cannot reach it at all.
 */

enum class SadhanaCapability(val label: String, val line: String, val invitation: String) {
    ARRIVE("Arrive", "Enter deliberately. Stop deliberately.", "enter one bounded practice"),
    RETURN("Return", "Notice drift. Come back by choice.", "use one return cue in ordinary life"),
    SEE("See", "Separate event, story and response.", "observe one decision point"),
    CHOOSE("Choose", "Protect one action that matters.", "protect one chosen action"),
    CARRY("Carry", "Practise when the phone is absent.", "practise privately, with less prompting"),
}

enum class PracticeStatus { RELEASED, STUDY_ONLY, QUALIFIED_CONTEXT, RESEARCH_ONLY }
enum class RiskClass { FOUNDATION, ORDINARY_ATTENTION, INTENSIVE }

data class SadhanaPracticeDefinition(
    val id: String,
    val contentVersion: String,
    val status: PracticeStatus,
    val risk: RiskClass,
    val capability: SadhanaCapability?,
    val title: String,
    val minutes: List<Int>,
    val effort: String,
    val prompt: String,
    /** Plain source line for the main app (no tradition named; ST-011 as amended 12 Oct 2026). */
    val sourcePlain: String,
    /** Named source, shown only inside deep study. */
    val sourceDeepStudy: String,
    val boundary: String,
    /** Content review manifest id. Null means not yet reviewed for public release. */
    val reviewManifestId: String? = null,
)

object SadhanaCatalog {
    const val VERSION = "sadhana-v1-proto-2026-10-12"

    /** Copy is verbatim from wireframes/sadhana-v1/app.js PRACTICES, except sourcePlain (naming rule). */
    val practices: List<SadhanaPracticeDefinition> = listOf(
        SadhanaPracticeDefinition("natural", VERSION, PracticeStatus.RELEASED, RiskClass.FOUNDATION, SadhanaCapability.ARRIVE,
            "Return to one ordinary breath", listOf(1, 3, 5), "Gentle attention",
            "Notice the breath already happening. Leave its size and speed alone.",
            "Classical source · modern adaptation · see Deep study", "Vijñāna-bhairava-tantra 24 · modern adaptation",
            "No paced breathing, holding, target feeling or readiness inference."),
        SadhanaPracticeDefinition("room", VERSION, PracticeStatus.RELEASED, RiskClass.FOUNDATION, SadhanaCapability.RETURN,
            "Receive the room", listOf(1, 3), "Eyes-open attention",
            "Choose one ordinary detail in the room. Return when you want to.",
            "Modern external-attention alternative", "Modern external-attention alternative",
            "No inner focus required. Change the detail or stop at any time."),
        SadhanaPracticeDefinition("threshold", VERSION, PracticeStatus.RELEASED, RiskClass.ORDINARY_ATTENTION, SadhanaCapability.SEE,
            "The moment before the reach", listOf(3, 5), "Real-world inquiry",
            "Notice the reach. Name what you intended to do before you decide.",
            "Shanivar behaviour-design synthesis", "Shanivar behaviour-design synthesis",
            "Does not diagnose compulsion or prove attention control."),
        SadhanaPracticeDefinition("oneaction", VERSION, PracticeStatus.RELEASED, RiskClass.ORDINARY_ATTENTION, SadhanaCapability.CHOOSE,
            "Protect one chosen action", listOf(5, 10), "Practice + ordinary action",
            "Choose the next action. Give it a boundary. Begin without waiting for a feeling.",
            "Shanivar Routine integration", "Shanivar Routine integration",
            "Completion does not measure productivity, worth or mental health."),
    )

    /** Contextual study records only (HANDOFF "Content and safety gates"). Never launchable. */
    val researchOnly: List<SadhanaPracticeDefinition> = listOf(
        "retention" to "Breath retention", "forceful" to "Forceful breathwork", "fasting" to "Fasting",
        "sleep" to "Sleep restriction", "deprivation" to "Sensory deprivation", "somatic" to "Strong somatic work",
        "kundalini" to "Kundalini-oriented induction", "sexuality" to "Sexuality-related practice",
        "altered" to "Altered-state techniques",
    ).map { (id, t) ->
        SadhanaPracticeDefinition("research-$id", VERSION, PracticeStatus.RESEARCH_ONLY, RiskClass.INTENSIVE, null,
            t, emptyList(), "Research / qualified setting", "", "Contextual study only", "Contextual study only",
            "Practice activity never unlocks this.")
    }

    val all: List<SadhanaPracticeDefinition> get() = practices + researchOnly

    fun byId(id: String): SadhanaPracticeDefinition? = all.firstOrNull { it.id == id }

    /**
     * The single launch gate. Depends only on the definition and the build's review policy.
     * allowUnreviewed is true only for debug pilot builds; release requires a review manifest.
     */
    fun canLaunch(def: SadhanaPracticeDefinition, allowUnreviewed: Boolean): Boolean =
        def.status == PracticeStatus.RELEASED &&
            def.risk != RiskClass.INTENSIVE &&
            def.minutes.isNotEmpty() &&
            (allowUnreviewed || ContentReviews.approves(def))

    fun validDuration(def: SadhanaPracticeDefinition, minutes: Int): Boolean = minutes in def.minutes

    fun next(id: String): SadhanaPracticeDefinition {
        val i = practices.indexOfFirst { it.id == id }
        return practices[(i + 1).mod(practices.size)]
    }
}
