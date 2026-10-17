package com.shanivar.nativeapp

import org.junit.Assert.*
import org.junit.Test

class SadhanaFocusLockTest {
    class FakeLock(val status: FocusLockStatus) : SadhanaFocusLockPort {
        var engaged = 0; var released = 0; var endsAt = 0L
        override fun engage(endsAtEpochMillis: Long): FocusLockStatus { engaged++; endsAt = endsAtEpochMillis; return status }
        override fun release() { released++ }
    }

    @Test fun allModeBlocksEverythingExceptEssentialAndOwn() {
        val set = FocusLockRules.packages("com.shanivar.nativeapp", setOf("com.my.launcher"))
        val b = { pkg: String -> ForegroundAppBlocker.shouldBlock(pkg, set, Long.MAX_VALUE, 1) }
        assertTrue(b("com.instagram.android"))
        assertFalse(b("com.shanivar.nativeapp")); assertFalse(b("com.google.android.dialer")); assertFalse(b("com.my.launcher")); assertFalse(b("com.android.emergency"))
        assertTrue(ForegroundAppBlocker.shouldBlock("x", setOf("x"), Long.MAX_VALUE, 1))
    }

    @Test fun lockOnlyDuringPracticeAndEndReleasesAtOnce() {
        val lock = FakeLock(FocusLockStatus(true, false))
        val clock = FakeSadhanaClock()
        val c = SadhanaController(MemDraftStore(), clock, true, focusLock = lock)
        val releasedAtStart = lock.released
        c.setStationary(true); c.begin()
        assertEquals(1, lock.engaged); assertTrue(c.focusStatus!!.appsBlocked)
        assertTrue(lock.endsAt > clock.wall())
        c.end()
        assertEquals(releasedAtStart + 1, lock.released); assertNull(c.focusStatus)
    }

    @Test fun defaultCanBeTurnedOffAndPersists() {
        val lock = FakeLock(FocusLockStatus(true, true)); val store = MemDraftStore()
        val c = SadhanaController(store, FakeSadhanaClock(), true, focusLock = lock)
        assertTrue(c.ui.focusLockDefault)
        c.setFocusLockDefault(false)
        val c2 = SadhanaController(store, FakeSadhanaClock(), true, focusLock = lock)
        assertFalse(c2.ui.focusLockDefault)
        c2.setStationary(true); c2.begin()
        assertEquals(0, lock.engaged)
    }

    @Test fun missingPermissionsAreStatedHonestly() {
        assertTrue(FocusLockStatus(false, false).line.contains("Nothing is blocked"))
        assertTrue(FocusLockStatus(true, false).line.contains("not silenced"))
        listOf(FocusLockStatus(true, true), FocusLockStatus(true, false)).forEach { assertTrue(it.line.contains("emergency")) }
    }
}
