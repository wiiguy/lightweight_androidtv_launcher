package com.tvlauncher

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    var icon: Drawable? = null,
    private var appManager: AppManager? = null,
    val shortcutId: String? = null,
    val shortcutLabel: String? = null,
    val shortcutIcon: Drawable? = null
) {
    val isShortcut: Boolean
        get() = shortcutId != null
    
    fun setAppManager(manager: AppManager) {
        appManager = manager
    }
    
    fun getIcon(packageManager: PackageManager): Drawable {
        if (icon == null) {
            // Use the app icon for shortcuts to minimize memory usage
            val cacheKey = packageName
            val cachedIcon = appManager?.getCachedIcon(cacheKey)
            if (cachedIcon != null) {
                icon = cachedIcon
            } else {
                try {
                    icon = packageManager.getApplicationIcon(packageName)
                    appManager?.cacheIcon(cacheKey, icon!!)
                } catch (e: Exception) {
                    icon = packageManager.defaultActivityIcon
                }
            }
        }
        return icon!!
    }
    
    fun getDisplayName(): String {
        return if (isShortcut && !shortcutLabel.isNullOrEmpty()) {
            shortcutLabel!!
        } else {
            appName
        }
    }
}

