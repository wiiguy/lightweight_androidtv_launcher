package com.tvlauncher

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class AppManager(private val context: Context) {

    init {
        instances.add(WeakReference(this))
    }

    private var cachedBaseApps: List<AppInfo>? = null
    private var cachedAppsWithShortcuts: List<AppInfo>? = null
    private var cachedSelectedOrder: List<String>? = null
    private val iconCache = mutableMapOf<String, android.graphics.drawable.Drawable>()
    private val loadExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val loadingApps = AtomicInteger(0)

    fun getInstalledApps(includeShortcuts: Boolean = isShortcutSupportEnabled()): List<AppInfo> {
        if (!includeShortcuts) {
            return getBaseApps()
        }
        cachedAppsWithShortcuts?.let { return it }
        val base = getBaseApps()
        val shortcuts = loadShortcutsForApps(base)
        val combined = (base + shortcuts).sortedBy { it.getDisplayName() }
        combined.forEach { it.setAppManager(this) }
        cachedAppsWithShortcuts = combined
        return combined
    }

    fun loadInstalledAppsAsync(
        includeShortcuts: Boolean,
        onResult: (List<AppInfo>) -> Unit
    ) {
        if (!includeShortcuts && cachedBaseApps != null) {
            onResult(cachedBaseApps!!)
            return
        }
        if (includeShortcuts && cachedAppsWithShortcuts != null) {
            onResult(cachedAppsWithShortcuts!!)
            return
        }

        loadingApps.incrementAndGet()
        loadExecutor.execute {
            val apps = try {
                getInstalledApps(includeShortcuts)
            } catch (_: Exception) {
                emptyList()
            }
            mainHandler.post {
                loadingApps.decrementAndGet()
                onResult(apps)
            }
        }
    }

    fun isLoadingApps(): Boolean = loadingApps.get() > 0

    private fun getBaseApps(): List<AppInfo> {
        cachedBaseApps?.let { return it }
        val packageManager = context.packageManager
        val appMap = linkedMapOf<String, AppInfo>()

        try {
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            installedApps.forEach { appInfo ->
                try {
                    val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    val isThisLauncher = appInfo.packageName == context.packageName

                    if (!isThisLauncher &&
                        (!isSystemApp || isUpdatedSystemApp || isUsefulSystemApp(appInfo.packageName))
                    ) {
                        val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                        if (launchIntent != null) {
                            val appName = packageManager.getApplicationLabel(appInfo).toString()
                            appMap[appInfo.packageName] = AppInfo(appInfo.packageName, appName)
                        }
                    }
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }

        addAppsFromIntentQuery(
            packageManager,
            android.content.Intent.ACTION_MAIN,
            android.content.Intent.CATEGORY_LAUNCHER,
            appMap
        )
        addAppsFromIntentQuery(
            packageManager,
            android.content.Intent.ACTION_MAIN,
            android.content.Intent.CATEGORY_LEANBACK_LAUNCHER,
            appMap
        )

        val apps = appMap.values.sortedBy { it.appName }
        apps.forEach { it.setAppManager(this) }
        cachedBaseApps = apps
        return apps
    }

    private fun addAppsFromIntentQuery(
        packageManager: PackageManager,
        action: String,
        category: String,
        appMap: LinkedHashMap<String, AppInfo>
    ) {
        try {
            val intent = android.content.Intent(action, null).apply { addCategory(category) }
            packageManager.queryIntentActivities(intent, 0).forEach { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo.packageName
                    if (packageName != context.packageName && !appMap.containsKey(packageName)) {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        appMap[packageName] = AppInfo(packageName, appName)
                    }
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun loadShortcutsForApps(baseApps: List<AppInfo>): List<AppInfo> {
        if (!isShortcutSupportEnabled() || Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return emptyList()
        }
        val appNameByPackage = baseApps.associate { it.packageName to it.appName }
        val shortcutApps = ShortcutHelper.queryShortcutsForPackages(
            context,
            baseApps.map { it.packageName }
        )
        return shortcutApps.map { shortcut ->
            shortcut.copy(appName = appNameByPackage[shortcut.packageName] ?: shortcut.packageName)
        }
    }

    private fun isUsefulSystemApp(packageName: String): Boolean {
        return packageName.contains("google") ||
            packageName.contains("android") ||
            packageName.contains("tv")
    }

    fun getSelectedApps(): List<String> {
        cachedSelectedOrder?.let { return it }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val orderString = prefs.getString(PREF_APPS_ORDER, null)
        val order = if (!orderString.isNullOrEmpty()) {
            orderString.split(ORDER_DELIMITER).filter { it.isNotEmpty() }
        } else {
            migrateLegacySelection(prefs)
        }
        val normalized = order.map { AppIdentifier.normalize(it) }.distinct()
        cachedSelectedOrder = normalized
        return normalized
    }

    private fun migrateLegacySelection(prefs: android.content.SharedPreferences): List<String> {
        val legacy = prefs.getStringSet(PREF_APPS_LEGACY, null)?.toList() ?: emptyList()
        if (legacy.isNotEmpty()) {
            val normalized = legacy.map { AppIdentifier.normalize(it) }.distinct()
            saveSelectedApps(normalized)
            prefs.edit().remove(PREF_APPS_LEGACY).apply()
            return normalized
        }
        return emptyList()
    }

    fun saveSelectedApps(apps: Collection<String>) {
        val ordered = apps.map { AppIdentifier.normalize(it) }.distinct().take(MAX_SLOTS)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(PREF_APPS_ORDER, ordered.joinToString(ORDER_DELIMITER))
            .apply()
        cachedSelectedOrder = ordered
    }

    fun moveApp(fromPosition: Int, toPosition: Int): Boolean {
        val current = getSelectedApps().toMutableList()
        if (fromPosition < 0 || fromPosition >= current.size ||
            toPosition < 0 || toPosition >= current.size ||
            fromPosition == toPosition
        ) {
            return false
        }
        Collections.swap(current, fromPosition, toPosition)
        saveSelectedApps(current)
        return true
    }

    fun canAddMoreSelections(currentCount: Int = getSelectedApps().size): Boolean {
        return currentCount < MAX_SLOTS
    }

    fun getSelectedAppInfos(): List<AppInfo> {
        val selectedIds = getSelectedApps()
        if (selectedIds.isEmpty()) {
            return emptyList()
        }

        val orderedResults = mutableListOf<AppInfo>()
        val validIds = mutableListOf<String>()

        selectedIds.forEach { id ->
            val normalized = AppIdentifier.normalize(id)
            val decoded = AppIdentifier.decode(normalized)
            val appInfo = if (decoded.shortcutId == null) {
                loadAppInfoForPackage(decoded.packageName)
            } else {
                val appName = loadAppInfoForPackage(decoded.packageName)?.appName ?: decoded.packageName
                resolveShortcutInfo(decoded.packageName, decoded.shortcutId, appName)
            }
            if (appInfo != null) {
                appInfo.setAppManager(this)
                orderedResults.add(appInfo)
                validIds.add(normalized)
            }
        }

        val previousIds = selectedIds.map { AppIdentifier.normalize(it) }
        if (validIds != previousIds) {
            saveSelectedApps(validIds)
        }

        return orderedResults.take(MAX_SLOTS)
    }

    private fun loadAppInfoForPackage(packageName: String): AppInfo? {
        return try {
            val pm = context.packageManager
            val applicationInfo = pm.getApplicationInfo(packageName, 0)
            AppInfo(packageName, pm.getApplicationLabel(applicationInfo).toString())
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveShortcutInfo(
        packageName: String,
        shortcutId: String,
        appName: String
    ): AppInfo? {
        if (!isShortcutSupportEnabled() || Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return null
        }
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE)
            as? android.content.pm.LauncherApps ?: return null
        val query = ShortcutHelper.buildQuery(packageName, shortcutIds = listOf(shortcutId)) ?: return null
        val shortcut = ShortcutHelper.getShortcuts(launcherApps, query)
            ?.firstOrNull { it.id == shortcutId }
            ?: return null
        return shortcut.toAppInfo(appName)
    }

    fun onTrimMemory(level: Int) {
        iconCache.clear()
        cachedBaseApps?.forEach { it.clearIcon() }
        cachedAppsWithShortcuts?.forEach { it.clearIcon() }
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            invalidateAppListCache()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun ShortcutInfo.toAppInfo(appName: String): AppInfo {
        return AppInfo(
            packageName = `package`,
            appName = appName,
            shortcutId = id,
            shortcutLabel = shortLabel?.toString() ?: longLabel?.toString()
        )
    }

    fun isShortcutSupportEnabled(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_SHORTCUTS_ENABLED, true)
    }

    fun setShortcutSupportEnabled(enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_SHORTCUTS_ENABLED, enabled).apply()
        invalidateAppListCache()
    }

    fun getAppIdentifier(appInfo: AppInfo): String = AppIdentifier.encode(appInfo)

    fun unpinShortcut(packageName: String, shortcutId: String) {
        ShortcutHelper.unpinShortcut(context, packageName, shortcutId)
    }

    fun removeSelectedApp(appId: String) {
        val updated = getSelectedApps().filter { AppIdentifier.normalize(it) != AppIdentifier.normalize(appId) }
        saveSelectedApps(updated)
    }

    fun invalidateAppListCache() {
        cachedBaseApps?.forEach { it.clearIcon() }
        cachedAppsWithShortcuts?.forEach { it.clearIcon() }
        cachedBaseApps = null
        cachedAppsWithShortcuts = null
        iconCache.clear()
    }

    fun invalidateSelectionCache() {
        cachedSelectedOrder = null
    }

    fun clearCache() {
        invalidateAppListCache()
        invalidateSelectionCache()
    }

    fun getCachedIcon(cacheKey: String): android.graphics.drawable.Drawable? = iconCache[cacheKey]

    fun cacheIcon(cacheKey: String, icon: android.graphics.drawable.Drawable) {
        if (iconCache.size < ICON_CACHE_MAX) {
            iconCache[cacheKey] = icon
        }
    }

    companion object {
        /** Max apps on home; the "+" tile is always shown separately (15 icons total). */
        const val MAX_SLOTS = 14
        private const val PREFS_NAME = "selected_apps"
        private const val PREF_APPS_ORDER = "apps_order"
        private const val PREF_APPS_LEGACY = "apps"
        private const val PREF_SHORTCUTS_ENABLED = "shortcuts_enabled"
        private const val ORDER_DELIMITER = ""
        private const val ICON_CACHE_MAX = 16
        private val instances = mutableListOf<WeakReference<AppManager>>()

        fun trimMemoryGlobal(level: Int) {
            instances.removeAll { it.get() == null }
            instances.forEach { it.get()?.onTrimMemory(level) }
        }
    }
}
