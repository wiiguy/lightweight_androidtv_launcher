package com.tvlauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIdentifierTest {

    @Test
    fun encode_app_uses_package_only() {
        assertEquals("com.example.app", AppIdentifier.encode("com.example.app", null))
    }

    @Test
    fun encode_shortcut_uses_unit_separator() {
        val id = AppIdentifier.encode("com.example.app", "shortcut-1")
        assertTrue(id.contains('\u001F'))
        assertFalse(id.contains(':'))
    }

    @Test
    fun decode_legacy_colon_format() {
        val decoded = AppIdentifier.decode("com.example.app:legacy-id")
        assertEquals("com.example.app", decoded.packageName)
        assertEquals("legacy-id", decoded.shortcutId)
    }

    @Test
    fun normalize_migrates_legacy_to_separator() {
        val normalized = AppIdentifier.normalize("com.foo:bar")
        assertEquals(AppIdentifier.encode("com.foo", "bar"), normalized)
    }

    @Test
    fun isShortcut_detects_shortcut_entries() {
        assertTrue(AppIdentifier.isShortcut("com.foo:bar"))
        assertTrue(AppIdentifier.isShortcut(AppIdentifier.encode("com.foo", "bar")))
        assertFalse(AppIdentifier.isShortcut("com.foo"))
    }
}
