package com.shanivar.nativeapp

import java.util.UUID

/*
 * Initiation V1, build step 4 (HANDOFF.md:102): practice attempt records for the 60-second start practice.
 * Monotonic, foreground-only time (pass SystemClock.elapsedRealtime()). Backgrounding pauses. Time while
 * closed or killed is never counted, and completion is only ever reached by active foreground seconds.
 * Ending early records ENDED, never COMPLETED. No score, no streak. Content and release gates of the
 * existing Steady player are unchanged; this only records what happened.
 */

enum class AttemptOutcome { IN_PROGRESS, COMPLETED, ENDED }

data class PracticeAttempt(
    val attemptId: String,
    val contentVersion: String,
    val startedAtMillis: Long,
    val activeMillis: Long = 0,
    val outcome: AttemptOutcome = AttemptOutcome.IN_PROGRESS,
    val endedAtMillis: Long? = null,
) {
    val activeSeconds: Long get() = activeMillis / 1000
}

class PracticeAttemptTracker(
    private val targetMillis: Long = 60_000,
    private val persist: (PracticeAttempt) -> Unit,
) {
    var attempt: PracticeAttempt? = null
        private set
    private var runningSince: Long? = null

    val isRunning: Boolean get() = runningSince != null

    /** Only after the stationary confirmation (PRACTICE_SAFETY:32). */
    fun start(stationaryConfirmed: Boolean, contentVersion: String, wallNow: Long, mono: Long, id: String = UUID.randomUUID().toString()): Boolean {
        if (!stationaryConfirmed || attempt?.outcome == AttemptOutcome.IN_PROGRESS) return false
        attempt = PracticeAttempt(id, contentVersion, wallNow).also(persist)
        runningSince = mono
        return true
    }

    /** Restore after process death: never resumes running, never adds time spent closed. */
    fun restore(saved: PracticeAttempt) { attempt = saved; runningSince = null }

    fun pause(mono: Long) = fold(mono)?.let { persist(it) }
    fun onBackground(mono: Long) = pause(mono)

    fun resume(mono: Long) {
        if (attempt?.outcome == AttemptOutcome.IN_PROGRESS && runningSince == null) runningSince = mono
    }

    /** Call from the UI clock. Returns remaining millis. */
    fun tick(mono: Long, wallNow: Long): Long {
        val a = attempt ?: return targetMillis
        val running = runningSince?.let { mono - it } ?: 0
        val active = a.activeMillis + running
        if (a.outcome == AttemptOutcome.IN_PROGRESS && runningSince != null && active >= targetMillis) {
            runningSince = null
            attempt = a.copy(activeMillis = targetMillis, outcome = AttemptOutcome.COMPLETED, endedAtMillis = wallNow).also(persist)
            return 0
        }
        return (targetMillis - active).coerceAtLeast(0)
    }

    /** End is always available and immediate. */
    fun end(mono: Long, wallNow: Long) {
        fold(mono)
        val a = attempt ?: return
        if (a.outcome != AttemptOutcome.IN_PROGRESS) return
        attempt = a.copy(outcome = AttemptOutcome.ENDED, endedAtMillis = wallNow).also(persist)
    }

    private fun fold(mono: Long): PracticeAttempt? {
        val a = attempt ?: return null
        val since = runningSince ?: return a
        runningSince = null
        val folded = a.copy(activeMillis = (a.activeMillis + (mono - since)).coerceAtMost(targetMillis))
        attempt = folded
        return folded
    }
}
