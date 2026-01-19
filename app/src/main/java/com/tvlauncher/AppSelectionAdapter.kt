package com.tvlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppSelectionAdapter(
    private val apps: List<AppInfo>,
    private val selectedApps: MutableSet<String>,
    private val onSelectionChanged: (String, Boolean) -> Unit,
    private val appManager: AppManager? = null
) : RecyclerView.Adapter<AppSelectionAdapter.AppSelectionViewHolder>() {

    class AppSelectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val appName: TextView = itemView.findViewById(R.id.appName)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppSelectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_selection, parent, false)
        return AppSelectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppSelectionViewHolder, position: Int) {
        val app = apps[position]
        // Lazy load icon only when visible
        holder.appIcon.setImageDrawable(app.getIcon(holder.itemView.context.packageManager))
        holder.appName.text = app.getDisplayName()
        
        // Prevent checkbox from getting focus during scrolling
        holder.checkBox.isFocusable = false
        holder.checkBox.isFocusableInTouchMode = false
        
        // Get app identifier (packageName for apps, packageName:shortcutId for shortcuts)
        val appIdentifier = appManager?.getAppIdentifier(app) ?: if (app.isShortcut) {
            "${app.packageName}:${app.shortcutId}"
        } else {
            app.packageName
        }
        
        // Remove listener before setting checked state to prevent false triggers
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selectedApps.contains(appIdentifier)
        
        // Set listener after state is set
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            // Only trigger if this is a real user action (not view recycling)
            if (holder.adapterPosition == position) {
                onSelectionChanged(appIdentifier, isChecked)
            }
        }
        
        holder.itemView.setOnClickListener {
            // Only allow selection through explicit click
            if (holder.adapterPosition == position) {
                val newCheckedState = !holder.checkBox.isChecked
                holder.checkBox.isChecked = newCheckedState
            }
        }
    }

    override fun getItemCount(): Int = apps.size
}
