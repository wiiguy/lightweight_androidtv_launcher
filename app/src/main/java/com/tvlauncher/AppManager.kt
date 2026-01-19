package com.tvlauncher

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Build

class AppManager(private val context: Context) {
    
    private var cachedApps: List<AppInfo>? = null
    private var cachedSelectedApps: Set<String>? = null
    private val iconCache = mutableMapOf<String, android.graphics.drawable.Drawable>()
    
    fun getInstalledApps(includeShortcuts: Boolean = true): List<AppInfo> {
        if (cachedApps != null) {
            return if (includeShortcuts) {
                cachedApps!!
            } else {
                cachedApps!!.filter { !it.isShortcut }
            }
        }
        val packageManager = context.packageManager
        val appMap = mutableMapOf<String, AppInfo>()
        
        try {
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            installedApps.forEach { appInfo ->
                try {
                    val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    val isThisLauncher = appInfo.packageName == context.packageName
                    
                    if (!isThisLauncher && 
                        (!isSystemApp || isUpdatedSystemApp || isUsefulSystemApp(appInfo.packageName))) {
                        
                        val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                        if (launchIntent != null) {
                            val appName = packageManager.getApplicationLabel(appInfo).toString()
                            appMap[appInfo.packageName] = AppInfo(appInfo.packageName, appName)
                        }
                    }
                } catch (e: Exception) {
                    // Skip inaccessible apps
                }
            }
        } catch (e: Exception) {
            // Fallback to intent-based approach
        }
        
        // Intent-based discovery for apps that might be missed
        try {
            val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
            launcherIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            val launcherApps = packageManager.queryIntentActivities(launcherIntent, 0)
            
            launcherApps.forEach { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo.packageName
                    if (packageName != context.packageName && !appMap.containsKey(packageName)) {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        appMap[packageName] = AppInfo(packageName, appName)
                    }
                } catch (e: Exception) {
                    // Skip inaccessible apps
                }
            }
        } catch (e: Exception) {
            // Skip if intent query fails
        }
        
        try {
            val leanbackIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
            leanbackIntent.addCategory(android.content.Intent.CATEGORY_LEANBACK_LAUNCHER)
            val leanbackApps = packageManager.queryIntentActivities(leanbackIntent, 0)
            
            leanbackApps.forEach { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo.packageName
                    if (packageName != context.packageName && !appMap.containsKey(packageName)) {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        appMap[packageName] = AppInfo(packageName, appName)
                    }
                } catch (e: Exception) {
                    // Skip inaccessible apps
                }
            }
        } catch (e: Exception) {
            // Skip if leanback query fails
        }
        
        val apps = appMap.values.sortedBy { it.appName }
        apps.forEach { it.setAppManager(this) }

        if (!includeShortcuts) {
            return apps
        }
        
        // Query shortcuts from all apps using LauncherApps (for launchers)
        val appsWithShortcuts = mutableListOf<AppInfo>()
        appsWithShortcuts.addAll(apps)
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? android.content.pm.LauncherApps
                if (launcherApps != null) {
                    // Get all installed packages to query shortcuts from
                    val allPackages = try {
                        context.packageManager.getInstalledPackages(0).map { it.packageName }.toSet()
                    } catch (e: Exception) {
                        apps.map { it.packageName }.toSet()
                    }
                    
                    var shortcutsFound = 0
                    // Query shortcuts for each package using ShortcutManager (simpler API)
                    allPackages.forEach { packageName ->
                        // Skip our own package
                        if (packageName == context.packageName) return@forEach
                        
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
                                // Use LauncherApps.getShortcuts with a reflected ShortcutQuery (no Builder)
                                val shortcuts = try {
                                    val flags = android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                                        android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                                        android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST

                                    val query = buildShortcutQuery(packageName, flags)
                                    var result = if (query != null) {
                                        getShortcuts(launcherApps, query)
                                    } else {
                                        null
                                    }

                                    if (result.isNullOrEmpty()) {
                                        // Fallback: query without package and filter locally
                                        val fallbackQuery = buildShortcutQuery(null, flags)
                                        if (fallbackQuery != null) {
                                            val allShortcuts = getShortcuts(launcherApps, fallbackQuery)
                                            result = allShortcuts?.filter { shortcut ->
                                                (shortcut as? ShortcutInfo)?.`package` == packageName
                                            }
                                        }
                                    }

                                    if (packageName == "de.szalkowski.activitylauncher") {
                                        android.util.Log.d(
                                            "AppManager",
                                            "Shortcuts found for $packageName: ${result?.size ?: 0}"
                                        )
                                    }
                                    result
                                } catch (e: Exception) {
                                    null
                                }
                                
                                shortcuts?.let { shortcutList ->
                                    if (shortcutList.isNotEmpty()) {
                                        shortcutsFound += shortcutList.size
                                    }
                                }
                                
                                shortcuts?.forEach { shortcutObj ->
                                    try {
                                        val shortcut = shortcutObj as android.content.pm.ShortcutInfo
                                        
                                        // Get app name for this package
                                        val appName = try {
                                            apps.firstOrNull { it.packageName == packageName }?.appName
                                                ?: try {
                                                    val pkgInfo = context.packageManager.getPackageInfo(packageName, 0)
                                                    context.packageManager.getApplicationLabel(pkgInfo.applicationInfo).toString()
                                                } catch (e: Exception) {
                                                    packageName
                                                }
                                        } catch (e: Exception) {
                                            packageName
                                        }
                                        
                                        val shortcutAppInfo = AppInfo(
                                            packageName = packageName,
                                            appName = appName,
                                            shortcutId = shortcut.id,
                                            shortcutLabel = shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString(),
                                            shortcutIcon = null
                                        )
                                        shortcutAppInfo.setAppManager(this)
                                        
                                        // Only add if not already in list (avoid duplicates)
                                        if (!appsWithShortcuts.any { it.packageName == packageName && it.shortcutId == shortcut.id }) {
                                            appsWithShortcuts.add(shortcutAppInfo)
                                        }
                                    } catch (e: Exception) {
                                        // Skip shortcuts that can't be loaded
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Skip packages that don't have shortcuts or can't query them
                            if (packageName == "de.szalkowski.activitylauncher") {
                                android.util.Log.e("AppManager", "Error querying shortcuts for $packageName", e)
                            }
                        }
                    }
                } else {
                }
            } else {
            }
        } catch (e: Exception) {
        }
        
        // Set app manager reference for icon caching
        appsWithShortcuts.forEach { it.setAppManager(this) }
        val sortedApps = appsWithShortcuts.sortedBy { it.getDisplayName() }
        cachedApps = sortedApps
        return sortedApps
    }
    
    private fun isUsefulSystemApp(packageName: String): Boolean {
        return packageName.contains("google") || 
               packageName.contains("android") ||
               packageName.contains("tv")
    }

    fun getSelectedApps(): Set<String> {
        if (cachedSelectedApps != null) {
            return cachedSelectedApps!!
        }
        val prefs = context.getSharedPreferences("selected_apps", Context.MODE_PRIVATE)
        val apps = prefs.getStringSet("apps", emptySet()) ?: emptySet()
        cachedSelectedApps = apps
        return apps
    }
    
    fun saveSelectedApps(apps: Set<String>) {
        val prefs = context.getSharedPreferences("selected_apps", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("apps", apps).apply()
        cachedSelectedApps = apps
    }
    
    fun getSelectedAppInfos(): List<AppInfo> {
        val selectedIds = getSelectedApps()
        val appsOnly = getInstalledApps(includeShortcuts = false)
        val appMap = appsOnly.associateBy { it.packageName }
        val results = mutableListOf<AppInfo>()

        val shortcutIdsByPackage = mutableMapOf<String, MutableList<String>>()
        selectedIds.forEach { id ->
            val parts = id.split(":", limit = 2)
            if (parts.size == 2) {
                shortcutIdsByPackage.getOrPut(parts[0]) { mutableListOf() }.add(parts[1])
            } else {
                appMap[id]?.let { results.add(it) }
            }
        }

        if (shortcutIdsByPackage.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? android.content.pm.LauncherApps
            if (launcherApps != null) {
                shortcutIdsByPackage.forEach { (packageName, shortcutIds) ->
                    val flags = android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                        android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                    val query = buildShortcutQuery(packageName, flags, shortcutIds)
                    val shortcuts = if (query != null) getShortcuts(launcherApps, query) else null
                    val appName = appMap[packageName]?.appName ?: packageName

                    shortcuts?.forEach shortcutLoop@ { shortcutObj ->
                        val shortcut = shortcutObj as? ShortcutInfo ?: return@shortcutLoop
                        val shortcutAppInfo = AppInfo(
                            packageName = packageName,
                            appName = appName,
                            shortcutId = shortcut.id,
                            shortcutLabel = shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString(),
                            shortcutIcon = null
                        )
                        shortcutAppInfo.setAppManager(this)
                        results.add(shortcutAppInfo)
                    }
                }
            }
        }

        return results
    }
    
    fun getAppIdentifier(appInfo: AppInfo): String {
        return if (appInfo.isShortcut) {
            "${appInfo.packageName}:${appInfo.shortcutId}"
        } else {
            appInfo.packageName
        }
    }

    fun unpinShortcut(packageName: String, shortcutId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? android.content.pm.LauncherApps
                ?: return

            val query = buildShortcutQuery(packageName, android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
                ?: return
            val pinnedShortcuts = getShortcuts(launcherApps, query)
                ?.mapNotNull { it as? ShortcutInfo }
                ?.filter { it.isPinned }
                ?.map { it.id }
                ?.toMutableList()
                ?: return

            if (!pinnedShortcuts.remove(shortcutId)) {
                return
            }

            launcherApps.pinShortcuts(packageName, pinnedShortcuts, android.os.Process.myUserHandle())
        } catch (e: Exception) {
            android.util.Log.e("AppManager", "Failed to unpin shortcut $packageName:$shortcutId", e)
        }
    }
    
    fun clearCache() {
        cachedApps = null
        cachedSelectedApps = null
        iconCache.clear()
        
        // Clear SharedPreferences cache
        try {
            val prefs = context.getSharedPreferences("selected_apps", Context.MODE_PRIVATE)
            // Force SharedPreferences to unload from memory
            prefs.edit().apply()
        } catch (e: Exception) {
            // Ignore if prefs are already cleared
        }
        
        // Force multiple garbage collections for aggressive cleanup
        System.gc()
        System.runFinalization()
        System.gc()
    }
    
    fun getCachedIcon(packageName: String): android.graphics.drawable.Drawable? {
        return iconCache[packageName]
    }
    
    fun cacheIcon(packageName: String, icon: android.graphics.drawable.Drawable) {
        if (iconCache.size < 10) { // Smaller cache to reduce RAM usage
            iconCache[packageName] = icon
        }
    }

    private fun buildShortcutQuery(
        packageName: String?,
        flags: Int,
        shortcutIds: List<String>? = null
    ): Any? {
        // Try direct API first (no reflection)
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
            } catch (e: Exception) {
                // Fall back to reflection below
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
        } catch (e: Exception) {
            null
        }
    }

    private fun getShortcuts(
        launcherApps: android.content.pm.LauncherApps,
        query: Any
    ): List<*>? {
        return try {
            if (query is LauncherApps.ShortcutQuery) {
                launcherApps.getShortcuts(query, android.os.Process.myUserHandle())
            } else {
                val shortcutQueryClass = Class.forName("android.content.pm.LauncherApps\$ShortcutQuery")
                val getShortcutsMethod = launcherApps.javaClass.getMethod(
                    "getShortcuts",
                    shortcutQueryClass,
                    android.os.UserHandle::class.java
                )
                getShortcutsMethod.invoke(launcherApps, query, android.os.Process.myUserHandle()) as? List<*>
            }
        } catch (e: Exception) {
            null
        }
    }
}
