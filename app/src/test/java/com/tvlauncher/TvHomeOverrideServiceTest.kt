package com.tvlauncher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvHomeOverrideServiceTest {

    @Test
    fun isWhitelistedPackage_matchesSettingsPackages() {
        assertTrue(TvHomeOverrideService.isWhitelistedPackage("com.android.settings"))
        assertTrue(TvHomeOverrideService.isWhitelistedPackage("com.android.tv.settings"))
        assertTrue(TvHomeOverrideService.isWhitelistedPackage("com.google.android.tv.settings"))
        assertTrue(TvHomeOverrideService.isWhitelistedPackage("com.amazon.tv.settings"))
        assertTrue(TvHomeOverrideService.isWhitelistedPackage("com.tcl.settings"))
        assertTrue(TvHomeOverrideService.isWhitelistedPackage("com.xiaomi.mitv.settings"))
        assertTrue(TvHomeOverrideService.isWhitelistedPackage("com.somebrand.settings"))
    }

    @Test
    fun isWhitelistedPackage_doesNotMatchLaunchers() {
        assertFalse(TvHomeOverrideService.isWhitelistedPackage("com.google.android.tvlauncher"))
        assertFalse(TvHomeOverrideService.isWhitelistedPackage("com.google.android.apps.tv.launcherx"))
        assertFalse(TvHomeOverrideService.isWhitelistedPackage("com.amazon.tv.launcher"))
        assertFalse(TvHomeOverrideService.isWhitelistedPackage("com.mitv.tvhome"))
    }
}
