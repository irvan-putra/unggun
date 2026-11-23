package com.vanipy.unggun.update_check_service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Foreground service that keeps an update-check batch alive (Mihon-style).
 *
 * The service owns notification id [NOTIFICATION_ID] on channel [CHANNEL_ID]
 * for as long as it runs; the Dart side routes progress updates through the
 * plugin channel instead of posting that id itself while promoted. On stop,
 * the notification is removed with the service.
 *
 * Keep-alive only: the actual checking still runs in the Dart isolate. The
 * service exists so Android treats the process as user-visible work and does
 * not kill it mid-check when the user leaves the app or the screen turns off.
 */
class UpdateCheckService : Service() {
    companion object {
        const val ACTION_START = "com.vanipy.unggun.action.UPDATE_CHECK_START"
        const val ACTION_UPDATE = "com.vanipy.unggun.action.UPDATE_CHECK_UPDATE"
        const val ACTION_STOP = "com.vanipy.unggun.action.UPDATE_CHECK_STOP"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_MAX = "max"

        // Must match CheckingUpdatesNotification in
        // lib/providers/notifications_provider.dart.
        const val NOTIFICATION_ID = 4
        const val CHANNEL_ID = "BG_UPDATE_CHECK"
    }

    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
                startForegroundWith(buildNotification(title, text, 0, 0))
                started = true
            }
            ACTION_UPDATE -> {
                if (started) {
                    val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                    val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
                    val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                    val max = intent.getIntExtra(EXTRA_MAX, 0)
                    val manager =
                        getSystemService(Context.NOTIFICATION_SERVICE)
                            as NotificationManager
                    manager.notify(
                        NOTIFICATION_ID,
                        buildNotification(title, text, progress, max),
                    )
                }
            }
            ACTION_STOP, null -> stopNow()
            else -> {}
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWith(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopNow() {
        started = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(
        title: String,
        text: String,
        progress: Int,
        max: Int,
    ): Notification {
        ensureChannel()
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        // ic_notification lives in the app module; resolve it at runtime
        // because a library cannot reference app resources at compile time.
        val iconRes = resources.getIdentifier(
            "ic_notification",
            "drawable",
            packageName,
        ).takeIf { it != 0 } ?: android.R.drawable.stat_sys_download
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(contentIntent)
        if (max > 0) {
            builder.setProgress(max, progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    /** The Dart side creates this channel on startup; this is a fallback. */
    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Update checking",
                NotificationManager.IMPORTANCE_MIN,
            ),
        )
    }
}
