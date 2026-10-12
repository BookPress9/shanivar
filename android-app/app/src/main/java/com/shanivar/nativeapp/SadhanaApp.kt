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
import java.time.ZoneId

object SystemSadhanaClock : SadhanaClock {
    override fun wall() = System.currentTimeMillis()
    override fun mono() = SystemClock.elapsedRealtime()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}

/** Sadhana V1 host (decision 0017). Debug builds only via BuildConfig.SADHANA_V1. */
@Composable fun SadhanaApp(
    controllerFactory: (Context) -> SadhanaController = { ctx ->
        SadhanaController(FileDraftStore(File(ctx.filesDir, "sadhana/state.properties")), SystemSadhanaClock, allowUnreviewed = BuildConfig.DEBUG)
    },
    reduceMotionOverride: Boolean? = null,
    onRoutine: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val c = remember { controllerFactory(context) }
    val reduceMotion = reduceMotionOverride ?: remember { initiationReduceMotion(context) }
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
                SadhanaView.HOME -> SadhanaHomeScreen(c)
                SadhanaView.SPINE -> SpineScreen(c)
                SadhanaView.PATH -> PathScreen(c)
                SadhanaView.PRACTICE -> PracticeDetailScreen(c, reduceMotion)
                SadhanaView.PREPARE -> PrepareScreen(c)
                SadhanaView.PLAYER -> PlayerScreen(c)
                SadhanaView.COMPLETE -> ReturnScreen(c, reduceMotion)
                SadhanaView.RECORD -> RecordScreen(c, c.today())
                SadhanaView.INDEPENDENT -> IndependentScreen(c, reduceMotion)
                SadhanaView.STUDY -> StudyScreen(c)
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
