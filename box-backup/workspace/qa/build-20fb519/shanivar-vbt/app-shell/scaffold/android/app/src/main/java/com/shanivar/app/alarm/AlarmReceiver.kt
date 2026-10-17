package com.shanivar.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * PHASE 1 SCAFFOLD — structural stub only.
 *
 * Fires when a scheduled session alarm goes off (see AlarmPlugin.
 * scheduleSessionAlarm). Its job is to launch AlarmActivity as a native,
 * non-WebView, full-screen experience — deliberately NOT routed through
 * MainActivity/the WebView, so the alarm survives even if the WebView
 * process has been reclaimed. See ../../../../../architecture-decision.md §3.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra("sessionId")
        // TODO(discipline-engine): post a full-screen-intent notification
        // (Android 10+ pattern) or directly launch AlarmActivity depending
        // on API level and lock-state, carrying sessionId through.
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("sessionId", sessionId)
        }
        context.startActivity(alarmIntent)
    }
}
