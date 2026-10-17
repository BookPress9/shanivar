package com.shanivar.nativeapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/**
 * Fires when the `AlarmManager.setAlarmClock` trigger elapses. Builds the full-screen-intent
 * notification per platform-research.md's recommended architecture: "fires a Notification with
 * setFullScreenIntent targeting a dedicated full-screen 'session alarm' Activity."
 *
 * `setFullScreenIntent`'s ability to auto-launch over the lock screen depends on the app having
 * completed the Play Console core-functionality (alarm) declaration and `USE_FULL_SCREEN_INTENT`
 * not being auto-revoked (Android 14+ behavior) - see the manifest for the permission and its
 * comment. If that grant is ever missing, this still posts a heads-up notification the user can
 * tap manually, so the alarm isn't silently lost, just less automatic.
 */
class AlarmFireReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID).orEmpty()
        val alarmIntent = Intent(context, AlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            .putExtra(AlarmActivity.EXTRA_SESSION_ID, sessionId)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, REQUEST_CODE, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // minSdk is 26 (Android O), so notification channels always exist - no version check needed.
        val channel = NotificationChannel(CHANNEL_ID, "Wake-up alarm", NotificationManager.IMPORTANCE_HIGH)
        channel.description = "Shanivar's non-dismissible wake-up alarm."
        channel.setBypassDnd(true)
        manager.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Time to wake up")
            .setContentText("Walk to your chosen place to dismiss.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
        // Also start the alarm activity directly as a redundant path: some OEMs suppress
        // full-screen intents when the screen is already on, in which case only the heads-up
        // notification would show without this.
        context.startActivity(alarmIntent)
    }

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
        private const val CHANNEL_ID = "alarm_fire"
        private const val NOTIFICATION_ID = 4202
        private const val REQUEST_CODE = 4203
    }
}
