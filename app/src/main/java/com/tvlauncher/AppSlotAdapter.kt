package com.tvlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppSlotAdapter(
    var slots: MutableList<AppSlot>,
    private val onSlotClick: (Int) -> Unit
) : RecyclerView.Adapter<AppSlotAdapter.AppSlotViewHolder>() {

    data class AppSlot(
        val appInfo: AppInfo? = null,
        val isEmpty: Boolean = true
    )

    class AppSlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val appName: TextView = itemView.findViewById(R.id.appName)
        val plusButton: TextView = itemView.findViewById(R.id.plusButton)
        val addText: TextView = itemView.findViewById(R.id.addText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppSlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_slot, parent, false)
        return AppSlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppSlotViewHolder, position: Int) {
        val slot = slots[position]
        
        if (slot.isEmpty) {
            holder.appIcon.visibility = View.GONE
            holder.appName.visibility = View.GONE
            holder.plusButton.visibility = View.VISIBLE
            holder.addText.visibility = View.VISIBLE
            
            holder.itemView.setOnClickListener {
                onSlotClick(position)
            }
        } else {
            slot.appInfo?.let { app ->
                // Lazy load icon only when visible
                holder.appIcon.setImageDrawable(app.getIcon(holder.itemView.context.packageManager))
                holder.appName.text = app.appName
                
                holder.appIcon.visibility = View.VISIBLE
                holder.appName.visibility = View.VISIBLE
                holder.plusButton.visibility = View.GONE
                holder.addText.visibility = View.GONE
                
                holder.itemView.setOnClickListener {
                    launchApp(holder.itemView.context, app.packageName)
                }
            }
        }
    }

    override fun getItemCount(): Int = slots.size
    
    fun updateSlots(newSlots: List<AppSlot>) {
        slots.clear()
        slots.addAll(newSlots)
        notifyDataSetChanged()
    }
    
    companion object {
        const val MAX_SLOTS = 8
    }
    
    fun getMaxSlots(): Int = MAX_SLOTS

    private fun launchApp(context: android.content.Context, packageName: String) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                return
            }
        } catch (e: Exception) {
            // Continue to next method
        }
        
        try {
            val leanbackIntent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            leanbackIntent.addCategory(android.content.Intent.CATEGORY_LEANBACK_LAUNCHER)
            leanbackIntent.setPackage(packageName)
            
            val resolveInfos = context.packageManager.queryIntentActivities(leanbackIntent, 0)
            if (resolveInfos.isNotEmpty()) {
                val activityInfo = resolveInfos[0].activityInfo
                val intent = android.content.Intent().apply {
                    setClassName(activityInfo.packageName, activityInfo.name)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        } catch (e: Exception) {
            // Continue to next method
        }
        
        try {
            val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            mainIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            mainIntent.setPackage(packageName)
            
            val resolveInfos = context.packageManager.queryIntentActivities(mainIntent, 0)
            if (resolveInfos.isNotEmpty()) {
                val activityInfo = resolveInfos[0].activityInfo
                val intent = android.content.Intent().apply {
                    setClassName(activityInfo.packageName, activityInfo.name)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        } catch (e: Exception) {
            // All methods failed
        }
    }
}
