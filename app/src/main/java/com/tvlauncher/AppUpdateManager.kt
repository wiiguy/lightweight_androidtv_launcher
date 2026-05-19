package com.tvlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager

object AppUpdateManager {

    private const val PREFS_NAME = "app_update"
    private const val PREF_AUTO_UPDATE = "auto_update_enabled"
    private const val GITHUB_API_LATEST =
        "https://api.github.com/repos/wiiguy/lightweight_androidtv_launcher/releases/latest"
    private const val WORK_NAME = "tvlauncher_weekly_update"
    private const val APK_FILE_NAME = "tvlauncher-update.apk"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 60_000

    data class ReleaseInfo(
        val versionName: String,
        val downloadUrl: String
    )

    data class ApkVersionInfo(
        val versionName: String,
        val versionCode: Long
    )

    fun isAutoUpdateEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_AUTO_UPDATE, true)
    }

    fun setAutoUpdateEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_AUTO_UPDATE, enabled)
            .apply()
        if (enabled) {
            scheduleWeeklyCheck(context)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    fun scheduleWeeklyCheck(context: Context) {
        if (!isAutoUpdateEnabled(context)) {
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<UpdateWorker>(7, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun checkDownloadAndInstall(context: Context, ignoreAutoUpdateSetting: Boolean = false): UpdateResult {
        if (!ignoreAutoUpdateSetting && !isAutoUpdateEnabled(context)) {
            return UpdateResult.Skipped
        }
        val currentVersion = getInstalledVersionName(context)
        val installedVersionCode = getInstalledVersionCode(context)
        val release = fetchLatestRelease() ?: return UpdateResult.NoUpdate
        if (!VersionUtils.isNewer(release.versionName, currentVersion)) {
            return UpdateResult.NoUpdate
        }
        val apkFile = downloadApk(context, release.downloadUrl) ?: return UpdateResult.DownloadFailed
        val apkVersion = readApkVersion(context, apkFile) ?: return UpdateResult.DownloadFailed
        if (apkVersion.versionCode <= installedVersionCode) {
            apkFile.delete()
            return UpdateResult.InvalidRelease
        }
        if (!VersionUtils.isNewer(apkVersion.versionName, currentVersion)) {
            apkFile.delete()
            return UpdateResult.InvalidRelease
        }
        return if (promptInstall(context, apkFile, apkVersion.versionName)) {
            UpdateResult.InstallStarted
        } else {
            UpdateResult.InstallPermissionNeeded
        }
    }

    fun getInstalledVersionName(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME
        }
    }

    fun getInstalledVersionCode(context: Context): Long {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (_: Exception) {
            BuildConfig.VERSION_CODE.toLong()
        }
    }

    fun readApkVersion(context: Context, apkFile: File): ApkVersionInfo? {
        val packageInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            ?: return null
        packageInfo.applicationInfo?.let { appInfo ->
            appInfo.sourceDir = apkFile.absolutePath
            appInfo.publicSourceDir = apkFile.absolutePath
        }
        val versionName = packageInfo.versionName ?: return null
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        return ApkVersionInfo(versionName = versionName, versionCode = versionCode)
    }

    fun fetchLatestRelease(): ReleaseInfo? {
        val connection = openGet(GITHUB_API_LATEST) ?: return null
        return try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseReleaseJson(body)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun parseReleaseJson(json: String): ReleaseInfo? {
        val root = JSONObject(json)
        val tagName = root.optString("tag_name", "")
        if (tagName.isEmpty()) {
            return null
        }
        val assets = root.optJSONArray("assets") ?: return null
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name", "")
            val url = asset.optString("browser_download_url", "")
            if (name.endsWith(".apk") && isAllowedApkUrl(url)) {
                if (name == "app-release.apk" || apkUrl == null) {
                    apkUrl = url
                }
            }
        }
        val downloadUrl = apkUrl ?: return null
        return ReleaseInfo(
            versionName = VersionUtils.normalizeTag(tagName),
            downloadUrl = downloadUrl
        )
    }

    private fun isAllowedApkUrl(url: String): Boolean {
        return url.startsWith("https://github.com/") ||
            url.startsWith("https://objects.githubusercontent.com/")
    }

    fun downloadApk(context: Context, downloadUrl: String): File? {
        if (!isAllowedApkUrl(downloadUrl)) {
            return null
        }
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val outFile = File(dir, APK_FILE_NAME)
        if (outFile.exists()) {
            outFile.delete()
        }

        val connection = openGet(downloadUrl) ?: return null
        return try {
            val input = BufferedInputStream(connection.inputStream)
            FileOutputStream(outFile).use { output ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) {
                        break
                    }
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
            if (outFile.length() < 10_000) {
                outFile.delete()
                null
            } else {
                outFile
            }
        } catch (_: Exception) {
            outFile.delete()
            null
        } finally {
            connection.disconnect()
        }
    }

    fun promptInstall(context: Context, apkFile: File, versionName: String): Boolean {
        if (!canInstallPackages(context)) {
            openInstallPermissionSettings(context)
            return false
        }
        val intent = Intent(context, UpdatePromptActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(UpdatePromptActivity.EXTRA_VERSION, versionName)
            putExtra(UpdatePromptActivity.EXTRA_APK_PATH, apkFile.absolutePath)
        }
        context.startActivity(intent)
        return true
    }

    fun installDownloadedApk(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) {
            return false
        }
        if (!canInstallPackages(context)) {
            openInstallPermissionSettings(context)
            return false
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        try {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun openGet(url: String): HttpURLConnection? {
        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "TVLauncher/${BuildConfig.VERSION_NAME}")
            }
            if (connection.responseCode in 200..299) {
                connection
            } else {
                connection.disconnect()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    enum class UpdateResult {
        NoUpdate,
        InstallStarted,
        InstallPermissionNeeded,
        DownloadFailed,
        InvalidRelease,
        Skipped
    }
}
