package com.shanivar.nativeapp

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/*
 * Shared Sadhana loading surface (wireframes/sadhana-v1/HANDOFF.md "Loading state contract", adapted for
 * PRACTICE_SAFETY; founder question recorded in decision 0017).
 *  - Shown only for genuine waits that outlast SHOW_AFTER_MS. Instant work never flashes it.
 *  - Explicit begin/end/error. A timeout resolves to a recoverable error with Retry.
 *  - Never shown while a practice is active.
 *  - Visual: NASA Cassini Saturn (PIA06193, see app/ASSET_NOTES.md) with a code-drawn ring layer turning
 *    steadily in one direction. No scale change, no in/out cycle, so it cannot pace breath. Static under reduced motion.
 */

sealed interface LoadState {
    data object Idle : LoadState
    data class Waiting(val label: String) : LoadState
    data class Visible(val label: String) : LoadState
    data class Failed(val label: String, val message: String) : LoadState
}

class SadhanaLoadingGate(
    val showAfterMs: Long = SHOW_AFTER_MS,
    val timeoutMs: Long = TIMEOUT_MS,
    private val practiceActive: () -> Boolean = { false },
) {
    var state: LoadState by mutableStateOf(LoadState.Idle)
        private set
    private var retryBlock: (suspend () -> Unit)? = null

    /** Runs [block]; shows the surface only if it outlasts [showAfterMs]; times out to a recoverable error. */
    suspend fun <T> run(label: String, block: suspend () -> T): Result<T> = coroutineScope {
        val blocking = !practiceActive()
        if (blocking) state = LoadState.Waiting(label)
        val reveal = launch { delay(showAfterMs); if (blocking && state is LoadState.Waiting) state = LoadState.Visible(label) }
        val result: Result<T> = try { Result.success(withTimeout(timeoutMs) { block() }) }
        catch (e: TimeoutCancellationException) { Result.failure(SadhanaLoadTimeout()) }
        catch (e: Exception) { Result.failure(e) }
        reveal.cancel()
        if (blocking) state = result.fold({ LoadState.Idle }, {
            LoadState.Failed(label, if (it is SadhanaLoadTimeout) "This is taking longer than expected. Try again, or continue without it." else (it.message ?: "Something went wrong. Try again."))
        })
        retryBlock = if (result.isFailure) ({ run(label, block) }) else null
        result
    }

    suspend fun retry() { retryBlock?.invoke() }
    fun dismiss() { state = LoadState.Idle; retryBlock = null }

    companion object { const val SHOW_AFTER_MS = 400L; const val TIMEOUT_MS = 12_000L }
}

class SadhanaLoadTimeout : Exception("timeout")

object SadhanaLoadingMotion {
    /** Ring layer: one full turn every 120 s, constant speed, one direction, restarting seamlessly. No scale change. */
    const val RING_TURN_MS = 120_000
    const val RING_PARTICLES = 160
    const val CREDIT = "Saturn image: NASA/JPL/Space Science Institute"
    // Ring geometry as fractions of the PIA06193 image (1280×654).
    const val CX = 0.495f; const val CY = 0.49f; const val RX = 0.404f; const val RY = 0.195f
    const val PLANET_R = 0.18f
    const val ASPECT = 1280f / 654f

    data class Particle(val angle: Float, val radius: Float, val size: Float, val warmth: Float)
    fun particles(seed: Int = 33): List<Particle> {
        val r = java.util.Random(seed.toLong())
        return List(RING_PARTICLES) { Particle(r.nextFloat() * 360f, 0.74f + r.nextFloat() * 0.26f, 0.6f + r.nextFloat() * 1.6f, r.nextFloat()) }
    }
    /** Angle of the ring layer at [elapsedMs]: monotonic, linear, same direction. */
    fun ringAngle(elapsedMs: Long): Float = (elapsedMs % RING_TURN_MS) * 360f / RING_TURN_MS
}

@Composable private fun SaturnWithRings(reduceMotion: Boolean) {
    val spin = if (reduceMotion) 0f else {
        val t = rememberInfiniteTransition(label = "rings")
        val v by t.animateFloat(0f, 360f, infiniteRepeatable(tween(SadhanaLoadingMotion.RING_TURN_MS, easing = LinearEasing), RepeatMode.Restart), label = "spin")
        v
    }
    val parts = remember { SadhanaLoadingMotion.particles() }
    Box(Modifier.fillMaxWidth().aspectRatio(SadhanaLoadingMotion.ASPECT)) {
        Image(painterResource(R.drawable.saturn_cassini_pia06193), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width * SadhanaLoadingMotion.CX; val cy = size.height * SadhanaLoadingMotion.CY
            val rx = size.width * SadhanaLoadingMotion.RX; val ry = size.height * SadhanaLoadingMotion.RY
            val pr = size.width * SadhanaLoadingMotion.PLANET_R
            parts.forEach { p ->
                val a = Math.toRadians((p.angle + spin).toDouble())
                val x = cx + (rx * p.radius * kotlin.math.cos(a)).toFloat()
                val y = cy + (ry * p.radius * kotlin.math.sin(a)).toFloat()
                val behind = kotlin.math.sin(a) < 0
                val dx = x - cx; val dy = y - size.height * 0.5f
                if (behind && dx * dx + dy * dy < pr * pr) return@forEach // hidden behind the planet
                val glow = (0.35f + 0.65f * ((kotlin.math.sin(a).toFloat() + 1f) / 2f)) // brighter on the near side
                val col = androidx.compose.ui.graphics.lerp(Color(0xFFFFF1D6), Color(0xFFE2B377), p.warmth)
                drawCircle(col.copy(alpha = 0.25f * glow), p.size.dp.toPx() * 2.6f, Offset(x, y))
                drawCircle(col.copy(alpha = 0.9f * glow), p.size.dp.toPx(), Offset(x, y))
            }
        }
    }
}

@Composable fun SadhanaLoadingSurface(state: LoadState, reduceMotion: Boolean, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val label = when (state) { is LoadState.Visible -> state.label; is LoadState.Failed -> state.label; else -> return }
    val failed = state as? LoadState.Failed
    Box(Modifier.fillMaxSize().background(Color.Black).semantics(mergeDescendants = failed == null) {
        if (failed == null) {
            contentDescription = "Shanivar is loading. $label"
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            liveRegion = LiveRegionMode.Polite
        }
    }) {
        Box(Modifier.fillMaxSize().padding(bottom = 120.dp), contentAlignment = Alignment.Center) { SaturnWithRings(reduceMotion || failed != null) }
        Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(start = 30.dp, end = 30.dp, bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SHANIVAR", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Text(label.uppercase(), color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, letterSpacing = 1.8.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 9.dp))
            if (failed != null) {
                Box(Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) {
                    Text(failed.message, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp, bottom = 12.dp))
                }
                SPrimary("Try again", onRetry)
                SText("Continue without it", onDismiss)
            }
            Text(SadhanaLoadingMotion.CREDIT, color = Color.White.copy(alpha = 0.45f), fontSize = 9.sp, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
