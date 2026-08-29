package com.tvlauncher

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Lightweight Accessibility Service to intercept HOME and APP_SWITCH key presses
 * on TV firmware where the default OEM launcher cannot be replaced in standard settings.
 */
class TvHomeOverrideService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false

        val isOverrideEnabled = isHomeOverrideEnabled(this)
        if (!isOverrideEnabled) return false

        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_HOME,
                KeyEvent.KEYCODE_TV_NETWORK,
                KeyEvent.KEYCODE_GUIDE -> {
                    launchHome()
                    return true
                }
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op - keep memory footprint zero
    }

    override fun onInterrupt() {
        // No-op
    }

    private fun launchHome() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val PREFS_NAME = "launcher_settings"
        private const val PREF_HOME_OVERRIDE = "home_override_enabled"
        private const val PREF_SUPPRESS_STOCK = "suppress_stock_launcher"

        fun isHomeOverrideEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(PREF_HOME_OVERRIDE, false)
        }

        fun setHomeOverrideEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_HOME_OVERRIDE, enabled).apply()
        }

        fun isSuppressStockLauncherEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(PREF_SUPPRESS_STOCK, false)
        }

        fun setSuppressStockLauncherEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_SUPPRESS_STOCK, enabled).apply()
        }
    }
}
