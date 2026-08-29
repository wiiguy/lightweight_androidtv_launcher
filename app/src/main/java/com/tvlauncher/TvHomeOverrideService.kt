package com.tvlauncher

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Bulletproof Accessibility Service to intercept HOME button and detect when
 * the default OEM launcher window appears on screen (Google TV, Fire TV, MiTV, etc.)
 */
class TvHomeOverrideService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var knownStockLauncherPackages = mutableSetOf(
        "com.google.android.tvlauncher",
        "com.google.android.apps.tv.launcherx",
        "com.google.android.katniss",
        "com.amazon.tv.launcher",
        "com.amazon.firehomestarter",
        "com.mitv.tvhome",
        "com.xiaomi.mitv.tvhome",
        "com.tcl.tvlauncher"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            info.notificationTimeout = 50
            serviceInfo = info
        } catch (_: Exception) {
        }
        discoverHomePackages()
    }

    private fun discoverHomePackages() {
        try {
            val pm = packageManager
            val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
            val homeResolvers = pm.queryIntentActivities(homeIntent, PackageManager.MATCH_ALL)
            homeResolvers.forEach { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg != packageName) {
                    knownStockLauncherPackages.add(pkg)
                }
            }
        } catch (_: Exception) {
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false
        if (!isHomeOverrideEnabled(this)) return false

        val keyCode = event.keyCode
        val isHomeKey = keyCode == KeyEvent.KEYCODE_HOME ||
            keyCode == KeyEvent.KEYCODE_TV_NETWORK ||
            keyCode == KeyEvent.KEYCODE_GUIDE ||
            keyCode == KeyEvent.KEYCODE_APP_SWITCH

        if (isHomeKey) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                launchHome()
            }
            return true
        }

        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!isHomeOverrideEnabled(this)) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkgName = event.packageName?.toString() ?: return
            if (pkgName != packageName && knownStockLauncherPackages.contains(pkgName)) {
                // Stock OEM Launcher intercepted on screen! Immediately pull our launcher to front.
                mainHandler.removeCallbacksAndMessages(null)
                mainHandler.postDelayed({
                    launchHome()
                }, 30)
            }
        }
    }

    override fun onInterrupt() {
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun launchHome() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            startActivity(intent)
            if (isSuppressStockLauncherEnabled(this)) {
                RamOptimizationHelper.suppressStockLauncherRam(applicationContext)
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val PREFS_NAME = "launcher_settings"
        private const val PREF_HOME_OVERRIDE = "home_override_enabled"
        private const val PREF_SUPPRESS_STOCK = "suppress_stock_launcher"

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${TvHomeOverrideService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            return enabledServices.split(':').any {
                it.equals(expectedServiceName, ignoreCase = true) ||
                    it.contains(context.packageName)
            }
        }

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
