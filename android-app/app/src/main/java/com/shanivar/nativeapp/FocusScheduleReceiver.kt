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
 * Fires when a recurring focus window's next boundary elapses (see [FocusScheduler]) and starts or
 * stops protection accordingly.
 *
 * ## Order of authority (decision 0014)
 *
 * The device acts **first**, synchronously, with no session and no network. Only afterwards, and
 * only if an account session happens to already be warm, is the change mirrored to the server.
 *
 * This is the opposite of how it used to work. Previously the whole body was inside
 * `if (ShanivarSession.api != null)`, so after process death a scheduled window silently did
 * nothing while the schedule advanced past it (DEV-4) - the user's protection just failed to
 * happen, with no error anywhere. 0014 is explicit that "a warm process, network connection or
 * server response must not be required for a scheduled boundary or immediate stop".
 *
 * ## Staleness
 *
 * An exact alarm can arrive long after its instant (Doze, device powered off), so both actions are
 * decided against [FocusWindowRecurrence.activeWindowEnd] evaluated at *delivery* time rather than
 * against the boundary that was armed. A START whose window already closed does nothing; an END
 * that arrives after a later occurrence has opened leaves that occurrence alone (DEV-1, DEV-2).
 */
class FocusScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = FocusScheduler(context)
        val rule = scheduler.pendingRule() ?: return
        val action = scheduler.pendingAction() ?: return
        val now = ZonedDateTime.now()
        val activeEnd = FocusWindowRecurrence.activeWindowEnd(rule.days, rule.startTime, rule.endTime, now)

        // ── 1. Device action, local and unconditional ───────────────────────────────────────────
        val local = LocalProtectionController.create(context)
        val acted = when (action) {
            FocusWindowAction.START ->
                activeEnd != null && local.start(rule.blockPackages, activeEnd.toInstant().toEpochMilli())
            FocusWindowAction.END -> {
                if (activeEnd == null) local.stopNow()
                activeEnd == null
            }
        }

        // ── 2. Always advance, whatever happened above ──────────────────────────────────────────
        // A schedule stuck retrying one missed boundary forever is worse than one that skips it and
        // tries again at the next occurrence.
        runCatching { scheduler.scheduleNext(rule) }

        // ── 3. Server mirror, best effort ───────────────────────────────────────────────────────
        // Secondary by design. No session means no mirror, and that is not a failure of the window -
        // the device is already enforcing the right thing.
        val api = ShanivarSession.api
        if (!acted || api == null) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val discipline = DisciplineEngine.create(context, api, AlarmScheduler(context))
                runCatching {
                    when (action) {
                        FocusWindowAction.START ->
                            discipline.startFocus(wholeMinutesUntil(activeEnd!!, now), rule.blockPackages)
                        FocusWindowAction.END -> discipline.releaseFocus()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Whole minutes from [now] until [end], for the server's `/focus/start`, which takes a duration
     * rather than an end timestamp.
     *
     * The clamp is a backstop, not the correctness mechanism. Correctness comes from the caller
     * passing the end of the occurrence active *right now* - see
     * [FocusWindowRecurrence.activeWindowEnd]. This previously took whatever `nextBoundary`
     * returned and called it "the end", so a late delivery measured to the *next* occurrence's
     * START instead and produced a 24-hour session (DEV-1).
     */
    private fun wholeMinutesUntil(end: ZonedDateTime, now: ZonedDateTime): Int {
        val minutes = (end.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()) / 60_000L
        return minutes.coerceIn(0L, MAX_WINDOW_MINUTES).toInt()
    }

    private companion object {
        /** A single occurrence cannot legitimately exceed one day. */
        const val MAX_WINDOW_MINUTES = 24 * 60L
    }
}
