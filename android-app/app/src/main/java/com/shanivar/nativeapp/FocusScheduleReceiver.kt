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
 * Fires when a recurring focus schedule's next boundary elapses (see `FocusScheduler`) and starts
 * or releases protection accordingly, via the same `DisciplineEngine` a manual "Start protected
 * focus" tap would use - this receiver is a second trigger source for the existing focus
 * lifecycle, not a second lifecycle.
 *
 * Known limitation, inherited rather than introduced: like `MovementDismissalActivity`'s dismissal
 * recording, this depends on `ShanivarSession.api` already holding a session in this process (see
 * that object's doc comment). If the process was killed and no session is cached, a scheduled
 * window silently cannot start/end until the app is opened again - an accepted v1 gap, not
 * something to paper over. The schedule itself still advances to its next boundary regardless, so
 * a missed window does not repeat the same failure forever.
 */
class FocusScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = FocusScheduler(context)
        val rule = scheduler.pendingRule() ?: return
        val action = scheduler.pendingAction() ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val api = ShanivarSession.api
                if (api != null) {
                    val discipline = DisciplineEngine.create(context, api, AlarmScheduler(context))
                    runCatching {
                        when (action) {
                            FocusWindowAction.START -> {
                                val minutes = minutesUntilWindowEnd(rule)
                                if (minutes >= 1) discipline.startFocus(minutes, rule.blockPackages)
                            }
                            // Release only when no other stored window is still in effect.
                            // releaseFocus() clears the one shared session, so an unconditional
                            // call here would drop a sibling window that is still active.
                            FocusWindowAction.END -> FocusWindowEnd.apply(
                                endingScheduleId = rule.scheduleId,
                                windows = scheduler.windows().map { it.asFocusWindow() },
                                at = ZonedDateTime.now(),
                                discipline = discipline,
                            )
                        }
                    }
                }
            } finally {
                // Always advance, even if the action above failed or no session was available -
                // a schedule that gets permanently stuck retrying one missed boundary is worse
                // than one that silently skips it and tries again at the next occurrence.
                runCatching { scheduler.armSoonest() }
                pendingResult.finish()
            }
        }
    }

    /** How many whole minutes remain until the window's end from right now - computed fresh at
     * fire time, not stored, since `/focus/start` takes a duration, not an end timestamp. Only
     * meaningful when called at (or very near) the window's actual start; clamped to a full day
     * so an exact-alarm delivery delayed past the window's own end (Doze, device off) produces a
     * bounded, still-wrong-but-safe duration rather than accidentally reaching into next week's
     * occurrence and returning a multi-day session length. */
    private fun minutesUntilWindowEnd(rule: FocusScheduler.Rule): Int {
        val now = ZonedDateTime.now()
        val (end, _) = FocusWindowRecurrence.nextBoundary(rule.days, rule.startTime, rule.endTime, now) ?: return 0
        val minutes = (end.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()) / 60_000L
        return minutes.coerceIn(0L, 24 * 60L).toInt()
    }
}
