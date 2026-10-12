package com.shanivar.nativeapp

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SadhanaLoadingTest {
    @Test fun instantWorkNeverShowsSurface() = runTest {
        val g = SadhanaLoadingGate()
        val seen = mutableListOf<LoadState>()
        val r = g.run("Preparing Sadhana") { seen += g.state; 42 }
        assertEquals(42, r.getOrNull()); assertEquals(LoadState.Idle, g.state)
        assertTrue(seen.none { it is LoadState.Visible })
    }
    @Test fun slowWorkShowsAfterThresholdThenClears() = runTest {
        val g = SadhanaLoadingGate()
        val job = launch { g.run("Preparing Sadhana") { delay(2_000); 1 } }
        advanceTimeBy(300); runCurrent(); assertTrue(g.state is LoadState.Waiting)
        advanceTimeBy(200); runCurrent(); assertEquals(LoadState.Visible("Preparing Sadhana"), g.state)
        job.join(); assertEquals(LoadState.Idle, g.state)
    }
    @Test fun timeoutBecomesRecoverableErrorAndRetryWorks() = runTest {
        val g = SadhanaLoadingGate(timeoutMs = 1_000)
        var slow = true
        val r = g.run("Opening source context") { if (slow) delay(5_000); "ok" }
        assertTrue(r.exceptionOrNull() is SadhanaLoadTimeout)
        assertTrue(g.state is LoadState.Failed)
        slow = false; g.retry(); assertEquals(LoadState.Idle, g.state)
    }
    @Test fun errorsSurfaceAndDismissClears() = runTest {
        val g = SadhanaLoadingGate()
        g.run<Unit>("x") { error("Source unavailable") }
        assertEquals("Source unavailable", (g.state as LoadState.Failed).message)
        g.dismiss(); assertEquals(LoadState.Idle, g.state)
    }
    @Test fun neverShownDuringPractice() = runTest {
        val g = SadhanaLoadingGate(practiceActive = { true })
        val job = launch { g.run("Opening source context") { delay(3_000) } }
        advanceTimeBy(1_000); runCurrent(); assertEquals(LoadState.Idle, g.state); job.join()
        g.run<Unit>("x") { error("boom") }; assertEquals(LoadState.Idle, g.state)
    }
    @Test fun motionIsOneWayAndNotBreathPaced() {
        assertTrue(SadhanaLoadingMotion.END_SCALE > SadhanaLoadingMotion.START_SCALE)
        assertTrue("slower than any breath cycle", SadhanaLoadingMotion.DRIFT_MS >= 15_000)
        val src = File("src/main/java/com/shanivar/nativeapp/SadhanaLoading.kt").readText()
        assertFalse(src.contains("RepeatMode") || src.contains("infiniteRepeatable"))
        assertFalse("never named breathing", Regex("(?i)breath(e|ing)").containsMatchIn(src.replace("cannot pace breathing", "")))
    }
}
