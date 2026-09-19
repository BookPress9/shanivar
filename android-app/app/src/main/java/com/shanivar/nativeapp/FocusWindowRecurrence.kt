package com.shanivar.nativeapp

import java.time.LocalTime
import java.time.ZonedDateTime

/** Which end of a focus window a computed boundary represents - the receiver that fires at that
 * instant needs to know whether to start or release protection. */
enum class FocusWindowAction { START, END }

/**
 * Pure recurrence math for a recurring focus/app-lock window ("all day", "a few days a week", or
 * "custom times" - three presets over the same underlying shape: selected days plus a start/end
 * time of day). Companion to [AlarmRecurrence], which this reuses directly rather than
 * reimplementing "next matching day/time" logic a second time.
 *
 * A window has two boundaries per active day, not one - reuses [AlarmRecurrence.nextOccurrence]
 * once for the start time and once for the end time, then returns whichever comes first. This
 * composes correctly with no special-casing for "we're currently inside the window": if `after`
 * falls inside today's window, today's start time is already in the past, so
 * `nextOccurrence(startTime, ...)` correctly skips to next week's start, while
 * `nextOccurrence(endTime, ...)` correctly returns *today's* end - so the pair-and-pick-soonest
 * below naturally returns (today's end, END) without the caller having to ask "am I inside the
 * window?" first.
 */
object FocusWindowRecurrence {
    /** True if [at] falls on a selected day, at-or-after [startTime] and strictly before
     * [endTime]. Used only for boot/process-restart catch-up - see `BootRescheduleReceiver` -
     * since ordinary operation never needs to ask "is it active right now," only "what's next." */
    fun isActiveAt(days: Set<Int>, startTime: LocalTime, endTime: LocalTime, at: ZonedDateTime): Boolean {
        if (days.isEmpty() || startTime >= endTime) return false
        val today = AlarmRecurrence.isoDayToOurConvention(at.dayOfWeek)
        if (today !in days) return false
        val time = at.toLocalTime()
        return !time.isBefore(startTime) && time.isBefore(endTime)
    }

    /** The next boundary strictly after [after], and whether it opens or closes the window.
     * Null only for invalid input (empty days, or start not before end) - the same non-empty,
     * validated input always has a next boundary. */
    fun nextBoundary(days: Set<Int>, startTime: LocalTime, endTime: LocalTime, after: ZonedDateTime): Pair<ZonedDateTime, FocusWindowAction>? {
        if (startTime >= endTime) return null
        val nextStart = AlarmRecurrence.nextOccurrence(startTime, days, after)
        val nextEnd = AlarmRecurrence.nextOccurrence(endTime, days, after)
        return when {
            nextStart == null && nextEnd == null -> null
            nextEnd == null -> nextStart!! to FocusWindowAction.START
            nextStart == null -> nextEnd to FocusWindowAction.END
            nextStart.isBefore(nextEnd) -> nextStart to FocusWindowAction.START
            else -> nextEnd to FocusWindowAction.END
        }
    }
}
