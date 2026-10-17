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
 * against the boundary that was armed. A START whose window already closed does nothing (DEV-1).
 *
 * ## END releases only what the closing occurrence owns (DEV-2 / FOCUS-5)
 *
 * END used to call `local.stopNow()` and then `discipline.releaseFocus()` with no notion of who
 * owned the protection in force. That tore down a manual focus session that outlived the window,
 * and - once R2 adds a second rule - would tear down a different, still-active window. END is now
 * decided by [FocusWindowEndHandler]: release only when nothing else is still valid, leave
 * protection that outlives the closing occurrence alone, and narrow to the still-active windows
 * when one ends inside another. The server is told to release only when the device released.
 */
class FocusScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = FocusScheduler(context)
        val rule = scheduler.pendingRule() ?: return
        val action = scheduler.pendingAction() ?: return
        // The instant that was armed is the end of the occurrence an END is closing. Read it before
        // rearm() overwrites it below.
        val armedAt = scheduler.pendingTriggerAtEpochMillis()
        val now = ZonedDateTime.now()
        val activeEnd = FocusWindowRecurrence.activeWindowEnd(rule.days, rule.startTime, rule.endTime, now)
        // R2: every enabled rule participates in END ownership (DEV-2 handler already accepts a list).
        val allEnabled = scheduler.enabledRules().ifEmpty { listOf(rule) }

        // ── 1. Device action, local, no session ─────────────────────────────────────────────────
        val local = LocalProtectionController.create(context)
        var mirror: Mirror? = null
        when (action) {
            FocusWindowAction.START -> {
                if (activeEnd != null && local.start(rule.blockPackages, activeEnd.toInstant().toEpochMilli())) {
                    mirror = Mirror.Start(wholeMinutesUntil(activeEnd.toInstant().toEpochMilli(), now), rule.blockPackages)
                }
            }
            FocusWindowAction.END -> {
                val outcome = FocusWindowEndHandler.apply(
                    rules = allEnabled,
                    closingEndEpochMillis = armedAt ?: now.toInstant().toEpochMilli(),
                    now = now,
                    local = local,
                )
                mirror = when {
                    outcome.result == FocusWindowEndHandler.Result.RELEASED -> Mirror.Release
                    // A still-active occurrence the device just took over; mirror it like a START.
                    // Not when a foreign session was folded in - the server holds one session and
                    // releasing or replacing someone else's is exactly the bug being fixed.
                    outcome.result == FocusWindowEndHandler.Result.CONTINUED && !outcome.absorbedForeign ->
                        Mirror.Start(wholeMinutesUntil(outcome.endsAtEpochMillis!!, now), outcome.packages)
                    else -> null
                }
            }
        }

        // ── 2. Always advance the global next boundary across the whole registry ───────────────
        // A schedule stuck retrying one missed boundary forever is worse than one that skips it and
        // tries again at the next occurrence.
        runCatching { scheduler.rearm(now) }

        // ── 3. Server mirror, best effort ───────────────────────────────────────────────────────
        // Secondary by design. No session means no mirror, and that is not a failure of the window -
        // the device is already enforcing the right thing.
        val api = ShanivarSession.api
        val change = mirror
        if (change == null || api == null) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val discipline = DisciplineEngine.create(context, api, AlarmScheduler(context))
                runCatching {
                    when (change) {
                        is Mirror.Start -> discipline.startFocus(change.minutes, change.packages)
                        Mirror.Release -> discipline.releaseFocus()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private sealed interface Mirror {
        data class Start(val minutes: Int, val packages: Set<String>) : Mirror
        data object Release : Mirror
    }

    /**
     * Whole minutes from [now] until [endEpochMillis], for the server's `/focus/start`, which takes
     * a duration rather than an end timestamp.
     *
     * The clamp is a backstop, not the correctness mechanism. Correctness comes from the caller
     * passing the end of the occurrence active *right now* - see
     * [FocusWindowRecurrence.activeWindowEnd]. This previously took whatever `nextBoundary`
     * returned and called it "the end", so a late delivery measured to the *next* occurrence's
     * START instead and produced a 24-hour session (DEV-1).
     */
    private fun wholeMinutesUntil(endEpochMillis: Long, now: ZonedDateTime): Int {
        val minutes = (endEpochMillis - now.toInstant().toEpochMilli()) / 60_000L
        return minutes.coerceIn(0L, MAX_WINDOW_MINUTES).toInt()
    }

    private companion object {
        /** A single occurrence cannot legitimately exceed one day. */
        const val MAX_WINDOW_MINUTES = 24 * 60L
    }
}

/**
 * What a focus window END does to local protection (DEV-2 / FOCUS-5).
 *
 * Effective-rule union: protection stays while **any** rule is active, and an END never shortens
 * or narrows protection that something other than the closing occurrence owns.
 *
 * The device keeps one guard with one package set and one end time ([LocalProtection]) and no
 * owner field, so ownership is inferred from time: protection whose end is later than the closing
 * occurrence's end (beyond [OWNERSHIP_SLACK_MS]) was not started for that occurrence - it is a
 * manual focus session or another window - and is "foreign". A START for an occurrence always
 * records exactly [FocusWindowRecurrence.activeWindowEnd], which is the END instant, so the
 * occurrence's own protection is never mistaken for foreign.
 *
 * Decision, evaluated at delivery time:
 * 1. No rule active and nothing foreign → [Result.RELEASED] (stop; idempotent cleanup after a
 *    late or stale END too).
 * 2. No rule active, foreign protection in force → [Result.KEPT], untouched.
 * 3. Some rule still active (another window, or the next occurrence of the same rule when this END
 *    was delivered late) → protect the union of their packages until the *earliest* of their ends;
 *    that rule's own END recomputes again. If foreign protection already covers that, [Result.KEPT].
 *    Otherwise restart with the union plus any foreign packages, never ending before the foreign
 *    end → [Result.CONTINUED]. Errs toward protecting: with one guard, a still-active window's apps
 *    may stay paused until a longer foreign session ends.
 * 4. The device cannot enforce the still-active rule (permissions revoked) → [Result.CANNOT_ENFORCE];
 *    foreign protection is left as it was, otherwise the closing occurrence's guard is stopped.
 *
 * No session, API or network parameter, on purpose - same rule as [BootFocusCatchUp].
 */
internal object FocusWindowEndHandler {
    enum class Result { RELEASED, KEPT, CONTINUED, CANNOT_ENFORCE }

    data class Outcome(
        val result: Result,
        /** Packages protected after this END; empty when released or nothing could be enforced. */
        val packages: Set<String> = emptySet(),
        /** End of protection in force after this END; null when released or nothing enforced. */
        val endsAtEpochMillis: Long? = null,
        /** True when foreign protection was merged into a [Result.CONTINUED] restart. */
        val absorbedForeign: Boolean = false,
    )

    /** START records the END instant exactly; the slack only tolerates sub-minute clock rounding. */
    const val OWNERSHIP_SLACK_MS = 60_000L

    fun apply(
        rules: List<FocusScheduler.Rule>,
        closingEndEpochMillis: Long,
        now: ZonedDateTime,
        local: LocalProtectionController,
    ): Outcome {
        val stillActive = rules.mapNotNull { rule ->
            FocusWindowRecurrence.activeWindowEnd(rule.days, rule.startTime, rule.endTime, now)
                ?.let { rule to it.toInstant().toEpochMilli() }
        }
        val foreign = local.active()?.takeIf { it.endsAtEpochMillis > closingEndEpochMillis + OWNERSHIP_SLACK_MS }

        if (stillActive.isEmpty()) {
            if (foreign != null) return Outcome(Result.KEPT, foreign.packages, foreign.endsAtEpochMillis)
            local.stopNow()
            return Outcome(Result.RELEASED)
        }

        val windowPackages = stillActive.flatMapTo(mutableSetOf()) { it.first.blockPackages }
        val windowEnd = stillActive.minOf { it.second }
        if (foreign != null && foreign.packages.containsAll(windowPackages) && foreign.endsAtEpochMillis >= windowEnd) {
            return Outcome(Result.KEPT, foreign.packages, foreign.endsAtEpochMillis)
        }
        val packages = windowPackages + foreign?.packages.orEmpty()
        val endsAt = maxOf(windowEnd, foreign?.endsAtEpochMillis ?: windowEnd)
        if (local.start(packages, endsAt)) {
            return Outcome(Result.CONTINUED, packages, endsAt, absorbedForeign = foreign != null)
        }
        if (foreign != null) return Outcome(Result.CANNOT_ENFORCE, foreign.packages, foreign.endsAtEpochMillis)
        local.stopNow()
        return Outcome(Result.CANNOT_ENFORCE)
    }
}
