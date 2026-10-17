package com.shanivar.nativeapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.ZonedDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Founder decision, 6 Oct 2026 (Sadhana proposal, 60 vs 90, option A): 3 minutes is the default when
 * a practice is opened from the Sadhana tab, the short host entries stay one-minute pauses, and the
 * 90-second "counts as practised" rule is unchanged.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BreathingDefaultDurationTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val owner = BreathingSessionOwner
    private var clock = 0L
    private val hosts = listOf(BreathingEntry.BEFORE_FOCUS, BreathingEntry.BLOCK_OVERLAY, BreathingEntry.AFTER_ALARM)

    @Before fun reset() {
        context.getSharedPreferences(BreathingPreferences.NAMESPACE, 0).edit().clear().commit()
        owner.resetForTest()
        clock = 0L
        owner.monotonicMillis = { clock }
        owner.wallClock = { ZonedDateTime.of(2026, 10, 6, 9, 0, 0, 0, BreathingFixtures.ZONE) }
        owner.newSessionId = { "s-$clock" }
    }

    @After fun clear() = owner.resetForTest()

    private fun shipped(): BreathingCatalogue {
        val raw = RuntimeEnvironment.getApplication().assets.open(BreathingCatalog.ASSET_NAME)
            .bufferedReader().use { it.readText() }
        return (BreathingCatalog.parse(raw) as BreathingCatalogResult.Valid).catalogue
    }

    @Test fun theRuleIsUnchangedAndTheDefaultsSitOnEitherSideOfIt() {
        assertEquals(90, MIN_PRACTISED_SECONDS)
        assertEquals(180, SADHANA_DEFAULT_SECONDS)
        assertEquals(60, PAUSE_DEFAULT_SECONDS)
        assertEquals(SADHANA_DEFAULT_SECONDS, defaultBudgetSeconds(BreathingEntry.HOME))
        assertTrue(defaultBudgetSeconds(BreathingEntry.HOME) >= MIN_PRACTISED_SECONDS)
        (hosts + BreathingEntry.PREVIEW).forEach {
            assertEquals(it.name, PAUSE_DEFAULT_SECONDS, defaultBudgetSeconds(it))
            assertTrue(defaultBudgetSeconds(it) < MIN_PRACTISED_SECONDS)
        }
        assertEquals(SADHANA_DEFAULT_SECONDS, BreathingDraft().budgetSeconds)
    }

    @Test fun everyShippedPracticeRunsThreeMinutesFromSadhanaAndOneMinuteFromAHost() {
        shipped().practices.forEach { practice ->
            assertEquals(practice.id, 180, practice.defaultDurationSeconds)
            assertEquals(practice.id, BreathingDuration.Ready(180, false), practice.resolveDuration(SADHANA_DEFAULT_SECONDS))
            assertEquals(practice.id, BreathingDuration.Ready(180, false), practice.resolveDuration(null))
            assertEquals(practice.id, BreathingDuration.Ready(60, false), practice.resolveDuration(PAUSE_DEFAULT_SECONDS))
        }
    }

    @Test fun loadingPreselectsTheEntryDefault() {
        owner.load(context, BreathingEntry.HOME)
        assertEquals(180, owner.draft.budgetSeconds)
        hosts.forEach {
            owner.load(context, it)
            assertEquals(it.name, 60, owner.draft.budgetSeconds)
        }
        owner.load(context, BreathingEntry.HOME)
        assertEquals(180, owner.draft.budgetSeconds)
    }

    @Test fun aChosenLengthWinsOnTheSameEntryAndLeavingStartsFresh() {
        owner.load(context, BreathingEntry.HOME)
        owner.draft = owner.draft.copy(budgetSeconds = 300, budgetChosen = true)
        owner.load(context, BreathingEntry.HOME)
        assertEquals(300, owner.draft.budgetSeconds)
        // A pause host opening the player keeps its own one-minute default.
        owner.load(context, BreathingEntry.BLOCK_OVERLAY)
        assertEquals(60, owner.draft.budgetSeconds)
        // A choice inside a host wins there too.
        owner.draft = owner.draft.copy(budgetSeconds = 180, budgetChosen = true)
        owner.load(context, BreathingEntry.BLOCK_OVERLAY)
        assertEquals(180, owner.draft.budgetSeconds)
        owner.leave()
        owner.load(context, BreathingEntry.HOME)
        assertEquals(180, owner.draft.budgetSeconds)
        assertFalse(owner.draft.budgetChosen)
    }

    private fun runToTheEnd(entry: BreathingEntry): BreathingRecord {
        owner.load(context, entry)
        owner.draft = owner.draft.copy(safeSettingConfirmed = true)
        val seconds = (BreathingFixtures.externalGrounding.resolveDuration(owner.draft.budgetSeconds) as BreathingDuration.Ready).seconds
        owner.choose(BreathingFixtures.externalGrounding, seconds)
        owner.start(context)
        clock += seconds * 1000L
        assertTrue(owner.tick(context))
        val row = BreathingPreferences(context).journal().last()
        owner.leave()
        return row
    }

    @Test fun aDefaultSadhanaSessionCountsAndAOneMinutePauseIsSavedButDoesNot() {
        val practice = runToTheEnd(BreathingEntry.HOME)
        assertEquals(180, practice.activeSeconds)
        assertTrue(practice.countsAsPractised)

        hosts.forEach { entry ->
            val pause = runToTheEnd(entry)
            assertEquals(entry, pause.entryContext)
            assertEquals(BreathingPhase.ENDED_TIMER, pause.outcome)
            assertEquals(60, pause.activeSeconds)
            assertFalse(entry.name, pause.countsAsPractised)
        }
        val journal = BreathingPreferences(context).journal()
        assertEquals(4, journal.size)
        // "Last practised" comes only from the Sadhana session, never from the later pauses.
        assertEquals(listOf("s-0"), journal.filter { it.countsAsPractised }.map { it.sessionId })
    }
}
