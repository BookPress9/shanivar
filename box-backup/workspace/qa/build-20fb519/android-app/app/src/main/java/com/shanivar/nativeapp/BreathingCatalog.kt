package com.shanivar.nativeapp

import android.content.Context
import org.json.JSONObject

sealed interface BreathingCatalogResult {
    data class Valid(val catalogue: BreathingCatalogue) : BreathingCatalogResult

    /** Every problem found, not just the first - a content author fixing one typo at a time across
     * eight entries is a bad trade for a longer message. */
    data class Invalid(val problems: List<String>) : BreathingCatalogResult
}

/**
 * Loads and validates the packaged practice catalogue.
 *
 * The validator is the enforcement point for several rules that are otherwise only prose in the
 * handoff: no prescribed breath phases, no source metadata missing from a practice that claims a
 * VBT verse, no duration whose stages cannot sum exactly, no unrecognised kind. A content edit that
 * breaks one of those fails here rather than shipping something that reads as authoritative but is
 * not (test guide C01-C08).
 *
 * There is no built-in hardcoded fallback script. The test guide permits one *only if its text is
 * independently validated*, and none has been, so a corrupt or missing asset produces
 * [BreathingCatalogResult.Invalid] and the UI shows an unavailable state with a working Exit.
 * Inventing instructions to keep a screen populated is the failure mode worth avoiding here.
 */
object BreathingCatalog {
    const val ASSET_NAME = "breathing-v1.json"
    const val SUPPORTED_SCHEMA_VERSION = 1

    /** Product minimum: the selection sheet offers 1/3/5 minutes, and a stage plan needs room for
     * arrival + instruction + a real practice stage. */
    private const val MIN_DURATION_SECONDS = 60
    private const val MAX_DURATION_SECONDS = 1800

    /** A verse label, never a technique ordinal. "24" and "156-A" pass; "technique 12" does not. */
    private val VERSE_LABEL = Regex("^\\d{1,3}(-[A-Z])?$")

    fun load(context: Context): BreathingCatalogResult {
        val raw = runCatching {
            context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        }.getOrElse { return BreathingCatalogResult.Invalid(listOf("Practice content could not be read: ${it.javaClass.simpleName}")) }
        return parse(raw)
    }

    fun parse(raw: String): BreathingCatalogResult {
        val root = runCatching { JSONObject(raw) }
            .getOrElse { return BreathingCatalogResult.Invalid(listOf("Practice content is not valid JSON.")) }
        val problems = mutableListOf<String>()

        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            problems += "Unsupported schemaVersion $schemaVersion; this build understands $SUPPORTED_SCHEMA_VERSION."
        }
        val catalogVersion = root.optString("catalogVersion").orBlankProblem("catalogVersion", problems)
        val status = root.optString("status").orBlankProblem("status", problems)
        val scriptLanguage = root.optString("scriptLanguage").orBlankProblem("scriptLanguage", problems)
        // A missing language-fallback statement is a validation failure, not a cosmetic gap: without
        // it there is nothing to show a Hindi-preferring reader when only English script exists (C09).
        val languageFallback = root.optString("languageFallback").orBlankProblem("languageFallback", problems)
        val globalGuidance = root.optString("globalGuidance").orBlankProblem("globalGuidance", problems)

        val practicesJson = root.optJSONArray("practices")
        if (practicesJson == null || practicesJson.length() == 0) {
            problems += "practices is missing or empty."
            return BreathingCatalogResult.Invalid(problems)
        }

        val practices = mutableListOf<BreathingPractice>()
        for (index in 0 until practicesJson.length()) {
            val item = practicesJson.optJSONObject(index)
            if (item == null) {
                problems += "practices[$index] is not an object."
                continue
            }
            parsePractice(item, index, problems)?.let { practices += it }
        }
        practices.groupBy { it.id }.filterValues { it.size > 1 }.keys.forEach {
            problems += "Duplicate practice id \"$it\"."
        }

        if (problems.isNotEmpty()) return BreathingCatalogResult.Invalid(problems)
        return BreathingCatalogResult.Valid(
            BreathingCatalogue(
                schemaVersion = schemaVersion,
                catalogVersion = catalogVersion,
                status = status,
                scriptLanguage = scriptLanguage,
                languageFallback = languageFallback,
                globalGuidance = globalGuidance,
                practices = practices,
            )
        )
    }

    private fun parsePractice(item: JSONObject, index: Int, problems: MutableList<String>): BreathingPractice? {
        val where = item.optString("id").ifBlank { "practices[$index]" }
        val id = item.optString("id")
        if (id.isBlank()) problems += "$where: id is missing."
        val title = item.optString("title")
        if (title.isBlank()) problems += "$where: title is missing."
        val kind = BreathingKind.fromId(item.optString("kind"))
        if (kind == null) problems += "$where: unrecognised kind \"${item.optString("kind")}\"."

        // Structural ban, not a warning: content carrying breath-phase timings cannot load at all.
        val phases = item.optJSONArray("prescribedBreathPhases")
        if (phases != null && phases.length() > 0) {
            problems += "$where: prescribedBreathPhases must be empty - this feature never prescribes breath timing."
        }

        val durations = item.optJSONArray("allowedDurationsSeconds")
            ?.let { array -> (0 until array.length()).map { array.optInt(it, -1) } }
            .orEmpty()
        if (durations.isEmpty()) problems += "$where: allowedDurationsSeconds is missing or empty."
        if (durations.any { it < MIN_DURATION_SECONDS || it > MAX_DURATION_SECONDS }) {
            problems += "$where: durations must be between $MIN_DURATION_SECONDS and $MAX_DURATION_SECONDS seconds."
        }
        if (durations.distinct().size != durations.size) problems += "$where: allowedDurationsSeconds repeats a value."
        val defaultDuration = item.optInt("defaultDurationSeconds", -1)
        if (defaultDuration !in durations) problems += "$where: defaultDurationSeconds is not one of allowedDurationsSeconds."

        val stages = mutableMapOf<BreathingStageId, String>()
        item.optJSONArray("stages")?.let { array ->
            for (position in 0 until array.length()) {
                val stage = array.optJSONObject(position) ?: continue
                val stageId = BreathingStageId.fromId(stage.optString("id"))
                if (stageId == null) {
                    problems += "$where: unrecognised stage \"${stage.optString("id")}\"."
                    continue
                }
                stages[stageId] = stage.optString("text")
            }
        }
        val isSleep = kind == BreathingKind.SLEEP_ATTENTION
        listOf(BreathingStageId.ARRIVAL, BreathingStageId.INSTRUCTION, BreathingStageId.PRACTICE).forEach {
            if (stages[it].isNullOrBlank()) problems += "$where: stage ${it.id} text is missing."
        }
        // A sleep practice must end silently; anything else must actually say how it ends.
        if (isSleep) {
            if (!stages[BreathingStageId.CLOSING].isNullOrBlank()) {
                problems += "$where: a sleep practice must have no closing text."
            }
        } else if (stages[BreathingStageId.CLOSING].isNullOrBlank()) {
            problems += "$where: stage closing text is missing."
        }

        val presentationJson = item.optJSONObject("presentation")
        if (presentationJson == null) problems += "$where: presentation is missing."
        val presentation = BreathingPresentation(
            eyesOpenAllowed = presentationJson?.optBoolean("eyesOpenAllowed", true) ?: true,
            keepScreenOn = presentationJson?.optBoolean("keepScreenOn", false) ?: false,
            closingBell = presentationJson?.optBoolean("closingBell", false) ?: false,
            showCompletionQuestions = presentationJson?.optBoolean("showCompletionQuestions", true) ?: true,
            toneAsset = presentationJson?.takeUnless { it.isNull("toneAsset") }?.optString("toneAsset")?.ifBlank { null },
        )
        val requiresSoundConsent = item.optBoolean("requiresSoundConsent", false)
        if (presentation.toneAsset != null && !requiresSoundConsent) {
            problems += "$where: a practice that plays a tone must require sound consent."
        }
        if (isSleep && (presentation.keepScreenOn || presentation.closingBell)) {
            problems += "$where: a sleep practice must not keep the screen on or ring a closing bell."
        }

        val requiresSleepIntent = item.optBoolean("requiresSleepIntent", false)
        val requiresSleepyNow = item.optBoolean("requiresSleepyNow", false)
        if (requiresSleepyNow && !requiresSleepIntent) {
            problems += "$where: requiring current sleepiness without an explicit sleep intention would let the clock route someone into sleep practice."
        }

        val source = parseSource(item, where, problems)

        if (kind == null || id.isBlank() || durations.isEmpty()) return null
        val practice = BreathingPractice(
            id = id,
            title = title,
            kind = kind,
            contentVersion = item.optInt("contentVersion", 1),
            status = item.optString("status"),
            source = source,
            allowedDurationsSeconds = durations,
            defaultDurationSeconds = defaultDuration,
            suitableFeelings = item.stringSet("suitableFeelings"),
            suitableContexts = item.stringSet("suitableContexts"),
            requiresBreathComfort = item.optBoolean("requiresBreathComfort", false),
            requiresSoundConsent = requiresSoundConsent,
            requiresPriorComfort = item.optBoolean("requiresPriorComfort", false),
            requiresSleepIntent = requiresSleepIntent,
            requiresSleepyNow = requiresSleepyNow,
            safeStationarySettingRequired = item.optBoolean("safeStationarySettingRequired", true),
            stages = stages,
            presentation = presentation,
            enabled = item.optBoolean("enabled", true),
        )
        // Every offered duration must produce a plan that sums back to it exactly (C06).
        durations.filter { it >= MIN_DURATION_SECONDS }.forEach { duration ->
            val planned = runCatching { practice.stagePlan(duration).sumOf { it.seconds } }.getOrNull()
            if (planned != duration) problems += "$where: stages do not sum to ${duration}s (got $planned)."
        }
        return practice
    }

    /** `source` may legitimately be null - `external_grounding` is a modern exercise and claiming a
     * verse for it would be fabricated attribution (C03). What is not allowed is a half-filled
     * source: if a practice cites the tradition, every field a reader needs must be present. */
    private fun parseSource(item: JSONObject, where: String, problems: MutableList<String>): BreathingSource? {
        val json = item.optJSONObject("source") ?: return null
        val verseLabels = json.optJSONArray("verseLabels")
            ?.let { array -> (0 until array.length()).map { array.optString(it) } }
            .orEmpty()
        if (verseLabels.isEmpty()) problems += "$where: source has no verseLabels."
        verseLabels.filterNot { VERSE_LABEL.matches(it) }.forEach {
            problems += "$where: \"$it\" is not a verse label."
        }
        val fields = listOf("work", "edition", "url", "relationship", "adaptationNote", "verification")
        fields.filter { json.optString(it).isBlank() }.forEach { problems += "$where: source.$it is missing." }
        return BreathingSource(
            work = json.optString("work"),
            verseLabels = verseLabels,
            edition = json.optString("edition"),
            url = json.optString("url"),
            relationship = json.optString("relationship"),
            adaptationNote = json.optString("adaptationNote"),
            verification = json.optString("verification"),
        )
    }

    private fun JSONObject.stringSet(key: String): Set<String> =
        optJSONArray(key)?.let { array -> (0 until array.length()).map { array.optString(it) }.toSet() }.orEmpty()

    private fun String.orBlankProblem(field: String, problems: MutableList<String>): String {
        if (isBlank()) problems += "$field is missing."
        return this
    }
}
