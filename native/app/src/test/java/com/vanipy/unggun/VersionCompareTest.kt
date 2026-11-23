package com.vanipy.unggun

import com.vanipy.unggun.logic.VersionCompare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {
    @Test fun stripsV() = assertEquals(listOf(0, 5, 1), VersionCompare.parts("v0.5.1"))
    @Test fun stopsAtSuffix() = assertEquals(listOf(1, 0, 0), VersionCompare.parts("1.0.0-rc1"))
    @Test fun numericNotLexical() = assertTrue(VersionCompare.compare("1.2.10", "1.2.9") > 0)
    @Test fun shorterPadsZero() = assertTrue(VersionCompare.compare("1.2", "1.2.0") == 0)
    @Test fun newerDetection() {
        assertTrue(VersionCompare.isNewer("0.5.1", "0.5.0"))
        assertFalse(VersionCompare.isNewer("0.5.0", "0.5.0"))
        assertFalse(VersionCompare.isNewer("0.4.9", "0.5.0"))
        assertTrue(VersionCompare.isNewer("0.5.0", null))
        assertTrue(VersionCompare.isNewer("0.5.0", ""))
    }
}
