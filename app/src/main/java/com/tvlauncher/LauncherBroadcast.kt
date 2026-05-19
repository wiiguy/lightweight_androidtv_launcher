package com.tvlauncher

import android.content.Context
import android.content.Intent

object LauncherBroadcast {
    const val ACTION_REFRESH_HOME = "com.tvlauncher.REFRESH_SHORTCUTS"
    const val ACTION_REFRESH_APP_SELECTION = "com.tvlauncher.REFRESH_APP_SELECTION"

    fun refreshHome(context: Context) {
        context.sendBroadcast(
            Intent(ACTION_REFRESH_HOME).setPackage(context.packageName)
        )
    }

    fun refreshAppSelection(context: Context) {
        context.sendBroadcast(
            Intent(ACTION_REFRESH_APP_SELECTION).setPackage(context.packageName)
        )
    }
}
