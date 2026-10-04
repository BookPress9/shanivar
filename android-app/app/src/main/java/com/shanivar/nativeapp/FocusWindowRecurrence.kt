package com.shanivar.nativeapp

import java.time.LocalTime
import java.time.ZonedDateTime

/** Which end of a focus window a computed boundary represents - the receiver that fires at that
 * instant needs to know whether to start or release protection. */
enum class FocusWindowAction { START, END }

/**
 * Pure recurrence math for a recurring focus/app-lock window: selected days plus a start and end
 * time of day. Companion to [AlarmRecurrence], which this reuses directly rather than
 * reimplementing "next matching day/time" logic a second time.
 *
 * ## Window shapes (decision 0014, "Protection-window semantics")
 *
 * - `start < end` — a same-day window. Monday 07:00-09:00 opens and closes on Monday.
 * - `start > end` — an **overnight** window, and **the selected day owns the start**. Monday
 *   22:00-06:00 means Monday night through Tuesday 06:00, and is *not* active on Sunday night
 *   unless Sunday is also selected. This is what makes sleep preparation expressible at all.
 * - `start == end` — **invalid**. It is rejected rather than interpreted, because the two readings
 *   ("zero minutes" and "a full 24 hours") are opposites and guessing either one silently is worse
 *   than refusing. "All day" must be modelled explicitly if it is ever required; encoding it as
 *   `00:00-23:59` leaves a one-minute hole every day and is not the same claim.
 *
 * A window has two boundaries per occurrence, not one. For a same-day window both fall on a
 * selected day; for an overnight window the END falls on the day *after* a selected start day,
 * which is why [endDaysFor] shifts the day set rather than reusing it.
 */
object FocusWindowRecurrence {

    /** Whether this window shape can be scheduled at all. Callers validating user input should use
     * this rather than inferring validity from a null boundary, which cannot distinguish "bad
     * shape" from "no selected days". */
    fun isValidShape(startTime: LocalTime, endTime: LocalTime): Boolean = startTime != endTime

    fun isOvernight(startTime: LocalTime, endTime: LocalTime): Boolean = startTime > endTime

    /**
     * True when [at] falls inside an occurrence of this window.
     *
     * Used for boot/process-restart catch-up and - since DEV-1 - to decide at delivery time whether
     * a START boundary is still legitimate.
     */
    fun isActiveAt(days: Set<Int>, startTime: LocalTime, endTime: LocalTime, at: ZonedDateTime): Boolean =
        activeWindowEnd(days, startTime, endTime, at) != null

    /**
     * The instant the currently-active occurrence ends, or null when no occurrence is active.
     *
     * This exists because [nextBoundary] is the wrong question for "how long should protection
     * run". It returns the next boundary of *either* kind, so a caller that assumed it was always
     * the END would compute a duration from the following occurrence's START whenever delivery
     * arrived late - which is exactly how a two-hour window became a 24-hour lockout (DEV-1).
     *
     * Returning null when nothing is active is the load-bearing part: a stale START cannot produce
     * a duration at all, rather than producing a wrong one.
     */
    fun activeWindowEnd(
        days: Set<Int>,
        startTime: LocalTime,
        endTime: LocalTime,
        at: ZonedDateTime,
    ): ZonedDateTime? {
        if (days.isEmpty() || !isValidShape(startTime, endTime)) return null
        val today = AlarmRecurrence.isoDayToOurConvention(at.dayOfWeek)
        val time = at.toLocalTime()

        if (!isOvernight(startTime, endTime)) {
            if (today !in days) return null
            if (time.isBefore(startTime) || !time.isBefore(endTime)) return null
            return at.toLocalDate().atTime(endTime).atZone(at.zone)
        }

        // Overnight, evening half: still on the start day, so the window closes tomorrow.
        if (today in days && !time.isBefore(startTime)) {
            return at.toLocalDate().plusDays(1).atTime(endTime).atZone(at.zone)
        }
        // Overnight, morning half: the occurrence started yesterday, so yesterday must be selected.
        if (previousDay(today) in days && time.isBefore(endTime)) {
            return at.toLocalDate().atTime(endTime).atZone(at.zone)
        }
        return null
    }

    /**
     * The next boundary strictly after [after], and whether it opens or closes the window.
     *
     * Null only for invalid input (no selected days, or `start == end`); any validated window has a
     * next boundary. When an occurrence is currently active this returns its END, because that END
     * is nearer than the following occurrence's START.
     */
    fun nextBoundary(
        days: Set<Int>,
        startTime: LocalTime,
        endTime: LocalTime,
        after: ZonedDateTime,
    ): Pair<ZonedDateTime, FocusWindowAction>? {
        if (days.isEmpty() || !isValidShape(startTime, endTime)) return null
        val nextStart = AlarmRecurrence.nextOccurrence(startTime, days, after)
        val nextEnd = AlarmRecurrence.nextOccurrence(endTime, endDaysFor(days, startTime, endTime), after)
        return when {
            nextStart == null && nextEnd == null -> null
            nextEnd == null -> nextStart!! to FocusWindowAction.START
            nextStart == null -> nextEnd to FocusWindowAction.END
            // Equal instants cannot happen for a valid shape (start != end), so a strict comparison
            // is total here.
            nextStart.isBefore(nextEnd) -> nextStart to FocusWindowAction.START
            else -> nextEnd to FocusWindowAction.END
        }
    }

    /** Which weekdays an END falls on. Same as the start days for a same-day window; shifted one
     * day later for an overnight window, since the selected day owns the start. */
    internal fun endDaysFor(days: Set<Int>, startTime: LocalTime, endTime: LocalTime): Set<Int> =
        if (isOvernight(startTime, endTime)) days.map { nextDay(it) }.toSet() else days

    private fun nextDay(day: Int): Int = (day + 1) % 7

    private fun previousDay(day: Int): Int = (day + 6) % 7
}
