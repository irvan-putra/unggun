package com.vanipy.unggun

import com.vanipy.unggun.data.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppEntryParseTest {
    @Test fun ownerRepo() = assertEquals("irvan-putra/tengok", AppEntry.parseRepoInput("irvan-putra/tengok"))
    @Test fun fullUrl() = assertEquals("irvan-putra/tengok", AppEntry.parseRepoInput("https://github.com/irvan-putra/tengok"))
    @Test fun trailingSlash() = assertEquals("irvan-putra/tengok", AppEntry.parseRepoInput("https://github.com/irvan-putra/tengok/"))
    @Test fun gitSuffix() = assertEquals("irvan-putra/tengok", AppEntry.parseRepoInput("https://github.com/irvan-putra/tengok.git"))
    @Test fun releasesSuffix() = assertEquals("irvan-putra/tengok", AppEntry.parseRepoInput("https://github.com/irvan-putra/tengok/releases"))
    @Test fun rejectsJunk() {
        assertNull(AppEntry.parseRepoInput(""))
        assertNull(AppEntry.parseRepoInput("tengok"))
        assertNull(AppEntry.parseRepoInput("a/b/c"))
        assertNull(AppEntry.parseRepoInput("https://gitlab.com/a/b"))
    }
}
