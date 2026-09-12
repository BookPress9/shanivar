package com.shanivar.nativeapp

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Seam between `DisciplineEngine`'s state machine and the actual foreground service, so the
 * engine's start/release logic is unit-testable without booting a real Android service. */
interface FocusGuardController {
    fun start(packageNames: Set<String>, endsAtEpochMillis: Long)
    fun stop()
}

class AndroidFocusGuardController(private val context: Context) : FocusGuardController {
    override fun start(packageNames: Set<String>, endsAtEpochMillis: Long) {
        val intent = Intent(context, FocusGuardService::class.java)
            .putStringArrayListExtra(FocusGuardService.EXTRA_PACKAGES, ArrayList(packageNames))
            .putExtra(FocusGuardService.EXTRA_ENDS_AT, endsAtEpochMillis)
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stop() {
        context.stopService(Intent(context, FocusGuardService::class.java))
    }
}
