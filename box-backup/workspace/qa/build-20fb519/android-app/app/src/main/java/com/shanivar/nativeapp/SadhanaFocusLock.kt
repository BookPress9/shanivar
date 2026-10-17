package com.shanivar.nativeapp

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager

/**
 * Default focus lock during an active Sadhana practice (founder, 13 Oct 2026; decision 0017, PRD).
 * - Exists only while a practice is running; End releases it immediately (local, no network).
 * - Blocks every other app via the existing protection engine (usage access + overlay), except
 *   essential ones: Shanivar, the phone/dialer and in-call screens, emergency apps and the home screen.
 * - Silences notification pop-ups with Do Not Disturb (priority) only if the user granted that access;
 *   the previous filter is restored on release. Calls follow the user's DND settings; emergency calls always work.
 * - Missing permissions are reported, never claimed as protected. Users can turn the default off.
 */
data class FocusLockStatus(val appsBlocked: Boolean, val popupsSilenced: Boolean) {
    val line: String get() = when {
        appsBlocked && popupsSilenced -> "Other apps are blocked and pop-ups are silenced until you end. Calls and emergency stay reachable."
        appsBlocked -> "Other apps are blocked until you end. Pop-ups are not silenced: Do Not Disturb access is not granted. Calls and emergency stay reachable."
        popupsSilenced -> "Pop-ups are silenced until you end. Other apps are not blocked: Usage access or overlay permission is missing."
        else -> "Focus lock is off: the permissions it needs are not granted. Nothing is blocked."
    }
}

interface SadhanaFocusLockPort {
    fun engage(endsAtEpochMillis: Long): FocusLockStatus
    fun release()
}

object FocusLockRules {
    /** Sentinel understood by [ForegroundAppBlocker]: block everything not explicitly allowed. */
    const val ALL = "*all*"
    const val ALLOW = "allow:"
    val ESSENTIAL = setOf(
        "com.android.dialer", "com.google.android.dialer", "com.samsung.android.dialer", "com.samsung.android.incallui",
        "com.android.incallui", "com.android.server.telecom", "com.android.phone", "com.android.emergency",
        "com.google.android.apps.safetyhub", "com.android.systemui", "com.android.settings", "android",
    )
    fun packages(own: String, extra: Set<String>): Set<String> = setOf(ALL) + (ESSENTIAL + extra + own).map { ALLOW + it }
}

class AndroidSadhanaFocusLock(context: Context) : SadhanaFocusLockPort {
    private val app = context.applicationContext
    private val local = LocalProtectionController.create(app)
    private val nm = app.getSystemService(NotificationManager::class.java)
    private val prefs = app.getSharedPreferences("sadhana_focus_lock", Context.MODE_PRIVATE)

    override fun engage(endsAtEpochMillis: Long): FocusLockStatus {
        val extra = buildSet {
            runCatching { app.getSystemService(TelecomManager::class.java)?.defaultDialerPackage }.getOrNull()?.let(::add)
            runCatching { app.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName }.getOrNull()?.let(::add)
        }
        val apps = local.active() == null && local.start(FocusLockRules.packages(app.packageName, extra), endsAtEpochMillis)
        prefs.edit().putBoolean("ownsGuard", apps).apply()
        var silenced = false
        if (nm != null && nm.isNotificationPolicyAccessGranted) {
            prefs.edit().putInt("prevFilter", nm.currentInterruptionFilter).apply()
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY); silenced = true
        }
        return FocusLockStatus(apps, silenced)
    }

    override fun release() {
        if (prefs.getBoolean("ownsGuard", false)) local.stopNow()
        val prev = prefs.getInt("prevFilter", -1)
        if (prev > 0 && nm != null && nm.isNotificationPolicyAccessGranted) runCatching { nm.setInterruptionFilter(prev) }
        prefs.edit().clear().apply()
    }
}
