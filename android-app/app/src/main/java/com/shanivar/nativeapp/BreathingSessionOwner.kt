package com.shanivar.nativeapp

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Founder decision, 6 Oct 2026 (Sadhana proposal, 60 vs 90, option A): a practice opened from the
 * Sadhana tab defaults to 3 minutes, so a default session that runs to the end clears
 * [MIN_PRACTISED_SECONDS]. Every catalogue practice offers 180 s.
 */
const val SADHANA_DEFAULT_SECONDS = 180

/**
 * The short host entries - after the alarm, before focus, from a blocked app - stay one-minute pauses.
 * They are journalled like any other attempt, but at 60 s they never show as "Last practised", and
 * that is intended: a pause is not a practice.
 */
const val PAUSE_DEFAULT_SECONDS = 60

/** The length preselected when a host opens the player. Exhaustive on purpose: a new entry point has
 * to decide whether it is a practice or a pause. */
fun defaultBudgetSeconds(entry: BreathingEntry): Int = when (entry) {
    BreathingEntry.HOME -> SADHANA_DEFAULT_SECONDS
    BreathingEntry.BEFORE_FOCUS,
    BreathingEntry.BLOCK_OVERLAY,
    BreathingEntry.AFTER_ALARM,
    BreathingEntry.PREVIEW -> PAUSE_DEFAULT_SECONDS
}

/** Everything the person answered this visit. Ephemeral by design - never written to storage. */
data class BreathingDraft(
    val feeling: BreathingFeeling? = null,
    val context: BreathingContext? = null,
    val sleepIntent: Boolean = false,
    val sleepyNow: Boolean = false,
    val breathComfort: Boolean? = null,
    val budgetSeconds: Int? = SADHANA_DEFAULT_SECONDS,
    /** True once the person picked a length this visit. Their choice then wins over the entry default
     * (a rotation or re-entry into the same host must not reset it). Ephemeral like the rest. */
    val budgetChosen: Boolean = false,
    val safeSettingConfirmed: Boolean = false,
    val feltWorseIds: Set<String> = emptySet(),
)

/**
 * The one owner of the one active breathing session in this process.
 *
 * A process singleton rather than a ViewModel, and that is a deliberate trade. The player is reached
 * from three different Activities - Home, [BlockOverlayActivity] and the post-alarm done screen - so
 * an Activity-scoped ViewModel would give each host its own session, its own timer and its own tone.
 * The test guide's S13 forbids exactly that ("one owner ... no stacked timer/tone"), and a singleton
 * is the only way to make it true rather than approximately true. Rotation is preserved as a side
 * effect, since the state never belonged to the Activity in the first place.
 *
 * The cost is that this object outlives any screen, so it holds no Context, no Activity and no
 * gateway - a Context arrives as a method parameter and is used immediately. Which brings up the
 * other structural point: there is no reference to [DisciplineGateway], [AlarmScheduler] or
 * [FocusScheduler] anywhere in this file. The player cannot release protection or dismiss an alarm
 * because it has no way to name them (handoff section 6).
 */
object BreathingSessionOwner {
    var catalogue by mutableStateOf<BreathingCatalogue?>(null)
        private set

    /** Non-empty when content failed validation. The UI shows an unavailable state, never a guess. */
    var contentProblems by mutableStateOf<List<String>>(emptyList())
        private set

    var draft by mutableStateOf(BreathingDraft())
    var session by mutableStateOf<BreathingSession?>(null)
        private set
    var practice by mutableStateOf<BreathingPractice?>(null)
        private set
    var entry by mutableStateOf(BreathingEntry.HOME)
        private set

    /** True after a session was lost to process death; the UI explains it once and never resumes. */
    var interruptedEarlier by mutableStateOf(false)
        private set

    /** True when the app - not the person - paused the practice. Only changes the explanation. */
    var pausedByHost by mutableStateOf(false)
        private set

    /** Monotonic time. Injectable so the reducer tests and Robolectric UI tests can drive it. */
    var monotonicMillis: () -> Long = { SystemClock.elapsedRealtime() }

    /** Local wall time, for schedule-aware *suggestions* and the date stamped on each journal record
     * (B2) only - never for duration. */
    var wallClock: () -> ZonedDateTime = { ZonedDateTime.now() }

    var newSessionId: () -> String = { UUID.randomUUID().toString() }

    private val tone = BreathingTonePlayer()

    fun load(context: Context, entryContext: BreathingEntry) {
        // The entry decides the preselected length: 3 min from Sadhana, a 1-minute pause from a host.
        // A length the person chose this visit is kept, unless a different host has opened the player.
        val entryChanged = entryContext != entry
        entry = entryContext
        if (entryChanged || !draft.budgetChosen) {
            draft = draft.copy(budgetSeconds = defaultBudgetSeconds(entryContext), budgetChosen = false)
        }
        if (catalogue == null && contentProblems.isEmpty()) {
            when (val result = BreathingCatalog.load(context)) {
                is BreathingCatalogResult.Valid -> catalogue = result.catalogue
                is BreathingCatalogResult.Invalid -> contentProblems = result.problems
            }
        }
        val prefs = BreathingPreferences(context)
        // A session that never reached a terminal state is recorded as interrupted exactly once. No
        // audio starts, nothing resumes, and nothing is presumed completed.
        if (session == null && prefs.settleInFlight()) interruptedEarlier = true
    }

    fun acknowledgeInterruption() { interruptedEarlier = false }

    fun suggestion(context: Context): BreathingSuggestion? =
        catalogue?.let { BreathingRecommender.suggest(it, request(context)) }

    fun request(context: Context): BreathingRequest {
        val prefs = BreathingPreferences(context)
        return BreathingRequest(
            feeling = draft.feeling,
            explicitContext = draft.context,
            sleepIntent = draft.sleepIntent,
            sleepyNow = draft.sleepyNow,
            breathComfort = draft.breathComfort,
            soundConsent = prefs.soundConsent,
            priorBreathComfort = prefs.priorBreathComfort,
            safeSettingConfirmed = draft.safeSettingConfirmed,
            budgetSeconds = draft.budgetSeconds,
            schedule = prefs.schedule(),
            now = wallClock(),
            soundAssetAvailable = tone.available(),
            feltWorseIds = draft.feltWorseIds,
        )
    }

    /** Prepares a session without starting it. Switching choice before Start leaves nothing running,
     * because the previous READY session is simply replaced (S14). */
    fun choose(selected: BreathingPractice, durationSeconds: Int) {
        val current = session
        if (current != null && !current.phase.terminal && current.phase != BreathingPhase.READY) return
        practice = selected
        session = BreathingSession.of(selected, durationSeconds, newSessionId())
    }

    fun start(context: Context) {
        val current = session ?: return
        if (current.phase != BreathingPhase.READY) return
        session = current.start(monotonicMillis())
        pausedByHost = false
        BreathingPreferences(context).markInFlight(recordFor(context, current, BreathingPhase.RUNNING))
    }

    /** Takes a Context because pausing at exactly zero finishes the session, which writes the one
     * journal record for it. */
    fun pause(context: Context) {
        val paused = session?.pause(monotonicMillis()) ?: return
        tone.stop()
        session = paused
        pausedByHost = false
        if (paused.phase.terminal) finish(context, paused)
    }

    fun resume() {
        pausedByHost = false
        session = session?.resume(monotonicMillis())
    }

    /** Immediate, idempotent, no confirmation dialog. */
    fun end(context: Context) {
        val ended = session?.endByUser(monotonicMillis()) ?: return
        tone.stop()
        session = ended
        finish(context, ended)
    }

    fun onHostBackgrounded(context: Context) {
        val current = session ?: return
        if (current.phase != BreathingPhase.RUNNING) return
        tone.stop()
        val paused = current.onHostBackgrounded(monotonicMillis())
        session = paused
        pausedByHost = paused.phase == BreathingPhase.PAUSED
    }

    /** An alarm firing, or any host with higher priority taking over. Never resumes afterwards. */
    fun interrupt(context: Context) {
        val current = session ?: return
        if (current.phase.terminal) return
        tone.stop()
        val interrupted = current.interrupt(monotonicMillis())
        session = interrupted
        finish(context, interrupted)
    }

    /** Advances the clock. Returns true when the session reached a terminal state on this tick. */
    fun tick(context: Context): Boolean {
        val current = session ?: return false
        val active = practice ?: return false
        if (!current.phase.counting) return false
        val advanced = current.tick(monotonicMillis())
        val withCue = maybePlayTone(active, advanced)
        session = withCue
        if (withCue.phase.terminal) {
            finish(context, withCue)
            return true
        }
        return false
    }

    /** The tone sounds once, on entering the practice stage. A cue missed while backgrounded is
     * never caught up - arriving back to a sound that belonged to two minutes ago is worse than
     * silence. */
    private fun maybePlayTone(active: BreathingPractice, state: BreathingSession): BreathingSession {
        if (active.presentation.toneAsset == null) return state
        if (state.phase != BreathingPhase.RUNNING) return state
        if (BreathingStageId.PRACTICE in state.cuesPlayed) return state
        val stage = state.currentStage(active, monotonicMillis())?.stage
        if (stage != BreathingStageId.PRACTICE) return state
        tone.playOnce()
        return state.withCuePlayed(BreathingStageId.PRACTICE)
    }

    fun recordFeedback(context: Context, feedback: BreathingFeedback) {
        val current = session ?: return
        BreathingPreferences(context).setFeedbackOnLatest(current.sessionId, feedback)
        // "Felt worse" suppresses this practice's automatic suggestion for the rest of the visit.
        // Manual selection stays available, and nothing about it is persisted.
        if (feedback == BreathingFeedback.FELT_WORSE) {
            draft = draft.copy(feltWorseIds = draft.feltWorseIds + current.practiceId)
        }
    }

    /** Clears the finished session so the next visit starts from selection. */
    fun clearSession() {
        tone.stop()
        session = null
        practice = null
    }

    /** Leaves the feature entirely: the draft's ephemeral answers go with it. */
    fun leave() {
        clearSession()
        draft = BreathingDraft(feltWorseIds = draft.feltWorseIds)
        pausedByHost = false
    }

    private fun finish(context: Context, state: BreathingSession) {
        val prefs = BreathingPreferences(context)
        prefs.clearInFlight()
        prefs.append(recordFor(context, state, state.phase))
    }

    private fun recordFor(context: Context, state: BreathingSession, outcome: BreathingPhase) =
        BreathingRecord(
            sessionId = state.sessionId,
            practiceId = state.practiceId,
            catalogVersion = catalogue?.catalogVersion.orEmpty(),
            contentVersion = practice?.contentVersion ?: 1,
            entryContext = entry,
            selectedDurationSeconds = state.durationSeconds,
            activeSeconds = (state.elapsedActiveMillis(monotonicMillis()) / 1000).toInt(),
            outcome = outcome,
            practisedAtEpochMillis = wallClock().toInstant().toEpochMilli(),
        )

    /** Test seam: resets process state between Robolectric cases. */
    fun resetForTest() {
        catalogue = null
        contentProblems = emptyList()
        draft = BreathingDraft()
        session = null
        practice = null
        entry = BreathingEntry.HOME
        interruptedEarlier = false
        pausedByHost = false
        monotonicMillis = { SystemClock.elapsedRealtime() }
        wallClock = { ZonedDateTime.now() }
    }
}
