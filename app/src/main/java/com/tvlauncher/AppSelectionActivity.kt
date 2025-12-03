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
    private var isProcessingSelection = false
    
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
            // Prevent rapid successive selections
            if (isProcessingSelection) {
                return@AppSelectionAdapter
            }
            
            if (isSelected) {
                isProcessingSelection = true
                selectedApps.add(packageName)
                appManager.saveSelectedApps(selectedApps)
                setResult(RESULT_OK)
                finish()
            } else {
                selectedApps.remove(packageName)
            }
        }
        
        // Calculate number of columns based on screen size
        val spanCount = calculateSpanCount()
        appList.layoutManager = GridLayoutManager(this@AppSelectionActivity, spanCount)
        appList.adapter = appSelectionAdapter
    }
    
    private fun setupClickListeners() {
        doneButton.setOnClickListener {
            appManager.saveSelectedApps(selectedApps)
            finish()
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
