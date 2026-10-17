package com.shanivar.nativeapp

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class RoutineSyncTest {
    private val zone = "Europe/Berlin"
    private fun routine(wake: String) = SyncedRoutine(zone, RoutinePlanV1(wakeTime = LocalTime.parse(wake), intendedSleepTime = LocalTime.parse("23:00")), RoutineSeed.practiceFor(LocalTime.parse(wake)))

    private class Remote(vararg results: RemoteSaveResult) : RoutineRemote {
        val queue = results.toMutableList(); val sent = mutableListOf<RoutineMutation>()
        override suspend fun putRoutine(mutation: RoutineMutation): RemoteSaveResult { sent += mutation; return queue.removeAt(0) }
    }

    private var keys = 0
    private fun coordinator(remote: RoutineRemote, saved: MutableList<RoutinePlanV1> = mutableListOf(), fail: Boolean = false) =
        RoutineSyncCoordinator({ if (fail) Result.failure(IllegalStateException("disk")) else { saved += it; Result.success(Unit) } }, remote, newKey = { "k${++keys}" })

    @Test fun practiceSeededAtWakePlusFive() {
        assertEquals(LocalTime.parse("07:05"), RoutineSeed.practiceFor(LocalTime.parse("07:00")))
        assertEquals(LocalTime.parse("00:02"), RoutineSeed.practiceFor(LocalTime.parse("23:57")))
        assertNull(RoutineSeed.practiceFor(null))
    }

    @Test fun localSaveFirstEvenWhenOffline() = runTest {
        val saved = mutableListOf<RoutinePlanV1>()
        val c = coordinator(UnconfiguredRoutineRemote, saved)
        assertTrue(c.save(routine("07:00"), signedIn = true).isSuccess)
        assertEquals(1, saved.size)
        assertEquals(SyncStatus.OFFLINE, c.flush().status)
        assertEquals("k1", c.state.pending?.idempotencyKey)
    }

    @Test fun guestStaysLocalOnly() = runTest {
        val c = coordinator(Remote())
        c.save(routine("07:00"), signedIn = false)
        assertEquals(SyncStatus.LOCAL_ONLY, c.state.status); assertNull(c.state.pending)
    }

    @Test fun localFailureDoesNotQueue() = runTest {
        val c = coordinator(Remote(), fail = true)
        assertTrue(c.save(routine("07:00"), true).isFailure)
        assertNull(c.state.pending)
    }

    @Test fun retryReusesIdempotencyKeyAndRevisionAdvances() = runTest {
        val remote = Remote(RemoteSaveResult.Offline, RemoteSaveResult.Saved(1))
        val c = coordinator(remote)
        c.save(routine("07:00"), true); c.flush()
        c.save(routine("07:00"), true) // identical payload: same key
        assertEquals(SyncStatus.SYNCED, c.flush().status)
        assertEquals(listOf("k1", "k1"), remote.sent.map { it.idempotencyKey })
        assertEquals(1, c.state.serverRevision)
        c.save(routine("06:30"), true)
        assertEquals(1, c.state.pending?.expectedRevision)
        assertEquals("k2", c.state.pending?.idempotencyKey)
    }

    @Test fun conflictIsSurfacedNotOverwritten() = runTest {
        val canonical = routine("08:00")
        val remote = Remote(RemoteSaveResult.Conflict(canonical, 5), RemoteSaveResult.Saved(6))
        val saved = mutableListOf<RoutinePlanV1>()
        val c = coordinator(remote, saved)
        c.save(routine("07:00"), true)
        assertEquals(SyncStatus.CONFLICT, c.flush().status)
        assertEquals(1, saved.size) // server copy not applied silently
        val after = c.keepMine()
        assertEquals(SyncStatus.SYNCED, after.status); assertEquals(6, after.serverRevision)
        assertEquals(5, remote.sent.last().expectedRevision)
    }

    @Test fun takeServerAppliesCanonicalLocally() = runTest {
        val canonical = routine("08:00")
        val saved = mutableListOf<RoutinePlanV1>()
        val c = coordinator(Remote(RemoteSaveResult.Conflict(canonical, 3)), saved)
        c.save(routine("07:00"), true); c.flush()
        assertTrue(c.takeServer().isSuccess)
        assertEquals(canonical.plan, saved.last()); assertEquals(3, c.state.serverRevision)
    }

    @Test fun authExpiryAndRateLimitKeepPending() = runTest {
        val c = coordinator(Remote(RemoteSaveResult.AuthExpired, RemoteSaveResult.RateLimited(30), RemoteSaveResult.Invalid("bad")))
        c.save(routine("07:00"), true)
        assertEquals(SyncStatus.NEEDS_SIGN_IN, c.flush().status)
        assertEquals(30L, c.flush().retryAfterSeconds)
        assertEquals(SyncStatus.REJECTED, c.flush().status)
        assertTrue(c.state.pending != null)
    }

    @Test fun dstGapResolvesToNextValidInstant() {
        val berlin = ZoneId.of(zone)
        val r = HandoffTimePolicy.resolve(LocalDate.parse("2026-03-29"), LocalTime.parse("02:30"), berlin)
        assertEquals(LocalTime.parse("03:00"), r.toLocalTime())
        // documents the difference from java.time default used by existing schedulers
        assertEquals(LocalTime.parse("03:30"), LocalDate.parse("2026-03-29").atTime(2, 30).atZone(berlin).toLocalTime())
    }

    @Test fun dstOverlapResolvesToFirstOccurrence() {
        val berlin = ZoneId.of(zone)
        val r = HandoffTimePolicy.resolve(LocalDate.parse("2026-10-25"), LocalTime.parse("02:30"), berlin)
        assertEquals(2, r.offset.totalSeconds / 3600) // CEST, the first 02:30
    }

    @Test fun zoneValidation() {
        assertTrue(HandoffTimePolicy.isValidZone("Asia/Kolkata"))
        assertFalse(HandoffTimePolicy.isValidZone("Mars/Base"))
        assertFalse(HandoffTimePolicy.isValidZone("UTC+2"))
    }
}
