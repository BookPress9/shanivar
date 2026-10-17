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
 *
 * v3 local-first (DEV-21): that catch-up is a device action. It uses [LocalProtectionController]
 * and [FocusWindowRecurrence.activeWindowEnd] with no account and no network, then re-arms the
 * next boundary, and only afterwards mirrors to the server if a session happens to already be warm.
 * Same order as [FocusScheduleReceiver]. A missing session is not a reason to stay unprotected.
 */
class BootRescheduleReceiver : BroadcastReceiver() {
    companion object {
        /** App updates also wipe AlarmManager entries, so MY_PACKAGE_REPLACED reconciles too. */
        val RECONCILE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RECONCILE_ACTIONS) return

        val alarmScheduler = AlarmScheduler(context)
        alarmScheduler.pendingRule()?.let { rule ->
            alarmScheduler.scheduleRule(rule.sessionId, rule.localTime, rule.days)
        }

        val focusScheduler = FocusScheduler(context)
        val focusRules = focusScheduler.enabledRules()
        if (focusRules.isEmpty()) {
            // Still try legacy single pending rule so pre-migration installs recover.
            focusScheduler.pendingRule()?.let { legacy ->
                val now = ZonedDateTime.now()
                BootFocusCatchUp.apply(
                    rules = listOf(legacy),
                    now = now,
                    local = LocalProtectionController.create(context),
                    rearm = { at -> focusScheduler.rearm(at) },
                )
            }
            return
        }
        val now = ZonedDateTime.now()

        // ── 1 + 2. Local catch-up and the next boundary, with no session ───────────────────────
        val outcome = BootFocusCatchUp.apply(
            rules = focusRules,
            now = now,
            local = LocalProtectionController.create(context),
            rearm = { at -> focusScheduler.rearm(at) },
        )

        // ── 3. Server mirror, best effort ───────────────────────────────────────────────────────
        val api = ShanivarSession.api
        val minutes = outcome.mirrorMinutes
        if (!outcome.started || api == null || minutes == null) return
        val pendingResult = goAsync()
        val packages = outcome.packages
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val discipline = DisciplineEngine.create(context, api, alarmScheduler)
                runCatching { discipline.startFocus(minutes, packages) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * Reboot (and clock-change) catch-up for the focus registry.
 *
 * Overlap is rejected in the first release, so at most one occurrence should be active. If more
 * than one is somehow active, packages are unioned and protection lasts until the earliest end
 * (same ownership stance as [FocusWindowEndHandler]).
 *
 * No session/API/network parameter on purpose. Mirror only after [Outcome.started].
 */
internal object BootFocusCatchUp {
    data class Outcome(
        /** True only when local protection actually started for an occurrence active at [now]. */
        val started: Boolean,
        /** Whole minutes until that occurrence ends. Null when protection did not start. */
        val mirrorMinutes: Int?,
        /** Packages started locally — for the optional server mirror. */
        val packages: Set<String> = emptySet(),
    )

    fun apply(
        rules: List<FocusScheduler.Rule>,
        now: ZonedDateTime,
        local: LocalProtectionController,
        rearm: (ZonedDateTime) -> Unit,
    ): Outcome {
        val active = rules.mapNotNull { rule ->
            FocusWindowRecurrence.activeWindowEnd(rule.days, rule.startTime, rule.endTime, now)
                ?.let { rule to it }
        }
        val started: Boolean
        val minutes: Int?
        val packages: Set<String>
        if (active.isEmpty()) {
            started = false
            minutes = null
            packages = emptySet()
        } else {
            packages = active.flatMapTo(mutableSetOf()) { it.first.blockPackages }
            val end = active.minOf { it.second }
            started = local.start(packages, end.toInstant().toEpochMilli())
            minutes = if (started) wholeMinutesUntil(end, now) else null
        }
        // Always re-arm the global next boundary. AlarmManager was wiped by the reboot.
        runCatching { rearm(now) }
        return Outcome(started, minutes, packages)
    }

    /** Single-rule convenience used by older unit tests. */
    fun apply(
        rule: FocusScheduler.Rule,
        now: ZonedDateTime,
        local: LocalProtectionController,
        scheduleNext: (FocusScheduler.Rule, ZonedDateTime) -> Unit,
    ): Outcome = apply(
        rules = listOf(rule),
        now = now,
        local = local,
        rearm = { at -> scheduleNext(rule, at) },
    )

    /**
     * Whole minutes from [now] until [end], for the server's `/focus/start`, which takes a duration
     * rather than an end timestamp. The clamp is a backstop; the end itself comes from the
     * occurrence active right now.
     */
    internal fun wholeMinutesUntil(end: ZonedDateTime, now: ZonedDateTime): Int {
        val minutes = (end.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()) / 60_000L
        return minutes.coerceIn(0L, MAX_WINDOW_MINUTES).toInt()
    }

    private const val MAX_WINDOW_MINUTES = 24 * 60L
}
