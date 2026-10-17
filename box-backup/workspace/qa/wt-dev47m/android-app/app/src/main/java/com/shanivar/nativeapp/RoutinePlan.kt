package com.shanivar.nativeapp

import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Versioned local Routine configuration (decision 0014 / R1).
 *
 * Personal activities are calendar entries only: saving one never arms an alarm or Focus rule.
 * Protection windows and the shared package set live here as configuration; [FocusScheduler]
 * owns armed triggers. Decode failure must not claim readiness — see [RoutinePlanCodec].
 */
data class RoutinePlanV1(
    val wakeTime: LocalTime? = null,
    val intendedSleepTime: LocalTime? = null,
    val activities: List<RoutineActivityV1> = emptyList(),
    val protectedPackages: Set<String> = emptySet(),
    val windows: List<ProtectionWindowV1> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
        const val MAX_ACTIVITIES = 12
        const val MAX_WINDOWS = 8
    }
}

data class RoutineActivityV1(
    val id: String,
    val title: String,
    val startTime: LocalTime,
    val durationMinutes: Int,
    /** 0=Sun … 6=Sat. Empty is invalid for a saved activity. */
    val days: Set<Int>,
    val enabled: Boolean = true,
)

data class ProtectionWindowV1(
    val id: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    /** Start-day ownership (0=Sun … 6=Sat). Overnight windows: selected day owns the start. */
    val startDays: Set<Int>,
    val enabled: Boolean = true,
)

/** How the UI presents activity recurrence; maps to [RoutineActivityV1.days]. */
enum class ActivityOccurrenceKind { EVERY_DAY, WEEKLY, CUSTOM }

object RoutineActivityOccurrence {
    val EVERY_DAY: Set<Int> = (0..6).toSet()

    fun kindOf(days: Set<Int>): ActivityOccurrenceKind = when {
        days == EVERY_DAY -> ActivityOccurrenceKind.EVERY_DAY
        days.size == 1 -> ActivityOccurrenceKind.WEEKLY
        else -> ActivityOccurrenceKind.CUSTOM
    }

    fun label(days: Set<Int>): String = when (kindOf(days)) {
        ActivityOccurrenceKind.EVERY_DAY -> "Every day"
        ActivityOccurrenceKind.WEEKLY -> weekdayName(days.first())
        ActivityOccurrenceKind.CUSTOM -> days.sorted().joinToString(", ") { weekdayName(it) }
    }

    fun weekdayName(day: Int): String = when (day) {
        0 -> "Sun"; 1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"; 4 -> "Thu"; 5 -> "Fri"; 6 -> "Sat"
        else -> "?"
    }
}

/** Quick-add starters from the wireframe — editable, not engine types. */
object RoutineActivityQuickAdds {
    data class Starter(val title: String, val durationMinutes: Int, val startHour: Int, val startMinute: Int)

    val ALL = listOf(
        Starter("Gym", 60, 7, 30),
        Starter("Read a book", 30, 21, 0),
        Starter("Extend Sadhana", 20, 6, 30),
    )

    fun create(starter: Starter, id: String = UUID.randomUUID().toString()): RoutineActivityV1 =
        RoutineActivityV1(
            id = id,
            title = starter.title,
            startTime = LocalTime.of(starter.startHour, starter.startMinute),
            durationMinutes = starter.durationMinutes,
            days = RoutineActivityOccurrence.EVERY_DAY,
            enabled = true,
        )

    fun blank(id: String = UUID.randomUUID().toString()): RoutineActivityV1 =
        RoutineActivityV1(
            id = id,
            title = "",
            startTime = LocalTime.of(9, 0),
            durationMinutes = 30,
            days = RoutineActivityOccurrence.EVERY_DAY,
            enabled = true,
        )
}

sealed class RoutinePlanValidation {
    data object Ok : RoutinePlanValidation()
    data class Error(val message: String) : RoutinePlanValidation()
}

object RoutinePlanRules {
    fun validate(plan: RoutinePlanV1): RoutinePlanValidation {
        if (plan.wakeTime != null && plan.intendedSleepTime != null &&
            plan.wakeTime == plan.intendedSleepTime
        ) {
            return RoutinePlanValidation.Error("Wake and intended sleep cannot be the same time.")
        }
        if (plan.activities.size > RoutinePlanV1.MAX_ACTIVITIES) {
            return RoutinePlanValidation.Error("At most ${RoutinePlanV1.MAX_ACTIVITIES} personal activities.")
        }
        plan.activities.forEach { activity ->
            validateActivity(activity)?.let { return it }
        }
        if (plan.windows.size > RoutinePlanV1.MAX_WINDOWS) {
            return RoutinePlanValidation.Error("At most ${RoutinePlanV1.MAX_WINDOWS} protection windows.")
        }
        plan.windows.forEach { window ->
            validateWindow(window)?.let { return it }
        }
        findEnabledOverlap(plan.windows)?.let { (a, b) ->
            return RoutinePlanValidation.Error(
                "Protection windows overlap: ${shortWindow(a)} and ${shortWindow(b)}. Adjacent is fine; overlap is not.",
            )
        }
        return RoutinePlanValidation.Ok
    }

    fun validateActivity(activity: RoutineActivityV1): RoutinePlanValidation.Error? {
        if (activity.title.isBlank()) return RoutinePlanValidation.Error("Activity needs a name.")
        if (activity.durationMinutes !in 1..24 * 60) {
            return RoutinePlanValidation.Error("Duration must be between 1 and 1440 minutes.")
        }
        if (activity.days.isEmpty() || activity.days.any { it !in 0..6 }) {
            return RoutinePlanValidation.Error("Pick at least one day for this activity.")
        }
        return null
    }

    fun validateWindow(window: ProtectionWindowV1): RoutinePlanValidation.Error? {
        if (!FocusWindowRecurrence.isValidShape(window.startTime, window.endTime)) {
            return RoutinePlanValidation.Error("A window cannot start and end at the same time (except midnight-to-midnight all-day).")
        }
        if (window.startDays.isEmpty() || window.startDays.any { it !in 0..6 }) {
            return RoutinePlanValidation.Error("Pick at least one start day for this window.")
        }
        return null
    }

    /**
     * Adjacent windows are allowed; overlapping enabled windows are not.
     * Compares each pair on a fixed reference week so overnight shapes are covered.
     */
    fun findEnabledOverlap(windows: List<ProtectionWindowV1>): Pair<ProtectionWindowV1, ProtectionWindowV1>? {
        val enabled = windows.filter { it.enabled }
        for (i in enabled.indices) {
            for (j in i + 1 until enabled.size) {
                if (windowsOverlap(enabled[i], enabled[j])) return enabled[i] to enabled[j]
            }
        }
        return null
    }

    /**
     * Exact overlap on a reference week (minute resolution, half-open intervals), so adjacent
     * windows (one ends 10:00, the next starts 10:00) never count, and any shared minute does —
     * including odd times like 09:05-09:10 vs 09:07-09:20, which a coarse sampling grid would miss.
     * Overnight occurrences belong to their start day and may wrap Saturday night into Sunday.
     */
    fun windowsOverlap(a: ProtectionWindowV1, b: ProtectionWindowV1): Boolean {
        val ia = ProtectionWindowGeometry.weekIntervals(a)
        val ib = ProtectionWindowGeometry.weekIntervals(b)
        return ia.any { x -> ib.any { y -> x.first < y.last + 1 && y.first < x.last + 1 } }
    }

    private fun shortWindow(w: ProtectionWindowV1): String = ProtectionWindowGeometry.describe(w)
}

/**
 * Next occurrence of an activity start strictly after [after], or null when disabled / invalid.
 * Pure — never touches alarm or Focus schedulers.
 */
object RoutineActivityRecurrence {
    fun nextStart(activity: RoutineActivityV1, after: ZonedDateTime): ZonedDateTime? {
        if (!activity.enabled || activity.days.isEmpty()) return null
        return AlarmRecurrence.nextOccurrence(activity.startTime, activity.days, after)
    }

    fun occursOn(activity: RoutineActivityV1, day: Int): Boolean =
        activity.enabled && day in activity.days
}

/**
 * Pure shape helpers for protection windows (decision 0014 semantics, shared with
 * [FocusWindowRecurrence]): same-day, overnight (selected day owns the start) and the explicit
 * full-calendar-day encoding `00:00-00:00`.
 */
object ProtectionWindowGeometry {
    const val MINUTES_PER_DAY = 24 * 60
    const val MINUTES_PER_WEEK = 7 * MINUTES_PER_DAY

    /** Minutes one occurrence lasts; 0 for an invalid shape. Full calendar day = 1440. */
    fun durationMinutes(start: LocalTime, end: LocalTime): Int {
        if (!FocusWindowRecurrence.isValidShape(start, end)) return 0
        if (FocusWindowRecurrence.isFullCalendarDay(start, end)) return MINUTES_PER_DAY
        val s = start.hour * 60 + start.minute
        val e = end.hour * 60 + end.minute
        return ((e - s) + MINUTES_PER_DAY) % MINUTES_PER_DAY
    }

    /**
     * Occurrences as closed minute ranges `[startMinute, endMinute - 1]` measured from Sunday 00:00
     * of a reference week. An occurrence that runs past Saturday midnight is split so the tail lands
     * at the start of the week (the week repeats).
     */
    fun weekIntervals(window: ProtectionWindowV1): List<IntRange> {
        val duration = durationMinutes(window.startTime, window.endTime)
        if (duration <= 0) return emptyList()
        val startOfDay = window.startTime.hour * 60 + window.startTime.minute
        return window.startDays.filter { it in 0..6 }.flatMap { day ->
            val s = day * MINUTES_PER_DAY + startOfDay
            val e = s + duration // exclusive
            if (e <= MINUTES_PER_WEEK) listOf(s until e)
            else listOf(s until MINUTES_PER_WEEK, 0 until (e - MINUTES_PER_WEEK))
        }
    }

    fun isOvernight(window: ProtectionWindowV1): Boolean =
        FocusWindowRecurrence.isOvernight(window.startTime, window.endTime) &&
            !FocusWindowRecurrence.isFullCalendarDay(window.startTime, window.endTime)

    fun isFullCalendarDay(window: ProtectionWindowV1): Boolean =
        FocusWindowRecurrence.isFullCalendarDay(window.startTime, window.endTime)

    /** "09:00–10:30 · Weekdays", "22:00–06:00 next day · Every day", "Full calendar day · Sat". */
    fun describe(window: ProtectionWindowV1): String {
        val days = WindowRepeat.label(window.startDays)
        if (isFullCalendarDay(window)) return "Full calendar day · $days"
        val start = window.startTime.toString().take(5)
        val end = window.endTime.toString().take(5)
        val suffix = if (isOvernight(window)) " next day" else ""
        return "$start–$end$suffix · $days"
    }
}

/** Wireframe repeat choices for protection windows; maps to [ProtectionWindowV1.startDays]. */
enum class WindowRepeat(val label: String) {
    EVERY_DAY("Every day"),
    WEEKDAYS("Weekdays"),
    WEEKENDS("Weekends"),
    CUSTOM("Custom week");

    companion object {
        val EVERY_DAY_DAYS: Set<Int> = (0..6).toSet()
        val WEEKDAY_DAYS: Set<Int> = (1..5).toSet()
        val WEEKEND_DAYS: Set<Int> = setOf(0, 6)

        fun kindOf(days: Set<Int>): WindowRepeat = when (days) {
            EVERY_DAY_DAYS -> EVERY_DAY
            WEEKDAY_DAYS -> WEEKDAYS
            WEEKEND_DAYS -> WEEKENDS
            else -> CUSTOM
        }

        /** Days for a preset; CUSTOM keeps [current] (or Mon if empty) so switching does not wipe picks. */
        fun daysFor(kind: WindowRepeat, current: Set<Int>): Set<Int> = when (kind) {
            EVERY_DAY -> EVERY_DAY_DAYS
            WEEKDAYS -> WEEKDAY_DAYS
            WEEKENDS -> WEEKEND_DAYS
            CUSTOM -> current.ifEmpty { setOf(1) }
        }

        fun label(days: Set<Int>): String = when (val kind = kindOf(days)) {
            CUSTOM -> if (days.isEmpty()) "No days" else
                // Monday-first, matching the wireframe week picker.
                days.sortedBy { (it + 6) % 7 }.joinToString(", ") { RoutineActivityOccurrence.weekdayName(it) }
            else -> kind.label
        }
    }
}

/** B5: Choose counts as linked to the Routine only when a saved, enabled window with at least one day exists. */
object RoutineBoundaryLink {
    fun linked(plan: RoutinePlanV1): Boolean = plan.windows.any { it.enabled && it.startDays.isNotEmpty() && it.startTime != it.endTime }
}
