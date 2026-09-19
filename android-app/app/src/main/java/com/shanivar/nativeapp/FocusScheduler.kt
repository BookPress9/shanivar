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
 * v1 scope: exactly one focus schedule at a time, matching the existing one-alarm limit. A second
 * concurrent schedule is a real later capability, not a hidden bug - see the ADR's "Revisit when."
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
        prefs.edit()
            .putString(KEY_SCHEDULE_ID, rule.scheduleId)
            .putString(KEY_DAYS, rule.days.sorted().joinToString(","))
            .putString(KEY_START, rule.startTime.toString())
            .putString(KEY_END, rule.endTime.toString())
            .putStringSet(KEY_PACKAGES, rule.blockPackages)
            .putString(KEY_PENDING_ACTION, action.name)
            .putLong(KEY_NEXT_TRIGGER, triggerAt)
            .apply()
        return triggerAt to action
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

    fun cancel() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(firePendingIntent())
        prefs.edit().clear().apply()
    }

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
        private const val REQUEST_CODE = 4301
    }
}
