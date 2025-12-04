package com.tvlauncher

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    var icon: Drawable? = null,
    private var appManager: AppManager? = null
) {
    fun setAppManager(manager: AppManager) {
        appManager = manager
    }
    
    fun getIcon(packageManager: PackageManager): Drawable {
        if (icon == null) {
            // Check cache first
            val cachedIcon = appManager?.getCachedIcon(packageName)
            if (cachedIcon != null) {
                icon = cachedIcon
            } else {
                try {
                    icon = packageManager.getApplicationIcon(packageName)
                    appManager?.cacheIcon(packageName, icon!!)
                } catch (e: Exception) {
                    icon = packageManager.defaultActivityIcon
                }
            }
        }
        return icon!!
    }
}

