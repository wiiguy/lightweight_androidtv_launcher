package com.tvlauncher

import android.content.Context

/**
 * F-Droid-flavor no-op updater. This flavor does not ship the in-app
 * self-updater (no WorkManager, no APK downloads); updates are delivered
 * through F-Droid.
 */
object UpdaterBridge {

    fun scheduleWeeklyCheck(context: Context) {
        // No-op: F-Droid handles updates.
    }

    fun isAutoUpdateEnabled(context: Context): Boolean {
        return false
    }

    fun setAutoUpdateEnabled(context: Context, enabled: Boolean) {
        // No-op: F-Droid handles updates.
    }

    fun checkDownloadAndInstall(
        context: Context,
        ignoreAutoUpdateSetting: Boolean = false
    ): UpdateResult {
        return UpdateResult.Skipped
    }
}
