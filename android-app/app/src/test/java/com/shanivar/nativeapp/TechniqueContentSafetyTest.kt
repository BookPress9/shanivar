package com.shanivar.nativeapp

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * DEV-39: bundled practice text must not pace the breath (counts, timed holds) and must not make
 * medical, physiological or energy claims. Scans every string in the shipped techniques.json and the
 * stage/guidance text of breathing-v1.json. Also scans the compiled sections of the founder sources in
 * shanivar-vbt/content/technique-*.md, read with a Kotlin mirror of scripts/compile-content.cjs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TechniqueContentSafetyTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun asset(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }

    private val breathWord = "(breath\\w*|breathe\\w*|inhal\\w*|exhal\\w*|in-breath|out-breath|hold\\w*|pause\\w*)"

    private val number = "(\\d+|two|three|four|five|six|seven|eight|nine|ten|twelve)"
    private val ordinal = "(first|second|third|fourth|fifth|sixth|seventh|eighth)"

    private val pacing = listOf(
        Regex("count of", RegexOption.IGNORE_CASE),
        Regex("\\bcounted\\b", RegexOption.IGNORE_CASE),
        Regex("hold (gently )?for", RegexOption.IGNORE_CASE),
        Regex("inhale for", RegexOption.IGNORE_CASE),
        Regex("exhale for", RegexOption.IGNORE_CASE),
        Regex("\\b(in|out) for (a count|one|two|three|four|five|six|seven|eight|\\d)", RegexOption.IGNORE_CASE),
        Regex("\\btwo, three, four\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(slow|deep|long) breaths?\\b", RegexOption.IGNORE_CASE),
        Regex("\\blong, slow\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(twice|\\w+ times) (a|per) minute\\b", RegexOption.IGNORE_CASE),
        Regex("\\b$number\\s*(to|or|-|–)\\s*$number\\s+rounds?\\b", RegexOption.IGNORE_CASE),
        Regex("\\b$number\\s+rounds\\b", RegexOption.IGNORE_CASE),
        Regex("\\b$ordinal\\s+or\\s+$ordinal\\s+round\\b", RegexOption.IGNORE_CASE),
        Regex("\\bextend(s|ed|ing)?\\b", RegexOption.IGNORE_CASE),
        Regex("\\bretention\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\d+\\s*(seconds?|counts?|beats?)\\b.{0,60}\\b$breathWord", RegexOption.IGNORE_CASE),
        Regex("\\b$breathWord\\b.{0,60}\\b\\d+\\s*(seconds?|counts?|beats?)\\b", RegexOption.IGNORE_CASE),
    )

    private val claims = listOf(
        "nervous system", "vagus", "vagal", "cortisol", "heart rate", "blood pressure", "energy center",
        "energy centre", "chakra",
    ).map { Regex(Regex.escape(it), RegexOption.IGNORE_CASE) } + listOf(
        Regex("\\bheal(s|ed|ing)?\\b", RegexOption.IGNORE_CASE),
        Regex("\\bcur(e|es|ed|ing)\\b", RegexOption.IGNORE_CASE),
        Regex("calming effect", RegexOption.IGNORE_CASE),
        Regex("\\bhas a calming\\b", RegexOption.IGNORE_CASE),
        Regex("breath-power", RegexOption.IGNORE_CASE),
        // Effect promises: the copy invites noticing, it does not predict an outcome.
        Regex("\\bsteadies faster\\b", RegexOption.IGNORE_CASE),
        Regex("\\btrains?\\b", RegexOption.IGNORE_CASE),
        Regex("\\bwill start to\\b", RegexOption.IGNORE_CASE),
        Regex("\\bcalmer\\b", RegexOption.IGNORE_CASE),
        Regex("\\bsome people notice\\b", RegexOption.IGNORE_CASE),
    )

    /**
     * Source citations may name the text and the translation, but must not carry a teacher's own
     * rendering or teaching (teachers stay internal) or energy-body language such as "breath-power".
     */
    private val sourceRules = listOf(
        Regex("breath-power", RegexOption.IGNORE_CASE),
        Regex("blog post", RegexOption.IGNORE_CASE),
        Regex("\\b(teacher|guru|teaches|taught by)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(Osho|Yogananda|Kriya)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(according to|independently renders)\\b", RegexOption.IGNORE_CASE),
        // Outcome promises carried in verse quotes: the source is a citation, not a promised result.
        Regex("\\bwill become\\b", RegexOption.IGNORE_CASE),
        Regex("\\bmanifest(s|ed|ing)?\\b", RegexOption.IGNORE_CASE),
        Regex("\\bshines?\\b", RegexOption.IGNORE_CASE),
        Regex("\\battain(s|ed|able|ment)?\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(then )?arises\\b", RegexOption.IGNORE_CASE),
        Regex("\\bbecomes (soothed|still|sublime)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(Bliss|Reality|reality is)\\b"),
        Regex("\\bfeeble or absent\\b", RegexOption.IGNORE_CASE),
    )

    private val quantity = "(\\d+|a|an|one|two|three|four|five|six|seven|eight|nine|ten|fifteen|twenty|thirty|forty|" +
        "forty-five|fifty|sixty|ninety|few|couple of|several|another|last|first|final|next|half an?|more|some|extra)"

    /**
     * Session shape in user-facing copy: no rounds, no timed stillness or durations, no hum tied to the
     * exhale. Not applied to the `duration` metadata field, which legitimately holds "3–5 minutes".
     * A bare ordinal "second" before a noun ("a second bite") is not a duration.
     */
    private val timing = listOf(
        Regex("\\brounds?\\b", RegexOption.IGNORE_CASE),
        Regex("\\b$quantity(\\s+(more|few|full|extra|couple of))*\\s+(seconds|minutes?|hours?)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b$quantity(\\s+(more|few|full|extra))*\\s+second\\b(?!\\s+(bite|sip|taste|time|look|wave|thought|helping))", RegexOption.IGNORE_CASE),
        Regex("\\b(half|split)[- ]?seconds?\\b", RegexOption.IGNORE_CASE),
        Regex("\\bfraction of a second\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(exhale|exhaled|exhaling|out-breath)\\b[^.]{0,40}\\bhum", RegexOption.IGNORE_CASE),
        Regex("\\bhum\\b[^.]{0,40}\\b(exhale|exhaled|exhaling|out-breath)\\b", RegexOption.IGNORE_CASE),
    )

    private fun matches(rules: List<Regex>, label: String, text: String): List<String> =
        rules.filter { it.containsMatchIn(text) }.map { "$label matches /${it.pattern}/: ${it.find(text)!!.value}" }

    private fun violations(label: String, text: String): List<String> = matches(pacing + claims, label, text)

    /** Rules for user-facing prose fields (title, essence, script). */
    private fun proseViolations(label: String, text: String): List<String> = matches(pacing + claims + timing, label, text)

    private data class CompiledTechnique(
        val id: String, val title: String, val essence: String, val duration: String, val script: String, val source: String,
    )

    /**
     * Mirror of scripts/compile-content.cjs: same file filter and sort, same `## ` section split, same
     * title prefix strip, same adaptation-header strip on the script, and the same source assembly from
     * the `**Verse` and `**Translation cited` lines only. Every other md section (What the verse says,
     * Modern Adaptation Notes, Category tags) is never read. [mdMirrorMatchesTheShippedAsset] proves the
     * mirror reproduces the asset exactly, so the md scan covers the same text the compiler ships.
     */
    private fun compiledFromMarkdown(): List<CompiledTechnique> {
        val dir = File("../../shanivar-vbt/content")
        assertTrue("missing ${dir.absolutePath}", dir.isDirectory)
        fun section(text: String, name: String): String {
            val marker = "## $name"
            val start = text.indexOf(marker)
            require(start >= 0) { "Missing section: $name" }
            return text.substring(start + marker.length).split(Regex("\\r?\\n## "))[0].trim()
        }
        return dir.listFiles()!!.map { it.name }.filter { Regex("^technique-\\d+.*\\.md$").matches(it) }.sorted().map { name ->
            val text = File(dir, name).readText()
            val primary = section(text, "Primary Source").split(Regex("\\r?\\n"))
            val verse = primary.first { it.contains("**Verse") }
            val edition = primary.first { it.contains("**Translation cited") }
            CompiledTechnique(
                id = name.removeSuffix(".md"),
                title = text.split(Regex("\\r?\\n"))[0].replaceFirst(Regex("^# Technique \\d+: "), ""),
                essence = section(text, "Essence"),
                duration = section(text, "Suggested Duration"),
                script = section(text, "Guided Session Script").replaceFirst(Regex("^\\*\\(Adaptation[\\s\\S]*?\\)\\*\\s*"), ""),
                source = listOf(verse, edition).joinToString("\n\n").replace(Regex("(?m)^- "), "").replace("**", "") +
                    "\n\nGuided text is a modern adaptation, not a literal translation.",
            )
        }
    }

    private fun compiledFromAsset(): List<CompiledTechnique> {
        val json = JSONArray(asset("techniques.json"))
        return (0 until json.length()).map { json.getJSONObject(it) }.map {
            CompiledTechnique(it.getString("id"), it.getString("title"), it.getString("essence"), it.getString("duration"),
                it.getString("script"), it.getString("source"))
        }
    }

    private fun techniqueViolations(origin: String, items: List<CompiledTechnique>): List<String> = items.flatMap { t ->
        val label = "$origin:${t.id}"
        proseViolations("$label.title", t.title) + proseViolations("$label.essence", t.essence) +
            proseViolations("$label.script", t.script) +
            // Metadata duration: pacing and claim rules only; a plain "3–5 minutes" is its purpose.
            violations("$label.duration", t.duration) +
            matches(sourceRules, "$label.source", t.source)
    }

    @Test
    fun mdMirrorMatchesTheShippedAsset() {
        val md = compiledFromMarkdown()
        assertEquals(12, md.size)
        assertEquals(compiledFromAsset(), md)
    }

    @Test
    fun markdownSourcesNeitherPaceNorClaimNorTime() {
        val found = techniqueViolations("md", compiledFromMarkdown())
        assertTrue(found.joinToString("\n"), found.isEmpty())
    }

    @Test
    fun techniquesNeitherPaceTheBreathNorMakeBodyClaims() {
        val json = JSONArray(asset("techniques.json"))
        assertEquals(12, json.length())
        val found = techniqueViolations("json", compiledFromAsset())
        assertTrue(found.joinToString("\n"), found.isEmpty())
        val pause = (0 until json.length()).map { json.getJSONObject(it) }.single { it.getString("id") == "technique-02-tranquil-retention" }
        assertEquals("Resting in the Pause", pause.getString("title"))
    }

    @Test
    fun breathingCatalogueGuidanceNeitherPacesNorClaims() {
        val root = JSONObject(asset(BreathingCatalog.ASSET_NAME))
        val found = mutableListOf<String>()
        found += proseViolations("globalGuidance", root.getString("globalGuidance"))
        val practices = root.getJSONArray("practices")
        for (i in 0 until practices.length()) {
            val p = practices.getJSONObject(i)
            found += proseViolations("${p.getString("id")}.title", p.getString("title"))
            val stages = p.getJSONArray("stages")
            for (s in 0 until stages.length()) {
                val stage = stages.getJSONObject(s)
                found += proseViolations("${p.getString("id")}.${stage.getString("id")}", stage.getString("text"))
            }
        }
        assertTrue(found.joinToString("\n"), found.isEmpty())
    }

    @Test
    fun patternsCatchTheOriginalBoxBreathingScript() {
        val original = "We're going to breathe in a simple, even pattern: in for a count of four, hold gently for four. " +
            "Breathe in... two, three, four. The sense of the nervous system settling."
        assertTrue(violations("fixture", original).size >= 3)
        assertTrue(violations("fixture", "Hold the breath for 4 seconds.").isNotEmpty())
        listOf(
            "the space breath leaves behind twice a minute", "Do this for six to eight rounds.", "four or five rounds",
            "a long, slow hum", "this one is about *extending* it", "Tranquil Retention", "tends to have a calming effect",
            "the breath-power settles", "Around the third or fourth round",
            "attention placed in an already-still point steadies faster", "trains a kind of sustained, effortless attention",
            "at some point it will start to shift", "calmer, looser, more settled", "some people notice their peripheral vision opening up",
        ).forEach { assertTrue(it, violations("fixture", it).isNotEmpty()) }
        assertTrue(sourceRules.any { it.containsMatchIn("Wallis's separate 2022 blog post, which independently renders the same verse: the breath-power") })
        listOf(
            "the nature of one's innate being may manifest", "that joy will become sublime", "Reality shines in the Center",
            "that [liminal] mental state is attainable", "one attains the 'form' of Bhairava",
            "then arises the state of being in which mental-emotional churning is feeble or absent",
            "reality is that which remains", "becomes soothed & still... and one attains the Divine Flood [of Bliss]",
        ).forEach { q -> assertTrue(q, sourceRules.any { it.containsMatchIn(q) }) }
        listOf(
            "Notice the breath as it is, without counting or controlling it.",
            "notice whether your internal state has shifted at all, and if so, how.",
            "Notice whether the feeling has some shape to it — whether it rises, holds, or shifts at all, even slightly.",
            "asks for a kind of sustained, effortless attention", "once you've calmed down", "Don't fight the drowsiness",
        ).forEach { assertTrue(it, violations("fixture", it).isEmpty()) }
        listOf(
            "the hum on later rounds", "On the last couple of rounds", "On the last round,", "Round two",
            "stay seated in the resulting quiet for another thirty seconds", "sit for a few more seconds", "another minute",
            "that half-second after the in-breath", "maybe a fraction of a second at first", "For the first minute,",
            "for the next few minutes", "for a minute or two", "In the final minute,", "look at it for a few seconds",
            "stay for a second.", "a second or two", "for 30 seconds", "As you exhale, make a soft, low hum",
            "a hum on the exhale", "uses an exhaled hum",
        ).forEach { assertTrue(it, proseViolations("fixture", it).isNotEmpty()) }
        listOf(
            "Take a second bite or sip, slower this time", "seeing \"around\" the point", "the surroundings",
            "let a hum sound naturally, without changing the breath", "Stay like this for a while.", "Toward the end,",
            "stay with that edge for a moment", "On an easy out-breath, try a gentle mmm.",
        ).forEach { assertTrue(it, proseViolations("fixture", it).isEmpty()) }
    }
}
