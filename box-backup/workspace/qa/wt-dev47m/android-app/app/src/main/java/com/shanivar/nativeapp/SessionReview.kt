package com.shanivar.nativeapp

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * MVP streamline task (package 5/7): one optional, skippable quick-answer review at the end of a
 * focus session. Deliberately fixed-choice, not free text - "no generated-data dependency" and
 * "no free-text disclosure" per the task spec, and this is a different, much smaller thing than
 * the deferred AI reflection scroller (`ReflectionScreen`), which it must not be confused with.
 *
 * Storage is local-only (package 7: "Defer broad analytics infrastructure" - this is a pilot-scale
 * self-report, not a reason to stand up a new backend event table). A bounded, append-only JSON
 * array in SharedPreferences is enough to review after a manual pilot without needing a server.
 */
private val QUICK_ANSWERS = listOf(
    "Staying with one app helped",
    "The timer helped",
    "It was mostly willpower",
    "Nothing in particular",
)
private const val MAX_STORED_ENTRIES = 50

@Composable
fun SessionReviewDialog(intentionLabel: String, onDone: () -> Unit) {
    val context = LocalContext.current
    fun record(answer: String?) {
        recordSessionReview(context, intentionLabel, answer)
        onDone()
    }
    AlertDialog(
        onDismissRequest = { record(null) },
        title = { Text("What helped you stay with your plan?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QUICK_ANSWERS.forEach { answer ->
                    TextButton(onClick = { record(answer) }) { Text(answer) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { record(null) }) { Text("Skip") } },
    )
}

private fun recordSessionReview(context: Context, intentionLabel: String, answer: String?) {
    val prefs = context.getSharedPreferences("session_reviews", Context.MODE_PRIVATE)
    val existing = runCatching { JSONArray(prefs.getString("entries", "[]")) }.getOrElse { JSONArray() }
    val entry = JSONObject()
        .put("at", Instant.now().toString())
        .put("intention", intentionLabel)
        .put("answer", answer) // JSONObject.put(key, null) stores JSON null, read back correctly via isNull()
    val next = JSONArray()
    val start = (existing.length() - MAX_STORED_ENTRIES + 1).coerceAtLeast(0)
    for (i in start until existing.length()) next.put(existing.get(i))
    next.put(entry)
    prefs.edit().putString("entries", next.toString()).apply()
}
