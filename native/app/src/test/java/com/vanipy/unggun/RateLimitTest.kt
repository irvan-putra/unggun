package com.vanipy.unggun

import com.vanipy.unggun.net.RateLimit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RateLimitTest {

    private val now = 1_700_000_000L

    @Test fun healthy200Passes() {
        assertNull(RateLimit.check(200, mapOf("x-ratelimit-remaining" to "0", "x-ratelimit-reset" to "1700003600"), now))
    }

    @Test fun exhausted403BlocksUntilReset() {
        val rl = RateLimit.check(403, mapOf("X-RateLimit-Remaining" to "0", "X-RateLimit-Reset" to "1700003600"), now)
        assertEquals(1700003600L, rl!!.resetEpochSec)
    }

    @Test fun retryAfterWins() {
        val rl = RateLimit.check(429, mapOf("Retry-After" to "120"), now)
        assertEquals(now + 120, rl!!.resetEpochSec)
    }

    @Test fun otherErrorsDoNotBlock() {
        assertNull(RateLimit.check(404, emptyMap(), now))
        assertNull(RateLimit.check(403, mapOf("x-ratelimit-remaining" to "5"), now))
        assertNull(RateLimit.check(500, emptyMap(), now))
    }
}
