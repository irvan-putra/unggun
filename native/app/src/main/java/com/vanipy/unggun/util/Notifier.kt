package com.vanipy.unggun.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vanipy.unggun.R
import com.vanipy.unggun.data.AppEntry

object Notifier {

    const val CH_UPDATES = "updates_available"
    const val CH_DOWNLOADS = "downloads"
    const val CH_INSTALL = "install_results"
    const val CH_ERRORS = "errors"

    private const val ID_UPDATES = 1001
    private const val ID_DOWNLOAD_BASE = 2000
    private const val ID_INSTALL_BASE = 3000

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_UPDATES, "updates available", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CH_DOWNLOADS, "downloads", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CH_INSTALL, "install results", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CH_ERRORS, "errors", NotificationManager.IMPORTANCE_DEFAULT),
            )
        )
    }

    fun notifyUpdates(context: Context, updates: List<AppEntry>) {
        val names = updates.joinToString(", ") { it.displayName }
        val text = if (updates.size == 1) "${updates[0].displayName} ${updates[0].latestVersion}" else names
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pi = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, CH_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${updates.size} update${if (updates.size == 1) "" else "s"} available")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        post(context, ID_UPDATES, n)
    }

    fun downloadProgress(context: Context, appId: String, fileName: String, progress: Int, max: Int) {
        val cancel = Intent(context, com.vanipy.unggun.net.DownloadCancelReceiver::class.java)
            .setAction(com.vanipy.unggun.net.DownloadCancelReceiver.ACTION_CANCEL)
            .putExtra(com.vanipy.unggun.net.DownloadCancelReceiver.EXTRA_APP_ID, appId)
        val cancelPi = PendingIntent.getBroadcast(
            context, appId.hashCode(), cancel, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val n = NotificationCompat.Builder(context, CH_DOWNLOADS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("downloading $fileName")
            .setProgress(max, progress, max <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "cancel", cancelPi)
            .build()
        post(context, ID_DOWNLOAD_BASE + (appId.hashCode() and 0x3FF), n)
    }

    fun downloadFinished(context: Context, appId: String) {
        NotificationManagerCompat.from(context).cancel(ID_DOWNLOAD_BASE + (appId.hashCode() and 0x3FF))
    }

    fun installResult(context: Context, app: AppEntry, ok: Boolean, detail: String) {
        val n = NotificationCompat.Builder(context, if (ok) CH_INSTALL else CH_ERRORS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (ok) "${app.displayName} installed" else "${app.displayName} install failed")
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setAutoCancel(true)
            .build()
        post(context, ID_INSTALL_BASE + (app.id.hashCode() and 0x3FF), n)
    }

    fun error(context: Context, title: String, text: String) {
        val n = NotificationCompat.Builder(context, CH_ERRORS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        post(context, ID_INSTALL_BASE + (title.hashCode() and 0x3FF), n)
    }

    private fun post(context: Context, id: Int, n: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted yet; logs still carry the event.
        }
    }
}
