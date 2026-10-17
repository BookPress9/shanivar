package com.shanivar.nativeapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Keeps the process alive while `AlarmActivity` is on screen and undismissed, reinforcing the
 * full-screen intent per platform-research.md ("Activity runs a foreground service to stay alive").
 * This is a reinforcing layer, not a guarantee: per the research doc's flagged limitations, a user
 * can still force-stop the app from Settings, which kills this service and the alarm along with it -
 * that is a documented, accepted Android limitation, not a bug in this implementation.
 */
class AlarmForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): android.app.Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // minSdk is 26 (Android O), so notification channels always exist - no version check needed.
        val channel = NotificationChannel(CHANNEL_ID, "Alarm active", NotificationManager.IMPORTANCE_LOW)
        channel.description = "Shows while a Shanivar wake-up alarm is awaiting dismissal."
        manager.createNotificationChannel(channel)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Alarm awaiting dismissal")
            .setContentText("Get up and move to finish dismissing this alarm.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "alarm_keepalive"
        private const val NOTIFICATION_ID = 4204

        fun start(context: Context) = context.startForegroundService(Intent(context, AlarmForegroundService::class.java))
        fun stop(context: Context) = context.stopService(Intent(context, AlarmForegroundService::class.java))
    }
}
