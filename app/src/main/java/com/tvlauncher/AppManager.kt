package com.tvlauncher

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

class AppManager(private val context: Context) {
    
    private var cachedApps: List<AppInfo>? = null
    private var cachedSelectedApps: Set<String>? = null
    private val iconCache = mutableMapOf<String, android.graphics.drawable.Drawable>()
    
    fun getInstalledApps(): List<AppInfo> {
        if (cachedApps != null) {
            return cachedApps!!
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
        // Set app manager reference for icon caching
        apps.forEach { it.setAppManager(this) }
        cachedApps = apps
        return apps
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
        val selectedPackages = getSelectedApps()
        val allApps = getInstalledApps()
        return allApps.filter { it.packageName in selectedPackages }
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
        if (iconCache.size < 20) { // Reduced cache size for lower memory usage
            iconCache[packageName] = icon
        }
    }
}
