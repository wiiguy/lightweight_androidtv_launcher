package com.tvlauncher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
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

            pickTargets(context, pm).forEach { packageName ->
                try {
                    activityManager.killBackgroundProcesses(packageName)
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Chooses which packages may be killed. Only pre-installed (system) launchers
     * are ever targeted — user-installed launchers (e.g. a third-party launcher the
     * user picked) are never touched, and settings/installer packages are whitelisted.
     */
    private fun pickTargets(context: Context, pm: PackageManager): Set<String> {
        val ownPackage = context.packageName
        val targets = mutableSetOf<String>()

        KNOWN_STOCK_LAUNCHERS.forEach { pkg ->
            if (pkg != ownPackage &&
                !TvHomeOverrideService.isWhitelistedPackage(pkg) &&
                isInstalled(pm, pkg)
            ) {
                targets.add(pkg)
            }
        }

        // Dynamically discovered HOME-capable apps: only system / updated-system apps.
        val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        pm.queryIntentActivities(homeIntent, PackageManager.MATCH_ALL).forEach { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == ownPackage || TvHomeOverrideService.isWhitelistedPackage(pkg)) {
                return@forEach
            }
            if (pkg in targets) {
                return@forEach
            }
            val appInfo = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (_: Exception) {
                null
            } ?: return@forEach
            val isSystemApp = (appInfo.flags and
                (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
            if (isSystemApp) {
                targets.add(pkg)
            }
        }
        return targets
    }

    private fun isInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getApplicationInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
