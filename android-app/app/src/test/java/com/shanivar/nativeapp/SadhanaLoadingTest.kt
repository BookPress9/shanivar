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
    @Test fun ringsSpinSteadilyOneDirectionNoScalePulse() {
        assertEquals(0f, SadhanaLoadingMotion.ringAngle(0), 0f)
        assertEquals(90f, SadhanaLoadingMotion.ringAngle(30_000), 0.01f)
        assertTrue("monotonic within a turn", SadhanaLoadingMotion.ringAngle(60_000) > SadhanaLoadingMotion.ringAngle(59_000))
        assertTrue("far slower than any breath cycle", SadhanaLoadingMotion.RING_TURN_MS >= 60_000)
        val src = File("src/main/java/com/shanivar/nativeapp/SadhanaLoading.kt").readText()
        assertFalse("no in/out cycle", src.contains("RepeatMode.Reverse"))
        assertFalse("no scale pulse", Regex("scaleX|scaleY|graphicsLayer|Animatable").containsMatchIn(src))
        assertFalse("never named breathing", Regex("(?i)breath(e|ing)").containsMatchIn(src.replace("pace breath", "")))
        assertFalse("kaleidoscope removed", src.contains("kaleido") || src.contains("sadhana_loading_debug_only"))
    }
    @Test fun saturnAssetHasRecordedSourceAndCredit() {
        val notes = File("ASSET_NOTES.md").readText()
        assertTrue(notes.contains("PIA06193") && notes.contains("NASA/JPL/Space Science Institute") && notes.contains("images.nasa.gov"))
        assertTrue(File("src/main/res/drawable-nodpi/saturn_cassini_pia06193.jpg").exists())
        assertFalse(File("src/debug/res/drawable-nodpi/sadhana_loading_debug_only.jpg").exists())
    }
}
