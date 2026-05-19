package com.tvlauncher

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PinShortcutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_shortcut)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            handlePinRequest()
        } else {
            finish()
        }
    }

    private fun handlePinRequest() {
        try {
            val launcherApps = getSystemService(android.content.Context.LAUNCHER_APPS_SERVICE)
                as? android.content.pm.LauncherApps
            if (launcherApps == null) {
                finish()
                return
            }

            val pinRequest = launcherApps.getPinItemRequest(intent)
            if (pinRequest == null ||
                pinRequest.requestType != android.content.pm.LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT ||
                !pinRequest.isValid()
            ) {
                finish()
                return
            }

            val shortcutInfo: ShortcutInfo = pinRequest.shortcutInfo ?: run {
                finish()
                return
            }

            val appManager = AppManager(this)
            if (!appManager.isShortcutSupportEnabled()) {
                finish()
                return
            }

            pinRequest.accept()

            appManager.invalidateAppListCache()

            val autoSelectId = AppIdentifier.encode(shortcutInfo.`package`, shortcutInfo.id)
            val selectionIntent = Intent(this, AppSelectionActivity::class.java).apply {
                putExtra(AppSelectionActivity.EXTRA_AUTO_SELECT_ID, autoSelectId)
            }
            startActivity(selectionIntent)
            LauncherBroadcast.refreshAppSelection(this)
            LauncherBroadcast.refreshHome(this)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("PinShortcutActivity", "Error handling pin request", e)
            }
        } finally {
            finish()
        }
    }
}
