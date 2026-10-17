package com.shanivar.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `LocalProtectionController` — the device-only protection path added for DEV-4.
 *
 * The property under test is mostly an absence: this class can start and stop app protection with no
 * account, no session and no network, because it has no way to reach any of them. The scheduled
 * boundary used to require all three, so a window silently did nothing after process death.
 */
class LocalProtectionTest {
    private class FakeStore(var stored: LocalProtection? = null) : ProtectionStateStore {
        override fun read(): LocalProtection? = stored
    }

    private class FakeGuardController : FocusGuardController {
        var startCalls = 0
        var stopCalls = 0
        var lastPackages: Set<String>? = null
        var lastEndsAt: Long? = null
        override fun start(packageNames: Set<String>, endsAtEpochMillis: Long) {
            startCalls++; lastPackages = packageNames; lastEndsAt = endsAtEpochMillis
        }
        override fun stop() { stopCalls++ }
    }

    private class FakePerms(var usage: Boolean = true, var overlay: Boolean = true) : DisciplinePermissions {
        override fun hasUsageAccess() = usage
        override fun hasOverlayPermission() = overlay
        override fun hasExactAlarmPermission() = true
        override fun hasNotificationPermission() = true
    }

    private val now = 1_800_000_000_000L

    private fun controller(
        store: FakeStore = FakeStore(),
        guard: FakeGuardController = FakeGuardController(),
        perms: FakePerms = FakePerms(),
    ) = Triple(LocalProtectionController(store, guard, perms) { now }, guard, store)

    @Test fun startsProtectionWithoutAnyAccountOrNetworkInvolved() {
        val (local, guard, _) = controller()
        val endsAt = now + 30 * 60_000L
        assertTrue(local.start(setOf("com.instagram.android"), endsAt))
        assertEquals(1, guard.startCalls)
        assertEquals(setOf("com.instagram.android"), guard.lastPackages)
        assertEquals(endsAt, guard.lastEndsAt)
    }

    @Test fun stopNowIsImmediateAndIdempotent() {
        val (local, guard, _) = controller()
        local.stopNow()
        local.stopNow()
        assertEquals(2, guard.stopCalls)
    }

    @Test fun refusesToStartWhenTheDeviceCannotActuallyEnforce() {
        // Claiming success without Usage access or overlay permission would be the
        // "never claim enforcement we cannot deliver" failure, so this returns false rather than
        // starting a guard that cannot see or block anything.
        val (noUsage, guardA, _) = controller(perms = FakePerms(usage = false))
        assertFalse(noUsage.start(setOf("com.instagram.android"), now + 60_000L))
        assertEquals(0, guardA.startCalls)

        val (noOverlay, guardB, _) = controller(perms = FakePerms(overlay = false))
        assertFalse(noOverlay.start(setOf("com.instagram.android"), now + 60_000L))
        assertEquals(0, guardB.startCalls)
    }

    @Test fun refusesDegenerateStartRequests() {
        val (local, guard, _) = controller()
        assertFalse(local.start(emptySet(), now + 60_000L))      // nothing to protect
        assertFalse(local.start(setOf("com.x"), now))            // already elapsed
        assertFalse(local.start(setOf("com.x"), now - 60_000L))  // in the past
        assertEquals(0, guard.startCalls)
    }

    @Test fun activeReflectsTheDeviceRecordAndTreatsAnElapsedWindowAsInactive() {
        val store = FakeStore()
        val (local, _, _) = controller(store = store)
        assertNull(local.active())

        store.stored = LocalProtection(setOf("com.instagram.android"), now + 60_000L)
        assertEquals(setOf("com.instagram.android"), local.active()?.packages)

        store.stored = LocalProtection(setOf("com.instagram.android"), now - 1L)
        assertNull("an elapsed record is not active protection", local.active())

        // A record with no packages cannot be enforcing anything either.
        store.stored = LocalProtection(emptySet(), now + 60_000L)
        assertNull(local.active())
    }

    @Test fun theLocalPathNamesNoApiTypeAtAll() {
        // Structural, and the reason DEV-4 is fixed rather than mitigated: if this file could name
        // an API type, a future change could put a network call back on the enforcement path.
        val source = java.io.File("src/main/java/com/shanivar/nativeapp/LocalProtection.kt").readText()
        val body = source.lines()
            .filterNot { it.trimStart().startsWith("*") || it.trimStart().startsWith("//") || it.trimStart().startsWith("/*") }
            .joinToString("\n")
        listOf("ShanivarApi", "DisciplineApi", "ShanivarSession", "HttpDisciplineApi", "suspend")
            .forEach { assertFalse("LocalProtection.kt must not reference $it", body.contains(it)) }
    }
}
