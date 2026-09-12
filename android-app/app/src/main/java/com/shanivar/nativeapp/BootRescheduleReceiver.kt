package com.shanivar.nativeapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * `AlarmManager` entries do not survive a reboot, so a pending alarm scheduled before a restart
 * would silently vanish without this. We only restore a *future* trigger; a trigger time that
 * already passed while the device was off is left alone for v1 (documented as unverified/unhandled
 * in the delivery report - deciding whether a missed wake-up alarm should fire immediately on boot
 * or be dropped is a product decision, not an engineering default to guess at).
 */
class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val scheduler = AlarmScheduler(context)
        val triggerAt = scheduler.pendingTriggerAtEpochMillis() ?: return
        val sessionId = scheduler.pendingSessionId() ?: return
        if (triggerAt > System.currentTimeMillis()) scheduler.schedule(triggerAt, sessionId)
    }
}
