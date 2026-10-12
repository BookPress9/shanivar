package com.shanivar.nativeapp

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
 *  - Motion: a single slow one-way zoom (no repeating in/out cycle) so it cannot pace breathing.
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
    const val START_SCALE = 1.015f
    const val END_SCALE = 1.065f
    const val STATIC_SCALE = 1.03f
    /** One direction, once. Slow enough to be barely perceptible; never reverses or repeats. */
    const val DRIFT_MS = 20_000
}

/** Debug-only image until ownership/production rights are verified (HANDOFF). Release falls back to black. */
@SuppressLint("DiscouragedApi")
@Composable private fun loadingImageId(): Int {
    val ctx = LocalContext.current
    return remember { ctx.resources.getIdentifier("sadhana_loading_debug_only", "drawable", ctx.packageName) }
}

@Composable fun SadhanaLoadingSurface(state: LoadState, reduceMotion: Boolean, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val label = when (state) { is LoadState.Visible -> state.label; is LoadState.Failed -> state.label; else -> return }
    val failed = state as? LoadState.Failed
    val scale = remember { Animatable(if (reduceMotion) SadhanaLoadingMotion.STATIC_SCALE else SadhanaLoadingMotion.START_SCALE) }
    LaunchedEffect(reduceMotion, failed == null) {
        if (!reduceMotion && failed == null) scale.animateTo(SadhanaLoadingMotion.END_SCALE, tween(SadhanaLoadingMotion.DRIFT_MS, easing = LinearEasing))
    }
    val img = loadingImageId()
    Box(Modifier.fillMaxSize().background(Color.Black).semantics(mergeDescendants = failed == null) {
        if (failed == null) {
            contentDescription = "Shanivar is loading. $label"
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            liveRegion = LiveRegionMode.Polite
        }
    }, contentAlignment = Alignment.BottomCenter) {
        if (img != 0) Image(painterResource(img), contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale.value; scaleY = scale.value })
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.48f to Color(0x14000000), 1f to Color(0xC2000000))))
        Column(Modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp, bottom = 58.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
        }
    }
}
