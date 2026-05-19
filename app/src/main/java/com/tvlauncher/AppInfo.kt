package com.tvlauncher

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
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

    fun getIcon(packageManager: PackageManager, iconSizePx: Int = 0): Drawable {
        if (icon == null) {
            val cacheKey = if (isShortcut) {
                "${packageName}_${shortcutId}"
            } else {
                packageName
            }
            val cachedIcon = appManager?.getCachedIcon(cacheKey)
            if (cachedIcon != null) {
                icon = cachedIcon
            } else {
                try {
                    val loaded = packageManager.getApplicationIcon(packageName)
                    icon = if (iconSizePx > 0) {
                        scaleDrawable(loaded, iconSizePx)
                    } else {
                        loaded
                    }
                    appManager?.cacheIcon(cacheKey, icon!!)
                } catch (_: Exception) {
                    icon = packageManager.defaultActivityIcon
                }
            }
        }
        return icon!!
    }

    fun clearIcon() {
        icon = null
    }

    fun getDisplayName(): String {
        return if (isShortcut && !shortcutLabel.isNullOrEmpty()) {
            shortcutLabel
        } else {
            appName
        }
    }

    private fun scaleDrawable(source: Drawable, sizePx: Int): Drawable {
        if (sizePx <= 0) {
            return source
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        source.setBounds(0, 0, sizePx, sizePx)
        source.draw(canvas)
        return BitmapDrawable(null, bitmap)
    }
}
