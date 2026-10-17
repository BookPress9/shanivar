package com.shanivar.nativeapp

import org.junit.Assert.*
import org.junit.Test

class ForegroundAppBlockerTest {
    @Test fun blocksWhenForegroundAppIsInTheBlockedSet() {
        assertTrue(ForegroundAppBlocker.shouldBlock("com.instagram.android", setOf("com.instagram.android"), 0, 1000))
    }

    @Test fun doesNotBlockUnlistedApps() {
        assertFalse(ForegroundAppBlocker.shouldBlock("com.android.chrome", setOf("com.instagram.android"), 0, 1000))
    }

    @Test fun doesNotBlockWithNoForegroundApp() {
        assertFalse(ForegroundAppBlocker.shouldBlock(null, setOf("com.instagram.android"), 0, 1000))
    }

    @Test fun doesNotBlockOnceTheSessionWindowHasElapsed() {
        val endsAt = 500L
        val now = 1000L
        assertFalse(ForegroundAppBlocker.shouldBlock("com.instagram.android", setOf("com.instagram.android"), endsAt, now))
    }

    @Test fun zeroEndsAtMeansNoExpiryYet() {
        // 0/unset is used as "no end time known" (e.g. before the backend confirms one) - it must
        // not be misread as "already expired."
        assertTrue(ForegroundAppBlocker.shouldBlock("com.instagram.android", setOf("com.instagram.android"), 0, 1_000_000))
    }
}
