package com.vanipy.unggun.net

import com.vanipy.unggun.logic.Release
import com.vanipy.unggun.logic.ReleaseParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Minimal GitHub releases client. Reads rate-limit headers on every response and
 *  throws RateLimited so callers can stop early and persist the block. */
object GitHubApi {

    private const val UA = "unggun-native/1.0"

    class HttpResult(val status: Int, val headers: Map<String, String>, val body: String)

    fun get(url: String, token: String): HttpResult {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            val status = conn.responseCode
            val headers = HashMap<String, String>()
            for ((k, v) in conn.headerFields) {
                if (k != null && v.isNotEmpty()) headers[k.lowercase()] = v[0]
            }
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.readBytes()?.toString(Charsets.UTF_8) ?: ""
            return HttpResult(status, headers, body)
        } finally {
            conn.disconnect()
        }
    }

    /** Latest release for a repo, honoring the prerelease flag. Throws RateLimited. */
    fun fetchLatestRelease(owner: String, repo: String, includePrerelease: Boolean, token: String): Release {
        val now = System.currentTimeMillis() / 1000
        if (includePrerelease) {
            val r = get("https://api.github.com/repos/$owner/$repo/releases?per_page=10", token)
            RateLimit.check(r.status, r.headers, now)?.let { throw it }
            if (r.status != 200) throw IOException("github $r.status for $owner/$repo")
            val list = ReleaseParser.parseArray(r.body)
            return ReleaseParser.pickLatest(list, includePrerelease = true)
                ?: throw IOException("no releases for $owner/$repo")
        } else {
            val r = get("https://api.github.com/repos/$owner/$repo/releases/latest", token)
            RateLimit.check(r.status, r.headers, now)?.let { throw it }
            if (r.status == 404) throw IOException("no stable release for $owner/$repo")
            if (r.status != 200) throw IOException("github ${r.status} for $owner/$repo")
            return ReleaseParser.parseObject(r.body)
        }
    }
}
