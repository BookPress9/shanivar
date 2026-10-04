package com.shanivar.nativeapp

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

private class FakePermissions(
    var usageAccess: Boolean = true,
    var overlay: Boolean = true,
    var exactAlarm: Boolean = true,
    var notification: Boolean = true,
) : DisciplinePermissions {
    override fun hasUsageAccess() = usageAccess
    override fun hasOverlayPermission() = overlay
    override fun hasExactAlarmPermission() = exactAlarm
    override fun hasNotificationPermission() = notification
}

private class FakeDisciplineApi : DisciplineApi {
    var active: FocusSessionInfo? = null
    var startFailure: ApiFailure? = null
    var releaseFailure: ApiFailure? = null
    var startCalls = 0
    var releaseCalls = 0
    var lastReleasedSessionId: String? = null
    val dismissals = mutableListOf<Triple<String, Boolean, JSONObject>>()

    override suspend fun activeFocus(): FocusSessionInfo? = active

    override suspend fun startFocus(minutes: Int, packageNames: Set<String>): FocusSessionInfo {
        startCalls++
        startFailure?.let { throw it }
        val info = FocusSessionInfo(id = "session-1", endsAtEpochMillis = 1_000_000L, packageNames = packageNames)
        active = info
        return info
    }

    override suspend fun releaseFocus(sessionId: String) {
        releaseCalls++
        lastReleasedSessionId = sessionId
        releaseFailure?.let { throw it }
        active = null
    }

    override suspend fun recordDismissal(method: String, verified: Boolean, detail: JSONObject, alarmScheduleId: String?) {
        dismissals += Triple(method, verified, detail)
    }
}

private class FakeGuard : FocusGuardController {
    var startCalls = 0
    var stopCalls = 0
    var lastPackages: Set<String>? = null
    var lastEndsAt: Long? = null

    override fun start(packageNames: Set<String>, endsAtEpochMillis: Long) {
        startCalls++
        lastPackages = packageNames
        lastEndsAt = endsAtEpochMillis
    }

    override fun stop() { stopCalls++ }
}

class DisciplineEngineTest {
    @Test fun expiredServerSessionDoesNotClaimProtectionOrBlockNextStart() = runBlocking {
        val api = FakeDisciplineApi().apply {
            active = FocusSessionInfo("elapsed", 1L, setOf("com.instagram.android"))
        }
        val guard = FakeGuard()
        val engine = DisciplineEngine(FakePermissions(), api, guard)
        assertEquals(ProtectionState.OFF, engine.status().protection)
        assertEquals(0, guard.startCalls)
        engine.startFocus(15, setOf("com.instagram.android"))
        assertEquals("elapsed", api.lastReleasedSessionId)
        assertEquals(1, api.startCalls)
    }

    private fun engine(
        permissions: FakePermissions = FakePermissions(),
        api: FakeDisciplineApi = FakeDisciplineApi(),
        guard: FakeGuard = FakeGuard(),
        alarmScheduled: Boolean = false,
    ) = Triple(DisciplineEngine(permissions, api, guard) { alarmScheduled }, api, guard)

    @Test fun statusReportsNeedsPermissionWithoutTouchingTheBackend() = runBlocking {
        val permissions = FakePermissions(usageAccess = false)
        val (engine, api, _) = engine(permissions)
        val status = engine.status()
        assertEquals(ProtectionState.NEEDS_PERMISSION, status.protection)
        assertEquals(0, api.startCalls) // activeFocus() isn't a "call" we track, but nothing should throw either
    }

    @Test fun statusReportsNeedsPermissionWhenOnlyOverlayIsMissing() = runBlocking {
        val (engine, _, _) = engine(FakePermissions(usageAccess = true, overlay = false))
        assertEquals(ProtectionState.NEEDS_PERMISSION, engine.status().protection)
    }

    @Test fun statusReportsOffWhenPermittedAndNoActiveSession() = runBlocking {
        val (engine, _, _) = engine()
        val status = engine.status()
        assertEquals(ProtectionState.OFF, status.protection)
    }

    @Test fun statusReportsActiveWithEndTimeAndPackagesFromTheBackend() = runBlocking {
        val api = FakeDisciplineApi().apply {
            active = FocusSessionInfo(id = "session-9", endsAtEpochMillis = Long.MAX_VALUE, packageNames = setOf("com.instagram.android"))
        }
        val (engine, _, _) = engine(api = api)
        val status = engine.status()
        assertEquals(ProtectionState.ACTIVE, status.protection)
        assertEquals(Long.MAX_VALUE, status.focusEndsAtEpochMillis)
        assertEquals(setOf("com.instagram.android"), status.protectedPackages)
    }

    @Test fun statusSurfacesAlarmScheduledFlagIndependentlyOfFocusState() = runBlocking {
        val (engine, _, _) = engine(alarmScheduled = true)
        assertTrue(engine.status().alarmScheduled)
    }

    @Test fun statusMapsNetworkFailureToDisciplineErrorNetwork() = runBlocking {
        val api = FakeDisciplineApi()
        val permissions = FakePermissions()
        val guard = FakeGuard()
        val failing = object : DisciplineApi by api {
            override suspend fun activeFocus(): FocusSessionInfo? = throw ApiFailure(500, "boom")
        }
        val engine = DisciplineEngine(permissions, failing, guard)
        val error = runCatching { engine.status() }.exceptionOrNull()
        assertTrue(error is DisciplineError.Network)
    }

    @Test fun startFocusRefusesLocallyWhenPermissionsAreMissingAndNeverCallsTheBackend() = runBlocking {
        val (engine, api, guard) = engine(FakePermissions(usageAccess = false))
        val error = runCatching { engine.startFocus(25, setOf("com.instagram.android")) }.exceptionOrNull()
        assertTrue(error is DisciplineError.PermissionRequired)
        assertEquals(0, api.startCalls)
        assertEquals(0, guard.startCalls)
    }

    @Test fun startFocusStartsTheGuardAndReturnsActiveOnSuccess() = runBlocking {
        val (engine, _, guard) = engine()
        val status = engine.startFocus(25, setOf("com.instagram.android"))
        assertEquals(ProtectionState.ACTIVE, status.protection)
        assertEquals(1, guard.startCalls)
        assertEquals(setOf("com.instagram.android"), guard.lastPackages)
        assertEquals(1_000_000L, guard.lastEndsAt)
    }

    @Test fun startFocusMapsAnEntitlementFailureDistinctlyAndNeverStartsTheGuard() = runBlocking {
        val api = FakeDisciplineApi().apply { startFailure = ApiFailure(402, "Upgrade required") }
        val (engine, _, guard) = engine(api = api)
        val error = runCatching { engine.startFocus(25, setOf("com.instagram.android")) }.exceptionOrNull()
        assertTrue(error is DisciplineError.EntitlementRequired)
        assertEquals(0, guard.startCalls)
    }

    @Test fun startFocusMapsAGenericServerFailureToNetworkNotEntitlement() = runBlocking {
        val api = FakeDisciplineApi().apply { startFailure = ApiFailure(500, "Server error") }
        val (engine, _, guard) = engine(api = api)
        val error = runCatching { engine.startFocus(25, setOf("com.instagram.android")) }.exceptionOrNull()
        assertTrue(error is DisciplineError.Network)
        assertEquals(0, guard.startCalls)
    }

    @Test fun releaseFocusUsesTheSessionIdFromStartAndStopsTheGuardOnSuccess() = runBlocking {
        val (engine, api, guard) = engine()
        engine.startFocus(25, setOf("com.instagram.android")) // establishes activeSessionId = "session-1"
        val status = engine.releaseFocus()
        assertEquals(ProtectionState.OFF, status.protection)
        assertEquals(1, guard.stopCalls)
        assertEquals(1, api.releaseCalls)
        assertEquals("session-1", api.lastReleasedSessionId)
    }

    @Test fun releaseFocusWithNoKnownOrActiveSessionIsANoOpSuccess() = runBlocking {
        // Mirrors the backend's own idempotent release semantics: nothing to release is treated
        // as already-released, not an error - the engine must still stop the local guard.
        val (engine, api, guard) = engine()
        val status = engine.releaseFocus()
        assertEquals(ProtectionState.OFF, status.protection)
        assertEquals(1, guard.stopCalls)
        assertEquals(0, api.releaseCalls)
    }

    @Test fun releaseFocusRediscoversTheSessionIdViaActiveFocusWhenNotLocallyTracked() = runBlocking {
        // Simulates a process restart: the engine never called status()/startFocus() this
        // process lifetime, so activeSessionId is null, but the backend still has an active
        // session - releaseFocus() must look it up rather than silently no-op'ing.
        val api = FakeDisciplineApi().apply {
            active = FocusSessionInfo(id = "session-42", endsAtEpochMillis = 1L, packageNames = emptySet())
        }
        val (engine, _, guard) = engine(api = api)
        val status = engine.releaseFocus()
        assertEquals(ProtectionState.OFF, status.protection)
        assertEquals(1, guard.stopCalls)
        assertEquals("session-42", api.lastReleasedSessionId)
    }

    @Test fun releaseFocusStopsTheGuardEvenWhenTheBackendCallFails() = runBlocking {
        // INVERTED for Routine V3 (decision 0014), and the old assertion is worth remembering.
        //
        // This used to assert the opposite, citing NATIVE_HANDOFF.md's "on release failure keep the
        // session screen open with retry" - the goal being not to drop protection the server still
        // thought was active. That reasoning protected the wrong party: it also meant a person with
        // no signal could not get their own apps back, because the API call threw before
        // guard.stop() was ever reached.
        //
        // 0014 settles it: "the local device state is authoritative for scheduling and recovery",
        // and immediate stop must not need a server response. A stale server session is reconciled
        // by the next status()/startFocus(); a user locked out of their phone is not reconciled by
        // anything.
        val api = FakeDisciplineApi().apply { releaseFailure = ApiFailure(500, "Server error") }
        val (engine, _, guard) = engine(api = api)
        engine.startFocus(25, setOf("com.instagram.android")) // establishes a session id so release actually calls the backend
        val status = engine.releaseFocus()
        assertEquals(ProtectionState.OFF, status.protection)
        assertEquals(1, guard.stopCalls)
        // The failure is swallowed rather than surfaced: the user-visible operation succeeded.
        assertEquals(1, api.releaseCalls)
    }

    @Test fun statusKeepsReportingActiveFromTheDeviceRecordWhenTheServerIsUnreachable() = runBlocking {
        // Offline, mid-window. The guard is still blocking apps, so reporting OFF would tell the
        // person their apps are available while the overlay keeps appearing, and raising a network
        // error would complain about a question the device can already answer.
        val api = FakeDisciplineApi()
        val failing = object : DisciplineApi by api {
            override suspend fun activeFocus(): FocusSessionInfo? = throw ApiFailure(500, "offline")
        }
        val guard = FakeGuard()
        val stored = LocalProtection(setOf("com.instagram.android"), System.currentTimeMillis() + 600_000L)
        val engine = DisciplineEngine(FakePermissions(), failing, guard, { stored })
        val status = engine.status()
        assertEquals(ProtectionState.ACTIVE, status.protection)
        assertEquals(setOf("com.instagram.android"), status.protectedPackages)
        assertEquals(stored.endsAtEpochMillis, status.focusEndsAtEpochMillis)
    }

    @Test fun statusStillErrorsOfflineWhenTheDeviceRecordHasElapsed() = runBlocking {
        // An elapsed record is not an active window, so there is nothing truthful to report and the
        // network failure is the honest answer.
        val api = FakeDisciplineApi()
        val failing = object : DisciplineApi by api {
            override suspend fun activeFocus(): FocusSessionInfo? = throw ApiFailure(500, "offline")
        }
        val elapsed = LocalProtection(setOf("com.instagram.android"), System.currentTimeMillis() - 1_000L)
        val engine = DisciplineEngine(FakePermissions(), failing, FakeGuard(), { elapsed })
        assertTrue(runCatching { engine.status() }.exceptionOrNull() is DisciplineError.Network)
    }
}
