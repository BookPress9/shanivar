package com.shanivar.nativeapp

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Pure function turning "this time of day, on these days of the week" into the next matching
 * instant strictly after [after]. `days` uses the 0=Sunday..6=Saturday convention shared with
 * `backend/src/routes/discipline.js`'s `days_of_week` column (`isValidDaysOfWeek`) and the JS
 * `Date.getDay()` convention the wire contract already assumes - NOT `java.time`'s own
 * `DayOfWeek.getValue()` (1=Monday..7=Sunday), which is why this file exists rather than calling
 * `java.time` directly at each call site.
 *
 * MVP streamline task (2026-09-18): `AlarmScheduler` previously persisted one trigger instant and
 * nothing ever recomputed the next one after it fired, so "repeat on Mon/Wed/Fri" could not
 * actually recur past the first ring. Recurrence has to be recomputed after every fire (and on
 * every boot, since the device clock/timezone can have moved while it was off) by calling this
 * again with `after` = now - see `AlarmScheduler.scheduleRule`/`advanceToNextOccurrence` and
 * `BootRescheduleReceiver`.
 */
object AlarmRecurrence {
    /**
     * Returns null if [days] is empty or contains a value outside 0..6 - the backend rejects an
     * empty `days_of_week` on write, and an empty set here has no meaningful "next occurrence" to
     * report, so this is an input-validation guard, not an expected runtime outcome.
     */
    fun nextOccurrence(localTime: LocalTime, days: Set<Int>, after: ZonedDateTime): ZonedDateTime? {
        if (days.isEmpty() || days.any { it !in 0..6 }) return null
        // 0..7 (8 candidate days) rather than 0..6: guarantees every day-of-week is tried at least
        // once strictly after `after`, even in the edge case where today is selected but today's
        // slot has already passed - offset 7 revisits today's weekday one week out.
        for (offset in 0..7) {
            val candidateDate = after.toLocalDate().plusDays(offset.toLong())
            if (isoDayToOurConvention(candidateDate.dayOfWeek) !in days) continue
            val candidate = candidateDate.atTime(localTime).atZone(after.zone)
            if (candidate.isAfter(after)) return candidate
        }
        // Unreachable given the validated, non-empty `days` above; null only guards the invalid-
        // input case, not a real "no next occurrence" outcome.
        return null
    }

    /** `java.time.DayOfWeek` is 1=Monday..7=Sunday; this project's wire contract is
     * 0=Sunday..6=Saturday, matching JS `Date.getDay()`. `day.value % 7` maps SUNDAY(7)->0 and
     * leaves MONDAY(1)..SATURDAY(6) unchanged. Internal (not private): `FocusWindowRecurrence`
     * shares this exact mapping rather than duplicating it - a second, slightly-different
     * day-convention helper drifting from this one is exactly the kind of bug that hides for
     * months in whichever branch runs less often. */
    internal fun isoDayToOurConvention(day: DayOfWeek): Int = day.value % 7
}
