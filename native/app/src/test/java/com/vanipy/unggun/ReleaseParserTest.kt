package com.vanipy.unggun

import com.vanipy.unggun.logic.ReleaseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseParserTest {

    private fun fixture(name: String): String =
        javaClass.classLoader!!.getResource("fixtures/$name")!!.readText()

    @Test fun parsesTengokReleases() {
        val list = ReleaseParser.parseArray(fixture("tengok-releases.json"))
        assertTrue(list.size >= 2)
        val first = list[0]
        assertEquals("v0.5.0", first.tag)
        assertTrue(first.prerelease)
        assertTrue(first.assets.any { it.name == "tengok-v0.5.0.apk" })
        assertTrue(first.assets.all { it.downloadUrl.startsWith("https://") })
    }

    @Test fun picksNewestByPublishedAt() {
        val list = ReleaseParser.parseArray(fixture("kokok-releases.json"))
        val latest = ReleaseParser.pickLatest(list, includePrerelease = true)
        assertNotNull(latest)
        assertEquals("v0.2.2", latest!!.tag)
    }

    @Test fun skipsPrereleasesWhenAsked() {
        val list = ReleaseParser.parseArray(fixture("tengok-releases.json"))
        // all tengok releases are prereleases: nothing stable left
        assertNull(ReleaseParser.pickLatest(list, includePrerelease = false))
    }
}
