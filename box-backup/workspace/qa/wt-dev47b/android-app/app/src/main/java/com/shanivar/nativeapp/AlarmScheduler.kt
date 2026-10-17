package com.shanivar.nativeapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Wraps `AlarmManager.setAlarmClock` - platform-research.md's recommendation over
 * `setExactAndAllowWhileIdle` "for maximum Doze exemption... the strongest Doze-exemption tier
 * available to a non-privileged app; this is what real alarm-clock apps use."
 *
 * v2 (MVP streamline task, 2026-09-18): persists the full recurrence RULE (local time + selected
 * days), not just a single trigger instant. `AlarmManager` can only ever hold one pending instant,
 * so "repeat on Mon/Wed/Fri" requires something to recompute and re-arm the *next* occurrence every
 * time the current one fires - v1 scheduled exactly one occurrence and nothing ever did that, so a
 * repeating alarm silently stopped repeating after its first ring. [AlarmRecurrence.nextOccurrence]
 * is the pure function that turns (time, days, from) into the next matching instant; this class is
 * the thin persistence + `AlarmManager` wrapper around it.
 *
 * Persists to SharedPreferences for three reasons: (1) so `DisciplineEngine`/
 * `DisciplineStatus.alarmScheduled` can answer synchronously without an IPC round-trip to
 * AlarmManager, (2) so `BootRescheduleReceiver` can restore the alarm after a reboot, since
 * AlarmManager entries do not survive one, and (3) so the rule itself (not just the next instant)
 * survives a reboot, which is what lets boot-time recovery *recompute* rather than merely resume.
 */
class AlarmScheduler(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class Rule(val sessionId: String, val localTime: LocalTime, val days: Set<Int>)

    /**
     * Persists [sessionId]/[localTime]/[days] as the current rule and arms the native alarm for
     * its next occurrence strictly after [after]. Returns the scheduled trigger time in epoch
     * millis, or null without scheduling or persisting anything if exact-alarm permission isn't
     * currently granted, or if [days] is invalid - callers should check
     * `DisciplinePermissions.hasExactAlarmPermission()` first and surface that distinctly rather
     * than silently no-op scheduling.
     */
    fun scheduleRule(sessionId: String, localTime: LocalTime, days: Set<Int>, after: ZonedDateTime = ZonedDateTime.now()): Long? {
        val occurrence = AlarmRecurrence.nextOccurrence(localTime, days, after) ?: return null
        val triggerAt = occurrence.toInstant().toEpochMilli()
        if (!armNativeAlarm(triggerAt, sessionId)) return null
        prefs.edit()
            .putString(KEY_SESSION_ID, sessionId)
            .putString(KEY_LOCAL_TIME, localTime.toString())
            .putString(KEY_DAYS, days.sorted().joinToString(","))
            .putLong(KEY_NEXT_TRIGGER, triggerAt)
            .apply()
        return triggerAt
    }

    /**
     * Advances the persisted rule to its next matching occurrence after [after] - call this once
     * the current ring has ended, whether by verified dismissal or by the ringer's own hard cap,
     * so a repeating alarm keeps repeating. A no-op returning null if no rule is currently
     * persisted (the alarm was cancelled/deleted between firing and this call, which is a normal
     * race, not an error).
     */
    fun advanceToNextOccurrence(after: ZonedDateTime = ZonedDateTime.now()): Long? {
        val rule = pendingRule() ?: return null
        return scheduleRule(rule.sessionId, rule.localTime, rule.days, after)
    }

    fun cancel() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(firePendingIntent(prefs.getString(KEY_SESSION_ID, "").orEmpty()))
        prefs.edit().remove(KEY_NEXT_TRIGGER).remove(KEY_SESSION_ID).remove(KEY_LOCAL_TIME).remove(KEY_DAYS).apply()
    }

    fun hasScheduledAlarm(): Boolean = prefs.contains(KEY_NEXT_TRIGGER)

    fun pendingTriggerAtEpochMillis(): Long? = prefs.getLong(KEY_NEXT_TRIGGER, -1L).takeIf { it > 0 }

    fun pendingSessionId(): String? = prefs.getString(KEY_SESSION_ID, null)

    /** The persisted rule, or null if nothing is scheduled or the stored rule is unreadable
     * (corrupt/cleared prefs) - treated as "no rule" rather than a crash. */
    fun pendingRule(): Rule? {
        val sessionId = prefs.getString(KEY_SESSION_ID, null) ?: return null
        val localTime = prefs.getString(KEY_LOCAL_TIME, null)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
        val days = prefs.getString(KEY_DAYS, null)?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet()
        if (days.isNullOrEmpty()) return null
        return Rule(sessionId, localTime, days)
    }

    private fun armNativeAlarm(triggerAtEpochMillis: Long, sessionId: String): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return false
        val showIntent = PendingIntent.getActivity(
            context, REQUEST_CODE, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val firePendingIntent = firePendingIntent(sessionId)
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtEpochMillis, showIntent), firePendingIntent)
        return true
    }

    private fun firePendingIntent(sessionId: String): PendingIntent {
        val fireIntent = Intent(context, AlarmFireReceiver::class.java).putExtra(AlarmFireReceiver.EXTRA_SESSION_ID, sessionId)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, fireIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val PREFS_NAME = "discipline_alarm"
        private const val KEY_NEXT_TRIGGER = "next_trigger_at"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_LOCAL_TIME = "rule_local_time"
        private const val KEY_DAYS = "rule_days"
        private const val REQUEST_CODE = 4201
    }
}
