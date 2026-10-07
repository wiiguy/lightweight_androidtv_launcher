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
            UpdateResult.NoUpdate,
            UpdateResult.InstallStarted,
            UpdateResult.InstallPermissionNeeded,
            UpdateResult.InvalidRelease,
            UpdateResult.Skipped -> Result.success()
            UpdateResult.DownloadFailed -> Result.retry()
        }
    }
}
