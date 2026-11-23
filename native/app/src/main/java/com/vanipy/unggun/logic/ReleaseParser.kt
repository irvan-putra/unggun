package com.vanipy.unggun.logic

import org.json.JSONArray
import org.json.JSONObject

data class ReleaseAsset(
    val name: String,
    val size: Long,
    val downloadUrl: String,
)

data class Release(
    val tag: String,
    val name: String,
    val prerelease: Boolean,
    val publishedAt: String,
    val notes: String,
    val htmlUrl: String,
    val assets: List<ReleaseAsset>,
)

/** Parses GitHub releases API responses. Pure JVM: pinned by unit tests with recorded fixtures. */
object ReleaseParser {

    fun parseArray(body: String): List<Release> {
        val arr = JSONArray(body)
        val out = ArrayList<Release>(arr.length())
        for (i in 0 until arr.length()) out.add(parseOne(arr.getJSONObject(i)))
        return out
    }

    fun parseObject(body: String): Release = parseOne(JSONObject(body))

    private fun parseOne(o: JSONObject): Release {
        val assetsJson = o.optJSONArray("assets") ?: JSONArray()
        val assets = ArrayList<ReleaseAsset>(assetsJson.length())
        for (i in 0 until assetsJson.length()) {
            val a = assetsJson.getJSONObject(i)
            assets.add(
                ReleaseAsset(
                    name = a.optString("name"),
                    size = a.optLong("size"),
                    downloadUrl = a.optString("browser_download_url"),
                )
            )
        }
        return Release(
            tag = o.optString("tag_name"),
            name = o.optString("name"),
            prerelease = o.optBoolean("prerelease"),
            publishedAt = o.optString("published_at"),
            notes = o.optString("body"),
            htmlUrl = o.optString("html_url"),
            assets = assets,
        )
    }

    /** Newest-first as GitHub returns the list; drafts are excluded by the API itself. */
    fun pickLatest(releases: List<Release>, includePrerelease: Boolean): Release? {
        val pool = if (includePrerelease) releases else releases.filter { !it.prerelease }
        return pool.maxByOrNull { it.publishedAt }
    }
}
