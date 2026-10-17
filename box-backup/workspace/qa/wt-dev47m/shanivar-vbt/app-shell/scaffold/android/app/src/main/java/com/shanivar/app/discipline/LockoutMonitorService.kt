package com.shanivar.app.discipline

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * REFERENCE SKETCH ONLY — see ../../../../../../scaffold/README.md.
 *
 * Replaces an earlier AccessibilityService-based draft of this file.
 * Reconciled against shanivar-vbt/discipline-engine/platform-research.md:
 * do NOT use AccessibilityService for app-lockout — Play Store policy
 * (tightened Jan 28 2026, further restricted under Advanced Protection
 * Mode Mar 2026) treats autonomous accessibility-API use by a "convenience"
 * app like this as a rejection/suspension risk, since app-blocking is not
 * a genuine accessibility-support purpose.
 *
 * Recommended mechanism instead: UsageStatsManager foreground-app polling
 * (Settings-granted via PACKAGE_USAGE_STATS, no runtime dialog, the same
 * policy-accepted mechanism AppBlock/Block ship today) + a
 * SYSTEM_ALERT_WINDOW-class overlay (BlockOverlayActivity) to actually
 * present the block screen once a match is detected.
 *
 * PHASE 1 SCAFFOLD — structural stub only. Real polling interval, kill-
 * resistance strategy (OEM battery-optimization allowlisting UX), and the
 * v1 blocked-package target (com.instagram.android, per platform-research.md)
 * are Discipline Engine Agent deliverables.
 */
class LockoutMonitorService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO(discipline-engine): start as a real foreground service with a
        // persistent notification (required for foregroundServiceType
        // "specialUse"), then poll UsageStatsManager.queryUsageStats /
        // queryEvents at a short interval for the active session window,
        // matching against the blocked package list (Instagram only, v1).
        // On a match, launch BlockOverlayActivity.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
