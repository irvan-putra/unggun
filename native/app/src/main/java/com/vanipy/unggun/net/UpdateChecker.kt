package com.vanipy.unggun.net

import android.content.Context
import com.vanipy.unggun.data.AppEntry
import com.vanipy.unggun.data.AppStore
import com.vanipy.unggun.data.LogStore
import com.vanipy.unggun.data.Prefs
import com.vanipy.unggun.data.TokenStore
import com.vanipy.unggun.logic.VersionCompare
import com.vanipy.unggun.util.Notifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** Manual update checks: up to 4 apps in parallel, one app's failure never blocks the others.
 *  Stops early when GitHub reports the quota is exhausted and persists the block. */
object UpdateChecker {

    @Volatile
    var running: Boolean = false
        private set

    data class Summary(val checked: Int, val updates: List<AppEntry>, val failed: Int, val rateLimitedUntil: Long)

    interface Progress {
        fun onProgress(done: Int, total: Int, current: String)
    }

    suspend fun checkAll(context: Context, onlyId: String? = null, progress: Progress? = null): Summary =
        withContext(Dispatchers.IO) {
            if (running) return@withContext Summary(0, emptyList(), 0, 0)
            running = true
            try {
                val token = TokenStore.read(context)
                val nowSec = System.currentTimeMillis() / 1000
                val blockedUntil = Prefs.rateLimitedUntil
                if (blockedUntil > nowSec) {
                    LogStore.log("check", "github limit hit, retry after ${RateLimit.formatHhMm(blockedUntil)}")
                    return@withContext Summary(0, emptyList(), 0, blockedUntil)
                }

                val targets = AppStore.all().filter { onlyId == null || it.id == onlyId }
                val updates = java.util.Collections.synchronizedList(ArrayList<AppEntry>())
                val failed = java.util.concurrent.atomic.AtomicInteger(0)
                val done = java.util.concurrent.atomic.AtomicInteger(0)
                val sem = Semaphore(4)

                coroutineScope {
                    val jobs = targets.map { app ->
                        launch {
                            sem.withPermit {
                                val blocked = Prefs.rateLimitedUntil
                                if (blocked > System.currentTimeMillis() / 1000) return@withPermit
                                progress?.onProgress(done.get(), targets.size, app.id)
                                try {
                                    val rel = GitHubApi.fetchLatestRelease(
                                        app.owner, app.repo, app.includePrerelease, token
                                    )
                                    val fresh = app.copy(
                                        latestVersion = rel.tag.removePrefix("v").ifBlank { rel.tag },
                                        latestUrl = rel.htmlUrl,
                                        releaseNotes = rel.notes.take(4000),
                                        lastCheckEpochSec = System.currentTimeMillis() / 1000,
                                        failureCount = 0,
                                        lastError = null,
                                    )
                                    // auto-refresh the actual installed version when we know the package id
                                    fresh.installedPackageId?.let { pkg ->
                                        try {
                                            val pi = context.packageManager.getPackageInfo(pkg, 0)
                                            fresh.installedVersion = pi.versionName
                                        } catch (_: Exception) {
                                        }
                                    }
                                    AppStore.update(fresh)
                                    if (VersionCompare.isNewer(fresh.latestVersion ?: "", fresh.installedVersion)) {
                                        updates.add(fresh)
                                    }
                                    LogStore.log("check", "${app.id}: latest ${fresh.latestVersion}, installed ${fresh.installedVersion ?: "?"}")
                                } catch (e: RateLimited) {
                                    Prefs.rateLimitedUntil = e.resetEpochSec
                                    LogStore.log("check", "github limit hit, retry after ${RateLimit.formatHhMm(e.resetEpochSec)}")
                                } catch (e: Exception) {
                                    failed.incrementAndGet()
                                    AppStore.update(
                                        app.copy(
                                            lastCheckEpochSec = System.currentTimeMillis() / 1000,
                                            failureCount = app.failureCount + 1,
                                            lastError = e.message ?: e.javaClass.simpleName,
                                        )
                                    )
                                    LogStore.log("check", "${app.id}: failed: ${e.message}")
                                }
                                done.incrementAndGet()
                            }
                        }
                    }
                    jobs.forEach { it.join() }
                }

                val blockedAfter = Prefs.rateLimitedUntil
                if (updates.isNotEmpty() && Prefs.notifyUpdates) {
                    Notifier.notifyUpdates(context, updates)
                }
                LogStore.log(
                    "check",
                    "done: ${done.get() - failed.get()} ok, ${updates.size} update(s), ${failed.get()} failed" +
                        if (blockedAfter > nowSec) ", limited until ${RateLimit.formatHhMm(blockedAfter)}" else ""
                )
                Summary(done.get() - failed.get(), updates.toList(), failed.get(), blockedAfter)
            } finally {
                running = false
            }
        }
}
