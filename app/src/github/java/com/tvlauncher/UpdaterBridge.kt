package com.tvlauncher

import android.content.Context

/**
 * GitHub-flavor updater facade. Delegates to the real in-app updater
 * ([AppUpdateManager]), which is only compiled into this flavor.
 */
object UpdaterBridge {

    fun scheduleWeeklyCheck(context: Context) {
        AppUpdateManager.scheduleWeeklyCheck(context)
    }

    fun isAutoUpdateEnabled(context: Context): Boolean {
        return AppUpdateManager.isAutoUpdateEnabled(context)
    }

    fun setAutoUpdateEnabled(context: Context, enabled: Boolean) {
        AppUpdateManager.setAutoUpdateEnabled(context, enabled)
    }

    fun checkDownloadAndInstall(
        context: Context,
        ignoreAutoUpdateSetting: Boolean = false
    ): UpdateResult {
        return AppUpdateManager.checkDownloadAndInstall(context, ignoreAutoUpdateSetting)
    }
}
