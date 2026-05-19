package com.tvlauncher

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppSelectionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_AUTO_SELECT_ID = "com.tvlauncher.EXTRA_AUTO_SELECT_ID"
    }

    private lateinit var appList: RecyclerView
    private lateinit var doneButton: Button
    private lateinit var loadingProgress: ProgressBar
    private lateinit var titleText: TextView
    private lateinit var appManager: AppManager
    private lateinit var appSelectionAdapter: AppSelectionAdapter
    private lateinit var shortcutToggle: androidx.appcompat.widget.SwitchCompat
    private val selectedApps = linkedSetOf<String>()
    private var pendingAutoSelectId: String? = null
    private val selectionIconSizePx: Int by lazy { (60 * resources.displayMetrics.density).toInt() }

    private val refreshReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == LauncherBroadcast.ACTION_REFRESH_APP_SELECTION) {
                reloadAppList(invalidateCache = true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)

        appList = findViewById(R.id.appList)
        doneButton = findViewById(R.id.doneButton)
        loadingProgress = findViewById(R.id.loadingProgress)
        titleText = findViewById(R.id.titleText)
        shortcutToggle = findViewById(R.id.shortcutToggle)

        appManager = AppManager(this)
        selectedApps.addAll(appManager.getSelectedApps())
        shortcutToggle.isChecked = appManager.isShortcutSupportEnabled()
        pendingAutoSelectId = intent.getStringExtra(EXTRA_AUTO_SELECT_ID)?.let { AppIdentifier.normalize(it) }

        titleText.text = getString(R.string.select_apps_max, AppManager.MAX_SLOTS)

        val spanCount = calculateSpanCount()
        appList.layoutManager = GridLayoutManager(this, spanCount)
        appSelectionAdapter = AppSelectionAdapter(
            selectedApps,
            ::onSelectionChanged,
            appManager,
            selectionIconSizePx
        )
        appList.adapter = appSelectionAdapter
        appList.setItemViewCacheSize(5)
        appList.recycledViewPool.setMaxRecycledViews(0, 5)

        setupClickListeners()
        reloadAppList(invalidateCache = false)

        ContextCompat.registerReceiver(
            this,
            refreshReceiver,
            android.content.IntentFilter(LauncherBroadcast.ACTION_REFRESH_APP_SELECTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun onSelectionChanged(appIdentifier: String, isSelected: Boolean) {
        val normalized = AppIdentifier.normalize(appIdentifier)
        if (isSelected) {
            if (!appManager.canAddMoreSelections(selectedApps.size) && !selectedApps.contains(normalized)) {
                Toast.makeText(
                    this,
                    getString(R.string.slot_limit_reached, AppManager.MAX_SLOTS),
                    Toast.LENGTH_SHORT
                ).show()
                appSelectionAdapter.updateSelectionState()
                return
            }
            selectedApps.add(normalized)
        } else {
            selectedApps.remove(normalized)
            handleShortcutUnselect(normalized)
        }
    }

    private fun reloadAppList(invalidateCache: Boolean) {
        if (invalidateCache) {
            appManager.invalidateAppListCache()
        }
        loadingProgress.visibility = View.VISIBLE
        appList.visibility = View.INVISIBLE

        appManager.loadInstalledAppsAsync(shortcutToggle.isChecked) { apps ->
            if (isFinishing) {
                return@loadInstalledAppsAsync
            }
            loadingProgress.visibility = View.GONE
            appList.visibility = View.VISIBLE
            appSelectionAdapter.submitList(apps)
            applyAutoSelectIfNeeded()
        }
    }

    private fun applyAutoSelectIfNeeded() {
        val autoSelectId = pendingAutoSelectId ?: return
        if (!appManager.isShortcutSupportEnabled()) {
            pendingAutoSelectId = null
            return
        }
        if (!selectedApps.contains(autoSelectId)) {
            if (!appManager.canAddMoreSelections(selectedApps.size)) {
                Toast.makeText(
                    this,
                    getString(R.string.slot_limit_reached, AppManager.MAX_SLOTS),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                selectedApps.add(autoSelectId)
                appSelectionAdapter.updateSelectionState()
            }
        }
        pendingAutoSelectId = null
    }

    private fun handleShortcutUnselect(appIdentifier: String) {
        if (!AppIdentifier.isShortcut(appIdentifier)) {
            return
        }
        val decoded = AppIdentifier.decode(appIdentifier)
        val shortcutId = decoded.shortcutId ?: return
        appManager.unpinShortcut(decoded.packageName, shortcutId)
    }

    private fun setupClickListeners() {
        doneButton.setOnClickListener {
            appManager.saveSelectedApps(selectedApps.toList())
            appManager.invalidateAppListCache()
            LauncherBroadcast.refreshHome(this)
            setResult(RESULT_OK)
            finish()
        }

        shortcutToggle.setOnCheckedChangeListener { _, isChecked ->
            appManager.setShortcutSupportEnabled(isChecked)
            if (!isChecked) {
                val toRemove = selectedApps.filter { AppIdentifier.isShortcut(it) }
                selectedApps.removeAll(toRemove.toSet())
            }
            reloadAppList(invalidateCache = true)
            LauncherBroadcast.refreshHome(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(refreshReceiver)
        } catch (_: Exception) {
        }
    }

    private fun calculateSpanCount(): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val itemWidthDp = 180f
        val spanCount = (screenWidthDp / itemWidthDp).toInt()
        return spanCount.coerceIn(3, 6)
    }
}
