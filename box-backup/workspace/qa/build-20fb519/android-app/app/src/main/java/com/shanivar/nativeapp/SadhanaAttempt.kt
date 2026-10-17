package com.shanivar.nativeapp

import java.util.UUID

/*
 * Sadhana V1, build step S2 (HANDOFF "Domain model", "Event semantics"). Generalises the 60-second
 * PracticeAttemptTracker to any reviewed duration. Active time uses a monotonic clock (pass
 * SystemClock.elapsedRealtime()) and only advances while running in the foreground. Pauses, background,
 * anchor changes and closed time never count. Process death is reconciled as INTERRUPTED. Completion
 * is reached only by active seconds in this tracker, never by wall-clock expiry. One practice at a time.
 */

enum class SadhanaOutcome { IN_PROGRESS, COMPLETED, ENDED, INTERRUPTED }
enum class SadhanaAnchor(val label: String) { BREATH("natural breath"), ROOM("the room") }
enum class PauseReason { USER, BACKGROUND, INTERRUPTION, ANCHOR_CHANGE }
enum class SadhanaEventType { PRACTICE_STARTED, PRACTICE_PAUSED, PRACTICE_RESUMED, ANCHOR_CHANGED, PRACTICE_ENDED, PRACTICE_COMPLETED, PRACTICE_INTERRUPTED }

data class SadhanaEvent(val type: SadhanaEventType, val atUtcMillis: Long, val activeSeconds: Long, val detail: String? = null)

data class SadhanaAttempt(
    val id: String,
    val practiceId: String,
    val contentVersion: String,
    val startedAtUtc: Long,
    val zoneId: String,
    val intendedSeconds: Long,
    val activeMillis: Long = 0,
    val outcome: SadhanaOutcome = SadhanaOutcome.IN_PROGRESS,
    val endedAtUtc: Long? = null,
    val anchor: SadhanaAnchor = SadhanaAnchor.BREATH,
    val anchorChanges: Int = 0,
    val events: List<SadhanaEvent> = emptyList(),
) {
    val activeSeconds: Long get() = activeMillis / 1000
    val evidenceType: String get() = "app_observed"
}

class SadhanaAttemptTracker(private val persist: (SadhanaAttempt) -> Unit) {
    var attempt: SadhanaAttempt? = null
        private set
    private var runningSince: Long? = null
    val isRunning: Boolean get() = runningSince != null
    val inProgress: Boolean get() = attempt?.outcome == SadhanaOutcome.IN_PROGRESS

    /** Starts only after explicit stationary confirmation and only for a launchable practice/duration. */
    fun start(
        def: SadhanaPracticeDefinition, minutes: Int, anchor: SadhanaAnchor, stationaryConfirmed: Boolean,
        allowUnreviewed: Boolean, wallNow: Long, mono: Long, zoneId: String, id: String = UUID.randomUUID().toString(),
    ): Boolean {
        if (!stationaryConfirmed || inProgress) return false
        if (!SadhanaCatalog.canLaunch(def, allowUnreviewed) || !SadhanaCatalog.validDuration(def, minutes)) return false
        attempt = SadhanaAttempt(id, def.id, def.contentVersion, wallNow, zoneId, minutes * 60L, anchor = anchor,
            events = listOf(SadhanaEvent(SadhanaEventType.PRACTICE_STARTED, wallNow, 0, anchor.name))).also(persist)
        runningSince = mono
        return true
    }

    /** After process death a saved in-progress attempt becomes INTERRUPTED; closed time is never added. */
    fun restore(saved: SadhanaAttempt, wallNow: Long) {
        runningSince = null
        attempt = if (saved.outcome == SadhanaOutcome.IN_PROGRESS)
            saved.copy(outcome = SadhanaOutcome.INTERRUPTED, endedAtUtc = wallNow,
                events = saved.events + SadhanaEvent(SadhanaEventType.PRACTICE_INTERRUPTED, wallNow, saved.activeSeconds)).also(persist)
        else saved
    }

    fun pause(reason: PauseReason, mono: Long, wallNow: Long) {
        if (!isRunning) return
        fold(mono)
        log(SadhanaEventType.PRACTICE_PAUSED, wallNow, reason.name)
    }

    fun resume(mono: Long, wallNow: Long) {
        if (!inProgress || isRunning) return
        runningSince = mono
        log(SadhanaEventType.PRACTICE_RESUMED, wallNow)
    }

    /** Anchor change pauses (never interpreted as failure); user resumes when ready. */
    fun changeAnchor(mono: Long, wallNow: Long): SadhanaAnchor? {
        val a = attempt ?: return null
        if (!inProgress) return null
        if (isRunning) { fold(mono); log(SadhanaEventType.PRACTICE_PAUSED, wallNow, PauseReason.ANCHOR_CHANGE.name) }
        val to = if (a.anchor == SadhanaAnchor.BREATH) SadhanaAnchor.ROOM else SadhanaAnchor.BREATH
        attempt = attempt!!.copy(anchor = to, anchorChanges = a.anchorChanges + 1)
        log(SadhanaEventType.ANCHOR_CHANGED, wallNow, "${a.anchor.name}->${to.name}")
        return to
    }

    /** Returns remaining millis; completes only from active foreground time. */
    fun tick(mono: Long, wallNow: Long): Long {
        val a = attempt ?: return 0
        val target = a.intendedSeconds * 1000
        val active = a.activeMillis + (runningSince?.let { mono - it } ?: 0)
        if (a.outcome == SadhanaOutcome.IN_PROGRESS && isRunning && active >= target) {
            runningSince = null
            attempt = a.copy(activeMillis = target, outcome = SadhanaOutcome.COMPLETED, endedAtUtc = wallNow)
            log(SadhanaEventType.PRACTICE_COMPLETED, wallNow)
            return 0
        }
        return if (a.outcome == SadhanaOutcome.IN_PROGRESS) (target - active).coerceAtLeast(0) else 0
    }

    /** End is always available and immediate; active seconds are kept. */
    fun end(mono: Long, wallNow: Long) {
        if (!inProgress) return
        fold(mono)
        attempt = attempt!!.copy(outcome = SadhanaOutcome.ENDED, endedAtUtc = wallNow)
        log(SadhanaEventType.PRACTICE_ENDED, wallNow)
    }

    fun clear() { attempt = null; runningSince = null }

    private fun fold(mono: Long) {
        val a = attempt ?: return
        val since = runningSince ?: return
        runningSince = null
        attempt = a.copy(activeMillis = (a.activeMillis + (mono - since)).coerceIn(0, a.intendedSeconds * 1000))
    }

    private fun log(t: SadhanaEventType, wallNow: Long, detail: String? = null) {
        val a = attempt ?: return
        attempt = a.copy(events = a.events + SadhanaEvent(t, wallNow, a.activeSeconds, detail)).also(persist)
    }
}
