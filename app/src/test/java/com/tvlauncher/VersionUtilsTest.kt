package com.tvlauncher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionUtilsTest {

    @Test
    fun isNewer_detects_patch() {
        assertTrue(VersionUtils.isNewer("1.5", "1.4"))
        assertFalse(VersionUtils.isNewer("1.4", "1.4"))
        assertFalse(VersionUtils.isNewer("1.3", "1.4"))
    }

    @Test
    fun isNewer_handles_v_prefix() {
        assertTrue(VersionUtils.isNewer("v1.5", "1.4"))
    }
}
