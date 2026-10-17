package com.shanivar.nativeapp

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * The bounded local pause from the MVP streamline task (package 5): a fixed one-minute, fully
 * local/offline observation with a clear ending. Deliberately self-contained (no account, no
 * network, no navigation dependency) so the exact same composable serves two different callers -
 * [OnboardingFlow]'s pre-account preview, and [BlockOverlayActivity]'s in-the-moment offer - without
 * either depending on the other's Activity/ViewModel graph.
 *
 * Non-negotiables this screen exists to satisfy: it never releases focus, changes entitlement, or
 * dismisses an alarm - it has no reference to any of those systems at all, which is what makes
 * "never" actually true rather than merely intended. It requires no free-text disclosure - only a
 * fixed prompt and a timer. [isPreview] only changes copy (the explicit "does not block other
 * apps" disclosure), never behavior.
 *
 * [prompt] comes from the reviewed practice catalogue (see [rememberGroundingPrompt]). It used to be
 * one of four hardcoded lines picked at random, one of which told people to breathe out slower than
 * they breathe in - unreviewed breath advice, which decision 0011 does not allow anywhere in the app.
 * A null prompt shows the timer with no instruction at all, which is the honest state when the
 * catalogue could not be read: better to say nothing than to invent guidance.
 */
@Composable
fun LocalPauseScreen(isPreview: Boolean, prompt: String? = null, onExit: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var deadline by remember { mutableStateOf(0L) }
    var remaining by remember { mutableStateOf(DURATION_MILLIS) }
    LaunchedEffect(deadline, paused, started) {
        if (started && !paused) {
            while (true) {
                remaining = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                if (remaining == 0L) break
                delay(200)
            }
        }
    }
    val complete = started && remaining <= 0
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Heading(if (complete) "That's the pause." else "A one-minute pause.")
            if (isPreview) Text("This pause does not block other apps.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            when {
                !started -> {
                    Text("One minute, on your terms. Pause or leave whenever you want.")
                    PrimaryAction("Begin") {
                        remaining = DURATION_MILLIS
                        deadline = SystemClock.elapsedRealtime() + DURATION_MILLIS
                        started = true
                    }
                    TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Not now") }
                }
                !complete -> {
                    val seconds = (remaining + 999) / 1000
                    Text("%d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    prompt?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = {
                            if (paused) deadline = SystemClock.elapsedRealtime() + remaining
                            else remaining = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                            paused = !paused
                        }) { Text(if (paused) "Resume" else "Pause") }
                        TextButton(onClick = onExit) { Text("End now") }
                    }
                }
                else -> {
                    Text("However that felt, it's information. Nothing to fix.")
                    PrimaryAction("Done", onExit)
                }
            }
        }
    }
}

private const val DURATION_MILLIS = 60_000L
