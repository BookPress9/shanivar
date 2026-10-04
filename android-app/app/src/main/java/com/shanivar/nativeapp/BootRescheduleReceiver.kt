package com.shanivar.nativeapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * `AlarmManager` entries do not survive a reboot, so a pending alarm or focus-schedule trigger
 * scheduled before a restart would silently vanish without this.
 *
 * v2 (MVP streamline task, 2026-09-18): recomputes the next occurrence from the persisted rule
 * relative to *now*, rather than blindly re-arming whatever instant was last stored. Two reasons:
 * (1) the device clock/timezone can have moved while it was off, so the old stored instant may no
 * longer be the correct wall-clock moment; (2) if the device was off through the alarm's actual
 * trigger time, that specific occurrence is gone - recomputing forward finds the next valid
 * occurrence per the recurrence rule instead of either firing hours late or (the v1 behavior)
 * silently dropping the alarm's recurrence forever. This still does not retroactively fire the
 * missed occurrence itself; deciding whether a missed wake-up alarm should fire immediately on
 * boot is a separate product decision, not an engineering default to guess at.
 *
 * v3 (2026-09-19, focus schedules): the focus side makes the OPPOSITE catch-up choice from the
 * alarm side, deliberately - see docs/decisions/0010-focus-schedule-when-then-commitment.md. An
 * alarm is a single instant; missing it and firing late has no "partial credit." A focus window
 * is a *span*: if the device was off through 9:00 and boots at 9:20 into a 9-5 window, the correct
 * behavior is to start protecting now, for whatever remains of the window - not to wait, silent
 * and unprotected, until tomorrow's 9:00. Skipping the catch-up here would be the more consistent
 * engineering choice but the wrong product one.
 */
class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) return

        val alarmScheduler = AlarmScheduler(context)
        alarmScheduler.pendingRule()?.let { rule ->
            alarmScheduler.scheduleRule(rule.sessionId, rule.localTime, rule.days)
        }

        val focusScheduler = FocusScheduler(context)
        val focusRule = focusScheduler.pendingRule() ?: return
        val now = ZonedDateTime.now()
        // Re-arm the correct next boundary regardless of whether we're currently inside the
        // window - FocusWindowRecurrence.nextBoundary already resolves to today's END when
        // called from inside an active window, with no special-casing needed here.
        runCatching { focusScheduler.scheduleNext(focusRule, now) }
        // One call answers both "is an occurrence running?" and "when does *it* end?". Previously
        // this asked isActiveAt() and then re-derived the end from nextBoundary(), which was correct
        // only because the guard above happened to precede it - the same shape that became a
        // 24-hour session in FocusScheduleReceiver once its guard was absent (DEV-1).
        val activeEnd = FocusWindowRecurrence.activeWindowEnd(
            focusRule.days, focusRule.startTime, focusRule.endTime, now,
        ) ?: return

        // We are inside the window and it was missed (device off or rebooting through its start), so
        // start protection now for whatever remains - via the same best-effort session bridge
        // FocusScheduleReceiver and MovementDismissalActivity already depend on.
        val api = ShanivarSession.api ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val minutes = ((activeEnd.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()) / 60_000L)
                    .coerceIn(1L, 24 * 60L).toInt()
                val discipline = DisciplineEngine.create(context, api, alarmScheduler)
                runCatching { discipline.startFocus(minutes, focusRule.blockPackages) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
