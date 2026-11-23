package com.vanipy.unggun.data

import android.content.Context
import org.json.JSONArray
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/** JSON-file persistence for tracked apps. Small by design; one file, written whole. */
object AppStore {

    private lateinit var file: File
    private val apps = ArrayList<AppEntry>()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private var loaded = false

    @Synchronized
    fun init(context: Context) {
        if (loaded) return
        file = File(context.filesDir, "apps.json")
        if (file.exists()) {
            try {
                val arr = JSONArray(file.readText())
                for (i in 0 until arr.length()) apps.add(AppEntry.fromJson(arr.getJSONObject(i)))
            } catch (e: Exception) {
                LogStore.log("store", "failed to read apps.json: ${e.message}; starting empty")
            }
        }
        loaded = true
    }

    fun addListener(l: () -> Unit) { listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }
    private fun notifyChanged() { listeners.forEach { it() } }

    @Synchronized
    fun all(): List<AppEntry> = apps.toList()

    @Synchronized
    fun get(id: String): AppEntry? = apps.firstOrNull { it.id == id }

    @Synchronized
    fun add(entry: AppEntry): Boolean {
        if (apps.any { it.id.equals(entry.id, ignoreCase = true) }) return false
        apps.add(entry)
        persist()
        notifyChanged()
        return true
    }

    @Synchronized
    fun update(entry: AppEntry) {
        val i = apps.indexOfFirst { it.id == entry.id }
        if (i >= 0) {
            apps[i] = entry
            persist()
            notifyChanged()
        }
    }

    @Synchronized
    fun remove(id: String) {
        if (apps.removeAll { it.id == id }) {
            persist()
            notifyChanged()
        }
    }

    @Synchronized
    fun export(): String = apps.joinToString("\n") { "https://github.com/${it.id}" }

    private fun persist() {
        val arr = JSONArray()
        apps.forEach { arr.put(it.toJson()) }
        val tmp = File(file.parentFile, "apps.json.tmp")
        tmp.writeText(arr.toString())
        if (!tmp.renameTo(file)) {
            file.writeText(arr.toString())
            tmp.delete()
        }
    }
}
