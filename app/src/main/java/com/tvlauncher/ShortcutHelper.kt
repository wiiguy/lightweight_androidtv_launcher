package com.tvlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Process

object ShortcutHelper {
    private val shortcutFlags: Int
        get() = LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST

    fun buildQuery(
        packageName: String?,
        flags: Int = shortcutFlags,
        shortcutIds: List<String>? = null
    ): Any? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val query = LauncherApps.ShortcutQuery()
                if (packageName != null) {
                    query.setPackage(packageName)
                }
                if (shortcutIds != null) {
                    query.setShortcutIds(shortcutIds)
                }
                query.setQueryFlags(flags)
                return query
            } catch (_: Exception) {
                // Fall back to reflection
            }
        }

        return try {
            val shortcutQueryClass = Class.forName("android.content.pm.LauncherApps\$ShortcutQuery")
            val query = shortcutQueryClass.getDeclaredConstructor().newInstance()
            val setPackageMethod = shortcutQueryClass.methods.firstOrNull {
                it.name == "setPackage" && it.parameterTypes.size == 1
            } ?: shortcutQueryClass.methods.firstOrNull {
                it.name == "setPackageName" && it.parameterTypes.size == 1
            }
            val setQueryFlagsMethod = shortcutQueryClass.methods.firstOrNull {
                it.name == "setQueryFlags" && it.parameterTypes.size == 1
            }
            val setShortcutIdsMethod = shortcutQueryClass.methods.firstOrNull {
                it.name == "setShortcutIds" && it.parameterTypes.size == 1
            }
            if (setQueryFlagsMethod == null) {
                null
            } else {
                if (packageName != null && setPackageMethod != null) {
                    setPackageMethod.invoke(query, packageName)
                }
                if (shortcutIds != null && setShortcutIdsMethod != null) {
                    setShortcutIdsMethod.invoke(query, shortcutIds)
                }
                setQueryFlagsMethod.invoke(query, flags)
                query
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getShortcuts(context: Context, query: Any): List<ShortcutInfo>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return null
        }
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: return null
        return getShortcuts(launcherApps, query)
    }

    fun getShortcuts(launcherApps: LauncherApps, query: Any): List<ShortcutInfo>? {
        return try {
            val raw = if (query is LauncherApps.ShortcutQuery) {
                launcherApps.getShortcuts(query, Process.myUserHandle())
            } else {
                val shortcutQueryClass = Class.forName("android.content.pm.LauncherApps\$ShortcutQuery")
                val getShortcutsMethod = launcherApps.javaClass.getMethod(
                    "getShortcuts",
                    shortcutQueryClass,
                    android.os.UserHandle::class.java
                )
                getShortcutsMethod.invoke(launcherApps, query, Process.myUserHandle()) as? List<*>
            }
            raw?.mapNotNull { it as? ShortcutInfo }
        } catch (_: Exception) {
            null
        }
    }

    fun queryShortcutsForPackages(
        context: Context,
        packageNames: Collection<String>
    ): List<AppInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return emptyList()
        }
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: return emptyList()
        val results = mutableListOf<AppInfo>()
        val seen = mutableSetOf<String>()

        packageNames.forEach { packageName ->
            if (packageName == context.packageName) {
                return@forEach
            }
            val query = buildQuery(packageName) ?: return@forEach
            var shortcuts = getShortcuts(launcherApps, query)
            if (shortcuts.isNullOrEmpty()) {
                val fallbackQuery = buildQuery(null) ?: return@forEach
                shortcuts = getShortcuts(launcherApps, fallbackQuery)
                    ?.filter { it.`package` == packageName }
            }
            shortcuts?.forEach shortcutLoop@ { shortcut ->
                val key = AppIdentifier.encode(packageName, shortcut.id)
                if (!seen.add(key)) {
                    return@shortcutLoop
                }
                results.add(
                    AppInfo(
                        packageName = packageName,
                        appName = packageName,
                        shortcutId = shortcut.id,
                        shortcutLabel = shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString()
                    )
                )
            }
        }
        return results
    }

    fun launchApp(context: Context, packageName: String) {
        val packageManager = context.packageManager
        try {
            packageManager.getLaunchIntentForPackage(packageName)?.let {
                context.startActivity(it)
                return
            }
        } catch (_: Exception) {
        }

        try {
            val leanbackIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                setPackage(packageName)
            }
            val resolveInfos = packageManager.queryIntentActivities(leanbackIntent, 0)
            if (resolveInfos.isNotEmpty()) {
                val activityInfo = resolveInfos[0].activityInfo
                context.startActivity(
                    Intent().apply {
                        setClassName(activityInfo.packageName, activityInfo.name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                return
            }
        } catch (_: Exception) {
        }

        try {
            val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
            }
            val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            if (resolveInfos.isNotEmpty()) {
                val activityInfo = resolveInfos[0].activityInfo
                context.startActivity(
                    Intent().apply {
                        setClassName(activityInfo.packageName, activityInfo.name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        } catch (_: Exception) {
        }
    }

    fun launchShortcut(context: Context, packageName: String, shortcutId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            launchApp(context, packageName)
            return
        }
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: run {
                launchApp(context, packageName)
                return
            }
        val query = buildQuery(packageName, shortcutIds = listOf(shortcutId)) ?: run {
            launchApp(context, packageName)
            return
        }
        val shortcut = getShortcuts(launcherApps, query)?.firstOrNull { it.id == shortcutId }
        if (shortcut != null) {
            try {
                launcherApps.startShortcut(shortcut, null, null)
                return
            } catch (_: Exception) {
            }
        }
        launchApp(context, packageName)
    }

    fun unpinShortcut(context: Context, packageName: String, shortcutId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                ?: return
            val query = buildQuery(
                packageName,
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            ) ?: return
            val pinnedShortcuts = getShortcuts(launcherApps, query)
                ?.filter { it.isPinned }
                ?.map { it.id }
                ?.toMutableList()
                ?: return
            if (!pinnedShortcuts.remove(shortcutId)) {
                return
            }
            launcherApps.pinShortcuts(packageName, pinnedShortcuts, Process.myUserHandle())
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ShortcutHelper", "Failed to unpin $packageName:$shortcutId", e)
            }
        }
    }
}
