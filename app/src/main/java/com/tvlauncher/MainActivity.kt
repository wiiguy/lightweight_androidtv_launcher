package com.tvlauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var appSlots: RecyclerView
    private lateinit var settingsButton: ImageButton
    private lateinit var wallpaperButton: ImageButton
    private lateinit var aboutButton: View
    private lateinit var wallpaperImageView: ImageView
    private lateinit var wallpaperDimOverlay: View
    private lateinit var appManager: AppManager
    private lateinit var appSlotAdapter: AppSlotAdapter
    private var lastFocusedPosition: Int = 0

    /** Increased bitmap size for crisp rounded icons. */
    private val slotIconSizePx: Int by lazy { (72 * resources.displayMetrics.density).toInt() }

    private val refreshReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == LauncherBroadcast.ACTION_REFRESH_HOME) {
                appManager.invalidateSelectionCache()
                appManager.invalidateAppListCache()
                loadAppSlots()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        appSlots = findViewById(R.id.appSlots)
        settingsButton = findViewById(R.id.settingsButton)
        wallpaperButton = findViewById(R.id.wallpaperButton)
        aboutButton = findViewById(R.id.aboutButton)
        wallpaperImageView = findViewById(R.id.wallpaperImageView)
        wallpaperDimOverlay = findViewById(R.id.wallpaperDimOverlay)

        appManager = AppManager(this)
        setupRecyclerView()
        setupClickListeners()
        loadAppSlots()
        applyWallpaper()

        ContextCompat.registerReceiver(
            this,
            refreshReceiver,
            android.content.IntentFilter(LauncherBroadcast.ACTION_REFRESH_HOME),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (appSlots.childCount > 0) {
                appSlots.getChildAt(0).requestFocus()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun setupRecyclerView() {
        val slotSizePx = calculateSlotSizePx()
        appSlotAdapter = AppSlotAdapter(
            onSlotClick = { position ->
                lastFocusedPosition = position
                openAppSelection()
            },
            onSlotLongClick = { position ->
                lastFocusedPosition = position
                showAppContextMenu(position)
            },
            slotSizePx = slotSizePx,
            iconSizePx = slotIconSizePx
        )

        val columns = HOME_GRID_COLUMNS
        appSlots.layoutManager = GridLayoutManager(this, columns)
        appSlots.adapter = appSlotAdapter
        appSlots.setHasFixedSize(true)
        appSlots.setItemViewCacheSize(0)
        appSlots.recycledViewPool.setMaxRecycledViews(0, 2)
    }

    private fun setupClickListeners() {
        settingsButton.setOnClickListener { openAndroidSettings() }
        wallpaperButton.setOnClickListener { showWallpaperDialog() }
        aboutButton.setOnClickListener { showAboutDialog() }
    }

    private fun applyWallpaper(force: Boolean = false) {
        val mode = WallpaperManager.getWallpaperMode(this)
        val dimEnabled = WallpaperManager.isDimOverlayEnabled(this)

        if (mode == WallpaperManager.MODE_SOLID) {
            wallpaperImageView.visibility = View.GONE
            wallpaperDimOverlay.visibility = View.GONE
            return
        }

        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        WallpaperManager.refreshWallpaperIfNeeded(this, width, height, force) { success, drawable ->
            if (success && drawable != null) {
                wallpaperImageView.setImageDrawable(drawable)
                wallpaperImageView.visibility = View.VISIBLE
                wallpaperDimOverlay.visibility = if (dimEnabled) View.VISIBLE else View.GONE
            } else if (mode == WallpaperManager.MODE_SOLID) {
                wallpaperImageView.visibility = View.GONE
                wallpaperDimOverlay.visibility = View.GONE
            }
        }
    }

    private fun showWallpaperDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_wallpaper_settings, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.wallpaperModeGroup)
        val radioSolid = dialogView.findViewById<RadioButton>(R.id.radioModeSolid)
        val radioReddit = dialogView.findViewById<RadioButton>(R.id.radioModeReddit)
        val radioCustom = dialogView.findViewById<RadioButton>(R.id.radioModeCustom)

        val redditContainer = dialogView.findViewById<LinearLayout>(R.id.redditOptionsContainer)
        val categoryButton = dialogView.findViewById<TextView>(R.id.categorySelectorButton)
        val intervalButton = dialogView.findViewById<TextView>(R.id.intervalSelectorButton)

        val customUrlContainer = dialogView.findViewById<LinearLayout>(R.id.customUrlContainer)
        val customUrlEdit = dialogView.findViewById<EditText>(R.id.customUrlEdit)
        val dimSwitch = dialogView.findViewById<SwitchCompat>(R.id.dimOverlaySwitch)

        // Categories setup
        val categories = listOf(
            WallpaperManager.CATEGORY_GENERAL to getString(R.string.wallpaper_category_general),
            WallpaperManager.CATEGORY_NATURE to getString(R.string.wallpaper_category_nature),
            WallpaperManager.CATEGORY_CARS to getString(R.string.wallpaper_category_cars),
            WallpaperManager.CATEGORY_ANIME to getString(R.string.wallpaper_category_anime),
            WallpaperManager.CATEGORY_SPACE to getString(R.string.wallpaper_category_space),
            WallpaperManager.CATEGORY_ARCHITECTURE to getString(R.string.wallpaper_category_architecture)
        )
        val initialCategoryKey = WallpaperManager.getCategory(this)
        var selectedCategoryIndex = categories.indexOfFirst { it.first == initialCategoryKey }.coerceAtLeast(0)
        categoryButton.text = categories[selectedCategoryIndex].second
        categoryButton.setOnClickListener {
            AlertDialog.Builder(this, R.style.Theme_TVLauncher_AboutDialog)
                .setTitle(R.string.wallpaper_category)
                .setSingleChoiceItems(
                    categories.map { it.second }.toTypedArray(),
                    selectedCategoryIndex
                ) { dialog, which ->
                    selectedCategoryIndex = which
                    categoryButton.text = categories[which].second
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        // Intervals setup
        val intervals = listOf(
            WallpaperManager.INTERVAL_15M to getString(R.string.wallpaper_interval_15m),
            WallpaperManager.INTERVAL_1H to getString(R.string.wallpaper_interval_1h),
            WallpaperManager.INTERVAL_6H to getString(R.string.wallpaper_interval_6h),
            WallpaperManager.INTERVAL_24H to getString(R.string.wallpaper_interval_24h)
        )
        val initialIntervalValue = WallpaperManager.getInterval(this)
        var selectedIntervalIndex = intervals.indexOfFirst { it.first == initialIntervalValue }.coerceAtLeast(0)
        intervalButton.text = intervals[selectedIntervalIndex].second
        intervalButton.setOnClickListener {
            AlertDialog.Builder(this, R.style.Theme_TVLauncher_AboutDialog)
                .setTitle(R.string.wallpaper_interval)
                .setSingleChoiceItems(
                    intervals.map { it.second }.toTypedArray(),
                    selectedIntervalIndex
                ) { dialog, which ->
                    selectedIntervalIndex = which
                    intervalButton.text = intervals[which].second
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        customUrlEdit.setText(WallpaperManager.getCustomUrl(this))
        dimSwitch.isChecked = WallpaperManager.isDimOverlayEnabled(this)

        fun updateVisibility(mode: String) {
            redditContainer.visibility = if (mode == WallpaperManager.MODE_REDDIT) View.VISIBLE else View.GONE
            customUrlContainer.visibility = if (mode == WallpaperManager.MODE_CUSTOM) View.VISIBLE else View.GONE
        }

        val initialMode = WallpaperManager.getWallpaperMode(this)
        when (initialMode) {
            WallpaperManager.MODE_SOLID -> radioSolid.isChecked = true
            WallpaperManager.MODE_REDDIT -> radioReddit.isChecked = true
            WallpaperManager.MODE_CUSTOM -> radioCustom.isChecked = true
        }
        updateVisibility(initialMode)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioModeReddit -> WallpaperManager.MODE_REDDIT
                R.id.radioModeCustom -> WallpaperManager.MODE_CUSTOM
                else -> WallpaperManager.MODE_SOLID
            }
            updateVisibility(mode)
        }

        fun saveWallpaperSettings(): String {
            val selectedMode = when (radioGroup.checkedRadioButtonId) {
                R.id.radioModeReddit -> WallpaperManager.MODE_REDDIT
                R.id.radioModeCustom -> WallpaperManager.MODE_CUSTOM
                else -> WallpaperManager.MODE_SOLID
            }
            WallpaperManager.setWallpaperMode(this, selectedMode)
            WallpaperManager.setCategory(this, categories[selectedCategoryIndex].first)
            WallpaperManager.setInterval(this, intervals[selectedIntervalIndex].first)
            WallpaperManager.setCustomUrl(this, customUrlEdit.text.toString().trim())
            WallpaperManager.setDimOverlayEnabled(this, dimSwitch.isChecked)
            return selectedMode
        }

        AlertDialog.Builder(this, R.style.Theme_TVLauncher_AboutDialog)
            .setTitle(R.string.wallpaper_settings)
            .setView(dialogView)
            .setPositiveButton(R.string.done) { _, _ ->
                saveWallpaperSettings()
                applyWallpaper(force = true)
            }
            .setNeutralButton(R.string.wallpaper_refresh_now) { _, _ ->
                Toast.makeText(this, R.string.wallpaper_updating, Toast.LENGTH_SHORT).show()
                saveWallpaperSettings()
                applyWallpaper(force = true)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAppContextMenu(position: Int) {
        val currentList = appSlotAdapter.currentList
        val slot = currentList.getOrNull(position) ?: return
        val app = slot.appInfo ?: return

        val options = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        // 1. Open
        options.add(getString(R.string.app_menu_open))
        actions.add {
            if (app.isShortcut) {
                ShortcutHelper.launchShortcut(this, app.packageName, app.shortcutId!!)
            } else {
                ShortcutHelper.launchApp(this, app.packageName)
            }
        }

        // 2. Move Left
        if (position > 0) {
            options.add(getString(R.string.app_menu_move_left))
            actions.add {
                if (appManager.moveApp(position, position - 1)) {
                    appManager.invalidateSelectionCache()
                    lastFocusedPosition = position - 1
                    loadAppSlots()
                }
            }
        }

        // 3. Move Right
        val totalApps = currentList.count { !it.isEmpty }
        if (position < totalApps - 1) {
            options.add(getString(R.string.app_menu_move_right))
            actions.add {
                if (appManager.moveApp(position, position + 1)) {
                    appManager.invalidateSelectionCache()
                    lastFocusedPosition = position + 1
                    loadAppSlots()
                }
            }
        }

        // 4. App Info
        if (!app.isShortcut) {
            options.add(getString(R.string.app_menu_info))
            actions.add {
                openAppDetails(app.packageName)
            }
        }

        // 5. Remove
        options.add(getString(R.string.remove))
        actions.add {
            confirmRemoveApp(app, position)
        }

        AlertDialog.Builder(this, R.style.Theme_TVLauncher_AboutDialog)
            .setTitle(app.getDisplayName())
            .setItems(options.toTypedArray()) { _, which ->
                actions.getOrNull(which)?.invoke()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openAppDetails(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, packageName, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAboutDialog() {
        val versionName = getVersionName()
        val message = getString(R.string.about_message, versionName, getString(R.string.github_repo_url))
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)
        dialogView.findViewById<TextView>(R.id.aboutMessageText).text = message

        // 1. Home button override switch
        val homeOverrideSwitch = dialogView.findViewById<SwitchCompat>(R.id.homeOverrideSwitch)
        val isOverrideConfigured = TvHomeOverrideService.isHomeOverrideEnabled(this)
        val isServiceActive = TvHomeOverrideService.isAccessibilityServiceEnabled(this)
        homeOverrideSwitch.isChecked = isOverrideConfigured && isServiceActive

        homeOverrideSwitch.setOnCheckedChangeListener { _, isChecked ->
            TvHomeOverrideService.setHomeOverrideEnabled(this, isChecked)
            if (isChecked) {
                if (!TvHomeOverrideService.isAccessibilityServiceEnabled(this)) {
                    Toast.makeText(this, R.string.enable_accessibility_prompt, Toast.LENGTH_LONG).show()
                    openAccessibilitySettings()
                }
            }
        }

        // 2. Stock Launcher RAM killer switch
        val suppressRamSwitch = dialogView.findViewById<SwitchCompat>(R.id.suppressStockRamSwitch)
        suppressRamSwitch.isChecked = TvHomeOverrideService.isSuppressStockLauncherEnabled(this)
        suppressRamSwitch.setOnCheckedChangeListener { _, isChecked ->
            TvHomeOverrideService.setSuppressStockLauncherEnabled(this, isChecked)
            if (isChecked) {
                RamOptimizationHelper.suppressStockLauncherRam(applicationContext)
            }
        }

        // 3. Auto update switch
        val autoUpdateSwitch = dialogView.findViewById<SwitchCompat>(R.id.autoUpdateSwitch)
        autoUpdateSwitch.isChecked = AppUpdateManager.isAutoUpdateEnabled(this)
        autoUpdateSwitch.setOnCheckedChangeListener { _, isChecked ->
            AppUpdateManager.setAutoUpdateEnabled(this, isChecked)
            val hint = if (isChecked) R.string.update_auto_on else R.string.update_auto_off
            Toast.makeText(this, hint, Toast.LENGTH_SHORT).show()
        }

        AlertDialog.Builder(this, R.style.Theme_TVLauncher_AboutDialog)
            .setTitle(R.string.about_title)
            .setView(dialogView)
            .setPositiveButton(R.string.about_open_github) { _, _ ->
                openUrl(getString(R.string.github_repo_url))
            }
            .setNeutralButton(R.string.update_check_now) { _, _ ->
                checkForUpdatesManually()
            }
            .setNegativeButton(R.string.about_close, null)
            .show()
    }

    private fun openAccessibilitySettings() {
        val intents = listOf(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            Intent("android.settings.ACCESSIBILITY_SETTINGS"),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }
    }

    private fun checkForUpdatesManually() {
        Toast.makeText(this, R.string.update_checking, Toast.LENGTH_SHORT).show()
        Thread {
            val result = AppUpdateManager.checkDownloadAndInstall(
                applicationContext,
                ignoreAutoUpdateSetting = true
            )
            runOnUiThread {
                val message = when (result) {
                    AppUpdateManager.UpdateResult.NoUpdate -> getString(R.string.update_none)
                    AppUpdateManager.UpdateResult.InstallStarted -> getString(R.string.update_ready)
                    AppUpdateManager.UpdateResult.InstallPermissionNeeded ->
                        getString(R.string.update_permission_needed)
                    AppUpdateManager.UpdateResult.DownloadFailed -> getString(R.string.update_failed)
                    AppUpdateManager.UpdateResult.InvalidRelease -> getString(R.string.update_invalid_release)
                    AppUpdateManager.UpdateResult.Skipped -> getString(R.string.update_disabled)
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun getVersionName(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionName
            }
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME
        } ?: BuildConfig.VERSION_NAME
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
        }
    }

    private fun calculateSlotSizePx(): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthPx = displayMetrics.widthPixels
        val density = displayMetrics.density
        val columns = HOME_GRID_COLUMNS

        val sideMarginPx = (24 * density).toInt() * 2
        val recyclerPaddingPx = (8 * density).toInt() * 2
        val itemMarginPx = (8 * density).toInt()
        val totalItemMarginsPx = itemMarginPx * 2 * columns

        val availablePx = screenWidthPx - sideMarginPx - recyclerPaddingPx - totalItemMarginsPx
        val rawSize = availablePx / columns
        val minSize = (96 * density).toInt()
        val maxSize = (130 * density).toInt()
        return rawSize.coerceIn(minSize, maxSize)
    }

    private fun openAndroidSettings() {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: Exception) {
            try {
                startActivity(Intent("android.settings.TV_SETTINGS"))
            } catch (_: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun loadAppSlots() {
        val selectedApps = appManager.getSelectedAppInfos()

        val slots = mutableListOf<AppSlotAdapter.AppSlot>()
        selectedApps.forEach { app ->
            slots.add(AppSlotAdapter.AppSlot(app, false))
        }

        // Always show "+" so user can add or change apps
        slots.add(AppSlotAdapter.AppSlot(null, true))

        appSlotAdapter.submitList(slots) {
            restoreFocus()
        }
    }

    private fun restoreFocus() {
        appSlots.post {
            val targetPos = lastFocusedPosition.coerceIn(0, (appSlotAdapter.itemCount - 1).coerceAtLeast(0))
            val holder = appSlots.findViewHolderForAdapterPosition(targetPos)
            if (holder != null) {
                holder.itemView.requestFocus()
            } else if (appSlots.childCount > 0) {
                appSlots.getChildAt(0).requestFocus()
            }
        }
    }

    private fun confirmRemoveApp(app: AppInfo, position: Int) {
        AlertDialog.Builder(this, R.style.Theme_TVLauncher_AboutDialog)
            .setTitle(R.string.remove_app_title)
            .setMessage(getString(R.string.remove_app_message, app.getDisplayName()))
            .setPositiveButton(R.string.remove) { _, _ ->
                appManager.removeSelectedApp(AppIdentifier.encode(app))
                appManager.invalidateSelectionCache()
                LauncherBroadcast.refreshHome(this)
                lastFocusedPosition = (position - 1).coerceAtLeast(0)
                loadAppSlots()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openAppSelection() {
        startActivity(Intent(this, AppSelectionActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        loadAppSlots()
        applyWallpaper(force = false)
        RamOptimizationHelper.suppressStockLauncherRam(applicationContext)
    }

    override fun onStop() {
        super.onStop()
        appManager.invalidateAppListCache()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(refreshReceiver)
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val HOME_GRID_COLUMNS = 5
    }
}
