package com.shanivar.nativeapp

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.io.File
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

object SystemSadhanaClock : SadhanaClock {
    override fun wall() = System.currentTimeMillis()
    override fun mono() = SystemClock.elapsedRealtime()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}

/** Sadhana V1 host (decision 0017). Debug builds only via BuildConfig.SADHANA_V1. */
@Composable fun SadhanaApp(
    controllerFactory: (Context) -> SadhanaController = { ctx ->
        val plans = SharedPrefsRoutinePlanRepository(ctx)
        SadhanaController(FileDraftStore(File(ctx.filesDir, "sadhana/state.properties")), SystemSadhanaClock, allowUnreviewed = BuildConfig.DEBUG,
            routineBoundaryLinked = { RoutineBoundaryLink.linked(plans.current()) },
            initiationAttempt = { InitiationUiCodec.decode(FileDraftStore(File(ctx.filesDir, "initiation/ui.properties")).read())?.attempt })
    },
    reduceMotionOverride: Boolean? = null,
    onRoutine: (() -> Unit)? = null,
    /** Source retrieval boundary. Null result = reader not available yet (no backend). */
    sourcePort: suspend (lane: String) -> String? = { null },
    hydrateDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
    gateFactory: (() -> Boolean) -> SadhanaLoadingGate = { active -> SadhanaLoadingGate(practiceActive = active) },
) {
    val context = LocalContext.current
    val reduceMotion = reduceMotionOverride ?: remember { initiationReduceMotion(context) }
    var controller by remember { mutableStateOf<SadhanaController?>(null) }
    val gate = remember { gateFactory { controller?.practiceInProgress == true } }
    val scope = rememberCoroutineScope()
    // Initial hydration: real file read off the main thread; the surface appears only if it is slow.
    var hydrateAttempt by remember { mutableStateOf(0) }
    LaunchedEffect(hydrateAttempt) {
        gate.run("Preparing Sadhana") { withContext(hydrateDispatcher) { controllerFactory(context) } }.onSuccess { controller = it }
    }
    Box(Modifier.fillMaxSize().background(Sad.Void)) {
        controller?.let { c ->
            SadhanaContent(c, reduceMotion, onRoutine) { lane ->
                scope.launch { gate.run("Opening source context") { sourcePort(lane) }.onSuccess { c.inspectSource(it) } }
            }
        }
        SadhanaLoadingSurface(gate.state, reduceMotion,
            onRetry = { if (controller == null) hydrateAttempt++ else scope.launch { gate.retry() } },
            onDismiss = { gate.dismiss(); if (controller == null) hydrateAttempt++ })
    }
}

@Composable private fun SadhanaContent(c: SadhanaController, reduceMotion: Boolean, onRoutine: (() -> Unit)?, onInspect: (String) -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) c.background() }
        owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val v = c.ui.view
    val scroll = rememberScrollState()
    LaunchedEffect(v) { scroll.scrollTo(0) }
    Column(Modifier.fillMaxSize().background(Sad.Void).systemBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 24.dp, vertical = 16.dp)) {
            when (v) {
                SadhanaView.HOME -> SadhanaHomeScreen(c, reduceMotion)
                SadhanaView.SPINE -> SpineScreen(c, reduceMotion)
                SadhanaView.PATH -> PathScreen(c)
                SadhanaView.PRACTICE -> PracticeDetailScreen(c, reduceMotion)
                SadhanaView.PREPARE -> PrepareScreen(c)
                SadhanaView.PLAYER -> PlayerScreen(c)
                SadhanaView.COMPLETE -> ReturnScreen(c, reduceMotion)
                SadhanaView.RECORD -> RecordScreen(c, c.today())
                SadhanaView.INDEPENDENT -> IndependentScreen(c, reduceMotion)
                SadhanaView.STUDY -> StudyScreen(c, onInspect)
            }
            c.ui.message?.let { Box(Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) { SNotice(it) } }
        }
        if (v != SadhanaView.PLAYER) { // End is the only exit from an active practice
            HorizontalDivider(color = Sad.Line)
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                val tabs = buildList {
                    if (onRoutine != null) add("◷ Routine" to null)
                    add("◌ Sadhana" to SadhanaView.HOME); add("⌁ Path" to SadhanaView.PATH); add("Ⅲ Record" to SadhanaView.RECORD); add("◇ Study" to SadhanaView.STUDY)
                }
                tabs.forEach { (label, target) ->
                    val active = target == v
                    TextButton(onClick = { if (target == null) onRoutine?.invoke() else c.go(target) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text(label, fontSize = 11.sp, color = if (active) Sad.Bronze else Sad.Tab, maxLines = 1)
                    }
                }
            }
        }
    }
}
