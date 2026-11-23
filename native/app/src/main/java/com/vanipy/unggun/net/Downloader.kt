package com.vanipy.unggun.net

import android.content.Context
import com.vanipy.unggun.data.AppEntry
import com.vanipy.unggun.data.LogStore
import com.vanipy.unggun.install.Installer
import com.vanipy.unggun.logic.ReleaseAsset
import com.vanipy.unggun.util.Notifier
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/** One background executor for downloads; a determinate notification per download with cancel. */
object Downloader {

    private val exec = Executors.newSingleThreadExecutor()
    private val cancelled = ConcurrentHashMap<String, Boolean>()

    fun cancel(appId: String) {
        cancelled[appId] = true
    }

    fun isCancelled(appId: String): Boolean = cancelled[appId] == true

    fun start(context: Context, app: AppEntry, asset: ReleaseAsset) {
        val appContext = context.applicationContext
        cancelled.remove(app.id)
        exec.execute {
            val dir = File(appContext.cacheDir, "apks").apply { mkdirs() }
            val out = File(dir, asset.name.replace(Regex("[^A-Za-z0-9._-]"), "_"))
            try {
                LogStore.log("download", "${app.id}: ${asset.name} (${asset.size / 1024} KB)")
                val conn = (URL(asset.downloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 60_000
                    setRequestProperty("User-Agent", "unggun-native/1.0")
                    setRequestProperty("Accept", "application/octet-stream")
                }
                val status = conn.responseCode
                if (status != 200) {
                    conn.disconnect()
                    throw java.io.IOException("download failed: http $status")
                }
                val total = conn.contentLength
                conn.inputStream.use { input ->
                    FileOutputStream(out).use { fos ->
                        val buf = ByteArray(64 * 1024)
                        var soFar = 0L
                        var lastNotified = -1
                        while (true) {
                            if (isCancelled(app.id)) throw Cancelled()
                            val n = input.read(buf)
                            if (n < 0) break
                            fos.write(buf, 0, n)
                            soFar += n
                            if (total > 0) {
                                val pct = (soFar * 100 / total).toInt()
                                if (pct != lastNotified && pct % 2 == 0) {
                                    lastNotified = pct
                                    Notifier.downloadProgress(appContext, app.id, asset.name, pct, 100)
                                }
                            } else {
                                Notifier.downloadProgress(appContext, app.id, asset.name, 0, 0)
                            }
                        }
                    }
                }
                conn.disconnect()
                Notifier.downloadFinished(appContext, app.id)
                LogStore.log("download", "${app.id}: saved ${out.name}, installing")
                Installer.install(appContext, out, app)
            } catch (c: Cancelled) {
                out.delete()
                Notifier.downloadFinished(appContext, app.id)
                LogStore.log("download", "${app.id}: canceled")
            } catch (e: Exception) {
                out.delete()
                Notifier.downloadFinished(appContext, app.id)
                LogStore.log("download", "${app.id}: failed: ${e.message}")
                if (com.vanipy.unggun.data.Prefs.notifyErrors) {
                    Notifier.error(appContext, "download failed", "${app.id}: ${e.message}")
                }
            }
        }
    }

    private class Cancelled : Exception()
}
