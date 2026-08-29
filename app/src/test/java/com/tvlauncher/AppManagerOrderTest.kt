package com.tvlauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppManagerOrderTest {

    @Test
    fun app_slot_diff_callback_compares_correctly() {
        val slot1 = AppSlotAdapter.AppSlot(AppInfo("com.netflix.ninja", "Netflix"), false)
        val slot2 = AppSlotAdapter.AppSlot(AppInfo("com.netflix.ninja", "Netflix"), false)
        val slotEmpty = AppSlotAdapter.AppSlot(null, true)

        assertEquals(slot1, slot2)
        assertFalse(slot1 == slotEmpty)
    }
}
