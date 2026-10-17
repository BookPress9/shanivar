package com.shanivar.nativeapp

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * What an END boundary is allowed to do when more than one focus window can be in effect.
 *
 * `releaseFocus()` tears down the one active protection session and the local guard. That is
 * correct when the window that just ended was the only one still covering "now". It is not
 * correct when another window is still in effect: the backend allows only one active session,
 * so the session is shared, and releasing it clears every window, not just the one that ended.
 *
 * The rule is an effective-window union. Ending window A releases protection only when no other
 * stored window is active at that instant. Otherwise protection stays up for whatever remains,
 * and the next alarm to arm is that remaining window's own boundary — not a blanket release.
 */
object FocusWindowEnd {
    data class Window(
        val scheduleId: String,
        val days: Set<Int>,
        val startTime: LocalTime,
        val endTime: LocalTime,
        val blockPackages: Set<String> = emptySet(),
    )

    data class Boundary(val scheduleId: String, val at: ZonedDateTime, val action: FocusWindowAction)

    /** Windows other than [endingScheduleId] that still cover [at]. The ending window is excluded
     * by id, not only by clock: a late END must not treat its own already-elapsed interval as a
     * reason to keep protection, and must not drop a different window that is still inside. */
    fun stillInEffect(endingScheduleId: String, windows: List<Window>, at: ZonedDateTime): List<Window> =
        windows.filter { window ->
            window.scheduleId != endingScheduleId &&
                FocusWindowRecurrence.isActiveAt(window.days, window.startTime, window.endTime, at)
        }

    fun shouldFullyRelease(endingScheduleId: String, windows: List<Window>, at: ZonedDateTime): Boolean =
        stillInEffect(endingScheduleId, windows, at).isEmpty()

    /** Soonest boundary strictly after [after] across every stored window. Used to re-arm after
     * a fire so ending one window does not leave a sibling without its next alarm. */
    fun soonestBoundary(windows: List<Window>, after: ZonedDateTime): Boundary? =
        windows.mapNotNull { window ->
            val (whenAt, action) = FocusWindowRecurrence.nextBoundary(
                window.days, window.startTime, window.endTime, after,
            ) ?: return@mapNotNull null
            Boundary(window.scheduleId, whenAt, action)
        }.minByOrNull { it.at.toInstant().toEpochMilli() }

    /**
     * Applies the END of [endingScheduleId]. Returns true only when protection was fully released.
     * When another window is still in effect, does not leave protection off: an already-covering
     * session is left untouched, and a session that died with the ended window is replaced with
     * coverage for the windows that remain.
     */
    suspend fun apply(
        endingScheduleId: String,
        windows: List<Window>,
        at: ZonedDateTime,
        discipline: DisciplineGateway,
    ): Boolean {
        val still = stillInEffect(endingScheduleId, windows, at)
        if (still.isEmpty()) {
            discipline.releaseFocus()
            return true
        }
        val packages = still.flatMap { it.blockPackages }.toSet()
        val until = still.mapNotNull { coverageEnd(it, at) }.maxByOrNull { it.toInstant().toEpochMilli() }
        val status = discipline.status()
        if (covers(status, until, packages)) return false
        // One active session per account. Replace a session that ends with the window that just
        // closed; never stop at the release — the remaining window is started before this returns.
        if (status.protection == ProtectionState.ACTIVE) discipline.releaseFocus()
        val minutes = remainingWholeMinutes(until, at)
        if (minutes >= 1 && packages.isNotEmpty()) discipline.startFocus(minutes, packages)
        return false
    }

    /** The still-open window's own end, which is the next boundary while [at] is inside it. */
    fun coverageEnd(window: Window, at: ZonedDateTime): ZonedDateTime? {
        val (end, action) = FocusWindowRecurrence.nextBoundary(
            window.days, window.startTime, window.endTime, at,
        ) ?: return null
        if (action != FocusWindowAction.END) return null
        return end
    }

    private fun remainingWholeMinutes(until: ZonedDateTime?, at: ZonedDateTime): Int {
        if (until == null) return 0
        val minutes = (until.toInstant().toEpochMilli() - at.toInstant().toEpochMilli()) / 60_000L
        return minutes.coerceIn(0L, 24 * 60L).toInt()
    }

    private fun covers(status: DisciplineStatus, until: ZonedDateTime?, packages: Set<String>): Boolean {
        if (status.protection != ProtectionState.ACTIVE || until == null) return false
        val neededUntil = until.toInstant().toEpochMilli()
        val activeUntil = status.focusEndsAtEpochMillis ?: return false
        // A second of slack: the session end and the window end are the same instant, but the
        // alarm and the clock are not compared on one timeline.
        return activeUntil + 1_000 >= neededUntil && status.protectedPackages.containsAll(packages)
    }
}
