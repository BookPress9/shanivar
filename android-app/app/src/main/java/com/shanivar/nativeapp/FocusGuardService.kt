package com.shanivar.nativeapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that polls `UsageStatsManager` for the current foreground app while a focus
 * session is active, and launches `BlockOverlayActivity` when it sees a blocked package - the
 * architecture platform-research.md recommends (UsageStatsManager detects, SYSTEM_ALERT_WINDOW-
 * permission-backed overlay blocks) instead of `AccessibilityService`.
 *
 * Poll interval: the research doc calls out "typically 1-2s" detection latency as an accepted
 * limitation of this approach (a brief flash of the blocked app before the overlay appears is
 * expected UX, not a bug). 1500ms is chosen as a middle point in that documented range - frequent
 * enough that the flash stays brief, infrequent enough not to be a meaningful battery drain for a
 * short focus-session window.
 */
class FocusGuardService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null
    private var lastOverlayLaunchAtMillis = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val saved = getSharedPreferences("focus_guard", MODE_PRIVATE)
        val packages = intent?.getStringArrayListExtra(EXTRA_PACKAGES)?.toSet()
            ?: saved.getStringSet("packages", emptySet()).orEmpty()
        val endsAt = intent?.getLongExtra(EXTRA_ENDS_AT, 0L) ?: saved.getLong("endsAt", 0L)
        if (endsAt <= System.currentTimeMillis() || packages.isEmpty()) {
            saved.edit().clear().apply()
            stopSelf()
            return START_NOT_STICKY
        }
        saved.edit().putStringSet("packages", packages).putLong("endsAt", endsAt).apply()
        startForeground(NOTIFICATION_ID, buildNotification())
        pollJob?.cancel()
        pollJob = scope.launch { pollLoop(packages, endsAt) }
        return START_STICKY
    }

    override fun onDestroy() {
        pollJob?.cancel()
        super.onDestroy()
    }

    private suspend fun pollLoop(blockedPackages: Set<String>, endsAtEpochMillis: Long) {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        while (scope.isActive) {
            val now = System.currentTimeMillis()
            if (endsAtEpochMillis in 1 until now) {
                stopSelf()
                return
            }
            val foreground = currentForegroundPackage(usageStatsManager)
            if (ForegroundAppBlocker.shouldBlock(foreground, blockedPackages, endsAtEpochMillis, now)) {
                presentBlockOverlay(foreground!!, endsAtEpochMillis)
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    // UsageEvents (not queryUsageStats' aggregate buckets) is the recommended way to answer "what
    // is in the foreground right now" - a short lookback window is enough since we poll frequently.
    private fun currentForegroundPackage(usageStatsManager: UsageStatsManager): String? {
        val end = System.currentTimeMillis()
        val begin = end - LOOKBACK_WINDOW_MS
        val events = usageStatsManager.queryEvents(begin, end)
        val event = UsageEvents.Event()
        var lastForeground: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) lastForeground = event.packageName
            if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND && event.packageName == lastForeground) lastForeground = null
        }
        return lastForeground
    }

    private fun presentBlockOverlay(blockedPackage: String, endsAtEpochMillis: Long) {
        val now = System.currentTimeMillis()
        // Debounce: the polling loop would otherwise try to relaunch the overlay Activity every
        // POLL_INTERVAL_MS while the user is still sitting in the blocked app.
        if (now - lastOverlayLaunchAtMillis < OVERLAY_RELAUNCH_COOLDOWN_MS) return
        lastOverlayLaunchAtMillis = now
        val intent = Intent(this, BlockOverlayActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(BlockOverlayActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
            .putExtra(BlockOverlayActivity.EXTRA_ENDS_AT, endsAtEpochMillis)
        startActivity(intent)
    }

    private fun buildNotification(): android.app.Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // minSdk is 26 (Android O), so notification channels always exist - no version check needed.
        val channel = NotificationChannel(CHANNEL_ID, "Focus protection", NotificationManager.IMPORTANCE_LOW)
        channel.description = "Shows while Shanivar is protecting your focus window."
        manager.createNotificationChannel(channel)
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus protection is on")
            .setContentText("Shanivar is watching for blocked apps during your session.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }

    companion object {
        const val EXTRA_PACKAGES = "packages"
        const val EXTRA_ENDS_AT = "ends_at"
        private const val CHANNEL_ID = "focus_guard"
        private const val NOTIFICATION_ID = 4101
        private const val POLL_INTERVAL_MS = 1500L
        private const val LOOKBACK_WINDOW_MS = 10_000L
        private const val OVERLAY_RELAUNCH_COOLDOWN_MS = 2_000L
    }
}
