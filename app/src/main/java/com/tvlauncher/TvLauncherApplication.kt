package com.tvlauncher

import android.app.Application
import android.content.ComponentCallbacks2

class TvLauncherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppUpdateManager.scheduleWeeklyCheck(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            AppManager.trimMemoryGlobal(level)
        }
    }
}
