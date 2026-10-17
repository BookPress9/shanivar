package com.shanivar.app.alarm

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

/**
 * Bridges the WebView (session scheduling UI, see
 * ../../../../../onboarding-flow.md §6) to the native non-dismissible alarm.
 *
 * PHASE 1 SCAFFOLD — structural stub. Actual exact-alarm scheduling
 * semantics and the physical-verification dismissal mechanic (QR scan /
 * puzzle / movement, per ai_org/shanivar_vbt_track.md's locked decision)
 * are the Discipline Engine Agent's deliverable. This class defines the
 * call surface only.
 */
@CapacitorPlugin(name = "DisciplineAlarm")
class AlarmPlugin : Plugin() {

    /**
     * Deep-links to the exact-alarm permission settings screen (Android 12+
     * requires explicit user grant for SCHEDULE_EXACT_ALARM on most OEMs).
     * Also would request display-over-other-apps / notification permission
     * as needed — combined here for the single onboarding "Enable" action
     * described in onboarding-flow.md §6; may need to split into separate
     * calls once Discipline Engine Agent's findings land.
     */
    @PluginMethod
    fun requestExactAlarmAndOverlayAccess(call: PluginCall) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(intent)
        call.resolve()
    }

    /**
     * Schedules the non-dismissible alarm for a given session.
     * TODO(discipline-engine): wire to AlarmManager.setExactAndAllowWhileIdle
     * + AlarmReceiver, carrying the session_id so AlarmActivity can deep-link
     * straight into the session player on dismissal (session-player-spec.md §3).
     */
    @PluginMethod
    fun scheduleSessionAlarm(call: PluginCall) {
        val sessionId = call.getString("sessionId")
        val triggerAtMillis = call.getLong("triggerAtMillis")
        if (sessionId == null || triggerAtMillis == null) {
            call.reject("sessionId and triggerAtMillis are required")
            return
        }
        // TODO(discipline-engine): real AlarmManager scheduling.
        val result = JSObject()
        result.put("scheduled", false) // stub — not yet implemented
        call.resolve(result)
    }

    @PluginMethod
    fun cancelSessionAlarm(call: PluginCall) {
        // TODO(discipline-engine): cancel the matching pending intent.
        call.resolve()
    }
}
