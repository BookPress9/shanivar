package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class RoutineTimelineTest {
    private val zone = ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 9, 14, 8, 0, 0, 0, zone) // Monday

    @Test fun timelineUsesStoredStateNotWireframeFixtures() {
        val plan = RoutinePlanV1(
            wakeTime = LocalTime.of(5, 45),
            intendedSleepTime = LocalTime.of(21, 15),
            activities = listOf(
                RoutineActivityV1("a1", "Deep work", LocalTime.of(10, 0), 90, (0..6).toSet(), true),
            ),
        )
        val items = RoutineSnapshotComposer.compose(RoutineSnapshot(plan = plan, now = now))
        assertTrue(items.any { it.kind == TimelineKind.WAKE && it.timeLabel == "05:45" })
        assertTrue(items.any { it.kind == TimelineKind.SLEEP && it.timeLabel == "21:15" })
        assertTrue(items.any { it.kind == TimelineKind.ACTIVITY && it.title == "Deep work" })
        // Wireframe samples must not appear when not stored.
        assertFalse(items.any { it.title == "Gym" })
    }

    @Test fun personalActivitiesNeverCarryDeviceStatus() {
        val plan = RoutinePlanV1(
            activities = listOf(
                RoutineActivityV1("a1", "Read", LocalTime.of(21, 0), 30, (0..6).toSet(), true),
            ),
        )
        val items = RoutineSnapshotComposer.compose(
            RoutineSnapshot(plan = plan, alarmArmed = true, protectionStatusLabel = "Active", now = now),
        )
        val activity = items.first { it.kind == TimelineKind.ACTIVITY }
        assertEquals(null, activity.deviceStatus)
    }

    @Test fun wakeRowCanShowAlarmReadyWithoutClaimingActiveFromSaveAlone() {
        val plan = RoutinePlanV1(wakeTime = LocalTime.of(7, 0))
        val savedOnly = RoutineSnapshotComposer.compose(RoutineSnapshot(plan = plan, alarmArmed = false, now = now))
        assertTrue(savedOnly.first { it.kind == TimelineKind.WAKE }.deviceStatus == null)

        val armed = RoutineSnapshotComposer.compose(
            RoutineSnapshot(
                plan = plan,
                alarmArmed = true,
                nextAlarmEpochMillis = now.plusHours(1).toInstant().toEpochMilli(),
                now = now,
            ),
        )
        assertEquals("Ready", armed.first { it.kind == TimelineKind.WAKE }.deviceStatus)
    }

    @Test fun itemsSortByNextOccurrence() {
        val plan = RoutinePlanV1(
            wakeTime = LocalTime.of(6, 0),
            intendedSleepTime = LocalTime.of(22, 0),
            activities = listOf(
                RoutineActivityV1("a1", "Evening read", LocalTime.of(20, 0), 30, (0..6).toSet(), true),
            ),
        )
        val items = RoutineSnapshotComposer.compose(RoutineSnapshot(plan = plan, now = now))
            .filter { it.kind != TimelineKind.GUIDANCE }
        val keys = items.map { it.sortKeyEpochMillis }
        assertEquals(keys.sorted(), keys)
    }

    @Test fun wireframeCopyContractLivesOnRoutineScreen() {
        val src = java.io.File("src/main/java/com/shanivar/nativeapp/RoutineScreen.kt").readText()
        assertTrue(src.contains("YOUR DAY"))
        assertTrue(src.contains("Not an ideal morning.\\nYour actual life."))
        assertTrue(src.contains("Edit Routine"))
    }
}
