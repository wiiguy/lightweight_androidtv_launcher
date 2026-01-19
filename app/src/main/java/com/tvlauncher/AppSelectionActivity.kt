package com.tvlauncher

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView

class AppSelectionActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_AUTO_SELECT_ID = "com.tvlauncher.EXTRA_AUTO_SELECT_ID"
    }

    private lateinit var appList: RecyclerView
    private lateinit var doneButton: Button
    private lateinit var appManager: AppManager
    private lateinit var appSelectionAdapter: AppSelectionAdapter
    private val selectedApps = mutableSetOf<String>()
    private var pendingAutoSelectId: String? = null
    
    private val refreshReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == "com.tvlauncher.REFRESH_APP_SELECTION") {
                // Refresh the app list when a shortcut is pinned
                refreshAppList()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)
        
        appList = findViewById(R.id.appList)
        doneButton = findViewById(R.id.doneButton)
        
        appManager = AppManager(this)
        selectedApps.addAll(appManager.getSelectedApps())
        pendingAutoSelectId = intent.getStringExtra(EXTRA_AUTO_SELECT_ID)
        setupRecyclerView()
        setupClickListeners()
        applyAutoSelectIfNeeded()
        
        // Register broadcast receiver for shortcut refresh
        val filter = android.content.IntentFilter("com.tvlauncher.REFRESH_APP_SELECTION")
        registerReceiver(refreshReceiver, filter)
    }
    
    private fun refreshAppList() {
        // Clear cache and reload
        appManager.clearCache()
        val allApps = appManager.getInstalledApps()
        
        // Update adapter with new list
        appSelectionAdapter = AppSelectionAdapter(
            allApps,
            selectedApps,
            { appIdentifier, isSelected ->
                if (isSelected) {
                    selectedApps.add(appIdentifier)
                } else {
                    selectedApps.remove(appIdentifier)
                    handleShortcutUnselect(appIdentifier)
                }
            },
            appManager
        )
        appList.adapter = appSelectionAdapter
        applyAutoSelectIfNeeded()
    }
    
    private fun setupRecyclerView() {
        val allApps = appManager.getInstalledApps()
        
        appSelectionAdapter = AppSelectionAdapter(
            allApps,
            selectedApps,
            { appIdentifier, isSelected ->
                // Just update the selection set, don't save or finish yet
                if (isSelected) {
                    selectedApps.add(appIdentifier)
                } else {
                    selectedApps.remove(appIdentifier)
                    handleShortcutUnselect(appIdentifier)
                }
            },
            appManager
        )
        
        // Calculate number of columns based on screen size
        val spanCount = calculateSpanCount()
        appList.layoutManager = GridLayoutManager(this@AppSelectionActivity, spanCount)
        appList.adapter = appSelectionAdapter
        // Reduce RecyclerView cache sizes for lower memory usage
        appList.setItemViewCacheSize(5)
        appList.recycledViewPool.setMaxRecycledViews(0, 5)
    }

    private fun applyAutoSelectIfNeeded() {
        val autoSelectId = pendingAutoSelectId ?: return
        if (!selectedApps.contains(autoSelectId)) {
            selectedApps.add(autoSelectId)
            appManager.saveSelectedApps(selectedApps)
            appSelectionAdapter.notifyDataSetChanged()
            sendBroadcast(android.content.Intent("com.tvlauncher.REFRESH_SHORTCUTS"))
        }
        pendingAutoSelectId = null
    }

    private fun handleShortcutUnselect(appIdentifier: String) {
        if (!appIdentifier.contains(":")) {
            return
        }
        val parts = appIdentifier.split(":", limit = 2)
        if (parts.size != 2) {
            return
        }
        val packageName = parts[0]
        val shortcutId = parts[1]
        appManager.unpinShortcut(packageName, shortcutId)
        appManager.saveSelectedApps(selectedApps)
        appManager.clearCache()
        refreshAppList()
        sendBroadcast(android.content.Intent("com.tvlauncher.REFRESH_SHORTCUTS"))
    }
    
    private fun setupClickListeners() {
        doneButton.setOnClickListener {
            appManager.saveSelectedApps(selectedApps)
            // Clear cache before finishing to free memory
            appManager.clearCache()
            setResult(RESULT_OK)
            finish()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Clear cache when paused to free memory
        appManager.clearCache()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(refreshReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
    
    private fun calculateSpanCount(): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        // Aim for items around 180-200dp wide
        val itemWidthDp = 180f
        val spanCount = (screenWidthDp / itemWidthDp).toInt()
        // Ensure at least 3 columns and at most 6 columns
        return spanCount.coerceIn(3, 6)
    }
}
