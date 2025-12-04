package com.tvlauncher

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView

class AppSelectionActivity : AppCompatActivity() {
    
    private lateinit var appList: RecyclerView
    private lateinit var doneButton: Button
    private lateinit var appManager: AppManager
    private lateinit var appSelectionAdapter: AppSelectionAdapter
    private val selectedApps = mutableSetOf<String>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)
        
        appList = findViewById(R.id.appList)
        doneButton = findViewById(R.id.doneButton)
        
        appManager = AppManager(this)
        selectedApps.addAll(appManager.getSelectedApps())
        setupRecyclerView()
        setupClickListeners()
    }
    
    private fun setupRecyclerView() {
        val allApps = appManager.getInstalledApps()
        
        appSelectionAdapter = AppSelectionAdapter(
            allApps,
            selectedApps
        ) { packageName, isSelected ->
            // Just update the selection set, don't save or finish yet
            if (isSelected) {
                selectedApps.add(packageName)
            } else {
                selectedApps.remove(packageName)
            }
        }
        
        // Calculate number of columns based on screen size
        val spanCount = calculateSpanCount()
        appList.layoutManager = GridLayoutManager(this@AppSelectionActivity, spanCount)
        appList.adapter = appSelectionAdapter
        // Reduce RecyclerView cache sizes for lower memory usage
        appList.setItemViewCacheSize(5)
        appList.recycledViewPool.setMaxRecycledViews(0, 5)
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
