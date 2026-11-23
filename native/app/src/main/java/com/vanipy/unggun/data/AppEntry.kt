package com.vanipy.unggun.data

import org.json.JSONObject

/** One tracked GitHub repo. Persisted as JSON; no compatibility contract with the Flutter app. */
data class AppEntry(
    val id: String, // "owner/repo"
    var displayName: String,
    var includePrerelease: Boolean = false,
    var trackOnly: Boolean = false,
    var nameFilter: String = "",
    var installedVersion: String? = null,
    var installedPackageId: String? = null,
    var latestVersion: String? = null,
    var latestUrl: String? = null,
    var releaseNotes: String? = null,
    var lastCheckEpochSec: Long = 0,
    var failureCount: Int = 0,
    var lastError: String? = null,
) {
    val owner: String get() = id.substringBefore('/')
    val repo: String get() = id.substringAfter('/')

    fun hasUpdate(): Boolean =
        latestVersion != null && com.vanipy.unggun.logic.VersionCompare.isNewer(latestVersion!!, installedVersion)

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("displayName", displayName)
        o.put("includePrerelease", includePrerelease)
        o.put("trackOnly", trackOnly)
        o.put("nameFilter", nameFilter)
        o.put("installedVersion", installedVersion)
        o.put("installedPackageId", installedPackageId)
        o.put("latestVersion", latestVersion)
        o.put("latestUrl", latestUrl)
        o.put("releaseNotes", releaseNotes)
        o.put("lastCheckEpochSec", lastCheckEpochSec)
        o.put("failureCount", failureCount)
        o.put("lastError", lastError)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): AppEntry = AppEntry(
            id = o.getString("id"),
            displayName = o.optString("displayName", o.getString("id")),
            includePrerelease = o.optBoolean("includePrerelease"),
            trackOnly = o.optBoolean("trackOnly"),
            nameFilter = o.optString("nameFilter"),
            installedVersion = o.optString("installedVersion").ifEmpty { null },
            installedPackageId = o.optString("installedPackageId").ifEmpty { null },
            latestVersion = o.optString("latestVersion").ifEmpty { null },
            latestUrl = o.optString("latestUrl").ifEmpty { null },
            releaseNotes = o.optString("releaseNotes").ifEmpty { null },
            lastCheckEpochSec = o.optLong("lastCheckEpochSec"),
            failureCount = o.optInt("failureCount"),
            lastError = o.optString("lastError").ifEmpty { null },
        )

        /** Accepts "owner/repo", github URLs with trailing slashes, .git or /releases suffixes. */
        fun parseRepoInput(raw: String): String? {
            var s = raw.trim()
            if (s.isEmpty()) return null
            s = s.removePrefix("https://").removePrefix("http://").removePrefix("github.com/")
            s = s.removePrefix("www.github.com/")
            s = s.removeSuffix(".git").removeSuffix("/")
            s = s.removeSuffix("/releases").removeSuffix("/")
            val parts = s.split('/').filter { it.isNotBlank() }
            if (parts.size != 2) return null
            val ok = Regex("^[A-Za-z0-9_.-]+$")
            if (!ok.matches(parts[0]) || !ok.matches(parts[1])) return null
            return "${parts[0]}/${parts[1]}"
        }
    }
}
