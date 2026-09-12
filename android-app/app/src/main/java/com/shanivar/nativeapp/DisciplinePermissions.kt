package com.shanivar.nativeapp

import android.app.AlarmManager
import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * All three checks here are "read the actual granted-permission state," never a runtime dialog -
 * per the task spec ("via Settings, not a runtime dialog") and platform-research.md's recommended
 * approach. `PACKAGE_USAGE_STATS` and `SYSTEM_ALERT_WINDOW` are both special access grants a user
 * makes once in system Settings; exact-alarm scheduling is a toggle on API 33+ the user can revoke
 * at any time. NATIVE_HANDOFF.md requires re-reading these on every resume from Settings, so keep
 * this interface cheap/synchronous to call often.
 */
interface DisciplinePermissions {
    fun hasUsageAccess(): Boolean
    fun hasOverlayPermission(): Boolean
    fun hasExactAlarmPermission(): Boolean
}

class AndroidDisciplinePermissions(private val context: Context) : DisciplinePermissions {
    override fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

    override fun hasExactAlarmPermission(): Boolean {
        // Below API 31 SCHEDULE_EXACT_ALARM is a normal manifest permission (granted at install);
        // canScheduleExactAlarms() itself only exists from API 31 onward.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }
}
