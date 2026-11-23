package com.vanipy.unggun.logic

/** Loose numeric version compare: strips a leading v/V, stops at the first
 *  non digit/dot character, compares component by component.
 *  "v0.5.1" > "0.5.0"; "1.0.0-rc1" == "1.0.0" (suffix ignored). */
object VersionCompare {

    fun parts(v: String): List<Int> {
        val cleaned = v.trim().removePrefix("v").removePrefix("V")
        val sb = StringBuilder()
        for (c in cleaned) {
            if (c.isDigit() || c == '.') sb.append(c) else break
        }
        return sb.toString().split('.').filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: 0 }
    }

    /** Positive when a is newer than b. */
    fun compare(a: String, b: String): Int {
        val pa = parts(a)
        val pb = parts(b)
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return if (x > y) 1 else -1
        }
        return 0
    }

    fun isNewer(candidate: String, current: String?): Boolean {
        if (current.isNullOrBlank()) return candidate.isNotBlank()
        return compare(candidate, current) > 0
    }
}
