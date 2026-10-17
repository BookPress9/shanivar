package com.shanivar.nativeapp

/**
 * DEV-11 R1 local plan model. Wake and sleep are anchors only. Personal activities are stored
 * data (time, duration, recurrence). Nothing in this model is scheduled or armed.
 *
 * Day numbers follow the app's existing 0=Sunday .. 6=Saturday convention.
 */
enum class RoutineRecurrenceKind {
    EVERY_DAY,
    WEEKLY,
    CUSTOM_DAYS,
}

data class RoutineRecurrenceV1(
    val kind: RoutineRecurrenceKind,
    val days: Set<Int> = emptySet(),
) {
    fun valid(): Boolean {
        if (days.any { it !in 0..6 }) return false
        return when (kind) {
            RoutineRecurrenceKind.EVERY_DAY -> days.isEmpty()
            RoutineRecurrenceKind.WEEKLY -> days.size == 1
            RoutineRecurrenceKind.CUSTOM_DAYS -> days.isNotEmpty()
        }
    }

    companion object {
        fun everyDay() = RoutineRecurrenceV1(RoutineRecurrenceKind.EVERY_DAY)
        fun weekly(day: Int) = RoutineRecurrenceV1(RoutineRecurrenceKind.WEEKLY, setOf(day))
        fun customDays(days: Set<Int>) = RoutineRecurrenceV1(RoutineRecurrenceKind.CUSTOM_DAYS, days)
    }
}

data class RoutineActivityV1(
    val id: String,
    val title: String,
    val time: String,
    val durationMinutes: Int,
    val recurrence: RoutineRecurrenceV1,
) {
    fun valid(): Boolean =
        id.isNotBlank() && title.isNotBlank() && durationMinutes > 0 && recurrence.valid() &&
            isClockTime(time)
}

data class RoutinePlanV1(
    val wakeTime: String? = null,
    val sleepTime: String? = null,
    val activities: List<RoutineActivityV1> = emptyList(),
) {
    fun valid(): Boolean {
        if (wakeTime != null && !isClockTime(wakeTime)) return false
        if (sleepTime != null && !isClockTime(sleepTime)) return false
        if (activities.map { it.id }.distinct().size != activities.size) return false
        return activities.all { it.valid() }
    }
}

/**
 * What a repository read returns. [activitiesArmed] is fixed false: a stored activity is not a
 * scheduled wake, a quiet-hours rule, or a running protection window.
 */
data class RoutineSnapshot(
    val plan: RoutinePlanV1,
    val migratedFromRhythm: Boolean,
) {
    val activitiesArmed: Boolean get() = false
}

internal fun isClockTime(value: String): Boolean {
    val match = CLOCK_TIME.matchEntire(value) ?: return false
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    return hour in 0..23 && minute in 0..59
}

private val CLOCK_TIME = Regex("""(\d{2}):(\d{2})""")
