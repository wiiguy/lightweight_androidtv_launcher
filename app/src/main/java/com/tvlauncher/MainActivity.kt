package com.tvlauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.ImageButton
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
    private lateinit var aboutButton: android.view.View
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
        aboutButton = findViewById(R.id.aboutButton)

        appManager = AppManager(this)
        setupRecyclerView()
        setupClickListeners()
        loadAppSlots()

        ContextCompat.registerReceiver(
            this,
            refreshReceiver,
            android.content.IntentFilter(LauncherBroadcast.ACTION_REFRESH_HOME),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // TV Home Launcher ignores back or moves focus to first slot
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
        aboutButton.setOnClickListener { showAboutDialog() }
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
        homeOverrideSwitch.isChecked = TvHomeOverrideService.isHomeOverrideEnabled(this)
        homeOverrideSwitch.setOnCheckedChangeListener { _, isChecked ->
            TvHomeOverrideService.setHomeOverrideEnabled(this, isChecked)
            if (isChecked) {
                Toast.makeText(this, R.string.enable_accessibility_prompt, Toast.LENGTH_LONG).show()
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (_: Exception) {
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
