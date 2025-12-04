package com.tvlauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    
    private lateinit var appSlots: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var settingsButton: Button
    private lateinit var clockText: TextView
    private lateinit var appManager: AppManager
    private lateinit var appSlotAdapter: AppSlotAdapter
    private val clockHandler = Handler(Looper.getMainLooper())
    private var clockRunnable: Runnable? = null
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Ensure the launcher doesn't prevent the TV from sleeping
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        appSlots = findViewById(R.id.appSlots)
        emptyText = findViewById(R.id.emptyText)
        settingsButton = findViewById(R.id.settingsButton)
        clockText = findViewById(R.id.clockText)
        
        appManager = AppManager(this)
        setupRecyclerView()
        setupClickListeners()
        loadAppSlots()
        startClock()
    }
    
    private fun setupRecyclerView() {
        appSlotAdapter = AppSlotAdapter(
            mutableListOf(),
            onSlotClick = { position ->
                openAppSelection(position)
            }
        )
        
        val layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
        appSlots.layoutManager = layoutManager
        appSlots.adapter = appSlotAdapter
        appSlots.setHasFixedSize(true)
        // Reduce RecyclerView cache sizes for lower memory usage
        appSlots.setItemViewCacheSize(2)
        appSlots.recycledViewPool.setMaxRecycledViews(0, 2)
    }
    
    private fun setupClickListeners() {
        settingsButton.setOnClickListener {
            openAndroidSettings()
        }
        
        settingsButton.isFocusable = false
        findViewById<Button>(R.id.addAppsButton).visibility = View.GONE
        findViewById<Button>(R.id.reorderButton).visibility = View.GONE
        findViewById<Button>(R.id.clearAllButton).visibility = View.GONE
        findViewById<TextView>(R.id.reorderStatusText).visibility = View.GONE
    }
    
    private fun openAndroidSettings() {
        try {
            val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS)
            startActivity(settingsIntent)
        } catch (e: Exception) {
            try {
                val tvSettingsIntent = Intent("android.settings.TV_SETTINGS")
                startActivity(tvSettingsIntent)
            } catch (e2: Exception) {
                try {
                    val systemSettingsIntent = Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS)
                    startActivity(systemSettingsIntent)
                } catch (e3: Exception) {
                    // All settings intents failed
                }
            }
        }
    }
    
    private fun loadAppSlots() {
        val selectedApps = appManager.getSelectedAppInfos()
        val maxSlots = appSlotAdapter.getMaxSlots()
        
        val slots = mutableListOf<AppSlotAdapter.AppSlot>()
        
        selectedApps.forEach { app ->
            slots.add(AppSlotAdapter.AppSlot(app, false))
        }
        
        // Always add at least one empty slot if we have space
        if (slots.size < maxSlots) {
            slots.add(AppSlotAdapter.AppSlot(null, true))
        }
        
        appSlotAdapter.updateSlots(slots)
        
        // Always show the app slots, never show empty text
        emptyText.visibility = View.GONE
        appSlots.visibility = View.VISIBLE
        
        appSlots.post {
            if (appSlots.childCount > 0) {
                appSlots.getChildAt(0).requestFocus()
            }
            settingsButton.isFocusable = true
        }
    }
    
    private fun openAppSelection(slotPosition: Int) {
        val intent = Intent(this, AppSelectionActivity::class.java)
        intent.putExtra("slot_position", slotPosition)
        startActivity(intent)
    }
    
    override fun onResume() {
        super.onResume()
        loadAppSlots()
        startClock()
    }
    
    override fun onPause() {
        super.onPause()
        stopClock()
        // Clear cache when paused to free memory
        appManager.clearCache()
        // Allow the TV to go to sleep properly
    }
    
    override fun onStop() {
        super.onStop()
        stopClock()
        unloadLauncher()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopClock()
    }
    
    private fun startClock() {
        updateClock()
        if (clockRunnable == null) {
            clockRunnable = object : Runnable {
                override fun run() {
                    updateClock()
                    clockHandler.postDelayed(this, 60000) // Update every minute
                }
            }
        }
        clockHandler.post(clockRunnable!!)
    }
    
    private fun stopClock() {
        clockRunnable?.let {
            clockHandler.removeCallbacks(it)
            clockRunnable = null
        }
        // Clear clock text to free memory when not visible
        if (::clockText.isInitialized) {
            clockText.text = ""
        }
    }
    
    private fun updateClock() {
        if (::clockText.isInitialized) {
            clockText.text = timeFormat.format(java.util.Date())
        }
    }
    
    private fun unloadLauncher() {
        // Only clear app manager cache to free memory
        appManager.clearCache()
        
        // Force garbage collection for cleanup
        System.gc()
    }
    
}