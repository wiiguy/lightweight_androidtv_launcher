package com.tvlauncher

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
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
                    val loaded = shortcutIcon ?: packageManager.getApplicationIcon(packageName)
                    icon = if (iconSizePx > 0) {
                        renderRoundedIcon(loaded, iconSizePx)
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

    private fun renderRoundedIcon(source: Drawable, sizePx: Int): Drawable {
        if (sizePx <= 0) return source

        // Use ARGB_8888 for clean alpha blending on rounded corners, or fallback
        val rawBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val rawCanvas = Canvas(rawBitmap)
        source.setBounds(0, 0, sizePx, sizePx)
        source.draw(rawCanvas)

        val outputBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val outputCanvas = Canvas(outputBitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(rawBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        // Modern 22% squircle-like corner radius for TV app icons
        val cornerRadius = sizePx * 0.22f
        outputCanvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        rawBitmap.recycle()
        return BitmapDrawable(null, outputBitmap)
    }
}
