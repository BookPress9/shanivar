package com.shanivar.nativeapp

import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Composes the Routine timeline from stored plan + optional device status.
 * Pure domain: no SharedPreferences, no schedulers. Device-backed rows carry status labels;
 * personal activities never claim alarm or Focus enforcement.
 */
enum class TimelineKind { WAKE, ACTIVITY, PROTECTION, SLEEP, GUIDANCE }

data class TimelineItem(
    val kind: TimelineKind,
    val title: String,
    val timeLabel: String,
    val detail: String? = null,
    /** Device status copy only — null for calendar-only rows. */
    val deviceStatus: String? = null,
    val sortKeyEpochMillis: Long,
)

data class RoutineSnapshot(
    val plan: RoutinePlanV1,
    val decodeHealthy: Boolean = true,
    val nextAlarmEpochMillis: Long? = null,
    val alarmArmed: Boolean = false,
    val protectionStatusLabel: String? = null,
    /** Per-window device status (from [RoutineProtectionStatus]); falls back to [protectionStatusLabel]. */
    val protectionStatusByWindowId: Map<String, String> = emptyMap(),
    val now: ZonedDateTime = ZonedDateTime.now(),
)

object RoutineSnapshotComposer {
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    fun compose(snapshot: RoutineSnapshot): List<TimelineItem> {
        val items = mutableListOf<TimelineItem>()
        val now = snapshot.now
        val plan = snapshot.plan

        // Wake anchor — around the next wake relative to now.
        plan.wakeTime?.let { wake ->
            val whenWake = nextClock(wake, now)
            val alarmDetail = when {
                !snapshot.decodeHealthy -> "Needs attention"
                snapshot.alarmArmed && snapshot.nextAlarmEpochMillis != null ->
                    "Alarm ready · next ${formatEpoch(snapshot.nextAlarmEpochMillis, now)}"
                snapshot.alarmArmed -> "Alarm configured"
                else -> null
            }
            items += TimelineItem(
                kind = TimelineKind.WAKE,
                title = "Wake",
                timeLabel = wake.format(timeFmt),
                detail = alarmDetail,
                deviceStatus = if (snapshot.alarmArmed) "Ready" else null,
                sortKeyEpochMillis = whenWake.toInstant().toEpochMilli(),
            )
            // Optional daylight guidance — labelled as guidance, not a protocol.
            items += TimelineItem(
                kind = TimelineKind.GUIDANCE,
                title = "Daylight, if you can",
                timeLabel = wake.plusMinutes(15).format(timeFmt),
                detail = "Guidance — not a measured protocol.",
                sortKeyEpochMillis = whenWake.plusMinutes(15).toInstant().toEpochMilli(),
            )
        }

        plan.activities.filter { it.enabled }.forEach { activity ->
            val start = RoutineActivityRecurrence.nextStart(activity, now) ?: return@forEach
            items += TimelineItem(
                kind = TimelineKind.ACTIVITY,
                title = activity.title,
                timeLabel = activity.startTime.format(timeFmt),
                detail = "${activity.durationMinutes} min · ${RoutineActivityOccurrence.label(activity.days)}",
                deviceStatus = null,
                sortKeyEpochMillis = start.toInstant().toEpochMilli(),
            )
        }

        plan.windows.filter { it.enabled }.forEach { window ->
            val boundary = FocusWindowRecurrence.nextBoundary(
                window.startDays, window.startTime, window.endTime, now,
            ) ?: return@forEach
            val (instant, action) = boundary
            val startLabel = window.startTime.format(timeFmt)
            val endLabel = window.endTime.format(timeFmt)
            val status = snapshot.protectionStatusByWindowId[window.id] ?: snapshot.protectionStatusLabel
            items += TimelineItem(
                kind = TimelineKind.PROTECTION,
                title = if (action == FocusWindowAction.START) "App protection" else "App protection ends",
                timeLabel = if (FocusWindowRecurrence.isFullCalendarDay(window.startTime, window.endTime)) "All day"
                    else "$startLabel–$endLabel",
                detail = ProtectionWindowGeometry.describe(window),
                deviceStatus = status,
                sortKeyEpochMillis = instant.toInstant().toEpochMilli(),
            )
        }

        plan.intendedSleepTime?.let { sleep ->
            val whenSleep = nextClock(sleep, now)
            items += TimelineItem(
                kind = TimelineKind.SLEEP,
                title = "Intended sleep",
                timeLabel = sleep.format(timeFmt),
                detail = "Changing this does not change your alarm.",
                sortKeyEpochMillis = whenSleep.toInstant().toEpochMilli(),
            )
        }

        return items.sortedBy { it.sortKeyEpochMillis }
    }

    private fun nextClock(time: LocalTime, after: ZonedDateTime): ZonedDateTime {
        val today = after.toLocalDate().atTime(time).atZone(after.zone)
        return if (today.isAfter(after)) today else today.plusDays(1)
    }

    private fun formatEpoch(epochMillis: Long, now: ZonedDateTime): String {
        val zdt = java.time.Instant.ofEpochMilli(epochMillis).atZone(now.zone)
        return zdt.toLocalTime().format(timeFmt)
    }
}
