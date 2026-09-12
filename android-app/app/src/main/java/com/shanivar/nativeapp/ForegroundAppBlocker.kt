package com.shanivar.nativeapp

/** Pure decision logic pulled out of `FocusGuardService` so it's unit-testable without
 * `UsageStatsManager`/a real device: given what the polling loop currently knows, should the
 * block overlay be shown right now? */
object ForegroundAppBlocker {
    fun shouldBlock(foregroundPackage: String?, blockedPackages: Set<String>, sessionEndsAtEpochMillis: Long, nowEpochMillis: Long): Boolean {
        if (sessionEndsAtEpochMillis in 1 until nowEpochMillis) return false // session window has elapsed
        if (foregroundPackage.isNullOrBlank()) return false
        return foregroundPackage in blockedPackages
    }
}
