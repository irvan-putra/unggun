package com.vanipy.unggun.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.vanipy.unggun.data.AppStore
import com.vanipy.unggun.data.LogStore
import com.vanipy.unggun.util.Notifier

class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STATUS) return
        val appId = intent.getStringExtra(EXTRA_APP_ID) ?: return
        val pkg = intent.getStringExtra(EXTRA_PKG)
        val app = AppStore.get(appId) ?: return
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val fill = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                fill?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fill)
                LogStore.log("install", "$appId: stock installer asking for one tap")
            }
            PackageInstaller.STATUS_SUCCESS -> {
                val updated = app.copy(
                    installedPackageId = pkg,
                    installedVersion = app.latestVersion ?: app.installedVersion,
                )
                try {
                    val pi = context.packageManager.getPackageInfo(pkg!!, 0)
                    updated.installedVersion = pi.versionName
                } catch (_: Exception) {
                }
                AppStore.update(updated)
                LogStore.log("install", "$appId: installed ${updated.installedVersion}")
                Notifier.installResult(context, updated, true, "version ${updated.installedVersion ?: "?"}")
            }
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "status $status"
                LogStore.log("install", "$appId: failed: $msg")
                Notifier.installResult(context, app, false, msg)
            }
        }
    }

    companion object {
        const val ACTION_STATUS = "com.vanipy.unggun.INSTALL_STATUS"
        const val EXTRA_APP_ID = "app_id"
        const val EXTRA_PKG = "pkg"
        const val EXTRA_SILENT = "silent"
    }
}
