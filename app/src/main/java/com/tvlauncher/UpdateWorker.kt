package com.tvlauncher

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class UpdateWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return when (AppUpdateManager.checkDownloadAndInstall(applicationContext)) {
            AppUpdateManager.UpdateResult.NoUpdate,
            AppUpdateManager.UpdateResult.InstallStarted,
            AppUpdateManager.UpdateResult.InstallPermissionNeeded,
            AppUpdateManager.UpdateResult.InvalidRelease,
            AppUpdateManager.UpdateResult.Skipped -> Result.success()
            AppUpdateManager.UpdateResult.DownloadFailed -> Result.retry()
        }
    }
}
