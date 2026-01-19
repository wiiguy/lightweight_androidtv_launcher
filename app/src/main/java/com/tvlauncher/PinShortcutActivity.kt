package com.tvlauncher

import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PinShortcutActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            
            
            // Set a minimal transparent layout to prevent crashes
            // We'll use the same layout as app selection but it won't be visible
            try {
                setContentView(R.layout.activity_app_selection)
            } catch (e: Exception) {
                android.util.Log.w("PinShortcutActivity", "Could not set content view, continuing anyway", e)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                handlePinRequest()
            } else {
                finish()
            }
        } catch (e: Exception) {
            android.util.Log.e("PinShortcutActivity", "Exception in onCreate", e)
            finish()
        }
    }
    
    @Suppress("DEPRECATION")
    private fun handlePinRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val launcherApps = getSystemService(android.content.Context.LAUNCHER_APPS_SERVICE) as? android.content.pm.LauncherApps
                if (launcherApps == null) {
                    finish()
                    return
                }
                
                val pinRequest = launcherApps.getPinItemRequest(intent)
                if (pinRequest == null) {
                    finish()
                    return
                }
                
                val requestType = pinRequest.requestType
                if (requestType != android.content.pm.LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
                    finish()
                    return
                }
                
                val shortcutInfo: ShortcutInfo? = pinRequest.shortcutInfo
                if (shortcutInfo == null) {
                    finish()
                    return
                }
                
                if (!pinRequest.isValid()) {
                    finish()
                    return
                }
                
                // Accept the pin request - this makes the shortcut available
                pinRequest.accept()
                android.util.Log.d(
                    "PinShortcutActivity",
                    "Accepted shortcut: ${shortcutInfo.`package`}:${shortcutInfo.id}"
                )
                android.util.Log.d(
                    "PinShortcutActivity",
                    "Accepted shortcut: ${shortcutInfo.`package`}:${shortcutInfo.id}"
                )
                
                // Clear cache so the shortcut will be immediately available
                val appManager = AppManager(this)
                appManager.clearCache()
                
                // Open the app selection screen so user can immediately see and select the shortcut
                val intent = android.content.Intent(this, AppSelectionActivity::class.java)
                intent.putExtra(
                    AppSelectionActivity.EXTRA_AUTO_SELECT_ID,
                    "${shortcutInfo.`package`}:${shortcutInfo.id}"
                )
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                
            } catch (e: Exception) {
                // Handle error - pin request may have failed
                android.util.Log.e("PinShortcutActivity", "Error handling pin request", e)
            }
        }
        finish()
    }
}

