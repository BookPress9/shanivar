package com.shanivar.nativeapp

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.time.ZonedDateTime
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
 * Sections E and F of VBT_BREATHING_TEST_GUIDE_2026-09-25.md, as far as a JVM test can reach.
 *
 * What it can establish: that the player is reachable, that Start is withheld until the setting is
 * confirmed, that ending is immediate and recorded once, and - structurally - that nothing in the
 * breathing code can touch alarms or focus protection.
 *
 * What it cannot establish, and the release gate stays open for: that a speaker actually went quiet,
 * that an alarm is audible over a tone, or how any of this behaves under a real lock screen or an
 * OEM battery manager. Those need a physical phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class BreathingUiTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private var clock = 0L

    @Before fun resetState() {
        context.getSharedPreferences(BreathingPreferences.NAMESPACE, 0).edit().clear().commit()
        BreathingSessionOwner.resetForTest()
        clock = 0L
        BreathingSessionOwner.monotonicMillis = { clock }
        BreathingSessionOwner.wallClock = { ZonedDateTime.of(2026, 9, 25, 14, 0, 0, 0, BreathingFixtures.ZONE) }
        BreathingSessionOwner.newSessionId = { "test-session" }
    }

    @After fun clearState() = BreathingSessionOwner.resetForTest()

    private fun openHost(entry: BreathingEntry = BreathingEntry.HOME, onExit: () -> Unit = {}) {
        compose.setContent { ShanivarTheme { BreathingHost(entry, onExit = onExit) } }
    }

    private fun confirmSafeSetting() {
        compose.onNodeWithText("I'm somewhere safe and still").performScrollTo().performClick()
    }

    @Test fun startIsWithheldUntilTheSettingIsConfirmedAndComingBackLaterAlwaysWorks() {
        var exited = false
        openHost(onExit = { exited = true })
        compose.onAllNodesWithText("Start").assertCountEquals(0)
        compose.onNodeWithText("Come back later").performScrollTo().performClick()
        assertTrue(exited)
    }

    @Test fun theGroundingExerciseIsOfferedFirstAndNeverClaimsATraditionalSource() {
        openHost()
        confirmSafeSetting()
        compose.onNodeWithText("Notice the room").assertExists()
        // Its own card says "modern exercise". Other cards legitimately do cite a verse, so the
        // per-practice attribution is asserted in BreathingCatalogTest rather than by a screen-wide
        // search for "VBT" here.
        compose.onNodeWithText("Modern attention exercise").assertExists()
    }

    @Test fun aPracticeRunsPausesResumesAndEndsWithOneJournalRecord() {
        openHost()
        confirmSafeSetting()
        compose.onAllNodesWithText("Start").onFirst().performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(BreathingPhase.RUNNING, BreathingSessionOwner.session?.phase)
        // Opened from Sadhana: the 3-minute default (founder, 6 Oct 2026).
        assertEquals(SADHANA_DEFAULT_SECONDS, BreathingSessionOwner.session?.durationSeconds)

        clock = 20_000
        compose.onNodeWithText("Pause").performScrollTo().performClick()
        assertEquals(BreathingPhase.PAUSED, BreathingSessionOwner.session?.phase)
        clock = 60_000
        compose.onNodeWithText("Resume").performScrollTo().performClick()
        // Resuming continues the remaining time; it does not restart the practice.
        assertEquals(160_000L, BreathingSessionOwner.session?.remainingMillis(60_000))

        compose.onNodeWithText("End").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(BreathingPhase.ENDED_USER, BreathingSessionOwner.session?.phase)
        val journal = BreathingPreferences(context).journal()
        assertEquals(1, journal.size)
        assertEquals(BreathingPhase.ENDED_USER, journal.single().outcome)
        assertEquals(BreathingEntry.HOME, journal.single().entryContext)
        // B2: the row is dated from the injected wall clock, never from the monotonic one.
        assertEquals(BreathingSessionOwner.wallClock().toInstant().toEpochMilli(), journal.single().practisedAtEpochMillis)
    }

    @Test fun b2_eachPracticeShowsOnlyAQuietDateOfWhenItWasLastPractised() {
        val yesterday = BreathingSessionOwner.wallClock().minusDays(1).toInstant().toEpochMilli()
        BreathingPreferences(context).append(
            BreathingRecord("s0", "external_grounding", "v1", 1, BreathingEntry.HOME, 180, 180, BreathingPhase.ENDED_TIMER,
                practisedAtEpochMillis = yesterday)
        )
        openHost()
        confirmSafeSetting()
        compose.onNodeWithText("Last practised yesterday").assertExists()
        // One fact for one practice; never a count, run or score.
        compose.onAllNodesWithText("Last practised", substring = true).assertCountEquals(1)
        listOf("streak", "in a row", "XP", "badge", "score").forEach {
            compose.onAllNodesWithText(it, substring = true, ignoreCase = true).assertCountEquals(0)
        }
    }

    @Test fun i12_aSessionLostToProcessDeathIsRecordedOnceAsInterruptedAndNeverResumed() {
        val prefs = BreathingPreferences(context)
        openHost()
        confirmSafeSetting()
        compose.onAllNodesWithText("Start").onFirst().performScrollTo().performClick()
        compose.waitForIdle()
        // Simulate the process dying: the owner's in-memory state is gone, the in-flight marker is not.
        // Calling load() directly is what the next launch does; the Compose rule allows only one
        // setContent per test, so the screen cannot simply be reopened here.
        BreathingSessionOwner.resetForTest()
        BreathingSessionOwner.monotonicMillis = { clock }
        BreathingSessionOwner.load(context, BreathingEntry.HOME)
        assertNull(BreathingSessionOwner.session)
        assertTrue(BreathingSessionOwner.interruptedEarlier)
        assertEquals(listOf(BreathingPhase.INTERRUPTED), prefs.journal().map { it.outcome })
        // Nothing resumed, and no elapsed time was invented from a clock that resets on reboot.
        assertEquals(0, prefs.journal().single().activeSeconds)
    }

    @Test fun i09_anAlarmInterruptsThePracticeAndTheInterruptionIsRecordedTruthfully() {
        openHost()
        confirmSafeSetting()
        compose.onAllNodesWithText("Start").onFirst().performScrollTo().performClick()
        compose.waitForIdle()
        clock = 10_000
        // What AlarmActivity.onCreate does, after the ringer has already started.
        BreathingSessionOwner.interrupt(context)
        assertEquals(BreathingPhase.INTERRUPTED, BreathingSessionOwner.session?.phase)
        assertEquals(BreathingPhase.INTERRUPTED, BreathingPreferences(context).journal().single().outcome)
        // No automatic resumption follows a dismissal.
        BreathingSessionOwner.resume()
        assertEquals(BreathingPhase.INTERRUPTED, BreathingSessionOwner.session?.phase)
    }

    @Test fun i14_clearingPracticeHistoryTouchesNothingBelongingToAlarmsOrFocus() {
        val rhythm = context.getSharedPreferences("rhythm", 0)
        rhythm.edit().putString("language", "hi").putBoolean("introduced", true).commit()
        val guard = context.getSharedPreferences("focus_guard", 0)
        guard.edit().putLong("endsAt", 4_102_444_800_000L).commit()
        val prefs = BreathingPreferences(context)
        prefs.soundConsent = true
        prefs.append(
            BreathingRecord("s1", "external_grounding", "v1", 1, BreathingEntry.HOME, 60, 60, BreathingPhase.ENDED_TIMER)
        )

        prefs.clearAll()

        assertEquals(emptyList<BreathingRecord>(), prefs.journal())
        assertFalse(prefs.soundConsent)
        assertEquals("hi", rhythm.getString("language", null))
        assertTrue(rhythm.getBoolean("introduced", false))
        assertEquals(4_102_444_800_000L, guard.getLong("endsAt", 0L))
    }

    @Test fun f_theFeltWorseAnswerSuppressesTheSuggestionWithoutSavingAnythingAboutTheFeeling() {
        openHost()
        confirmSafeSetting()
        compose.onAllNodesWithText("Start").onFirst().performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("End").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Felt worse").performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue("external_grounding" in BreathingSessionOwner.draft.feltWorseIds)
        // The judgement is attached to the session record, not to a stored profile of the person.
        assertEquals(BreathingFeedback.FELT_WORSE, BreathingPreferences(context).journal().single().feedback)
        val stored = context.getSharedPreferences(BreathingPreferences.NAMESPACE, 0).all.toString()
        listOf("scattered", "tense", "agitated", "feeling").forEach { assertFalse(it in stored) }
    }

    @Test fun theSadhanaTabPreselectsThreeMinutesAndStillOffersOneThreeAndFive() {
        openHost(BreathingEntry.HOME)
        confirmSafeSetting()
        compose.waitForIdle()
        assertEquals(180, BreathingSessionOwner.draft.budgetSeconds)
        // The chips stay 1 / 3 / 5; the practice cards show the length they will run at, now 3 min.
        listOf("1 min", "3 min", "5 min").forEach { compose.onAllNodesWithText(it).onFirst().assertExists() }
        assertTrue(compose.onAllNodesWithText("3 min").fetchSemanticsNodes().size >= 2)
        compose.onAllNodesWithText("1 min").assertCountEquals(1)
    }

    @Test fun aLengthThePersonChoseWinsOverTheDefaultAndSurvivesReEntry() {
        openHost(BreathingEntry.HOME)
        confirmSafeSetting()
        compose.onAllNodesWithText("1 min").onFirst().performScrollTo().performClick()
        compose.waitForIdle()
        // What a rotation or returning to the same host does.
        BreathingSessionOwner.load(context, BreathingEntry.HOME)
        assertEquals(60, BreathingSessionOwner.draft.budgetSeconds)
        compose.onAllNodesWithText("Start").onFirst().performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(60, BreathingSessionOwner.session?.durationSeconds)
    }

    @Test fun theBlockedAppHostStaysAOneMinutePauseThatIsSavedButNotLastPractised() {
        openHost(BreathingEntry.BLOCK_OVERLAY)
        confirmSafeSetting()
        compose.onAllNodesWithText("Start").onFirst().performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(PAUSE_DEFAULT_SECONDS, BreathingSessionOwner.session?.durationSeconds)
        clock = 60_000
        compose.runOnIdle { BreathingSessionOwner.tick(context) }
        compose.waitForIdle()
        val row = BreathingPreferences(context).journal().single()
        assertEquals(BreathingPhase.ENDED_TIMER, row.outcome)
        assertEquals(BreathingEntry.BLOCK_OVERLAY, row.entryContext)
        assertEquals(60, row.activeSeconds)
        assertFalse(row.countsAsPractised)
        compose.onAllNodesWithText("Last practised", substring = true).assertCountEquals(0)
    }

    /**
     * A source-level structural check, and deliberately so.
     *
     * The invariant is "breathing code cannot release focus or dismiss an alarm". Reflection cannot
     * see a call that constructs its own engine, and a behavioural test can only show that one path
     * did not make the call. What actually guarantees it is that these types are never named in these
     * files at all - so that is what this asserts. If someone later needs enforcement in the player,
     * this test fails first and the handoff's section 6 gets re-read before it is written.
     */
    @Test fun e_noBreathingSourceFileCanNameTheEnforcementSystemsAtAll() {
        val forbidden = listOf(
            "DisciplineGateway", "DisciplineEngine", "releaseFocus", "startFocus",
            "AlarmScheduler", "FocusScheduler", "AlarmForegroundService", "AlarmRinger",
        )
        val sources = File("src/main/java/com/shanivar/nativeapp")
            .listFiles { file -> file.name.startsWith("Breathing") && file.extension == "kt" }
            .orEmpty()
        assertTrue("breathing sources not found from the test working directory", sources.size >= 6)
        sources.forEach { file ->
            val body = file.readText()
                // Doc comments may explain the boundary; code may not cross it.
                .lines().filterNot { it.trimStart().startsWith("*") || it.trimStart().startsWith("//") }
                .joinToString("\n")
            forbidden.forEach { symbol ->
                assertFalse("${file.name} references $symbol", body.contains(symbol))
            }
        }
    }
}
