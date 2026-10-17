package com.shanivar.nativeapp

import java.time.LocalTime

/**
 * Which part of the person's *own stated* day it is. Named "window" rather than "phase" on purpose:
 * these are product heuristics over two times the user typed, not a measurement of anything
 * biological. Nothing here may be presented as knowing whether someone is actually rested, sleepy
 * or alert (decision 0011, test guide C10).
 */
enum class ScheduleWindow {
    /** No usable wake/sleep pair. Offer a quiet edit option; never substitute a default day. */
    UNKNOWN,
    JUST_WOKE_WINDOW,
    DAYTIME,
    WIND_DOWN_WINDOW,

    /** Past the intended sleep time. Evidence of a chosen schedule, never evidence of sleep - a
     * shift worker at 03:00 may be mid-shift, so this only ever suggests a winding-down option. */
    REST_WINDOW,
}

/**
 * The optional wake and intended-sleep times for breathing suggestions.
 *
 * Intended-sleep is stored separately from [Rhythm.quietTime] rather than reusing it. quietTime is
 * "when the app goes quiet", which is not the same claim as "when I intend to sleep", and silently
 * reinterpreting it would put a time the user set for one purpose behind a different inference
 * (handoff section 2; test guide T06).
 */
data class BreathingSchedule(val wakeTime: LocalTime?, val sleepTime: LocalTime?) {

    /** Minutes awake according to the user's own two times, or null when the pair is unusable. */
    val awakeSpanMinutes: Int?
        get() {
            val wake = wakeTime ?: return null
            val sleep = sleepTime ?: return null
            val span = Math.floorMod(sleep.toMinuteOfDay() - wake.toMinuteOfDay(), MINUTES_PER_DAY)
            return if (span in MIN_PLAUSIBLE_SPAN..MAX_PLAUSIBLE_SPAN) span else null
        }

    /**
     * Half-open intervals, in this order. [now] is local wall time, which is the right clock for
     * "what time of day does this person think it is" - and the wrong clock for session duration,
     * which uses a monotonic source so a DST jump cannot shorten someone's practice (T08/T10).
     */
    fun window(now: LocalTime): ScheduleWindow {
        val span = awakeSpanMinutes ?: return ScheduleWindow.UNKNOWN
        val wake = wakeTime ?: return ScheduleWindow.UNKNOWN
        val elapsed = Math.floorMod(now.toMinuteOfDay() - wake.toMinuteOfDay(), MINUTES_PER_DAY)
        return when {
            elapsed >= span -> ScheduleWindow.REST_WINDOW
            elapsed < EDGE_MINUTES -> ScheduleWindow.JUST_WOKE_WINDOW
            elapsed >= span - EDGE_MINUTES -> ScheduleWindow.WIND_DOWN_WINDOW
            else -> ScheduleWindow.DAYTIME
        }
    }

    companion object {
        const val MINUTES_PER_DAY = 1440

        /** An engineering plausibility guard, *not* a recommended amount of waking time. Outside
         * this range the pair is almost certainly a typo or a half-finished edit, and a suggestion
         * built on it would be worse than no suggestion. */
        const val MIN_PLAUSIBLE_SPAN = 360
        const val MAX_PLAUSIBLE_SPAN = 1200

        /** How long "just woke" and "winding down" last at each end of the stated day. */
        const val EDGE_MINUTES = 120

        val UNSET = BreathingSchedule(null, null)

        /** Unparseable or absent values yield UNKNOWN rather than a guessed default (T04/T12). */
        fun parse(wake: String?, sleep: String?): BreathingSchedule =
            BreathingSchedule(parseTime(wake), parseTime(sleep))

        private fun parseTime(value: String?): LocalTime? =
            value?.takeIf { it.isNotBlank() }?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    }
}

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
