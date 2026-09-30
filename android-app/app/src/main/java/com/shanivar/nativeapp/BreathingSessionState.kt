package com.shanivar.nativeapp

enum class BreathingPhase {
    READY,
    RUNNING,

    /** Paused by the person, or by the app losing the foreground/audio focus. Identical state
     * either way; only the explanation shown on return differs. */
    PAUSED,

    /** Sleep practice only: the instructions are read, the screen may go dark, and nothing further
     * is asked of the person. No wake lock, no notification, no closing sound. */
    RESTING_SILENTLY,

    /** The timer reached zero. This is the *only* automatic outcome, and it means exactly that -
     * never "the practice succeeded" and never "the person slept". */
    ENDED_TIMER,
    ENDED_USER,

    /** The process died, or a host with higher priority (an alarm) took over. Recorded truthfully
     * rather than resolved into a completion. */
    INTERRUPTED;

    val terminal: Boolean get() = this == ENDED_TIMER || this == ENDED_USER || this == INTERRUPTED
    val counting: Boolean get() = this == RUNNING || this == RESTING_SILENTLY
}

/**
 * The whole session, as pure data with a pure reducer.
 *
 * Time comes only from an injected monotonic source (`SystemClock.elapsedRealtime()` in the app).
 * Local wall time decides *which practice to suggest*; it must never decide *how much time is left*,
 * or a DST shift or a manual clock change would silently lengthen or truncate someone's practice
 * (test guide T08/T10). [activeMillis] accumulates only while counting, so a pause costs nothing.
 *
 * Every transition is idempotent in the direction that matters: a second Start does not create a
 * second timer, and a second End does not produce a second terminal record (S01/S04).
 */
data class BreathingSession(
    val sessionId: String,
    val practiceId: String,
    val isSleepPractice: Boolean,
    val durationSeconds: Int,
    val phase: BreathingPhase = BreathingPhase.READY,
    val activeMillis: Long = 0L,
    /** Monotonic reading of when the current counting stretch began; null whenever not counting. */
    val countingSinceMillis: Long? = null,
    /** Stage cues already delivered. Cues missed while backgrounded are never replayed on return. */
    val cuesPlayed: Set<BreathingStageId> = emptySet(),
    val interruptedWhileCounting: Boolean = false,
) {
    val durationMillis: Long get() = durationSeconds * 1000L

    fun elapsedActiveMillis(nowMillis: Long): Long =
        activeMillis + (countingSinceMillis?.let { (nowMillis - it).coerceAtLeast(0L) } ?: 0L)

    fun remainingMillis(nowMillis: Long): Long =
        (durationMillis - elapsedActiveMillis(nowMillis)).coerceIn(0L, durationMillis)

    fun expired(nowMillis: Long): Boolean = remainingMillis(nowMillis) == 0L

    /** Start counting. A second call is a no-op, which is what makes "one timer owner" true rather
     * than merely intended - a double tap on Start cannot produce two sessions. */
    fun start(nowMillis: Long): BreathingSession =
        if (phase != BreathingPhase.READY) this
        else copy(phase = BreathingPhase.RUNNING, countingSinceMillis = nowMillis)

    fun pause(nowMillis: Long): BreathingSession = when {
        !phase.counting -> this
        // Pausing at exactly zero must finish rather than park the session at 0:00 forever.
        expired(nowMillis) -> endByTimer(nowMillis)
        else -> settle(nowMillis).copy(phase = BreathingPhase.PAUSED)
    }

    /** Continues the remaining time. It never restarts the full duration. */
    fun resume(nowMillis: Long): BreathingSession = when {
        phase != BreathingPhase.PAUSED -> this
        expired(nowMillis) -> endByTimer(nowMillis)
        else -> copy(
            phase = if (isSleepPractice && pastSpokenStages(nowMillis)) BreathingPhase.RESTING_SILENTLY
            else BreathingPhase.RUNNING,
            countingSinceMillis = nowMillis,
        )
    }

    fun endByUser(nowMillis: Long): BreathingSession =
        if (phase.terminal) this else settle(nowMillis).copy(phase = BreathingPhase.ENDED_USER)

    fun endByTimer(nowMillis: Long): BreathingSession =
        if (phase.terminal) this
        else settle(nowMillis).copy(phase = BreathingPhase.ENDED_TIMER, activeMillis = durationMillis)

    /** An alarm firing, or a process that died while counting. Never resolves into a completion. */
    fun interrupt(nowMillis: Long): BreathingSession =
        if (phase.terminal) this
        else settle(nowMillis).copy(phase = BreathingPhase.INTERRUPTED, interruptedWhileCounting = phase.counting)

    /**
     * Advance the clock. Returns the same instance when nothing changed, so a caller can cheaply
     * detect a real transition.
     *
     * A sleep practice crosses into [BreathingPhase.RESTING_SILENTLY] by itself once the text has
     * been read: from there the person is meant to be able to put the phone down, and the silent
     * interval is allowed to elapse with the app in the background.
     */
    fun tick(nowMillis: Long): BreathingSession = when {
        !phase.counting -> this
        expired(nowMillis) -> endByTimer(nowMillis)
        isSleepPractice && phase == BreathingPhase.RUNNING && pastSpokenStages(nowMillis) ->
            copy(phase = BreathingPhase.RESTING_SILENTLY)
        else -> this
    }

    /**
     * The app lost the foreground, the screen locked, or audio focus went away. Ordinary practice
     * pauses and stops its cues; a sleep practice already resting keeps resting, because putting the
     * phone down is the instruction, not an interruption.
     */
    fun onHostBackgrounded(nowMillis: Long): BreathingSession =
        if (phase == BreathingPhase.RESTING_SILENTLY) this else pause(nowMillis)

    fun withCuePlayed(stage: BreathingStageId): BreathingSession = copy(cuesPlayed = cuesPlayed + stage)

    /** Which slice of the plan the accumulated active time falls in. */
    fun currentStage(practice: BreathingPractice, nowMillis: Long): BreathingStageSlice? {
        val elapsedSeconds = elapsedActiveMillis(nowMillis) / 1000
        var boundary = 0L
        practice.stagePlan(durationSeconds).forEach { slice ->
            boundary += slice.seconds
            if (elapsedSeconds < boundary) return slice
        }
        return practice.stagePlan(durationSeconds).lastOrNull()
    }

    private fun settle(nowMillis: Long): BreathingSession =
        copy(activeMillis = elapsedActiveMillis(nowMillis).coerceAtMost(durationMillis), countingSinceMillis = null)

    /** Arrival + instruction are the only stages a sleep practice asks anything of the person. */
    private fun pastSpokenStages(nowMillis: Long): Boolean =
        elapsedActiveMillis(nowMillis) >= SLEEP_SPOKEN_MILLIS

    companion object {
        /** arrival 10s + instruction 20s, per the catalogue's sleep stage timing. */
        const val SLEEP_SPOKEN_MILLIS = 30_000L

        fun of(practice: BreathingPractice, durationSeconds: Int, sessionId: String): BreathingSession =
            BreathingSession(
                sessionId = sessionId,
                practiceId = practice.id,
                isSleepPractice = practice.isSleepPractice,
                durationSeconds = durationSeconds,
            )
    }
}

/**
 * One bounded local journal row. What is *not* here is the point: no raw feeling label, no free
 * text, no blocked package name, nothing that could be uploaded to describe someone's inner state
 * (handoff section 5). [entryContext] records which host opened the player, which is a navigation
 * fact, not a mood.
 */
data class BreathingRecord(
    val sessionId: String,
    val practiceId: String,
    val catalogVersion: String,
    val contentVersion: Int,
    val entryContext: BreathingEntry,
    val selectedDurationSeconds: Int,
    val activeSeconds: Int,
    val outcome: BreathingPhase,
    val feedback: BreathingFeedback? = null,
)

enum class BreathingEntry { HOME, BEFORE_FOCUS, BLOCK_OVERLAY, AFTER_ALARM, PREVIEW }

enum class BreathingFeedback { HELPED, NEUTRAL, FELT_WORSE }

/** Keeps the newest [MAX_BREATHING_RECORDS] rows. A journal that grows without bound is a privacy
 * problem disguised as a feature. */
fun trimBreathingJournal(records: List<BreathingRecord>): List<BreathingRecord> =
    if (records.size <= MAX_BREATHING_RECORDS) records else records.takeLast(MAX_BREATHING_RECORDS)

const val MAX_BREATHING_RECORDS = 100
