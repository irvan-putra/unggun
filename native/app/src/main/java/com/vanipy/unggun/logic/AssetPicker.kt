package com.vanipy.unggun.logic

/** Chooses one APK asset out of a release: optional name filter first, then ABI match
 *  in the device's preference order, then a universal (no-ABI) asset, then the first APK. */
object AssetPicker {

    private val ABI_TOKENS = linkedMapOf(
        "arm64-v8a" to listOf("arm64-v8a", "arm64"),
        "armeabi-v7a" to listOf("armeabi-v7a", "armeabi", "v7a"),
        "x86_64" to listOf("x86_64"),
        "x86" to listOf("x86"),
    )

    fun pick(assets: List<ReleaseAsset>, abis: List<String>, nameFilter: String = ""): ReleaseAsset? {
        var pool = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        val filter = nameFilter.trim()
        if (filter.isNotEmpty()) {
            pool = pool.filter { it.name.contains(filter, ignoreCase = true) }
        }
        if (pool.isEmpty()) return null

        for (abi in abis) {
            val tokens = ABI_TOKENS[abi] ?: listOf(abi)
            val match = pool.filter { a -> tokens.any { t -> matchesAbiToken(a.name, t) } }
            if (match.isNotEmpty()) return match.first()
        }

        val allTokens = ABI_TOKENS.values.flatten()
        val universal = pool.filter { a -> allTokens.none { t -> matchesAbiToken(a.name, t) } }
        return universal.firstOrNull() ?: pool.first()
    }

    /** "x86" must not match "x86_64": a token matches when the next char is not [a-z0-9]. */
    internal fun matchesAbiToken(name: String, token: String): Boolean {
        val n = name.lowercase()
        val t = token.lowercase()
        var idx = n.indexOf(t)
        while (idx >= 0) {
            val after = idx + t.length
            if (after >= n.length || (!n[after].isLetterOrDigit() && n[after] != '_')) return true
            idx = n.indexOf(t, idx + 1)
        }
        return false
    }
}
