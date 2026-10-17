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

/**
 * DEV-39: bundled practice text must not pace the breath (counts, timed holds) and must not make
 * medical, physiological or energy claims. Scans every string in the shipped techniques.json and the
 * stage/guidance text of breathing-v1.json.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TechniqueContentSafetyTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun asset(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }

    private val breathWord = "(breath\\w*|breathe\\w*|inhal\\w*|exhal\\w*|in-breath|out-breath|hold\\w*|pause\\w*)"

    private val pacing = listOf(
        Regex("count of", RegexOption.IGNORE_CASE),
        Regex("\\bcounted\\b", RegexOption.IGNORE_CASE),
        Regex("hold (gently )?for", RegexOption.IGNORE_CASE),
        Regex("inhale for", RegexOption.IGNORE_CASE),
        Regex("exhale for", RegexOption.IGNORE_CASE),
        Regex("\\b(in|out) for (a count|one|two|three|four|five|six|seven|eight|\\d)", RegexOption.IGNORE_CASE),
        Regex("\\btwo, three, four\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(slow|deep|long) breaths?\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\d+\\s*(seconds?|counts?|beats?)\\b.{0,60}\\b$breathWord", RegexOption.IGNORE_CASE),
        Regex("\\b$breathWord\\b.{0,60}\\b\\d+\\s*(seconds?|counts?|beats?)\\b", RegexOption.IGNORE_CASE),
    )

    private val claims = listOf(
        "nervous system", "vagus", "vagal", "cortisol", "heart rate", "blood pressure", "energy center",
        "energy centre", "chakra",
    ).map { Regex(Regex.escape(it), RegexOption.IGNORE_CASE) } + listOf(
        Regex("\\bheal(s|ed|ing)?\\b", RegexOption.IGNORE_CASE),
        Regex("\\bcur(e|es|ed|ing)\\b", RegexOption.IGNORE_CASE),
    )

    private fun violations(label: String, text: String): List<String> =
        (pacing + claims).filter { it.containsMatchIn(text) }.map { "$label matches /${it.pattern}/: ${it.find(text)!!.value}" }

    @Test
    fun techniquesNeitherPaceTheBreathNorMakeBodyClaims() {
        val json = JSONArray(asset("techniques.json"))
        assertEquals(12, json.length())
        val found = (0 until json.length()).flatMap { i ->
            val item = json.getJSONObject(i)
            // The source field quotes the cited translation verbatim; it is citation, not guidance.
            listOf("title", "essence", "duration", "script").flatMap { key ->
                violations("${item.getString("id")}.$key", item.getString(key))
            }
        }
        assertTrue(found.joinToString("\n"), found.isEmpty())
    }

    @Test
    fun breathingCatalogueGuidanceNeitherPacesNorClaims() {
        val root = JSONObject(asset(BreathingCatalog.ASSET_NAME))
        val found = mutableListOf<String>()
        found += violations("globalGuidance", root.getString("globalGuidance"))
        val practices = root.getJSONArray("practices")
        for (i in 0 until practices.length()) {
            val p = practices.getJSONObject(i)
            found += violations("${p.getString("id")}.title", p.getString("title"))
            val stages = p.getJSONArray("stages")
            for (s in 0 until stages.length()) {
                val stage = stages.getJSONObject(s)
                found += violations("${p.getString("id")}.${stage.getString("id")}", stage.getString("text"))
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
        assertTrue(violations("fixture", "Notice the breath as it is, without counting or controlling it.").isEmpty())
    }
}
