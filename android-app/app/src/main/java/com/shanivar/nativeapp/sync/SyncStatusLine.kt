package com.shanivar.nativeapp.sync

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shanivar.nativeapp.BuildConfig
import com.shanivar.nativeapp.Sad
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** DEV-47 AC4: exactly three strings. No counts, streaks, scores or prompts. */
object SyncStatusText {
    const val SAVED = "Saved on this device"
    const val OFFLINE = "Will sync when online"
    fun of(s: SyncUiState, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String = when (s) {
        SyncUiState.LocalOnly -> SAVED
        SyncUiState.WaitingForNetwork -> OFFLINE
        is SyncUiState.Synced -> "Synced " + time(s.at, now, zone)
    }
    private fun time(at: Instant, now: Instant, zone: ZoneId): String {
        val t = at.atZone(zone); val clock = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(t)
        return if (t.toLocalDate() == now.atZone(zone).toLocalDate()) clock
        else DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(t) + " " + clock
    }
}

/** Quiet status line for the Sadhana Record view. Renders nothing when sync is off. Debug builds: long-press opens diagnostics. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SyncStatusLine(sync: AppSync, debug: Boolean = BuildConfig.DEBUG) {
    val source = sync.status ?: return
    val state by source.state.collectAsState()
    var diagnostics by remember { mutableStateOf(false) }
    val tap = if (debug) Modifier.combinedClickable(onClick = {}, onLongClick = { diagnostics = true }) else Modifier
    Text(SyncStatusText.of(state), fontSize = 11.sp, color = Sad.Fine, modifier = tap.testTag(STATUS_TAG).padding(vertical = 8.dp))
    if (debug && diagnostics) SyncDiagnostics(sync, source) { diagnostics = false }
}

/** Debug-only diagnostics (AC5). The only place the dead-letter count appears. */
@Composable
private fun SyncDiagnostics(sync: AppSync, source: SyncStatusSource, onClose: () -> Unit) {
    val dead by source.deadLetterCount.collectAsState()
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } },
        title = { Text("Sync diagnostics (debug)") },
        text = {
            val lost = if (sync.sessionLost == true) " (session lost)" else ""
            Text("Session: ${if (sync.hasSession) "yes" else "none"}$lost\nDead-letter items: $dead", modifier = Modifier.testTag(DIAG_TAG))
        },
    )
}

const val STATUS_TAG = "sync-status"
const val DIAG_TAG = "sync-diagnostics"
