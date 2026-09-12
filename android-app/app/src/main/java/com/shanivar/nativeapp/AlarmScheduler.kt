package com.shanivar.nativeapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Wraps `AlarmManager.setAlarmClock` - platform-research.md's recommendation over
 * `setExactAndAllowWhileIdle` "for maximum Doze exemption... the strongest Doze-exemption tier
 * available to a non-privileged app; this is what real alarm-clock apps use."
 *
 * Persists the pending trigger/session to SharedPreferences for two reasons: (1) so
 * `DisciplineEngine`/`DisciplineStatus.alarmScheduled` can answer synchronously without an IPC
 * round-trip to AlarmManager, and (2) so `BootRescheduleReceiver` can restore the alarm after a
 * reboot, since AlarmManager entries do not survive one.
 */
class AlarmScheduler(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns false without scheduling anything if exact-alarm permission isn't currently
     * granted - callers should check `DisciplinePermissions.hasExactAlarmPermission()` first and
     * surface that distinctly rather than silently no-op scheduling. */
    fun schedule(triggerAtEpochMillis: Long, sessionId: String): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return false
        val showIntent = PendingIntent.getActivity(
            context, REQUEST_CODE, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val firePendingIntent = firePendingIntent(sessionId)
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtEpochMillis, showIntent), firePendingIntent)
        prefs.edit()
            .putLong(KEY_NEXT_TRIGGER, triggerAtEpochMillis)
            .putString(KEY_SESSION_ID, sessionId)
            .apply()
        return true
    }

    fun cancel() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(firePendingIntent(prefs.getString(KEY_SESSION_ID, "").orEmpty()))
        prefs.edit().remove(KEY_NEXT_TRIGGER).remove(KEY_SESSION_ID).apply()
    }

    fun hasScheduledAlarm(): Boolean = prefs.contains(KEY_NEXT_TRIGGER)

    fun pendingTriggerAtEpochMillis(): Long? = prefs.getLong(KEY_NEXT_TRIGGER, -1L).takeIf { it > 0 }

    fun pendingSessionId(): String? = prefs.getString(KEY_SESSION_ID, null)

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
        private const val REQUEST_CODE = 4201
    }
}
