package com.tvlauncher

/**
 * Result of an in-app update check. Lives in the main source set so UI code can
 * reference it in both flavors; the updater itself is github-flavor only.
 */
enum class UpdateResult {
    NoUpdate,
    InstallStarted,
    InstallPermissionNeeded,
    DownloadFailed,
    InvalidRelease,
    Skipped
}
