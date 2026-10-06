package com.shanivar.nativeapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Wish-list B2 (decision 0013 Option A): a date on each journal row, and the one quiet fact derived
 * from it - when someone last sat with each practice. Robolectric only because the journal is JSON in
 * SharedPreferences; the derivation and wording are pure.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BreathingHistoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val zone = BreathingFixtures.ZONE
    private fun at(day: Int, hour: Int = 9, month: Int = 10) =
        ZonedDateTime.of(2026, month, day, hour, 0, 0, 0, zone).toInstant().toEpochMilli()

    private fun record(id: String, practiceId: String = "external_grounding", at: Long? = null) =
        BreathingRecord(id, practiceId, "v1", 1, BreathingEntry.HOME, 60, 60, BreathingPhase.ENDED_TIMER,
            practisedAtEpochMillis = at)

    @Before fun clear() {
        context.getSharedPreferences(BreathingPreferences.NAMESPACE, 0).edit().clear().commit()
    }

    @Test fun aDatedRecordRoundTripsThroughStorage() {
        val prefs = BreathingPreferences(context)
        val saved = record("s1", at = at(5)).copy(feedback = BreathingFeedback.HELPED)
        prefs.append(saved)
        assertEquals(listOf(saved), prefs.journal())
        // Rewriting for feedback keeps the date.
        prefs.setFeedbackOnLatest("s1", BreathingFeedback.NEUTRAL)
        assertEquals(at(5), prefs.journal().single().practisedAtEpochMillis)
    }

    @Test fun legacyRecordsWithoutADateStillLoadAndSurviveARewrite() {
        // Exactly the shape written before B2: no date key at all, plus one explicit JSON null.
        val legacy = """[
            {"sessionId":"old1","practiceId":"external_grounding","catalogVersion":"v1","contentVersion":1,
             "entryContext":"HOME","selectedDurationSeconds":60,"activeSeconds":60,"outcome":"ENDED_TIMER"},
            {"sessionId":"old2","practiceId":"natural_anchor","catalogVersion":"v1","contentVersion":1,
             "entryContext":"HOME","selectedDurationSeconds":180,"activeSeconds":42,"outcome":"ENDED_USER",
             "feedback":"HELPED","practisedAtEpochMillis":null}
        ]"""
        context.getSharedPreferences(BreathingPreferences.NAMESPACE, 0).edit().putString("journal", legacy).commit()
        val prefs = BreathingPreferences(context)
        val loaded = prefs.journal()
        assertEquals(listOf("old1", "old2"), loaded.map { it.sessionId })
        assertTrue(loaded.all { it.practisedAtEpochMillis == null })
        assertEquals(42, loaded[1].activeSeconds)
        assertEquals(BreathingFeedback.HELPED, loaded[1].feedback)

        prefs.append(record("new", at = at(6)))
        val after = prefs.journal()
        assertEquals(listOf("old1", "old2", "new"), after.map { it.sessionId })
        assertEquals(listOf(null, null, at(6)), after.map { it.practisedAtEpochMillis })
    }

    @Test fun aSessionLostToProcessDeathKeepsItsStartDateWhileLegacyMarkersStayUndated() {
        val prefs = BreathingPreferences(context)
        prefs.markInFlight(record("s1", at = at(5, hour = 23)).copy(outcome = BreathingPhase.RUNNING))
        assertTrue(prefs.settleInFlight())
        assertEquals(at(5, hour = 23), prefs.journal().single().practisedAtEpochMillis)

        context.getSharedPreferences(BreathingPreferences.NAMESPACE, 0).edit()
            .putString("inFlight", """{"sessionId":"s2","practiceId":"natural_anchor","selectedDurationSeconds":60}""")
            .commit()
        assertTrue(prefs.settleInFlight())
        assertNull(prefs.journal().last().practisedAtEpochMillis)
    }

    @Test fun lastPractisedIsTheNewestDatedRowPerPracticeInLocalTime() {
        val records = listOf(
            record("a", "external_grounding", at(3)),
            record("b", "external_grounding", at(5)),
            record("c", "external_grounding", at(4)),
            record("d", "natural_anchor", null),
            // 23:30 UTC on 1 Oct is already 2 Oct in Berlin.
            record("e", "sleep_threshold", ZonedDateTime.of(2026, 10, 1, 23, 30, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()),
        )
        val dates = lastPractisedDates(records, zone)
        assertEquals(LocalDate.of(2026, 10, 5), dates["external_grounding"])
        assertEquals(LocalDate.of(2026, 10, 2), dates["sleep_threshold"])
        // Undated legacy rows say nothing, rather than a guessed date.
        assertNull(dates["natural_anchor"])
        assertEquals(setOf("external_grounding", "sleep_threshold"), dates.keys)
    }

    @Test fun theLabelIsTodayYesterdayOrAPlainDateAndNothingWhenUnknown() {
        val today = LocalDate.of(2026, 10, 6)
        assertEquals("Last practised today", lastPractisedLabel(today, today))
        assertEquals("Last practised yesterday", lastPractisedLabel(today.minusDays(1), today))
        assertEquals("Last practised 3 Oct", lastPractisedLabel(LocalDate.of(2026, 10, 3), today))
        assertEquals("Last practised 28 Dec 2025", lastPractisedLabel(LocalDate.of(2025, 12, 28), today))
        assertNull(lastPractisedLabel(null, today))
    }

    @Test fun theHistoryCapStillHoldsWithDatedRows() {
        val prefs = BreathingPreferences(context)
        (1..105).forEach { prefs.append(record("s$it", at = at(1) + it)) }
        val kept = prefs.journal()
        assertEquals(MAX_BREATHING_RECORDS, kept.size)
        assertEquals("s6", kept.first().sessionId)
        assertEquals(at(1) + 105, kept.last().practisedAtEpochMillis)
    }
}
