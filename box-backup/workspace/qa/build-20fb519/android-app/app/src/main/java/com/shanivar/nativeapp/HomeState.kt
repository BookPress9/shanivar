package com.shanivar.nativeapp

/** The three destinations of the founder's 2026-09-29 navigation direction. */
enum class ShanivarTab(val label: String) {
    DASHBOARD("Dashboard"),
    ROUTINE("Routine"),
    SADHANA("Sadhana"),
}

enum class NextActionKind {
    /** Protection is running. The most useful thing the app can do is get out of the way. */
    FOCUS_ACTIVE,
    ALARM_SOON,
    SET_UP_ROUTINE,
    OFFER_PRACTICE,
    START_FOCUS,
}

/**
 * What the Dashboard offers right now. One action, plus the tab that fulfils it.
 *
 * [actionLabel] is a verb the person can act on, never a status word - the straight-line rule in
 * CLAUDE.md section 5: state the current state, the result they chose, and the next required action.
 */
data class NextAction(
    val kind: NextActionKind,
    val headline: String,
    val detail: String,
    val actionLabel: String,
    val tab: ShanivarTab,
)

/**
 * Everything the resolver may see. All of it is already on the device: scheduler state, the focus
 * guard's own record, the rhythm the person configured, and the local practice journal.
 *
 * There is no field here for anything sensed, inferred or fetched. That is the point - the Dashboard
 * decides what to offer with arithmetic, not with a model call, so it is instant, offline, free and
 * explainable. If a future change wants a new signal it has to appear here first.
 */
data class HomeSnapshot(
    val nowEpochMillis: Long,
    /** 0 when no focus window is running. Written by the focus engine, read-only here. */
    val focusEndsAtEpochMillis: Long = 0L,
    val nextAlarmEpochMillis: Long? = null,
    val hasFocusSchedule: Boolean = false,
    val intentionLabel: String = FocusPresets.DEFAULT.label,
    val focusMinutes: Int = 30,
    val practisedToday: Boolean = false,
    val scheduleWindow: ScheduleWindow = ScheduleWindow.UNKNOWN,
)

/**
 * Resolves the Dashboard's single next action.
 *
 * Deterministic and ordered by what is already true rather than by what we would like the person to
 * do. Two consequences worth stating, because both are easy to erode later:
 *
 * 1. **An active focus window resolves to leaving.** If protection is running, the person already did
 *    the thing the app exists for; offering them something to do inside Shanivar at that moment works
 *    against it.
 * 2. **A practice is offered, never required.** Nothing here escalates, and `practisedToday` only
 *    suppresses a repeat offer - it never becomes a streak, a score or a reason to nudge.
 */
object HomeState {
    /** How close an alarm has to be before it outranks everything else. */
    const val ALARM_SOON_MILLIS = 60 * 60 * 1000L

    fun resolve(snapshot: HomeSnapshot): NextAction {
        val focusRemaining = snapshot.focusEndsAtEpochMillis - snapshot.nowEpochMillis
        if (snapshot.focusEndsAtEpochMillis > 0L && focusRemaining > 0L) {
            val minutes = (focusRemaining / 60_000L).coerceAtLeast(1L)
            return NextAction(
                kind = NextActionKind.FOCUS_ACTIVE,
                headline = "Your time is protected.",
                detail = "${snapshot.intentionLabel} · $minutes min remaining.",
                actionLabel = "Return to my day",
                tab = ShanivarTab.DASHBOARD,
            )
        }

        val untilAlarm = snapshot.nextAlarmEpochMillis?.minus(snapshot.nowEpochMillis)
        if (untilAlarm != null && untilAlarm in 1..ALARM_SOON_MILLIS) {
            val minutes = (untilAlarm / 60_000L).coerceAtLeast(1L)
            return NextAction(
                kind = NextActionKind.ALARM_SOON,
                headline = "Your alarm is close.",
                detail = "It rings in $minutes min.",
                actionLabel = "See routine",
                tab = ShanivarTab.ROUTINE,
            )
        }

        // Nothing configured yet: the routine is the whole product until it exists.
        if (snapshot.nextAlarmEpochMillis == null && !snapshot.hasFocusSchedule) {
            return NextAction(
                kind = NextActionKind.SET_UP_ROUTINE,
                headline = "Set your day once.",
                detail = "An alarm you choose, and the apps you want paused.",
                actionLabel = "Set up routine",
                tab = ShanivarTab.ROUTINE,
            )
        }

        if (!snapshot.practisedToday && snapshot.scheduleWindow.suggestsPractice()) {
            return NextAction(
                kind = NextActionKind.OFFER_PRACTICE,
                headline = snapshot.scheduleWindow.practiceHeadline(),
                detail = "A short practice, if you want one.",
                actionLabel = "Find my pause",
                tab = ShanivarTab.SADHANA,
            )
        }

        return NextAction(
            kind = NextActionKind.START_FOCUS,
            headline = "One thing, protected.",
            detail = "${snapshot.intentionLabel} · ${snapshot.focusMinutes} min.",
            actionLabel = "Start focus",
            tab = ShanivarTab.ROUTINE,
        )
    }
}

/** The clock only ever *suggests*; it never gates anything, and it makes no claim about the body. */
private fun ScheduleWindow.suggestsPractice(): Boolean = when (this) {
    ScheduleWindow.JUST_WOKE_WINDOW, ScheduleWindow.WIND_DOWN_WINDOW -> true
    ScheduleWindow.DAYTIME, ScheduleWindow.REST_WINDOW, ScheduleWindow.UNKNOWN -> false
}

private fun ScheduleWindow.practiceHeadline(): String = when (this) {
    ScheduleWindow.JUST_WOKE_WINDOW -> "Begin where you are."
    ScheduleWindow.WIND_DOWN_WINDOW -> "Let the day settle."
    else -> "A moment, if you want one."
}
