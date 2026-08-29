package com.tvlauncher

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
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

    private enum class FilterTab { ALL, SELECTED, SHORTCUTS }

    private lateinit var appList: RecyclerView
    private lateinit var doneButton: Button
    private lateinit var loadingProgress: ProgressBar
    private lateinit var searchEdit: EditText
    private lateinit var selectedCountBadge: TextView
    private lateinit var chipAll: TextView
    private lateinit var chipSelected: TextView
    private lateinit var chipShortcuts: TextView
    private lateinit var appManager: AppManager
    private lateinit var appSelectionAdapter: AppSelectionAdapter
    private lateinit var shortcutToggle: androidx.appcompat.widget.SwitchCompat

    private var currentFilter = FilterTab.ALL
    private val selectedApps = linkedSetOf<String>()
    private var allLoadedApps = listOf<AppInfo>()
    private var pendingAutoSelectId: String? = null
    private val selectionIconSizePx: Int by lazy { (68 * resources.displayMetrics.density).toInt() }

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
        searchEdit = findViewById(R.id.searchEdit)
        selectedCountBadge = findViewById(R.id.selectedCountBadge)
        chipAll = findViewById(R.id.chipAll)
        chipSelected = findViewById(R.id.chipSelected)
        chipShortcuts = findViewById(R.id.chipShortcuts)
        shortcutToggle = findViewById(R.id.shortcutToggle)

        appManager = AppManager(this)
        selectedApps.addAll(appManager.getSelectedApps())
        shortcutToggle.isChecked = appManager.isShortcutSupportEnabled()
        pendingAutoSelectId = intent.getStringExtra(EXTRA_AUTO_SELECT_ID)?.let { AppIdentifier.normalize(it) }

        updateCountBadge()

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
        setupSearch()
        setupFilterChips()
        reloadAppList(invalidateCache = false)

        ContextCompat.registerReceiver(
            this,
            refreshReceiver,
            android.content.IntentFilter(LauncherBroadcast.ACTION_REFRESH_APP_SELECTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun setupFilterChips() {
        chipAll.setOnClickListener { setFilterTab(FilterTab.ALL) }
        chipSelected.setOnClickListener { setFilterTab(FilterTab.SELECTED) }
        chipShortcuts.setOnClickListener { setFilterTab(FilterTab.SHORTCUTS) }
    }

    private fun setFilterTab(tab: FilterTab) {
        currentFilter = tab
        chipAll.isSelected = tab == FilterTab.ALL
        chipSelected.isSelected = tab == FilterTab.SELECTED
        chipShortcuts.isSelected = tab == FilterTab.SHORTCUTS
        filterApps(searchEdit.text.toString())
    }

    private fun updateCountBadge() {
        selectedCountBadge.text = getString(R.string.selected_badge, selectedApps.size, AppManager.MAX_SLOTS)
    }

    private fun setupSearch() {
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterApps(query: String) {
        val trimmed = query.trim().lowercase()
        val baseList = when (currentFilter) {
            FilterTab.ALL -> allLoadedApps
            FilterTab.SELECTED -> allLoadedApps.filter { app ->
                val id = AppIdentifier.normalize(appManager.getAppIdentifier(app))
                selectedApps.contains(id)
            }
            FilterTab.SHORTCUTS -> allLoadedApps.filter { it.isShortcut }
        }

        val filtered = if (trimmed.isEmpty()) {
            baseList
        } else {
            baseList.filter { app ->
                app.getDisplayName().lowercase().contains(trimmed) ||
                    app.packageName.lowercase().contains(trimmed)
            }
        }
        appSelectionAdapter.submitList(filtered)
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
        updateCountBadge()
        if (currentFilter == FilterTab.SELECTED) {
            filterApps(searchEdit.text.toString())
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
            allLoadedApps = apps
            loadingProgress.visibility = View.GONE
            appList.visibility = View.VISIBLE
            filterApps(searchEdit.text.toString())
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
                updateCountBadge()
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
                updateCountBadge()
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
