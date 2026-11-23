package com.vanipy.unggun

import com.vanipy.unggun.logic.AssetPicker
import com.vanipy.unggun.logic.ReleaseAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssetPickerTest {

    private fun a(name: String) = ReleaseAsset(name, 1000, "https://example.com/$name")

    private val arm64 = listOf("arm64-v8a", "armeabi-v7a")
    private val x86 = listOf("x86_64")

    @Test fun ignoresNonApk() {
        val pick = AssetPicker.pick(listOf(a("app.apk.sha256"), a("SHA256SUMS")), arm64)
        assertNull(pick)
    }

    @Test fun singleApk() {
        assertEquals("tengok-v0.5.0.apk",
            AssetPicker.pick(listOf(a("tengok-v0.5.0.apk"), a("tengok-v0.5.0.apk.sha256")), arm64)!!.name)
    }

    @Test fun prefersDeviceAbi() {
        val assets = listOf(a("app-arm64-v8a-release.apk"), a("app-armeabi-v7a-release.apk"), a("app-x86_64-release.apk"))
        assertEquals("app-arm64-v8a-release.apk", AssetPicker.pick(assets, arm64)!!.name)
        assertEquals("app-x86_64-release.apk", AssetPicker.pick(assets, x86)!!.name)
    }

    @Test fun x86TokenDoesNotMatchX86_64() {
        val assets = listOf(a("app-x86_64.apk"))
        // a device offering plain x86 must not get the x86_64 build as its ABI match
        val pick = AssetPicker.pick(assets, listOf("x86"))
        // falls back to "universal or first" instead of claiming an ABI match; asset picker
        // still returns something usable, but the token check itself must be strict
        org.junit.Assert.assertFalse(AssetPicker.matchesAbiToken("app-x86_64.apk", "x86"))
        org.junit.Assert.assertTrue(AssetPicker.matchesAbiToken("app-x86.apk", "x86"))
        assertEquals("app-x86_64.apk", pick!!.name) // fallback still returns the only apk
    }

    @Test fun nameFilterApplies() {
        val assets = listOf(a("app-full.apk"), a("app-lite.apk"))
        assertEquals("app-lite.apk", AssetPicker.pick(assets, arm64, "lite")!!.name)
        assertNull(AssetPicker.pick(assets, arm64, "pro"))
    }

    @Test fun universalPreferredOverWrongAbi() {
        val assets = listOf(a("app-x86_64.apk"), a("app-universal.apk"))
        assertEquals("app-universal.apk", AssetPicker.pick(assets, arm64)!!.name)
    }
}
