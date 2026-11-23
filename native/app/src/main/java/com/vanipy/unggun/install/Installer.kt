package com.vanipy.unggun.install

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import com.vanipy.unggun.data.AppEntry
import com.vanipy.unggun.data.LogStore
import java.io.File

/** Stock PackageInstaller only. No root, no Shizuku.
 *  When unggun is already the installer of record for a package it asks Android to
 *  skip the confirm dialog (Android 12+); everything else gets the stock prompt. */
object Installer {

    fun install(context: Context, apk: File, app: AppEntry) {
        val pm = context.packageManager
        val installer = pm.packageInstaller
        val pkg = try {
            pm.getPackageArchiveInfo(apk.absolutePath, 0)?.packageName
        } catch (e: Exception) {
            null
        }
        if (pkg == null) {
            LogStore.log("install", "${app.id}: cannot read package id from ${apk.name}")
            return
        }

        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(pkg)
        var silentRequested = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val src = pm.getInstallSourceInfo(pkg)
                if (src.installingPackageName == context.packageName) {
                    params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                    silentRequested = true
                }
            } catch (_: PackageManager.NameNotFoundException) {
                // not installed yet: the stock prompt will show
            }
        }

        val cb = Intent(context, InstallResultReceiver::class.java)
            .setAction(InstallResultReceiver.ACTION_STATUS)
            .putExtra(InstallResultReceiver.EXTRA_APP_ID, app.id)
            .putExtra(InstallResultReceiver.EXTRA_PKG, pkg)
            .putExtra(InstallResultReceiver.EXTRA_SILENT, silentRequested)
            .putExtra(InstallResultReceiver.EXTRA_APK_PATH, apk.absolutePath)
        val sessionId = installer.createSession(params)
        val session = installer.openSession(sessionId)
        try {
            apk.inputStream().use { input ->
                session.openWrite("apk", 0, apk.length()).use { out ->
                    input.copyTo(out)
                    session.fsync(out)
                }
            }
            val pi = PendingIntent.getBroadcast(
                context, sessionId, cb, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            LogStore.log("install", "${app.id}: committing session for $pkg" + if (silentRequested) " (silent allowed)" else "")
            session.commit(pi.intentSender)
        } finally {
            session.close()
        }
    }
}
