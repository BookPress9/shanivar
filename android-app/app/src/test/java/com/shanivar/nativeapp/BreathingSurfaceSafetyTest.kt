package com.shanivar.nativeapp

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.time.ZonedDateTime
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DEV-89: the Breathing* surfaces, the block screen and the breathing asset must not pace or time
 * the breath, and must not make physiological, medical or energy claims (or score anything).
 *
 * Covers what DEV-39's TechniqueContentSafetyTest does not: every string literal in the Breathing*
 * source files and the screens that host them, every string value in breathing-v1.json (including the
 * "Origin & adaptation" notes the UI shows), the JSON's structure, the practice data model, the
 * absence of periodic animation, and the block screen's own options. techniques.json stays DEV-39's.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class BreathingSurfaceSafetyTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before fun resetState() {
        context.getSharedPreferences(BreathingPreferences.NAMESPACE, 0).edit().clear().commit()
        BreathingSessionOwner.resetForTest()
        InitiationSitting.resetForTest()
        BreathingSessionOwner.monotonicMillis = { 0L }
        BreathingSessionOwner.wallClock = { ZonedDateTime.of(2026, 9, 25, 14, 0, 0, 0, BreathingFixtures.ZONE) }
        BreathingSessionOwner.newSessionId = { "safety-session" }
    }

    @After fun clearState() { BreathingSessionOwner.resetForTest(); InitiationSitting.resetForTest() }

    // ---- Rules -------------------------------------------------------------------------------

    private fun rx(p: String) = Regex(p, RegexOption.IGNORE_CASE)
    private val breathWord = "(breath\\w*|breathe\\w*|inhal\\w*|exhal\\w*|in-breath|out-breath)"
    private val number = "(\\d+|a count|one|two|three|four|five|six|seven|eight|nine|ten|twelve)"

    /** Pacing or timing the breath. No negation exemption: these phrasings have no safe use. */
    private val pacing = listOf(
        rx("\\b(inhal|exhal|breathe (in|out))\\w*\\b[^.]{0,40}\\b(for|to) $number\\b"),
        rx("\\bcount of\\b"),
        rx("\\bcount(ing)? (to|your|each|every|the) ($number|breath\\w*)"),
        rx("\\bhold (your |the )?breath"),
        rx("\\bhold (gently )?for\\b"),
        rx("\\b(slow|slower|deep|deeper|long|longer|full|big)\\b,?[^.]{0,20}?\\b(breaths?|inhales?|exhales?|in-breaths?|out-breaths?)\\b"),
        rx("\\b$number\\b[^.]{0,20}\\bbreaths\\b"),
        rx("\\bbreathe (in |out )?(slowly|deeply|slower|faster|longer|deeper)\\b"),
        rx("\\b(slower|faster|longer|deeper) than (you )?breathe"),
        rx("\\brounds?\\b"),
        rx("\\bcycles? of\\b"),
        rx("\\b\\d+ (times|cycles|reps)\\b"),
        rx("\\bbreaths? (per|a) minute\\b"),
        rx("\\bbpm\\b"),
        rx("\\b\\d-\\d-\\d\\b"),
        rx("\\bbox breath"),
        rx("\\b\\d+\\s*(seconds?|secs?|counts?|beats?)\\b.{0,60}\\b$breathWord"),
        rx("\\b$breathWord\\b.{0,60}\\b\\d+\\s*(seconds?|secs?|counts?|beats?)\\b"),
        rx("\\b(follow|match)\\b[^.]{0,20}\\b(circle|ring|planet|orbit|animation|tone|bell|shape)\\b[^.]{0,30}\\bbreath"),
        rx("\\bbreathe (with|along with) (the|this)\\b"),
        rx("\\b(as|when) (the )?(circle|ring|shape|orbit|planet) (grows|expands|shrinks|rises|falls)"),
    )

    /**
     * Claims and scoring. A match is exempt when its own sentence already negates it before the match
     * ("No retention, ... or promised physiological effect.", "This isn't a diagnosis", "Nobody
     * scores it."): the shipped copy uses those to state what the app does not do.
     */
    private val claims = listOf(
        rx("nervous system"), rx("\\bvag(us|al)\\b"), rx("cortisol"), rx("heart rate"), rx("blood (pressure|flow)"),
        rx("\\boxygen"), rx("carbon dioxide|\\bco2\\b"), rx("\\blungs?\\b"), rx("hormon"), rx("immun"), rx("detox"),
        rx("physiolog"), rx("\\bmedical\\b"), rx("\\bdiagnos"), rx("\\btherap"), rx("\\btreat(s|ment|ing)?\\b"),
        rx("\\bheal(s|ed|ing)?\\b"), rx("\\bcur(e|es|ed|ing)\\b"), rx("\\bretention\\b|\\bretain\\b"),
        rx("\\bcalm(s|ing)? (your|the) (mind|body|nerves)"), rx("\\brelax(es|ing)? (your|the) (body|mind|muscles)"),
        rx("\\b(reduces?|lowers?|relieves?|eases?) (stress|anxiety|tension|pain)"),
        rx("\\b(boost|raise|increase|restore|recharge|balance|lift)s? (your )?energy"), rx("\\benergi[sz](e|es|ed|ing)\\b"),
        rx("\\benergy (flow|flows|centre|center|body|channel|level|levels|reading)\\b"), rx("chakra"), rx("\\bprana"),
        rx("kundalini"), rx("\\bimprov(e|es|ing) (your )?(sleep|focus|mood|health|breathing)"),
        rx("\\b(will|guaranteed to|proven to) (help|calm|relax|reduce|improve|make you)\\b"),
        rx("\\b(scientifically|clinically)\\b"),
        rx("\\b(xp|streaks?|scores?|scored|points|badges?|leaderboard|level up)\\b"),
    )
    private val negation = rx("(\\bno\\b|\\bnot\\b|n't|n’t|\\bnever\\b|\\bwithout\\b|\\bomits\\b|\\bnor\\b|\\bnobody\\b|\\bnothing\\b)")

    private fun violations(label: String, text: String): List<String> {
        val found = pacing.mapNotNull { r -> r.find(text)?.let { "$label paces /${r.pattern}/: \"${it.value}\" in \"$text\"" } }
        val claimed = claims.flatMap { r ->
            r.findAll(text).filterNot { m ->
                val sentenceStart = text.lastIndexOfAny(charArrayOf('.', ';', '!', '?'), (m.range.first - 1).coerceAtLeast(0)) + 1
                negation.containsMatchIn(text.substring(sentenceStart, m.range.first))
            }.map { "$label claims /${r.pattern}/: \"${it.value}\" in \"$text\"" }.toList()
        }
        return found + claimed
    }

    // ---- Sources -----------------------------------------------------------------------------

    private val srcDir = File("src/main/java/com/shanivar/nativeapp")

    /** Every Breathing* file (picked up automatically, so a new one is covered too), the pause, the
     * block screen, and the two other screens rendered inside or around BreathingHost. */
    private fun surfaceFiles(): List<File> {
        val breathing = srcDir.listFiles()!!.filter { it.name.startsWith("Breathing") && it.name.endsWith(".kt") }.sortedBy { it.name }
        assertTrue("expected the Breathing* files, found ${breathing.map { it.name }}", breathing.size >= 9)
        return breathing + listOf("LocalPauseScreen.kt", "BlockOverlayActivity.kt", "InitiationSitting.kt", "V1Home.kt").map { File(srcDir, it) }
    }

    private fun asset(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }

    // ---- AC1 / AC2: shipped text ------------------------------------------------------------------

    @Test fun breathingSourceStringsNeitherPaceNorClaim() {
        // InitiationSitting's own banned-word lists are the one place these words may appear as data.
        val bannedLists = Regex("val (forbiddenWords|bannedPhrases) = listOf\\([^)]*\\)")
        val literals = surfaceFiles().flatMap { file ->
            KotlinLiterals.of(file.readText().replace(bannedLists, "")).map { file.name to it }
        } + InitiationSittingCopy.allText().map { "InitiationSittingCopy" to it }
        // Sanity: the lexer really is reading the shipped copy.
        assertTrue(literals.size > 150)
        assertTrue(literals.any { it.second == "Take a one-minute pause" })
        assertTrue(literals.any { it.second == BREATHING_SAFETY_NOTE })
        val found = literals.flatMap { (file, text) -> violations(file, text) }
        assertTrue(found.joinToString("\n"), found.isEmpty())
    }

    @Test fun everyStringInTheBreathingAssetNeitherPacesNorClaims() {
        val found = mutableListOf<String>()
        fun walk(path: String, node: Any?) {
            when (node) {
                is JSONObject -> node.keys().forEach { walk("$path.$it", node.get(it)) }
                is JSONArray -> (0 until node.length()).forEach { walk("$path[$it]", node.get(it)) }
                is String -> if (!path.endsWith(".url")) found += violations(path, node)
            }
        }
        walk("breathing-v1", JSONObject(asset(BreathingCatalog.ASSET_NAME)))
        assertTrue(found.joinToString("\n"), found.isEmpty())
    }

    @Test fun theBreathingAssetHasNoWayToExpressBreathTiming() {
        val root = JSONObject(asset(BreathingCatalog.ASSET_NAME))
        val rules = root.getJSONObject("rules")
        listOf("noPrescribedBreathTiming", "noRetention", "noBreathMeasurement").forEach { assertTrue(it, rules.getBoolean(it)) }
        // Stage timing is screen-reading time for the four text stages only, never a breath phase.
        val allowedTiming = setOf("arrivalSeconds", "instructionSeconds", "closingSeconds", "practiceSeconds", "sleepOverride")
        val timing = root.getJSONObject("stageTiming")
        assertEquals(allowedTiming, timing.keys().asSequence().toSet())
        assertEquals(allowedTiming - "sleepOverride", timing.getJSONObject("sleepOverride").keys().asSequence().toSet())
        val bannedKey = rx("inhal|exhal|hold|retention|retain|pace|pacing|rhythm|rate|perminute|round|cycle|count|rep(s|etition)")
        val badKeys = mutableListOf<String>()
        fun walk(path: String, node: Any?) {
            when (node) {
                is JSONObject -> node.keys().forEach { key ->
                    if (path != "rules" && bannedKey.containsMatchIn(key)) badKeys += "$path.$key"
                    walk(if (path.isEmpty()) key else "$path.$key", node.get(key))
                }
                is JSONArray -> (0 until node.length()).forEach { walk("$path[$it]", node.get(it)) }
            }
        }
        walk("", root)
        assertTrue(badKeys.joinToString(), badKeys.isEmpty())
        val practices = root.getJSONArray("practices")
        for (i in 0 until practices.length()) {
            val p = practices.getJSONObject(i)
            assertEquals("${p.getString("id")} prescribes breath phases", 0, p.getJSONArray("prescribedBreathPhases").length())
            assertEquals(setOf("arrival", "instruction", "practice", "closing"),
                (0 until p.getJSONArray("stages").length()).map { p.getJSONArray("stages").getJSONObject(it).getString("id") }.toSet())
        }
        // And the validator still refuses content that tries.
        val paced = asset(BreathingCatalog.ASSET_NAME).replaceFirst("\"prescribedBreathPhases\": []",
            "\"prescribedBreathPhases\": [{\"phase\": \"inhale\", \"seconds\": 4}]")
        assertTrue(BreathingCatalog.parse(paced) is BreathingCatalogResult.Invalid)
    }

    @Test fun thePracticeModelHasNoBreathTimingField() {
        val banned = rx("inhal|exhal|hold|retention|pace|pacing|breathrate|perminute|round|cycle|count")
        listOf(BreathingPractice::class.java, BreathingPresentation::class.java, BreathingStageSlice::class.java, BreathingSource::class.java)
            .flatMap { cls -> cls.declaredFields.map { "${cls.simpleName}.${it.name}" } }
            .forEach { assertFalse(it, banned.containsMatchIn(it.substringAfter('.'))) }
        assertEquals(listOf("arrival", "instruction", "practice", "closing"), BreathingStageId.values().map { it.id })
    }

    @Test fun noBreathingSurfaceRunsAPeriodicAnimation() {
        val periodic = listOf("rememberInfiniteTransition", "infiniteRepeatable", "repeatable(", "Animatable(", "animateTo(",
            "keyframes", "AsState(", "animateFloat", "animateDp", "withFrameNanos", "withFrameMillis")
        val found = surfaceFiles().flatMap { file ->
            val code = KotlinLiterals.codeOnly(file.readText())
            periodic.filter { code.contains(it) }.map { "${file.name} uses $it" }
        }
        assertTrue(found.joinToString("\n"), found.isEmpty())
    }

    @Test fun theRenderedPracticeListNeitherPacesNorClaims() {
        BreathingPreferences(context).apply { soundConsent = true; priorBreathComfort = true }
        compose.setContent { ShanivarTheme { BreathingHost(BreathingEntry.HOME, onExit = {}) } }
        compose.onNodeWithText("I'm somewhere safe and still").performScrollTo().performClick()
        compose.onNodeWithText("Breathing is comfortable").performScrollTo().performClick()
        compose.onNodeWithText("Going to sleep").performScrollTo().performClick()
        compose.onNodeWithText("I'm going to sleep now").performScrollTo().performClick()
        compose.onNodeWithText("I feel sleepy now").performScrollTo().performClick()
        compose.onNodeWithText("All suitable options").performScrollTo().performClick()
        compose.waitForIdle()
        val shown = renderedText()
        // Every practice is on screen (the tone practice only where the device can make the tone), so
        // this reads each one's instruction as the UI renders it.
        assertTrue(compose.onAllNodesWithText("Origin & adaptation").fetchSemanticsNodes().size >= 7)
        val found = shown.flatMap { violations("BreathingHost", it) }
        assertTrue(found.joinToString("\n"), found.isEmpty())
    }

    // ---- AC3: the block screen ---------------------------------------------------------------

    @Test fun theBlockScreenOffersOnlyTheNonPacedPauseAndReturn() {
        var paused = 0
        var returned = 0
        compose.setContent { ShanivarTheme { BlockOverlayScreen("com.example.feed", Triple("", "", ""), onPause = { paused++ }, onReturn = { returned++ }) } }
        compose.onAllNodesWithText("Choose a short practice").assertCountEquals(0)
        compose.onAllNodes(hasClickAction()).assertCountEquals(2)
        compose.onNodeWithText("Take a one-minute pause").assertHasClickAction().performClick()
        compose.onNodeWithText("Return to my day").assertHasClickAction().performClick()
        assertEquals(1, paused)
        assertEquals(1, returned)
        val found = renderedText().flatMap { violations("BlockOverlayScreen", it) }
        assertTrue(found.joinToString("\n"), found.isEmpty())
    }

    @Test fun theBlockScreenCannotOpenTheBreathingPracticeList() {
        val code = KotlinLiterals.codeOnly(File(srcDir, "BlockOverlayActivity.kt").readText())
        assertFalse("block screen still routes to BreathingHost", code.contains("BreathingHost"))
        assertFalse("block screen still uses the BLOCK_OVERLAY practice entry", code.contains("BreathingEntry.BLOCK_OVERLAY"))
        assertFalse("block screen still has a breathe route", code.contains("\"breathe\""))
        assertTrue(code.contains("LocalPauseScreen("))
    }

    @Test fun theBlockScreenPauseGivesNoBreathInstruction() {
        compose.setContent { ShanivarTheme { LocalPauseScreen(isPreview = false, prompt = rememberGroundingPrompt(), onExit = {}) } }
        compose.onNodeWithText("Begin").performClick()
        compose.waitForIdle()
        val prompt = (BreathingCatalog.load(context) as BreathingCatalogResult.Valid)
            .catalogue.practice("external_grounding")!!.stages.getValue(BreathingStageId.INSTRUCTION)
        compose.onNodeWithText(prompt).assertExists()
        val shown = renderedText()
        val breathy = shown.filter { rx("\\b$breathWord").containsMatchIn(it) }
        assertTrue("the pause mentions breathing: $breathy", breathy.isEmpty())
        val found = shown.flatMap { violations("LocalPauseScreen", it) }
        assertTrue(found.joinToString("\n"), found.isEmpty())
    }

    // ---- AC4: the rules fail on the old content ------------------------------------------------

    @Test fun rulesCatchPreviouslyShippedAndLegacyContent() {
        listOf(
            // LocalPauseScreen.kt hardcoded prompt, removed in 5f5b1be.
            "Breathe out slower than you breathe in.",
            // Legacy Render backend daily-note fallback (backend/src/routes/insights.js).
            "Take three slow, conscious breaths before responding to any external trigger.",
            // Box breathing from the original techniques.json (DEV-39 fixture).
            "in for a count of four, hold gently for four. Breathe in... two, three, four.",
            "Hold the breath for 4 seconds.", "Do this for six to eight rounds.", "Inhale for four seconds.",
            "Breathe deeply and slowly.", "Take a deep breath.", "Six breaths per minute.", "Try 4-7-8 breathing.",
            "Follow the circle as you breathe.", "Breathe with the planet.", "As the ring expands, inhale.",
            "This calms the nervous system.", "Boost your energy with each exhale.", "Lowers cortisol.",
            "It reduces stress.", "It will help you sleep.", "Earn XP for every session.", "Keep your streak going.",
            "The breath-holding retention phase.",
        ).forEach { assertTrue(it, violations("fixture", it).isNotEmpty()) }
        assertTrue(Regex("\"breathe\" -> BreathingHost").containsMatchIn(
            "\"breathe\" -> BreathingHost(BreathingEntry.BLOCK_OVERLAY, rememberProtectionSnapshot(), onExit = ::goHome)"))
    }

    @Test fun rulesLeaveTheShippedSafeWordingAlone() {
        listOf(
            "Notice one ordinary breath coming in and going out. Leave its size and speed alone.",
            "Whenever attention wanders, return to an easy part of breathing. Nothing needs to become deeper or slower.",
            "If the words become distracting, drop them and notice ordinary breathing. There is no count to finish.",
            "Gentle mmm replaces the traditional utterance. No retention, forced emptying, central-channel visualization or promised physiological effect.",
            "Your own words for it. This isn't a diagnosis and nothing is saved.",
            "It stays on this phone. Nobody scores it. Edit or clear it whenever you like.",
            "Keep breathing comfortable. Stop if you feel dizzy, short of breath or more distressed.",
            "Take a one-minute pause", "One minute, on your terms. Pause or leave whenever you want.",
        ).forEach { assertTrue(it, violations("fixture", it).isEmpty()) }
    }

    @Test fun lexerReadsLiteralsAndSkipsComments() {
        val src = "// \"inhale for four\"\n/* \"hold for 4\" */ val a = \"one\" + \"t\\\"wo \${x(\"three\")} end\"\nval c = '\"'\nval r = \"\"\"raw\"\"\""
        assertEquals(setOf("one", "t\"wo   end", "three", "raw"), KotlinLiterals.of(src).toSet())
        assertFalse(KotlinLiterals.codeOnly(src).contains("inhale"))
    }

    private fun renderedText(): List<String> = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
        .fetchSemanticsNodes().flatMap { node -> node.config[SemanticsProperties.Text].map { it.text } }
}

/** A small Kotlin lexer: string literals (templates replaced by a space, nested literals included) and
 * code with comments removed. Enough for source-level content checks; not a compiler. */
internal object KotlinLiterals {
    fun of(src: String): List<String> = Lexer(src).also { it.code(stopAtBrace = false) }.literals

    fun codeOnly(src: String): String = Lexer(src).also { it.code(stopAtBrace = false) }.codeText.toString()

    private class Lexer(val s: String) {
        var i = 0
        val literals = mutableListOf<String>()
        val codeText = StringBuilder()

        fun code(stopAtBrace: Boolean) {
            var depth = 0
            while (i < s.length) {
                val c = s[i]
                when {
                    s.startsWith("//", i) -> { while (i < s.length && s[i] != '\n') i++ }
                    s.startsWith("/*", i) -> { val end = s.indexOf("*/", i + 2); i = if (end < 0) s.length else end + 2 }
                    s.startsWith("\"\"\"", i) -> { val end = s.indexOf("\"\"\"", i + 3); val stop = if (end < 0) s.length else end
                        literals += s.substring(i + 3, stop); codeText.append("\"\""); i = stop + 3 }
                    c == '"' -> { codeText.append('"'); i++; string(); codeText.append('"') }
                    c == '\'' -> { i += if (i + 1 < s.length && s[i + 1] == '\\') 4 else 3 }
                    c == '{' -> { depth++; codeText.append(c); i++ }
                    c == '}' -> { if (stopAtBrace && depth == 0) { i++; return }; depth--; codeText.append(c); i++ }
                    else -> { codeText.append(c); i++ }
                }
            }
        }

        fun string() {
            val sb = StringBuilder()
            while (i < s.length) {
                val c = s[i]
                when {
                    c == '\\' && i + 1 < s.length -> { sb.append(s[i + 1]); i += 2 }
                    c == '"' -> { i++; literals += sb.toString(); return }
                    s.startsWith("\${", i) -> { i += 2; code(stopAtBrace = true); sb.append(' ') }
                    else -> { sb.append(c); i++ }
                }
            }
            literals += sb.toString()
        }
    }
}
