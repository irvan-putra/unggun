package com.vanipy.unggun.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Append-only operational log, kept to the newest 500 lines. This is how failures are diagnosed. */
object LogStore {

    private const val MAX_LINES = 500
    private lateinit var file: File
    private var ready = false
    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)

    @Synchronized
    fun init(context: Context) {
        if (ready) return
        file = File(context.filesDir, "logs.txt")
        ready = true
    }

    @Synchronized
    fun log(tag: String, message: String) {
        if (!ready) return
        val line = "${fmt.format(Date())} [$tag] $message"
        try {
            file.appendText(line + "\n")
            trimIfNeeded()
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun lines(): List<String> {
        if (!ready || !file.exists()) return emptyList()
        return file.readLines()
    }

    @Synchronized
    fun clear() {
        if (ready) file.delete()
    }

    private fun trimIfNeeded() {
        val lines = file.readLines()
        if (lines.size > MAX_LINES) {
            file.writeText(lines.takeLast(MAX_LINES).joinToString("\n") + "\n")
        }
    }
}
