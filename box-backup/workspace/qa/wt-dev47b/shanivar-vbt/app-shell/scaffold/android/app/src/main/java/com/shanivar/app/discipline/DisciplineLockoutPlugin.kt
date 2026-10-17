package com.shanivar.app.discipline

import android.content.Intent
import android.provider.Settings
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

/**
 * REFERENCE SKETCH ONLY — see ../../../../../scaffold/README.md. This file
 * models a Capacitor-plugin call surface from an earlier hybrid-leaning
 * draft of the architecture decision; the reconciled decision
 * (architecture-decision.md) is fully-native, so Codex's real native
 * module will not literally be a CapacitorPlugin. Kept because the CALL
 * SURFACE SHAPE (getStatus / release / permission-request) is still a
 * useful reference for how a Compose UI ↔ discipline-engine-module
 * boundary should look, whatever the concrete mechanism.
 *
 * PHASE 1 SCAFFOLD — method bodies are structural stubs (TODO), not a
 * working lockout. Real blocking logic (UsageStatsManager polling +
 * SYSTEM_ALERT_WINDOW overlay, NOT AccessibilityService — see
 * shanivar-vbt/discipline-engine/platform-research.md) is the Discipline
 * Engine Agent's deliverable; this class defines a call surface, not the
 * implementation.
 */
@CapacitorPlugin(name = "DisciplineLockout")
class DisciplineLockoutPlugin : Plugin() {

    /**
     * Deep-links to Settings > Special app access > Usage access
     * (ACTION_USAGE_ACCESS_SETTINGS) so the user can grant PACKAGE_USAGE_STATS
     * — the UsageStatsManager-based mechanism platform-research.md
     * recommends in place of AccessibilityService. No runtime-permission
     * callback exists for this class of "special access" grant — the app
     * must re-check state on next onResume. See onboarding-flow.md §6.
     */
    @PluginMethod
    fun requestUsageAccess(call: PluginCall) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(intent)
        call.resolve()
    }

    /**
     * Returns current lockout state for the session-player's status pill
     * (session-player-spec.md §2.2). TODO(discipline-engine): wire to the
     * real AccessibilityService/UsageStatsManager state once that research
     * lands — currently returns a static stub shape only.
     */
    @PluginMethod
    fun getStatus(call: PluginCall) {
        val result = JSObject()
        result.put("enabled", false) // TODO: read real LockoutMonitorService running-state
        result.put("blockedPackage", "com.instagram.android") // v1 target per platform-research.md
        call.resolve(result)
    }

    /**
     * Called by the session player's hard-exit flow (session-player-spec.md
     * §2.3) to tear down the lock in the same action that closes the UI —
     * must not desync from the player closing.
     */
    @PluginMethod
    fun release(call: PluginCall) {
        // TODO(discipline-engine): signal LockoutAccessibilityService to
        // stop enforcing for the current session.
        call.resolve()
    }
}
