package com.tvlauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var appSlots: RecyclerView
    private lateinit var settingsButton: Button
    private lateinit var aboutButton: TextView
    private lateinit var appManager: AppManager
    private lateinit var appSlotAdapter: AppSlotAdapter
    /** Small bitmaps on home — avoids holding full-resolution launcher icons in RAM. */
    private val slotIconSizePx: Int by lazy { (60 * resources.displayMetrics.density).toInt() }

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

    private fun setupRecyclerView() {
        val slotSizePx = calculateSlotSizePx()
        appSlotAdapter = AppSlotAdapter(
            mutableListOf(),
            onSlotClick = { openAppSelection() },
            onSlotLongClick = { position -> confirmRemoveApp(position) },
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
        settingsButton.isFocusable = false
        aboutButton.isFocusable = true
    }

    private fun showAboutDialog() {
        val versionName = getVersionName()
        val message = getString(R.string.about_message, versionName, getString(R.string.github_repo_url))
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)
        dialogView.findViewById<TextView>(R.id.aboutMessageText).text = message
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
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        } catch (_: Exception) {
        }
    }

    private fun calculateSlotSizePx(): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthPx = displayMetrics.widthPixels
        val density = displayMetrics.density
        val columns = HOME_GRID_COLUMNS

        val sideMarginPx = (16 * density).toInt() * 2
        val recyclerPaddingPx = (8 * density).toInt() * 2
        val itemMarginPx = (8 * density).toInt()
        val totalItemMarginsPx = itemMarginPx * 2 * columns

        val availablePx = screenWidthPx - sideMarginPx - recyclerPaddingPx - totalItemMarginsPx
        val rawSize = availablePx / columns
        val minSize = (96 * density).toInt()
        val maxSize = (120 * density).toInt()
        return rawSize.coerceIn(minSize, maxSize)
    }

    private fun openAndroidSettings() {
        try {
            startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
        } catch (_: Exception) {
            try {
                startActivity(Intent("android.settings.TV_SETTINGS"))
            } catch (_: Exception) {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS))
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

        // Always show "+" so user can add or change apps (even when 8 are already pinned)
        slots.add(AppSlotAdapter.AppSlot(null, true))

        appSlotAdapter.updateSlots(slots)
        appSlots.visibility = android.view.View.VISIBLE

        appSlots.post {
            if (appSlots.childCount > 0) {
                appSlots.getChildAt(0).requestFocus()
            }
            settingsButton.isFocusable = true
        }
    }

    private fun confirmRemoveApp(position: Int) {
        val slot = appSlotAdapter.slots.getOrNull(position) ?: return
        val app = slot.appInfo ?: return

        AlertDialog.Builder(this)
            .setTitle(R.string.remove_app_title)
            .setMessage(getString(R.string.remove_app_message, app.getDisplayName()))
            .setPositiveButton(R.string.remove) { _, _ ->
                appManager.removeSelectedApp(AppIdentifier.encode(app))
                appManager.invalidateSelectionCache()
                LauncherBroadcast.refreshHome(this)
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
