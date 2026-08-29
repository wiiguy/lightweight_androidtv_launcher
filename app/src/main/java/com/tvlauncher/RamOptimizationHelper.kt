package com.tvlauncher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object RamOptimizationHelper {

    // Common OEM / Stock TV launcher package names (Google TV, Android TV, Fire TV, Xiaomi, etc.)
    private val KNOWN_STOCK_LAUNCHERS = setOf(
        "com.google.android.tvlauncher",
        "com.google.android.apps.tv.launcherx", // Google TV launcher
        "com.amazon.tv.launcher", // Fire TV launcher
        "com.amazon.firehomestarter",
        "com.mitv.tvhome", // Xiaomi PatchWall
        "com.xiaomi.mitv.tvhome",
        "com.tcl.tvlauncher"
    )

    fun suppressStockLauncherRam(context: Context) {
        if (!TvHomeOverrideService.isSuppressStockLauncherEnabled(context)) return

        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return

            val pm = context.packageManager
            val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
            val homeResolvers = pm.queryIntentActivities(homeIntent, PackageManager.MATCH_ALL)

            val targets = mutableSetOf<String>()
            homeResolvers.forEach { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg != context.packageName && !TvHomeOverrideService.isWhitelistedPackage(pkg)) {
                    targets.add(pkg)
                }
            }
            KNOWN_STOCK_LAUNCHERS.forEach { pkg ->
                if (!TvHomeOverrideService.isWhitelistedPackage(pkg)) {
                    targets.add(pkg)
                }
            }

            targets.forEach { packageName ->
                try {
                    activityManager.killBackgroundProcesses(packageName)
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
    }
}
