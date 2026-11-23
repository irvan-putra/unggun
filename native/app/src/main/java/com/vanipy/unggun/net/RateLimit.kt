package com.vanipy.unggun.net

/** Thrown when GitHub says the quota is exhausted. resetEpochSec is when requests may resume. */
class RateLimited(val resetEpochSec: Long, detail: String) : Exception(detail)

/** Pure header inspection, unit-testable without a network. */
object RateLimit {

    /**
     * Decide whether a response means "stop until a reset".
     * status: HTTP status; headers: case-insensitive names mapped to a single value.
     */
    fun check(status: Int, headers: Map<String, String>, nowEpochSec: Long): RateLimited? {
        val h = HashMap<String, String>()
        for ((k, v) in headers) h[k.lowercase()] = v

        val retryAfter = h["retry-after"]?.toLongOrNull()
        if (retryAfter != null && (status == 403 || status == 429)) {
            return RateLimited(nowEpochSec + retryAfter, "retry-after $retryAfter s")
        }
        val remaining = h["x-ratelimit-remaining"]?.toLongOrNull()
        val reset = h["x-ratelimit-reset"]?.toLongOrNull()
        if ((status == 403 || status == 429) && remaining == 0L && reset != null) {
            return RateLimited(reset, "rate limit exhausted, resets at $reset")
        }
        return null
    }

    fun formatHhMm(epochSec: Long): String {
        val d = java.util.Date(epochSec * 1000)
        val f = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
        return f.format(d)
    }
}
