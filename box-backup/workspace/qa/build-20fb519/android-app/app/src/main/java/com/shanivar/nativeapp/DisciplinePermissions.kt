package com.shanivar.nativeapp

import android.Manifest
import android.app.AlarmManager
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * All checks here are "read the actual granted-permission state," never a runtime dialog for the
 * Settings-only grants - per the task spec ("via Settings, not a runtime dialog") and
 * platform-research.md's recommended approach. `PACKAGE_USAGE_STATS` and `SYSTEM_ALERT_WINDOW` are
 * both special access grants a user makes once in system Settings; exact-alarm scheduling is a
 * toggle on API 33+ the user can revoke at any time. `hasNotificationPermission` is the one
 * runtime-dialog exception (`POST_NOTIFICATIONS`, requested via `ActivityResultContracts` at alarm
 * setup, per the MVP streamline task) - it is read here the same way so callers have one interface
 * to poll. NATIVE_HANDOFF.md requires re-reading these on every resume from Settings, so keep this
 * interface cheap/synchronous to call often.
 */
interface DisciplinePermissions {
    fun hasUsageAccess(): Boolean
    fun hasOverlayPermission(): Boolean
    fun hasExactAlarmPermission(): Boolean
    /**
     * On API 33+ (Tiramisu), `manager.notify()` for a denied-permission app is a silent no-op -
     * `AlarmFireReceiver`'s full-screen intent notification never posts, and `AlarmActivity` never
     * launches. Below API 33 the permission does not exist and notifications are granted
     * automatically, so this returns true unconditionally there.
     */
    fun hasNotificationPermission(): Boolean
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

    override fun hasNotificationPermission(): Boolean {
        if (!(context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
