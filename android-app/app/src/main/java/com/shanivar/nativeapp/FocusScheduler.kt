package com.shanivar.nativeapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Persists and arms the recurring focus/app-lock window ("all day" / "a few days a week" /
 * "custom times" - see `docs/decisions/0010-focus-schedule-when-then-commitment.md`). Mirrors
 * `AlarmScheduler` deliberately: same persist-the-rule-then-recompute-on-every-fire shape, same
 * `AlarmManager` wrapper pattern, same reason (a single pending trigger can't express recurrence
 * by itself). The one structural difference is [FocusWindowRecurrence.nextBoundary] returning
 * *which* boundary (start or end) is next, since a focus window - unlike an alarm - has two kinds
 * of event, not one.
 *
 * The Focus card still saves one schedule. The persisted window list can hold more than one rule
 * so an END releases protection only when no other window is still in effect (`FocusWindowEnd`).
 * One AlarmManager trigger is armed at a time. `scheduleNext` arms the rule it was given (the
 * card's contract). After a boundary fires, `armSoonest` points that single trigger at whichever
 * stored window's boundary is next, so ending window A does not drop window B.
 *
 * Uses `setExactAndAllowWhileIdle`, not `setAlarmClock`: this window toggle is a silent background
 * action with no user-facing alarm-clock semantics, and `setAlarmClock` would put a persistent
 * alarm-clock icon in the status bar for something that isn't one.
 */
class FocusScheduler(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class Rule(
        val scheduleId: String, val days: Set<Int>, val startTime: LocalTime, val endTime: LocalTime,
        val blockPackages: Set<String>,
    )

    /** Persists [rule] and arms the native trigger for its next boundary strictly after [after].
     * Returns the trigger instant and which action it represents, or null without persisting or
     * scheduling anything if exact-alarm permission isn't granted or the rule is invalid - callers
     * should check `DisciplinePermissions.hasExactAlarmPermission()` first, matching AlarmScheduler. */
    fun scheduleNext(rule: Rule, after: ZonedDateTime = ZonedDateTime.now()): Pair<Long, FocusWindowAction>? {
        val (occurrence, action) = FocusWindowRecurrence.nextBoundary(rule.days, rule.startTime, rule.endTime, after) ?: return null
        val triggerAt = occurrence.toInstant().toEpochMilli()
        if (!armNativeAlarm(triggerAt)) return null
        val stored = storedWindows().filter { it.scheduleId != rule.scheduleId } + rule
        prefs.edit()
            .putString(KEY_SCHEDULE_ID, rule.scheduleId)
            .putString(KEY_DAYS, rule.days.sorted().joinToString(","))
            .putString(KEY_START, rule.startTime.toString())
            .putString(KEY_END, rule.endTime.toString())
            .putStringSet(KEY_PACKAGES, rule.blockPackages)
            .putString(KEY_PENDING_ACTION, action.name)
            .putLong(KEY_NEXT_TRIGGER, triggerAt)
            .putString(KEY_WINDOWS, encodeWindows(stored))
            .apply()
        return triggerAt to action
    }

    /** Re-arm the single native trigger for the soonest boundary across every stored window.
     * No-op (returns null, changes nothing) when nothing is stored or exact alarms are unavailable. */
    fun armSoonest(after: ZonedDateTime = ZonedDateTime.now()): Pair<Long, FocusWindowAction>? {
        val rules = windows()
        val boundary = FocusWindowEnd.soonestBoundary(rules.map { it.asFocusWindow() }, after) ?: return null
        val rule = rules.firstOrNull { it.scheduleId == boundary.scheduleId } ?: return null
        return scheduleNext(rule, after)
    }

    /** What the persisted rule's *currently pending* trigger represents - read this before
     * acting, since the receiver must know whether to start or release protection. */
    fun pendingAction(): FocusWindowAction? =
        prefs.getString(KEY_PENDING_ACTION, null)?.let { runCatching { FocusWindowAction.valueOf(it) }.getOrNull() }

    fun pendingTriggerAtEpochMillis(): Long? = prefs.getLong(KEY_NEXT_TRIGGER, -1L).takeIf { it > 0 }

    fun pendingRule(): Rule? {
        val scheduleId = prefs.getString(KEY_SCHEDULE_ID, null) ?: return null
        val start = prefs.getString(KEY_START, null)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
        val end = prefs.getString(KEY_END, null)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
        val days = prefs.getString(KEY_DAYS, null)?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet()
        if (days.isNullOrEmpty()) return null
        val packages = prefs.getStringSet(KEY_PACKAGES, null) ?: return null
        return Rule(scheduleId, days, start, end, packages)
    }

    /** Every window whose END/START must be considered. The armed rule is included even if it was
     * persisted before the window list existed, so a single-window install keeps its old behavior. */
    fun windows(): List<Rule> {
        val stored = storedWindows()
        val pending = pendingRule() ?: return stored
        return if (stored.any { it.scheduleId == pending.scheduleId }) stored else stored + pending
    }

    fun cancel() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(firePendingIntent())
        prefs.edit().clear().apply()
    }

    private fun storedWindows(): List<Rule> =
        prefs.getString(KEY_WINDOWS, null)?.lineSequence()?.mapNotNull { decodeWindow(it) }?.toList().orEmpty()

    private fun armNativeAlarm(triggerAtEpochMillis: Long): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return false
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, firePendingIntent())
        return true
    }

    private fun firePendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE, Intent(context, FocusScheduleReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val PREFS_NAME = "focus_schedule"
        private const val KEY_SCHEDULE_ID = "schedule_id"
        private const val KEY_DAYS = "days"
        private const val KEY_START = "start_time"
        private const val KEY_END = "end_time"
        private const val KEY_PACKAGES = "packages"
        private const val KEY_PENDING_ACTION = "pending_action"
        private const val KEY_NEXT_TRIGGER = "next_trigger_at"
        private const val KEY_WINDOWS = "windows"
        private const val REQUEST_CODE = 4301

        private fun encodeWindows(rules: List<Rule>): String =
            rules.joinToString("\n") { rule ->
                listOf(
                    rule.scheduleId,
                    rule.days.sorted().joinToString(","),
                    rule.startTime.toString(),
                    rule.endTime.toString(),
                    rule.blockPackages.sorted().joinToString(","),
                ).joinToString("\t")
            }

        private fun decodeWindow(line: String): Rule? {
            val parts = line.split('\t')
            if (parts.size != 5 || parts[0].isBlank()) return null
            val days = parts[1].split(',').mapNotNull { it.toIntOrNull() }.toSet()
            if (days.isEmpty()) return null
            val start = runCatching { LocalTime.parse(parts[2]) }.getOrNull() ?: return null
            val end = runCatching { LocalTime.parse(parts[3]) }.getOrNull() ?: return null
            val packages = parts[4].split(',').filter { it.isNotBlank() }.toSet()
            return Rule(parts[0], days, start, end, packages)
        }
    }
}

internal fun FocusScheduler.Rule.asFocusWindow() = FocusWindowEnd.Window(
    scheduleId, days, startTime, endTime, blockPackages,
)
