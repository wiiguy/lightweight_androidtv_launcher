package com.tvlauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * High-performance, zero-dependency Wallpaper Manager with downsampling for low-RAM devices.
 * Fetches HD wallpapers via Wallhaven / Reddit public endpoints with offline disk caching.
 */
object WallpaperManager {

    const val MODE_SOLID = "solid"
    const val MODE_REDDIT = "reddit"
    const val MODE_CUSTOM = "custom"

    const val CATEGORY_GENERAL = "wallpaper"
    const val CATEGORY_NATURE = "nature"
    const val CATEGORY_CARS = "cars"
    const val CATEGORY_ANIME = "anime"
    const val CATEGORY_SPACE = "space"
    const val CATEGORY_ARCHITECTURE = "city"

    const val INTERVAL_15M = 15 * 60 * 1000L
    const val INTERVAL_1H = 60 * 60 * 1000L
    const val INTERVAL_6H = 6 * 60 * 60 * 1000L
    const val INTERVAL_24H = 24 * 60 * 60 * 1000L

    private const val PREFS_NAME = "wallpaper_settings"
    private const val PREF_MODE = "wallpaper_mode"
    private const val PREF_CATEGORY = "wallpaper_category"
    private const val PREF_INTERVAL = "wallpaper_interval"
    private const val PREF_CUSTOM_URL = "wallpaper_custom_url"
    private const val PREF_DIM_OVERLAY = "wallpaper_dim_overlay"
    private const val PREF_LAST_FETCH_TIME = "wallpaper_last_fetch_time"
    private const val CACHE_FILE_NAME = "current_wallpaper.jpg"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun getWallpaperMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_MODE, MODE_SOLID) ?: MODE_SOLID
    }

    fun setWallpaperMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_MODE, mode).apply()
    }

    fun getCategory(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_CATEGORY, CATEGORY_GENERAL) ?: CATEGORY_GENERAL
    }

    fun setCategory(context: Context, category: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_CATEGORY, category).apply()
    }

    fun getInterval(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(PREF_INTERVAL, INTERVAL_1H)
    }

    fun setInterval(context: Context, interval: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(PREF_INTERVAL, interval).apply()
    }

    fun getCustomUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_CUSTOM_URL, "") ?: ""
    }

    fun setCustomUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_CUSTOM_URL, url).apply()
    }

    fun isDimOverlayEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_DIM_OVERLAY, true)
    }

    fun setDimOverlayEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_DIM_OVERLAY, enabled).apply()
    }

    fun loadCachedWallpaper(context: Context, targetWidth: Int, targetHeight: Int): Drawable? {
        val file = File(context.cacheDir, CACHE_FILE_NAME)
        if (!file.exists() || file.length() == 0L) return null
        return decodeSampledBitmapFromFile(file.absolutePath, targetWidth, targetHeight)?.let {
            BitmapDrawable(context.resources, it)
        }
    }

    fun refreshWallpaperIfNeeded(
        context: Context,
        targetWidth: Int,
        targetHeight: Int,
        force: Boolean = false,
        onComplete: (Boolean, Drawable?) -> Unit = { _, _ -> }
    ) {
        val mode = getWallpaperMode(context)
        if (mode == MODE_SOLID) {
            onComplete(true, null)
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastFetch = prefs.getLong(PREF_LAST_FETCH_TIME, 0L)
        val interval = getInterval(context)
        val now = System.currentTimeMillis()

        val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
        if (!force && cacheFile.exists() && (now - lastFetch < interval)) {
            val cached = loadCachedWallpaper(context, targetWidth, targetHeight)
            onComplete(true, cached)
            return
        }

        executor.execute {
            val imageUrl = when (mode) {
                MODE_REDDIT -> fetchOnlineWallpaperUrl(getCategory(context))
                MODE_CUSTOM -> getCustomUrl(context)
                else -> null
            }

            if (imageUrl.isNullOrEmpty()) {
                mainHandler.post { onComplete(false, null) }
                return@execute
            }

            val success = downloadAndCacheImage(context, imageUrl)
            if (success) {
                prefs.edit().putLong(PREF_LAST_FETCH_TIME, now).apply()
                val drawable = loadCachedWallpaper(context, targetWidth, targetHeight)
                mainHandler.post { onComplete(true, drawable) }
            } else {
                mainHandler.post { onComplete(false, null) }
            }
        }
    }

    private fun fetchOnlineWallpaperUrl(category: String): String? {
        return fetchFromWallhaven(category) ?: fetchFromRedditFallback(category)
    }

    private fun fetchFromWallhaven(category: String): String? {
        return try {
            val query = when (category) {
                CATEGORY_NATURE, "earthporn" -> "nature"
                CATEGORY_CARS, "carporn" -> "cars"
                CATEGORY_ANIME, "animewallpaper" -> "anime"
                CATEGORY_SPACE, "spaceporn" -> "space"
                CATEGORY_ARCHITECTURE, "cityporn" -> "city"
                else -> "wallpaper"
            }
            val endpoint = "https://wallhaven.cc/api/v1/search?q=$query&sorting=random&ratios=16x9,16x10&purity=100"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }

            if (conn.responseCode != 200) return null
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val dataArray = json.getJSONArray("data")
            if (dataArray.length() == 0) return null

            val randomIndex = (0 until dataArray.length()).random()
            val item = dataArray.getJSONObject(randomIndex)
            item.optString("path", null)
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchFromRedditFallback(category: String): String? {
        return try {
            val sub = when (category) {
                CATEGORY_NATURE, "nature" -> "earthporn"
                CATEGORY_CARS, "cars" -> "carporn"
                CATEGORY_ANIME, "anime" -> "animewallpaper"
                CATEGORY_SPACE, "space" -> "spaceporn"
                CATEGORY_ARCHITECTURE, "city" -> "cityporn"
                else -> "wallpaper"
            }
            val endpoint = "https://www.reddit.com/r/$sub/top.json?t=month&limit=25"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android TV) AppleWebKit/537.36")
            }

            if (conn.responseCode != 200) return null
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val children = json.getJSONObject("data").getJSONArray("children")

            val candidates = mutableListOf<String>()
            for (i in 0 until children.length()) {
                val post = children.getJSONObject(i).getJSONObject("data")
                val postUrl = post.optString("url", "")
                val isOver18 = post.optBoolean("over_18", false)
                if (!isOver18 && (postUrl.endsWith(".jpg") || postUrl.endsWith(".jpeg") || postUrl.endsWith(".png"))) {
                    candidates.add(postUrl)
                }
            }
            if (candidates.isEmpty()) null else candidates.random()
        } catch (_: Exception) {
            null
        }
    }

    private fun downloadAndCacheImage(context: Context, imageUrl: String): Boolean {
        var currentUrl = imageUrl
        var redirects = 0
        while (redirects < 5) {
            try {
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12000
                    readTimeout = 12000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                }

                val status = conn.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER
                ) {
                    val location = conn.getHeaderField("Location") ?: return false
                    currentUrl = location
                    redirects++
                    continue
                }

                if (status !in 200..299) return false

                val tempFile = File(context.cacheDir, "${CACHE_FILE_NAME}.tmp")
                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val destFile = File(context.cacheDir, CACHE_FILE_NAME)
                if (destFile.exists()) destFile.delete()
                tempFile.renameTo(destFile)
                return true
            } catch (_: Exception) {
                return false
            }
        }
        return false
    }

    private fun decodeSampledBitmapFromFile(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            val width = if (reqWidth > 0) reqWidth else 1280
            val height = if (reqHeight > 0) reqHeight else 720

            options.inSampleSize = calculateInSampleSize(options, width, height)
            options.inJustDecodeBounds = false
            // Use RGB_565 to cut RAM by 50% for full-screen TV background
            options.inPreferredConfig = Bitmap.Config.RGB_565

            BitmapFactory.decodeFile(path, options)
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
